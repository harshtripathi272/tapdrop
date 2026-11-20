package dev.tapdrop;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ComponentName;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

/**
 * Learn from a phone. While this screen is open the watch looks like an empty,
 * writable NFC tag. Any tag-writing app can write a message to it, and the watch
 * keeps that message as a new item. Closing the screen makes the tag read-only again.
 */
public class LearnActivity extends Activity {
    private static final long CLOSE_AFTER_MS = 1900;

    private CardView card;
    private String nfcProblem = null;
    private int seenLearned = 0;
    private Item learned = null;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (NdefTagService.learned != seenLearned) {
                seenLearned = NdefTagService.learned;
                learned = Store.find(LearnActivity.this, NdefTagService.lastLearnedId);
                card.burst = 0f;
                animate(620, new LinearInterpolator(), true);
                Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (v != null) v.vibrate(VibrationEffect.createWaveform(new long[] {0, 35, 70, 55}, -1));
                handler.postDelayed(LearnActivity.this::finish, CLOSE_AFTER_MS);
            }
            render();
            handler.postDelayed(this, 200);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        card = new CardView(this);
        setContentView(card);
        seenLearned = NdefTagService.learned;
        card.enter = 0f;
        animate(420, new DecelerateInterpolator(1.6f), false);
    }

    /** Runs burst (or enter) from 0 to 1 and redraws on every frame. */
    private void animate(long duration, android.animation.TimeInterpolator curve, boolean burst) {
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(duration);
        a.setInterpolator(curve);
        a.addUpdateListener(v -> {
            float value = (Float) v.getAnimatedValue();
            if (burst) card.burst = value;
            else card.enter = value;
            card.invalidate();
        });
        a.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        // Writes are accepted only from here until onPause.
        NdefTagService.learnScreenOpen = nfcProblem == null;
        Log.d(NdefTagService.TAG, "learn screen: " + (nfcProblem == null ? "accepting writes" : nfcProblem));
        handler.post(poll);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
        NdefTagService.learnScreenOpen = false;
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter != null) CardEmulation.getInstance(adapter).unsetPreferredService(this);
    }

    @Override
    public void finish() {
        super.finish();
        Ui.leave(this);
    }

    private void render() {
        card.glyph = Kinds.CUSTOM;
        card.color = Kinds.color(Kinds.CUSTOM);
        card.warning = nfcProblem != null;
        card.sent = learned != null;
        card.ready = nfcProblem == null && learned == null;
        if (learned != null) {
            card.title = Kinds.title(learned);
            card.subtitle = Kinds.subtitle(learned, null);
            card.footer = "Saved";
        } else {
            card.title = "Learn";
            card.subtitle = "Write a tag to me";
            card.footer = nfcProblem != null ? nfcProblem : "Use any NFC tag app";
        }
        card.invalidate();
    }
}
