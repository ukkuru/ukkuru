# Maze Makers: Product Requirements (v0.1)

> Archived original. Superseded by [PRD.md](PRD.md) (v0.2). See [STRESS-TEST.md](STRESS-TEST.md) for what changed and why.

## 1. Overview

Maze Makers is a mobile game where players build mazes, play each other's mazes, and climb a creator leaderboard. The core chase mechanic is original. The name, characters, sprites and sound are all ours. We do not use Pac-Man names, art, or ghost designs.

**Launch scope:** 50 hand-made mazes plus a creator leaderboard, with the maze editor opening only after the launch metrics are met.

**Why this matters:** The product is a social creation loop. Players make something, a stranger plays it, and the creator sees the results. That loop is the thing to validate first.

## 2. Goals and Non-Goals

**Goals**
- Prove that players will play mazes made by strangers, not just our own.
- Get at least 1 in 5 active players to make or play a community maze within their first week.
- Build a creator leaderboard that people want to climb.

**Non-goals for v1**
- No real-money prizes, betting or cash-out of any kind.
- No pay-to-win mechanics. Monetization is cosmetic only.
- No multiplayer live chase. Maze runs are single-player with asynchronous leaderboards.
- No open editor until the gating metrics in section 9 are met.

## 3. Target Users

- **Players (primary):** Casual puzzle players, 16 to 35, who play in short sessions and like challenges.
- **Creators (secondary but critical):** Players who enjoy designing levels and sharing them. Expect this group to be small at first and to supply most of the content.

## 4. Core Loop

1. Player plays a maze (curated or community).
2. Run ends. The player sees their score, time and ranking against the maze's best runs.
3. Player sees the creator's name and a "beat this maze" prompt.
4. Player can rate the maze, report it, or follow the creator.
5. Creators see plays, ratings and completion rates on their dashboard.

## 5. Functional Requirements

### 5.1 Gameplay (Player Mode)

| ID | Requirement | Priority |
|----|-------------|----------|
| G-01 | Player navigates a grid maze collecting pellets while avoiding roaming enemies. | Must |
| G-02 | Maze has a fixed set of enemy spawn points and enemy behavior profiles (chase, ambush, random). | Must |
| G-03 | Run ends on enemy contact (loss of one life) or completion of all pellets (win). Three lives per run. | Must |
| G-04 | Score is based on pellets collected, time, and lives remaining. Formula is set per maze by the creator within a fixed range. | Must |
| G-05 | Power-up tile that temporarily flips enemies to vulnerable. Duration is fixed and configurable within limits. | Should |
| G-06 | Pause, resume, and restart are available at any time before the run ends. | Must |
| G-07 | Tutorial maze that teaches movement, pellets, and enemies in under 60 seconds. | Must |

### 5.2 Maze Editor (Creator Mode, Gated)

| ID | Requirement | Priority |
|----|-------------|----------|
| E-01 | Grid editor with walls, pellets, player start, exit, enemy spawns, and power-up tiles. | Must (post-gate) |
| E-02 | Editor enforces a grid size limit (proposed: 21 by 21 maximum) and a minimum pellet count. | Must |
| E-03 | Automatic validation before publish: all pellets reachable from start, exit reachable, at least one enemy-free path to exit. | Must |
| E-04 | Automatic difficulty estimate computed by running a solver bot through the maze. Shown to creator before publish. | Must |
| E-05 | Draft autosave and undo/redo. | Must |
| E-06 | Maze metadata: title (max 40 characters), tags (max 3, from a fixed list), short description (max 140 characters). | Must |
| E-07 | Preview mode where the creator can play their own maze before publishing. | Must |
| E-08 | Creators cannot publish mazes that fail E-03. No override. | Must |

### 5.3 Publishing and Discovery

