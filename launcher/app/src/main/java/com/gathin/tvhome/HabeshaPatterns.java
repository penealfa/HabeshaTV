package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * The woven patterns of the design: six small modules (cross, diamond, dotted disc, chevron, half-discs, framed
 * square), the turning ring behind the home screen, the friezes on panels and the saver ring. The modules are drawn
 * once into small bitmaps, so a frame costs a few dozen textured quads instead of hundreds of vector shapes.
 */
final class HabeshaPatterns {

    static final float RING_RADIUS = 700f;
    private static final int MODULE_PX = 96;
    private static final int RING_MODULES = 90;
    private static final float RING_MODULE_RADIUS = 945f;

    private final HabeshaTheme theme;
    private final Bitmap[] modules = new Bitmap[6];
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dots = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wovenA = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wovenB = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final RectF src = new RectF(0, 0, MODULE_PX, MODULE_PX);

    HabeshaPatterns(HabeshaTheme theme) {
        this.theme = theme;
        for (int i = 0; i < 6; i++) modules[i] = renderModule(i, theme.pairs[i][0], theme.pairs[i][1]);
        line.setStyle(Paint.Style.STROKE);
        dots.setStyle(Paint.Style.STROKE);
        float per = (float) (2 * Math.PI * 1000 / 350);
        dots.setStrokeWidth(6);
        dots.setColor(theme.weave[2]);
        dots.setPathEffect(new DashPathEffect(new float[]{per * 0.3f, per * 0.7f}, 0));
        wovenA.setStyle(Paint.Style.STROKE);
        wovenB.setStyle(Paint.Style.STROKE);
        float d = (float) (2 * Math.PI * 246 / 72 / 2);
        wovenA.setStrokeWidth(10);
        wovenA.setColor(theme.weave[0]);
        wovenA.setPathEffect(new DashPathEffect(new float[]{d, d}, 0));
        wovenB.setStrokeWidth(10);
        wovenB.setColor(theme.weave[1]);
        wovenB.setPathEffect(new DashPathEffect(new float[]{d, d}, d));
    }

    /** One module drawn into a 96 px square, from the design's 48 unit drawing. */
    private static Bitmap renderModule(int type, int bg, int fg) {
        Bitmap bmp = Bitmap.createBitmap(MODULE_PX, MODULE_PX, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.scale(MODULE_PX / 48f, MODULE_PX / 48f);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(bg);
        c.drawRect(0, 0, 48, 48, p);
        Path path = new Path();
        switch (type) {
            case 0: // quarter discs in the corners and a dot in the middle
                p.setColor(fg);
                c.drawCircle(0, 0, 22, p);
                c.drawCircle(48, 0, 22, p);
                c.drawCircle(0, 48, 22, p);
                c.drawCircle(48, 48, 22, p);
                c.drawCircle(24, 24, 4, p);
                break;
            case 1: // nested diamonds
                diamond(c, p, path, 24, 24, 21, fg);
                diamond(c, p, path, 24, 24, 12, bg);
                diamond(c, p, path, 24, 24, 5, fg);
                break;
            case 2: // disc with four holes
                p.setColor(fg);
                c.drawCircle(24, 24, 19, p);
                p.setColor(bg);
                c.drawCircle(18, 18, 3.6f, p);
                c.drawCircle(30, 18, 3.6f, p);
                c.drawCircle(18, 30, 3.6f, p);
                c.drawCircle(30, 30, 3.6f, p);
                break;
            case 3: // chevron over a small diamond
                p.setColor(fg);
                path.moveTo(6, 28);
                path.lineTo(24, 10);
                path.lineTo(42, 28);
                path.lineTo(42, 36);
                path.lineTo(24, 18);
                path.lineTo(6, 36);
                path.close();
                c.drawPath(path, p);
                diamond(c, p, path, 24, 34, 6, fg);
                break;
            case 4: // two half discs and a diamond
                p.setColor(fg);
                c.drawCircle(0, 24, 18, p);
                c.drawCircle(48, 24, 18, p);
                diamond(c, p, path, 24, 24, 8, fg);
                break;
            default: // framed square
                p.setColor(fg);
                c.drawRect(7, 7, 41, 41, p);
                p.setColor(bg);
                c.drawRect(13, 13, 35, 35, p);
                p.setColor(fg);
                c.drawRect(19, 19, 29, 29, p);
                break;
        }
        return bmp;
    }

    private static void diamond(Canvas c, Paint p, Path path, float cx, float cy, float r, int color) {
        path.reset();
        path.moveTo(cx, cy - r);
        path.lineTo(cx + r, cy);
        path.lineTo(cx, cy + r);
        path.lineTo(cx - r, cy);
        path.close();
        p.setColor(color);
        c.drawPath(path, p);
    }

    /**
     * The ring behind the home screen, centred on (0, 0): a thin track at the app radius, 90 woven modules, two thin
     * rings and a dotted rim. The caller translates to the ring centre and rotates.
     */
    void drawRing(Canvas c, int alpha) {
        line.setColor(theme.glassB);
        line.setStrokeWidth(2);
        line.setAlpha(Math.round(Color.alpha(theme.glassB) * alpha / 255f));
        c.drawCircle(0, 0, RING_RADIUS, line);
        bitmapPaint.setAlpha(alpha);
        float z = (float) (2 * Math.PI * RING_MODULE_RADIUS / RING_MODULES);
        float step = 360f / RING_MODULES;
        for (int i = 0; i < RING_MODULES; i++) {
            c.save();
            c.rotate(i * step);
            rect.set(-z / 2, -RING_MODULE_RADIUS - z / 2, z / 2, -RING_MODULE_RADIUS + z / 2);
            c.drawBitmap(modules[i % 6], null, rect, bitmapPaint);
            c.restore();
        }
        line.setStrokeWidth(3);
        line.setColor(theme.weave[0]);
        line.setAlpha(alpha);
        c.drawCircle(0, 0, 909, line);
        line.setColor(theme.weave[1]);
        line.setAlpha(alpha);
        c.drawCircle(0, 0, 981, line);
        dots.setAlpha(alpha);
        c.drawCircle(0, 0, 1000, dots);
    }

    /** A row of the six modules repeated across a rectangle, like the border of a netela. */
    void drawFrieze(Canvas c, float x, float y, float width, float height) {
        c.save();
        c.clipRect(x, y, x + width, y + height);
        bitmapPaint.setAlpha(255);
        int i = 0;
        for (float px = x; px < x + width; px += height, i++) {
            rect.set(px, y, px + height, y + height);
            c.drawBitmap(modules[i % 6], null, rect, bitmapPaint);
        }
        c.restore();
    }

    /** The screensaver ring centred on (0, 0): a thin track, a woven band and a dotted rim. */
    void drawSaverRing(Canvas c) {
        line.setStrokeWidth(4);
        line.setColor(theme.glassB);
        line.setAlpha(Color.alpha(theme.glassB));
        c.drawCircle(0, 0, 212, line);
        c.drawCircle(0, 0, 246, wovenA);
        c.drawCircle(0, 0, 246, wovenB);
        line.setStrokeWidth(4);
        line.setColor(theme.weave[2]);
        float per = (float) (2 * Math.PI * 266 / 96);
        line.setPathEffect(new DashPathEffect(new float[]{per * 0.3f, per * 0.7f}, 0));
        c.drawCircle(0, 0, 266, line);
        line.setPathEffect(null);
    }
}
