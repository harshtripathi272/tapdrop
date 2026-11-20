package dev.tapdrop.spike;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.text.InputType;

import java.util.Locale;

/**
 * The kinds of item TapDrop can share: what each one is called, which fields it
 * has, how it reads on screen, and which NDEF record a tapping phone receives.
 */
final class Kinds {
    static final String CONTACT = "contact";
    static final String WIFI = "wifi";
    static final String UPI = "upi";
    static final String LOCATION = "location";
    static final String LINK = "link";
    /** A message written to the watch by an NFC tag app, kept and shared as it arrived. */
    static final String CUSTOM = "custom";

    /** Order shown when adding. */
    static final String[] ALL = {CONTACT, WIFI, UPI, LOCATION, LINK, CUSTOM};

    static final int AMOUNT_STEP = 10;
    static final int AMOUNT_MAX = 100000;

    /** A location fix older than this is not shared. */
    static final long LOCATION_MAX_AGE_MS = 10 * 60 * 1000;

    private Kinds() {}

    /** One editable field: storage key, label, example shown when empty, keyboard type, required or not. */
    static final class Field {
        final String key;
        final String label;
        final String example;
        final int inputType;
        final boolean required;

        Field(String key, String label, String example, int inputType, boolean required) {
            this.key = key;
            this.label = label;
            this.example = example;
            this.inputType = inputType;
            this.required = required;
        }
    }

    private static final int TEXT = InputType.TYPE_CLASS_TEXT;
    private static final int NAME = TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_WORDS;
    private static final int EMAIL = TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;

    static Field[] fields(String type) {
        switch (type) {
            case CONTACT:
                return new Field[] {
                    new Field("name", "Name", "Your name", NAME, true),
                    new Field("phone", "Phone", "+91 98765 43210", InputType.TYPE_CLASS_PHONE, false),
                    new Field("email", "Email", "you@example.com", EMAIL, false),
                };
            case WIFI:
                return new Field[] {
                    new Field("ssid", "Network", "Network name", TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS, true),
                    new Field("pass", "Password", "8 or more characters",
                        TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, true),
                };
            case UPI:
                return new Field[] {
                    new Field("upi_id", "UPI ID", "name@bank", EMAIL, true),
                    new Field("upi_name", "Name shown to payer", "Your name", NAME, false),
                    new Field("amount", "Amount in rupees", "Leave empty for any", InputType.TYPE_CLASS_NUMBER, false),
                };
            case LINK:
                return new Field[] {
                    new Field("label", "Title", "Instagram", TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, false),
                    new Field("url", "Link", "instagram.com/you", TEXT | InputType.TYPE_TEXT_VARIATION_URI, true),
                };
            case CUSTOM:
                return new Field[] {       // the content came from the phone; only its name is editable
                    new Field("label", "Title", "Name this tag", TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, false),
                };
            default:
                return new Field[0];       // location has nothing to fill in
        }
    }

    /** Stores one field from typed text. The amount is kept as whole rupees. */
    static void set(Item item, Field field, String raw) {
        String value = raw.trim();
        if (!field.key.equals("amount")) {
            item.put(field.key, value);
            return;
        }
        int amount = 0;
        try {
            if (!value.isEmpty()) amount = Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            // Leave it at 0, which means the payer chooses.
        }
        item.put("amount", Math.max(0, Math.min(AMOUNT_MAX, amount)));
    }

    /** The text to prefill a form field with. */
    static String text(Item item, Field field) {
        if (!field.key.equals("amount")) return item.get(field.key);
        int amount = item.getInt("amount");
        return amount > 0 ? String.valueOf(amount) : "";
    }

    static String name(String type) {
        switch (type) {
            case CONTACT: return "Contact";
            case WIFI: return "Wi-Fi";
            case UPI: return "Pay me";
            case LOCATION: return "Location";
            case CUSTOM: return "Learned tag";
            default: return "Link";
        }
    }

    /** What the kind is called in the list of things to add. */
    static String addName(String type) {
        return type.equals(CUSTOM) ? "Learn from phone" : name(type);
    }

    /** A few words saying what the kind is for, shown when adding. Kept short: rows are narrow. */
    static String blurb(String type) {
        switch (type) {
            case CONTACT: return "Your details";
            case WIFI: return "For guests";
            case UPI: return "UPI request";
            case LOCATION: return "Where you are";
            case CUSTOM: return "Any NFC tag app";
            default: return "Any web link";
        }
    }

    /** What a learned item holds, as {kind, title}. Never null. */
    static String[] learned(Item item) {
        byte[] message = learnedMessage(item);
        String[] what = message == null ? null : Ndef.describe(message);
        return what == null ? new String[] {"Tag", "Tag data"} : what;
    }