| ID | Requirement | Priority |
|----|-------------|----------|
| D-01 | Published mazes appear in a "New" feed sorted by publish time. | Must |
| D-02 | A "Trending" feed ranks mazes by plays and completion rate over the past 72 hours. | Must |
| D-03 | Every maze has a unique shareable link that opens the maze directly. | Must |
| D-04 | Creator profile page shows published mazes, total plays, and average rating. | Must |
| D-05 | Players can follow creators. Following drives a "From creators you follow" feed. | Should |
| D-06 | Search by maze title and creator name. | Should |

### 5.4 Leaderboards and Ranking

| ID | Requirement | Priority |
|----|-------------|----------|
| L-01 | Each maze has a best-score leaderboard showing top 100 runs and the player's own rank. | Must |
| L-02 | Global creator leaderboard ranks creators by weighted score: unique players, completion rate, and average rating. Weights are published to users. | Must |
| L-03 | Weekly creator leaderboard resets every Monday, with a separate all-time board. | Should |
| L-04 | Scores are validated server-side. A run must include the full input sequence to be accepted. | Must |
| L-05 | Creators cannot earn leaderboard points from their own mazes. | Must |

### 5.5 Moderation and Trust

This is the hardest part of the product. Plan for it from day one.

| ID | Requirement | Priority |
|----|-------------|----------|
| M-01 | Every player can report a maze with one of a fixed set of reasons (offensive title, impossible maze, spam, other). | Must |
| M-02 | Mazes with 5 or more unique reports are hidden automatically and placed in a review queue. | Must |
| M-03 | Titles and descriptions pass an automated text filter before publish. | Must |
| M-04 | Titles and descriptions are restricted to a blocklist and a character set. Free text is not allowed in map art. | Must |
| M-05 | Creators with 3 removed mazes are reviewed. Repeat violators are suspended. | Must |
| M-06 | Anti-cheat: flag runs that complete implausibly fast or with input patterns that no human produces. | Must |
| M-07 | Flagged scores are excluded from leaderboards pending review. | Must |
| M-08 | Each maze has a difficulty rating from the solver bot and a player-reported difficulty. Large gaps trigger a review. | Should |

### 5.6 Accounts and Identity

| ID | Requirement | Priority |
|----|-------------|----------|
| A-01 | Guest play is allowed. Creating or publishing a maze requires an account. | Must |
| A-02 | Sign-in with Apple, Google, and email. | Must |
| A-03 | Creators pick a display name that is unique and passes the name filter. | Must |
| A-04 | Age gate at sign-up. Users under 13 cannot create accounts. Confirm the exact rule with counsel for US COPPA compliance before launch. | Must |

### 5.7 Sharing

| ID | Requirement | Priority |
|----|-------------|----------|
| S-01 | After each run, a share card shows the maze name, the player's score, and a play link. | Must |
| S-02 | Share card renders as an image suitable for Instagram Stories and TikTok. | Should |
| S-03 | Maze links open in the app if installed, or the store listing if not. | Must |

### 5.8 Monetization (Cosmetic Only)

| ID | Requirement | Priority |
|----|-------------|----------|
| R-01 | Cosmetic skins for the player avatar and maze themes. | Should |
| R-02 | No purchases affect gameplay, scores, or leaderboard position. | Must |
| R-03 | Creator tip jar is out of scope for v1. Revisit after 90 days of data. | Won't (v1) |

## 6. Non-Functional Requirements

- **Performance:** Maze load under 2 seconds on a mid-range Android device from 2022. Validation (E-03 and E-04) completes within 5 seconds for a 21 by 21 grid.
- **Offline:** Curated mazes play offline. Scores sync when reconnected. Community mazes cache the last 20 played.
- **Size:** Initial install under 60 MB. Important for India expansion later and for US users on older phones.
- **Platform:** iOS 16 and later, Android 10 and later. Landscape and portrait both supported. Test the editor on tablets.
- **Accessibility:** Colorblind palette option. Haptic feedback can be turned off. Text scales with system settings where possible.
- **Data retention:** Maze data is kept while the creator account is active. Deleted accounts remove mazes within 30 days.
- **Privacy:** Collect the minimum analytics needed. Publish a clear privacy policy before launch.

