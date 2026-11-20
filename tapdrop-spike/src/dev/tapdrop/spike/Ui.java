package dev.tapdrop.spike;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * The app's look, modelled on Samsung's One UI for watches: black background,
 * rows grouped in one rounded container with hairline dividers, solid colour
 * icon discs, bold centred titles and a blue accent. Framework widgets only.
 */
final class Ui {
    static final int CONTAINER = 0xFF252527;
    static final int DIVIDER = 0xFF47474B;
    static final int TEXT = 0xFFFFFFFF;
    static final int TEXT_SECONDARY = 0xFFB4B4B8;
    static final int TEXT_TERTIARY = 0xFF8A8A8E;
    static final int ACCENT = 0xFF4D7CFE;
    static final int NEUTRAL = 0xFF46464A;
    static final int DANGER = 0xFFFF6B66;

    static final Typeface BOLD = Typeface.create(Typeface.DEFAULT, 700, false);
    static final Typeface MEDIUM = Typeface.create(Typeface.DEFAULT, 500, false);

    private static final DecelerateInterpolator SETTLE = new DecelerateInterpolator(1.8f);

    private Ui() {}

    static int dp(Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    // ---- screens ---------------------------------------------------------------

    /**
     * Sets the activity's content to a scrolling column for a round screen. Space
     * above and below lets the first and last rows reach the middle. Small
     * standalone pieces shrink and fade near the top and bottom edge; a tall group
     * of rows is left alone, as in Samsung's own lists.
     */
    static LinearLayout column(Activity a) {
        final ArcScrollView scroll = new ArcScrollView(a);
        scroll.setBackgroundColor(Color.BLACK);
        final LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        DisplayMetrics m = a.getResources().getDisplayMetrics();
        int side = Math.round(m.widthPixels * 0.03f);
        col.setPadding(side, Math.round(m.heightPixels * 0.115f), side, Math.round(m.heightPixels * 0.30f));
        scroll.addView(col, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        a.setContentView(scroll);
        scroll.setOnScrollChangeListener((v, x, y, oldX, oldY) -> curve(scroll, col));
        col.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> curve(scroll, col));
        return col;
    }

    private static void curve(ScrollView scroll, LinearLayout col) {
        int height = scroll.getHeight();
        if (height == 0) return;
        float middle = scroll.getScrollY() + height / 2f;
        for (int i = 0; i < col.getChildCount(); i++) {
            View child = col.getChildAt(i);
            if (child.getHeight() > height * 0.4f) continue;
            float center = child.getTop() + child.getHeight() / 2f;
            float distance = Math.min(1f, Math.abs(center - middle) / (height / 2f));
            float eased = distance * distance * distance;
            float scale = 1f - 0.2f * eased;
            child.setTag(scale);                // pressScale returns to this, not to 1
            child.setScaleX(scale);
            child.setScaleY(scale);
        }
    }

    /** Opens another screen with the app's arrive animation. */
    static void go(Activity from, Intent intent) {
        from.startActivity(intent);
        from.overridePendingTransition(R.anim.open_enter, R.anim.open_exit);
    }

    /** Call after finish() so a screen leaves with the matching animation. */
    static void leave(Activity a) {
        a.overridePendingTransition(R.anim.close_enter, R.anim.close_exit);
    }

    /** Brings a group's rows in one after another. */
    static void stagger(ViewGroup group) {
        int shown = 0;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(dp(group.getContext(), 12));
            child.animate().alpha(1f).translationY(0f)
                .setStartDelay(60 + 28L * shown).setDuration(260).setInterpolator(SETTLE).start();
            if (child.getLayoutParams().height != 1) shown++;      // dividers ride along with the row below
        }
    }

    // ---- text ------------------------------------------------------------------

