package dev.tapdrop;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.fonts.Font;
import android.graphics.fonts.SystemFonts;
import android.os.Build;
import android.provider.Settings;
import android.view.animation.DecelerateInterpolator;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * The app's look as a set of values that Ui, IconView, CardView and ArcScrollView
 * read, so the whole app can be drawn in either of two styles:
 *
 * One UI, after Samsung's watch apps: rows grouped in one rounded container with
 * hairline dividers, solid colour icon discs with white icons, and a blue accent.
 *
 * Material 3 Expressive, after Wear OS's Material 3 (androidx.wear.compose.material3's
 * tokens): every row its own 52 dp button, bare icons, radio and switch rows, Roboto
 * Flex, buttons that morph their corners when pressed, springy motion, edge-hugging
 * buttons, and rows that narrow towards the top and bottom of the screen. It takes the
 * watch's dynamic colours when the wearer has them on, and otherwise a blue scheme
 * made from the app icon's blue.
 *
 * The wearer can pick either. Until they do, a Samsung watch gets One UI and any other
 * watch gets Material 3, so the app matches the rest of the watch.
 */
final class Theme {
    static final String AUTO = "auto";
    static final String ONE_UI = "oneui";
    static final String MATERIAL = "material";

    static final String ONE_UI_NAME = "One UI";
    static final String MATERIAL_NAME = "Material 3 Expressive";

    private static final String KEY_THEME = "theme";

    final String id;
    final String name;
    final String shortName;         // for a row's second line

    // ---- colour ----------------------------------------------------------------
    final int background;
    final int container;            // rows, cards, fields and quiet buttons
    final int divider;
    final int text;
    final int textSecondary;
    final int textTertiary;
    final int accent;               // the main action; Material's primary
    final int onAccent;             // text on an accent fill
    final int accentContainer;      // a chosen or switched-on row
    final int onAccentContainer;
    final int outline;              // an unselected radio or switch
    final int neutral;              // an icon with no kind colour
    final int danger;
    final int ripple;               // a row inside a group
    final int pillRipple;           // a button or standalone row
    final int scrollTrack;
    final int scrollThumb;

    // ---- type ------------------------------------------------------------------
    final Typeface bold;            // titles
    final Typeface medium;          // labels and buttons
    final Typeface regular;
    final float titleSize;
    final float sectionSize;
    final int sectionColor;
    final float labelSize;          // a row's or button's main line
    final float secondaryLabelSize;
    final float bodySize;

    // ---- shape -----------------------------------------------------------------
    /** True for One UI's single container per group, false for one button per row. */
    final boolean groupedRows;
    final float groupRadius;
    final float rowRadius;          // a standalone row or button
    final float pressedRadius;      // a button's corners while a finger is on it
    final float fieldRadius;
    final float rowHeight;
    final float rowGap;             // between rows of a group that are not grouped
    final float iconSize;
    /** Icons drawn bare in a tone of their colour, as Material does, instead of on a disc. */
    final boolean bareIcons;
    /** Choices and on/off settings use radio and switch rows instead of ticks and buttons. */
    final boolean controls;

    // ---- motion ----------------------------------------------------------------
    final TimeInterpolator settle;
    final long settleMs;
    /** Rows morph near the screen's top and bottom edge, as in TransformingLazyColumn. */
    final boolean morphRows;
    /** A screen's main action is an edge-hugging button at the bottom of the screen. */
    final boolean edgeButton;
    /** The share screen shows a wavy progress ring and a morphing icon shape, not One UI's ripples. */
    final boolean wavyProgress;

    private Theme(Builder b) {
        id = b.id; name = b.name; shortName = b.shortName;
        background = b.background; container = b.container; divider = b.divider;
        text = b.text; textSecondary = b.textSecondary; textTertiary = b.textTertiary;
        accent = b.accent; onAccent = b.onAccent; accentContainer = b.accentContainer;
        onAccentContainer = b.onAccentContainer; outline = b.outline;
        neutral = b.neutral; danger = b.danger;
        ripple = b.ripple; pillRipple = b.pillRipple; scrollTrack = b.scrollTrack; scrollThumb = b.scrollThumb;
        bold = b.bold; medium = b.medium; regular = b.regular;
        titleSize = b.titleSize; sectionSize = b.sectionSize; sectionColor = b.sectionColor;
        labelSize = b.labelSize; secondaryLabelSize = b.secondaryLabelSize; bodySize = b.bodySize;
        groupedRows = b.groupedRows; groupRadius = b.groupRadius; rowRadius = b.rowRadius;
        pressedRadius = b.pressedRadius; fieldRadius = b.fieldRadius; rowHeight = b.rowHeight;
        rowGap = b.rowGap; iconSize = b.iconSize; bareIcons = b.bareIcons; controls = b.controls;
        settle = b.settle; settleMs = b.settleMs; morphRows = b.morphRows; edgeButton = b.edgeButton;
        wavyProgress = b.wavyProgress;
    }

