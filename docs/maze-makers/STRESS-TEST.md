# Maze Makers PRD: Stress Test of v0.1

This is the teardown that produced [PRD v0.2](PRD.md). The original is archived at [PRD-v0.1.md](PRD-v0.1.md).

## The verdict

The bones are good. You picked the right fear (moderation), the right instinct on sequencing (make the editor earn its way in), and you wrote non-goals that will save real money. But v0.1 has a crack right at its center: the phase meant to protect the idea measures the wrong thing. Fix that and the rest is tightening bolts.

Below are ten problems, ranked by how much damage each one does, what actually breaks, and the fix that went into v0.2.

## 1. Phase 0 tests a different hypothesis than the one you said matters

Section 1 says the thing to validate first is the social creation loop: a stranger makes a maze, another stranger plays it, the creator sees the result. Phase 0 then ships 50 in-house mazes and zero strangers.

What Phase 0 really tests is "do people like a maze chase game with 50 levels." That is a crowded, mostly free category where the famous original is one search away. If Day 7 lands at 14%, you will shelve the project without ever testing the idea that made it worth building. That's a false negative, and it's the single most expensive mistake on the page.

**Fix in v0.2:** bring strangers into Phase 0 at almost no cost. Commission 20 of the 50 launch mazes from 10 to 20 outside designers, credit them by name, and run an A/B test on attribution. Half of players see "Made by @name. Beat this maze." The other half see "Level 12." If attribution moves plays per session or retention, the social loop has pull. If it doesn't, you learn that in week 6 instead of month 6. (Section 8, Gate A.)

## 2. The "six month editor" fear is aimed at the wrong target

You need an editor to build 50 mazes. You are building one regardless. A 21 by 21 tile grid with six tile types is weeks of work, not months. What eats months is everything around it: deterministic validation, versioning, moderation tooling, discovery.

**Fix in v0.2:** E-00. The internal editor is the creator editor. The team dogfoods it all through Phase 0. The gate decides who gets access, not whether the editor exists. By the time beta creators show up, the tool has built 50 mazes and the rough edges are gone.

## 3. Server replay validation can't work as written

L-04 says the server replays the full input sequence. That only works if the simulation produces identical results on every iPhone, every Android phone and the server. Floating point movement, frame rate dependent timing and an unseeded "random" enemy will diverge on the same inputs. You will reject honest runs and accept fake ones, and you won't know which.

**Fix in v0.2:** new section 5.0, Core Simulation. Fixed tick rate, integer grid movement, seeded random number generator, one headless simulation library shared by client and server, and a simulation version stamped on every run. This also means open question 1 (engine choice) has to be answered before week 1, not "eventually."

## 4. The rules contradict each other

* **Win condition.** G-03 says you win by clearing all pellets. E-01 and E-03 talk about an exit. Which one? v0.2: clear every pellet, then reach the exit. That makes the exit tile mean something and gives creators a design lever.
* **"Enemy free path."** E-03 requires at least one enemy free path to the exit. Enemies roam. No path is ever enemy free. That check can't be built. v0.2 replaces it with two checks that can: a seeded solver bot must clear the maze in at least 3 of 10 runs, and the creator must clear it once themselves before publishing. That second rule is the one Super Mario Maker uses, and it kills most troll levels on its own.
* **"Impossible maze" reports.** M-01 lets players report an impossible maze while E-03 guarantees every maze is possible. v0.2 renames the reason to "unfair or broken," which is what players actually mean.
* **The blocklist.** M-04 says text is "restricted to a blocklist." A blocklist keeps things out, it doesn't define what's allowed in. And "free text is not allowed in map art" stops nobody. A 21 by 21 grid of walls can draw a hate symbol or a slur as pixel art without a single typed character. v0.2 names this risk outright, adds "offensive map art" as a report reason, and covers it with human review in Phase 1.
* **Creator leaderboard at launch.** The overview launches a creator leaderboard, but Phase 0 has no creators besides your own test accounts. A leaderboard of employees teaches you nothing and looks empty to players. v0.2 starts the creator board in Phase 1, when real creators exist.

## 5. Your auto hide rule is a weapon

M-02 hides any maze with 5 unique reports. A jealous creator with five throwaway accounts can take the top maze off Trending in about a minute. On launch week, someone will.

**Fix in v0.2:** guests can't report. Reporters need an account older than 24 hours. Reports are weighted by reporter trust, and people whose reports keep getting rejected lose weight. Every hidden maze gets a human decision within 24 hours, with automatic restore and a creator notification if it's cleared.

## 6. The creator leaderboard rewards the wrong mazes

Unique players, completion rate and average rating sound fair. In practice:

* completion rate rewards trivially easy mazes,
* average rating rewards a creator whose five friends gave five stars,
* unique players rewards whoever farms guest installs.

Publishing the weights is good for trust, but it also means gamers read the manual first.

