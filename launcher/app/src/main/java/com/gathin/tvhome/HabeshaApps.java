package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The apps the HabeshaTV ring can show: what is installed, how each one looks, and the saved order. */
final class HabeshaApps {

    /** Apps whose tile is the brand colour with the name in it, as in the design. */
    private static final Map<String, int[]> BRANDS = new HashMap<>();

    static {
        BRANDS.put("com.google.android.youtube.tv", new int[]{0xC4302B, 0xFFFFFF, 0xE0443A});
        BRANDS.put("com.netflix.ninja", new int[]{0x141414, 0xE50914, 0xB0060F});
        BRANDS.put("com.spotify.tv.android", new int[]{0x121212, 0x1ED760, 0x1DB954});
        BRANDS.put("org.videolan.vlc", new int[]{0xE57A00, 0xFFFFFF, 0xFF8800});
        BRANDS.put("com.esaba.downloader", new int[]{0x2B5FA8, 0xFFFFFF, 0x3D7BD6});
        BRANDS.put("com.brave.browser", new int[]{0x22324A, 0xE8EEF6, 0x3F6FB0});
    }

    /** First on the ring when installed, in this order. */
    static final List<String> PREFERRED = Collections.unmodifiableList(Arrays.asList(
            "com.google.android.youtube.tv", "com.netflix.ninja", "com.spotify.tv.android"));

    static final class AppInfo {
        final String pkg;
        final String label;
        final boolean branded;
        final int bg, fg, glow;
        volatile Bitmap icon;

        AppInfo(String pkg, String label) {
            this.pkg = pkg;
            this.label = label;
            int[] b = BRANDS.get(pkg);
            this.branded = b != null;
            this.bg = b != null ? 0xFF000000 | b[0] : 0;
            this.fg = b != null ? 0xFF000000 | b[1] : 0;
            this.glow = b != null ? 0xFF000000 | b[2] : 0;
        }
    }

    private HabeshaApps() {
    }

    /** Launchable apps except this launcher, preferred ones first and the rest A to Z. */
    static LinkedHashMap<String, AppInfo> load(Context context) {
        PackageManager pm = context.getPackageManager();
        Map<String, String> labels = new HashMap<>();
        for (String category : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo ri : pm.queryIntentActivities(query, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(context.getPackageName()) || labels.containsKey(pkg)) continue;
                CharSequence label = ri.loadLabel(pm);
                labels.put(pkg, label == null ? pkg : label.toString());
            }
        }
        List<String> pkgs = new ArrayList<>(labels.keySet());
        final Collator collator = Collator.getInstance(Locale.getDefault());
        Collections.sort(pkgs, (a, b) -> {
            int pa = PREFERRED.indexOf(a), pb = PREFERRED.indexOf(b);
            if (pa >= 0 || pb >= 0) return (pa < 0 ? 99 : pa) - (pb < 0 ? 99 : pb);
            return collator.compare(labels.get(a), labels.get(b));
        });
        LinkedHashMap<String, AppInfo> out = new LinkedHashMap<>();
        for (String pkg : pkgs) out.put(pkg, new AppInfo(pkg, labels.get(pkg)));
        return out;
    }

    /** Draws each app's icon into a 192 px bitmap. Slow on a TV, so call it off the main thread. */
    static void loadIcons(Context context, Map<String, AppInfo> apps) {
        PackageManager pm = context.getPackageManager();
        for (AppInfo info : apps.values()) {
            if (info.icon != null) continue;
            try {
                Drawable d = pm.getApplicationIcon(info.pkg);
                Bitmap b = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(b);
                d.setBounds(0, 0, 192, 192);
                d.draw(c);
                info.icon = b;
            } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {
                // The app went away while loading, or its icon would not draw: the tile shows its name instead.
            }
        }
    }

    static Intent launchIntent(Context context, String pkg) {
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (intent == null) intent = pm.getLaunchIntentForPackage(pkg);
        if (intent != null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        return intent;
    }

    private static final String PREFS = "habesha";

    static List<String> savedList(Context context, String key) {
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, "");
        List<String> out = new ArrayList<>();
        for (String s : raw.split("\n")) if (!s.isEmpty()) out.add(s);
        return out;
    }

    static void saveLists(Context context, List<String> apps, List<String> removed) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("order", String.join("\n", apps))
                .putString("removed", String.join("\n", removed))
                .apply();
    }

    static String savedTheme(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("theme", "mesob");
    }

    static void saveTheme(Context context, String name) {
        SharedPreferences.Editor e = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        e.putString("theme", name).apply();
    }

    /** Ge'ez numerals (the default) or plain digits, remembered per device. */
    static boolean savedGeez(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("geez", true);
    }

    static void saveGeez(Context context, boolean geez) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("geez", geez).apply();
    }

    /** 0 normal, 1 bright, 2 brightest: how much of the wallpaper shows through the dark scrim. */
    static int savedBrightness(Context context) {
        return Math.max(0, Math.min(2, context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("brightness", 0)));
    }

    static void saveBrightness(Context context, int level) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("brightness", level).apply();
    }
}