    static TextView title(Context c, String value) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextColor(TEXT);
        t.setTextSize(15);
        t.setTypeface(BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, 0, 0, dp(c, 11));
        return t;
    }

    /** A small heading above a group of rows, like "Paired devices" in Samsung's settings. */
    static TextView section(Context c, String value) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextColor(TEXT_SECONDARY);
        t.setTextSize(13);
        t.setTypeface(BOLD);
        t.setPadding(dp(c, 18), dp(c, 8), dp(c, 18), dp(c, 7));
        return t;
    }

    /** A short explanation in the quieter text colour, left-aligned like One UI's. */
    static TextView body(Context c, String value, int color) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(13);
        t.setLineSpacing(dp(c, 2), 1f);
        t.setPadding(dp(c, 18), dp(c, 2), dp(c, 18), dp(c, 10));
        return t;
    }

    // ---- rows ------------------------------------------------------------------

    /** One rounded container that holds several rows. */
    static LinearLayout group(Context c) {
        LinearLayout g = new LinearLayout(c);
        g.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(CONTAINER);
        shape.setCornerRadius(dp(c, 24));
        g.setBackground(shape);
        g.setClipToOutline(true);               // keeps a row's press highlight inside the corners
        g.setLayoutParams(spaced(c, 8));
        return g;
    }

    /** Adds a row to a group, with a hairline above every row but the first. */
    static void addRow(LinearLayout group, View row) {
        Context c = group.getContext();
        if (group.getChildCount() > 0) {
            View line = new View(c);
            line.setBackgroundColor(DIVIDER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
            lp.leftMargin = dp(c, 16);
            lp.rightMargin = dp(c, 16);
            group.addView(line, lp);
        }
        group.addView(row);
    }

    /** A row for a group: icon disc, title, and an optional quieter second line. */
    static View row(Context c, String glyph, int color, String titleText, String subtitleText,
                    Runnable onTap, Runnable onLong) {
        LinearLayout r = rowShell(c, glyph, color, titleText, subtitleText);
        r.setBackground(new RippleDrawable(ColorStateList.valueOf(0x26FFFFFF), null, new ColorDrawable(Color.WHITE)));
        r.setOnClickListener(v -> onTap.run());
        if (onLong != null) r.setOnLongClickListener(v -> { onLong.run(); return true; });
        return r;
    }

    /** A row that stands alone as its own pill, such as "Add". */
    static View card(Context c, String glyph, int color, String titleText, Runnable onTap) {
        LinearLayout r = rowShell(c, glyph, color, titleText, null);
        r.setBackground(pill(c, CONTAINER, 30));
        r.setLayoutParams(spaced(c, 8));
        r.setOnClickListener(v -> onTap.run());
        pressScale(r);
        return r;
    }

    private static LinearLayout rowShell(Context c, String glyph, int color, String titleText, String subtitleText) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(c, 11), dp(c, 11), dp(c, 14), dp(c, 11));
        r.setMinimumHeight(dp(c, 52));

        r.addView(new IconView(c, glyph, color), dp(c, 30), dp(c, 30));

        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(c, 10), 0, 0, 0);

        TextView t = new TextView(c);
        t.setText(titleText);
        t.setTextColor(TEXT);
        t.setTextSize(15);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(t);

        if (subtitleText != null && !subtitleText.isEmpty()) {
            TextView s = new TextView(c);
            s.setText(subtitleText);
            s.setTextColor(TEXT_SECONDARY);
            s.setTextSize(12);
            s.setSingleLine(true);
            s.setEllipsize(TextUtils.TruncateAt.END);
            texts.addView(s);
        }
        r.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return r;
    }

    // ---- forms -----------------------------------------------------------------

    /** A screen heading with the kind's icon above its name. */
    static View heading(Context c, String glyph, int color, String name) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.addView(new IconView(c, glyph, color), dp(c, 30), dp(c, 30));
        TextView t = title(c, name);
        t.setPadding(0, dp(c, 5), 0, dp(c, 12));
        box.addView(t);
        return box;
    }

    /** A labelled text field with a filled background in place of the stock underline. */
    static View field(Context c, String label, EditText input) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);

        TextView l = new TextView(c);
        l.setText(label);
        l.setTextColor(TEXT_SECONDARY);
        l.setTextSize(12);
        l.setTypeface(MEDIUM);
        l.setPadding(dp(c, 18), 0, dp(c, 18), dp(c, 4));
        box.addView(l);

        input.setTextColor(TEXT);
        input.setHintTextColor(TEXT_TERTIARY);
        input.setTextSize(15);
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setPadding(dp(c, 18), dp(c, 12), dp(c, 18), dp(c, 12));
        input.setBackground(pill(c, CONTAINER, 24));
        input.setHighlightColor((ACCENT & 0x00FFFFFF) | 0x66000000);
        box.addView(input, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        box.setLayoutParams(spaced(c, 10));
        return box;
    }

    // ---- buttons ---------------------------------------------------------------

    /** The main action on a screen: a filled pill in the accent blue. */
    static View primaryButton(Context c, String label, Runnable onTap) {
        TextView b = textButton(c, label, TEXT, onTap);
        b.setTypeface(BOLD);
        b.setBackground(pill(c, ACCENT, 26));
        pressScale(b);
        return b;
    }

    /** A quieter action on the container colour. */
    static View secondaryButton(Context c, String label, int textColor, Runnable onTap) {
        TextView b = textButton(c, label, textColor, onTap);
        b.setBackground(pill(c, CONTAINER, 26));
        pressScale(b);
        return b;
    }

    /** A bare text action, used for delete. */
    static TextView textButton(Context c, String label, int textColor, Runnable onTap) {
        TextView b = new TextView(c);
        b.setText(label);
        b.setTextColor(textColor);
        b.setTextSize(15);
        b.setTypeface(MEDIUM);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(c, 16), dp(c, 13), dp(c, 16), dp(c, 13));
        b.setLayoutParams(spaced(c, 8));
        b.setOnClickListener(v -> onTap.run());
        return b;
    }

    /** Shrinks a view slightly while a finger is on it. */
    static void pressScale(View view) {
        view.setOnTouchListener((v, e) -> {
            float rest = v.getTag() instanceof Float ? (Float) v.getTag() : 1f;
            int action = e.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(rest * 0.95f).scaleY(rest * 0.95f).setDuration(90).start();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(rest).scaleY(rest).setDuration(180).setInterpolator(SETTLE).start();
            }
            return false;       // let the click through
        });
    }

    static RippleDrawable pill(Context c, int fill, float radiusDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(c, radiusDp));
        return new RippleDrawable(ColorStateList.valueOf(0x30FFFFFF), shape, null);
    }

    static LinearLayout.LayoutParams spaced(Context c, float bottomDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, bottomDp);
        return lp;
    }

    /** Scrolls the column with the bezel. Returns true when the event was a bezel turn. */
    static boolean bezelScroll(LinearLayout column, MotionEvent ev) {
        if (ev.getAction() != MotionEvent.ACTION_SCROLL || !ev.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) {
            return false;
        }
        ScrollView scroll = (ScrollView) column.getParent();
        // Clockwise reports a negative scroll value and should move down the list.
        scroll.smoothScrollBy(0, Math.round(-ev.getAxisValue(MotionEvent.AXIS_SCROLL) * dp(column.getContext(), 60)));
        return true;
    }
}
