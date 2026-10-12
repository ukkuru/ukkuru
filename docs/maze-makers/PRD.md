# Maze Makers: Product Requirements (v0.2)

**Status:** Draft for review. Supersedes [v0.1](PRD-v0.1.md). Every change from v0.1 is explained in [STRESS-TEST.md](STRESS-TEST.md).

**Legend:** requirements marked **(new)** did not exist in v0.1. Requirements marked **(changed)** were rewritten.

## 1. Overview

Maze Makers is a mobile game where players build mazes, play each other's mazes, and climb player and creator leaderboards. The name, characters, sprites, sound and enemy designs are all ours. We do not use Pac-Man names, art, ghost designs or ghost behaviors.

**The bet:** a social creation loop. A stranger makes something, another stranger plays it, and the creator sees the result. That loop is what makes this more than another maze chase game.

**The hypotheses, in the order we test them:**

| # | Hypothesis | Tested in | How |
|---|------------|-----------|-----|
| H1 | Players enjoy the core chase enough to come back. | Phase 0 | Retention and session data on 50 launch mazes |
| H2 | Players care who made a maze, and will play mazes made by strangers. | Phase 0 | A/B test on creator attribution (section 8) |
| H3 | Some players will create, and their mazes hold up against ours. | Phase 1 | Creator beta with exit criteria (section 9) |
| H4 | Creators come back when strangers play their work. | Phase 1 and 2 | Repeat publish rate, creator retention |

**Launch scope (Phase 0):** 50 mazes, of which 20 are made by named outside designers. Per maze player leaderboards. The editor exists from day one as our internal tool and opens to outside creators only when the gates in section 9 are met.

## 2. Goals and Non Goals

**Goals (each one has a number and a phase)**

* **H2 signal:** Attributed mazes ("Made by @name") get at least as many plays per session as unattributed mazes in the Phase 0 A/B test, measured over a cohort of at least 3,000 installs. *(changed: now testable in Phase 0)*
* **Stranger play:** In Phase 1, at least 20% of new players play a community maze within their first 7 days. *(changed: split "make or play"; making is tracked separately below)*
* **Creation:** In Phase 1, at least 30% of invited creators publish one maze, and at least 10% publish three or more.
* **Creator leaderboard:** Launches in Phase 1. Success means at least 40% of creators who publish once publish again within 14 days.

**Non goals for v1**

* No real money prizes, betting or cash out of any kind.
* No pay to win mechanics. Nothing purchased affects gameplay, scores or rank.
* No live multiplayer. Runs are single player with asynchronous leaderboards.
* No free text on mazes beyond the title. *(new)*
* No creator tunable scoring. *(new)*
* No open editor until the gates in section 9 are met.

## 3. Target Users

* **Players (primary):** Casual puzzle players, 16 to 35, who play in short sessions and like a challenge.
* **Creators (secondary, critical):** Players who enjoy designing levels and sharing them. Small at first, and they supply most of the content.
* **Reality check (new):** A game aimed at 16 to 35 will still be downloaded by 10 to 15 year olds. Every requirement in sections 5.5 and 5.6 assumes minors are present.

## 4. Core Loop

1. Player plays a maze (curated or community).
2. Run ends. Player sees score, time, and rank against the maze's best runs.
3. Player sees the creator's name and a "beat this maze" prompt, and can watch the number 1 run. *(changed)*
4. Player rates the maze (thumbs up or down, plus "too easy / just right / too hard"), reports it, or follows the creator. *(changed: rating defined)*
5. One tap starts the next maze from the "Play next" queue. *(new: this is where most community plays come from)*
6. Creators see plays, ratings and completion rates on their dashboard, and get a weekly plays summary.

## 5. Functional Requirements

### 5.0 Core Simulation (new)

Server side replay validation (L-04) only works if the game produces identical results on every device and on the server. This section is a precondition for leaderboards, anti cheat, offline sync and the solver bot.

