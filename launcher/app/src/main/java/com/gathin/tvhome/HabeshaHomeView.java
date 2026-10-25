package com.gathin.tvhome;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;
import android.view.animation.PathInterpolator;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws the HabeshaTV screens from {@link HabeshaState} on a 1920 x 1080 stage scaled to fit the display. Everything is
 * drawn with the canvas (no layouts) so it stays light on a 2 GB stick. Positions and sizes come from the design.
 */
final class HabeshaHomeView extends View {

    private static final float W = 1920f, H = 1080f;
    private static final float STEP_DEG = 19f;
    private static final float RING_R = HabeshaPatterns.RING_RADIUS;

    private static final String[] EN_WEEKDAYS = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private static final String[] EN_MONTHS = {"January", "February", "March", "April", "May", "June", "July",
            "August", "September", "October", "November", "December"};

    private static final TimeInterpolator EASE_OUT = new PathInterpolator(.2f, .8f, .2f, 1f);
    private static final TimeInterpolator SPRING = new PathInterpolator(.34f, 1.45f, .64f, 1f);
    private static final TimeInterpolator EASE_IN_OUT = new PathInterpolator(.42f, 0f, .58f, 1f);
    private static final TimeInterpolator LINEAR = t -> t;

    private final HabeshaState st;
    private final Map<String, HabeshaApps.AppInfo> info = new HashMap<>();
    private HabeshaTheme theme;
    private HabeshaPatterns patterns;
    private HabeshaNet.Info wifi = HabeshaNet.OFFLINE;
    private final GregorianCalendar now = new GregorianCalendar();
    private boolean ethClock = true, ethCalendar = true, geezNumerals = true;
    private int brightness; // 0 normal, 1 bright, 2 brightest

    private final Typeface menLight, menMedium, menSemi, figReg, figMedium, figSemi, figBold, bsLight, bsBold;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final RectF box = new RectF();
    private final Path path = new Path();

    private Bitmap photo;
    private Shader scrimShader;
    private Shader glowShader, warmShader;
    private int glowShaderColor, glowShaderThemeKey;

    private static final class Anim {
        float from, to;
        long t0;
        int duration;
        TimeInterpolator interpolator;
    }

    private final Map<String, Anim> anims = new HashMap<>();
    private boolean animating;
    private long frameTime;

    HabeshaHomeView(Context context, HabeshaState state, HabeshaTheme theme) {
        super(context);
        this.st = state;
        menLight = font(R.font.menbere_light);
        menMedium = font(R.font.menbere_medium);
        menSemi = font(R.font.menbere_semibold);
        figReg = font(R.font.figtree_regular);
        figMedium = font(R.font.figtree_medium);
        figSemi = font(R.font.figtree_semibold);
        figBold = font(R.font.figtree_bold);
        bsLight = font(R.font.bigshoulders_extralight);
        bsBold = font(R.font.bigshoulders_extrabold);
        stroke.setStyle(Paint.Style.STROKE);
        setTheme(theme);
        setFocusable(false);
    }

    private Typeface font(int res) {
        Typeface tf = getResources().getFont(res);
        return tf == null ? Typeface.DEFAULT : tf;
    }

