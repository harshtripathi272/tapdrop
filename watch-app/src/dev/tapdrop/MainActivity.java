package dev.tapdrop;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.Arrays;
import java.util.List;

/**
 * Home: the list of things this watch can share. Tap one to open its share screen,
 * hold one to edit it, and use the last row to add another. A fresh install has no
 * items, so the list starts as a short menu of what can be added.
 *
 * Items can also be set from a computer:
 *   adb shell am start -n dev.tapdrop/.MainActivity --es type contact --es name "..." --es phone "..."
 *   ... --es type wifi --es ssid "..." --es pass "..."
 *   ... --es type upi  --es upi_id "..." --es upi_name "..." --es amount 250
 *   ... --es type link --es label "..." --es url "https://..."
 *   ... --es type location
 *   ... --es pay_page "https://.../pay.html"      (where UPI requests are sent, see Kinds.payLink)
 */
public class MainActivity extends Activity {
    private LinearLayout column;
    private int scrollY = 0;
    private boolean firstBuild = true;

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
        column.addView(Ui.title(this, "TapDrop"));

        // The user's own items first.
        List<Item> items = Store.load(this);
        LinearLayout mine = Ui.group(this);
        if (items.isEmpty()) {
            column.addView(Ui.body(this, "Choose what to share from your wrist.", Ui.TEXT_SECONDARY));
        } else {
            String quick = Store.quick(this);
            for (Item item : items) {
                final String id = item.id;
                // With several items, mark the one a tap gets while TapDrop is closed.
                String detail = items.size() > 1 && id.equals(quick)
                    ? "Quick tap · " + Kinds.rowSubtitle(item) : Kinds.rowSubtitle(item);
                Ui.addRow(mine, Ui.row(this, item.type, Kinds.color(item.type), Kinds.title(item), detail,
                    () -> Ui.go(this, new Intent(this, ShareActivity.class).putExtra(ShareActivity.EXTRA_ID, id)),
                    () -> Ui.go(this, new Intent(this, EditActivity.class).putExtra(EditActivity.EXTRA_ID, id))));
            }
            column.addView(mine);
            column.addView(Ui.section(this, "Add another"));
        }

        // Every kind that can be added is always on this screen. Behind an "Add" button
        // they looked as if they had been removed.
        LinearLayout kinds = Ui.group(this);
        for (final String kind : Kinds.ALL) {
            Ui.addRow(kinds, Ui.row(this, kind, Kinds.color(kind), Kinds.addName(kind), Kinds.blurb(kind),
                () -> Ui.go(this, new Intent(this, EditActivity.class).putExtra(EditActivity.EXTRA_KIND, kind)),
                null));
        }
        column.addView(kinds);

        // What goes out when a phone taps while TapDrop is closed, and how.
        Item quickItem = Store.find(this, Store.quick(this));
        column.addView(Ui.section(this, "When TapDrop is closed"));
        column.addView(Ui.card(this, Icons.CHECK, Ui.NEUTRAL, "Quick tap",
            quickItem == null ? "Shares nothing" : "Shares " + Kinds.title(quickItem),
            () -> Ui.go(this, new Intent(this, OneTapActivity.class))));

        if (firstBuild) {
            firstBuild = false;
            Ui.stagger(items.isEmpty() ? kinds : mine);     // rows arrive one after another, once per launch
        }
        final ScrollView scroll = (ScrollView) column.getParent();
        scroll.post(() -> scroll.scrollTo(0, scrollY));
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        if (column != null && Ui.bezelScroll(column, ev)) return true;
        return super.dispatchGenericMotionEvent(ev);
    }

    /** Creates or replaces one item from intent extras and opens its share screen. */
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
        // One item per kind by default. Pass --es id <name> to keep several of a kind, such as two links.
        String name = intent.getStringExtra("id");
        Item item = new Item("adb-" + (name == null ? type : name), type, null);
        for (Kinds.Field field : Kinds.fields(type)) {
            String value = intent.getStringExtra(field.key);
            if (value != null) Kinds.set(item, field, value);
        }
        Store.upsert(this, item);
        Log.d(NdefTagService.TAG, "item set from adb: " + type);
        if (!intent.getBooleanExtra("quiet", false)) {
            Ui.go(this, new Intent(this, ShareActivity.class).putExtra(ShareActivity.EXTRA_ID, item.id));
        }
    }
}
