package com.ukkuru.mazemakers.core;

import java.util.Arrays;

/**
 * Solver bot (PRD E-03, E-04). Plays through the real simulation with no
 * access to anything a player can't see. Heads for the nearest dot (or the
 * open exit) while routing around tiles near enemies.
 */
public final class Bot {
    public static final int RUNS = 10;
    public static final int MAX_SECONDS = 240;

    private Bot() {}

    /** Number of clears out of RUNS seeded runs. */
    public static int clears(Maze m) {
        int c = 0;
        for (int seed = 1; seed <= RUNS; seed++) if (play(m, seed * 7919L) == Sim.WON) c++;
        return c;
    }

    /** Difficulty tier 1 (easy) to 5 (hard) from bot clears, or 0 if the bot never clears it. */
    public static int tierFromClears(int clears) {
        if (clears >= 9) return 1;
        if (clears >= 7) return 2;
        if (clears >= 5) return 3;
        if (clears >= 3) return 4;
        if (clears >= 1) return 5;
        return 0;
    }

    public static int play(Maze m, long seed) {
        Sim s = new Sim(m, seed, false);
        int limit = MAX_SECONDS * Sim.TPS;
        while (!s.isOver() && s.tick < limit) {
            if (s.pProg == 0) s.setWant(decide(s));
            s.step();
        }
        return s.status;
    }

    public static int decide(Sim s) {
        Maze m = s.maze;
        int n = m.w * m.h;
        int[] danger = new int[n];
        if (s.freezeTicks <= Sim.STEPS) {
            for (int k = 0; k < s.eTile.length; k++) {
                int[] d = m.distancesFrom(s.eTile[k]);
                for (int i = 0; i < n; i++) {
                    if (d[i] <= 1) danger[i] = Math.max(danger[i], 1000);
                    else if (d[i] <= 3) danger[i] = Math.max(danger[i], 40 / d[i]);
                }
            }
        }
        // Dijkstra from the player over danger weighted tiles (grid is at most 21 by 21).
        int[] cost = new int[n];
        int[] first = new int[n];
        boolean[] done = new boolean[n];
        Arrays.fill(cost, Integer.MAX_VALUE);
        cost[s.pTile] = 0;
        for (int iter = 0; iter < n; iter++) {
            int c = -1, cBest = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) if (!done[i] && cost[i] < cBest) { cBest = cost[i]; c = i; }
            if (c < 0) break;
            done[c] = true;
            for (int d = 1; d <= 4; d++) {
                int nb = m.step(c, d);
                if (nb < 0 || done[nb]) continue;
                int nc = cBest + 1 + danger[nb];
                if (nc < cost[nb]) {
                    cost[nb] = nc;
                    first[nb] = c == s.pTile ? d : first[c];
                }
            }
        }
        int target = -1, tBest = Integer.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            boolean goal = s.pelletsLeft > 0 ? (s.pellets[i] || s.freezes[i]) : i == m.exit;
            if (goal && i != s.pTile && cost[i] < tBest) { tBest = cost[i]; target = i; }
        }
        if (target >= 0 && tBest < 1000) return first[target];
        // Boxed in: step to the neighbor with the least danger.
        int best = Maze.NONE, bd = Integer.MAX_VALUE;
        for (int d = 1; d <= 4; d++) {
            int nb = m.step(s.pTile, d);
            if (nb >= 0 && danger[nb] < bd) { bd = danger[nb]; best = d; }
        }
        return best;
    }
}