    /** The disc behind an icon in the given colour, or 0 for none. */
    int disc(int color) {
        return bareIcons ? 0 : color;
    }

    /**
     * The icon itself. One UI draws it white on the disc. Material draws it bare: in
     * primary when it has no colour of its own, else in a light tone of its colour.
     */
    int glyph(int color) {
        if (!bareIcons) return text;
        if (color == neutral || color == accent) return accent;
        return mix(color, 0xFFFFFFFF, 0.5f);
    }

    /** A ring or other line drawn straight on the background in the given colour. */
    int stroke(int color) {
        return bareIcons ? mix(color, 0xFFFFFFFF, 0.6f) : color;
    }

    // ---- the two themes --------------------------------------------------------

    static final Theme ONE_UI_THEME = new Builder(ONE_UI, ONE_UI_NAME, "One UI")
        .colors(0xFF000000, 0xFF252527, 0xFF47474B, 0xFFFFFFFF, 0xFFB4B4B8, 0xFF8A8A8E,
            0xFF4D7CFE, 0xFFFFFFFF, 0xFF46464A, 0xFFFF6B66)
        .containers(0xFF4D7CFE, 0xFFFFFFFF, 0xFF8A8A8E)
        .ripple(0x26FFFFFF, 0x30FFFFFF).scroll(0x2EFFFFFF, 0xE1FFFFFF)
        .type(weight(700), weight(500), Typeface.DEFAULT, 15, 13, 0xFFB4B4B8, 15, 12, 13)
        .shape(true, 24, 30, 30, 24, 52, 0, 30)
        .motion(new DecelerateInterpolator(1.8f), 260, false, false)
        .build();

    /**
     * Material 3 Expressive. Sizes, weights and shapes are androidx.wear.compose.material3's
     * tokens. Colours are the watch's dynamic colours when it has them, else the Wear
     * baseline scheme turned to the app icon's blue (#4D7CFE): each baseline tone keeps
     * its lightness and colourfulness and takes the icon's hue. Primary sits a step
     * deeper than baseline tone 90, so it reads as blue rather than near-white.
     */
    private static Theme material(Context c) {
        Builder b = new Builder(MATERIAL, MATERIAL_NAME, "Material 3")
            .colors(
                0xFF000000,     // background
                0xFF2B313E,     // surfaceContainer
                0xFF404755,     // surfaceContainerHigh
                0xFFFFFFFF,     // onBackground
                0xFFC1C7D3,     // onSurfaceVariant
                0xFF8D929C,     // outline
                0xFFC4D6FF,     // primary
                0xFF08184C,     // onPrimary
                0xFF585F6D,     // neutral
                0xFFF2B8B5)     // error
            .containers(0xFF2A4488, 0xFFEBF1FE, 0xFF8D929C)
            .ripple(0x33C4D6FF, 0x33C4D6FF).scroll(0x4DFFFFFF, 0xFFFFFFFF)
            .sectionColor(0xFFEBF1FE)
            // CornerLarge (26 dp) on a 52 dp button is fully round; pressed morphs to CornerMedium (18 dp)
            .shape(false, 26, 26, 18, 26, 52, 4, 24)
            // the Expressive default spring: damping 0.75, stiffness 350
            .motion(new Spring(0.75f, 350f), 520, true, true)
            .material();
        // titleMedium 16 sp at weight 550, labelMedium 15 at 500, labelSmall 13, bodyMedium 14 at 450;
        // all at width 110 of Roboto Flex
        b.type(flex(550, 110), flex(500, 110), flex(450, 110), 16, 14, b.sectionColor, 15, 13, 14);
        if (dynamicColor(c)) dynamic(b, c.getResources());
        return b.build();
    }

