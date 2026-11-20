package dev.tapdrop.spike;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * Turn the bezel to choose what to share, then hold the watch to a phone.
 * On the PAY ME slot, tap the screen and the bezel sets the amount instead.
 *
 * Fields can be set from the laptop:
 *   adb shell am start -n dev.tapdrop.spike/.MainActivity --es type contact --es name "..." --es phone "..."
 *   ... --es type wifi --es ssid "..." --es pass "..."
 *   ... --es type upi  --es upi_id "..." --es upi_name "..." --es amount 250
 *   ... --es type link --es url "https://..."
 */
public class MainActivity extends Activity {
    private static final long SENT_FLASH_MS = 2500;

    private CardView card;
    private SharedPreferences prefs;
    private String nfcProblem = null;
    private boolean editingAmount = false;
    private float bezel = 0f;
    private int seenReads = 0;
    private long sentAt = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (NdefTagService.reads != seenReads) {
                seenReads = NdefTagService.reads;
                sentAt = SystemClock.elapsedRealtime();
            }
            render();
            handler.postDelayed(this, 200);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        prefs = getSharedPreferences(Slots.PREFS, Context.MODE_PRIVATE);
        card = new CardView(this);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (Slots.index(prefs) != Slots.UPI) return;
                editingAmount = !editingAmount;
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                render();
            }
        });
        setContentView(card);
        card.requestFocus();
        seenReads = NdefTagService.reads;
        takeFields(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        takeFields(intent);
    }

    /** Stores any fields passed as extras and jumps to that slot. Other slots are left alone. */
    private void takeFields(Intent intent) {
        if (intent == null) return;
        int slot = Slots.indexOf(intent.getStringExtra("type"));
        if (slot < 0) return;
        SharedPreferences.Editor e = prefs.edit();
        for (String key : Slots.STRING_KEYS) {
            String value = intent.getStringExtra(key);
            if (value != null) e.putString(key, value);
        }
        String amount = intent.getStringExtra(Slots.KEY_AMOUNT);
        if (amount != null) {
            try {
                e.putInt(Slots.KEY_AMOUNT, Math.max(0, Math.min(Slots.AMOUNT_MAX, Integer.parseInt(amount.trim()))));
            } catch (NumberFormatException ignored) {
                Log.w(NdefTagService.TAG, "amount is not a whole number: " + amount);
            }
        }
        e.putInt(Slots.KEY_SLOT, slot).apply();
        editingAmount = false;
        Log.d(NdefTagService.TAG, "fields set, slot=" + Slots.TYPES[slot]);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_SCROLL && ev.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) {
            // Clockwise reports a negative scroll value.
            bezel -= ev.getAxisValue(MotionEvent.AXIS_SCROLL);
            while (bezel >= 0.9f) { step(1); bezel -= 1f; }
            while (bezel <= -0.9f) { step(-1); bezel += 1f; }
            return true;
        }
        return super.onGenericMotionEvent(ev);
    }

    private void step(int dir) {
        int slot = Slots.index(prefs);
        if (editingAmount && slot == Slots.UPI) {
            int amount = prefs.getInt(Slots.KEY_AMOUNT, 0) + dir * Slots.AMOUNT_STEP;
            prefs.edit().putInt(Slots.KEY_AMOUNT, Math.max(0, Math.min(Slots.AMOUNT_MAX, amount))).apply();
        } else {
            int n = Slots.TYPES.length;
            prefs.edit().putInt(Slots.KEY_SLOT, (slot + dir + n) % n).apply();
            editingAmount = false;
        }
        sentAt = 0;
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Samsung Health registers the same tag ID. Priority goes to the foreground app that asks.
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter == null) {
            nfcProblem = "No NFC on this watch";
        } else if (!adapter.isEnabled()) {
            nfcProblem = "NFC is off";
        } else {
            boolean ok = CardEmulation.getInstance(adapter)
                .setPreferredService(this, new ComponentName(this, NdefTagService.class));
            nfcProblem = ok ? null : "NFC priority refused";
        }
        Log.d(NdefTagService.TAG, "onResume: " + (nfcProblem == null ? "ready" : nfcProblem));
        handler.post(poll);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(poll);
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter != null) CardEmulation.getInstance(adapter).unsetPreferredService(this);
    }

    private void render() {
        int slot = Slots.index(prefs);
        boolean sent = sentAt != 0 && SystemClock.elapsedRealtime() - sentAt < SENT_FLASH_MS;
        card.index = slot;
        card.title = Slots.title(prefs, slot);
        card.subtitle = Slots.subtitle(prefs, slot);
        card.sent = sent;
        card.warning = nfcProblem != null;
        if (nfcProblem != null) {
            card.footer = nfcProblem;
        } else if (sent) {
            card.footer = "Sent";
        } else if (slot == Slots.UPI && editingAmount) {
            card.footer = "Turn bezel, tap to lock";
        } else if (slot == Slots.UPI) {
            card.footer = "Tap to set amount";
        } else {
            card.footer = "Hold to a phone";
        }
        card.invalidate();
    }
}
