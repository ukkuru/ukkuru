package com.ukkuru.mazemakers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.ukkuru.mazemakers.core.Bot;
import com.ukkuru.mazemakers.core.Levels;
import com.ukkuru.mazemakers.core.Maze;
import com.ukkuru.mazemakers.core.Sim;
import com.ukkuru.mazemakers.core.Validator;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private Store store;
    private Maze[] curated;
    private Runnable onBack;
    private GameView game;

    private static final String[] TUTORIAL = {
        "How to play",
        "Swipe to move. You keep going.",
        "Eat every dot to open the door.",
        "Then reach the door to clear.",
        "Touching an enemy costs a life.",
        "The ice star freezes enemies 3s.",
    };

    @Override
    protected void onCreate(Bundle state) {
        setTheme(android.R.style.Theme_Material_NoActionBar);
        super.onCreate(state);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        store = new Store(this);
        curated = Levels.all();
        showHome();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (game != null) game.pause();
    }

    @Override
    public void onBackPressed() {
        if (onBack != null) onBack.run();
        else super.onBackPressed();
    }

    private void show(View v, Runnable back) {
        game = v instanceof GameView ? (GameView) v : null;
        if (game != null) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        onBack = back;
        setContentView(v);
    }

    // ---------------------------------------------------------------- Home

    private void showHome() {
        LinearLayout col = Ui.column(this);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.addView(new Spacer(this, 28));
        col.addView(new Logo(this));
        TextView title = Ui.text(this, "Maze Makers", 34, Ui.TEXT, true);
        title.setGravity(Gravity.CENTER);
        col.addView(title);
        TextView sub = Ui.text(this, "Phase 0 test build " + BuildInfo.VERSION, 14, Ui.MUTED, false);
        sub.setGravity(Gravity.CENTER);
        col.addView(sub);
        col.addView(new Spacer(this, 20));

        int cleared = 0;
        for (Maze m : curated) if (store.cleared(m.id)) cleared++;
        col.addView(Ui.button(this, "Play  (" + cleared + "/" + curated.length + " cleared)", true, v -> showMazes()));
        col.addView(Ui.button(this, "Make a maze", false, v -> openEditor(null)));
        col.addView(Ui.button(this, "My mazes", false, v -> showMine()));
        col.addView(Ui.button(this, "How it works", false, v -> showHelp()));
        col.addView(Ui.button(this, "Settings", false, v -> showSettings()));

        col.addView(new Spacer(this, 24));
        TextView foot = Ui.text(this,
            "Prototype for internal testing. Scores, ratings and mazes are stored on this phone only.",
            12, Ui.MUTED, false);
        foot.setGravity(Gravity.CENTER);
        col.addView(foot);
        show(Ui.page(this, col), null);
    }

    // ---------------------------------------------------------------- Curated list

    private void showMazes() {
        LinearLayout col = Ui.column(this);
        col.addView(header("Launch mazes", "Ten curated mazes across five tiers"));
        for (int i = 0; i < curated.length; i++) {
            final int idx = i;
            col.addView(mazeCard((i + 1) + ".  " + curated[i].title, curated[i], v -> play(idx)));
        }
        col.addView(Ui.button(this, "Back", false, v -> showHome()));
        show(Ui.page(this, col), this::showHome);
    }

    private LinearLayout mazeCard(String label, Maze m, View.OnClickListener l) {
        LinearLayout card = Ui.card(this, l);
        card.addView(Ui.text(this, label, 18, Ui.TEXT, true));
        String tier = m.tier > 0 ? "Tier " + m.tier + " · " + Levels.TIER_NAMES[m.tier] : "Unrated";
        card.addView(Ui.text(this, tier + " · " + m.enemyPos.length + (m.enemyPos.length == 1 ? " enemy" : " enemies") + " · by " + m.creator, 13, Ui.MUTED, false));
        int best = store.best(m.id);
        String status = store.cleared(m.id) ? "Cleared · Best " + best : (store.plays(m.id) > 0 ? "Not cleared yet · Best " + best : "New");
        card.addView(Ui.text(this, status, 13, store.cleared(m.id) ? Ui.ACCENT : Ui.MUTED, false));
        return card;
    }

    private void play(int idx) {
        Maze m = curated[idx];
        boolean firstTime = !store.introSeen(m.id);
        String[] intro = idx == 0 && firstTime ? TUTORIAL : null;
        startRun(m, intro, null, sim -> showResult(m, sim, idx), this::showMazes);
    }

    interface RunEnd { void done(Sim sim); }

    private void startRun(Maze m, String[] intro, String banner, RunEnd end, Runnable quit) {
        if (intro != null) store.setIntroSeen(m.id);
        final GameView[] holder = new GameView[1];
        holder[0] = new GameView(this, m, store.assist(), store.haptics(), intro, banner, new GameView.Listener() {
            @Override public void onRunEnd(Sim sim) { if (game == holder[0]) end.done(sim); }
            @Override public void onQuit() { quit.run(); }
        });
        show(holder[0], () -> {
            if (holder[0].isPaused()) quit.run();
            else holder[0].pause();
        });
    }

    // ---------------------------------------------------------------- Results

    private void showResult(Maze m, Sim sim, int curatedIdx) {
        boolean won = sim.status == Sim.WON;
        int prevBest = store.best(m.id);
        store.recordRun(m.id, sim.score(), won, sim.assist);
        boolean newBest = !sim.assist && sim.score() > prevBest;

        LinearLayout col = Ui.column(this);
        col.addView(header(won ? "Maze cleared" : "Out of lives", m.title + " · by " + m.creator));

        LinearLayout card = Ui.card(this, null);
        TextView big = Ui.text(this, String.valueOf(sim.score()), 44, won ? Ui.ACCENT : Ui.TEXT, true);
        card.addView(big);
        card.addView(Ui.text(this, newBest ? "New personal best" : "Personal best " + Math.max(prevBest, store.best(m.id)), 14, newBest ? Ui.ACCENT : Ui.MUTED, false));
        card.addView(new Spacer(this, 10));
        card.addView(line("Dots", sim.pelletsEaten + " × 10", sim.pelletsEaten * 10));
        if (won) {
            int t = Math.max(0, 3000 - sim.seconds() * 20);
            card.addView(line("Clear bonus", "", 1000));
            card.addView(line("Lives left", sim.lives + " × 500", sim.lives * 500));
            card.addView(line("Time bonus", String.format(Locale.US, "%d:%02d", sim.seconds() / 60, sim.seconds() % 60), t));
        } else {
            card.addView(Ui.text(this, sim.pelletsLeft + " dots left. Bonuses only pay out on a clear.", 13, Ui.MUTED, false));
        }
        if (sim.assist) card.addView(Ui.text(this, "Assist run. Not counted toward best scores.", 13, Ui.ICE, false));
        col.addView(card);

        // Rating (PRD core loop step 4)
        LinearLayout rate = Ui.card(this, null);
        rate.addView(Ui.text(this, "Rate this maze", 15, Ui.TEXT, true));
        LinearLayout thumbs = Ui.row(this);
        LinearLayout diff = Ui.row(this);
        rate.addView(thumbs);
        rate.addView(Ui.text(this, "How hard was it?", 13, Ui.MUTED, false));
        rate.addView(diff);
        Runnable[] redraw = new Runnable[1];
        redraw[0] = () -> {
            thumbs.removeAllViews();
            diff.removeAllViews();
            int r = store.rating(m.id), d = store.difficultyVote(m.id);
            thumbs.addView(Ui.chip(this, "Liked it", r == 1, v -> { store.setRating(m.id, r == 1 ? 0 : 1); redraw[0].run(); }));
            thumbs.addView(Ui.chip(this, "Not for me", r == -1, v -> { store.setRating(m.id, r == -1 ? 0 : -1); redraw[0].run(); }));
            String[] labels = {"Too easy", "Just right", "Too hard"};
            for (int i = 0; i < 3; i++) {
                final int val = i + 1;
                diff.addView(Ui.chip(this, labels[i], d == val, v -> { store.setDifficultyVote(m.id, d == val ? 0 : val); redraw[0].run(); }));
            }
        };
        redraw[0].run();
        col.addView(rate);

        if (curatedIdx >= 0 && curatedIdx + 1 < curated.length && won) {
            col.addView(Ui.button(this, "Next maze: " + curated[curatedIdx + 1].title, true, v -> play(curatedIdx + 1)));
            col.addView(Ui.button(this, "Play again", false, v -> replay(m, curatedIdx)));
        } else {
            col.addView(Ui.button(this, won ? "Play again" : "Try again", true, v -> replay(m, curatedIdx)));
        }
        col.addView(Ui.button(this, "Share score", false, v -> share(m, sim)));
        Runnable back = curatedIdx >= 0 ? this::showMazes : this::showMine;
        col.addView(Ui.button(this, curatedIdx >= 0 ? "All mazes" : "My mazes", false, v -> back.run()));
        show(Ui.page(this, col), back);
    }

    private void replay(Maze m, int curatedIdx) {
        if (curatedIdx >= 0) play(curatedIdx);
        else playMine(m);
    }

    private View line(String label, String detail, int pts) {
        LinearLayout r = Ui.row(this);
        TextView a = Ui.text(this, label + (detail.isEmpty() ? "" : "  (" + detail + ")"), 14, Ui.TEXT, false);
        r.addView(a, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(Ui.text(this, "+" + pts, 14, Ui.TEXT, true));
        r.setPadding(0, Ui.dp(this, 3), 0, Ui.dp(this, 3));
        return r;
    }

    private void share(Maze m, Sim sim) {
        String msg = sim.status == Sim.WON
            ? "I cleared \"" + m.title + "\" in Maze Makers with " + sim.score() + " points. Beat that."
            : "\"" + m.title + "\" in Maze Makers beat me. Think you can clear it?";
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, msg);
        startActivity(Intent.createChooser(i, "Share score"));
    }

    // ---------------------------------------------------------------- Editor

    private static final char[] TOOLS = {'#', '.', ' ', 'S', 'E', '*', 'H', 'F', 'D', 'T'};
    private static final String[] TOOL_NAMES = {"Wall", "Dot", "Erase", "Start", "Exit", "Freeze", "Hunter", "Flanker", "Drifter", "Sentry"};

    private void openEditor(Maze source) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        int p = Ui.dp(this, 12);
        root.setPadding(p, p, p, p);

        TextView status = Ui.text(this, "", 13, Ui.MUTED, false);
        root.addView(Ui.text(this, source == null ? "Maze editor" : "Editing " + source.title, 20, Ui.TEXT, true));
        root.addView(status);

        EditorView ed = new EditorView(this);
        if (source != null) ed.load(source.rows());
        else if (store.draft() != null) ed.load(store.draft().split("/", -1));
        else ed.load(starterRows());
        root.addView(ed, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        // Tool palette: two rows of five so every tool is visible without scrolling.
        Button[] toolButtons = new Button[TOOLS.length];
        Runnable refreshTools = () -> {
            for (int i = 0; i < TOOLS.length; i++) {
                boolean on = ed.tool() == TOOLS[i];
                toolButtons[i].setBackground(Ui.rounded(on ? Ui.ACCENT : Ui.CARD_PRESSED, Ui.dp(this, 20)));
                toolButtons[i].setTextColor(on ? Ui.BG : Ui.TEXT);
            }
        };
        LinearLayout toolRow = null;
        for (int i = 0; i < TOOLS.length; i++) {
            if (i % 5 == 0) {
                toolRow = Ui.row(this);
                root.addView(toolRow);
            }
            final char t = TOOLS[i];
            Button b = Ui.chip(this, TOOL_NAMES[i], false, v -> { });
            b.setOnClickListener(v -> { ed.setTool(t); refreshTools.run(); });
            b.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13);
            b.setPadding(Ui.dp(this, 2), Ui.dp(this, 8), Ui.dp(this, 2), Ui.dp(this, 8));
            ((LinearLayout.LayoutParams) b.getLayoutParams()).setMargins(Ui.dp(this, 2), Ui.dp(this, 6), Ui.dp(this, 2), 0);
            toolButtons[i] = b;
            toolRow.addView(b);
        }
        refreshTools.run();

        LinearLayout actions1 = Ui.row(this);
        actions1.addView(Ui.chip(this, "Undo", false, v -> ed.undo()));
        actions1.addView(Ui.chip(this, "Redo", false, v -> ed.redo()));
        actions1.addView(Ui.chip(this, "Fill dots", false, v -> ed.fillDots()));
        actions1.addView(Ui.chip(this, "Clear", false, v -> ed.newMaze()));
        root.addView(actions1);
        LinearLayout actions2 = Ui.row(this);
        actions2.addView(Ui.chip(this, "Check", false, v -> checkDraft(ed)));
        Button publish = Ui.chip(this, "Test & publish", true, v -> testAndPublish(ed, source));
        actions2.addView(publish);
        root.addView(actions2);

        Runnable updateStatus = () -> {
            List<String> errs = Validator.check(ed.toMaze());
            status.setText(errs.isEmpty() ? "Ready to test. You must clear it yourself before it publishes." : errs.get(0));
            status.setTextColor(errs.isEmpty() ? Ui.ACCENT : Ui.MUTED);
        };
        ed.setListener(msg -> {
            store.setDraft(ed.joined()); // autosave (PRD E-05)
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            updateStatus.run();
        });
        updateStatus.run();
        show(root, this::showHome);
    }

    private static String[] starterRows() {
        return new String[]{
            "#############",
            "#S..........#",
            "#.#########.#",
            "#...........#",
            "#.##.###.##.#",
            "#...........#",
            "#.#.##.##.#.#",
            "#.....D.....#",
            "#.#.##.##.#.#",
            "#...........#",
            "#.##.###.##.#",
            "#...........#",
            "#.#########.#",
            "#...........#",
            "#.#########.#",
            "#..........E#",
            "#############",
        };
    }

    private void checkDraft(EditorView ed) {
        Maze m = ed.toMaze();
        List<String> errs = Validator.check(m);
        if (!errs.isEmpty()) {
            alert("Not ready yet", "• " + String.join("\n• ", errs));
            return;
        }
        int clears = Bot.clears(m);
        int tier = Bot.tierFromClears(clears);
        String verdict = tier == 0
            ? "The solver bot couldn't clear it. Players may find it unfair. The PRD bar is 3 of 10."
            : "Estimated difficulty: " + Levels.TIER_NAMES[tier] + " (tier " + tier + ")."
                + (clears < 3 ? "\nBelow the PRD bar of 3 of 10 clears." : "");
        alert("Checks passed", "Solver bot cleared it " + clears + " of " + Bot.RUNS + " runs.\n" + verdict);
    }

    private void testAndPublish(EditorView ed, Maze source) {
        Maze draft = ed.toMaze();
        List<String> errs = Validator.check(draft);
        if (!errs.isEmpty()) {
            alert("Fix these first", "• " + String.join("\n• ", errs));
            return;
        }
        String[] intro = {
            "Clear check",
            "Beat your own maze to publish it.",
            store.assist() ? "Turn off assist mode first." : "Same rules as everyone else.",
        };
        startRun(draft, intro, "Clear check: beat it to publish", sim -> {
            if (sim.status == Sim.WON && !sim.assist) askTitle(draft, source);
            else {
                Toast.makeText(this, sim.assist ? "Assist runs don't count for the clear check." : "Not cleared. Tweak it or try again.", Toast.LENGTH_LONG).show();
                openEditor(source == null ? null : draft.withMeta(source.id, source.title, source.creator, source.tier));
            }
        }, () -> openEditor(source == null ? null : draft.withMeta(source.id, source.title, source.creator, source.tier)));
    }

    private static final Pattern TITLE_OK = Pattern.compile("[A-Za-z0-9 '!?.,&]{1,40}");
    private static final String[] BLOCKED = {"fuck", "shit", "cunt", "bitch", "nazi", "porn"};

    /** Local stand in for the server text filter (PRD M-03, M-04). */
    static String titleProblem(String t) {
        String s = t.trim();
        if (s.isEmpty()) return "Give it a title.";
        if (!TITLE_OK.matcher(s).matches()) return "Letters, numbers, spaces and . , ! ? ' & only. Max 40.";
        String low = s.toLowerCase(Locale.US).replaceAll("[^a-z]", "");
        for (String b : BLOCKED) if (low.contains(b)) return "That title isn't allowed.";
        if (low.contains("http") || low.contains("www") || s.contains("@")) return "No links or handles in titles.";
        return null;
    }

    private void askTitle(Maze draft, Maze source) {
        EditText input = new EditText(this);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
        input.setSingleLine(true);
        input.setHint("Maze title");
        if (source != null) input.setText(source.title);
        LinearLayout wrap = new LinearLayout(this);
        int p = Ui.dp(this, 20);
        wrap.setPadding(p, Ui.dp(this, 8), p, 0);
        wrap.addView(input, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        AlertDialog d = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("You cleared it. Name your maze.")
            .setView(wrap)
            .setPositiveButton("Publish", null)
            .setNegativeButton("Back to editor", (di, w) -> openEditor(null))
            .setCancelable(false)
            .create();
        d.setOnShowListener(di -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = input.getText().toString().trim();
            String problem = titleProblem(title);
            if (problem != null) {
                input.setError(problem);
                return;
            }
            int tier = Bot.tierFromClears(Bot.clears(draft));
            // Published mazes are immutable; an edit publishes a new version (PRD E-09).
            Maze pub = draft.withMeta("m" + System.currentTimeMillis(), title, "you", tier);
            store.saveMine(pub);
            store.setDraft(null);
            d.dismiss();
            Toast.makeText(this, "Published \"" + title + "\"", Toast.LENGTH_SHORT).show();
            showMine();
        }));
        d.show();
    }

    // ---------------------------------------------------------------- My mazes

    private void showMine() {
        LinearLayout col = Ui.column(this);
        col.addView(header("My mazes", "Published on this phone. Tap to play."));
        List<Maze> mine = store.mine();
        if (mine.isEmpty()) {
            col.addView(Ui.text(this, "Nothing yet. Build one, beat it, and it shows up here.", 15, Ui.MUTED, false));
        }
        for (Maze m : mine) {
            LinearLayout card = mazeCard(m.title, m, v -> playMine(m));
            LinearLayout r = Ui.row(this);
            r.addView(Ui.chip(this, "Edit as new version", false, v -> openEditor(m)));
            r.addView(Ui.chip(this, "Delete", false, v -> confirm("Delete \"" + m.title + "\"?", () -> { store.deleteMine(m.id); showMine(); })));
            card.addView(r);
            col.addView(card);
        }
        col.addView(Ui.button(this, "Make a maze", true, v -> openEditor(null)));
        col.addView(Ui.button(this, "Back", false, v -> showHome()));
        show(Ui.page(this, col), this::showHome);
    }

    private void playMine(Maze m) {
        startRun(m, null, null, sim -> showResult(m, sim, -1), this::showMine);
    }

    // ---------------------------------------------------------------- Help and settings

    private void showHelp() {
        LinearLayout col = Ui.column(this);
        col.addView(header("How it works", "One rule set for every maze"));
        LinearLayout rules = Ui.card(this, null);
        rules.addView(Ui.text(this,
            "Swipe to move. You keep moving until you turn or hit a wall.\n\n"
            + "Eat every dot. That opens the door. Reach the door to clear the maze.\n\n"
            + "Touching an enemy costs one life. You get 3 (4 in assist mode).\n\n"
            + "The ice star freezes every enemy for 3 seconds. Frozen enemies can't hurt you.\n\n"
            + "Pausing hides the maze so nobody can plan on pause.",
            15, Ui.TEXT, false));
        col.addView(rules);

        LinearLayout legend = Ui.card(this, null);
        legend.addView(Ui.text(this, "Enemies", 16, Ui.TEXT, true));
        for (int t = 0; t < 4; t++) {
            LinearLayout r = Ui.row(this);
            r.addView(new Sprite(this, t), new LinearLayout.LayoutParams(Ui.dp(this, 36), Ui.dp(this, 36)));
            TextView tv = Ui.text(this, Ui.ENEMY_BLURB[t], 14, Ui.TEXT, false);
            tv.setPadding(Ui.dp(this, 10), 0, 0, 0);
            r.addView(tv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            r.setPadding(0, Ui.dp(this, 6), 0, 0);
            legend.addView(r);
        }
        col.addView(legend);

        LinearLayout score = Ui.card(this, null);
        score.addView(Ui.text(this, "Scoring (same formula on every maze)", 16, Ui.TEXT, true));
        score.addView(Ui.text(this,
            "Dots: 10 each\nOn a clear only:\n  Clear bonus: 1,000\n  Lives left: 500 each\n  Time bonus: 3,000 minus 20 per second (never below 0)\n\nAssist runs never count toward best scores.",
            14, Ui.TEXT, false));
        col.addView(score);
        col.addView(Ui.button(this, "Back", false, v -> showHome()));
        show(Ui.page(this, col), this::showHome);
    }

    private void showSettings() {
        LinearLayout col = Ui.column(this);
        col.addView(header("Settings", null));
        col.addView(toggle("Assist mode", "Slower enemies and an extra life. Assist runs don't count toward best scores.",
            store.assist(), store::setAssist));
        col.addView(toggle("Haptics", "Buzz when you lose a life, grab a freeze, or open the exit.",
            store.haptics(), store::setHaptics));
        col.addView(Ui.button(this, "Reset progress", false, v -> confirm("Reset best scores, clears and ratings? Your mazes stay.", () -> {
            store.resetProgress();
            Toast.makeText(this, "Progress reset", Toast.LENGTH_SHORT).show();
        })));
        col.addView(Ui.button(this, "Back", false, v -> showHome()));
        col.addView(new Spacer(this, 16));
        col.addView(Ui.text(this, "Build " + BuildInfo.VERSION + " · simulation v" + Sim.SIM_VERSION, 12, Ui.MUTED, false));
        show(Ui.page(this, col), this::showHome);
    }

    interface BoolSink { void set(boolean v); }

    private View toggle(String title, String desc, boolean on, BoolSink sink) {
        LinearLayout card = Ui.card(this, null);
        LinearLayout r = Ui.row(this);
        TextView t = Ui.text(this, title, 17, Ui.TEXT, true);
        r.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Switch sw = new Switch(this);
        sw.setChecked(on);
        sw.setOnCheckedChangeListener((b, v) -> sink.set(v));
        r.addView(sw);
        card.addView(r);
        card.addView(Ui.text(this, desc, 13, Ui.MUTED, false));
        return card;
    }

    // ---------------------------------------------------------------- Helpers

    private View header(String title, String sub) {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 6));
        h.addView(Ui.text(this, title, 28, Ui.TEXT, true));
        if (sub != null) h.addView(Ui.text(this, sub, 14, Ui.MUTED, false));
        return h;
    }

    private void alert(String title, String msg) {
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(title).setMessage(msg).setPositiveButton("OK", null).show();
    }

    private void confirm(String msg, Runnable yes) {
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setMessage(msg).setPositiveButton("Yes", (d, w) -> yes.run()).setNegativeButton("Cancel", null).show();
    }

    static final class Spacer extends View {
        Spacer(Context c, int dp) {
            super(c);
            setLayoutParams(new LinearLayout.LayoutParams(1, Ui.dp(c, dp)));
        }
    }

    /** Draws one enemy for the legend. */
    static final class Sprite extends View {
        private final int type;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        Sprite(Context c, int type) {
            super(c);
            this.type = type;
        }

        @Override
        protected void onDraw(Canvas cv) {
            Ui.drawEnemy(cv, p, path, type, getWidth() / 2f, getHeight() / 2f, Math.min(getWidth(), getHeight()), false, 0);
        }
    }

    /** Home screen mark: the player bot chased by a Hunter. */
    static final class Logo extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        Logo(Context c) {
            super(c);
            setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(c, 90)));
        }

        @Override
        protected void onDraw(Canvas cv) {
            float s = getHeight() * 0.62f, cx = getWidth() / 2f, cy = getHeight() / 2f;
            p.setColor(Ui.DOT);
            for (int i = 1; i <= 3; i++) cv.drawCircle(cx - s * 0.3f - i * s * 0.42f, cy, s * 0.08f, p);
            Ui.drawPlayer(cv, p, cx, cy, s, Maze.LEFT);
            Ui.drawEnemy(cv, p, path, Maze.HUNTER, cx + s * 1.25f, cy, s * 0.9f, false, 0);
        }
    }
}
