package dev.tapdrop.spike;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

/**
 * The share screen for one item, drawn full screen on the round display.
 * A ring in the item's colour sits at the edge and lights up when a phone has read it.
 */
public class CardView extends View {
    String label = "";
    int color = Color.WHITE;
    String title = "";
    String subtitle = "";
    String footer = "";
    boolean sent = false;       // briefly true after a phone finishes reading
    boolean warning = false;    // footer is a problem such as "NFC is off"

    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Typeface bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);

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

        canvas.drawColor(Color.BLACK);

        ring.setStrokeWidth((sent ? 14 : 8) * s);
        ring.setColor(color);
        ring.setAlpha(sent ? 255 : 110);
        canvas.drawCircle(cx, cy, w / 2f - 13 * s, ring);

        float maxWidth = w * 0.72f;

        text.setTypeface(bold);
        text.setLetterSpacing(0.16f);
        text.setTextSize(21 * s);
        text.setColor(color);
        canvas.drawText(label, cx, cy - 82 * s, text);

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
