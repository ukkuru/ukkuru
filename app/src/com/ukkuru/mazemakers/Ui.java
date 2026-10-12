package com.ukkuru.mazemakers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ukkuru.mazemakers.core.Maze;

/** Colors, view builders and sprite drawing shared by every screen. No XML resources. */
final class Ui {
    static final int BG = 0xFF0F1222;
    static final int CARD = 0xFF1A1F3A;
    static final int CARD_PRESSED = 0xFF262D52;
    static final int TEXT = 0xFFEDEFF7;
    static final int MUTED = 0xFF9AA3C7;
    static final int ACCENT = 0xFF36E2C4;
    static final int ACCENT_DARK = 0xFF0B3A33;
    static final int WALL = 0xFF2E3A8C;
    static final int WALL_EDGE = 0xFF4B5BC4;
    static final int FLOOR = 0xFF141831;
    static final int DOT = 0xFFF5E6C8;
    static final int ICE = 0xFF8FE3FF;
    static final int EXIT_OPEN = 0xFF3DDC84;
    static final int EXIT_SHUT = 0xFF59607F;
    static final int DANGER = 0xFFFF4D5E;
    static final int[] ENEMY_COLORS = {0xFFFF4D5E, 0xFFFF9F1C, 0xFFB07CFF, 0xFF4DA3FF};
    static final String[] ENEMY_BLURB = {
        "Hunter. Takes the shortest path straight at you.",
        "Flanker. Aims a few tiles ahead of you to cut you off.",
        "Drifter. Picks a random turn at every junction.",
        "Sentry. Patrols one straight line and turns back at walls.",
    };

