package com.ukkuru.mazemakers.core;

/**
 * Deterministic game simulation (PRD C-01, C-02).
 *
 * Fixed tick, integer positions, one seeded generator. Given the same maze,
 * seed, assist flag and input list, every device produces the same run, so a
 * server can replay a run record and check the score.
 */
public final class Sim {
    public static final int SIM_VERSION = 1;
    public static final int TPS = 30;           // logic ticks per second
    public static final int STEPS = 6;          // sub steps per tile
    public static final int PLAYER_SPEED = 10;  // sub steps per 10 ticks
    public static final int[] ENEMY_SPEED = {8, 8, 7, 7}; // Hunter, Flanker, Drifter, Sentry
    public static final int FREEZE_TICKS = 3 * TPS;
    public static final int READY_TICKS = TPS * 3 / 2;
    public static final int CONTACT = STEPS * 2 / 3;

    public static final int READY = 0, PLAYING = 1, WON = 2, LOST = 3;

    public final Maze maze;
    public final boolean assist;
    private final Rng rng;

    public final boolean[] pellets;
    public final boolean[] freezes;
    public int pelletsLeft;
    public int pelletsEaten;
    public int lives;
    public int status = READY;
    public int tick;            // total ticks since run start
    public int playTicks;       // ticks spent with the clock running
    public int readyTicks;
    public int freezeTicks;
    public int livesLost;

    // Player
    public int pTile, pDir, pProg, want;
    // Enemies
    public final int[] eTile, eDir, eProg, eAcc, eRelease;

    /** Event flags for the renderer and haptics. Cleared at the start of each tick. */
    public boolean evPellet, evFreeze, evHit, evExitOpen;

    public Sim(Maze maze, long seed, boolean assist) {
        this.maze = maze;
        this.assist = assist;
        this.rng = new Rng(seed);
        int n = maze.w * maze.h;
        pellets = maze.pellet.clone();
        freezes = maze.freeze.clone();
        pelletsLeft = maze.pelletCount();
        lives = assist ? 4 : 3;
        int ec = maze.enemyPos.length;
        eTile = new int[ec]; eDir = new int[ec]; eProg = new int[ec]; eAcc = new int[ec]; eRelease = new int[ec];
        resetPositions();
        readyTicks = READY_TICKS;
        if (n == 0) status = LOST;
    }

    private void resetPositions() {
        pTile = maze.start; pDir = Maze.NONE; pProg = 0; want = Maze.NONE;
        for (int k = 0; k < eTile.length; k++) {
            eTile[k] = maze.enemyPos[k];
            eProg[k] = 0;
            eAcc[k] = 0;
            eRelease[k] = TPS / 2 + k * TPS * 2; // stagger enemy release: 0.5 s, 2.5 s, 4.5 s ...
            eDir[k] = maze.enemyType[k] == Maze.SENTRY ? sentryStartDir(eTile[k]) : Maze.NONE;
        }
    }

    private int sentryStartDir(int t) {
        int[] order = {Maze.RIGHT, Maze.LEFT, Maze.DOWN, Maze.UP};
        for (int d : order) if (maze.step(t, d) >= 0) return d;
        return Maze.NONE;
    }

    public boolean exitOpen() {
        return pelletsLeft == 0;
    }

    public boolean isOver() {
        return status == WON || status == LOST;
    }

    public void setWant(int d) {
        want = d;
    }

    /** Advance one logic tick. */
    public void step() {
        evPellet = evFreeze = evHit = evExitOpen = false;
        if (isOver()) return;
        tick++;
        if (readyTicks > 0) {
            readyTicks--;
            if (readyTicks == 0) status = PLAYING;
            return;
        }
        playTicks++;
        movePlayer();
        if (isOver()) return;
        if (checkContact()) return;
        if (freezeTicks > 0) {
            freezeTicks--;
        } else {
            int speedDrop = assist ? 2 : 0;
            for (int k = 0; k < eTile.length; k++) {
                if (eRelease[k] > 0) { eRelease[k]--; continue; }
                eAcc[k] += ENEMY_SPEED[maze.enemyType[k]] - speedDrop;
                while (eAcc[k] >= 10) {
                    eAcc[k] -= 10;
                    moveEnemy(k);
                }
            }
        }
        checkContact();
    }

    private void movePlayer() {
        if (pProg == 0) {
            if (want != Maze.NONE && maze.step(pTile, want) >= 0) pDir = want;
            else if (pDir != Maze.NONE && maze.step(pTile, pDir) < 0) pDir = Maze.NONE;
            if (pDir == Maze.NONE) return;
        } else if (want != Maze.NONE && want == Maze.opposite(pDir)) {
            // Reverse mid tile: swap the tile we are leaving and the one we are heading to.
            pTile = maze.step(pTile, pDir);
            pDir = want;
            pProg = STEPS - pProg;
        }
        pProg++;
        if (pProg >= STEPS) {
            pTile = maze.step(pTile, pDir);
            pProg = 0;
            arrive();
        }
    }