    private static byte[] learnedMessage(Item item) {
        try {
            byte[] message = android.util.Base64.decode(item.get("ndef"), android.util.Base64.NO_WRAP);
            return message.length == 0 ? null : message;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static int color(String type) {
        switch (type) {
            // Solid discs with a white icon on top, so each is dark enough to carry white.
            case CONTACT: return 0xFF5B74E8;
            case WIFI: return 0xFF2FA968;
            case UPI: return 0xFFE8922E;
            case LOCATION: return 0xFFE5566A;
            case CUSTOM: return 0xFF2E9AAF;
            default: return 0xFF8E63D9;
        }
    }

    /** The large line: on the share screen and as the row title in the list. */
    static String title(Item item) {
        switch (item.type) {
            case CONTACT: return item.get("name");
            case WIFI: return item.get("ssid");
            case UPI: {
                int amount = item.getInt("amount");
                return amount > 0 ? "₹" + amount : "Any amount";
            }
            case LOCATION: return "My location";
            case CUSTOM: return item.get("label").isEmpty() ? learned(item)[1] : item.get("label");
            default: {
                String label = item.get("label");
                if (!label.isEmpty()) return label;
                String host = Uri.parse(link(item)).getHost();
                return host == null ? "Link" : host;
            }
        }
    }

    /** The second line on the share screen. {@code fix} is only used for a location item. */
    static String subtitle(Item item, Location fix) {
        switch (item.type) {
            case CONTACT: return item.get("phone").isEmpty() ? item.get("email") : item.get("phone");
            case WIFI: return "Wi-Fi network";
            case UPI: return "to " + (item.get("upi_name").isEmpty() ? item.get("upi_id") : item.get("upi_name"));
            case LOCATION:
                return usable(fix)
                    ? String.format(Locale.US, "%.5f, %.5f", fix.getLatitude(), fix.getLongitude())
                    : "Finding you…";
            case CUSTOM: return learned(item)[0];
            default: return bare(link(item));
        }
    }

    /** The second line of a row in the home list. */
    static String rowSubtitle(Item item) {
        switch (item.type) {
            case WIFI: return "Wi-Fi";
            case UPI: return item.get("upi_id");
            case LOCATION: return "Live position";
            default: return subtitle(item, null);
        }
    }

    static boolean usable(Location fix) {
        return fix != null && System.currentTimeMillis() - fix.getTime() < LOCATION_MAX_AGE_MS;
    }

    /** The NDEF message for an item, or null when there is nothing to send yet. */
    static byte[] record(Context context, Item item, Location fix) {
        switch (item.type) {
            case CONTACT:
                return Ndef.vcard(item.get("name"), item.get("phone"), item.get("email"));
            case WIFI:
                return Ndef.wifi(item.get("ssid"), item.get("pass"));
            case UPI:
                return Ndef.uri(payLink(item, Store.payPage(context)));
            case LOCATION:
                if (!usable(fix)) return null;
                // A maps web link opens the Maps app on Android and works on iPhone too.
                return Ndef.uri(String.format(Locale.US, "https://www.google.com/maps?q=%.6f,%.6f",
                    fix.getLatitude(), fix.getLongitude()));
            case CUSTOM:
                return learnedMessage(item);     // shared byte for byte as the phone wrote it
            default:
                return Ndef.uri(link(item));
        }
    }

    private static String link(Item item) {
        String url = item.get("url").trim();
        return url.contains("://") || url.startsWith("tel:") || url.startsWith("mailto:") ? url : "https://" + url;
    }

    /** A link without its scheme or a trailing slash, for display. */
    private static String bare(String url) {
        String s = url.replaceFirst("^https?://(www\\.)?", "");
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    /**
     * A UPI payment request. Nothing moves until the payer approves it in their own app.
     *
     * A bare upi:// link in a tag reaches no UPI app: phones dispatch tags with an NFC
     * action that GPay, PhonePe and Paytm do not listen for. So when a pay page is set,
     * the tag carries a web link to it with the request after "#", and the page hands
     * off to whichever UPI app the payer picks.
     */
    static String payLink(Item item, String payPage) {
        StringBuilder query = new StringBuilder("pa=").append(Uri.encode(item.get("upi_id"), "@.-_"));
        if (!item.get("upi_name").isEmpty()) query.append("&pn=").append(Uri.encode(item.get("upi_name")));
        int amount = item.getInt("amount");
        if (amount > 0) query.append("&am=").append(amount).append(".00");
        query.append("&cu=INR");
        return payPage.isEmpty() ? "upi://pay?" + query : payPage + "#" + query;
    }
}
