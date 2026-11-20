package dev.tapdrop.spike;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.text.InputType;

import java.util.Locale;

/**
 * The kinds of item TapDrop can share: how each one is labelled, which fields it
 * has, how it reads on screen, and which NDEF record a tapping phone receives.
 */
final class Kinds {
    static final String CONTACT = "contact";
    static final String WIFI = "wifi";
    static final String UPI = "upi";
    static final String LOCATION = "location";
    static final String LINK = "link";

    /** Order shown in the "add" picker. */
    static final String[] ALL = {CONTACT, WIFI, UPI, LOCATION, LINK};

    static final int AMOUNT_STEP = 10;
    static final int AMOUNT_MAX = 100000;

    /** A location fix older than this is not shared. */
    static final long LOCATION_MAX_AGE_MS = 10 * 60 * 1000;

    private Kinds() {}

    /** One editable field: storage key, hint shown in the form, keyboard type, required or not. */
    static final class Field {
        final String key;
        final String hint;
        final int inputType;
        final boolean required;

        Field(String key, String hint, int inputType, boolean required) {
            this.key = key;
            this.hint = hint;
            this.inputType = inputType;
            this.required = required;
        }
    }

    private static final int TEXT = InputType.TYPE_CLASS_TEXT;

    static Field[] fields(String type) {
        switch (type) {
            case CONTACT:
                return new Field[] {
                    new Field("name", "Name", TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME
                        | InputType.TYPE_TEXT_FLAG_CAP_WORDS, true),
                    new Field("phone", "Phone", InputType.TYPE_CLASS_PHONE, false),
                    new Field("email", "Email", TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, false),
                };
            case WIFI:
                return new Field[] {
                    new Field("ssid", "Network name", TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS, true),
                    new Field("pass", "Password", TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, true),
                };
            case UPI:
                return new Field[] {
                    new Field("upi_id", "UPI ID (name@bank)", TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, true),
                    new Field("upi_name", "Your name", TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME
                        | InputType.TYPE_TEXT_FLAG_CAP_WORDS, false),
                    new Field("amount", "Amount, 0 for any", InputType.TYPE_CLASS_NUMBER, false),
                };
            case LINK:
                return new Field[] {
                    new Field("label", "Title", TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, false),
                    new Field("url", "Link", TEXT | InputType.TYPE_TEXT_VARIATION_URI, true),
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

    static String label(String type) {
        switch (type) {
            case CONTACT: return "CONTACT";
            case WIFI: return "WI-FI";
            case UPI: return "PAY ME";
            case LOCATION: return "LOCATION";
            default: return "LINK";
        }
    }

    /** Wording for the "add" picker. */
    static String addLabel(String type) {
        switch (type) {
            case CONTACT: return "Contact card";
            case WIFI: return "Wi-Fi network";
            case UPI: return "Payment request";
            case LOCATION: return "My location";
            default: return "Link";
        }
    }

    static int color(String type) {
        switch (type) {
            case CONTACT: return 0xFF4FC3F7;
            case WIFI: return 0xFF81C784;
            case UPI: return 0xFFFFB74D;
            case LOCATION: return 0xFFF06292;
            default: return 0xFFBA68C8;
        }
    }

    static String title(Item item) {
        switch (item.type) {
            case CONTACT: return item.get("name");
            case WIFI: return item.get("ssid");
            case UPI: {
                int amount = item.getInt("amount");
                return amount > 0 ? "₹" + amount : "Any amount";
            }
            case LOCATION: return "My location";
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
            case WIFI: return "Tap to join";
            case UPI: return "to " + (item.get("upi_name").isEmpty() ? item.get("upi_id") : item.get("upi_name"));
            case LOCATION:
                return usable(fix)
                    ? String.format(Locale.US, "%.5f, %.5f", fix.getLatitude(), fix.getLongitude())
                    : "Finding you…";
            default: return link(item);
        }
    }

    /** One line for the home list, under the type label. */
    static String summary(Item item) {
        return item.type.equals(UPI) ? title(item) + " " + subtitle(item, null) : title(item);
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
            default:
                return Ndef.uri(link(item));
        }
    }

    private static String link(Item item) {
        String url = item.get("url").trim();
        return url.contains("://") || url.startsWith("tel:") || url.startsWith("mailto:") ? url : "https://" + url;
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
