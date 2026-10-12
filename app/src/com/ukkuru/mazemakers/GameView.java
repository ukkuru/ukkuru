package com.ukkuru.mazemakers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.Choreographer;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.ukkuru.mazemakers.core.Maze;
import com.ukkuru.mazemakers.core.Sim;

/** Renders a run and turns swipes into moves. All game rules live in Sim. */
final class GameView extends View implements Choreographer.FrameCallback {

    interface Listener {
        void onRunEnd(Sim sim);
        void onQuit();
    }

    private final Maze maze;
    private final boolean assist;
    private final boolean haptics;
    private final String[] intro;
    private final String banner;
    private final Listener listener;

    private Sim sim;
    private boolean paused;
    private boolean showIntro;
    private boolean ended;
    private long lastNanos;
    private double acc;
    private int frame;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF tmp = new RectF();
    private final RectF pauseBtn = new RectF();
    private final RectF resumeBtn = new RectF(), restartBtn = new RectF(), quitBtn = new RectF();

    private float downX, downY;
    private final float swipeThreshold;
    private final float hud;

    GameView(Context c, Maze maze, boolean assist, boolean haptics, String[] intro, String banner, Listener listener) {
        super(c);
        this.maze = maze;
        this.assist = assist;
        this.haptics = haptics;
        this.intro = intro;
        this.banner = banner;
        this.listener = listener;
        this.showIntro = intro != null;
        this.swipeThreshold = Ui.dp(c, 22);
        this.hud = Ui.dp(c, 92);
        setBackgroundColor(Ui.BG);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setHapticFeedbackEnabled(haptics);
        newRun();
    }

    private void newRun() {
        // The server issues seeds in the real product (PRD C-02). Locally any seed works.
        sim = new Sim(maze, System.nanoTime(), assist);
        paused = false;
        ended = false;
        acc = 0;
        lastNanos = 0;
    }

    boolean isPaused() { return paused; }

