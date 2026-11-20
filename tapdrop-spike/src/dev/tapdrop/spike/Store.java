package dev.tapdrop.spike;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/** The user's items, kept as one JSON array in app-private preferences. */
final class Store {
    private static final String PREFS = "tapdrop";
    private static final String KEY_ITEMS = "items";
    private static final String KEY_ACTIVE = "active";
    static final String KEY_PAY_PAGE = "pay_page";

    private Store() {}

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static List<Item> load(Context c) {
        String raw = prefs(c).getString(KEY_ITEMS, null);
        if (raw == null) {
            List<Item> samples = samples();
            save(c, samples);
            return samples;
        }
        List<Item> items = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) items.add(Item.fromJson(array.getJSONObject(i)));
        } catch (JSONException e) {
            // Unreadable data: start over with the samples instead of crashing on every launch.
            return samples();
        }
        return items;
    }

    static void save(Context c, List<Item> items) {
        JSONArray array = new JSONArray();
        for (Item item : items) array.put(item.toJson());
        prefs(c).edit().putString(KEY_ITEMS, array.toString()).apply();
    }

    static Item find(Context c, String id) {
        if (id == null) return null;
        for (Item item : load(c)) if (item.id.equals(id)) return item;
        return null;
    }

    /** Replaces the item with the same id, or appends it. */
    static void upsert(Context c, Item item) {
        List<Item> items = load(c);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id.equals(item.id)) {
                items.set(i, item);
                save(c, items);
                return;
            }
        }
        items.add(item);
        save(c, items);
    }

    static void delete(Context c, String id) {
        List<Item> items = load(c);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id.equals(id)) {
                items.remove(i);
                break;
            }
        }
        save(c, items);
    }

    static String newId() {
        return Long.toString(System.currentTimeMillis(), 36);
    }

    /** The item a tapping phone receives. Null when no share screen is open. */
    static String active(Context c) {
        return prefs(c).getString(KEY_ACTIVE, null);
    }

    static void setActive(Context c, String id) {
        // commit, not apply: the NFC service may read this from another thread right away.
        if (id == null) prefs(c).edit().remove(KEY_ACTIVE).commit();
        else prefs(c).edit().putString(KEY_ACTIVE, id).commit();
    }

    /** Web page that hands a UPI request to the payer's app. Empty until one is hosted. */
    static String payPage(Context c) {
        return prefs(c).getString(KEY_PAY_PAGE, "");
    }

    /** Placeholder items for a fresh install, so the list is never empty. */
    private static List<Item> samples() {
        List<Item> items = new ArrayList<>();

        Item contact = new Item("sample-contact", Kinds.CONTACT, null);
        contact.put("name", "TapDrop Test");
        contact.put("phone", "+15550100");
        items.add(contact);

        Item wifi = new Item("sample-wifi", Kinds.WIFI, null);
        wifi.put("ssid", "TapDrop-Test");
        wifi.put("pass", "tapdrop1234");
        items.add(wifi);

        Item pay = new Item("sample-pay", Kinds.UPI, null);
        pay.put("upi_id", "tapdrop.test@upi");
        pay.put("upi_name", "TapDrop Test");
        pay.put("amount", 0);
        items.add(pay);

        items.add(new Item("sample-location", Kinds.LOCATION, null));

        Item link = new Item("sample-link", Kinds.LINK, null);
        link.put("label", "Example");
        link.put("url", "https://example.com/?tapdrop=it-works");
        items.add(link);

        return items;
    }
}
