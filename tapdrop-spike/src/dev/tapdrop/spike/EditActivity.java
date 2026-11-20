package dev.tapdrop.spike;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds a new item or edits an existing one. Opened without an id it first asks
 * which kind of item to add, then shows that kind's form.
 */
public class EditActivity extends Activity {
    static final String EXTRA_ID = "id";
    private static final String UPI_ID_PATTERN = "^[A-Za-z0-9._-]{2,256}@[A-Za-z0-9._-]{2,64}$";

    private LinearLayout column;
    private Item existing;
    private String type;
    private final List<EditText> inputs = new ArrayList<>();
    private TextView error;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        existing = Store.find(this, getIntent().getStringExtra(EXTRA_ID));
        if (existing != null) showForm(existing.type);
        else showPicker();
    }

    private void showPicker() {
        column = Ui.column(this);
        column.addView(Ui.header(this, "ADD", 0xFF9A9A9A));
        for (final String kind : Kinds.ALL) {
            column.addView(Ui.row(this, Kinds.label(kind), Kinds.color(kind), Kinds.addLabel(kind),
                () -> pick(kind), null));
        }
    }

    private void pick(String kind) {
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
        column.addView(Ui.header(this, Kinds.label(kind), Kinds.color(kind)));

        Kinds.Field[] fields = Kinds.fields(kind);
        for (Kinds.Field field : fields) {
            EditText input = new EditText(this);
            input.setHint(field.hint);
            input.setInputType(field.inputType);
            input.setSingleLine(true);
            input.setTextSize(15);
            input.setImeOptions(EditorInfo.IME_ACTION_DONE);
            if (existing != null) input.setText(Kinds.text(existing, field));
            input.setLayoutParams(Ui.spaced(this));
            column.addView(input);
            inputs.add(input);
        }

        error = Ui.note(this, "", 0xFFFF8A80);
        error.setVisibility(View.GONE);
        column.addView(error);

        if (fields.length > 0) {
            column.addView(Ui.button(this, "Save", Kinds.color(kind), 0xFF101010, this::save));
        } else {
            column.addView(Ui.note(this, "Nothing to edit here", 0xFF8A8A8A));
        }
        if (existing != null) {
            column.addView(Ui.button(this, "Delete", 0xFF2A1B1B, 0xFFFF8A80, () -> {
                Store.delete(this, existing.id);
                finish();
            }));
        }
    }

    private void save() {
        Kinds.Field[] fields = Kinds.fields(type);
        Item item = existing != null ? existing : new Item(Store.newId(), type, null);
        for (int i = 0; i < fields.length; i++) {
            String raw = inputs.get(i).getText().toString().trim();
            if (fields[i].required && raw.isEmpty()) {
                fail(fields[i].hint + " is needed");
                return;
            }
            Kinds.set(item, fields[i], raw);
        }
        if (type.equals(Kinds.UPI) && !item.get("upi_id").matches(UPI_ID_PATTERN)) {
            fail("UPI ID should look like name@bank");
            return;
        }
        if (type.equals(Kinds.WIFI) && item.get("pass").length() < 8) {
            fail("Password needs 8 or more characters");
            return;
        }
        Store.upsert(this, item);
        finish();
    }

    private void fail(String message) {
        error.setText(message);
        error.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        if (column != null && Ui.bezelScroll(column, ev)) return true;
        return super.dispatchGenericMotionEvent(ev);
    }
}
