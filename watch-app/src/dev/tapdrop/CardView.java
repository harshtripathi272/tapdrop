package dev.tapdrop;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

/**
 * The share screen for one item, drawn full screen on the round display.
 *
 * While the watch is answering NFC readers, rings travel outward to the edge,
 * like a signal being sent. When a phone has read the item, the icon turns into
 * a tick and one bright ring bursts out. The activity drives three animated
 * values: {@link #enter}, {@link #bump} and {@link #burst}.
 */
public class CardView extends View {
    String glyph = Icons.ADD;
    int color = Color.WHITE;
    String title = "";
    String subtitle = "";
    String footer = "";
    boolean ready = false;      // NFC is answering with this item
    boolean sent = false;       // briefly true after a phone finishes reading
    boolean warning = false;    // footer is a problem such as "NFC is off"

    float enter = 1f;           // 0 to 1 as the screen arrives
    float bump = 0f;            // 1 to 0 after the title's value changes
    float burst = 1f;           // 0 to 1 after a phone reads the item

    private static final long WAVE_MS = 2400;
    private static final int WAVES = 2;

    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint disc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    public CardView(Context context) {
        super(context);
        ring.setStyle(Paint.Style.STROKE);
        text.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float s = w / 438f;                      // designed against the 438 px Watch8 Classic
        float edge = w / 2f - 9f * s;

        canvas.drawColor(Color.BLACK);
        ring.setColor(color);

        // A steady rim, brighter once sent.
        ring.setStrokeWidth((sent ? 7f : 4f) * s);
        ring.setAlpha(sent ? 255 : (ready ? 95 : 45));
        canvas.drawCircle(cx, cy, edge, ring);

        // Waves travelling outward while ready.
        if (ready && !sent) {
            long now = SystemClock.uptimeMillis();
            for (int i = 0; i < WAVES; i++) {
                float t = ((now + i * WAVE_MS / WAVES) % WAVE_MS) / (float) WAVE_MS;
                float eased = 1f - (1f - t) * (1f - t);
                ring.setStrokeWidth((5f - 3f * t) * s);
                ring.setAlpha(Math.round(150 * (1f - t) * (1f - t)));
                canvas.drawCircle(cx, cy, edge * (0.87f + 0.13f * eased), ring);
            }
            postInvalidateDelayed(33);
        }

        // One bright ring leaving the icon when a phone has read the item.
        if (burst < 1f) {
            float eased = 1f - (1f - burst) * (1f - burst);
            ring.setStrokeWidth((16f - 12f * burst) * s);
            ring.setAlpha(Math.round(230 * (1f - burst)));
            canvas.drawCircle(cx, cy, 30f * s + (edge - 30f * s) * eased, ring);
        }

        // Icon disc. It lands with a small overshoot, and pops once on sent.
        float iconY = cy - 112f * s;
        float overshoot = 1f + 0.18f * (float) Math.sin(Math.min(1f, enter) * Math.PI);
        float pop = burst < 1f ? 1f + 0.22f * (float) Math.sin(burst * Math.PI) : 1f;
        float iconScale = (0.5f + 0.5f * enter) * overshoot * pop;
        disc.setColor(color);
        disc.setAlpha(Math.round(255 * Math.min(1f, enter * 1.5f)));
        canvas.drawCircle(cx, iconY, 30f * s * iconScale, disc);
        Icons.draw(canvas, sent ? Icons.CHECK : glyph, cx, iconY, 34f * s * iconScale, Ui.TEXT);

        // Text rises into place.
        int alpha = Math.round(255 * enter);
        float rise = (1f - enter) * 16f * s;
        float maxWidth = w * 0.76f;

        text.setTypeface(Ui.BOLD);
        text.setColor(Ui.TEXT);
        float size = 48f * s;
        text.setTextSize(size);
        while (text.measureText(title) > maxWidth && size > 26f * s) {
            size -= 2f * s;
            text.setTextSize(size);
        }
        text.setTextSize(size * (1f + 0.10f * bump));
        text.setAlpha(alpha);
        canvas.drawText(fit(title, maxWidth), cx, cy - 18f * s + rise, text);

        text.setTypeface(Typeface.DEFAULT);
        text.setTextSize(22f * s);
        text.setColor(Ui.TEXT_SECONDARY);
        text.setAlpha(alpha);
        canvas.drawText(fit(subtitle, maxWidth), cx, cy + 24f * s + rise, text);

        text.setTextSize(19f * s);
        if (sent) {
            text.setTypeface(Ui.BOLD);
            text.setColor(Ui.TEXT);
        } else {
            text.setColor(warning ? Ui.DANGER : Ui.TEXT_TERTIARY);
        }
        text.setAlpha(alpha);
        canvas.drawText(fit(footer, w * 0.66f), cx, cy + 74f * s + rise, text);
    }

    private String fit(String value, float width) {
        return TextUtils.ellipsize(value, text, width, TextUtils.TruncateAt.END).toString();
    }
}
