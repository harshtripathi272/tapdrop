package dev.tapdrop;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.PathInterpolator;

/**
 * The share screen for one item, drawn full screen on the round display. The
 * activity sets the state (ready, sent, warning) and drives three animated values:
 * {@link #enter}, {@link #bump} and {@link #burst}.
 *
 * One UI: while the watch is answering NFC readers, rings travel outward to the edge,
 * like a signal being sent. When a phone has read the item, the icon turns into a
 * tick and one bright ring bursts out.
 *
 * Material 3 Expressive: the same rim, ripples and burst, in Material's terms. The
 * rim is a surfaceContainer track that fills with the item's colour once sent; the
 * ripples are in a light tone of that colour, with round caps and Material's
 * emphasized decelerate easing. The icon sits in a scalloped "cookie" shape that
 * slowly turns and morphs into a circle with a tick when sent.
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

    // Material's emphasized decelerate easing, for ripples that leave fast and settle at the rim.
    private static final PathInterpolator EMPHASIZED = new PathInterpolator(0.05f, 0.7f, 0.1f, 1f);

    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint disc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    // Material state, eased towards its target on every frame so changes never jump.
    private float live = 0f;            // 0 quiet to 1 ripples showing
    private float done = 0f;            // 0 waiting to 1 sent
    private long lastFrame = 0;
    private final long born = SystemClock.uptimeMillis();

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

        Theme th = Theme.of(getContext());
        canvas.drawColor(th.background);
        float iconY = cy - 112f * s;
        if (th.wavyProgress) drawMaterial(canvas, th, cx, cy, s, iconY);
        else drawOneUi(canvas, th, cx, cy, s, iconY);
        drawText(canvas, th, w, cx, cy, s);
    }

    // ---- One UI ----------------------------------------------------------------

    private void drawOneUi(Canvas canvas, Theme th, float cx, float cy, float s, float iconY) {
        float edge = getWidth() / 2f - 9f * s;
        ring.setColor(th.stroke(color));
        ring.setStrokeCap(Paint.Cap.BUTT);

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
        float iconScale = (0.5f + 0.5f * enter) * overshoot() * pop();
        disc.setColor(th.disc(color));
        disc.setAlpha(Math.round(255 * Math.min(1f, enter * 1.5f)));
        canvas.drawCircle(cx, iconY, 30f * s * iconScale, disc);
        Icons.draw(canvas, sent ? Icons.CHECK : glyph, cx, iconY, 34f * s * iconScale, th.glyph(color));
    }

    // ---- Material 3 Expressive ---------------------------------------------------

    private void drawMaterial(Canvas canvas, Theme th, float cx, float cy, float s, float iconY) {
        Context c = getContext();
        long now = SystemClock.uptimeMillis();
        float dt = lastFrame == 0 ? 0f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        live = towards(live, ready && !sent ? 1f : 0f, dt, 5f);
        done = towards(done, sent ? 1f : 0f, dt, sent ? 8f : 3f);

        float edge = getWidth() / 2f - 9f * s;
        int tone = th.stroke(color);
        float alpha = Math.min(1f, enter * 1.5f);
        ring.setStrokeCap(Paint.Cap.ROUND);

        // The rim: a soft track on surfaceContainer that fills with the item's colour once sent.
        ring.setStrokeWidth(Ui.dp(c, 3) + Ui.dp(c, 2) * done);
        ring.setColor(Theme.mix(tone, th.container, done));
        ring.setAlpha(Math.round(255 * alpha));
        canvas.drawCircle(cx, cy, edge, ring);

        // Ripples travelling out to the rim while ready, fading as they arrive.
        if (live > 0.01f) {
            for (int i = 0; i < WAVES; i++) {
                float t = ((now + i * WAVE_MS / WAVES) % WAVE_MS) / (float) WAVE_MS;
                float eased = EMPHASIZED.getInterpolation(t);
                ring.setColor(tone);
                ring.setStrokeWidth(Ui.dp(c, 3) - Ui.dp(c, 1.5f) * t);
                ring.setAlpha(Math.round(170 * (1f - t) * (1f - t) * live * alpha));
                canvas.drawCircle(cx, cy, edge * (0.84f + 0.16f * eased), ring);
            }
        }

        // One ring bursting from the icon to the rim when a phone has read the item.
        if (burst < 1f) {
            float eased = EMPHASIZED.getInterpolation(burst);
            ring.setColor(tone);
            ring.setStrokeWidth(Ui.dp(c, 6) - Ui.dp(c, 4) * burst);
            ring.setAlpha(Math.round(230 * (1f - burst)));
            canvas.drawCircle(cx, cy, 34f * s + (edge - 34f * s) * eased, ring);
        }

        // Icon: a nine-scallop cookie that turns slowly, morphing to a circle when sent.
        float iconScale = (0.5f + 0.5f * enter) * overshoot() * pop();
        float r = 34f * s * iconScale;
        float scallop = 0.075f * (1f - done);
        float turn = (now - born) / 14000f * 360f;
        path.rewind();
        for (int i = 0; i <= 180; i++) {
            double a = Math.toRadians(i * 2f);
            double rr = r * (1 + scallop * Math.cos(9 * (a + Math.toRadians(turn))));
            float x = cx + (float) (rr * Math.cos(a));
            float y = iconY + (float) (rr * Math.sin(a));
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        path.close();
        disc.setColor(Theme.mix(color, th.background, 0.34f));
        disc.setAlpha(Math.round(255 * alpha));
        canvas.drawPath(path, disc);
        Icons.draw(canvas, sent ? Icons.CHECK : glyph, cx, iconY, 36f * s * iconScale, th.glyph(color));

        boolean settled = !ready && !sent && done < 0.001f && live < 0.001f && burst >= 1f;
        if (!settled) postInvalidateOnAnimation();
    }

    private static float towards(float value, float target, float dt, float rate) {
        return value + (target - value) * (1f - (float) Math.exp(-rate * dt));
    }

    // ---- shared ----------------------------------------------------------------

    /** The icon lands with a small overshoot. */
    private float overshoot() {
        return 1f + 0.18f * (float) Math.sin(Math.min(1f, enter) * Math.PI);
    }

    /** And pops once on sent. */
    private float pop() {
        return burst < 1f ? 1f + 0.22f * (float) Math.sin(burst * Math.PI) : 1f;
    }

    /** Title, subtitle and footer rise into place. */
    private void drawText(Canvas canvas, Theme th, float w, float cx, float cy, float s) {
        int alpha = Math.round(255 * enter);
        float rise = (1f - enter) * 16f * s;
        float maxWidth = w * 0.76f;

        text.setTypeface(th.bold);
        text.setColor(th.text);
        float size = 48f * s;
        text.setTextSize(size);
        while (text.measureText(title) > maxWidth && size > 26f * s) {
            size -= 2f * s;
            text.setTextSize(size);
        }
        text.setTextSize(size * (1f + 0.10f * bump));
        text.setAlpha(alpha);
        canvas.drawText(fit(title, maxWidth), cx, cy - 18f * s + rise, text);

        text.setTypeface(th.regular);
        text.setTextSize(22f * s);
        text.setColor(th.textSecondary);
        text.setAlpha(alpha);
        canvas.drawText(fit(subtitle, maxWidth), cx, cy + 24f * s + rise, text);

        text.setTextSize(19f * s);
        if (sent) {
            text.setTypeface(th.bold);
            text.setColor(th.text);
        } else {
            text.setColor(warning ? th.danger : th.textTertiary);
        }
        text.setAlpha(alpha);
        canvas.drawText(fit(footer, w * 0.66f), cx, cy + 74f * s + rise, text);
    }

    private String fit(String value, float width) {
        return TextUtils.ellipsize(value, text, width, TextUtils.TruncateAt.END).toString();
    }
}
