package dev.tapdrop.spike;

import android.content.Context;
import android.content.SharedPreferences;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Emulates an NFC Forum Type 4 tag that holds one NDEF record.
 * A reader selects the NDEF application, reads the capability container (E103),
 * then reads the NDEF file (E104). Read-only: writes are refused.
 *
 * The record comes from the slot selected in the activity: a contact card,
 * a Wi-Fi login, a UPI payment request, or a link.
 */
public class NdefTagService extends HostApduService {
    static final String TAG = "TapDrop";

    /** Count of completed reads. The activity polls it to flash "Sent". */
    static volatile int reads = 0;

    private static final byte[] NDEF_AID = hex("D2760000850101");
    private static final byte[] OK = {(byte) 0x90, 0x00};
    private static final byte[] NOT_FOUND = {0x6A, (byte) 0x82};
    private static final byte[] WRONG_PARAMS = {0x6B, 0x00};
    private static final byte[] NOT_SUPPORTED = {0x6D, 0x00};

    private static final int MAX_NDEF_FILE = 0x0400;

    // Capability container: v2.0, MLe 0x3B, MLc 0x34, one NDEF file E104, read open, write denied.
    private static final byte[] CC_FILE = {
        0x00, 0x0F, 0x20, 0x00, 0x3B, 0x00, 0x34,
        0x04, 0x06, (byte) 0xE1, 0x04,
        (byte) (MAX_NDEF_FILE >> 8), (byte) MAX_NDEF_FILE,
        0x00, (byte) 0xFF
    };

    private byte[] ndefFile = new byte[0];
    private byte[] selected = null;
    private boolean buzzed = false;

    @Override
    public byte[] processCommandApdu(byte[] apdu, Bundle extras) {
        byte[] reply = handle(apdu);
        Log.d(TAG, "<- " + toHex(apdu) + "   -> " + toHex(reply));
        return reply;
    }

