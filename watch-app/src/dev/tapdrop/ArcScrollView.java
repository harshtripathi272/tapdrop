package dev.tapdrop;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.widget.ScrollView;

/**
 * A ScrollView with the curved position indicator round watches use: a short arc
 * at the right edge that shows while scrolling and fades out afterwards.
 */
public class ArcScrollView extends ScrollView {
    private static final float TRACK_SWEEP = 44f;       // degrees, centred on 3 o'clock
    private static final long HOLD_MS = 700;
    private static final long FADE_MS = 350;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private long lastScroll = 0;

    public ArcScrollView(Context context) {
        super(context);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        setVerticalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldL, int oldT) {
        super.onScrollChanged(l, t, oldL, oldT);
        lastScroll = SystemClock.uptimeMillis();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (getChildCount() == 0) return;
        int range = getChildAt(0).getHeight() - getHeight();
        if (range <= 0) return;

        long idle = SystemClock.uptimeMillis() - lastScroll;
        if (lastScroll == 0 || idle > HOLD_MS + FADE_MS) return;
        float visible = idle <= HOLD_MS ? 1f : 1f - (idle - HOLD_MS) / (float) FADE_MS;

        float stroke = getWidth() * 0.013f;
        float inset = stroke * 1.4f;
        canvas.save();
        canvas.translate(0, getScrollY());      // this view's canvas moves with its content
        oval.set(inset, inset, getWidth() - inset, getHeight() - inset);
        paint.setStrokeWidth(stroke);

        paint.setColor(0xFFFFFFFF);
        paint.setAlpha(Math.round(46 * visible));
        canvas.drawArc(oval, -TRACK_SWEEP / 2f, TRACK_SWEEP, false, paint);

        float shown = getHeight() / (float) getChildAt(0).getHeight();
        float thumb = Math.max(9f, TRACK_SWEEP * shown);
        float position = Math.max(0f, Math.min(1f, getScrollY() / (float) range));
        paint.setAlpha(Math.round(225 * visible));
        canvas.drawArc(oval, -TRACK_SWEEP / 2f + (TRACK_SWEEP - thumb) * position, thumb, false, paint);
        canvas.restore();

        postInvalidateOnAnimation();            // keep drawing until the fade finishes
    }
}
