package dev.tapdrop.spike;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

/**
 * Line icons drawn in code on a 24-unit grid, so the app ships no image files.
 * One per item kind, plus "add" and "check". Call from the UI thread only.
 */
final class Icons {
    static final String ADD = "add";
    static final String CHECK = "check";

    private static final Paint STROKE = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path PATH = new Path();
    private static final RectF OVAL = new RectF();

    static {
        STROKE.setStyle(Paint.Style.STROKE);
        STROKE.setStrokeWidth(2f);
        STROKE.setStrokeCap(Paint.Cap.ROUND);
        STROKE.setStrokeJoin(Paint.Join.ROUND);
        FILL.setStyle(Paint.Style.FILL);
        FILL.setTextAlign(Paint.Align.CENTER);
        FILL.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }

    private Icons() {}

    /** Draws the glyph centred on (cx, cy) inside a square of the given size. */
    static void draw(Canvas canvas, String glyph, float cx, float cy, float size, int color) {
        float unit = size / 24f;
        STROKE.setColor(color);
        FILL.setColor(color);
        canvas.save();
        canvas.translate(cx - 12f * unit, cy - 12f * unit);
        canvas.scale(unit, unit);
        PATH.rewind();

        switch (glyph) {
            case Kinds.CONTACT:
                canvas.drawCircle(12f, 8.5f, 3.5f, STROKE);
                PATH.moveTo(5f, 19.5f);
                PATH.cubicTo(5f, 15.5f, 8.5f, 14f, 12f, 14f);
                PATH.cubicTo(15.5f, 14f, 19f, 15.5f, 19f, 19.5f);
                canvas.drawPath(PATH, STROKE);
                break;

            case Kinds.WIFI:
                canvas.drawCircle(12f, 18.5f, 1.3f, FILL);
                for (float r : new float[] {5f, 9.25f, 13.5f}) {
                    OVAL.set(12f - r, 18.5f - r, 12f + r, 18.5f + r);
                    canvas.drawArc(OVAL, 225f, 90f, false, STROKE);
                }
                break;

            case Kinds.UPI:
                FILL.setTextSize(18f);
                Paint.FontMetrics m = FILL.getFontMetrics();
                canvas.drawText("₹", 12f, 12f - (m.ascent + m.descent) / 2f, FILL);
                break;

            case Kinds.LOCATION:
                PATH.moveTo(12f, 21.5f);
                PATH.cubicTo(8f, 17.5f, 5.5f, 14f, 5.5f, 10f);
                OVAL.set(5.5f, 3.5f, 18.5f, 16.5f);
                PATH.arcTo(OVAL, 180f, 180f);
                PATH.cubicTo(18.5f, 14f, 16f, 17.5f, 12f, 21.5f);
                PATH.close();
                canvas.drawPath(PATH, STROKE);
                canvas.drawCircle(12f, 10f, 2.2f, STROKE);
                break;

            case Kinds.LINK:
                canvas.rotate(-45f, 12f, 12f);
                PATH.moveTo(10f, 7.5f);
                PATH.lineTo(8f, 7.5f);
                OVAL.set(3.5f, 7.5f, 12.5f, 16.5f);
                PATH.arcTo(OVAL, 270f, -180f);
                PATH.lineTo(10f, 16.5f);
                PATH.moveTo(14f, 7.5f);
                PATH.lineTo(16f, 7.5f);
                OVAL.set(11.5f, 7.5f, 20.5f, 16.5f);
                PATH.arcTo(OVAL, 270f, 180f);
                PATH.lineTo(14f, 16.5f);
                PATH.moveTo(9f, 12f);
                PATH.lineTo(15f, 12f);
                canvas.drawPath(PATH, STROKE);
                break;

            case CHECK:
                PATH.moveTo(5.5f, 12.5f);
                PATH.lineTo(10f, 17f);
                PATH.lineTo(18.5f, 7.5f);
                canvas.drawPath(PATH, STROKE);
                break;

            default:    // ADD
                canvas.drawLine(12f, 6f, 12f, 18f, STROKE);
                canvas.drawLine(6f, 12f, 18f, 12f, STROKE);
        }
        canvas.restore();
    }
}
