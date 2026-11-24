package dev.tapdrop;

import android.app.Activity;
import android.content.ComponentName;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Quick tap: what the watch shares when a phone taps while TapDrop is closed or the
 * display is asleep, and whether that takes one tap or two.
 *
 * The first part is a choice of item, or nothing. The second part is one-tap sharing:
 *
 * With TapDrop closed, a tap makes the watch ask the wearer to choose between Samsung
 * Health and TapDrop, because both answer the same NFC tag ID. This screen lets the
 * wearer end that by switching off Samsung Health's NFC tag service, which leaves
 * TapDrop as the only answer. It is the wearer's decision and it is reversible here.
 *
 * It uses CardEmulation.setServiceEnabledForCategoryOther, which Android added "to
 * avoid AID collision". It is looked up by name because it may not exist, or may be
 * refused, on a given watch.
 */
public class OneTapActivity extends Activity {
    private static final ComponentName SAMSUNG_HEALTH_TAG = new ComponentName(
        "com.samsung.android.wear.shealth", "com.samsung.android.wear.shealth.device.NfcHceService");
    private static final String KEY_ON = "one_tap";

    private LinearLayout column;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        build(null, false);
    }

    private void build(String result, boolean problem) {
        boolean on = Store.prefs(this).getBoolean(KEY_ON, false);
        column = Ui.column(this);
        column.addView(Ui.title(this, "Quick tap"));

        // Which item goes out when a phone taps while TapDrop is closed or the display sleeps.
        column.addView(Ui.body(this,
            "What a phone gets when it taps while TapDrop is closed or the screen is off.",
            Ui.theme(this).textSecondary));
        String quick = Store.quick(this);
        LinearLayout choices = Ui.group(this);
        for (Item item : Store.load(this)) {
            final String id = item.id;
            boolean chosen = id.equals(quick);
            Ui.addRow(choices, Ui.choice(this, item.type, Kinds.color(item.type), Kinds.title(item),
                chosen ? "Shared when closed" : Kinds.rowSubtitle(item), chosen,
                () -> { Store.setQuick(this, id); build(null, false); }));
        }
        Ui.addRow(choices, Ui.choice(this, Icons.NONE, Ui.theme(this).neutral, "Nothing",
            quick == null ? "Silent when closed" : "Stay silent", quick == null,
            () -> { Store.setQuick(this, Store.QUICK_NONE); build(null, false); }));
        column.addView(choices);

        // Whether that takes one tap or two.
        column.addView(Ui.section(this, "One-tap sharing"));
        if (on) {
            column.addView(Ui.body(this,
                "On. A phone that touches this watch gets the item above straight away, screen on or off.",
                Ui.theme(this).textSecondary));
        } else {
            column.addView(Ui.body(this,
                "Off. The watch first asks you to pick an app, then the phone taps again. "
                    + "That is because Samsung Health also answers NFC taps.",
                Ui.theme(this).textSecondary));
        }

        if (result != null) {
            TextView r = Ui.body(this, result, problem ? Ui.theme(this).danger : Ui.theme(this).text);
            column.addView(r);
        }

        // A switch row in Material; "Turn on" or "Turn off" buttons in One UI.
        column.addView(Ui.toggle(this, "Share in one tap", on ? "On" : "Off", on, () -> apply(!on)));
        if (!on) {
            column.addView(Ui.body(this,
                "Turning it on switches off Samsung Health's NFC tag feature, which some gym machines use. "
                    + "No one will be asked before the item above is sent, even while the screen is off, "
                    + "so keep it to something you'd hand to a stranger.",
                Ui.theme(this).textTertiary));
        }
    }

    /** Turns Samsung Health's tag service off (one tap on) or back on (one tap off). */
    private void apply(boolean oneTap) {
        String outcome;
        boolean ok = false;
        try {
            NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
            if (adapter == null) throw new IllegalStateException("no NFC");
            Method set = CardEmulation.class.getMethod(
                "setServiceEnabledForCategoryOther", ComponentName.class, boolean.class);
            Object code = set.invoke(CardEmulation.getInstance(adapter), SAMSUNG_HEALTH_TAG, !oneTap);
            int status = code instanceof Integer ? (Integer) code : -1;
            Log.d(NdefTagService.TAG, "one tap " + (oneTap ? "on" : "off") + ": status=" + status);
            switch (status) {
                case 0:     // done
                case 3:     // already in that state
                    ok = true;
                    outcome = oneTap ? "Done. Try a tap with TapDrop closed." : "Done. Samsung Health answers taps again.";
                    break;
                case 1:
                    outcome = "This watch does not allow apps to change it.";
                    break;
                case 2:
                    outcome = "Samsung Health's NFC service was not found, so there may be nothing to switch off.";
                    break;
                default:
                    outcome = "The watch reported an error (" + status + ").";
            }
        } catch (NoSuchMethodException e) {
            outcome = "This version of Android has no setting for it.";
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            Log.w(NdefTagService.TAG, "one tap refused: " + cause);
            outcome = cause instanceof SecurityException
                ? "The watch refused. Only system apps may change this."
                : "The watch refused: " + cause.getClass().getSimpleName();
        } catch (ReflectiveOperationException | RuntimeException e) {
            Log.w(NdefTagService.TAG, "one tap failed: " + e);
            outcome = "Could not change it: " + e.getClass().getSimpleName();
        }
        if (ok) Store.prefs(this).edit().putBoolean(KEY_ON, oneTap).apply();
        build(outcome, !ok);
    }

    @Override
    public void finish() {
        super.finish();
        Ui.leave(this);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        if (column != null && Ui.bezelScroll(column, ev)) return true;
        return super.dispatchGenericMotionEvent(ev);
    }
}
