# Spec: Gem Match (Bejeweled-style clone, Android)

Status: approved by the owner, living document. Updated 2026-10-15 after the core module (T4-T10) was built.
Source of intent: `docs/intent/bejeweled-clone.md`. Quality bar: `CONSTRAINTS.md`.

Scope check (Phase 0): one product with two layers (`core`, `app`). Not decomposed into a capability map, because the layers are not independently shippable. Layer boundaries are specified below.

## Objective

An offline, native Android match-3 game for the owner's Samsung Galaxy A52. Endless play, no ads, no network, no accounts. The APK stays tiny. Quitting or being interrupted at any moment never loses progress.

Secondary goals: portfolio piece and blog material. The core is pure Kotlin, KMP-ready, and reusable under a second UI later.

Success looks like:
- The owner installs the APK by sideloading and plays it daily.
- Killing the app at any moment (home button, swipe away, battery death) and reopening shows the same board and score.
- Release APK size meets `CONSTRAINTS.md`, and ideally lands at about 1-2 MB.

## Assumptions (correct me, or these stand)

1. Board is 8x8 with 7 gem types (classic Bejeweled).
2. A move is a swap of two orthogonally adjacent gems. A swap that creates no match is rejected and reverts.
3. A match is 3 or more in a row or column. Intersecting runs (L, T, +) clear as one union.
4. After a clear, gems above fall, new gems spawn from above the top edge, and new matches cascade automatically.
5. Scoring is tiered, to reward planned moves over lucky chains. Each cleared run scores by length: 3 gems = 30, 4 gems = 60, 5 or more = 100. A group of runs that intersect (L, T, +) also earns one flat +50 shape bonus per group. Everything is multiplied by cascade depth (1x for the player's own match, 2x for the first chain, and so on). Constants live in one place and are tunable. A "best single move" record is a possible later addition and is not in v1.
6. The starting board has no matches and at least one valid move.
7. If no valid move exists after a cascade settles, the board reshuffles its existing gems until it has no matches and at least one valid move. There is no game over.
8. Core resolves a whole move instantly and returns a list of steps. The presentation layer plays those steps back as animation. State is saved after the move resolves, so a resumed game is never mid-animation.
9. Core owns its random number generator (SplitMix64 or similar), because `kotlin.random.Random` does not expose serializable state. RNG state is part of saved state, so resume continues the exact same future gems.
10. Save format is a compact hand-written text encoding in core. No serialization library, to keep the APK small and core dependency-free.
11. App uses the framework `android.app.Activity` and `SurfaceView` only. No AndroidX, no Compose, no Kotlin dependencies beyond stdlib. Gems are drawn as vector shapes (Canvas paths), no bitmap assets. Each gem type has a distinct shape as well as a distinct color.
12. Portrait only. minSdk 26. compileSdk and targetSdk are the latest stable at scaffold time.
13. Distribution is sideloading via `adb install`. No Play Store in v1. The release keystore is never committed.
14. Game and package names avoid the "Bejeweled" trademark (placeholder: `Gem Match`, package `dev.kesatria.gemmatch`). The repo name stays as is.

## Input and Idle Hint

- Input is swipe only, and a move is decided on release. Touch-down picks the gem up: it grows to 120% and is drawn on top. While the finger is down the gem follows it along the dominant axis, up to one cell, and the neighbour in that direction slides the opposite way (with a small sidestep so both stay visible). This is a preview only; dragging back undoes it. On release, if the drag travelled at least 35% of a cell the swap is attempted, continuing from where the gems are. Otherwise the gems spring back. A drag toward the board edge, a perfect diagonal, and a plain tap do nothing. The gem moves the same way whether or not the swap would match, so the preview never reveals which moves are valid. Input is blocked while steps are animating.
- Idle means: the board is fully settled and the player has not touched the screen for 8 seconds (tunable constant). Then one valid move pulses as a hint. Any touch clears the hint and resets the timer.
- Idle time is the sum of foreground frame deltas from the game loop, never wall-clock time. The loop stops in `onPause`, so time spent backgrounded (home button, phone call, screen off) never counts. On resume the idle timer restarts at 0 and any hint is cleared.

## Tech Stack

- Language: Kotlin (latest stable at scaffold time, pinned in `gradle/libs.versions.toml`).
- Build: Gradle with wrapper, Kotlin DSL, version catalog. Android Gradle Plugin for `app` only.
- `core`: plain `kotlin("jvm")` module. Kotlin stdlib only. No `android.*`, `java.*`, `javax.*` imports.
- `app`: Android application module. Framework APIs only.
- Test: `kotlin.test` on JUnit for `core`. Kover for coverage. ktlint and detekt for style and static analysis.
- Dependencies: none beyond Kotlin stdlib and test libraries.

## Commands

```
Build debug APK:     ./gradlew :app:assembleDebug
Build release APK:   ./gradlew :app:assembleRelease
Install on phone:    adb install -r app/build/outputs/apk/release/app-release.apk
Core tests:          ./gradlew :core:test
Core coverage gate:  ./gradlew :core:koverVerify
Compile core only:   ./gradlew :core:compileKotlin
Style:               ./gradlew ktlintCheck
Static analysis:     ./gradlew detekt lint
Core boundary:       scripts/check-core-boundary.sh
APK size:            scripts/check-apk-size.sh
Secrets:             gitleaks detect --redact --no-banner
Dependencies:        osv-scanner scan source -r .
```

## Project Structure

```
core/src/main/kotlin/...    Pure game logic (board, match, cascade, scoring, rng, save codec, swipe geometry)
core/src/test/kotlin/...    Unit tests, written test-first
app/src/main/kotlin/...     Activity, SurfaceView, game loop, renderer, input, persistence
app/src/main/res/           Manifest resources, launcher icon (vector)
scripts/                    check-core-boundary.sh, check-apk-size.sh
docs/intent/                Confirmed intent
tasks/                      plan.md, todo.md (created in the planning phase)
SPEC.md, CONSTRAINTS.md, CLAUDE.md
```

Blog posts live in the owner's separate `../kesatriadev` repo. Nothing in this repo writes to it.

## Core API (shape, not final signatures)

Core is pure and immutable. The app drives it, never the reverse.

```kotlin
class Game private constructor(val board: Board, val score: Int, private val rng: Rng) {
    fun trySwap(a: Pos, b: Pos): SwapOutcome          // Rejected, or Accepted(game, steps)
    fun encode(): String                              // save format
    companion object {
        fun new(seed: Long): Game
        fun decode(text: String): Game?               // null on corrupt data
    }
}

sealed interface Step {                               // what the renderer animates
    data class Swap(val a: Pos, val b: Pos) : Step
    data class Clear(val cells: Set<Pos>, val points: Int, val depth: Int) : Step
    data class Fall(val moves: List<Move>) : Step     // existing gems drop
    data class Spawn(val gems: List<Placed>) : Step   // new gems enter from the top
    data class Reshuffle(val board: Board) : Step     // dead board rearranged into this one
}
```

The high score is stored by the app, not by `Game`, because it outlives a game.

## Code Style

Kotlin official style, enforced by ktlint. Immutable data, expression bodies, no shared mutable state in core, time and randomness injected.

```kotlin
data class Pos(val row: Int, val col: Int) {
    fun isAdjacentTo(other: Pos): Boolean =
        abs(row - other.row) + abs(col - other.col) == 1
}

fun findMatches(board: Board): Set<Pos> =
    (runsIn(board.rows()) + runsIn(board.columns()))
        .filter { it.size >= MIN_MATCH }
        .flatMapTo(mutableSetOf()) { it }
```

Conventions: `PascalCase` types, `camelCase` functions, `UPPER_SNAKE` constants, one top-level concept per file, no wildcard imports, no `!!`, no `TODO()` in committed code.

## Testing Strategy

- `core`: test-first (red, green, refactor). Behavior tests per rule: match detection (3, 4, 5, L, T), swap validity, gravity, cascade depth and scoring, no-valid-move detection, reshuffle result is valid, initial board validity, RNG determinism, encode/decode round trip, corrupt save returns null.
- Property-style checks with many seeds: every generated board has no matches and at least one move, every accepted move resolves to a board with no matches.
- Coverage: `core` line coverage ≥ 80% per `CONSTRAINTS.md`.
- `app`: no unit-test target in v1. Verified by hand on the A52: swipe input, animation order, kill-and-resume, rotate and background behavior, frame pacing. Pure helper logic discovered in `app` that is testable should move to `core`.
- Size and boundary checks run as scripts (`CONSTRAINTS.md`).

## Boundaries

- Always:
  - Read `CONSTRAINTS.md` before coding and run the checks it assigns to each stage.
  - Write core tests first.
  - Keep `core` free of Android and `java.*` imports.
  - Inject time and randomness into core.
  - Save state after every resolved move and in `onPause`/`onStop`, with atomic file replacement (write temp, rename).
  - Treat any unreadable save as a new game, never a crash.
- Ask first:
  - Adding any dependency, including AndroidX.
  - Raising an APK size cap, or editing `CONSTRAINTS.md`.
  - Changing the save format after the first release (needs a version byte and migration).
  - Adding bitmap, audio or font assets.
  - Anything that touches `../kesatriadev`.
  - Creating a Play Store listing or adding network permission.
- Never:
  - Request `INTERNET` or any other permission. The manifest declares none.
  - Commit keystores, passwords or secrets.
  - Skip, delete or weaken tests, or add suppressions to get a check green.
  - Put game rules in `app`.
  - Use `git add .`, or commit without the owner's go-ahead.

## Success Criteria

1. `./gradlew :core:test` passes. `:core:koverVerify` reports ≥ 80% on `core`.
2. `scripts/check-core-boundary.sh` finds no `android.*`, `java.*`, `javax.*` import in `core`.
3. Release APK ≤ 10 MiB today (the placeholder cap), with the target of ~1-2 MiB recorded and the cap ratcheted down after the first build.
4. Manifest declares zero permissions. Game works in airplane mode.
5. On the A52: a swap that makes a match plays swap, clear, fall and spawn in order, and cascades chain with rising multiplier.
6. On the A52: an invalid swap animates back and costs nothing.
7. On the A52: force-stop the app mid-cascade, reopen, and see the board and score from the last resolved move. Same future gems appear as would have without the kill.
8. High score persists across New Game and app restarts.
9. A board with no valid moves reshuffles itself, never stalls.
10. New Game asks for confirmation before discarding the current game.
11. Frame time on the A52 feels smooth (informal: no visible stutter during cascades). A measured number is recorded in `CONSTRAINTS.md` once the render loop exists.

12. Pressing home mid-game, then returning: board and score are as left, no hint is showing, and the idle timer restarts from 0.

## Out of Scope (v1)

Special gems and power-ups, levels or goals, timers, "best single move" record, sound, haptics, themes, tablets and landscape, tap-tap input, online anything, ads, accounts, leaderboards, iOS, web, Play Store release.

## Decisions Log

- App name `Gem Match`, package `dev.kesatria.gemmatch`: confirmed by owner.
- 7 gem types: confirmed by owner.
- Swipe-only input (gem plus constrained direction): chosen by owner.
- Tiered scoring is the v1 minimum: chosen by owner. Simple flat scoring and the "best single move" record are the other levels considered.
- Idle hint is in v1, with the definition above.

### Decisions made while building the core (approved by the owner, 2026-10-15)

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

**D5. Code follows ktlint's default (`ktlint_official`) style, which is stricter than the snippet in "Code Style" below.**
- Why: no config loosening. The tool's default is the standard that ktlint's ecosystem targets, and `CONSTRAINTS.md` says to use the de facto tool as is.
- Impact: every file uses trailing commas and one parameter per line for functions with several parameters. The snippet in "Code Style" is the older, more compact look and is intentionally left as a rough illustration.
- Rollback: add a `.editorconfig` with `ktlint_code_style = intellij_idea`, then run `./gradlew ktlintFormat`. Produces one large formatting-only diff and no behaviour change.

### Decisions made after the first playtest (approved by the owner, 2026-10-15)

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

## Open Questions

1. The tunable numbers (30/60/100, +50 shape bonus, 8 s idle, drag threshold) are starting guesses. Tune them by playing on the A52. Agreed?
