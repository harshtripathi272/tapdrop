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

    // ---- reading ----------------------------------------------------------------

    /**
     * Says what a message written by another app holds, as {kind, title}: for example
     * {"Link", "instagram.com/asha"} or {"Wi-Fi", "Home 5G"}. A message with several
     * records is described by its first, with "+1" added to the kind. Returns null
     * when the bytes are not a well-formed NDEF message.
     */
    static String[] describe(byte[] message) {
        try {
            int pos = 0;
            int count = 0;
            String[] first = null;
            while (pos < message.length) {
                int header = message[pos++] & 0xFF;
                int typeLen = message[pos++] & 0xFF;
                int payloadLen;
                if ((header & 0x10) != 0) {                 // short record
                    payloadLen = message[pos++] & 0xFF;
                } else {
                    payloadLen = ((message[pos] & 0xFF) << 24) | ((message[pos + 1] & 0xFF) << 16)
                        | ((message[pos + 2] & 0xFF) << 8) | (message[pos + 3] & 0xFF);
                    pos += 4;
                }
                int idLen = (header & 0x08) != 0 ? (message[pos++] & 0xFF) : 0;
                if (payloadLen < 0 || pos + typeLen + idLen + payloadLen > message.length) return null;
                String type = new String(message, pos, typeLen, StandardCharsets.US_ASCII);
                pos += typeLen + idLen;
                if (count == 0) first = describeRecord(header & 0x07, type, message, pos, payloadLen);
                pos += payloadLen;
                count++;
                if ((header & 0x40) != 0) break;            // message end
            }
            if (first == null) return null;
            if (count > 1) first[0] = first[0] + " +" + (count - 1);
            return first;
        } catch (RuntimeException e) {
            return null;                                    // ran off the end: not a valid message
        }
    }

    private static String[] describeRecord(int tnf, String type, byte[] m, int offset, int length) {
        if (tnf == 0x01 && type.equals("U") && length >= 1) {
            String[] prefixes = {"", "http://www.", "https://www.", "http://", "https://", "tel:", "mailto:"};
            int code = m[offset] & 0xFF;
            String uri = (code < prefixes.length ? prefixes[code] : "")
                + new String(m, offset + 1, length - 1, StandardCharsets.UTF_8);
            if (uri.startsWith("tel:")) return new String[] {"Phone", uri.substring(4)};
            if (uri.startsWith("mailto:")) return new String[] {"Email", uri.substring(7)};
            if (uri.startsWith("sms:")) return new String[] {"Message", uri.substring(4)};
            if (uri.startsWith("geo:")) return new String[] {"Place", uri.substring(4)};
            String shown = uri.replaceFirst("^https?://(www\\.)?", "");
            return new String[] {"Link", shown.endsWith("/") ? shown.substring(0, shown.length() - 1) : shown};
        }
        if (tnf == 0x01 && type.equals("T") && length >= 1) {
            int language = m[offset] & 0x3F;
            return new String[] {"Text",
                new String(m, offset + 1 + language, length - 1 - language, StandardCharsets.UTF_8)};
        }
        if (tnf == 0x01 && type.equals("Sp")) return new String[] {"Link", "Smart poster"};
        if (tnf == 0x02) {
            String mime = type.toLowerCase(java.util.Locale.US);
            if (mime.contains("vcard")) {
                String card = new String(m, offset, length, StandardCharsets.UTF_8);
                java.util.regex.Matcher name = java.util.regex.Pattern.compile("(?m)^FN[^:]*:(.+)$").matcher(card);
                return new String[] {"Contact", name.find() ? name.group(1).trim() : "Contact card"};
            }
            if (mime.equals("application/vnd.wfa.wsc")) {
                // The network name is the value of field 0x1045, wherever it is nested.
                for (int i = offset; i + 4 <= offset + length; i++) {
                    if ((m[i] & 0xFF) == 0x10 && (m[i + 1] & 0xFF) == 0x45) {
                        int n = ((m[i + 2] & 0xFF) << 8) | (m[i + 3] & 0xFF);
                        if (n > 0 && i + 4 + n <= offset + length) {
                            return new String[] {"Wi-Fi", new String(m, i + 4, n, StandardCharsets.UTF_8)};
                        }
                    }
                }
                return new String[] {"Wi-Fi", "Wi-Fi network"};
            }
            if (mime.contains("bluetooth")) return new String[] {"Bluetooth", "Bluetooth device"};
            return new String[] {"Data", type};
        }
        if (tnf == 0x04 && type.equals("android.com:pkg")) {
            return new String[] {"App", new String(m, offset, length, StandardCharsets.UTF_8)};
        }
        return new String[] {"Tag", type.isEmpty() ? "Tag data" : type};
    }

    private static void tlv(ByteArrayOutputStream out, int id, byte[] value) {
        out.write(id >> 8);
        out.write(id);
        out.write(value.length >> 8);
        out.write(value.length);
        out.write(value, 0, value.length);
    }
}
