package com.ukkuru.mazemakers;

import android.content.Context;
import android.content.SharedPreferences;

import com.ukkuru.mazemakers.core.Maze;

import java.util.ArrayList;
import java.util.List;

/** Local persistence. In the real product scores and mazes live on the server. */
final class Store {
    private final SharedPreferences p;

    Store(Context c) {
        p = c.getSharedPreferences("mazemakers", Context.MODE_PRIVATE);
    }

    int best(String id) { return p.getInt("best_" + id, 0); }
    boolean cleared(String id) { return p.getBoolean("clear_" + id, false); }
    int plays(String id) { return p.getInt("plays_" + id, 0); }
    int rating(String id) { return p.getInt("rate_" + id, 0); }
    int difficultyVote(String id) { return p.getInt("diff_" + id, 0); }
    boolean introSeen(String id) { return p.getBoolean("intro_" + id, false); }
    boolean assist() { return p.getBoolean("assist", false); }
    boolean haptics() { return p.getBoolean("haptics", true); }
    String draft() { return p.getString("draft", null); }

    void recordRun(String id, int score, boolean won, boolean assist) {
        SharedPreferences.Editor e = p.edit();
        e.putInt("plays_" + id, plays(id) + 1);
        // Assist runs never count toward best scores (PRD G-09).
        if (!assist) {
            if (score > best(id)) e.putInt("best_" + id, score);
            if (won) e.putBoolean("clear_" + id, true);
        }
        e.apply();
    }

    void setRating(String id, int v) { p.edit().putInt("rate_" + id, v).apply(); }
    void setDifficultyVote(String id, int v) { p.edit().putInt("diff_" + id, v).apply(); }
    void setIntroSeen(String id) { p.edit().putBoolean("intro_" + id, true).apply(); }
    void setAssist(boolean v) { p.edit().putBoolean("assist", v).apply(); }
    void setHaptics(boolean v) { p.edit().putBoolean("haptics", v).apply(); }
    void setDraft(String rows) { p.edit().putString("draft", rows).apply(); }

    List<Maze> mine() {
        List<Maze> out = new ArrayList<>();
        String ids = p.getString("mine_ids", "");
        for (String id : ids.split(",")) {
            if (id.isEmpty()) continue;
            Maze m = Maze.deserialize(p.getString("mine_" + id, ""));
            if (m != null) out.add(m);
        }
        return out;
    }

    void saveMine(Maze m) {
        String ids = p.getString("mine_ids", "");
        p.edit()
            .putString("mine_" + m.id, m.serialize())
            .putString("mine_ids", ids.isEmpty() ? m.id : m.id + "," + ids)
            .apply();
    }

    void deleteMine(String id) {
        StringBuilder sb = new StringBuilder();
        for (String s : p.getString("mine_ids", "").split(",")) {
            if (s.isEmpty() || s.equals(id)) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(s);
        }
        p.edit().remove("mine_" + id).putString("mine_ids", sb.toString()).apply();
    }

    void resetProgress() {
        boolean a = assist(), h = haptics();
        String ids = p.getString("mine_ids", "");
        SharedPreferences.Editor e = p.edit();
        for (String k : p.getAll().keySet()) {
            if (k.startsWith("best_") || k.startsWith("clear_") || k.startsWith("plays_")
                || k.startsWith("rate_") || k.startsWith("diff_") || k.startsWith("intro_")) e.remove(k);
        }
        e.putBoolean("assist", a).putBoolean("haptics", h).putString("mine_ids", ids).apply();
    }
}