    void setTheme(HabeshaTheme t) {
        theme = t;
        patterns = new HabeshaPatterns(t);
        glowShader = null;
        warmShader = null;
        photo = null; // let the old photo go before decoding another: it is 8 MB on a 2 GB stick
        if (t.hasPhoto()) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inScaled = false;
            photo = BitmapFactory.decodeResource(getResources(), R.drawable.wallpaper_mesob, o);
        }
        invalidate();
    }

    void setApps(Map<String, HabeshaApps.AppInfo> apps) {
        info.clear();
        info.putAll(apps);
        invalidate();
    }

    void setWifi(HabeshaNet.Info w) {
        wifi = w;
        invalidate();
    }

    void setTime(long millis) {
        now.setTimeInMillis(millis);
        invalidate();
    }

    HabeshaTheme theme() { return theme; }

    void setGeezNumerals(boolean geez) {
        geezNumerals = geez;
        invalidate();
    }

    boolean geezNumerals() { return geezNumerals; }

    void setBrightness(int level) {
        brightness = Math.max(0, Math.min(2, level));
        scrimShader = null;
        invalidate();
    }

    int brightness() { return brightness; }

    // ------------------------------------------------------------------ animation

    private float anim(String key, float target, int duration, TimeInterpolator ip, float snapAbove) {
        Anim a = anims.get(key);
        if (a == null) {
            a = new Anim();
            a.from = a.to = target;
            a.t0 = frameTime - duration;
            a.duration = duration;
            a.interpolator = ip;
            anims.put(key, a);
            return target;
        }
        float current = animValue(a);
        if (a.to != target) {
            if (snapAbove > 0 && Math.abs(target - a.to) > snapAbove) {
                a.from = a.to = target;
                a.t0 = frameTime - duration;
                return target;
            }
            a.from = current;
            a.to = target;
            a.t0 = frameTime;
            a.duration = duration;
            a.interpolator = ip;
            current = a.from;
        }
        if (frameTime - a.t0 >= a.duration) return a.to;
        animating = true;
        return current;
    }

    private float animValue(Anim a) {
        float t = (frameTime - a.t0) / (float) a.duration;
        if (t >= 1f) return a.to;
        if (t <= 0f) return a.from;
        return a.from + (a.to - a.from) * a.interpolator.getInterpolation(t);
    }

    private static int lerpColor(int a, int b, float t) {
        int aa = (a >>> 24) & 255, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = (b >>> 24) & 255, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    // ------------------------------------------------------------------ text helpers

    /** Draws one line the way CSS lays it out: the font's ascent and descent centred in a line box of height lh. */
    private float drawText(Canvas c, String s, float x, float top, float size, float lh, Typeface tf, int color, Paint.Align align) {
        if (s == null || s.isEmpty()) return 0;
        text.setTypeface(tf);
        text.setTextSize(size);
        text.setColor(color);
        text.setTextAlign(align);
        Paint.FontMetrics fm = text.getFontMetrics();
        float asc = -fm.ascent, desc = fm.descent;
        c.drawText(s, x, top + (lh - (asc + desc)) / 2f + asc, text);
        return text.measureText(s);
    }

    private float width(String s, float size, Typeface tf) {
        if (s == null || s.isEmpty()) return 0;
        text.setTypeface(tf);
        text.setTextSize(size);
        return text.measureText(s);
    }

    private void round(Canvas c, float l, float t, float r, float b, float radius, int fill) {
        box.set(l, t, r, b);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fill);
        c.drawRoundRect(box, radius, radius, paint);
    }

    private void outline(Canvas c, float l, float t, float r, float b, float radius, float w, int color) {
        box.set(l + w / 2, t + w / 2, r - w / 2, b - w / 2);
        stroke.setStrokeWidth(w);
        stroke.setColor(color);
        c.drawRoundRect(box, radius, radius, stroke);
    }

    private String label(String id) {
        HabeshaApps.AppInfo a = info.get(id);
        return a == null ? id : a.label;
    }

    private String amNum(int n) { return geezNumerals ? Ethiopic.geez(n) : String.valueOf(n); }

    // ------------------------------------------------------------------ draw

    @Override
    protected void onDraw(Canvas canvas) {
        frameTime = android.os.SystemClock.uptimeMillis();
        animating = false;
        float scale = Math.min(getWidth() / W, getHeight() / H);
        canvas.save();
        canvas.translate((getWidth() - W * scale) / 2f, (getHeight() - H * scale) / 2f);
        canvas.scale(scale, scale);
        canvas.clipRect(0, 0, W, H);
        canvas.drawColor(theme.bg);
        drawPhoto(canvas);
        switch (st.screen) {
            case SAVER: drawSaver(canvas); break;
            case LIBRARY: drawLibrary(canvas); drawToast(canvas, 980); break;
            default: drawHome(canvas); break;
        }
        canvas.restore();
        if (animating) {
            if (st.screen == HabeshaState.Screen.SAVER) postInvalidateDelayed(40);
            else postInvalidateOnAnimation();
        }
    }

    /** The basket photo with the design's dark scrim, behind every screen: 90% black in the middle, 60% at the edges. */
    private void drawPhoto(Canvas c) {
        if (photo == null) return;
        box.set(0, 0, W, H);
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
        c.drawBitmap(photo, null, box, paint);
        if (scrimShader == null) {
            // Normal is the design's 90 / 78 / 60 percent black; each step lets more of the wallpaper through.
            final int[][] levels = {{0xE6, 0xC7, 0x99}, {0xB8, 0x99, 0x6B}, {0x8A, 0x70, 0x40}};
            int[] a = levels[brightness];
            scrimShader = new RadialGradient(0, 0, 1152f,
                    new int[]{(a[0] << 24) | 0x0B0806, (a[1] << 24) | 0x0B0806, (a[2] << 24) | 0x0B0806},
                    new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
        }
        c.save();
        c.translate(960f, 0.62f * H);
        c.scale(1f, 0.70f * H / 1152f);
        paint.setShader(scrimShader);
        c.drawRect(-1152f, -1152f, 1152f, 1152f, paint);
        paint.setShader(null);
        c.restore();
    }

    private void drawHome(Canvas c) {
        boolean ringZone = st.zone == HabeshaState.Zone.RING;
        String focusId = st.focusedId();
        HabeshaApps.AppInfo focusInfo = info.get(focusId);

        // Soft glow behind the focused app, in its colour.
        int target = ringZone && focusInfo != null && focusInfo.branded ? focusInfo.glow : theme.accent;
        drawGlow(c, glowColor(target));

        drawWarm(c);

        // The turning weave ring.
        float rot = anim("rot", st.rotation * STEP_DEG * 0.5f, 520, EASE_OUT, 0);
        c.save();
        c.translate(W / 2f, 1240f);
        c.rotate(rot);
        patterns.drawRing(c, 255);
        c.restore();

        // Greeting.
        String gKey = HabeshaStrings.greetingKey(now.get(Calendar.HOUR_OF_DAY));
        drawText(c, HabeshaStrings.am(gKey), 96, 50, 64, 64 * 1.45f, menLight, theme.text, Paint.Align.LEFT);

        // Highlight: a pill around a top button, or a circle around the focused tile.
        drawHighlight(c, ringZone);

        drawButtons(c, ringZone);
        drawRingItems(c);
        drawFocusBlock(c, focusInfo, focusId);
        drawLibraryHint(c);

        if (st.menuOpen) drawMenu(c);
        if (st.moving) drawMovingChip(c, focusInfo, focusId);
        if (st.overlay == HabeshaState.Overlay.WIFI) {
            c.drawColor(0x9E000000);
            drawWifiPanel(c);
        }
        drawToast(c, 300);
    }

    private int glowFrom, glowTo;
    private long glowT0;
    private boolean glowInit;

    /** The glow colour, fading over 400 ms when the focused app (and so its colour) changes. */
    private int glowColor(int target) {
        if (!glowInit) {
            glowFrom = glowTo = target;
            glowT0 = frameTime - 400;
            glowInit = true;
        }
        float t = Math.min(1f, (frameTime - glowT0) / 400f);
        int cur = lerpColor(glowFrom, glowTo, t);
        if (target != glowTo) {
            glowFrom = cur;
            glowTo = target;
            glowT0 = frameTime;
            t = 0f;
        }
        if (t < 1f) animating = true;
        return cur;
    }

    private void drawGlow(Canvas c, int rgb) {
        int a = (rgb & 0xFFFFFF);
        if (glowShader == null || glowShaderColor != a || glowShaderThemeKey != theme.hashCode()) {
            glowShader = new RadialGradient(0, 0, 600f,
                    new int[]{HabeshaTheme.rgba(a, 80 / 255f), HabeshaTheme.rgba(a, 22 / 255f), 0},
                    new float[]{0f, 0.42f, 0.72f}, Shader.TileMode.CLAMP);
            glowShaderColor = a;
            glowShaderThemeKey = theme.hashCode();
        }
        c.save();
        c.translate(960f, 540f);
        c.scale(1f, 400f / 600f);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(glowShader);
        c.drawRect(-600, -600, 600, 600, paint);
        paint.setShader(null);
        c.restore();
    }

    private void drawWarm(Canvas c) {
        if (warmShader == null) {
            warmShader = new RadialGradient(0, 0, 1056f, new int[]{theme.warm, 0}, new float[]{0f, 0.75f}, Shader.TileMode.CLAMP);
        }
        c.save();
        c.clipRect(0, 640, W, H);
        c.translate(960f, 1080f);
        c.scale(1f, 352f / 1056f);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(warmShader);
        c.drawRect(-1056, -1056, 1056, 1056, paint);
        paint.setShader(null);
        c.restore();
    }

    private static final float[][] BUTTONS = {{1012, 230}, {1258, 290}, {1564, 220}}; // x, width

    private void drawHighlight(Canvas c, boolean ringZone) {
        float x, y, w, h, r;
        if (!ringZone) {
            float[] b = BUTTONS[st.topIndex];
            x = b[0] - 6; y = 54; w = b[1] + 12; h = 96; r = 48;
        } else {
            float d = st.moving ? 266f : 248f;
            x = 960 - d / 2; y = 1240 - RING_R - d / 2 - (st.moving ? 14 : 0); w = d; h = d; r = d / 2;
        }
        x = anim("hx", x, 300, SPRING, 0);
        y = anim("hy", y, 300, SPRING, 0);
        w = anim("hw", w, 300, SPRING, 0);
        h = anim("hh", h, 300, SPRING, 0);
        r = anim("hr", r, 260, EASE_OUT, 0);
        int color = st.moving ? theme.accent : theme.focus;
        if (!ringZone) round(c, x, y, x + w, y + h, r, theme.focus);
        outline(c, x, y, x + w, y + h, r, 4, color);
    }

    private void drawButtons(Canvas c, boolean ringZone) {
        String[] am = {HabeshaStrings.am("apps"), HabeshaStrings.am("wifi"), HabeshaStrings.am("settings")};
        String[] en = {"Apps", wifi.name, "Settings"};
        for (int i = 0; i < 3; i++) {
            boolean f = !ringZone && st.topIndex == i && st.screen == HabeshaState.Screen.HOME;
            float x = BUTTONS[i][0], w = BUTTONS[i][1];
            if (!f) {
                round(c, x, 60, x + w, 144, 42, theme.glass);
                outline(c, x, 60, x + w, 144, 42, 1, theme.glassB);
            }
            int fg = f ? theme.onFocus : theme.text, fg2 = f ? theme.onFocus : theme.text2;
            drawText(c, am[i], x + w / 2f, 60 + 8, 27, 27 * 1.3f, menMedium, fg, Paint.Align.CENTER);
            float ty = 60 + 8 + 27 * 1.3f;
            if (i == BUTTON_WIFI) {
                float tw = width(en[i], 18, figMedium);
                float total = 9 + 8 + tw;
                float sx = x + w / 2f - total / 2f;
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(f ? theme.onFocus : (wifi.connected ? theme.accent : theme.text2));
                c.drawCircle(sx + 4.5f, ty + 18 * 1.2f / 2f, 4.5f, paint);
                drawText(c, en[i], sx + 17, ty, 18, 18 * 1.2f, figMedium, fg2, Paint.Align.LEFT);
            } else {
                drawText(c, en[i], x + w / 2f, ty, 18, 18 * 1.2f, figMedium, fg2, Paint.Align.CENTER);
            }
        }
    }

    private static final int BUTTON_WIFI = HabeshaState.BUTTON_WIFI;

    private void drawRingItems(Canvas c) {
        List<String> ring = st.ring();
        int n = ring.size();
        String focusId = st.focusedId();
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < n; i++) {
                boolean isFocus = i == Math.min(st.focus, n - 1);
                if ((pass == 1) != isFocus) continue;
                String id = ring.get(i);
                int d = (i - st.focus) % n;
                if (d < 0) d += n;
                if (d > n / 2) d -= n;
                float dAnim = anim("d:" + id, d, 520, EASE_OUT, 1.5f);
                float ad = Math.abs(dAnim);
                float sc = ad <= 1 ? 1f - 0.3f * ad : ad <= 2 ? 0.7f - 0.08f * (ad - 1f) : 0.62f;
                if (isFocus && st.moving) sc = 1.06f;
                float alpha = ad <= 2 ? 1f : ad <= 3 ? 1f - 0.5f * (ad - 2f) : ad <= 4 ? 0.5f * (4f - ad) : 0f;
                if (alpha <= 0.01f) continue;
                float lift = isFocus && st.moving ? -14f : 0f;
                double ang = Math.toRadians(dAnim * STEP_DEG);
                float cx = 960f + (float) Math.sin(ang) * RING_R, cy = 1240f - (float) Math.cos(ang) * RING_R + lift;
                drawTile(c, id, cx, cy, 110f * sc, sc, alpha);
            }
        }
    }

    private void drawTile(Canvas c, String id, float cx, float cy, float radius, float sc, float alpha) {
        int a255 = Math.round(alpha * 255);
        HabeshaApps.AppInfo app = info.get(id);
        boolean library = HabeshaState.LIBRARY_ID.equals(id);
        int bg = library ? theme.surface : app != null && app.branded ? app.bg : theme.surface;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(bg);
        paint.setAlpha(Math.round(alphaOf(bg) * alpha));
        c.drawCircle(cx, cy, radius, paint);
        stroke.setStrokeWidth(Math.max(1f, sc));
        stroke.setColor(library ? theme.glassB : 0x0FFFFFFF);
        stroke.setAlpha(Math.round(alphaOf(stroke.getColor()) * alpha));
        c.drawCircle(cx, cy, radius - 0.5f, stroke);
        if (library) {
            stroke.setStrokeWidth(2f * sc);
            stroke.setColor(theme.text);
            stroke.setAlpha(a255);
            float r2 = 48f * sc;
            c.drawCircle(cx, cy, r2, stroke);
            stroke.setStrokeWidth(5f * sc);
            float arm = 18f * sc;
            c.drawLine(cx - arm, cy, cx + arm, cy, stroke);
            c.drawLine(cx, cy - arm, cx, cy + arm, stroke);
            return;
        }
        if (app == null) return;
        if (app.branded || app.icon == null) {
            float size = (app.label.length() > 8 ? 30f : 36f) * sc;
            int fg = app.branded ? app.fg : theme.text2;
            drawTextAlpha(c, app.label, cx, cy - size * 0.5f * 1.0f, size, size, figBold, fg, alpha);
        } else {
            float half = 70f * sc;
            box.set(cx - half, cy - half, cx + half, cy + half);
            paint.setAlpha(a255);
            c.drawBitmap(app.icon, null, box, paint);
        }
    }

    private static int alphaOf(int argb) { return (argb >>> 24) & 0xFF; }

    private void drawTextAlpha(Canvas c, String s, float x, float top, float size, float lh, Typeface tf, int color, float alpha) {
        int a = Math.round(((color >>> 24) & 0xFF) * alpha);
        drawText(c, s, x, top, size, lh, tf, (a << 24) | (color & 0xFFFFFF), Paint.Align.CENTER);
    }

    private void drawFocusBlock(Canvas c, HabeshaApps.AppInfo focusInfo, String focusId) {
        String name;
        if (HabeshaState.LIBRARY_ID.equals(focusId)) name = "All apps";
        else name = focusInfo == null ? focusId : focusInfo.label;
        if (st.moving) name = "Moving " + name;
        name = name.toUpperCase();
        float size = 112f;
        text.setLetterSpacing(0.01f);
        float nameW = width(name, size, bsBold);
        // Stay between the two tiles beside the focused one (their inner edges are about 726 px apart).
        if (nameW > 720f) size = size * 720f / nameW;
        drawText(c, name, 960f, 676, size, 112f, bsBold, theme.text, Paint.Align.CENTER);
        text.setLetterSpacing(0f);

        String time = timeString();
        Typeface timeFont = geezNumerals ? menLight : bsLight;
        float timeW = width(time, 124, timeFont);
        String eth = ethClock ? Ethiopic.clockLine(now, geezNumerals) : null;
        String ethDate = ethCalendar ? Ethiopic.dateLine(now, geezNumerals) : null;
        String greg = EN_WEEKDAYS[now.get(Calendar.DAY_OF_WEEK) - 1] + " " + now.get(Calendar.DAY_OF_MONTH) + " "
                + EN_MONTHS[now.get(Calendar.MONTH)] + " " + now.get(Calendar.YEAR);
        float colW = Math.max(width(greg, 22, figReg), Math.max(width(eth, 32, menLight), width(ethDate, 26, menLight)));
        float total = timeW + 36 + 2 + 36 + colW;
        float x = 960f - total / 2f;
        float top = 676 + 112 + 34;
        drawText(c, time, x, top, 124, 124, timeFont, theme.text, Paint.Align.LEFT);
        float dx = x + timeW + 36;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(theme.glassB);
        c.drawRect(dx, top + 10, dx + 2, top + 114, paint);
        float tx = dx + 2 + 36;
        float colH = (eth != null ? 32 * 1.4f : 0) + (ethDate != null ? 26 * 1.4f : 0) + 22 * 1.4f;
        float ty = top + (124 - colH) / 2f;
        if (eth != null) { drawText(c, eth, tx, ty, 32, 32 * 1.4f, menLight, theme.accent, Paint.Align.LEFT); ty += 32 * 1.4f; }
        if (ethDate != null) { drawText(c, ethDate, tx, ty, 26, 26 * 1.4f, menLight, theme.text, Paint.Align.LEFT); ty += 26 * 1.4f; }
        drawText(c, greg, tx, ty, 22, 22 * 1.4f, figReg, theme.text2, Paint.Align.LEFT);
    }

    /** The big clock: Ethiopian hours, so 10:24 shows 4:24. */
    private String timeString() {
        return Ethiopic.clockDigits(now, geezNumerals);
    }

    private void drawLibraryHint(Canvas c) {
        String am = HabeshaStrings.am("library");
        String pos = amNum(Math.min(st.focus, st.ring().size() - 1) + 1) + " / " + amNum(st.ring().size());
        float wAm = width(am, 24, menMedium), wEn = width("All apps", 20, figReg), wPos = width(pos, 24, menMedium);
        float total = 40 + 12 + wAm + 12 + wEn + 28 + 6 + 28 + wPos;
        float x = 960f - total / 2f, top = 978f;
        stroke.setStrokeWidth(1);
        stroke.setColor(theme.glassB);
        c.drawCircle(x + 20, top + 20, 19.5f, stroke);
        stroke.setStrokeWidth(2);
        stroke.setColor(theme.text2);
        c.drawLine(x + 20, top + 12, x + 20, top + 28, stroke);
        path.reset();
        path.moveTo(x + 14, top + 22);
        path.lineTo(x + 20, top + 28);
        path.lineTo(x + 26, top + 22);
        c.drawPath(path, stroke);
        float tx = x + 40 + 12;
        drawText(c, am, tx, top + 20 - 18, 24, 36, menMedium, theme.text, Paint.Align.LEFT);
        tx += wAm + 12;
        drawText(c, "All apps", tx, top + 20 - 12.5f, 20, 25, figReg, theme.text2, Paint.Align.LEFT);
        tx += wEn + 28;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(theme.glassB);
        c.drawCircle(tx + 3, top + 20, 3, paint);
        tx += 6 + 28;
        drawText(c, pos, tx, top + 20 - 18, 24, 36, menMedium, theme.text2, Paint.Align.LEFT);
    }

    private void drawMenu(Canvas c) {
        final int rows = 6;
        float px = 1120, py = 340, pw = 440, rowH = 76, ph = 16 * 2 + rows * rowH;
        round(c, px, py, px + pw, py + ph, 28, theme.panel);
        outline(c, px, py, px + pw, py + ph, 28, 1, theme.glassB);
        float hy = anim("menuhl", py + 16 + st.menuIndex * rowH, 220, SPRING, 0);
        round(c, px + 16, hy, px + pw - 16, hy + rowH, 18, theme.focus);
        String name = theme.name.substring(0, 1).toUpperCase() + theme.name.substring(1);
        String[] levels = {"Normal", "Bright", "Brightest"};
        String[] am = {HabeshaStrings.am("move"), HabeshaStrings.am("remove"), HabeshaStrings.am("theme"), "", "", HabeshaStrings.am("cancel")};
        String[] en = {"Move", "Remove", name, "Numbers · " + (geezNumerals ? "Ge'ez" : "1 2 3"),
                "Brightness · " + levels[brightness], "Cancel"};
        for (int i = 0; i < rows; i++) {
            boolean f = st.menuIndex == i;
            float top = py + 16 + i * rowH;
            int fg = f ? theme.onFocus : theme.text, fg2 = f ? theme.onFocus : theme.text2;
            if (am[i].isEmpty()) {
                // The Numbers and Brightness rows have no Amharic yet: they are English only until a word is chosen.
                drawText(c, en[i], px + 16 + 22, top + (rowH - 36) / 2f, 28, 36, figSemi, fg, Paint.Align.LEFT);
            } else {
                float w1 = drawText(c, am[i], px + 16 + 22, top + (rowH - 28 * 1.5f) / 2f, 28, 28 * 1.5f, menMedium, fg, Paint.Align.LEFT);
                drawText(c, en[i], px + 16 + 22 + w1 + 16, top + (rowH - 25) / 2f, 20, 25, figMedium, fg2, Paint.Align.LEFT);
            }
        }
    }

    private void drawMovingChip(Canvas c, HabeshaApps.AppInfo fi, String id) {
        String am = HabeshaStrings.am("moving");
        String en = "Left and Right carry " + (fi == null ? id : fi.label) + " around the ring · Back puts it down";
        float wa = width(am, 26, menSemi), we = width(en, 22, figMedium);
        float w = 30 + wa + 16 + we + 30, h = 12 + 39 + 12, x = 960 - w / 2, y = 300;
        round(c, x, y, x + w, y + h, 40, theme.accent);
        drawText(c, am, x + 30, y + 12, 26, 39, menSemi, theme.onFocus, Paint.Align.LEFT);
        drawText(c, en, x + 30 + wa + 16, y + (h - 27.5f) / 2f, 22, 27.5f, figMedium, theme.onFocus, Paint.Align.LEFT);
    }

    private void drawWifiPanel(Canvas c) {
        float px = 1224, py = 166, pw = 560;
        float ph = 14 + 92 + 2 * 104 + 16;
        round(c, px, py, px + pw, py + ph, 30, theme.panel);
        c.save();
        box.set(px, py, px + pw, py + ph);
        path.reset();
        path.addRoundRect(box, 30, 30, Path.Direction.CW);
        c.clipPath(path);
        patterns.drawFrieze(c, px, py, pw, 14);
        c.restore();
        outline(c, px, py, px + pw, py + ph, 30, 1, theme.glassB);
        float w1 = drawText(c, HabeshaStrings.am("wifi"), px + 16 + 20, py + 14 + (92 - 48) / 2f, 32, 48, menMedium, theme.text, Paint.Align.LEFT);
        drawText(c, "Wi-Fi", px + 16 + 20 + w1 + 14, py + 14 + (92 - 27.5f) / 2f, 22, 27.5f, figReg, theme.text2, Paint.Align.LEFT);
        float rowsTop = py + 14 + 92;
        float hy = anim("wifihl", rowsTop + st.overlayIndex * 104, 220, SPRING, 0);
        round(c, px + 16, hy, px + pw - 16, hy + 104, 22, theme.focus);
        String[] title = {wifi.name, "Network settings"};
        String[] am = {wifi.connected ? HabeshaStrings.am("connected") : "", HabeshaStrings.am("netSettings")};
        String[] en = {wifi.detail, ""};
        for (int i = 0; i < 2; i++) {
            boolean f = st.overlayIndex == i;
            float top = rowsTop + i * 104;
            int fg = f ? theme.onFocus : theme.text, fg2 = f ? theme.onFocus : theme.text2;
            drawText(c, title[i], px + 16 + 22, top + 12, 29, 36, figSemi, fg, Paint.Align.LEFT);
            float sx = px + 16 + 22;
            if (!am[i].isEmpty()) sx += drawText(c, am[i], sx, top + 52, 20, 30, menMedium, i == 0 && !f ? theme.accent : fg2, Paint.Align.LEFT) + 10;
            drawText(c, en[i], sx, top + 56, 20, 25, figReg, fg2, Paint.Align.LEFT);
            if (i == 0) {
                for (int b = 0; b < 4; b++) {
                    boolean on = b < wifi.bars;
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(on ? (f ? theme.onFocus : theme.text) : (f ? 0x40000000 : theme.glassB));
                    float bx = px + pw - 16 - 22 - 4 * 7 - 3 * 5 + b * 12;
                    float bh = 10 + b * 6;
                    c.drawRoundRect(bx, top + 38 + 28 - bh, bx + 7, top + 38 + 28, 2, 2, paint);
                }
            }
        }
    }

    private void drawToast(Canvas c, float y) {
        HabeshaState.Toast t = st.toast;
        if (t == null) return;
        String am = HabeshaStrings.am(t.amKey);
        String en = t.en == null ? "" : t.en;
        if (t.appId != null) en = en.isEmpty() ? label(t.appId) : label(t.appId) + " " + en;
        if (t.amKey != null && t.amKey.equals("opening")) en = "Opening " + label(t.appId);
        float wa = width(am, 26, menSemi), we = width(en, 23, figMedium);
        float w = 30 + wa + (wa > 0 && we > 0 ? 14 : 0) + we + 30, h = 12 + 39 + 12, x = 960 - w / 2;
        round(c, x, y, x + w, y + h, 40, theme.focus);
        float tx = x + 30;
        if (wa > 0) { drawText(c, am, tx, y + 12, 26, 39, menSemi, theme.onFocus, Paint.Align.LEFT); tx += wa + 14; }
        drawText(c, en, tx, y + (h - 28.75f) / 2f, 23, 28.75f, figMedium, theme.onFocus, Paint.Align.LEFT);
    }

    // ------------------------------------------------------------------ library

    private void drawLibrary(Canvas c) {
        drawText(c, HabeshaStrings.am("library"), 96, 50, 58, 58 * 1.45f, menLight, theme.text, Paint.Align.LEFT);
        drawText(c, "App library", 96, 50 + 58 * 1.45f, 24, 24 * 1.3f, figReg, theme.text2, Paint.Align.LEFT);
        String hint = "adds an app to the mesob";
        float wh = width(hint, 24, figReg);
        drawText(c, hint, 1824 - wh, 84, 24, 36, figReg, theme.text2, Paint.Align.LEFT);
        float chipW = 16 + width("OK", 24, figBold) + 16;
        round(c, 1824 - wh - 14 - chipW, 84 + 2, 1824 - wh - 14, 84 + 2 + 32, 22, theme.focus);
        drawText(c, "OK", 1824 - wh - 14 - chipW / 2f, 84 + 2, 24, 32, figBold, theme.onFocus, Paint.Align.CENTER);
        patterns.drawFrieze(c, 96, 192, 1728, 24);

        List<String> lib = st.library();
        int row = st.libraryIndex / HabeshaState.COLUMNS;
        float scroll = anim("libscroll", Math.max(0, row - 2) * 250f, 280, SPRING, 0);
        c.save();
        c.clipRect(0, 232, W, H);
        c.translate(0, -scroll);
        float lx = 0, ly = 0, lw = 256, lh = 144;
        for (int i = 0; i < lib.size(); i++) {
            float x = 96 + (i % HabeshaState.COLUMNS) * 294.4f, y = 262 + (i / HabeshaState.COLUMNS) * 250f;
            String id = lib.get(i);
            HabeshaApps.AppInfo app = info.get(id);
            boolean on = st.apps().contains(id);
            boolean f = i == st.libraryIndex;
            float sc = anim("lt:" + i, f ? 1.06f : 1f, 280, SPRING, 0);
            if (f) { lx = x; ly = y; }
            float alpha = on ? 1f : 0.5f;
            float w = lw * sc, h = lh * sc, l = x - (w - lw) / 2, t = y - (h - lh) / 2;
            int bg = app != null && app.branded ? app.bg : theme.surface;
            round(c, l, t, l + w, t + h, 18, (Math.round(((bg >>> 24) & 255) * alpha) << 24) | (bg & 0xFFFFFF));
            if (app != null) {
                if (app.branded || app.icon == null) {
                    drawTextAlpha(c, app.label, l + w / 2, t + h / 2 - 16 * sc, 32 * sc, 32 * sc, figBold,
                            app.branded ? app.fg : theme.text2, alpha);
                } else {
                    box.set(l + w / 2 - 48 * sc, t + h / 2 - 48 * sc, l + w / 2 + 48 * sc, t + h / 2 + 48 * sc);
                    paint.setAlpha(Math.round(255 * alpha));
                    c.drawBitmap(app.icon, null, box, paint);
                }
            }
            int lc = on ? theme.text2 : theme.accent;
            float ty = y + 158;
            String am = HabeshaStrings.am(on ? "onHome" : "addHome");
            String en = on ? "On the mesob" : "Add to the mesob";
            float wa = width(am, 22, menMedium);
            float gx = x + lw / 2 - (22 + 8 + wa) / 2f;
            stroke.setColor(lc);
            stroke.setStrokeWidth(3);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            if (on) {
                path.reset();
                path.moveTo(gx + 3, ty + 18);
                path.lineTo(gx + 9, ty + 24);
                path.lineTo(gx + 19, ty + 11);
                c.drawPath(path, stroke);
            } else {
                c.drawLine(gx + 3, ty + 17, gx + 19, ty + 17, stroke);
                c.drawLine(gx + 11, ty + 9, gx + 11, ty + 25, stroke);
            }
            stroke.setStrokeCap(Paint.Cap.BUTT);
            drawText(c, am, gx + 30, ty, 22, 33, menMedium, lc, Paint.Align.LEFT);
            drawText(c, en, x + lw / 2, ty + 33, 19, 24, figReg, lc, Paint.Align.CENTER);
        }
        if (!lib.isEmpty()) {
            float hx = anim("lhx", lx - (lw * 0.03f + 10), 280, SPRING, 0);
            float hy = anim("lhy", ly - (lh * 0.03f + 10), 280, SPRING, 0);
            outline(c, hx, hy, hx + lw + 2 * (lw * 0.03f + 10), hy + lh + 2 * (lh * 0.03f + 10), 28, 4, theme.focus);
        }
        c.restore();
    }

    // ------------------------------------------------------------------ saver

    /** The screensaver: the woven ring with the clock inside it and the date beneath. It stays where it is. */
    private void drawSaver(Canvas c) {
        float cardH = 560 + 26 + 36;
        float top = H / 2f - cardH / 2f;
        float cx = W / 2f;
        c.save();
        c.translate(cx, top + 280);
        patterns.drawSaverRing(c);
        c.restore();
        int dim = (theme.text & 0xFFFFFF) | (0xE6 << 24);
        Typeface timeFont = geezNumerals ? menLight : bsLight;
        drawText(c, timeString(), cx, top + 280 - 84 - 24, 168, 168, timeFont, dim, Paint.Align.CENTER);
        if (ethClock) drawText(c, Ethiopic.clockLine(now, geezNumerals), cx, top + 280 + 60, 32, 48, menLight, theme.accent, Paint.Align.CENTER);
        String date = ethCalendar ? Ethiopic.dateLine(now, geezNumerals) + " · " + gregorian() : gregorian();
        drawText(c, date, cx, top + 560 + 26, 24, 36, menLight, theme.text2, Paint.Align.CENTER);
    }

    private String gregorian() {
        return EN_WEEKDAYS[now.get(Calendar.DAY_OF_WEEK) - 1] + " " + now.get(Calendar.DAY_OF_MONTH) + " "
                + EN_MONTHS[now.get(Calendar.MONTH)] + " " + now.get(Calendar.YEAR);
    }
}
