package dev.tapdrop.spike;

import android.content.SharedPreferences;
import android.net.Uri;

/**
 * The four things the watch can offer, and where their fields live in preferences.
 * The bezel moves between slots; the selected slot is what a tapping phone receives.
 */
final class Slots {
    static final String PREFS = "tapdrop";
    static final String KEY_SLOT = "slot";

    static final String[] TYPES = {"contact", "wifi", "upi", "link"};
    static final String[] LABELS = {"CONTACT", "WI-FI", "PAY ME", "LINK"};
    static final int[] COLORS = {0xFF4FC3F7, 0xFF81C784, 0xFFFFB74D, 0xFFBA68C8};
    static final int UPI = 2;

    static final String KEY_NAME = "name";
    static final String KEY_PHONE = "phone";
    static final String KEY_EMAIL = "email";
    static final String KEY_MIME = "mime";          // override for the vCard MIME type
    static final String KEY_SSID = "ssid";
    static final String KEY_PASS = "pass";
    static final String KEY_UPI_ID = "upi_id";
    static final String KEY_UPI_NAME = "upi_name";
    static final String KEY_AMOUNT = "amount";      // whole rupees, 0 means payer decides
    static final String KEY_URL = "url";
    static final String KEY_PAY_PAGE = "pay_page";  // web page that hands a request to any UPI app

    /** String fields that can be set from the laptop with --es extras. */
    static final String[] STRING_KEYS = {
        KEY_NAME, KEY_PHONE, KEY_EMAIL, KEY_MIME, KEY_SSID, KEY_PASS, KEY_UPI_ID, KEY_UPI_NAME, KEY_URL,
        KEY_PAY_PAGE
    };

    // Placeholder data so every slot works before anything is configured.
    static final String DEMO_NAME = "TapDrop Test";
    static final String DEMO_PHONE = "+15550100";
    static final String DEMO_SSID = "TapDrop-Test";
    static final String DEMO_PASS = "tapdrop1234";
    static final String DEMO_UPI_ID = "tapdrop.test@upi";
    static final String DEMO_URL = "https://example.com/?tapdrop=it-works";

    static final int AMOUNT_STEP = 10;
    static final int AMOUNT_MAX = 100000;

    private Slots() {}

    static int index(SharedPreferences p) {
        int i = p.getInt(KEY_SLOT, 0);
        return (i < 0 || i >= TYPES.length) ? 0 : i;
    }

    /** Accepts the names used by the first test build as well. */
    static int indexOf(String type) {
        if ("vcard".equals(type)) return 0;
        if ("url".equals(type)) return 3;
        for (int i = 0; i < TYPES.length; i++) if (TYPES[i].equals(type)) return i;
        return -1;
    }

    static String title(SharedPreferences p, int i) {
        switch (i) {
            case 0: return p.getString(KEY_NAME, DEMO_NAME);
            case 1: return p.getString(KEY_SSID, DEMO_SSID);
            case 2: {
                int amount = p.getInt(KEY_AMOUNT, 0);
                return amount > 0 ? "₹" + amount : "Any amount";
            }
            default: {
                String host = Uri.parse(p.getString(KEY_URL, DEMO_URL)).getHost();
                return host == null ? "Link" : host;
            }
        }
    }

    static String subtitle(SharedPreferences p, int i) {
        switch (i) {
            case 0: return p.getString(KEY_PHONE, DEMO_PHONE);
            case 1: return "Tap to join";
            case 2: return "to " + p.getString(KEY_UPI_NAME, p.getString(KEY_UPI_ID, DEMO_UPI_ID));
            default: return p.getString(KEY_URL, DEMO_URL);
        }
    }

    /**
     * A UPI payment request. Nothing moves until the payer approves it in their own app.
     *
     * A bare upi:// link in a tag reaches no UPI app: phones dispatch tags with an NFC
     * action that GPay, PhonePe and Paytm do not listen for. So when a pay page is set,
     * the tag carries a web link to it with the request after "#", and the page hands
     * off to whichever UPI app the payer picks.
     */
    static String upiLink(SharedPreferences p) {
        String id = p.getString(KEY_UPI_ID, DEMO_UPI_ID);
        StringBuilder query = new StringBuilder("pa=").append(Uri.encode(id, "@.-_"));
        query.append("&pn=").append(Uri.encode(p.getString(KEY_UPI_NAME, DEMO_NAME)));
        int amount = p.getInt(KEY_AMOUNT, 0);
        if (amount > 0) query.append("&am=").append(amount).append(".00");
        query.append("&cu=INR");
        String page = p.getString(KEY_PAY_PAGE, "");
        return page.isEmpty() ? "upi://pay?" + query : page + "#" + query;
    }
}