**Fix in v0.2:** count only qualified players (accounts that play other people's mazes too). Use a Bayesian average so three 5 star votes can't outrank three hundred 4 star votes. Measure completion against the expected rate for the maze's difficulty tier instead of raw. Add a Rising board for creators under a play threshold, so a newcomer has a board they can actually climb. Publish the formula, keep the fraud filters private.

## 7. App store and legal gaps that can block launch

* **Apple Guideline 1.2.** Apps with user generated content must filter objectionable material, take reports and respond in a timely way, let users block abusive users, and publish contact information. v0.1 has no block feature. That is a rejection waiting to happen.
* **Apple Guideline 5.1.1(v).** Apps with account creation must offer account deletion inside the app. Not in v0.1.
* **Pac-Man resemblance goes deeper than the name.** Chase, ambush and random enemies map onto three of Pac-Man's four ghost personalities, and a tile that makes enemies vulnerable is the power pellet. Courts have looked at this exact pattern. In *Atari v. North American Philips* (1982), K.C. Munchkin was found to infringe Pac-Man even though it changed plenty of details. v0.2 swaps the power up to a freeze, adds a Sentry enemy with no Pac-Man equivalent, and puts the enemy set and power up into the legal review, not just the name and art.
* **Guest play sidesteps your age gate.** A 10 year old can open the app and start generating analytics identifiers before ever reaching sign up, which is where v0.1 put the age check. v0.2 moves a neutral age screen to first launch and runs under 13s in a curated only mode with no community content. Counsel needs to confirm this against the amended COPPA Rule now in force, plus the UK Children's Code and EU rules if you soft launch there.
* **Deep links.** Firebase Dynamic Links shut down in August 2025. If S-03 quietly assumed it, pick a replacement before week 1.

## 8. Creator tuned scoring and free text multiply your problems

G-04 lets creators tune the score formula. That makes scores impossible to compare across mazes, adds editor UI, and hands cheaters another lever. v0.2 uses one published formula for every maze.

Same logic for descriptions. 140 characters of free text on every maze is a moderation surface you don't need while you're still proving the loop. v0.2 ships title and tags only.

## 9. Discovery is the product, and v0.1 treats it as a feed

"New" sorted by time plus "Trending" by 72 hour plays is a rich get richer machine. The top 20 mazes soak up every play, new creators get nothing, and they quit. That's your creator burnout risk showing up in week 2, not month 6.

**Fix in v0.2:**

* A "Play next" queue after every run, matched to the player's skill tier. This is the main way a stranger's maze gets played. Feeds are for browsing; the queue is where the plays come from.
* An exposure guarantee. Every approved new maze gets at least 25 plays from people who don't follow its creator within 72 hours.

## 10. The gates have no denominator

* **2,000 waitlist sign ups** means something very different at 5,000 installs than at 500,000. v0.2 uses a rate, plus a floor big enough to fill the beta.
* **20% Day 7 on 400 installs is noise.** v0.2 sets a minimum cohort of 3,000 installs, which means budgeting paid installs in soft launch markets.
* **A 3% share rate in Phase 0** asks people to share a level you made. They won't. v0.2 moves share rate to Gate B, where creators are sharing their own mazes.
* **20% Day 7 is a top tier result** for a casual mobile game. As the bar to even start testing creation, it's built to fail. v0.2 splits it: 15% to open the creator beta, 20% to open the public editor. Your 12% kill line stays exactly where it was.
* **Phase 1 had no exit criteria at all.** v0.2 adds them: creator activation, repeat publishing, share of plays going to community mazes, quality versus curated, and review cost per maze.

## Smaller fixes that also went in

* **Maze versioning.** v0.1 never said what happens when a creator edits a published maze with a leaderboard on it. v0.2: published mazes are immutable, edits create a new version with a fresh leaderboard.
* **Pause abuse.** Pausing a run lets a player study enemy positions. v0.2 blurs the maze while paused.
* **Portrait only for v1 play.** Supporting landscape and portrait doubles UI work for a grid game. Ship portrait, keep tablet landscape for the editor.
* **Assist mode for accessibility.** Slower enemies, clearly labeled, excluded from leaderboards. Accessibility and competitive integrity both win.
* **Watch the best run.** Replays already exist for validation. Letting players watch the number 1 run proves a hard maze is fair and is free marketing.
* **Analytics gaps.** v0.1 had no events for feed impressions, queue plays, abandoned runs, validation failures or ratings. Without them Gate B can't be measured.
* **Open questions now have owners and deadlines.** An open question with no date is a decision you'll make in a panic.

## Things v0.1 got right. Keep them.

* **Committing to gate metrics before seeing the data.** Most teams quietly move the goalposts. You wrote down that you won't.
* **Cosmetic only, no cash out.** Keeps you clear of gambling law and app store trouble.
* **No live multiplayer.** Async leaderboards cost a fraction as much and still scratch the competitive itch.
* **Human review of every Phase 1 maze.** Expensive. Right call.
* **Calling moderation the hardest part.** It is.

## What v0.2 still hasn't proven

Be honest with yourself about these. A document can't fix them; only data can.

1. **Revenue.** Cosmetics alone in casual mobile rarely pay for user acquisition. v0.2 forces a revenue model decision by the end of Phase 0, but the model itself is unproven.
2. **Creators outside the invite list.** The beta recruits people who already love level design. Whether ordinary players become creators is a Phase 2 question.
3. **Moderation cost at scale.** Phase 1 will tell you minutes per review. It won't tell you what happens at 10,000 mazes a week.
4. **The hook.** Without the creation layer, this is a maze chase game in a crowded aisle. The attribution test in Phase 0 is your earliest read on whether the social layer is the reason people stay.

The idea is worth building. v0.2 just makes sure that if it fails, it fails for the right reason, and you find out fast.
