package dev.tapdrop.spike;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Small view helpers shared by the list and form screens. Framework widgets only. */
final class Ui {
    private static final Typeface BOLD = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);

    private Ui() {}

    static int dp(Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    /**
     * Sets the activity's content to a scrolling column padded for a round screen:
     * extra space above and below lets the first and last rows reach the middle.
     */
    static LinearLayout column(Activity a) {
        ScrollView scroll = new ScrollView(a);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setBackgroundColor(Color.BLACK);
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        DisplayMetrics m = a.getResources().getDisplayMetrics();
        int side = Math.round(m.widthPixels * 0.13f);
        col.setPadding(side, Math.round(m.heightPixels * 0.12f), side, Math.round(m.heightPixels * 0.32f));
        scroll.addView(col, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        a.setContentView(scroll);
        return col;
    }

    static TextView header(Context c, String value, int color) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(13);
        t.setTypeface(BOLD);
        t.setLetterSpacing(0.14f);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, 0, 0, dp(c, 10));
        return t;
    }

    static TextView note(Context c, String value, int color) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(12);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(c, 6), 0, dp(c, 6));
        return t;
    }

    /** A tappable row: a small coloured label over a one-line title. */
    static View row(Context c, String label, int color, String title, Runnable onTap, Runnable onLong) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(dp(c, 18), dp(c, 9), dp(c, 18), dp(c, 10));
        r.setBackground(pill(c, 0xFF1E2024));

        TextView l = new TextView(c);
        l.setText(label);
        l.setTextColor(color);
        l.setTextSize(11);
        l.setTypeface(BOLD);
        l.setLetterSpacing(0.14f);
        r.addView(l);

        TextView t = new TextView(c);
        t.setText(title);
        t.setTextColor(Color.WHITE);
        t.setTextSize(16);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        r.addView(t);

        r.setLayoutParams(spaced(c));
        r.setOnClickListener(v -> onTap.run());
        if (onLong != null) r.setOnLongClickListener(v -> { onLong.run(); return true; });
        return r;
    }

    /** A filled button. */
    static View button(Context c, String label, int fill, int textColor, Runnable onTap) {
        TextView b = new TextView(c);
        b.setText(label);
        b.setTextColor(textColor);
        b.setTextSize(15);
        b.setTypeface(BOLD);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
        b.setBackground(pill(c, fill));
        b.setLayoutParams(spaced(c));
        b.setOnClickListener(v -> onTap.run());
        return b;
    }

    private static RippleDrawable pill(Context c, int fill) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(c, 22));
        return new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), shape, null);
    }

    static LinearLayout.LayoutParams spaced(Context c) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, 7);
        return lp;
    }

    /** Scrolls the column with the bezel. Returns true when the event was a bezel turn. */
    static boolean bezelScroll(View columnOrScroll, MotionEvent ev) {
        if (ev.getAction() != MotionEvent.ACTION_SCROLL || !ev.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) {
            return false;
        }
        View target = columnOrScroll instanceof ScrollView ? columnOrScroll : (View) columnOrScroll.getParent();
        // Clockwise reports a negative scroll value and should move down the list.
        int distance = Math.round(-ev.getAxisValue(MotionEvent.AXIS_SCROLL) * dp(target.getContext(), 64));
        ((ScrollView) target).smoothScrollBy(0, distance);
        return true;
    }
}
