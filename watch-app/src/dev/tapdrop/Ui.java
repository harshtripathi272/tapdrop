package dev.tapdrop;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Builds the app's screens from framework widgets, in the look of the current
 * {@link Theme}: One UI or Material 3 Expressive. Screens call these helpers and
 * never pick colours or shapes themselves, so both looks come from one layout.
 */
final class Ui {
    private Ui() {}

    static Theme theme(Context c) {
        return Theme.of(c);
    }

    static int dp(Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    // ---- screens ---------------------------------------------------------------

    /**
     * Sets the activity's content to a scrolling column for a round screen. Space
     * above and below lets the first and last rows reach the middle.
     *
     * Near the top and bottom edge, One UI shrinks small standalone pieces and leaves
     * a tall group of rows alone, as in Samsung's own lists. Material 3 narrows every
     * row on its own, as Wear OS's TransformingLazyColumn does.
     */
    static LinearLayout column(Activity a) {
        Theme.refresh(a);                       // the watch's dynamic colours may have changed
        Theme t = theme(a);
        a.getWindow().getDecorView().setBackgroundColor(t.background);
        final ArcScrollView scroll = new ArcScrollView(a);
        scroll.setBackgroundColor(t.background);
        final LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        DisplayMetrics m = a.getResources().getDisplayMetrics();
        // Material keeps rows inside the round screen's safe area, about 5% each side.
        int side = Math.round(m.widthPixels * (t.morphRows ? 0.052f : 0.03f));
        col.setPadding(side, Math.round(m.heightPixels * 0.115f), side, Math.round(m.heightPixels * 0.30f));
        scroll.addView(col, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        FrameLayout root = new FrameLayout(a);              // room for a pinned edge button
        root.addView(scroll, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        a.setContentView(root);
        scroll.setOnScrollChangeListener((v, x, y, oldX, oldY) -> curve(scroll, col));
        col.addOnLayoutChangeListener((v, l, top, r, b, ol, ot, or, ob) -> curve(scroll, col));
        return col;
    }

    private static void curve(ScrollView scroll, LinearLayout col) {
        int height = scroll.getHeight();
        if (height == 0) return;
        boolean morph = theme(col.getContext()).morphRows;
        float middle = scroll.getScrollY() + height / 2f;
        for (int i = 0; i < col.getChildCount(); i++) {
            View child = col.getChildAt(i);
            if (morph && child instanceof Group) {
                ViewGroup group = (ViewGroup) child;
                for (int j = 0; j < group.getChildCount(); j++) {
                    View row = group.getChildAt(j);
                    morph(row, child.getTop() + row.getTop() + row.getHeight() / 2f, middle, height);
                }
            } else if (morph) {
                morph(child, child.getTop() + child.getHeight() / 2f, middle, height);
            } else {
                if (child.getHeight() > height * 0.4f) continue;
                float center = child.getTop() + child.getHeight() / 2f;
                float distance = Math.min(1f, Math.abs(center - middle) / (height / 2f));
                float eased = distance * distance * distance;
                float scale = 1f - 0.2f * eased;
                child.setTag(scale);                // press returns to this, not to 1
                child.setScaleX(scale);
                child.setScaleY(scale);
            }
        }
    }

    /** Material's edge morph: a row narrows and flattens a little as it nears the rim. */
    private static void morph(View row, float center, float middle, int height) {
        float distance = Math.min(1f, Math.abs(center - middle) / (height / 2f));
        float edge = Math.max(0f, (distance - 0.45f) / 0.55f);     // only the outer part of the screen
        float eased = edge * edge;
        float scale = 1f - 0.24f * eased;
        row.setTag(scale);
        row.setScaleX(scale);
        row.setScaleY(1f - 0.12f * eased);
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
        Theme t = theme(group.getContext());
        int shown = 0;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(dp(group.getContext(), 12));
            child.animate().alpha(1f).translationY(0f)
                .setStartDelay(60 + 28L * shown).setDuration(t.settleMs).setInterpolator(t.settle).start();
            if (child.getLayoutParams().height != 1) shown++;      // dividers ride along with the row below
        }

    }

    // ---- text ------------------------------------------------------------------

    static TextView title(Context c, String value) {
        Theme th = theme(c);
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextColor(th.text);
        t.setTextSize(th.titleSize);
        t.setTypeface(th.bold);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, 0, 0, dp(c, 11));
        return t;
    }

    /**
     * The home screen's heading. One UI writes the app's name. Material puts the app's
     * round icon beside the name, as a Wear app's first screen often does.
     */
    static View appTitle(Context c, String name) {
        Theme th = theme(c);
        TextView t = title(c, name);
        if (!th.bareIcons) return t;
        t.setPadding(dp(c, 6), 0, 0, 0);
        ImageView icon = new ImageView(c);
        icon.setImageResource(R.mipmap.ic_launcher);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);    // the name says it
        icon.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline outline) { outline.setOval(0, 0, v.getWidth(), v.getHeight()); }
        });
        icon.setClipToOutline(true);
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(0, 0, 0, dp(c, 11));
        box.addView(icon, dp(c, 24), dp(c, 24));
        box.addView(t);
        return box;
    }

    /** A small heading above a group of rows, like "Paired devices" in Samsung's settings. */
    static TextView section(Context c, String value) {
        Theme th = theme(c);
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextColor(th.sectionColor);
        t.setTextSize(th.sectionSize);
        t.setTypeface(th.bold);
        if (th.groupedRows) {
            t.setPadding(dp(c, 18), dp(c, 8), dp(c, 18), dp(c, 7));
        } else {
            // Material's ListSubHeader: centred, with room above it.
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(c, 14), dp(c, 14), dp(c, 14), dp(c, 8));
        }
        return t;
    }

    /** A short explanation in a quieter text colour. */
    static TextView body(Context c, String value, int color) {
        Theme th = theme(c);
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(th.bodySize);
        t.setTypeface(th.regular);
        t.setLineSpacing(dp(c, 2), 1f);
        t.setPadding(dp(c, 18), dp(c, 2), dp(c, 18), dp(c, 10));
        return t;
    }

    // ---- rows ------------------------------------------------------------------

    /** Marks a group of rows, so the edge morph can reach the rows inside it. */
    private static final class Group extends LinearLayout {
        Group(Context c) { super(c); }
    }

    /**
     * Holds several rows. In One UI it is one rounded container; in Material each
     * row is its own button and the group only spaces them.
     */
    static LinearLayout group(Context c) {
        Theme t = theme(c);
        LinearLayout g = new Group(c);
        g.setOrientation(LinearLayout.VERTICAL);
        if (t.groupedRows) {
            GradientDrawable shape = new GradientDrawable();
            shape.setColor(t.container);
            shape.setCornerRadius(dp(c, t.groupRadius));
            g.setBackground(shape);
            g.setClipToOutline(true);           // keeps a row's press highlight inside the corners
        } else {
            g.setClipChildren(false);           // rows scale and morph past their slot
        }
        g.setLayoutParams(spaced(c, 8));
        return g;
    }

    /** Adds a row to a group: below a hairline in One UI, below a small gap in Material. */
    static void addRow(LinearLayout group, View row) {
        Context c = group.getContext();
        Theme t = theme(c);
        if (group.getChildCount() > 0 && t.groupedRows) {
            View line = new View(c);
            line.setBackgroundColor(t.divider);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
            lp.leftMargin = dp(c, 16);
            lp.rightMargin = dp(c, 16);
            group.addView(line, lp);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (group.getChildCount() > 0) lp.topMargin = dp(c, t.rowGap);
        group.addView(row, lp);
    }

    /** A row for a group: icon disc, title, and an optional quieter second line. */
    static View row(Context c, String glyph, int color, String titleText, String subtitleText,
                    Runnable onTap, Runnable onLong) {
        Theme t = theme(c);
        LinearLayout r = rowShell(c, glyph, color, titleText, subtitleText);
        if (t.groupedRows) {
            r.setBackground(new RippleDrawable(ColorStateList.valueOf(t.ripple), null, new ColorDrawable(0xFFFFFFFF)));
        } else {
            r.setBackground(pill(c, t.container, t.rowRadius));
            press(r);
        }
        r.setOnClickListener(v -> onTap.run());
        if (onLong != null) r.setOnLongClickListener(v -> { onLong.run(); return true; });
        return r;
    }

    /** A row that stands alone as its own pill, such as "Quick tap". */
    static View card(Context c, String glyph, int color, String titleText, String subtitleText, Runnable onTap) {
        Theme t = theme(c);
        LinearLayout r = rowShell(c, glyph, color, titleText, subtitleText);
        r.setBackground(pill(c, t.container, t.groupedRows ? 30 : t.rowRadius));
        r.setLayoutParams(spaced(c, 8));
        r.setOnClickListener(v -> onTap.run());
        press(r);
        return r;
    }

    /**
     * One of several choices, such as which item quick tap shares. One UI marks the
     * chosen row with a tick in the accent colour. Material uses a radio row: the
     * chosen row fills with primaryContainer and a radio sits at its end.
     */
    static View choice(Context c, String glyph, int color, String titleText, String subtitleText,
                       boolean selected, Runnable onTap) {
        Theme t = theme(c);
        if (!t.controls) {
            return row(c, selected ? Icons.CHECK : glyph, selected ? t.accent : color,
                titleText, subtitleText, onTap, null);
        }
        LinearLayout r = rowShell(c, glyph, selected ? t.accent : color, titleText, subtitleText);
        selectedText(r, selected, t);
        r.addView(new RadioMark(c, selected), dp(c, 22), dp(c, 22));
        r.setBackground(pill(c, selected ? t.accentContainer : t.container, t.rowRadius));
        r.setOnClickListener(v -> onTap.run());
        press(r);
        return r;
    }

    /**
     * An on/off setting. One UI shows "Turn on" as the main button, or "Turn off" as a
     * quieter one. Material shows a switch row, filled with primaryContainer while on;
     * the switch slides over before flip runs.
     */
    static View toggle(Context c, String label, String detail, boolean on, Runnable flip) {
        Theme t = theme(c);
        if (!t.controls) {
            return on ? secondaryButton(c, "Turn off", t.accent, flip) : primaryButton(c, "Turn on", flip);
        }
        LinearLayout r = rowShell(c, null, 0, label, detail);
        selectedText(r, on, t);
        final SwitchMark mark = new SwitchMark(c, on);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(c, 34), dp(c, 22));
        lp.leftMargin = dp(c, 6);
        r.addView(mark, lp);
        r.setBackground(pill(c, on ? t.accentContainer : t.container, t.rowRadius));
        r.setLayoutParams(spaced(c, 8));
        r.setOnClickListener(v -> {
            v.setClickable(false);
            ValueAnimator a = ValueAnimator.ofFloat(on ? 1f : 0f, on ? 0f : 1f);
            a.setDuration(t.settleMs);
            a.setInterpolator(t.settle);
            a.addUpdateListener(x -> { mark.progress = (Float) x.getAnimatedValue(); mark.invalidate(); });
            a.start();
            v.postDelayed(flip, 220);
        });
        press(r);
        return r;
    }

    /** Recolours a row's lines for a chosen or switched-on row. */
    private static void selectedText(LinearLayout row, boolean selected, Theme t) {
        if (!selected) return;
        LinearLayout texts = (LinearLayout) row.getChildAt(row.getChildCount() - 1);
        ((TextView) texts.getChildAt(0)).setTextColor(t.onAccentContainer);
        if (texts.getChildCount() > 1) {
            ((TextView) texts.getChildAt(1)).setTextColor((t.onAccentContainer & 0x00FFFFFF) | 0xE6000000);
        }
    }

    /** Material's radio: a ring, with a dot inside when chosen. */
    private static final class RadioMark extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean selected;

        RadioMark(Context c, boolean selected) {
            super(c);
            this.selected = selected;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            Theme t = theme(getContext());
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float stroke = dp(getContext(), 2);
            float ring = dp(getContext(), 9);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(selected ? t.accent : t.outline);
            canvas.drawCircle(cx, cy, ring, paint);
            if (selected) {
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx, cy, dp(getContext(), 5), paint);
            }
        }
    }

    /**
     * Material's switch: a track that fills with primary when on, and a thumb that
     * grows and carries a tick when on. progress runs 0 (off) to 1 (on).
     */
    private static final class SwitchMark extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        float progress;

        SwitchMark(Context c, boolean on) {
            super(c);
            progress = on ? 1f : 0f;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            Theme t = theme(getContext());
            Context c = getContext();
            float w = getWidth(), h = getHeight();
            float p = Math.max(0f, Math.min(1.1f, progress));       // the spring may overshoot a little
            float on = Math.min(1f, p);
            float stroke = dp(c, 2);
            float r = h / 2f;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Theme.mix(t.accent, t.container, on));
            canvas.drawRoundRect(0, 0, w, h, r, r, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(Theme.mix(t.accent, t.outline, on));
            canvas.drawRoundRect(stroke / 2f, stroke / 2f, w - stroke / 2f, h - stroke / 2f, r, r, paint);

            float thumb = dp(c, 6) + dp(c, 3) * on;                 // radius: small when off, larger when on
            float x0 = r, x1 = w - r;
            float cx = x0 + (x1 - x0) * p;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Theme.mix(t.accentContainer, t.outline, on));
            canvas.drawCircle(cx, h / 2f, thumb, paint);
            if (on > 0.5f) Icons.draw(canvas, Icons.CHECK, cx, h / 2f, thumb * 1.5f, t.accent);
        }
    }

    private static LinearLayout rowShell(Context c, String glyph, int color, String titleText, String subtitleText) {
        Theme th = theme(c);
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        if (th.groupedRows) r.setPadding(dp(c, 11), dp(c, 11), dp(c, 14), dp(c, 11));
        else r.setPadding(dp(c, 14), dp(c, 8), dp(c, 14), dp(c, 8));     // Wear M3 Button content padding
        r.setMinimumHeight(dp(c, th.rowHeight));

        if (glyph != null) r.addView(new IconView(c, glyph, color), dp(c, th.iconSize), dp(c, th.iconSize));

        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(glyph == null ? dp(c, 4) : dp(c, th.bareIcons ? 8 : 10), 0, 0, 0);

        TextView t = new TextView(c);
        t.setText(titleText);
        t.setTextColor(th.text);
        t.setTextSize(th.labelSize);
        if (!th.groupedRows) t.setTypeface(th.medium);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(t);

        if (subtitleText != null && !subtitleText.isEmpty()) {
            TextView s = new TextView(c);
            s.setText(subtitleText);
            s.setTextColor(th.textSecondary);
            s.setTextSize(th.secondaryLabelSize);
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
        Theme th = theme(c);
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.addView(new IconView(c, glyph, color), dp(c, th.iconSize), dp(c, th.iconSize));
        TextView t = title(c, name);
        t.setPadding(0, dp(c, 5), 0, dp(c, 12));
        box.addView(t);
        return box;
    }

    /** A labelled text field with a filled background in place of the stock underline. */
    static View field(Context c, String label, EditText input) {
        Theme th = theme(c);
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);

        TextView l = new TextView(c);
        l.setText(label);
        l.setTextColor(th.textSecondary);
        l.setTextSize(12);
        l.setTypeface(th.medium);
        l.setPadding(dp(c, 18), 0, dp(c, 18), dp(c, 4));
        box.addView(l);

        input.setTextColor(th.text);
        input.setHintTextColor(th.textTertiary);
        input.setTextSize(15);
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setPadding(dp(c, 18), dp(c, 12), dp(c, 18), dp(c, 12));
        input.setBackground(pill(c, th.container, th.fieldRadius));
        input.setHighlightColor((th.accent & 0x00FFFFFF) | 0x66000000);
        box.addView(input, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        box.setLayoutParams(spaced(c, 10));
        return box;
    }

    // ---- buttons ---------------------------------------------------------------

    /** The main action on a screen: a filled pill in the accent colour. */
    static View primaryButton(Context c, String label, Runnable onTap) {
        Theme t = theme(c);
        TextView b = textButton(c, label, t.onAccent, onTap);
        b.setTypeface(t.bold);
        b.setBackground(pill(c, t.accent, t.groupedRows ? 26 : t.rowRadius));
        press(b);
        return b;
    }

    /** A quieter action on the container colour. */
    static View secondaryButton(Context c, String label, int textColor, Runnable onTap) {
        Theme t = theme(c);
        TextView b = textButton(c, label, textColor, onTap);
        b.setBackground(pill(c, t.container, t.groupedRows ? 26 : t.rowRadius));
        press(b);
        return b;
    }

    /** A bare text action, used for delete. */
    static TextView textButton(Context c, String label, int textColor, Runnable onTap) {
        Theme t = theme(c);
        TextView b = new TextView(c);
        b.setText(label);
        b.setTextColor(textColor);
        b.setTextSize(t.labelSize);
        b.setTypeface(t.medium);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(c, 16), dp(c, 13), dp(c, 16), dp(c, 13));
        if (!t.groupedRows) b.setMinHeight(dp(c, t.rowHeight));
        b.setLayoutParams(spaced(c, 8));
        b.setOnClickListener(v -> onTap.run());
        return b;
    }

    /**
     * A screen's one action pinned to the bottom of the round screen, such as Edit on
     * the share screen. One UI shows a small pill above the edge. Material shows an
     * EdgeButton: its bottom follows the curve of the screen.
     */
    static View bottomButton(Activity a, String label, Runnable onTap) {
        Theme t = theme(a);
        if (t.edgeButton) return edge(a, label, onTap, 46, false);  // ExtraSmall and tonal: a quiet way out
        TextView b = new TextView(a);
        b.setText(label);
        b.setOnClickListener(v -> onTap.run());
        b.setGravity(Gravity.CENTER);
        b.setTextColor(t.text);
        b.setTextSize(13);
        b.setTypeface(t.medium);
        b.setPadding(dp(a, 22), dp(a, 8), dp(a, 22), dp(a, 8));
        b.setBackground(pill(a, t.container, 20));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = Math.round(a.getResources().getDisplayMetrics().heightPixels * 0.08f);
        b.setLayoutParams(lp);
        press(b);
        return b;
    }

    /**
     * A screen's main action, such as Save. One UI puts a filled button in the list at
     * this point. Material pins an EdgeButton to the bottom of the screen that grows in
     * as the list reaches its end and shrinks away as the list scrolls back up, as
     * Wear's ScreenScaffold does. Call after Ui.column.
     */
    static void mainAction(Activity a, LinearLayout column, String label, Runnable onTap) {
        if (!theme(a).edgeButton) {
            column.addView(primaryButton(a, label, onTap));
            return;
        }
        final View b = edge(a, label, onTap, 56, true);
        final ScrollView scroll = (ScrollView) column.getParent();
        ((FrameLayout) scroll.getParent()).addView(b);
        final Runnable follow = () -> {
            int height = b.getHeight();
            if (height == 0) return;
            int end = Math.max(0, column.getHeight() - scroll.getHeight());
            float away = end - scroll.getScrollY();
            float shown = 1f - Math.min(1f, Math.max(0f, away / height));
            b.setPivotX(b.getWidth() / 2f);
            b.setPivotY(height);
            b.setScaleY(shown);
            b.setScaleX(0.85f + 0.15f * shown);
            b.setAlpha(shown);
            b.setVisibility(shown < 0.05f ? View.INVISIBLE : View.VISIBLE);
        };
        scroll.setOnScrollChangeListener((v, x, y, oldX, oldY) -> {
            curve(scroll, column);
            follow.run();
        });
        column.addOnLayoutChangeListener((v, l, top, r, bottom, ol, ot, or, ob) -> follow.run());
        b.addOnLayoutChangeListener((v, l, top, r, bottom, ol, ot, or, ob) -> follow.run());
    }

    /**
     * An EdgeButton for the bottom of a FrameLayout that fills the screen, in one of
     * Wear's heights: 46 dp (ExtraSmall), 56 (Small), 70 (Medium) or 96 (Large). Filled
     * in primary for a screen's main action, or tonal on surfaceContainer for a quieter one.
     */
    private static TextView edge(Activity a, String label, Runnable onTap, float heightDp, boolean filled) {
        Theme t = theme(a);
        TextView b = new TextView(a);
        b.setText(label);
        b.setOnClickListener(v -> onTap.run());
        b.setGravity(Gravity.CENTER);

        // Wear M3's EdgeButton: 3 dp above the rim, and as wide as the curve allows at
        // its height, with 20 dp to spare on each side.
        DisplayMetrics m = a.getResources().getDisplayMetrics();
        float screen = m.widthPixels;
        float height = dp(a, heightDp);
        float gap = dp(a, 3);
        float ellipsis = dp(a, 58) + (height - dp(a, 46)) * 1.42f;
        float half = screen / 2f;
        float reach = half - gap - ellipsis / 2f;
        float side = half - (float) Math.sqrt(Math.max(0f, half * half - reach * reach)) + dp(a, 20);

        b.setTextColor(filled ? t.onAccent : t.text);
        b.setTextSize(t.labelSize);
        b.setTypeface(t.bold);
        b.setPadding(0, dp(a, 6), 0, dp(a, 8));
        EdgeShape shape = new EdgeShape(filled ? t.accent : t.container, dp(a, t.rowRadius), half, m.heightPixels / 2f, gap);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(filled ? 0x33000000 : t.pillRipple), shape, null));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Math.round(height), Gravity.BOTTOM);
        lp.leftMargin = Math.round(side);
        lp.rightMargin = Math.round(side);
        lp.bottomMargin = Math.round(gap);
        b.setLayoutParams(lp);
        press(b);
        return b;
    }

    /**
     * The EdgeButton's outline: rounded top corners, and a bottom cut by the screen's
     * circle so the button hugs the rim.
     */
    private static final class EdgeShape extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final Path circle = new Path();
        private final float corner, screenHalf, screenMiddle, gap;

        EdgeShape(int color, float corner, float screenHalf, float screenMiddle, float gap) {
            paint.setColor(color);
            this.corner = corner;
            this.screenHalf = screenHalf;
            this.screenMiddle = screenMiddle;
            this.gap = gap;
        }

        @Override
        protected void onBoundsChange(Rect b) {
            path.rewind();
            float r = corner;
            path.addRoundRect(b.left, b.top, b.right, b.bottom + r,
                new float[] {r, r, r, r, 0, 0, 0, 0}, Path.Direction.CW);
            // The screen's circle in this view's coordinates, pulled in by the gap.
            circle.rewind();
            circle.addCircle(b.exactCenterX(), b.bottom + gap - screenMiddle, screenHalf - gap, Path.Direction.CW);
            path.op(circle, Path.Op.INTERSECT);
        }

        @Override public void draw(Canvas canvas) { canvas.drawPath(path, paint); }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        @Override public void getOutline(Outline outline) { outline.setPath(path); }
    }

    /**
     * Answers a finger on a view. One UI shrinks it slightly. Material 3 Expressive
     * also morphs its corners tighter, then springs back when the finger lifts.
     */
    static void press(View view) {
        Theme t = theme(view.getContext());
        final GradientDrawable shape = shapeOf(view);
        final float rounded = dp(view.getContext(), t.rowRadius);
        final float pressed = dp(view.getContext(), t.pressedRadius);
        final boolean morph = !t.groupedRows && shape != null;
        view.setOnTouchListener((v, e) -> {
            float rest = v.getTag() instanceof Float ? (Float) v.getTag() : 1f;
            int action = e.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                float s = morph ? 0.97f : 0.95f;
                v.animate().scaleX(rest * s).scaleY(rest * s).setDuration(90).setInterpolator(null).start();
                if (morph) corners(shape, pressed, 120, null);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(rest).scaleY(rest).setDuration(morph ? t.settleMs : 180)
                    .setInterpolator(t.settle).start();
                if (morph) corners(shape, rounded, t.settleMs, t.settle);
            }
            return false;       // let the click through
        });
    }

    private static GradientDrawable shapeOf(View v) {
        if (v.getBackground() instanceof RippleDrawable) {
            RippleDrawable r = (RippleDrawable) v.getBackground();
            if (r.getNumberOfLayers() > 0 && r.getDrawable(0) instanceof GradientDrawable) {
                return (GradientDrawable) r.getDrawable(0);
            }
        }
        return null;
    }

    private static void corners(GradientDrawable shape, float to, long ms, android.animation.TimeInterpolator curve) {
        ValueAnimator a = ValueAnimator.ofFloat(shape.getCornerRadius(), to);
        a.setDuration(ms);
        if (curve != null) a.setInterpolator(curve);
        a.addUpdateListener(v -> shape.setCornerRadius((Float) v.getAnimatedValue()));
        a.start();
    }

    static RippleDrawable pill(Context c, int fill, float radiusDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(c, radiusDp));
        return new RippleDrawable(ColorStateList.valueOf(theme(c).pillRipple), shape, null);
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
