package com.ukkuru.mazemakers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.ukkuru.mazemakers.core.Maze;
import com.ukkuru.mazemakers.core.Validator;

import java.util.ArrayDeque;

/** Grid editor (PRD E-01, E-02, E-05). Edits a char grid in the same format as Maze.parse. */
final class EditorView extends View {
    static final int W = 13, H = 17;

    interface Listener {
        void onChanged(String message);
    }

    private final char[][] grid = new char[H][W];
    private final ArrayDeque<String> undo = new ArrayDeque<>();
    private final ArrayDeque<String> redo = new ArrayDeque<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF tmp = new RectF();
    private Listener listener;
    private char tool = '#';
    private float tile, ox, oy;
    private int lastCell = -1;

    EditorView(Context c) {
        super(c);
        setBackgroundColor(Ui.BG);
        clearGrid();
    }

    void setListener(Listener l) { listener = l; }
    void setTool(char t) { tool = t; }
    char tool() { return tool; }

    void clearGrid() {
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++)
                grid[y][x] = (x == 0 || y == 0 || x == W - 1 || y == H - 1) ? '#' : ' ';
        invalidate();
    }

    void newMaze() {
        pushUndo();
        clearGrid();
        changed(null);
    }

    void load(String[] rows) {
        clearGrid();
        for (int y = 0; y < Math.min(H, rows.length); y++)
            for (int x = 0; x < Math.min(W, rows[y].length()); x++)
                grid[y][x] = rows[y].charAt(x);
        invalidate();
    }

    String[] rows() {
        String[] out = new String[H];
        for (int y = 0; y < H; y++) out[y] = new String(grid[y]);
        return out;
    }

    String joined() { return String.join("/", rows()); }

    Maze toMaze() { return Maze.parse("draft", "Draft", "you", 0, rows()); }

    void fillDots() {
        pushUndo();
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++)
                if (grid[y][x] == ' ') grid[y][x] = '.';
        changed(null);
    }

    boolean undo() {
        if (undo.isEmpty()) return false;
        redo.push(joined());
        load(undo.pop().split("/", -1));
        changed(null);
        return true;
    }

    boolean redo() {
        if (redo.isEmpty()) return false;
        undo.push(joined());
        load(redo.pop().split("/", -1));
        changed(null);
        return true;
    }

    private void pushUndo() {
        undo.push(joined());
        if (undo.size() > 100) undo.removeLast();
        redo.clear();
    }

    private void changed(String msg) {
        invalidate();
        if (listener != null) listener.onChanged(msg);
    }

    private int count(char c) {
        int n = 0;
        for (char[] r : grid) for (char x : r) if (x == c) n++;
        return n;
    }

    private int enemyCount() {
        int n = 0;
        for (char[] r : grid) for (char x : r) if (Maze.ENEMY_CHARS.indexOf(x) >= 0) n++;
        return n;
    }

    /** Applies the current tool to a cell. Returns a message when the tool is refused. */
    private String apply(int cx, int cy) {
        char cur = grid[cy][cx];
        if (cur == tool) return null;
        boolean unique = tool == 'S' || tool == 'E';
        if (Maze.ENEMY_CHARS.indexOf(tool) >= 0 && Maze.ENEMY_CHARS.indexOf(cur) < 0 && enemyCount() >= Validator.MAX_ENEMIES)
            return "Max " + Validator.MAX_ENEMIES + " enemies per maze.";
        if (tool == '*' && count('*') >= Validator.MAX_FREEZE)
            return "Max " + Validator.MAX_FREEZE + " freeze tiles per maze.";
        if (unique) {
            for (int y = 0; y < H; y++)
                for (int x = 0; x < W; x++)
                    if (grid[y][x] == tool) grid[y][x] = ' ';
        }
        grid[cy][cx] = tool;
        return null;
    }

    private void layoutBoard() {
        float pad = Ui.dp(getContext(), 6);
        tile = (float) Math.floor(Math.min((getWidth() - pad * 2) / W, (getHeight() - pad * 2) / H));
        ox = (getWidth() - tile * W) / 2f;
        oy = (getHeight() - tile * H) / 2f;
    }

    @Override
    protected void onDraw(Canvas cv) {
        layoutBoard();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Ui.FLOOR);
        tmp.set(ox, oy, ox + tile * W, oy + tile * H);
        cv.drawRect(tmp, paint);
        paint.setColor(0xFF1E2446);
        paint.setStrokeWidth(1);
        for (int x = 0; x <= W; x++) cv.drawLine(ox + x * tile, oy, ox + x * tile, oy + H * tile, paint);
        for (int y = 0; y <= H; y++) cv.drawLine(ox, oy + y * tile, ox + W * tile, oy + y * tile, paint);

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                char c = grid[y][x];
                float px = ox + x * tile, py = oy + y * tile, cx = px + tile / 2, cy = py + tile / 2;
                switch (c) {
                    case '#': tmp.set(px, py, px + tile, py + tile); Ui.drawWall(cv, paint, tmp, tile); break;
                    case '.': paint.setColor(Ui.DOT); cv.drawCircle(cx, cy, tile * 0.12f, paint); break;
                    case '*': Ui.drawFreeze(cv, paint, path, cx, cy, tile, 0.5f); break;
                    case 'E': Ui.drawExit(cv, paint, px, py, tile, true); break;
                    case 'S': Ui.drawPlayer(cv, paint, cx, cy, tile, Maze.NONE); break;
                    default:
                        int t = Maze.ENEMY_CHARS.indexOf(c);
                        if (t >= 0) Ui.drawEnemy(cv, paint, path, t, cx, cy, tile, false, 0);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int cx = (int) Math.floor((e.getX() - ox) / tile);
        int cy = (int) Math.floor((e.getY() - oy) / tile);
        boolean inside = cx >= 0 && cy >= 0 && cx < W && cy < H;
        boolean paintTool = tool == '#' || tool == '.' || tool == ' ';
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastCell = -1;
                if (!inside) return true;
                getParent().requestDisallowInterceptTouchEvent(true);
                pushUndo();
                // fall through
            case MotionEvent.ACTION_MOVE:
                if (inside && (paintTool || e.getActionMasked() == MotionEvent.ACTION_DOWN)) {
                    int cell = cy * W + cx;
                    if (cell != lastCell) {
                        lastCell = cell;
                        String msg = apply(cx, cy);
                        changed(msg);
                    }
                }
                return true;
            default:
                return true;
        }
    }
}
