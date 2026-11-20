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

    /** The user's items. A fresh install has none: nothing is shared until the user adds it. */
    static List<Item> load(Context c) {
        List<Item> items = new ArrayList<>();
        String raw = prefs(c).getString(KEY_ITEMS, null);
        if (raw == null) return items;
        boolean droppedSamples = false;
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                Item item = Item.fromJson(array.getJSONObject(i));
                // Earlier builds seeded placeholder items. They were mistaken for real data.
                if (item.id.startsWith("sample-")) droppedSamples = true;
                else items.add(item);
            }
        } catch (JSONException e) {
            // Unreadable data: start empty instead of crashing on every launch.
            return new ArrayList<>();
        }
        if (droppedSamples) save(c, items);
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

    /** True only while the Learn screen is open. The tag accepts writes at no other time. */
    private static final String KEY_LEARNING = "learning";

    static boolean learning(Context c) {
        return prefs(c).getBoolean(KEY_LEARNING, false);
    }

    static void setLearning(Context c, boolean on) {
        // commit, not apply: the NFC service may read this from another thread right away.
        prefs(c).edit().putBoolean(KEY_LEARNING, on).commit();
    }

    /** Web page that hands a UPI request to the payer's app. Empty until one is hosted. */
    static String payPage(Context c) {
        return prefs(c).getString(KEY_PAY_PAGE, "");
    }
}
