package com.ukkuru.mazemakers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.ukkuru.mazemakers.core.Bot;
import com.ukkuru.mazemakers.core.Maze;
import com.ukkuru.mazemakers.core.Sim;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowChoreographer;
import org.robolectric.shadows.ShadowDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w393dp-h852dp-port-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SmokeTest {
    private ActivityController<MainActivity> controller;
    private MainActivity act;
    private static final long FRAME_MS = 1000 / Sim.TPS;
    private static final File SCREENS = new File(System.getProperty("screens.dir", "target/screens"));

    @Before
    public void start() {
        // Robolectric normally fakes vsync by advancing the clock on every frame
        // request, so a self reposting game loop never yields. Pausing it and
        // using a 33 ms vsync gives one real frame (layout, draw, game tick) per
        // step of the test clock, like a 30 fps device.
        ShadowChoreographer.setPaused(true);
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(FRAME_MS));
        SCREENS.mkdirs();
        controller = Robolectric.buildActivity(MainActivity.class).setup();
        act = controller.get();
        idle();
    }

    // ------------------------------------------------------------------ flows

    @Test
    public void curatedRunEndToEnd() throws Exception {
        shot("01_home");
        click("Play");
        assertNotNull(find("1.  First Steps"));
        shot("02_maze_list");

        clickCard("1.  First Steps");
        GameView gv = game();
        assertTrue("tutorial overlay shows on first play", (Boolean) get(gv, "showIntro"));
        shot("03_tutorial");
        tap(gv, gv.getWidth() / 2f, gv.getHeight() / 2f);
        assertTrue(!(Boolean) get(gv, "showIntro"));

        // Swipe right and check the move is buffered.
        frames(gv, 60, false);
        swipe(gv, 200, 900, 420, 905);
        assertEquals(Maze.RIGHT, sim(gv).want);
        frames(gv, 60, true);
        shot("04_playing");

        // Back pauses and hides the maze; back again leaves the run.
        act.onBackPressed();
        assertTrue(gv.isPaused());
        frames(gv, 5, false);
        shot("05_paused");
        int tickBefore = sim(gv).tick;
        frames(gv, 30, false);
        assertEquals("sim must not advance while paused", tickBefore, sim(gv).tick);
        RectF resume = (RectF) get(gv, "resumeBtn");
        tap(gv, resume.centerX(), resume.centerY());
        assertTrue(!gv.isPaused());

        Sim s = playToEnd(gv);
        assertTrue(s.isOver());
        idleFor(1500);
        shot("06_result");
        assertNotNull(find(s.status == Sim.WON ? "Maze cleared" : "Out of lives"));

        Store store = new Store(act);
        assertEquals(1, store.plays("c01"));
        click("Liked it");
        click("Just right");
        assertEquals(1, store.rating("c01"));
        assertEquals(2, store.difficultyVote("c01"));
        shot("07_result_rated");

        click("Share score");
        Intent started = shadowOf(act).getNextStartedActivity();
        assertNotNull(started);
        assertEquals(Intent.ACTION_CHOOSER, started.getAction());

        if (s.status == Sim.WON) {
            click("Next maze");
            assertEquals("c02", game().getClass() == GameView.class ? ((Maze) get(game(), "maze")).id : "");
            act.onBackPressed();
            act.onBackPressed();
            assertNotNull(find("Launch mazes"));
        }
    }

    @Test
    public void everyCuratedMazePlaysThroughTheRealView() throws Exception {
        click("Play");
        for (int i = 1; i <= 10; i++) {
            String label = i + ".  ";
            clickCardStartingWith(label);
            GameView gv = game();
            if ((Boolean) get(gv, "showIntro")) tap(gv, 10, gv.getHeight() / 2f);
            frames(gv, 150, true);
            Maze m = (Maze) get(gv, "maze");
            shot("maze_" + m.id);
            Sim s = playToEnd(gv);
            System.out.printf("  %s %-14s %s score %d in %ds, lives lost %d%n", m.id, m.title,
                s.status == Sim.WON ? "WON " : "LOST", s.score(), s.seconds(), s.livesLost);
            idleFor(1500);
            assertNotNull(find(s.status == Sim.WON ? "Maze cleared" : "Out of lives"));
            click("All mazes");
        }
    }

    @Test
    public void editorBuildTestPublishEditDelete() throws Exception {
        click("Make a maze");
        EditorView ed = editor();
        assertNotNull(ed);
        shot("08_editor");

        // Paint a few walls, place a Flanker, undo it, redo it.
        clickChip("Wall");
        touchCell(ed, 3, 3);
        touchCell(ed, 9, 3);
        clickChip("Flanker");
        touchCell(ed, 6, 11);
        assertTrue(String.join("", ed.rows()).contains("F"));
        click("Undo");
        assertTrue(!String.join("", ed.rows()).contains("F"));
        click("Redo");
        assertTrue(String.join("", ed.rows()).contains("F"));
        assertNotNull("draft autosaves", new Store(act).draft());
        shot("09_editor_edited");

        click("Check");
        AlertDialog check = (AlertDialog) ShadowDialog.getLatestDialog();
        assertNotNull(check);
        String checkMsg = shadowOf(check).getMessage().toString();
        System.out.println("  Check dialog: " + checkMsg.replace('\n', ' '));
        check.dismiss();

        // Clear check: beat your own maze before it publishes.
        click("Test & publish");
        GameView gv = game();
        tap(gv, 10, gv.getHeight() / 2f);
        Sim s = playToEnd(gv);
        idleFor(1500);
        if (s.status != Sim.WON) {
            System.out.println("  Bot lost the clear check; retrying once");
            click("Test & publish");
            gv = game();
            tap(gv, 10, gv.getHeight() / 2f);
            s = playToEnd(gv);
            idleFor(1500);
        }
        assertEquals("bot clears the edited starter maze", Sim.WON, s.status);

        AlertDialog name = (AlertDialog) ShadowDialog.getLatestDialog();
        EditText input = findType(name.getWindow().getDecorView(), EditText.class);
        input.setText("www.spam.com");
        name.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        idle();
        assertNotNull("bad title rejected", input.getError());
        input.setText("Bot Test Maze");
        name.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        idle();

        Store store = new Store(act);
        assertEquals(1, store.mine().size());
        assertEquals("Bot Test Maze", store.mine().get(0).title);
        assertNull("draft cleared after publish", store.draft());
        assertNotNull(find("Bot Test Maze"));
        shot("10_my_mazes");

        clickCard("Bot Test Maze");
        assertNotNull(game());
        act.onBackPressed();
        act.onBackPressed();
        assertNotNull(find("My mazes"));

        click("Edit as new version");
        assertNotNull(editor());
        act.onBackPressed();
        click("My mazes");
        click("Delete");
        ((AlertDialog) ShadowDialog.getLatestDialog()).getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        idle();
        assertEquals(0, store.mine().size());
    }

    @Test
    public void validatorBlocksBrokenMazes() throws Exception {
        click("Make a maze");
        click("Clear");
        click("Test & publish");
        AlertDialog d = (AlertDialog) ShadowDialog.getLatestDialog();
        String msg = shadowOf(d).getMessage().toString();
        System.out.println("  Empty maze errors: " + msg.replace('\n', ' '));
        assertTrue(msg.contains("player start"));
        assertTrue(msg.contains("exit"));
        assertNotNull("still on the editor, nothing published", editor());
    }

    @Test
    public void titleFilter() {
        assertNull(MainActivity.titleProblem("Crossroads 2"));
        assertNotNull(MainActivity.titleProblem(""));
        assertNotNull(MainActivity.titleProblem("visit www.example.com"));
        assertNotNull(MainActivity.titleProblem("@someone"));
        assertNotNull(MainActivity.titleProblem("Nazi base"));
        assertNotNull(MainActivity.titleProblem("emoji 😀"));
        assertNotNull(MainActivity.titleProblem("x".repeat(41)));
    }

    @Test
    public void settingsHelpAndAssistMode() throws Exception {
        click("How it works");
        assertNotNull(find("Enemies"));
        shot("11_how_it_works");
        act.onBackPressed();

        click("Settings");
        shot("12_settings");
        List<Switch> switches = new ArrayList<>();
        collect(root(), Switch.class, switches);
        assertEquals(2, switches.size());
        switches.get(0).performClick();
        assertTrue(new Store(act).assist());
        act.onBackPressed();

        click("Play");
        clickCard("2.  Twin Halls");
        GameView gv = game();
        assertTrue(sim(gv).assist);
        assertEquals("assist gives an extra life", 4, sim(gv).lives);
        frames(gv, 120, true);
        shot("13_assist_mode");
        Sim s = playToEnd(gv);
        idleFor(1500);
        assertNotNull(find("Assist run. Not counted toward best scores."));
        assertEquals("assist runs don't set a best", 0, new Store(act).best("c02"));
    }

    @Test
    public void backgroundingPausesAndRelaunchWorks() throws Exception {
        click("Play");
        clickCard("3.  Crossroads");
        GameView gv = game();
        frames(gv, 60, true);
        controller.pause().stop();
        assertTrue("leaving the app pauses the run", gv.isPaused());
        controller.restart().start().resume();
        idle();
        assertTrue("coming back stays paused until the player taps Resume", gv.isPaused());
        controller.pause().stop().destroy();

        // Cold start after the process was killed: best scores survive.
        controller = Robolectric.buildActivity(MainActivity.class).setup();
        act = controller.get();
        idle();
        assertNotNull(find("Maze Makers"));
        click("Play");
        assertNotNull(find("3.  Crossroads"));
    }

    // ------------------------------------------------------------------ helpers

    private Sim playToEnd(GameView gv) throws Exception {
        Method decide = Bot.class.getDeclaredMethod("decide", Sim.class);
        decide.setAccessible(true);
        int guard = 0;
        while (!sim(gv).isOver() && guard++ < Bot.MAX_SECONDS * Sim.TPS) {
            Sim s = sim(gv);
            if (s.pProg == 0) s.setWant((Integer) decide.invoke(null, s));
            frame(gv);
        }
        Sim s = sim(gv);
        frame(gv);
        return s;
    }

    private void frames(GameView gv, int n, boolean bot) throws Exception {
        Method decide = Bot.class.getDeclaredMethod("decide", Sim.class);
        decide.setAccessible(true);
        for (int i = 0; i < n; i++) {
            Sim s = sim(gv);
            if (bot && s.pProg == 0 && !s.isOver()) s.setWant((Integer) decide.invoke(null, s));
            frame(gv);
        }
    }

    /** Advance the clock by one frame; Android runs layout, draw and GameView.doFrame. */
    private void frame(GameView gv) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(FRAME_MS + 1));
    }

    private Sim sim(GameView gv) throws Exception {
        return (Sim) get(gv, "sim");
    }

    private static Object get(Object o, String field) throws Exception {
        Field f = o.getClass().getDeclaredField(field);
        f.setAccessible(true);
        return f.get(o);
    }

    private void idle() {
        shadowOf(Looper.getMainLooper()).idle();
        // One frame so newly set content views get attached, laid out and drawn.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(FRAME_MS + 1));
    }

    private void idleFor(long ms) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms));
    }

    private View root() {
        return ((ViewGroup) act.findViewById(android.R.id.content)).getChildAt(0);
    }

    private GameView game() {
        View r = root();
        return r instanceof GameView ? (GameView) r : null;
    }

    private EditorView editor() {
        return findType(root(), EditorView.class);
    }

    private TextView find(String prefix) {
        return findText(root(), prefix);
    }

    private static TextView findText(View v, String prefix) {
        if (v instanceof TextView && ((TextView) v).getText().toString().startsWith(prefix)) return (TextView) v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                TextView t = findText(g.getChildAt(i), prefix);
                if (t != null) return t;
            }
        }
        return null;
    }

    private static <T> T findType(View v, Class<T> type) {
        if (type.isInstance(v)) return type.cast(v);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                T t = findType(g.getChildAt(i), type);
                if (t != null) return t;
            }
        }
        return null;
    }

    private static <T> void collect(View v, Class<T> type, List<T> out) {
        if (type.isInstance(v)) out.add(type.cast(v));
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) collect(g.getChildAt(i), type, out);
        }
    }

    private void click(String prefix) {
        TextView t = find(prefix);
        assertNotNull("no view starting with \"" + prefix + "\"", t);
        assertTrue(prefix + " is clickable", t.performClick());
        idle();
    }

    private void clickChip(String label) {
        click(label);
    }

    private void clickCard(String label) {
        TextView t = find(label);
        assertNotNull("no card \"" + label + "\"", t);
        ((View) t.getParent()).performClick();
        idle();
    }

    private void clickCardStartingWith(String prefix) {
        clickCard(prefix);
    }

    private void tap(View v, float x, float y) {
        long now = SystemClock.uptimeMillis();
        v.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0));
        v.dispatchTouchEvent(MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, x, y, 0));
        idle();
    }

    private void swipe(View v, float x1, float y1, float x2, float y2) {
        long now = SystemClock.uptimeMillis();
        v.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x1, y1, 0));
        v.dispatchTouchEvent(MotionEvent.obtain(now, now + 30, MotionEvent.ACTION_MOVE, (x1 + x2) / 2, (y1 + y2) / 2, 0));
        v.dispatchTouchEvent(MotionEvent.obtain(now, now + 60, MotionEvent.ACTION_MOVE, x2, y2, 0));
        v.dispatchTouchEvent(MotionEvent.obtain(now, now + 90, MotionEvent.ACTION_UP, x2, y2, 0));
    }

    private void touchCell(EditorView ed, int cx, int cy) throws Exception {
        float tile = (Float) get(ed, "tile"), ox = (Float) get(ed, "ox"), oy = (Float) get(ed, "oy");
        tap(ed, ox + (cx + 0.5f) * tile, oy + (cy + 0.5f) * tile);
    }

    private void shot(String name) throws Exception {
        View decor = act.getWindow().getDecorView();
        if (decor.getWidth() == 0) return;
        Bitmap b = Bitmap.createBitmap(decor.getWidth(), decor.getHeight(), Bitmap.Config.ARGB_8888);
        decor.draw(new Canvas(b));
        try (FileOutputStream out = new FileOutputStream(new File(SCREENS, name + ".png"))) {
            b.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }
}
