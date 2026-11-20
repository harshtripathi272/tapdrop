package dev.tapdrop.spike;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
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
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.WindowManager;

/**
 * One item, ready to share. While this screen is open the watch answers NFC readers
 * with that item. On a payment request the bezel sets the amount.
 */
public class ShareActivity extends Activity {
    static final String EXTRA_ID = "id";
    private static final long SENT_FLASH_MS = 2500;

    private CardView card;
    private Item item;
    private String nfcProblem = null;
    private String locationProblem = null;
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
        item = Store.find(this, getIntent().getStringExtra(EXTRA_ID));
        if (item == null) { finish(); return; }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        card = new CardView(this);
        setContentView(card);
        seenReads = NdefTagService.reads;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (item == null) return;
        Store.setActive(this, item.id);

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
        if (item == null) return;
        Store.setActive(this, null);
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
                    render();
                }
            }
            return true;
        }
        return super.dispatchGenericMotionEvent(ev);
    }

    // ---- location ------------------------------------------------------------

    private void startLocation() {
        boolean fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) {
            if (locationProblem == null) {
                locationProblem = "Allow location to share it";
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

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        // onResume runs again after the permission dialog closes and starts updates if granted.
        boolean granted = false;
        for (int r : results) granted |= r == PackageManager.PERMISSION_GRANTED;
        locationProblem = granted ? null : "Location not allowed";
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
        boolean sent = sentAt != 0 && SystemClock.elapsedRealtime() - sentAt < SENT_FLASH_MS;
        boolean isLocation = item.type.equals(Kinds.LOCATION);
        String problem = nfcProblem != null ? nfcProblem : (isLocation ? locationProblem : null);

        card.label = Kinds.label(item.type);
        card.color = Kinds.color(item.type);
        card.title = Kinds.title(item);
        card.subtitle = Kinds.subtitle(item, NdefTagService.fix);
        card.sent = sent;
        card.warning = problem != null;
        if (problem != null) {
            card.footer = problem;
        } else if (sent) {
            card.footer = "Sent";
        } else if (isLocation && !Kinds.usable(NdefTagService.fix)) {
            card.footer = "Waiting for a fix";
        } else if (item.type.equals(Kinds.UPI)) {
            card.footer = "Bezel sets amount";
        } else {
            card.footer = "Hold to a phone";
        }
        card.invalidate();
    }
}