    private byte[] handle(byte[] apdu) {
        if (apdu == null || apdu.length < 4) return NOT_SUPPORTED;
        int ins = apdu[1] & 0xFF;
        int p1 = apdu[2] & 0xFF;
        int p2 = apdu[3] & 0xFF;

        if (ins == 0xA4) {                       // SELECT
            byte[] data = body(apdu);
            if (p1 == 0x04) {                    // by application name
                if (!Arrays.equals(data, NDEF_AID)) return NOT_FOUND;
                SharedPreferences p = getSharedPreferences(Slots.PREFS, Context.MODE_PRIVATE);
                ndefFile = buildNdefFile(p);
                Log.d(TAG, "serving type=" + Slots.TYPES[Slots.index(p)] + " bytes=" + ndefFile.length);
                selected = null;
                buzzed = false;
                return OK;
            }
            if (data.length == 2 && (data[0] & 0xFF) == 0xE1) {   // by file id
                if ((data[1] & 0xFF) == 0x03) { selected = CC_FILE; return OK; }
                if ((data[1] & 0xFF) == 0x04) { selected = ndefFile; return OK; }
            }
            return NOT_FOUND;
        }

        if (ins == 0xB0) {                       // READ BINARY
            if (selected == null) return NOT_FOUND;
            int offset = (p1 << 8) | p2;
            int le = apdu.length > 4 ? (apdu[4] & 0xFF) : 0;
            if (le == 0) le = 256;
            if (offset > selected.length) return WRONG_PARAMS;
            int n = Math.min(le, selected.length - offset);
            byte[] out = new byte[n + 2];
            System.arraycopy(selected, offset, out, 0, n);
            out[n] = (byte) 0x90;
            out[n + 1] = 0x00;
            if (selected == ndefFile && offset + n >= ndefFile.length && !buzzed) {
                buzzed = true;
                reads++;
                Log.d(TAG, "READ COMPLETE");
                buzz();
            }
            return out;
        }

        return NOT_SUPPORTED;                    // includes UPDATE BINARY: tag is read-only
    }

    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "deactivated, reason=" + reason);
        selected = null;
    }

    private void buzz() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) v.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    // ---- NDEF building -------------------------------------------------------

    /** NDEF file = 2-byte message length, then one record for the selected slot. */
    static byte[] buildNdefFile(SharedPreferences p) {
        byte[] record;
        switch (Slots.index(p)) {
            case 0:
                record = record(0x02, ascii(p.getString(Slots.KEY_MIME, "text/vcard")),
                    vcard(p).getBytes(StandardCharsets.UTF_8));
                break;
            case 1:
                record = record(0x02, ascii("application/vnd.wfa.wsc"),
                    wifiCredential(p.getString(Slots.KEY_SSID, Slots.DEMO_SSID),
                        p.getString(Slots.KEY_PASS, Slots.DEMO_PASS)));
                break;
            case 2:
                record = uriRecord(Slots.upiLink(p));
                break;
            default:
                record = uriRecord(p.getString(Slots.KEY_URL, Slots.DEMO_URL));
        }
        byte[] file = new byte[2 + record.length];
        file[0] = (byte) (record.length >> 8);
        file[1] = (byte) record.length;
        System.arraycopy(record, 0, file, 2, record.length);
        return file;
    }

    /** A single-record message: MB and ME set, short form when the payload fits in one byte. */
    static byte[] record(int tnf, byte[] type, byte[] payload) {
        boolean shortForm = payload.length < 256;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x80 | 0x40 | (shortForm ? 0x10 : 0) | tnf);
        out.write(type.length);
        if (shortForm) {
            out.write(payload.length);
        } else {
            out.write(payload.length >>> 24);
            out.write(payload.length >>> 16);
            out.write(payload.length >>> 8);
            out.write(payload.length);
        }
        out.write(type, 0, type.length);
        out.write(payload, 0, payload.length);
        return out.toByteArray();
    }

    static byte[] uriRecord(String url) {
        // NDEF URI prefix codes 1 to 4. The www forms come first so the longest prefix wins.
        // Anything else, such as upi://, is sent whole with code 0.
        String[] prefixes = {"http://www.", "https://www.", "http://", "https://"};
        int code = 0;
        String rest = url;
        for (int i = 0; i < prefixes.length; i++) {
            if (url.startsWith(prefixes[i])) { code = i + 1; rest = url.substring(prefixes[i].length()); break; }
        }
        byte[] restBytes = rest.getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[1 + restBytes.length];
        payload[0] = (byte) code;
        System.arraycopy(restBytes, 0, payload, 1, restBytes.length);
        return record(0x01, ascii("U"), payload);
    }

    static String vcard(SharedPreferences p) {
        String name = p.getString(Slots.KEY_NAME, Slots.DEMO_NAME);
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCARD\r\nVERSION:3.0\r\n");
        sb.append("FN:").append(name).append("\r\n");
        sb.append("N:").append(name).append(";;;;\r\n");
        String phone = p.getString(Slots.KEY_PHONE, Slots.DEMO_PHONE);
        if (!phone.isEmpty()) sb.append("TEL;TYPE=CELL:").append(phone).append("\r\n");
        String email = p.getString(Slots.KEY_EMAIL, "");
        if (!email.isEmpty()) sb.append("EMAIL:").append(email).append("\r\n");
        sb.append("END:VCARD\r\n");
        return sb.toString();
    }

    /** Wi-Fi Simple Configuration credential for a WPA2-Personal network. */
    static byte[] wifiCredential(String ssid, String pass) {
        ByteArrayOutputStream cred = new ByteArrayOutputStream();
        tlv(cred, 0x1026, new byte[] {0x01});                                 // network index
        tlv(cred, 0x1045, ssid.getBytes(StandardCharsets.UTF_8));             // SSID
        tlv(cred, 0x1003, new byte[] {0x00, 0x20});                           // auth: WPA2-Personal
        tlv(cred, 0x100F, new byte[] {0x00, 0x08});                           // encryption: AES
        tlv(cred, 0x1027, pass.getBytes(StandardCharsets.UTF_8));             // network key
        tlv(cred, 0x1020, hex("FFFFFFFFFFFF"));                               // MAC: any
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        tlv(out, 0x104A, new byte[] {0x10});                                  // version 1.0
        tlv(out, 0x100E, cred.toByteArray());                                 // credential
        return out.toByteArray();
    }

    private static void tlv(ByteArrayOutputStream out, int id, byte[] value) {
        out.write(id >> 8);
        out.write(id);
        out.write(value.length >> 8);
        out.write(value.length);
        out.write(value, 0, value.length);
    }

    // ---- helpers -------------------------------------------------------------

    private static byte[] body(byte[] apdu) {
        if (apdu.length < 5) return new byte[0];
        int lc = apdu[4] & 0xFF;
        if (apdu.length < 5 + lc) return new byte[0];
        return Arrays.copyOfRange(apdu, 5, 5 + lc);
    }

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    static byte[] hex(String s) {
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) out[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        return out;
    }

    static String toHex(byte[] b) {
        if (b == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02X", x));
        return sb.toString();
    }
}
