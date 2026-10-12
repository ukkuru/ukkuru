import com.ukkuru.mazemakers.core.*;

import java.util.List;

/** Host side checks for the shared core. Run by build.sh before packaging. */
public class CoreTest {
    static int failures = 0;

    static void check(boolean ok, String msg) {
        if (!ok) { failures++; System.out.println("FAIL " + msg); }
    }

    public static void main(String[] args) {
        Maze[] levels = Levels.all();
        for (Maze m : levels) {
            check(m.w == 13 && m.h == 17, m.id + " size " + m.w + "x" + m.h);
            List<String> errs = Validator.check(m);
            check(errs.isEmpty(), m.id + " validation: " + errs);
            int clears = Bot.clears(m);
            System.out.printf("%s %-14s tier %d  bot clears %2d/%d  (bot tier %d)%n",
                m.id, m.title, m.tier, clears, Bot.RUNS, Bot.tierFromClears(clears));
            check(clears >= 1, m.id + " bot never clears it");
        }

        // Traps: a maze where the bot stalls (neither wins nor loses) usually hides
        // dots in a spot only reachable by threading past a patrol. Flag those.
        for (Maze m : levels) {
            int stalls = 0;
            for (long seed = 1; seed <= 60; seed++) {
                Sim s = new Sim(m, seed * 104729L, false);
                while (!s.isOver() && s.tick < Bot.MAX_SECONDS * Sim.TPS) {
                    if (s.pProg == 0) s.setWant(Bot.decide(s));
                    s.step();
                }
                if (!s.isOver()) stalls++;
            }
            check(stalls == 0, m.id + " bot stalled in " + stalls + "/60 runs (possible dot trap)");
        }

        // Determinism (PRD C-06): the same seed and inputs give the same end state.
        for (Maze m : levels) {
            long a = scripted(m, 42), b = scripted(m, 42);
            check(a == b, m.id + " determinism");
        }

        // Round trip of the storage format.
        for (Maze m : levels) {
            Maze r = Maze.deserialize(m.serialize());
            check(r != null && r.serialize().equals(m.serialize()), m.id + " serialize round trip");
        }

        // Validator catches a broken maze.
        Maze bad = Maze.parse("x", "x", "x", 0, new String[]{"#####", "#S..#", "#####"});
        check(!Validator.check(bad).isEmpty(), "validator accepts broken maze");

        System.out.println(failures == 0 ? "ALL CORE TESTS PASSED" : failures + " FAILURES");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** Pseudo random but fixed input script, then hash the state. */
    static long scripted(Maze m, long seed) {
        Sim s = new Sim(m, seed, false);
        int d = 1;
        while (!s.isOver() && s.tick < 60 * Sim.TPS) {
            if (s.tick % 17 == 0) d = (d % 4) + 1;
            s.setWant(d);
            s.step();
        }
        return s.stateHash();
    }
}
