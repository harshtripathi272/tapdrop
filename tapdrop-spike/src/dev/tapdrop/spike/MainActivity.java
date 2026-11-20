package dev.tapdrop.spike;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.Arrays;

/**
 * Home: the list of things this watch can share. Tap one to open its share screen,
 * hold one to edit or delete it, and use the last row to add a new one.
 *
 * Items can also be set from a computer for testing:
 *   adb shell am start -n dev.tapdrop.spike/.MainActivity --es type contact --es name "..." --es phone "..."
 *   ... --es type wifi --es ssid "..." --es pass "..."
 *   ... --es type upi  --es upi_id "..." --es upi_name "..." --es amount 250
 *   ... --es type link --es label "..." --es url "https://..."
 *   ... --es type location
 *   ... --es pay_page "https://.../pay.html"      (where UPI requests are sent, see Kinds.payLink)
 */
public class MainActivity extends Activity {
    private LinearLayout column;
    private int scrollY = 0;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        takeFields(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        takeFields(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        build();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (column != null) scrollY = ((ScrollView) column.getParent()).getScrollY();
    }

    private void build() {
        column = Ui.column(this);
        column.addView(Ui.header(this, "TAPDROP", 0xFF9A9A9A));
        for (Item item : Store.load(this)) {
            final String id = item.id;
            column.addView(Ui.row(this, Kinds.label(item.type), Kinds.color(item.type), Kinds.summary(item),
                () -> startActivity(new Intent(this, ShareActivity.class).putExtra(ShareActivity.EXTRA_ID, id)),
                () -> startActivity(new Intent(this, EditActivity.class).putExtra(EditActivity.EXTRA_ID, id))));
        }
        column.addView(Ui.row(this, "NEW", 0xFFB0B0B0, "Add something",
            () -> startActivity(new Intent(this, EditActivity.class)), null));
        column.addView(Ui.note(this, "Tap to share\nHold to edit", 0xFF707070));

        final ScrollView scroll = (ScrollView) column.getParent();
        scroll.post(() -> scroll.scrollTo(0, scrollY));
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        if (column != null && Ui.bezelScroll(column, ev)) return true;
        return super.dispatchGenericMotionEvent(ev);
    }

    /** Test hook: creates or replaces one item from intent extras and opens its share screen. */
    private void takeFields(Intent intent) {
        if (intent == null) return;
        String page = intent.getStringExtra(Store.KEY_PAY_PAGE);
        if (page != null) Store.prefs(this).edit().putString(Store.KEY_PAY_PAGE, page.trim()).apply();

        String type = intent.getStringExtra("type");
        if (type == null) return;
        intent.removeExtra("type");
        if (!Arrays.asList(Kinds.ALL).contains(type)) {
            Log.w(NdefTagService.TAG, "unknown type: " + type);
            return;
        }
        Item item = new Item("adb-" + type, type, null);
        for (Kinds.Field field : Kinds.fields(type)) {
            String value = intent.getStringExtra(field.key);
            if (value != null) Kinds.set(item, field, value);
        }
        Store.upsert(this, item);
        Log.d(NdefTagService.TAG, "item set from adb: " + type);
        startActivity(new Intent(this, ShareActivity.class).putExtra(ShareActivity.EXTRA_ID, item.id));
    }
}
