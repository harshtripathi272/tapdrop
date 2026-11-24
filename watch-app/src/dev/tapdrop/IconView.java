package dev.tapdrop;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/**
 * An item's icon on a disc. One UI draws a solid disc in the item's colour with a
 * white icon; Material 3 draws the icon bare, in a light tone of that colour.
 */
public class IconView extends View {
    private final String glyph;
    private final Paint disc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int ink;

    public IconView(Context context, String glyph, int color) {
        super(context);
        this.glyph = glyph;
        Theme t = Theme.of(context);
        disc.setColor(t.disc(color));
        ink = t.glyph(color);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float size = Math.min(getWidth(), getHeight());
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        if (disc.getColor() == 0) {
            Icons.draw(canvas, glyph, cx, cy, size, ink);         // bare, as Material draws it
        } else {
            canvas.drawCircle(cx, cy, size / 2f, disc);
            Icons.draw(canvas, glyph, cx, cy, size * 0.6f, ink);
        }
    }
}
