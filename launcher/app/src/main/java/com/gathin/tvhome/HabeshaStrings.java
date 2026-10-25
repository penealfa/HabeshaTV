package com.gathin.tvhome;

import java.util.HashMap;
import java.util.Map;

/**
 * Amharic text of the HabeshaTV home screen, reviewed by a native reader. The calendar words live in
 * {@link Ethiopic}. tools/build_fonts.py reads the Ge'ez characters from both files to cut the fonts down,
 * so after adding a word here, run it again.
 */
final class HabeshaStrings {

    private static final Map<String, String> AM = new HashMap<>();

    static {
        AM.put("settings", "ቅንብሮች");
        AM.put("apps", "መተግበሪያዎች");
        AM.put("wifi", "ዋይፋይ");
        AM.put("morning", "እንደምን አደሩ");
        AM.put("day", "እንደምን ዋሉ");
        AM.put("evening", "እንደምን አመሹ");
        AM.put("connected", "ተገናኝቷል");
        AM.put("netSettings", "የአውታረ መረብ ቅንብሮች");
        AM.put("move", "አንቀሳቅስ");
        AM.put("remove", "ከመሶቡ አስወግድ");
        AM.put("cancel", "ተመለስ");
        AM.put("addHome", "ወደ መሶቡ አክል");
        AM.put("onHome", "መሶቡ ላይ");
        AM.put("library", "ሁሉም መተግበሪያዎች");
        AM.put("opening", "በመክፈት ላይ");
        AM.put("moving", "በማንቀሳቀስ ላይ");
        AM.put("added", "ታክሏል");
        AM.put("removed", "ተወግዷል");
        AM.put("buna", "የቡና ሰዓት");
        AM.put("abol", "አቦል");
        AM.put("tona", "ቶና");
        AM.put("bereka", "በረካ");
    }

    private HabeshaStrings() {
    }

    /** The Amharic text for a key, or "" when there is none. */
    static String am(String key) {
        String s = key == null ? null : AM.get(key);
        return s == null ? "" : s;
    }

    /** "morning", "day" or "evening" for the greeting at a given hour (0 to 23). */
    static String greetingKey(int hour24) {
        if (hour24 >= 4 && hour24 < 11) return "morning";
        if (hour24 >= 11 && hour24 < 17) return "day";
        return "evening";
    }

    static String greetingEnglish(String key) {
        switch (key) {
            case "morning": return "Good morning";
            case "day": return "Good day";
            default: return "Good evening";
        }
    }

    /** Which of the three coffee cups (0 Abol, 1 Tona, 2 Bereka) is being poured, ten minutes each. */
    static int coffeeRound(long elapsedMs) {
        return (int) (Math.max(0, elapsedMs) / COFFEE_ROUND_MS % 3);
    }

    /** How far through the current cup, 0 to just under 1. */
    static float coffeeProgress(long elapsedMs) {
        return (float) (Math.max(0, elapsedMs) % COFFEE_ROUND_MS) / COFFEE_ROUND_MS;
    }

    static final long COFFEE_ROUND_MS = 10L * 60 * 1000;
}
