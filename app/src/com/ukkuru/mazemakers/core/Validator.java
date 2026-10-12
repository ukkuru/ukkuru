package com.ukkuru.mazemakers.core;

import java.util.ArrayList;
import java.util.List;

/** Publish checks from PRD E-02 and E-03 (structure and reachability). */
public final class Validator {
    public static final int MIN_SIZE = 11, MAX_SIZE = 21;
    public static final int MIN_PELLETS = 20;
    public static final int MAX_ENEMIES = 4, MAX_FREEZE = 4;
    public static final int MIN_START_DISTANCE = 4;

    private Validator() {}

    public static List<String> check(Maze m) {
        List<String> errors = new ArrayList<>();
        if (m.w < MIN_SIZE || m.h < MIN_SIZE || m.w > MAX_SIZE || m.h > MAX_SIZE)
            errors.add("Grid must be between " + MIN_SIZE + " and " + MAX_SIZE + " tiles on each side.");
        if (m.start < 0) errors.add("Place a player start.");
        if (m.exit < 0) errors.add("Place an exit.");
        int pellets = m.pelletCount();
        if (pellets < MIN_PELLETS) errors.add("Needs at least " + MIN_PELLETS + " dots (has " + pellets + ").");
        int enemies = m.enemyPos.length;
        if (enemies < 1 || enemies > MAX_ENEMIES) errors.add("Use 1 to " + MAX_ENEMIES + " enemies (has " + enemies + ").");
        if (m.freezeCount() > MAX_FREEZE) errors.add("At most " + MAX_FREEZE + " freeze tiles.");
        if (m.start < 0 || m.exit < 0) return errors;

        int[] dist = m.distancesFrom(m.start);
        int unreachable = 0;
        for (int i = 0; i < m.w * m.h; i++) {
            if ((m.pellet[i] || m.freeze[i]) && dist[i] == Integer.MAX_VALUE) unreachable++;
        }
        if (unreachable > 0) errors.add(unreachable + " dot or freeze tiles can't be reached from the start.");
        if (dist[m.exit] == Integer.MAX_VALUE) errors.add("The exit can't be reached from the start.");
        for (int k = 0; k < enemies; k++) {
            int d = dist[m.enemyPos[k]];
            if (d == Integer.MAX_VALUE) {
                errors.add("An enemy is walled off from the player.");
                break;
            }
            if (d < MIN_START_DISTANCE) {
                errors.add("Enemies must start at least " + MIN_START_DISTANCE + " steps from the player.");
                break;
            }
        }
        return errors;
    }
}
