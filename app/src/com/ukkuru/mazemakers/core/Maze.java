package com.ukkuru.mazemakers.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable maze definition. Plain Java with no Android imports so the same
 * class runs on the phone, on a server, and in host tests (PRD C-03).
 *
 * Text format, one string per row:
 *   '#' wall, '.' pellet, ' ' empty floor, 'S' player start, 'E' exit,
 *   '*' freeze power up, 'H' Hunter, 'F' Flanker, 'D' Drifter, 'T' Sentry.
 */
public final class Maze {
    public static final int HUNTER = 0, FLANKER = 1, DRIFTER = 2, SENTRY = 3;
    public static final String ENEMY_CHARS = "HFDT";
    public static final String[] ENEMY_NAMES = {"Hunter", "Flanker", "Drifter", "Sentry"};

    public final String id;
    public final String title;
    public final String creator;
    public final int tier;
    public final int w, h;
    public final boolean[] wall;
    public final boolean[] pellet;
    public final boolean[] freeze;
    public final int start, exit;
    public final int[] enemyPos;
    public final int[] enemyType;

    private Maze(String id, String title, String creator, int tier, int w, int h,
                 boolean[] wall, boolean[] pellet, boolean[] freeze, int start, int exit,
                 int[] enemyPos, int[] enemyType) {
        this.id = id; this.title = title; this.creator = creator; this.tier = tier;
        this.w = w; this.h = h; this.wall = wall; this.pellet = pellet; this.freeze = freeze;
        this.start = start; this.exit = exit; this.enemyPos = enemyPos; this.enemyType = enemyType;
    }

    /** Parses rows. Missing start or exit is stored as -1 so the validator can report it. */
    public static Maze parse(String id, String title, String creator, int tier, String[] rows) {
        int h = rows.length;
        int w = 0;
        for (String r : rows) w = Math.max(w, r.length());
        boolean[] wall = new boolean[w * h], pellet = new boolean[w * h], freeze = new boolean[w * h];
        int start = -1, exit = -1;
        List<int[]> enemies = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                char c = x < rows[y].length() ? rows[y].charAt(x) : '#';
                int i = y * w + x;
                switch (c) {
                    case '#': wall[i] = true; break;
                    case '.': pellet[i] = true; break;
                    case '*': freeze[i] = true; break;
                    case 'S': start = i; break;
                    case 'E': exit = i; break;
                    default:
                        int t = ENEMY_CHARS.indexOf(c);
                        if (t >= 0) enemies.add(new int[]{i, t});
                }
            }
        }
        int[] ep = new int[enemies.size()], et = new int[enemies.size()];
        for (int k = 0; k < ep.length; k++) { ep[k] = enemies.get(k)[0]; et[k] = enemies.get(k)[1]; }
        return new Maze(id, title, creator, tier, w, h, wall, pellet, freeze, start, exit, ep, et);
    }

    public char charAt(int i) {
        if (wall[i]) return '#';
        if (i == start) return 'S';
        if (i == exit) return 'E';
        for (int k = 0; k < enemyPos.length; k++) if (enemyPos[k] == i) return ENEMY_CHARS.charAt(enemyType[k]);
        if (freeze[i]) return '*';
        if (pellet[i]) return '.';
        return ' ';
    }

    public String[] rows() {
        String[] out = new String[h];
        for (int y = 0; y < h; y++) {
            StringBuilder sb = new StringBuilder(w);
            for (int x = 0; x < w; x++) sb.append(charAt(y * w + x));
            out[y] = sb.toString();
        }
        return out;
    }

    public int pelletCount() {
        int n = 0;
        for (boolean p : pellet) if (p) n++;
        return n;
    }

    public int freezeCount() {
        int n = 0;
        for (boolean p : freeze) if (p) n++;
        return n;
    }

    public Maze withMeta(String newId, String newTitle, String newCreator, int newTier) {
        return new Maze(newId, newTitle, newCreator, newTier, w, h, wall, pellet, freeze, start, exit, enemyPos, enemyType);
    }

    /** Single line storage format: id|title|creator|tier|row/row/row. Titles never contain '|' or '/'. */
    public String serialize() {
        return id + "|" + title + "|" + creator + "|" + tier + "|" + String.join("/", rows());
    }

    public static Maze deserialize(String s) {
        String[] p = s.split("\\|", 5);
        if (p.length < 5) return null;
        try {
            return parse(p[0], p[1], p[2], Integer.parseInt(p[3]), p[4].split("/", -1));
        } catch (RuntimeException e) {
            return null;
        }
    }

    // Directions: 0 none, 1 up, 2 right, 3 down, 4 left.
    public static final int NONE = 0, UP = 1, RIGHT = 2, DOWN = 3, LEFT = 4;
    public static final int[] DX = {0, 0, 1, 0, -1};
    public static final int[] DY = {0, -1, 0, 1, 0};

    public static int opposite(int d) {
        return d == NONE ? NONE : ((d + 1) % 4) + 1;
    }

    /** Neighbor index in direction d, or -1 if it is off the grid or a wall. */
    public int step(int i, int d) {
        int x = i % w + DX[d], y = i / w + DY[d];
        if (x < 0 || y < 0 || x >= w || y >= h) return -1;
        int j = y * w + x;
        return wall[j] ? -1 : j;
    }

    /** BFS distances from a source tile over open floor. Unreachable tiles get Integer.MAX_VALUE. */
    public int[] distancesFrom(int src) {
        int[] dist = new int[w * h];
        java.util.Arrays.fill(dist, Integer.MAX_VALUE);
        if (src < 0 || wall[src]) return dist;
        int[] q = new int[w * h];
        int head = 0, tail = 0;
        dist[src] = 0;
        q[tail++] = src;
        while (head < tail) {
            int c = q[head++];
            for (int d = 1; d <= 4; d++) {
                int n = step(c, d);
                if (n >= 0 && dist[n] == Integer.MAX_VALUE) {
                    dist[n] = dist[c] + 1;
                    q[tail++] = n;
                }
            }
        }
        return dist;
    }
}