    private Ui() {}

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    static float sp(Context c, float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, c.getResources().getDisplayMetrics());
    }

    static TextView text(Context c, String s, float sizeSp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLineSpacing(0, 1.15f);
        return t;
    }

    static GradientDrawable rounded(int color, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        return g;
    }

    static Button button(Context c, String label, boolean primary, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(primary ? BG : TEXT);
        b.setBackground(rounded(primary ? ACCENT : CARD, dp(c, 14)));
        b.setStateListAnimator(null);
        b.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 10);
        b.setLayoutParams(lp);
        return b;
    }

    /** Small pill used for toggles such as ratings. */
    static Button chip(Context c, String label, boolean on, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        b.setTextColor(on ? BG : TEXT);
        b.setBackground(rounded(on ? ACCENT : CARD, dp(c, 20)));
        b.setStateListAnimator(null);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(c, 12), dp(c, 8), dp(c, 12), dp(c, 8));
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(c, 4), dp(c, 6), dp(c, 4), 0);
        b.setLayoutParams(lp);
        return b;
    }

    static LinearLayout row(Context c) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        // Baseline alignment shifts buttons down and clips their bottoms.
        r.setBaselineAligned(false);
        return r;
    }

    static LinearLayout column(Context c) {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 20);
        col.setPadding(p, p, p, p);
        return col;
    }

    static ScrollView page(Context c, View content) {
        ScrollView s = new ScrollView(c);
        s.setBackgroundColor(BG);
        s.setFillViewport(true);
        s.addView(content);
        return s;
    }

    static LinearLayout card(Context c, View.OnClickListener l) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 16);
        card.setPadding(p, dp(c, 14), p, dp(c, 14));
        android.graphics.drawable.StateListDrawable bg = new android.graphics.drawable.StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, rounded(CARD_PRESSED, dp(c, 16)));
        bg.addState(new int[]{}, rounded(CARD, dp(c, 16)));
        card.setBackground(bg);
        if (l != null) {
            card.setClickable(true);
            card.setOnClickListener(l);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 10);
        card.setLayoutParams(lp);
        return card;
    }

    // Sprites. Shapes differ per enemy so they read without color (colorblind friendly by design).

    static void drawPlayer(Canvas cv, Paint p, float cx, float cy, float size, int dir) {
        float r = size * 0.40f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(ACCENT);
        cv.drawRoundRect(new RectF(cx - r, cy - r, cx + r, cy + r), r * 0.55f, r * 0.55f, p);
        float lookX = Maze.DX[dir] * r * 0.22f, lookY = Maze.DY[dir] * r * 0.22f;
        p.setColor(0xFFFFFFFF);
        float er = r * 0.26f;
        cv.drawCircle(cx - r * 0.38f + lookX, cy - r * 0.12f + lookY, er, p);
        cv.drawCircle(cx + r * 0.38f + lookX, cy - r * 0.12f + lookY, er, p);
        p.setColor(BG);
        float pr = er * 0.5f;
        cv.drawCircle(cx - r * 0.38f + lookX * 1.6f, cy - r * 0.12f + lookY * 1.6f, pr, p);
        cv.drawCircle(cx + r * 0.38f + lookX * 1.6f, cy - r * 0.12f + lookY * 1.6f, pr, p);
    }

    static void drawEnemy(Canvas cv, Paint p, Path path, int type, float cx, float cy, float size, boolean frozen, int tick) {
        float r = size * 0.40f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(frozen ? ICE : ENEMY_COLORS[type]);
        path.reset();
        switch (type) {
            case Maze.HUNTER:
                path.moveTo(cx, cy - r);
                path.lineTo(cx + r, cy + r * 0.85f);
                path.lineTo(cx - r, cy + r * 0.85f);
                path.close();
                cv.drawPath(path, p);
                break;
            case Maze.FLANKER:
                path.moveTo(cx, cy - r);
                path.lineTo(cx + r, cy);
                path.lineTo(cx, cy + r);
                path.lineTo(cx - r, cy);
                path.close();
                cv.drawPath(path, p);
                break;
            case Maze.DRIFTER:
                float wob = frozen ? 0 : (float) Math.sin(tick * 0.25) * r * 0.08f;
                cv.drawCircle(cx, cy + wob, r * 0.92f, p);
                break;
            default:
                cv.drawRect(cx - r * 0.85f, cy - r * 0.85f, cx + r * 0.85f, cy + r * 0.85f, p);
                p.setColor(BG);
                cv.drawRect(cx - r * 0.6f, cy - r * 0.18f, cx + r * 0.6f, cy + r * 0.12f, p);
                p.setColor(frozen ? ICE : ENEMY_COLORS[type]);
                break;
        }
        // Single eye so none of them read as a ghost.
        if (type != Maze.SENTRY) {
            p.setColor(BG);
            cv.drawCircle(cx, cy + (type == Maze.HUNTER ? r * 0.25f : 0), r * 0.2f, p);
        }
    }

    static void drawFreeze(Canvas cv, Paint p, Path path, float cx, float cy, float size, float pulse) {
        float r = size * (0.30f + 0.04f * pulse);
        float q = r * 0.32f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(ICE);
        path.reset();
        path.moveTo(cx, cy - r);
        path.lineTo(cx + q, cy - q);
        path.lineTo(cx + r, cy);
        path.lineTo(cx + q, cy + q);
        path.lineTo(cx, cy + r);
        path.lineTo(cx - q, cy + q);
        path.lineTo(cx - r, cy);
        path.lineTo(cx - q, cy - q);
        path.close();
        cv.drawPath(path, p);
    }

    static void drawExit(Canvas cv, Paint p, float x, float y, float size, boolean open) {
        float in = size * 0.14f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(open ? EXIT_OPEN : EXIT_SHUT);
        cv.drawRoundRect(new RectF(x + in, y + in, x + size - in, y + size - in), size * 0.12f, size * 0.12f, p);
        p.setColor(BG);
        if (open) {
            cv.drawRect(x + size * 0.36f, y + size * 0.30f, x + size * 0.64f, y + size - in, p);
        } else {
            cv.drawRect(x + size * 0.30f, y + size * 0.45f, x + size * 0.70f, y + size * 0.56f, p);
        }
    }

    static void drawWall(Canvas cv, Paint p, RectF r, float size) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(WALL);
        float in = size * 0.06f;
        r.inset(in, in);
        cv.drawRoundRect(r, size * 0.18f, size * 0.18f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1f, size * 0.06f));
        p.setColor(WALL_EDGE);
        cv.drawRoundRect(r, size * 0.18f, size * 0.18f, p);
        p.setStyle(Paint.Style.FILL);
    }
}
