package dev.tapdrop.spike;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

/**
 * One slot drawn full screen on the round display. Four arc segments around the edge
 * mirror the bezel position: the lit segment is the selected slot.
 */
public class CardView extends View {
    int index = 0;
    String title = "";
    String subtitle = "";
    String footer = "";
    boolean sent = false;       // briefly true after a phone finishes reading
    boolean warning = false;    // footer is an error such as "NFC is off"

    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final Typeface bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);

    public CardView(Context context) {
        super(context);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeCap(Paint.Cap.ROUND);
        text.setTextAlign(Paint.Align.CENTER);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float s = w / 438f;                      // designed against the 438 px Watch8 Classic
        int color = Slots.COLORS[index];

        canvas.drawColor(Color.BLACK);

        // Edge segments, slot 0 centred at 12 o'clock.
        float inset = 13 * s;
        oval.set(inset, inset, w - inset, h - inset);
        ring.setStrokeWidth(9 * s);
        int n = Slots.TYPES.length;
        float span = 360f / n;
        float gap = 14f;
        for (int i = 0; i < n; i++) {
            boolean lit = sent || i == index;
            ring.setColor(lit ? color : Slots.COLORS[i]);
            ring.setAlpha(lit ? 255 : 55);
            canvas.drawArc(oval, -90f + i * span - (span - gap) / 2f, span - gap, false, ring);
        }

        float maxWidth = w * 0.72f;

        text.setTypeface(bold);
        text.setLetterSpacing(0.16f);
        text.setTextSize(21 * s);
        text.setColor(color);
        canvas.drawText(Slots.LABELS[index], cx, cy - 82 * s, text);

        text.setLetterSpacing(0f);
        text.setColor(Color.WHITE);
        float size = 56 * s;
        text.setTextSize(size);
        while (text.measureText(title) > maxWidth && size > 26 * s) {
            size -= 2 * s;
            text.setTextSize(size);
        }
        canvas.drawText(fit(title, maxWidth), cx, cy + 6 * s, text);

        text.setTypeface(Typeface.DEFAULT);
        text.setTextSize(22 * s);
        text.setColor(0xFFB8B8B8);
        canvas.drawText(fit(subtitle, maxWidth), cx, cy + 50 * s, text);

        text.setTextSize(19 * s);
        if (sent) {
            text.setTypeface(bold);
            text.setColor(color);
        } else {
            text.setColor(warning ? 0xFFFF8A80 : 0xFF8A8A8A);
        }
        canvas.drawText(fit(footer, w * 0.6f), cx, cy + 112 * s, text);
    }

    private String fit(String value, float width) {
        return TextUtils.ellipsize(value, text, width, TextUtils.TruncateAt.END).toString();
    }
}