    void pause() {
        if (!ended && !showIntro) {
            paused = true;
            invalidate();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        requestFocus();
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        Choreographer.getInstance().removeFrameCallback(this);
        super.onDetachedFromWindow();
    }

    @Override
    public void doFrame(long nanos) {
        if (lastNanos == 0) lastNanos = nanos;
        double dt = (nanos - lastNanos) / 1e9;
        lastNanos = nanos;
        frame++;
        if (!paused && !showIntro && !ended) {
            acc = Math.min(acc + dt, 0.25);
            double tickLen = 1.0 / Sim.TPS;
            while (acc >= tickLen && !sim.isOver()) {
                sim.step();
                acc -= tickLen;
                if (haptics) {
                    if (sim.evHit) performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    else if (sim.evFreeze || sim.evExitOpen) performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                }
            }
            if (sim.isOver()) {
                ended = true;
                postDelayed(() -> listener.onRunEnd(sim), 900);
            }
        }
        invalidate();
        Choreographer.getInstance().postFrameCallback(this);
    }

    // Layout of the board inside the view.
    private float tile, ox, oy;

    private void layoutBoard() {
        float pad = Ui.dp(getContext(), 8);
        float availW = getWidth() - pad * 2;
        float availH = getHeight() - hud - pad * 2;
        tile = (float) Math.floor(Math.min(availW / maze.w, availH / maze.h));
        ox = (getWidth() - tile * maze.w) / 2f;
        // Board sits right under the HUD; the space below stays free for thumb swipes.
        oy = hud + pad;
    }

    @Override
    protected void onDraw(Canvas cv) {
        layoutBoard();
        drawHud(cv);
        if (paused) {
            drawPaused(cv);
            return;
        }
        drawBoard(cv);
        if (showIntro) drawIntro(cv);
        else if (sim.status == Sim.READY) drawCenterLabel(cv, sim.livesLost == 0 ? "Ready" : "Lives left: " + sim.lives);
        else if (sim.status == Sim.WON) drawCenterLabel(cv, "Cleared!");
        else if (sim.status == Sim.LOST) drawCenterLabel(cv, "Out of lives");
    }

    private void drawHud(Canvas cv) {
        Context c = getContext();
        float m = Ui.dp(c, 16);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Ui.TEXT);
        textPaint.setTextSize(Ui.sp(c, 18));
        float y1 = Ui.dp(c, 34);
        cv.drawText(ellipsize(maze.title, getWidth() - Ui.dp(c, 90)), m, y1, textPaint);

        textPaint.setTypeface(Typeface.DEFAULT);
        textPaint.setTextSize(Ui.sp(c, 13));
        textPaint.setColor(Ui.MUTED);
        String sub = banner != null ? banner : "by " + maze.creator;
        if (assist) sub += "  ·  Assist mode";
        cv.drawText(sub, m, y1 + Ui.dp(c, 20), textPaint);

        // Stats line
        float y2 = y1 + Ui.dp(c, 46);
        textPaint.setTextSize(Ui.sp(c, 15));
        textPaint.setColor(Ui.TEXT);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        cv.drawText("Score " + sim.score(), m, y2, textPaint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        cv.drawText("Dots " + sim.pelletsLeft, getWidth() / 2f, y2, textPaint);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        int s = sim.seconds();
        cv.drawText(String.format(java.util.Locale.US, "%d:%02d", s / 60, s % 60), getWidth() - m, y2, textPaint);

        // Lives as small player icons
        float lr = Ui.dp(c, 9);
        for (int i = 0; i < sim.lives; i++) {
            Ui.drawPlayer(cv, paint, getWidth() / 2f + Ui.dp(c, 60) + i * lr * 2.4f, y2 - lr * 0.8f, lr * 2.2f, Maze.NONE);
        }

        // Pause button
        float bs = Ui.dp(c, 44);
        pauseBtn.set(getWidth() - m - bs, Ui.dp(c, 10), getWidth() - m, Ui.dp(c, 10) + bs);
        paint.setColor(Ui.CARD);
        cv.drawRoundRect(pauseBtn, bs / 3, bs / 3, paint);
        paint.setColor(Ui.TEXT);
        float bw = bs * 0.12f, bh = bs * 0.38f, cx = pauseBtn.centerX(), cy = pauseBtn.centerY();
        cv.drawRect(cx - bw * 2, cy - bh / 2, cx - bw, cy + bh / 2, paint);
        cv.drawRect(cx + bw, cy - bh / 2, cx + bw * 2, cy + bh / 2, paint);
    }

    private String ellipsize(String s, float max) {
        if (textPaint.measureText(s) <= max) return s;
        while (s.length() > 1 && textPaint.measureText(s + "...") > max) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    private void drawBoard(Canvas cv) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Ui.FLOOR);
        tmp.set(ox, oy, ox + tile * maze.w, oy + tile * maze.h);
        cv.drawRoundRect(tmp, tile * 0.3f, tile * 0.3f, paint);

        float pulse = (float) (0.5 + 0.5 * Math.sin(frame * 0.12));
        for (int i = 0; i < maze.w * maze.h; i++) {
            float x = ox + (i % maze.w) * tile, y = oy + (i / maze.w) * tile;
            if (maze.wall[i]) {
                tmp.set(x, y, x + tile, y + tile);
                Ui.drawWall(cv, paint, tmp, tile);
            } else if (i == maze.exit) {
                Ui.drawExit(cv, paint, x, y, tile, sim.exitOpen());
            } else if (sim.freezes[i]) {
                Ui.drawFreeze(cv, paint, path, x + tile / 2, y + tile / 2, tile, pulse);
            } else if (sim.pellets[i]) {
                paint.setColor(Ui.DOT);
                cv.drawCircle(x + tile / 2, y + tile / 2, tile * 0.12f, paint);
            }
        }

        boolean frozen = sim.freezeTicks > 0;
        for (int k = 0; k < sim.eTile.length; k++) {
            float ex = ox + (sim.subX(sim.eTile[k], sim.eDir[k], sim.eProg[k]) / (float) Sim.STEPS + 0.5f) * tile;
            float ey = oy + (sim.subY(sim.eTile[k], sim.eDir[k], sim.eProg[k]) / (float) Sim.STEPS + 0.5f) * tile;
            Ui.drawEnemy(cv, paint, path, maze.enemyType[k], ex, ey, tile, frozen, frame);
        }

        boolean blink = sim.status == Sim.READY && sim.livesLost > 0 && (frame / 8) % 2 == 0;
        if (!blink && sim.status != Sim.LOST) {
            float px = ox + (sim.subX(sim.pTile, sim.pDir, sim.pProg) / (float) Sim.STEPS + 0.5f) * tile;
            float py = oy + (sim.subY(sim.pTile, sim.pDir, sim.pProg) / (float) Sim.STEPS + 0.5f) * tile;
            Ui.drawPlayer(cv, paint, px, py, tile, sim.pDir);
        }

        if (frozen) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            textPaint.setTextSize(Ui.sp(getContext(), 14));
            textPaint.setColor(Ui.ICE);
            cv.drawText(String.format(java.util.Locale.US, "FROZEN %.1fs", sim.freezeTicks / (float) Sim.TPS),
                getWidth() / 2f, oy - Ui.dp(getContext(), 2), textPaint);
        } else if (sim.exitOpen() && sim.status == Sim.PLAYING) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            textPaint.setTextSize(Ui.sp(getContext(), 14));
            textPaint.setColor(Ui.EXIT_OPEN);
            cv.drawText("Exit open. Get to the door!", getWidth() / 2f, oy - Ui.dp(getContext(), 2), textPaint);
        }
    }

    private void drawCenterLabel(Canvas cv, String s) {
        Context c = getContext();
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextSize(Ui.sp(c, 26));
        float w = textPaint.measureText(s) + Ui.dp(c, 40);
        float cy = oy + tile * maze.h / 2f;
        tmp.set(getWidth() / 2f - w / 2, cy - Ui.dp(c, 32), getWidth() / 2f + w / 2, cy + Ui.dp(c, 20));
        paint.setColor(0xE6000000 | (Ui.BG & 0xFFFFFF));
        cv.drawRoundRect(tmp, Ui.dp(c, 14), Ui.dp(c, 14), paint);
        textPaint.setColor(Ui.TEXT);
        cv.drawText(s, getWidth() / 2f, cy, textPaint);
    }

    private void drawIntro(Canvas cv) {
        Context c = getContext();
        float m = Ui.dp(c, 24);
        float lh = Ui.dp(c, 30);
        float h = lh * (intro.length + 2) + Ui.dp(c, 20);
        float top = oy + (tile * maze.h - h) / 2f;
        tmp.set(m, top, getWidth() - m, top + h);
        paint.setColor(Ui.CARD);
        cv.drawRoundRect(tmp, Ui.dp(c, 18), Ui.dp(c, 18), paint);
        textPaint.setTextAlign(Paint.Align.LEFT);
        float x = m + Ui.dp(c, 20);
        float y = top + lh;
        for (int i = 0; i < intro.length; i++) {
            textPaint.setTypeface(i == 0 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            textPaint.setTextSize(Ui.sp(c, i == 0 ? 20 : 15));
            textPaint.setColor(i == 0 ? Ui.ACCENT : Ui.TEXT);
            cv.drawText(intro[i], x, y, textPaint);
            y += lh;
        }
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextSize(Ui.sp(c, 16));
        textPaint.setColor(Ui.ACCENT);
        textPaint.setTextAlign(Paint.Align.CENTER);
        cv.drawText("Tap to start", getWidth() / 2f, y + Ui.dp(c, 10), textPaint);
    }

    /** The maze is hidden while paused so pause can't be used to plan (PRD G-06). */
    private void drawPaused(Canvas cv) {
        Context c = getContext();
        float m = Ui.dp(c, 32);
        float bh = Ui.dp(c, 56), gap = Ui.dp(c, 14);
        float cy = getHeight() / 2f;
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextSize(Ui.sp(c, 30));
        textPaint.setColor(Ui.TEXT);
        cv.drawText("Paused", getWidth() / 2f, cy - bh * 2, textPaint);
        textPaint.setTypeface(Typeface.DEFAULT);
        textPaint.setTextSize(Ui.sp(c, 14));
        textPaint.setColor(Ui.MUTED);
        cv.drawText("The maze stays hidden while paused.", getWidth() / 2f, cy - bh * 2 + Ui.dp(c, 30), textPaint);

        resumeBtn.set(m, cy - bh, getWidth() - m, cy);
        restartBtn.set(m, cy + gap, getWidth() - m, cy + gap + bh);
        quitBtn.set(m, cy + gap * 2 + bh, getWidth() - m, cy + gap * 2 + bh * 2);
        drawButton(cv, resumeBtn, "Resume", true);
        drawButton(cv, restartBtn, "Restart", false);
        drawButton(cv, quitBtn, "Quit run", false);
    }

    private void drawButton(Canvas cv, RectF r, String label, boolean primary) {
        Context c = getContext();
        paint.setColor(primary ? Ui.ACCENT : Ui.CARD);
        cv.drawRoundRect(r, Ui.dp(c, 14), Ui.dp(c, 14), paint);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextSize(Ui.sp(c, 17));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(primary ? Ui.BG : Ui.TEXT);
        cv.drawText(label, r.centerX(), r.centerY() + Ui.sp(c, 6), textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = x;
                downY = y;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!paused && !showIntro && !ended) {
                    float dx = x - downX, dy = y - downY;
                    if (Math.max(Math.abs(dx), Math.abs(dy)) > swipeThreshold) {
                        int d = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? Maze.RIGHT : Maze.LEFT) : (dy > 0 ? Maze.DOWN : Maze.UP);
                        sim.setWant(d);
                        downX = x;
                        downY = y;
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (showIntro) {
                    showIntro = false;
                    lastNanos = 0;
                } else if (paused) {
                    if (resumeBtn.contains(x, y)) { paused = false; lastNanos = 0; }
                    else if (restartBtn.contains(x, y)) newRun();
                    else if (quitBtn.contains(x, y)) listener.onQuit();
                } else if (pauseBtn.contains(x, y) && pauseBtn.contains(downX, downY)) {
                    pause();
                }
                invalidate();
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int d = Maze.NONE;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP: case KeyEvent.KEYCODE_W: d = Maze.UP; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: case KeyEvent.KEYCODE_D: d = Maze.RIGHT; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: case KeyEvent.KEYCODE_S: d = Maze.DOWN; break;
            case KeyEvent.KEYCODE_DPAD_LEFT: case KeyEvent.KEYCODE_A: d = Maze.LEFT; break;
            case KeyEvent.KEYCODE_SPACE: case KeyEvent.KEYCODE_ENTER: case KeyEvent.KEYCODE_DPAD_CENTER:
                if (showIntro) { showIntro = false; lastNanos = 0; return true; }
                break;
            default:
        }
        if (d != Maze.NONE && !paused && !ended) {
            if (showIntro) { showIntro = false; lastNanos = 0; }
            sim.setWant(d);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
