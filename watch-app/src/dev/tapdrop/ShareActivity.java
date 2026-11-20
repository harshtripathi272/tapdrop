package dev.tapdrop;

import android.Manifest;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * One item, ready to share. While this screen is open the watch answers NFC readers
 * with that item. On a payment request the bezel sets the amount.
 */
public class ShareActivity extends Activity {
    static final String EXTRA_ID = "id";
    private static final long SENT_FLASH_MS = 2500;

    private CardView card;
    private String id;
    private Item item;
    private String nfcProblem = null;
    private String locationProblem = null;
    private boolean askedForLocation = false;
    private float bezel = 0f;
    private int seenReads = 0;
    private long sentAt = 0;
    private LocationManager locations;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (NdefTagService.reads != seenReads) {
                seenReads = NdefTagService.reads;
                sentAt = SystemClock.elapsedRealtime();
                card.burst = 0f;
                animate(620, 0, new LinearInterpolator(), v -> card.burst = v);
                confirmBuzz();
            }
            render();
            handler.postDelayed(this, 200);
        }
    };

    private final LocationListener onFix = new LocationListener() {
        @Override public void onLocationChanged(Location l) { consider(l); }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        id = getIntent().getStringExtra(EXTRA_ID);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        FrameLayout root = new FrameLayout(this);
        card = new CardView(this);
        root.addView(card, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        // Editing is one tap away, so nobody has to discover a long-press to change their details.
        TextView edit = new TextView(this);
        edit.setText("Edit");
        edit.setTextColor(Ui.TEXT);
        edit.setTextSize(13);
        edit.setTypeface(Ui.MEDIUM);
        edit.setPadding(Ui.dp(this, 22), Ui.dp(this, 8), Ui.dp(this, 22), Ui.dp(this, 8));
        edit.setBackground(Ui.pill(this, Ui.CONTAINER, 20));
        edit.setOnClickListener(v ->
            Ui.go(this, new Intent(this, EditActivity.class).putExtra(EditActivity.EXTRA_ID, id)));
        Ui.pressScale(edit);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = Math.round(getResources().getDisplayMetrics().heightPixels * 0.08f);
        root.addView(edit, lp);

        setContentView(root);
        seenReads = NdefTagService.reads;

        // The content arrives a beat after the screen does.
        card.enter = 0f;
        edit.setAlpha(0f);
        edit.animate().alpha(1f).setStartDelay(260).setDuration(240).start();
        animate(420, 80, new DecelerateInterpolator(1.6f), v -> card.enter = v);
    }

    /** Runs a 0 to 1 animation and redraws the card on every frame. */
    private void animate(long duration, long delay, TimeInterpolator curve, FloatConsumer apply) {
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(duration);
        a.setStartDelay(delay);
        a.setInterpolator(curve);
        a.addUpdateListener(v -> {
            apply.accept((Float) v.getAnimatedValue());
            card.invalidate();
        });
        a.start();
    }

    private interface FloatConsumer {
        void accept(float value);
    }

    @Override
    public void finish() {
        super.finish();
        Ui.leave(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        item = Store.find(this, id);        // reloaded: it may have been edited or deleted
        if (item == null) { finish(); return; }
        NdefTagService.openItemId = item.id;

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
        Log.d(NdefTagService.TAG, "sharing " + item.type + ": " + (nfcProblem == null ? "ready" : nfcProblem));

        if (item.type.equals(Kinds.LOCATION)) startLocation();
        handler.post(poll);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(poll);
        NdefTagService.openItemId = null;
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter != null) CardEmulation.getInstance(adapter).unsetPreferredService(this);
        if (locations != null) locations.removeUpdates(onFix);
    }

    // ---- bezel ---------------------------------------------------------------

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_SCROLL && ev.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) {
            if (item != null && item.type.equals(Kinds.UPI)) {
                // Clockwise reports a negative scroll value. One detent is one step.
                bezel -= ev.getAxisValue(MotionEvent.AXIS_SCROLL);
                int steps = (int) (bezel / 0.9f);
                if (steps != 0) {
                    bezel -= steps;
                    int amount = item.getInt("amount") + steps * Kinds.AMOUNT_STEP;
                    item.put("amount", Math.max(0, Math.min(Kinds.AMOUNT_MAX, amount)));
                    Store.upsert(this, item);
                    sentAt = 0;
                    // The number jumps a little and the wrist ticks, like a physical dial.
                    card.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    animate(180, 0, new DecelerateInterpolator(), v -> card.bump = 1f - v);
                    render();
                }
            }
            return true;
        }
        return super.dispatchGenericMotionEvent(ev);
    }

    /** Two short taps on the wrist: the item reached a phone. */
    private void confirmBuzz() {
        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (v != null) v.vibrate(VibrationEffect.createWaveform(new long[] {0, 35, 70, 55}, -1));
    }

    // ---- location ------------------------------------------------------------

    private void startLocation() {
        boolean fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) {
            locationProblem = "Allow location to share it";
            if (!askedForLocation) {
                askedForLocation = true;
                requestPermissions(new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }
            return;
        }
        locationProblem = null;
        locations = (LocationManager) getSystemService(LOCATION_SERVICE);
        for (String provider : new String[] {"fused", LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
            try {
                if (!locations.getAllProviders().contains(provider)) continue;
                consider(locations.getLastKnownLocation(provider));
                locations.requestLocationUpdates(provider, 2000L, 0f, onFix, Looper.getMainLooper());
            } catch (SecurityException | IllegalArgumentException e) {
                Log.w(NdefTagService.TAG, "location provider " + provider + " unavailable: " + e.getMessage());
            }
        }
    }

    /** Keeps the newer fix, or the more accurate one when two arrive close together. */
    private void consider(Location candidate) {
        if (candidate == null) return;
        Location current = NdefTagService.fix;
        boolean newer = current == null || candidate.getTime() > current.getTime() + 10_000;
        boolean sharper = current != null && Math.abs(candidate.getTime() - current.getTime()) <= 10_000
            && candidate.getAccuracy() < current.getAccuracy();
        if (newer || sharper) NdefTagService.fix = candidate;
    }

    // ---- drawing -------------------------------------------------------------

    private void render() {
        if (item == null) return;
        boolean sent = sentAt != 0 && SystemClock.elapsedRealtime() - sentAt < SENT_FLASH_MS;
        boolean isLocation = item.type.equals(Kinds.LOCATION);
        boolean waitingForFix = isLocation && !Kinds.usable(NdefTagService.fix);
        String problem = nfcProblem != null ? nfcProblem : (isLocation ? locationProblem : null);

        card.glyph = item.type;
        card.color = Kinds.color(item.type);
        card.title = Kinds.title(item);
        card.subtitle = Kinds.subtitle(item, NdefTagService.fix);
        card.sent = sent;
        card.warning = problem != null;
        card.ready = problem == null && !waitingForFix;
        if (problem != null) {
            card.footer = problem;
        } else if (sent) {
            card.footer = "Sent";
        } else if (waitingForFix) {
            card.footer = "Waiting for a fix";
        } else if (item.type.equals(Kinds.UPI)) {
            card.footer = "Bezel sets the amount";
        } else {
            card.footer = "Hold to a phone";
        }
        card.invalidate();
    }
}
