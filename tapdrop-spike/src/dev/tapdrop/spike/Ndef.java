package dev.tapdrop.spike;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Builders for the NDEF records TapDrop serves. Each returns a complete one-record message. */
final class Ndef {
    private Ndef() {}

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

    static byte[] mime(String mimeType, byte[] payload) {
        return record(0x02, mimeType.getBytes(StandardCharsets.US_ASCII), payload);
    }

    static byte[] uri(String url) {
        // NDEF URI prefix codes 1 to 4. The www forms come first so the longest prefix wins.
        // Anything else is sent whole with code 0.
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
        return record(0x01, new byte[] {'U'}, payload);
    }

    static byte[] vcard(String name, String phone, String email) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCARD\r\nVERSION:3.0\r\n");
        sb.append("FN:").append(name).append("\r\n");
        sb.append("N:").append(name).append(";;;;\r\n");
        if (!phone.isEmpty()) sb.append("TEL;TYPE=CELL:").append(phone).append("\r\n");
        if (!email.isEmpty()) sb.append("EMAIL:").append(email).append("\r\n");
        sb.append("END:VCARD\r\n");
        return mime("text/vcard", sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Wi-Fi Simple Configuration credential for a WPA2-Personal network. */
    static byte[] wifi(String ssid, String pass) {
        ByteArrayOutputStream cred = new ByteArrayOutputStream();
        tlv(cred, 0x1026, new byte[] {0x01});                                 // network index
        tlv(cred, 0x1045, ssid.getBytes(StandardCharsets.UTF_8));             // SSID
        tlv(cred, 0x1003, new byte[] {0x00, 0x20});                           // auth: WPA2-Personal
        tlv(cred, 0x100F, new byte[] {0x00, 0x08});                           // encryption: AES
        tlv(cred, 0x1027, pass.getBytes(StandardCharsets.UTF_8));             // network key
        tlv(cred, 0x1020, new byte[] {-1, -1, -1, -1, -1, -1});               // MAC: any
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        tlv(out, 0x104A, new byte[] {0x10});                                  // version 1.0
        tlv(out, 0x100E, cred.toByteArray());                                 // credential
        return mime("application/vnd.wfa.wsc", out.toByteArray());
    }

    private static void tlv(ByteArrayOutputStream out, int id, byte[] value) {
        out.write(id >> 8);
        out.write(id);
        out.write(value.length >> 8);
        out.write(value.length);
        out.write(value, 0, value.length);
    }
}