| ID | Requirement | Priority |
|----|-------------|----------|
| C-01 | Game logic runs on a fixed tick with integer grid movement. No floating point in game state. Rendering may interpolate; logic may not. | Must |
| C-02 | All randomness comes from a seeded generator. The seed is issued by the server per run (or derived from maze ID and run ID when offline). | Must |
| C-03 | One headless simulation library runs on iOS, Android and the server. No second copy of the game logic on the server. | Must |
| C-04 | A run record contains: maze version ID, simulation version, seed, and every input with its tick number. | Must |
| C-05 | Every simulation change bumps the simulation version. Leaderboards are scoped to a simulation version or re-verified on change. | Must |
| C-06 | A cross device determinism test runs in CI: the same recorded runs must produce identical end states on iOS, Android and server builds. | Must |

### 5.1 Gameplay (Player Mode)

| ID | Requirement | Priority |
|----|-------------|----------|
| G-01 | Player moves through a grid maze collecting pellets while avoiding roaming enemies. | Must |
| G-02 | **(changed)** Each maze places 1 to 4 enemies at fixed spawn points. Enemy types: **Hunter** (pursues the player), **Flanker** (moves to cut off the player's path ahead), **Drifter** (random turns at junctions), **Sentry** (patrols a straight line and reverses at walls). The full set, their look and their behavior go through legal review (section 10). | Must |
| G-03 | **(changed)** Win: collect every pellet, then reach the exit. Touching an enemy costs one life. The run ends in a loss when all 3 lives are gone. | Must |
| G-04 | **(changed)** One published score formula for every maze, based on pellets, time and lives remaining. Creators cannot change it. | Must |
| G-05 | **(changed)** Power up tile: **Freeze** stops all enemies for a fixed duration (proposed 3 seconds). Enemies are not eaten or sent home. | Should |
| G-06 | **(changed)** Pause, resume and restart are available any time before the run ends. The maze is hidden while paused, so pause can't be used to study enemy positions. | Must |
| G-07 | Tutorial maze teaches movement, pellets, enemies and the exit in under 60 seconds. | Must |
| G-08 | **(new)** Controls: swipe with a one move input buffer. Optional on screen d pad. | Must |
| G-09 | **(new)** Assist mode: slower enemies and one extra life, for accessibility. Assist runs are labeled and never appear on leaderboards. | Should |
| G-10 | **(new)** Watch the number 1 run on any maze, replayed from its validated run record. | Should |

### 5.2 Maze Editor

The editor is built once and used twice: by our team in Phase 0, and by creators once gated access opens.

| ID | Requirement | Priority |
|----|-------------|----------|
| E-00 | **(new)** The same editor is used internally to build all curated mazes in Phase 0. No separate internal tool. | Must |
| E-01 | Tiles: wall, pellet, player start, exit, enemy spawn (with type), power up. Nothing else in v1. | Must |
| E-02 | **(changed)** Limits: grid from 11 by 11 to 21 by 21; at least 20 pellets; 1 to 4 enemies; at most 4 power ups. | Must |
| E-03 | **(changed)** Publish validation. (a) Every pellet and the exit are reachable from the start. (b) The solver bot clears the maze in at least 3 of 10 seeded runs within the time cap. (c) All E-02 limits hold. | Must |
| E-04 | **(changed)** Difficulty estimate from solver results (clear rate, lives lost, time), mapped onto the same 5 tiers as curated mazes. The server result is authoritative; the device may show a preview estimate. | Must |
| E-05 | Draft autosave and undo/redo. | Must |
| E-06 | **(changed)** Metadata: title (max 40 characters from the allowed character set) and up to 3 tags from a fixed list. No description in v1. | Must |
| E-07 | **(changed)** Clear check: the creator must complete their own maze once, unassisted, on the current version before it can be published. | Must |
| E-08 | Mazes that fail E-03 or E-07 cannot be published. No override. | Must |
| E-09 | **(new)** Published mazes are immutable. Editing creates a new version with its own leaderboard. Old links open the latest version. | Must |
| E-10 | **(new)** Publish rate cap per creator: 3 per day in Phase 1. Risk based caps in Phase 2. | Must |
| E-11 | **(new)** Phone editing works with pinch zoom, pan and a drag brush. Tested on tablets in landscape. | Must |

### 5.3 Publishing and Discovery

| ID | Requirement | Priority |
|----|-------------|----------|
| D-01 | "New" feed of approved mazes, sorted by publish time. | Must |
| D-02 | **(changed)** "Trending" ranks mazes over the past 72 hours by plays from qualified players and rating, with completion judged against the expected rate for the maze's difficulty tier. | Must |
| D-03 | Every maze has a unique shareable link that opens it directly. | Must |
| D-04 | Creator profile shows published mazes, total plays and average rating. | Must |
| D-05 | Players can follow creators. Follows drive a "From creators you follow" feed. | Should |
| D-06 | Search by maze title and creator name. | Should |
| D-07 | **(new)** "Play next" queue after every run, mixing curated and community mazes matched to the player's skill tier. | Must |
| D-08 | **(new)** Exposure guarantee: each newly approved maze is placed in the queue until it reaches at least 25 plays from non followers, within 72 hours of approval. | Must |

### 5.4 Leaderboards and Ranking

| ID | Requirement | Priority |
|----|-------------|----------|
| L-01 | **(changed)** Each maze version has a best score leaderboard showing the top 100 runs and the player's own rank. | Must |
| L-02 | **(changed)** Creator leaderboard (from Phase 1). Score combines plays from qualified players, Bayesian average rating, and completion rate relative to the difficulty tier. The formula is published. Fraud filters are not. | Must |
| L-03 | Weekly creator board resets every Monday, plus an all time board. | Should |
| L-04 | Scores are accepted only after the server replays the full run record (C-04) and gets the same result. | Must |
| L-05 | **(changed)** Creators earn no points from their own mazes. Accounts linked to the creator by device or payment signals are treated as the creator. | Must |
| L-06 | **(new)** "Rising" creator board for creators under a total play threshold, so new creators have a board they can climb. | Should |
| L-07 | **(new)** Guests can play and see their rank, but guest scores appear on public boards only after the guest creates an account. Guest progress merges into the new account. | Must |

**Qualified player (new definition):** an account at least 3 days old that has played mazes from at least 3 different creators. Used by D-02, L-02 and M-02.

### 5.5 Moderation and Trust

The hardest part of this product. Planned from day one.

| ID | Requirement | Priority |
|----|-------------|----------|
| M-01 | **(changed)** Signed in players can report a maze for: offensive title, offensive map art, spam, unfair or broken, other. Guests cannot report. | Must |
| M-02 | **(changed)** Reports are weighted by reporter trust (account age, history of upheld reports). A maze hides automatically at a weighted score of 5 and enters the review queue. Reporters whose reports are repeatedly rejected lose weight. | Must |
| M-03 | Titles pass an automated text filter before publish. | Must |
| M-04 | **(changed)** Titles use an allowed character set per locale, pass the blocklist, and may not contain links or handles. Map art (shapes drawn with walls or pellets) cannot be caught by text filters and is covered by human review in Phase 1 and the map art report reason after. | Must |
| M-05 | Creators with 3 removed mazes are reviewed. Repeat violators are suspended. | Must |
| M-06 | **(changed)** Anti cheat flags runs faster than the solver's best time by more than a set margin, and input timing that is consistently frame perfect. | Must |
| M-07 | Flagged scores are excluded from leaderboards pending review. | Must |
| M-08 | Large gaps between solver difficulty and player reported difficulty trigger a review. | Should |
| M-09 | **(new)** Review SLA: auto hidden mazes get a human decision within 24 hours. Cleared mazes are restored automatically and the creator is told. | Must |
| M-10 | **(new)** Every removal sends the creator a statement of reasons. Creators can appeal each removal or suspension once, with a decision within 72 hours. | Must |
| M-11 | **(new)** Players can block a creator. Blocked creators' mazes and profile disappear for that player. (Apple Guideline 1.2.) | Must |
| M-12 | **(new)** In app support contact and a published contact address. (Apple Guideline 1.2.) | Must |

### 5.6 Accounts and Identity

| ID | Requirement | Priority |
|----|-------------|----------|
| A-01 | Guest play is allowed. Creating, publishing, rating and reporting require an account. | Must |
| A-02 | Sign in with Apple, Google and email. | Must |
| A-03 | Display names are unique and pass the name filter. | Must |
| A-04 | **(changed)** Neutral age screen at first launch, before any analytics identifier is created. Under 13: no account, curated mazes only, no community content, analytics limited to what is needed to run the app. 13 to 17: account allowed with protective defaults (no follower list shown publicly, no personalized ads). Exact rules confirmed by counsel under the amended COPPA Rule and the rules of each soft launch market before Phase 0 starts. | Must |
| A-05 | **(new)** Account deletion inside the app. (Apple Guideline 5.1.1(v).) | Must |

### 5.7 Sharing

| ID | Requirement | Priority |
|----|-------------|----------|
| S-01 | After each run, a share card shows the maze name, creator, score and play link. | Must |
| S-02 | Share card renders as an image sized for Instagram Stories and TikTok. | Should |
| S-03 | **(changed)** Maze links open the app if installed, or the store listing if not, then open the maze after install. Uses iOS Universal Links and Android App Links plus a deferred deep link provider. (Firebase Dynamic Links shut down in August 2025.) | Must |
| S-04 | **(new)** Creators can share their own published maze with a "Can you beat my maze?" card. | Must |

### 5.8 Monetization

| ID | Requirement | Priority |
|----|-------------|----------|
| R-01 | Cosmetic skins for the player avatar and maze themes. | Should |
| R-02 | Nothing purchased affects gameplay, scores or leaderboard position. | Must |
| R-03 | Creator tip jar is out of scope for v1. Revisit after 90 days of data. | Won't (v1) |
| R-04 | **(new)** Revenue model decision by the end of Phase 0: cosmetics only, or cosmetics plus opt in rewarded ads that grant cosmetic currency (never shown to under 18 accounts). The decision includes a target revenue per daily active user and must cover expected install cost before Phase 2 spend. | Must |

## 6. Non Functional Requirements

* **Performance:** Maze load under 2 seconds on a mid range 2022 Android device. Publish validation (E-03, E-04) completes within 5 seconds on the server for a 21 by 21 grid.
* **Determinism (new):** Zero divergence across iOS, Android and server on the C-06 test suite. Any divergence blocks release.
* **Offline:** Curated mazes play offline. Offline runs keep their full run record and sync when reconnected; they go through the same L-04 validation. Community mazes cache the last 20 played.
* **Size:** Initial install under 60 MB. Matters for India later and older US phones.
* **Platform (changed):** iOS 16 and later, Android 10 and later. Play is portrait only in v1. The editor also supports landscape on tablets.
* **Accessibility:** Colorblind palette option. Haptics can be turned off. Text scales with system settings where possible. Assist mode (G-09).
* **Data retention (changed):** Maze data is kept while the creator account is active. Deleting an account removes its mazes within 30 days. The deleted account's runs on other people's mazes are anonymized, not shown with a name.
* **Privacy:** Collect the minimum analytics needed. Publish a clear privacy policy before soft launch.

## 7. Analytics Events

Track from the first build. Without these, the gates in section 9 are guesswork.

**Play**
* `maze_started`, `maze_completed`, `maze_failed`, `run_abandoned` with maze ID, maze version, creator ID, source (feed, queue, link, follow), and assist flag *(changed)*
* `life_lost` with tick and enemy type *(new)*
* `attribution_variant` assigned per player for the H2 test *(new)*

**Discovery**
* `feed_impression`, `maze_card_tapped` with feed name and position *(new)*
* `queue_next_played`, `queue_skipped` *(new)*
* `best_run_watched` *(new)*

**Social**
* `maze_rated` with thumb and difficulty vote *(new)*
* `follow_creator`, `block_creator` *(changed)*
* `share_clicked` with channel and card type; `share_link_opened` with install attribution *(changed)*

**Creation**
* `editor_opened`, `maze_drafted`, `validation_failed` with reason, `clear_check_attempted`, `maze_published`, each with timing *(changed)*

**Trust**
* `maze_reported` with reason and reporter weight
* `maze_hidden`, `review_decided` with outcome and minutes spent, `appeal_filed`, `appeal_decided` *(new)*
* `run_flagged` with reason

**Retention**
* Day 1, Day 7 and Day 30 retention, split by new vs returning, creator vs player, attribution variant, and install source.

## 8. Launch Plan

**Before week 1 (new)**
* Engine chosen against C-01 to C-06.
* Counsel sign off on the age screen, the minor defaults and the terms of service (creator rights, section 11).
* Legal review of the name, enemy set, power up, art and sound started.
* Review queue owner named.

**Phase 0: Soft launch, curated (weeks 1 to 8)**
* Soft launch in 2 to 3 English speaking test markets outside the US. The US launch waits for Phase 2.
* 50 mazes in 5 difficulty tiers: 30 made in house, 20 commissioned from 10 to 20 outside designers using the real editor (E-00).
* **Attribution A/B test (H2):** half of players see creator names and "beat this maze by @name"; the other half see level numbers only.
* Per maze player leaderboards live. No creator leaderboard yet.
* Paid installs budgeted to reach at least 3,000 installs per measured cohort.
* Creator waitlist open in app, with one question: "What maze would you build first?"
* Measure review minutes per maze on the 20 outside mazes to size Phase 1 moderation.

**Phase 1: Creator beta (weeks 9 to 14, requires Gate A)**
* Invite 200 to 500 creators from the waitlist, Discord, Reddit and maze design communities.
* Editor opens to invitees only. Publish cap of 3 per day.
* Human review of every community maze before it goes live.
* Creator leaderboard, Rising board and Creator of the Week go live.
* Community mazes enter the "Play next" queue with the exposure guarantee.

**Phase 2: Public editor (requires Gate B)**
* Editor opens to all accounts 13 and older.
* US launch.
* Human review of every maze for the first 30 days, then risk based review (new creators and reported mazes reviewed first).

## 9. Gates

Commit to these before seeing the data. Adjust only between phases, in writing, with the reason.

**Minimum sample for any gate:** at least 3,000 installs in the measured cohort. Below that, the gate is not evaluated; extend the phase.

### Gate A: open the creator beta (end of Phase 0)

| Metric | Target |
|--------|--------|
| Day 1 retention | 35% or higher |
| Day 7 retention | 15% or higher |
| Average session length | 6 minutes or higher |
| Day 1 retained players who complete at least 3 mazes | 40% or higher |
| Attributed mazes: plays per session vs unattributed (H2) | Equal or better |
| Outside designer mazes: average rating vs in house | Within 10% |
| Waitlist sign ups | 5% of Day 7 retained players, and at least 300 people |

**Kill line:** if Day 7 is under 12% after 8 weeks, stop. Fix the core loop before any creator work.

**Warning line:** if attributed mazes do worse than unattributed ones, the social loop is weaker than we think. Phase 1 can still run, but the team writes down why before it starts.

### Gate B: open the public editor (end of Phase 1)

| Metric | Target |
|--------|--------|
| Invited creators who publish at least 1 maze | 30% or higher |
| Invited creators who publish 3 or more | 10% or higher |
| Publishing creators who publish again within 14 days | 40% or higher |
| New players who play a community maze within 7 days | 20% or higher |
| Share of all plays going to community mazes | 25% or higher |
| Community maze average rating vs curated | Within 10% |
| Day 7 retention, players exposed to community mazes | 20% or higher |
| Share rate on community maze completions | 3% or higher |
| Median human review time per maze | 3 minutes or less |
| Community mazes removed after review | 10% or less |

## 10. Risks and Mitigations

**Moderation load.** Review work scales with success. Mitigation: title only metadata, publish caps, weighted reports, review SLA, risk based review after 30 days, and Phase 0 review timing to size the team before Phase 1.

**Report brigading (new).** Rival creators mass report a popular maze. Mitigation: no guest reports, account age floor, trust weighted reports, 24 hour review with automatic restore.

**Offensive map art (new).** Walls and pellets can draw symbols or words that no text filter sees. Mitigation: human review in Phase 1, map art report reason, strikes and suspension. A shape classifier is out of scope for v1.

**Cold start for creators.** Most players won't make mazes. Mitigation: 20 outside designer mazes at launch, Rising board, exposure guarantee, Creator of the Week with real placement, not cash.

**Low quality mazes.** Bad mazes kill trust. Mitigation: solver validation, creator clear check, difficulty tiers, ratings, and the play queue matching difficulty to skill.

**Leaderboard gaming.** Bots, scripted runs, alt accounts. Mitigation: deterministic replay validation, solver based speed flags, qualified player rules, linked account detection.

**Determinism bugs (new).** One divergence breaks trust in every leaderboard. Mitigation: shared simulation library and the cross device CI suite (C-03, C-06).

**Pac-Man resemblance (changed).** Game mechanics aren't protected, but the overall look and feel can be. In *Atari v. North American Philips* (1982), K.C. Munchkin was found to infringe Pac-Man. Mitigation: original enemy set including the Sentry, Freeze instead of a vulnerability power up, distinct art and sound, legal review of all of it before any public marketing, and no use of "Pac-Man" in store listings, ads or keyword targeting. Run trademark clearance on "Maze Makers" in every launch market.

**Minors (new).** Under 13s will download the app; 13 to 17s are part of the core audience. Mitigation: first launch age screen, curated only mode for under 13s, protective defaults for teens, counsel sign off before soft launch.

**App store rejection (new).** UGC apps need filtering, reporting, blocking and contact info (Apple Guideline 1.2) and in app account deletion (5.1.1(v)). Mitigation: M-11, M-12, A-05 are Must.

**Revenue (new).** Cosmetics alone rarely pay for installs in casual mobile. Mitigation: R-04 forces a decision with a number attached before Phase 2 spend.

**Editor complexity.** Mitigation: E-00 means one tool built once; v1 is limited to the six tiles in E-01. The expensive parts (validation, versioning, moderation tools) are scoped in their own requirements so their cost is visible.

**Creator burnout.** Creators stop when nobody plays their work. Mitigation: exposure guarantee (D-08), Rising board (L-06), weekly plays summary.

## 11. Decisions Needed (with owners and deadlines)

| # | Question | Recommendation | Owner | Due |
|---|----------|----------------|-------|-----|
| 1 | Engine and framework | Any engine that can host a headless simulation library shared with the server (C-03). Decide on that constraint first, then on editor tooling. | Tech lead | Before week 1 |
| 2 | Who runs the review queue in Phase 1 | A named person with backup coverage, sized from Phase 0 review timing. At 500 creators and 2 mazes each a week at 3 minutes per maze, that is about 50 hours a week. | Founder | Week 6 |
| 3 | Age screen and consent flow | Per A-04, confirmed by counsel for the US and each soft launch market. | Counsel | Before week 1 |
| 4 | Creator rights | Creators keep ownership and grant us a worldwide, royalty free license to host, display, adapt for display and promote their mazes. We never sell a maze on its own. | Counsel | Before week 1 |
| 5 | Valid but offensive mazes | Reports, human review, statement of reasons, appeal, strikes (M-01 to M-10). | Trust owner | Before Phase 1 |
| 6 | Revenue model | Per R-04. | Founder | End of Phase 0 |
| 7 | Soft launch markets | 2 to 3 English speaking markets outside the US with affordable installs. | Founder | Week 0 |

## 12. Summary

Phase 0 no longer just asks "is the chase fun." It also asks "do players care who made the maze," which is the question this product lives or dies on, and it does so with 20 outside mazes and an A/B test instead of a six month bet.

The editor gets built once and proven on our own content. Leaderboards rest on a simulation that gives the same answer everywhere. Moderation assumes bad actors and minors from the first day.

If this fails, it should fail on a clear signal, early, for the right reason. If it works, every gate we pass makes the next one more likely.
