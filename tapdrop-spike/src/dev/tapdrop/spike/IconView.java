package dev.tapdrop.spike;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** A solid circle in the item's colour with a white icon, the way One UI draws list icons. */
public class IconView extends View {
    private final String glyph;
    private final Paint disc = new Paint(Paint.ANTI_ALIAS_FLAG);

    public IconView(Context context, String glyph, int color) {
        super(context);
        this.glyph = glyph;
        disc.setColor(color);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float size = Math.min(getWidth(), getHeight());
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        canvas.drawCircle(cx, cy, size / 2f, disc);
        Icons.draw(canvas, glyph, cx, cy, size * 0.6f, Ui.TEXT);
    }
}