## 7. Analytics Events

Track these from the first build. Without them, the gating decision in section 9 is guesswork.

- `maze_started`, `maze_completed`, `maze_failed` with maze ID and creator ID
- `editor_opened`, `maze_drafted`, `maze_published` with timing
- `maze_reported` with reason
- `run_flagged` with reason
- `share_clicked` with channel
- `follow_creator`
- Day 1, Day 7 and Day 30 retention, split by new vs returning and by creator vs player

## 8. Launch Plan

**Phase 0: Curated only (weeks 1 to 8)**
- 50 hand-made mazes in 5 difficulty tiers.
- Creator leaderboard seeded with our own test accounts clearly labeled as internal.
- Track player behavior and ranking dynamics.

**Phase 1: Creator beta (weeks 9 to 14)**
- Invite 200 to 500 creators from Discord, Reddit and maze-design communities.
- Editor opens to invitees only.
- Human review of every community maze before it goes live.
- Measure creator activation and maze quality.

**Phase 2: Public editor (after gate)**
- Open the editor to all accounts that meet the minimum age and account requirements.
- Keep human review for the first 30 days, then move to a risk-based review.

## 9. Gating Metrics (Proposed Starting Points)

Treat these as hypotheses. Adjust them once Phase 0 data is in, but commit to them before you see the data.

| Metric | Target to open editor |
|--------|-----------------------|
| Day 7 retention | 20% or higher |
| Average session length | 6 minutes or higher |
| Players who complete at least 3 curated mazes | 40% or higher |
| Share rate on run completion | 3% or higher |
| Creator interest (sign-ups to beta waitlist) | 2,000 or more |

If Day 7 retention stays under 12% after 8 weeks, pause and fix the core loop before any editor work.

## 10. Risks and Mitigations

**Moderation load.** Community mazes create review work that scales with success. Mitigation: automated filters, report thresholds, risk-based review, and a hard cap on new publishes per creator per day in the early weeks.

**Cold start for creators.** Most players will not make mazes. Mitigation: 50 strong curated mazes, a visible creator leaderboard, and a Creator of the Week feature with real visibility, not a cash prize.

**Low quality mazes.** Bad mazes kill trust. Mitigation: solver validation, difficulty estimation, and a visible quality score on each maze.

**Leaderboard gaming.** Bots and scripted runs break rankings. Mitigation: server-side replay validation and anomaly flagging from day one.

**Pac-Man resemblance.** The mechanic is generic, but the name, characters and visual style are not. Mitigation: legal review of the final name, character designs, and sound before any public marketing. Do not use the phrase "Pac-Man" in store listings, ads, or keyword targeting.

**Editor complexity.** A maze editor can quietly eat six months of engineering. Mitigation: keep v1 editor tools minimal, with only the tiles listed in E-01.

**Creator burnout.** Creators stop when their mazes get no plays. Mitigation: featured placement rotates across new creators, and creators get a plays summary every week.

## 11. Open Questions

1. Which engine or framework will we build on, and does it support the solver bot on-device or only on the server?
2. Who owns the review queue in Phase 1: a founder, a contractor, or a community moderator program?
3. What is the exact age gate and consent flow for US users under COPPA?
4. Do creators keep rights to their mazes, and what does the terms of service say about commercial use?
5. How do we handle a maze that is technically valid but offensive in a way the filter misses?

## 12. Stress-Test Summary

The concept is strong only if the first 8 weeks prove players care about the curated mazes. If they don't, no editor will save it. The highest-risk assumption is that players will make mazes at all. Phase 1 exists to test that assumption cheaply, before the editor becomes a six-month investment.

Build the curated game first, measure hard, and let the creator data earn the next phase.
