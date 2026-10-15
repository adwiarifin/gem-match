# Decisions

Why things are the way they are: the choices made while building Gem Match, what each one costs to reverse, and what depends on it. What the game is and must do lives in `SPEC.md`.

- App name `Gem Match`, package `dev.kesatria.gemmatch`: confirmed by owner.
- 7 gem types: confirmed by owner.
- Swipe-only input (gem plus constrained direction): chosen by owner.
- Tiered scoring is the v1 minimum: chosen by owner. Simple flat scoring and the "best single move" record are the other levels considered.
- Idle hint is in v1, with the definition in `SPEC.md` under "Input and Idle Hint".

## Decisions made while building the core (approved by the owner, 2026-10-15)

The owner approved these without being able to judge their impact yet, and asked that implications and rollback be written down. Each entry says what depends on the decision and what it costs to reverse. General rule: everything here is cheap to reverse today and gets more expensive once the renderer (T11-T15) and real saved games exist.

**D1. `Step.Reshuffle` carries the whole new `Board`, not a list of moves.**
- Why: a dead board is rearranged, and sometimes (rare fallback) replaced by a fresh one. A full board describes both exactly, and replaying steps stays trivial.
- Impact: the renderer (T15) must animate the old layout into the new one itself, by pairing each gem with a same-type destination (for example nearest first). Core does not say which gem went where.
- Rollback: change `Step.Reshuffle` to carry moves (`GemMove` list), make `rearranged()` in `Reshuffle.kt` return the permutation, and adjust `TestReplay.kt` and the reshuffle tests. Core only, roughly 3 files, under an hour today. After T15 it also touches the renderer's reshuffle animation. The fallback fresh board would then need its own step, because a fresh board is not a permutation.

**D2. The +50 shape bonus is once per group, not once per intersecting pair.**
- Why: simpler to explain, and a +, T or L is one visible event. Three or more runs in one group still earn one bonus.
- Impact: score balance only. Larger shapes score slightly less than the per-pair reading.
- Rollback: change one function, `scoreFor(group)` in `Scoring.kt`, plus about 3 tests in `ScoringTest.kt`. No save format impact, because scores are stored as plain numbers. The one cost: a saved high score earned under the old rule is not strictly comparable to scores under the new one.

**D3. Reshuffle keeps the same gems, places them without completing a run, and falls back to a fresh board only if that is impossible.**
- Why: the board keeps its composition. A plain random shuffle almost never works with few gem colours, so run-avoiding placement is needed. The fallback guarantees the game never stalls.
- Impact: gameplay feel only (a reshuffle does not change which gems exist). It happens rarely with 7 colours. Saved games are not affected, because they store the board and random state, not the reshuffle method.
- Rollback: replace `rearranged()` in `Reshuffle.kt` (for example with "always generate a fresh board"). No data or format impact. Past games and saves stay valid. Only future reshuffles differ.

**D4. Swipe geometry lives in `core`, with small `Point` and `BoardLayout` types.**
- Why: testable on the JVM and reusable by any future UI (Compose, KMP port). The owner noted it is arguable whether it belongs in core or an input layer.
- Impact: the app's input handler (T13) must build a `BoardLayout` from the renderer's board position and a `Point` from each touch, then call `resolveSwipe`. A future port must supply the same two things.
- Rollback: move `Swipe.kt` and `SwipeTest.kt` into the app (or a new `input` module). They depend only on `Pos` and `Move`, so nothing else changes. The core boundary check keeps passing either way. Under an hour.

**D5. Code follows ktlint's default (`ktlint_official`) style, which is stricter than the snippet in the "Code Style" section of `SPEC.md`.**
- Why: no config loosening. The tool's default is the standard that ktlint's ecosystem targets, and `CONSTRAINTS.md` says to use the de facto tool as is.
- Impact: every file uses trailing commas and one parameter per line for functions with several parameters. The snippet in `SPEC.md` is the older, more compact look and is intentionally left as a rough illustration.
- Rollback: add a `.editorconfig` with `ktlint_code_style = intellij_idea`, then run `./gradlew ktlintFormat`. Produces one large formatting-only diff and no behaviour change.

## Decisions made after the first playtest (approved by the owner, 2026-10-15)

First playtest: 89 accepted and 18 rejected moves in about 13 minutes, median 59 fps, no swipe missed because of the threshold. Feedback led to these changes.

**D6. A move is decided on release, with a live drag preview (approach C).**
- Why: the owner did not like a move firing while the finger was still down (it fired a median 101 ms after touch).
- Alternatives considered: commit when the drag passes the threshold (first build), release-only with just a lift, tap-tap selection, and live preview plus tap-tap. Live preview was chosen because it is the only one where the swap is visible before it is final. Tap-tap was left out because mixing input methods is a known bug source.
- Impact: `core` gained `previewDrag` (where a drag stands: held cell, neighbour, progress 0..1) and `resolveSwipe` now uses it; the commit threshold moved from 0.3 to 0.35 of a cell. `GameController` moves the held gem and its neighbour on every touch move and decides on release. Constants: lift 120% (`LIFT_SCALE`), commit 35% (`SWIPE_THRESHOLD`), dead zone 8% (`DRAG_DEAD_ZONE`), neighbour sidestep 0.22 cell (`SIDESTEP`), spring-back 0.12 s. The held gem hides under the finger, so the neighbour's movement is what makes the preview readable.
- Rollback: to get back to lift-only release, remove `followFinger` and the preview calls in `GameController.kt`. To go back to the old threshold, set `SWIPE_THRESHOLD` to 0.3 (two core tests change with it). No data impact.

**D7. A reshuffle is announced and slow enough to notice.**
- Why: the owner saw the board change too fast to tell anything had happened.
- Impact: about 1.5 s total: the board rattles under a "No moves left - reshuffling" notice (0.8 s), the gems shrink away (0.3 s), and the new arrangement grows in (0.4 s). Input is blocked meanwhile, so a stuck board costs the player a short pause. The durations and the notice text are constants in `Timeline.kt`.
- Rollback: shorten or remove the shake and notice in `ReshuffleAnimation`. Cosmetic, no data impact.

**D8. New game has a visible transition.**
- Why: same complaint, the new layout appeared instantly.
- Impact: the old gems shrink away (0.2 s) and the new board drops in from the top (about 0.6 s). The score resets immediately and the best score is kept. Input is blocked for the transition.
- Rollback: replace the two-animation timeline in `startNewGame` with an instant load. Cosmetic, no data impact.

**What would be expensive to change later (not decided here, for awareness):** the save format `GM1` once real games are saved on your phone (needs a version bump and a migration), and the `Step` vocabulary once the renderer depends on it (every new step type needs animation code).
