package dev.tapdrop.spike;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds a new item or edits an existing one.
 * Opened with an item id it edits that item. Opened with a kind it shows that
 * kind's empty form. Opened with neither it first asks which kind to add.
 */
public class EditActivity extends Activity {
    static final String EXTRA_ID = "id";
    static final String EXTRA_KIND = "kind";
    private static final String UPI_ID_PATTERN = "^[A-Za-z0-9._-]{2,256}@[A-Za-z0-9._-]{2,64}$";
    private static final int ASK_WIFI = 2;

    private LinearLayout column;
    private Item existing;
    private String type;
    private final List<EditText> inputs = new ArrayList<>();
    private TextView message;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        existing = Store.find(this, getIntent().getStringExtra(EXTRA_ID));
        String kind = getIntent().getStringExtra(EXTRA_KIND);
        if (existing != null) showForm(existing.type);
        else if (kind != null) pick(kind);
        else showPicker();
    }

    private void showPicker() {
        column = Ui.column(this);
        column.addView(Ui.title(this, "Add"));
        LinearLayout group = Ui.group(this);
        for (final String kind : Kinds.ALL) {
            Ui.addRow(group, Ui.row(this, kind, Kinds.color(kind), Kinds.addName(kind), Kinds.blurb(kind),
                () -> pick(kind), null));
        }
        column.addView(group);
        Ui.stagger(group);
    }

    @Override
    public void finish() {
        super.finish();
        Ui.leave(this);
    }

    private void pick(String kind) {
        if (kind.equals(Kinds.CUSTOM)) {            // filled in by a phone, not by a form
            Ui.go(this, new Intent(this, LearnActivity.class));
            super.finish();
            return;
        }
        if (Kinds.fields(kind).length == 0) {       // nothing to fill in, such as a location
            Store.upsert(this, new Item(Store.newId(), kind, null));
            finish();
            return;
        }
        showForm(kind);
    }

    private void showForm(String kind) {
        type = kind;
        inputs.clear();
        column = Ui.column(this);
        column.addView(Ui.heading(this, kind, Kinds.color(kind), Kinds.name(kind)));

        if (kind.equals(Kinds.WIFI)) {
            column.addView(Ui.secondaryButton(this, "Use current Wi-Fi", Ui.ACCENT, this::fillCurrentWifi));
        }

        Kinds.Field[] fields = Kinds.fields(kind);
        for (Kinds.Field field : fields) {
            EditText input = new EditText(this);
            input.setHint(field.example);
            input.setInputType(field.inputType);
            if (existing != null) input.setText(Kinds.text(existing, field));
            column.addView(Ui.field(this, field.label, input));
            inputs.add(input);
        }

        message = Ui.body(this, "", Ui.TEXT_SECONDARY);
        message.setVisibility(View.GONE);
        column.addView(message);

        if (fields.length > 0) {
            column.addView(Ui.primaryButton(this, "Save", this::save));
        } else {
            say("Nothing to set. It shares where the watch is when you tap.", false);
        }
        if (existing != null) {
            // The quick-tap item is what goes out when someone taps while TapDrop is closed.
            if (existing.id.equals(Store.quick(this))) {
                column.addView(Ui.body(this,
                    "Quick tap: this is shared when someone taps while TapDrop is closed.", Ui.TEXT_SECONDARY));
            } else {
                column.addView(Ui.secondaryButton(this, "Use for quick tap", Ui.ACCENT, () -> {
                    Store.setQuick(this, existing.id);
                    finish();
                }));
            }
            column.addView(Ui.textButton(this, "Delete", Ui.DANGER, () -> {
                Store.delete(this, existing.id);
                finish();
            }));
        }

        // A new Wi-Fi item starts with the connected network when the watch may already read it.
        if (kind.equals(Kinds.WIFI) && existing == null
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            String ssid = currentSsid();
            if (ssid != null) inputs.get(0).setText(ssid);
        }
    }

    // ---- Wi-Fi ---------------------------------------------------------------

    private void fillCurrentWifi() {
        // Android only reveals the network name to apps that hold location permission.
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, ASK_WIFI);
            return;
        }
        String ssid = currentSsid();
        if (ssid == null) {
            say("This watch isn't on Wi-Fi right now. It may be using the phone's connection.", true);
            return;
        }
        inputs.get(0).setText(ssid);
        say("Type the password once. Android doesn't let apps read saved passwords.", false);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        if (code != ASK_WIFI) return;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) fillCurrentWifi();
        else say("Without location access the watch can't read the network name. Type it instead.", true);
    }

    /** The name of the Wi-Fi network the watch is connected to, or null. */
    @SuppressWarnings("deprecation")
    private String currentSsid() {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo info = wifi == null ? null : wifi.getConnectionInfo();
        String ssid = info == null ? null : info.getSSID();
        if (ssid == null || ssid.isEmpty() || ssid.equals(WifiManager.UNKNOWN_SSID)) return null;
        if (ssid.length() >= 2 && ssid.startsWith("\"") && ssid.endsWith("\"")) ssid = ssid.substring(1, ssid.length() - 1);
        return ssid;
    }

    // ---- saving --------------------------------------------------------------

    private void save() {
        Kinds.Field[] fields = Kinds.fields(type);
        Item item = existing != null ? existing : new Item(Store.newId(), type, null);
        for (int i = 0; i < fields.length; i++) {
            String raw = inputs.get(i).getText().toString().trim();
            if (fields[i].required && raw.isEmpty()) {
                say(fields[i].label + " is needed", true);
                return;
            }
            Kinds.set(item, fields[i], raw);
        }
        if (type.equals(Kinds.UPI) && !item.get("upi_id").matches(UPI_ID_PATTERN)) {
            say("A UPI ID looks like name@bank", true);
            return;
        }
        if (type.equals(Kinds.WIFI) && item.get("pass").length() < 8) {
            say("The password needs 8 or more characters", true);
            return;
        }
        Store.upsert(this, item);
        finish();
    }

    private void say(String text, boolean problem) {
        message.setText(text);
        message.setTextColor(problem ? Ui.DANGER : Ui.TEXT_SECONDARY);
        message.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        if (column != null && Ui.bezelScroll(column, ev)) return true;
        return super.dispatchGenericMotionEvent(ev);
    }
}
