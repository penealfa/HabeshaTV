package com.gathin.tvhome;

/**
 * Colours of the HabeshaTV home screen, from the design. All values are 0xAARRGGBB. The design's "mesob" theme
 * has a basket photo behind it; the photo is not shipped (rights), so here it is the lalibela colours with
 * warmer glass.
 */
final class HabeshaTheme {

    final String name;
    final int bg, surface, panel, glass, glassB, text, text2, accent, focus, onFocus, warm;
    /** Three colours for the woven rings. */
    final int[] weave;
    /** Background and foreground of the six weave modules. */
    final int[][] pairs;

    private HabeshaTheme(String name, int bg, int surface, int panel, int glass, int glassB, int text, int text2,
                         int accent, int focus, int onFocus, int warm, int[] weave, int[][] pairs) {
        this.name = name;
        this.bg = bg;
        this.surface = surface;
        this.panel = panel;
        this.glass = glass;
        this.glassB = glassB;
        this.text = text;
        this.text2 = text2;
        this.accent = accent;
        this.focus = focus;
        this.onFocus = onFocus;
        this.warm = warm;
        this.weave = weave;
        this.pairs = pairs;
    }

    static final String[] NAMES = {"mesob", "tilet", "lalibela", "buna"};

    static HabeshaTheme byName(String name) {
        if ("tilet".equals(name)) return TILET;
        if ("lalibela".equals(name)) return LALIBELA;
        if ("buna".equals(name)) return BUNA;
        return MESOB;
    }

    /** The name after this one, wrapping, for the Menu key. */
    static String next(String name) {
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(name)) return NAMES[(i + 1) % NAMES.length];
        return NAMES[0];
    }

    /** "#RRGGBB" with an alpha 0 to 1. */
    static int rgba(int rgb, float alpha) {
        return ((Math.round(alpha * 255) & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    private static int[][] pairs(int... v) {
        int[][] p = new int[v.length / 2][2];
        for (int i = 0; i < p.length; i++) {
            p[i][0] = 0xFF000000 | v[2 * i];
            p[i][1] = 0xFF000000 | v[2 * i + 1];
        }
        return p;
    }

    private static int[] solid(int... v) {
        int[] c = new int[v.length];
        for (int i = 0; i < v.length; i++) c[i] = 0xFF000000 | v[i];
        return c;
    }

    private static final int[][] LALIBELA_PAIRS = pairs(
            0x16100C, 0xE0A443, 0xC2613A, 0xEFE3CE, 0xCDBB9C, 0x16100C,
            0x16100C, 0xC2613A, 0xE0A443, 0x16100C, 0x355F49, 0xCDBB9C);

    static final HabeshaTheme LALIBELA = new HabeshaTheme("lalibela",
            0xFF0B0806, 0xFF1A1310, 0xFF1C1511, rgba(0xEFE3CE, .07f), rgba(0xEFE3CE, .16f),
            0xFFF2E8DA, 0xFFB3A390, 0xFFD9774A, 0xFFF2E8DA, 0xFF16100C, 0xFF3A1C10,
            solid(0xE0A443, 0xC2613A, 0xCDBB9C), LALIBELA_PAIRS);

    static final HabeshaTheme TILET = new HabeshaTheme("tilet",
            0xFF000000, 0xFF14110C, 0xFF17130D, rgba(0xF4ECDD, .07f), rgba(0xF4ECDD, .16f),
            0xFFF4ECDD, 0xFFA99F8E, 0xFFE2AA3B, 0xFFF2C766, 0xFF120E06, 0xFF3A2709,
            solid(0xE2AA3B, 0xC25A3E, 0x2F8A5E),
            pairs(0x0E0C09, 0xE2AA3B, 0xE2AA3B, 0x0E0C09, 0x0E0C09, 0x2F8A5E,
                    0x0E0C09, 0xC25A3E, 0x0E0C09, 0xE2AA3B, 0x0E0C09, 0x7E5BA6));

    static final HabeshaTheme BUNA = new HabeshaTheme("buna",
            0xFF040A07, 0xFF0D1813, 0xFF0F1B15, rgba(0xEEE2C8, .07f), rgba(0xEEE2C8, .16f),
            0xFFEEE2C8, 0xFFA3AC9C, 0xFFD9B47A, 0xFFEEE2C8, 0xFF08110D, 0xFF3D2414,
            solid(0xD6A24C, 0x2D6A4F, 0xEEE2C8),
            pairs(0x2D6A4F, 0xEEE2C8, 0x08110D, 0xD6A24C, 0xEEE2C8, 0x6E4429,
                    0x08110D, 0xEEE2C8, 0x6E4429, 0xD6A24C, 0x2D6A4F, 0x08110D));

    static final HabeshaTheme MESOB = new HabeshaTheme("mesob",
            0xFF0B0806, 0xFF1A1310, 0xFF1C1511, rgba(0x140C08, .55f), rgba(0xEFE3CE, .22f),
            0xFFF2E8DA, 0xFFC9B8A4, 0xFFE39A5C, 0xFFF2E8DA, 0xFF16100C, 0xFF3A1C10,
            solid(0xE0A443, 0xC2613A, 0xCDBB9C), LALIBELA_PAIRS);
}