    /** Whether the wearer has Wear OS's dynamic colours on, the same test Wear's Material 3 makes. */
    private static boolean dynamicColor(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return false;
        try {
            return Settings.Global.getInt(c.getContentResolver(), "dynamic_color_theme_enabled", 0) == 1;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** The system's dynamic colours, mapped to roles as Wear's dynamicColorScheme maps them. */
    private static void dynamic(Builder b, Resources r) {
        b.container = r.getColor(android.R.color.system_surface_container_dark, null);
        b.divider = r.getColor(android.R.color.system_surface_container_high_dark, null);
        b.text = r.getColor(android.R.color.system_on_background_dark, null);
        b.textSecondary = r.getColor(android.R.color.system_on_surface_variant_dark, null);
        b.textTertiary = r.getColor(android.R.color.system_outline_dark, null);
        b.outline = b.textTertiary;
        b.accent = r.getColor(android.R.color.system_primary_fixed, null);
        b.onAccent = r.getColor(android.R.color.system_on_primary_fixed, null);
        b.accentContainer = r.getColor(android.R.color.system_primary_container_dark, null);
        b.onAccentContainer = r.getColor(android.R.color.system_on_primary_container_dark, null);
        b.neutral = r.getColor(android.R.color.system_outline_variant_dark, null);
        b.danger = r.getColor(android.R.color.system_error_dark, null);
        b.sectionColor = r.getColor(android.R.color.system_on_surface_dark, null);
        b.ripple = (b.accent & 0x00FFFFFF) | 0x33000000;
        b.pillRipple = b.ripple;
    }

    // ---- choosing --------------------------------------------------------------

    private static Theme current;

    /** The theme in use. Cheap: it keeps the answer until {@link #refresh} or a new choice. */
    static Theme of(Context c) {
        if (current == null) current = resolve(c, choice(c));
        return current;
    }

    /** Looks again, for when the watch's dynamic colours may have changed. Called as a screen is built. */
    static void refresh(Context c) {
        current = resolve(c, choice(c));
    }

    /** What the wearer chose: AUTO, ONE_UI or MATERIAL. */
    static String choice(Context c) {
        String v = Store.prefs(c).getString(KEY_THEME, AUTO);
        return ONE_UI.equals(v) || MATERIAL.equals(v) ? v : AUTO;
    }

    static void choose(Context c, String choice) {
        Store.prefs(c).edit().putString(KEY_THEME, choice).apply();
        current = resolve(c, choice);
    }

    /** The short name of the look AUTO picks on this watch. */
    static String automaticName() {
        return isSamsung() ? "One UI" : "Material 3";
    }

    private static Theme resolve(Context c, String choice) {
        if (ONE_UI.equals(choice)) return ONE_UI_THEME;
        if (MATERIAL.equals(choice)) return material(c);
        return isSamsung() ? ONE_UI_THEME : material(c);
    }

    /** Galaxy Watches report Samsung as both maker and brand. */
    static boolean isSamsung() {
        return "samsung".equalsIgnoreCase(Build.MANUFACTURER) || "samsung".equalsIgnoreCase(Build.BRAND);
    }

    // ---- type ------------------------------------------------------------------

    private static Typeface weight(int w) {
        return Typeface.create(Typeface.DEFAULT, w, false);
    }

    private static File robotoFlex;
    private static boolean lookedForFlex;
    private static final Map<String, Typeface> FLEX = new HashMap<>();

    /**
     * Roboto Flex at the given weight and width, as Material 3 Expressive asks for.
     * Wear OS ships it as a variable font; a watch without it gets the system font.
     */
    private static Typeface flex(int weight, int width) {
        String key = weight + "/" + width;
        Typeface made = FLEX.get(key);
        if (made == null) {
            made = makeFlex(weight, width);
            FLEX.put(key, made);
        }
        return made;
    }

    private static Typeface makeFlex(int weight, int width) {
        if (!lookedForFlex) {
            lookedForFlex = true;
            try {
                for (Font font : SystemFonts.getAvailableFonts()) {
                    File file = font.getFile();
                    if (file != null && file.getName().startsWith("RobotoFlex")) {
                        robotoFlex = file;
                        break;
                    }
                }
            } catch (RuntimeException e) {
                robotoFlex = null;
            }
        }
        if (robotoFlex != null) {
            try {
                Typeface t = new Typeface.Builder(robotoFlex)
                    .setFontVariationSettings("'wght' " + weight + ", 'wdth' " + width)
                    .build();
                if (t != null) return t;
            } catch (RuntimeException e) {
                // fall through to the system font
            }
        }
        return weight(weight);
    }

    // ---- helpers ---------------------------------------------------------------

    /** Blends a over b; amount 1 is all a. */
    static int mix(int a, int b, float amount) {
        int r = Math.round(((a >> 16) & 0xFF) * amount + ((b >> 16) & 0xFF) * (1 - amount));
        int g = Math.round(((a >> 8) & 0xFF) * amount + ((b >> 8) & 0xFF) * (1 - amount));
        int bl = Math.round((a & 0xFF) * amount + (b & 0xFF) * (1 - amount));
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    /**
     * A damped spring as an interpolator, for Material's spring motion without a
     * library. Starts at rest at 0, settles at 1 and may overshoot on the way.
     * The animation's duration should cover the spring settling.
     */
    static final class Spring implements TimeInterpolator {
        private final float damping;
        private final float omega;
        private final float span;           // seconds the 0 to 1 input stands for

        Spring(float dampingRatio, float stiffness) {
            damping = dampingRatio;
            omega = (float) Math.sqrt(stiffness);
            // Long enough for the bounce to die away to under 0.1% of the travel.
            span = (float) (Math.log(1000) / (dampingRatio * omega));
        }

        @Override
        public float getInterpolation(float input) {
            if (input >= 1f) return 1f;
            double t = input * span;
            double wd = omega * Math.sqrt(1 - damping * damping);
            double decay = Math.exp(-damping * omega * t);
            return (float) (1 - decay * (Math.cos(wd * t) + damping * omega / wd * Math.sin(wd * t)));
        }
    }

    private static final class Builder {
        final String id, name, shortName;
        int background, container, divider, text, textSecondary, textTertiary, accent, onAccent, neutral, danger;
        int accentContainer, onAccentContainer, outline;
        int ripple, pillRipple, scrollTrack, scrollThumb, sectionColor;
        Typeface bold, medium, regular;
        float titleSize, sectionSize, labelSize, secondaryLabelSize, bodySize;
        boolean groupedRows, morphRows, edgeButton, bareIcons, controls, wavyProgress;
        float groupRadius, rowRadius, pressedRadius, fieldRadius, rowHeight, rowGap, iconSize;
        TimeInterpolator settle;
        long settleMs;

        Builder(String id, String name, String shortName) {
            this.id = id; this.name = name; this.shortName = shortName;
        }

        Builder colors(int background, int container, int divider, int text, int textSecondary,
                       int textTertiary, int accent, int onAccent, int neutral, int danger) {
            this.background = background; this.container = container; this.divider = divider;
            this.text = text; this.textSecondary = textSecondary; this.textTertiary = textTertiary;
            this.accent = accent; this.onAccent = onAccent; this.neutral = neutral; this.danger = danger;
            return this;
        }

        Builder containers(int accentContainer, int onAccentContainer, int outline) {
            this.accentContainer = accentContainer; this.onAccentContainer = onAccentContainer;
            this.outline = outline;
            return this;
        }

        Builder ripple(int row, int pill) { ripple = row; pillRipple = pill; return this; }

        Builder scroll(int track, int thumb) { scrollTrack = track; scrollThumb = thumb; return this; }

        Builder sectionColor(int color) { sectionColor = color; return this; }

        Builder type(Typeface bold, Typeface medium, Typeface regular, float title, float section,
                     int sectionColor, float label, float secondaryLabel, float body) {
            this.bold = bold; this.medium = medium; this.regular = regular;
            titleSize = title; sectionSize = section; this.sectionColor = sectionColor;
            labelSize = label; secondaryLabelSize = secondaryLabel; bodySize = body;
            return this;
        }

        Builder shape(boolean grouped, float group, float row, float pressed, float field,
                      float height, float gap, float icon) {
            groupedRows = grouped; groupRadius = group; rowRadius = row; pressedRadius = pressed;
            fieldRadius = field; rowHeight = height; rowGap = gap; iconSize = icon;
            return this;
        }

        Builder motion(TimeInterpolator settle, long ms, boolean morph, boolean edge) {
            this.settle = settle; settleMs = ms; morphRows = morph; edgeButton = edge;
            return this;
        }

        Builder material() { bareIcons = true; controls = true; wavyProgress = true; return this; }

        Theme build() { return new Theme(this); }
    }
}
