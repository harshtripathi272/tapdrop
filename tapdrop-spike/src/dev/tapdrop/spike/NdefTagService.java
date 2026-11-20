package dev.tapdrop.spike;

import android.content.Context;
import android.location.Location;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import java.util.Arrays;

/**
 * Emulates an NFC Forum Type 4 tag that holds one NDEF record.
 * A reader selects the NDEF application, reads the capability container (E103),
 * then reads the NDEF file (E104). Read-only: writes are refused.
 *
 * The record comes from the item whose share screen is open. With no share
 * screen open the tag does not answer, so nothing leaks from the home list.
 */
public class NdefTagService extends HostApduService {
    static final String TAG = "TapDrop";

    /** Count of completed reads. The share screen polls it to flash "Sent". */
    static volatile int reads = 0;

    /** Latest location fix, written by the share screen of a location item. */
    static volatile Location fix = null;

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
                selected = null;
                buzzed = false;
                Item item = Store.find(this, Store.active(this));
                byte[] record = item == null ? null : Kinds.record(this, item, fix);
                if (record == null) {
                    Log.d(TAG, item == null ? "no share screen open, not answering" : "nothing to send yet for " + item.type);
                    ndefFile = new byte[0];
                    return NOT_FOUND;
                }
                if (record.length + 2 > MAX_NDEF_FILE) {
                    Log.w(TAG, "record too large: " + record.length);
                    ndefFile = new byte[0];
                    return NOT_FOUND;
                }
                ndefFile = new byte[2 + record.length];
                ndefFile[0] = (byte) (record.length >> 8);
                ndefFile[1] = (byte) record.length;
                System.arraycopy(record, 0, ndefFile, 2, record.length);
                Log.d(TAG, "serving type=" + item.type + " bytes=" + ndefFile.length);
                return OK;
            }
            if (ndefFile.length > 0 && data.length == 2 && (data[0] & 0xFF) == 0xE1) {   // by file id
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

    private static byte[] body(byte[] apdu) {
        if (apdu.length < 5) return new byte[0];
        int lc = apdu[4] & 0xFF;
        if (apdu.length < 5 + lc) return new byte[0];
        return Arrays.copyOfRange(apdu, 5, 5 + lc);
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
