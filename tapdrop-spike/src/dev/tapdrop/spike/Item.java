package dev.tapdrop.spike;

import org.json.JSONException;
import org.json.JSONObject;

/** One thing the watch can share: a type from {@link Kinds} plus its fields. */
final class Item {
    final String id;
    final String type;
    private final JSONObject fields;

    Item(String id, String type, JSONObject fields) {
        this.id = id;
        this.type = type;
        this.fields = fields == null ? new JSONObject() : fields;
    }

    String get(String key) {
        return fields.optString(key, "");
    }

    int getInt(String key) {
        return fields.optInt(key, 0);
    }

    void put(String key, Object value) {
        try {
            fields.put(key, value);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    JSONObject toJson() {
        try {
            return new JSONObject().put("id", id).put("type", type).put("fields", fields);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    static Item fromJson(JSONObject o) {
        return new Item(o.optString("id"), o.optString("type"), o.optJSONObject("fields"));
    }
}