    private void arrive() {
        if (pellets[pTile]) {
            pellets[pTile] = false;
            pelletsLeft--;
            pelletsEaten++;
            evPellet = true;
            if (pelletsLeft == 0) evExitOpen = true;
        }
        if (freezes[pTile]) {
            freezes[pTile] = false;
            freezeTicks = FREEZE_TICKS;
            evFreeze = true;
        }
        if (pTile == maze.exit && exitOpen()) status = WON;
    }

    private boolean checkContact() {
        if (freezeTicks > 0) return false;
        int px = subX(pTile, pDir, pProg), py = subY(pTile, pDir, pProg);
        for (int k = 0; k < eTile.length; k++) {
            int ex = subX(eTile[k], eDir[k], eProg[k]), ey = subY(eTile[k], eDir[k], eProg[k]);
            if (Math.abs(px - ex) + Math.abs(py - ey) < CONTACT) {
                lives--;
                livesLost++;
                evHit = true;
                if (lives <= 0) {
                    status = LOST;
                } else {
                    resetPositions();
                    readyTicks = READY_TICKS;
                    status = READY;
                }
                return true;
            }
        }
        return false;
    }

    public int subX(int tile, int dir, int prog) {
        return (tile % maze.w) * STEPS + Maze.DX[dir] * prog;
    }

    public int subY(int tile, int dir, int prog) {
        return (tile / maze.w) * STEPS + Maze.DY[dir] * prog;
    }

    private void moveEnemy(int k) {
        if (eProg[k] == 0) {
            eDir[k] = chooseDir(k);
            if (eDir[k] == Maze.NONE) return;
        }
        eProg[k]++;
        if (eProg[k] >= STEPS) {
            eTile[k] = maze.step(eTile[k], eDir[k]);
            eProg[k] = 0;
        }
    }

    private int chooseDir(int k) {
        int t = eTile[k], cur = eDir[k], type = maze.enemyType[k];
        if (type == Maze.SENTRY) {
            if (cur != Maze.NONE && maze.step(t, cur) >= 0) return cur;
            int back = Maze.opposite(cur);
            if (back != Maze.NONE && maze.step(t, back) >= 0) return back;
            return sentryStartDir(t);
        }
        int[] opts = new int[4];
        int n = 0;
        int back = Maze.opposite(cur);
        for (int d = 1; d <= 4; d++) if (d != back && maze.step(t, d) >= 0) opts[n++] = d;
        if (n == 0) return back != Maze.NONE && maze.step(t, back) >= 0 ? back : Maze.NONE;
        if (type == Maze.DRIFTER) return opts[rng.nextInt(n)];

        int target = type == Maze.HUNTER ? pTile : flankTarget(t);
        int[] dist = maze.distancesFrom(target);
        int best = opts[0], bestD = Integer.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            int nb = maze.step(t, opts[i]);
            if (dist[nb] < bestD) { bestD = dist[nb]; best = opts[i]; }
        }
        return best;
    }

    /** Flanker aims up to 3 open tiles ahead of the player to cut off the path. */
    private int flankTarget(int self) {
        int target = pTile;
        int d = pDir;
        if (d != Maze.NONE) {
            for (int s = 0; s < 3; s++) {
                int n = maze.step(target, d);
                if (n < 0) break;
                target = n;
            }
        }
        return target == self ? pTile : target;
    }

    /** Published score formula (PRD G-04). Same for every maze. */
    public int score() {
        int s = pelletsEaten * 10;
        if (status == WON) {
            int seconds = playTicks / TPS;
            s += 1000 + lives * 500 + Math.max(0, 3000 - seconds * 20);
        }
        return s;
    }

    public int seconds() {
        return playTicks / TPS;
    }

    /** Hash of the full state, used by the determinism test (PRD C-06). */
    public long stateHash() {
        long h = 1125899906842597L;
        int[] vals = {tick, playTicks, readyTicks, freezeTicks, lives, status, pelletsLeft, pTile, pDir, pProg};
        for (int v : vals) h = 31 * h + v;
        for (int k = 0; k < eTile.length; k++) h = 31 * (31 * (31 * h + eTile[k]) + eDir[k]) + eProg[k];
        for (boolean p : pellets) h = 31 * h + (p ? 1 : 0);
        return h;
    }
}
