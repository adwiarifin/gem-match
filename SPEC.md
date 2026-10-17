# Spec: Gem Match (Bejeweled-style clone, Android)

Status: living document. The base game was approved by the owner and updated 2026-10-15 after the core module (T4-T10) was built. The special gems additions (marked below) were approved by the owner on 2026-10-17, built, and revised after the owner's playtest the same day (D16 to D20 in `docs/decisions.md`); the owner signed off the on-device session and the feature is built. The special gem rules in one page: `docs/special-gem-rules.md`.
Source of intent: `docs/intent/bejeweled-clone.md`, plus one file per later feature (see Features at the end). Quality bar: `CONSTRAINTS.md`.

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
2. A move is a swap of two orthogonally adjacent gems. A swap that creates no match is rejected and reverts, unless one of the two gems is a special gem (see Special Gems).
3. A match is 3 or more in a row or column. Intersecting runs (L, T, +) clear as one union. A run of 3 with a gem hanging off it can also clear as one group (see Special Gems). Special gems count in a run of their colour; the colour gem never does.
4. After a clear, gems above fall, new gems spawn from above the top edge, and new matches cascade automatically.
5. Scoring is tiered, to reward planned moves over lucky chains. Each cleared run scores by length: 3 gems = 30, 4 gems = 60, 5 or more = 100. A group of runs that intersect (L, T, +) also earns one flat +50 shape bonus per group. Everything is multiplied by cascade depth (1x for the player's own match, 2x for the first chain, and so on). Constants live in one place and are tunable. A "best single move" record is a possible later addition and is not in v1.
6. The starting board has no matches and at least one valid move.
7. If no valid move exists after a cascade settles (a board holding a special gem always has one), the board reshuffles its existing gems until it has no matches and at least one valid move. There is no game over.
8. Core resolves a whole move instantly and returns a list of steps. The presentation layer plays those steps back as animation. State is saved after the move resolves, so a resumed game is never mid-animation.
9. Core owns its random number generator (SplitMix64 or similar), because `kotlin.random.Random` does not expose serializable state. RNG state is part of saved state, so resume continues the exact same future gems.
10. Save format is a compact hand-written text encoding in core. No serialization library, to keep the APK small and core dependency-free. Version `GM2` adds special gems; `GM1` saves still load (see Special Gems).
11. App uses the framework `android.app.Activity` and `SurfaceView` only. No AndroidX, no Compose, no Kotlin dependencies beyond stdlib. Gems are drawn as vector shapes (Canvas paths), no bitmap assets. Each gem type has a distinct shape as well as a distinct color (white is a pentagon). Special gems are the same shape plus a vector overlay.
12. Portrait only. minSdk 26. compileSdk and targetSdk are the latest stable at scaffold time.
13. Distribution is sideloading via `adb install`. No Play Store in v1. The release keystore is never committed.
14. Game and package names avoid the "Bejeweled" trademark (placeholder: `Gem Match`, package `dev.kesatria.gemmatch`). The GitHub repository is named `gem-match`.

## Input and Idle Hint

- Input is swipe only, and a move is decided on release. Touch-down picks the gem up: it grows to 120% and is drawn on top. While the finger is down the gem follows it along the dominant axis, up to one cell, and the neighbour in that direction slides the opposite way (with a small sidestep so both stay visible). This is a preview only; dragging back undoes it. On release, if the drag travelled at least 35% of a cell the swap is attempted, continuing from where the gems are. Otherwise the gems spring back. A drag toward the board edge, a perfect diagonal, and a plain tap do nothing. The gem moves the same way whether or not the swap would match, so the preview never reveals which moves are valid. Input is blocked while steps are animating.
- Idle means: the board is fully settled and the player has not touched the screen for 8 seconds (tunable constant). Then one valid move pulses as a hint. Any touch clears the hint and resets the timer.
- Idle time is the sum of foreground frame deltas from the game loop, never wall-clock time. The loop stops when the surface is destroyed (which happens on pause), so time spent backgrounded (home button, phone call, screen off) never counts. On resume the idle timer restarts at 0 and any hint is cleared.

## Special Gems (approved 2026-10-17, revised after playtest)

Intent: `docs/intent/special-gems.md`. Every rule, the pair table and where each lives in the code: `docs/special-gem-rules.md`. Six special gems give more variety in moves, fewer stuck states, and a "use it now or save it" decision.

| Made by | Gem | When it fires |
|---|---|---|
| Run of 4 | Blast | Clears the 3x3 around it |
| Run of 5 or more | Colour | Clears every gem of the partner's colour |
| Short L: run of 3, one gem hanging off an end | Line | Clears its row or its column, along the run |
| Short T: bar of 3 with a stem of 2 (5 gems, also a 3 by 3 +) | Short X | Clears both diagonals through it, 2 cells out |
| L with both arms 3 or more | Cross | Clears its row and its column |
| Long T: any run of 4 or more crossing inside a run | Full X | Clears both diagonals through it, across the board |

A special gem stays on the board until the player swaps it with an orthogonal neighbour, a match uses it, or another effect covers it. A swap that touches a special is always legal, even when no match would form. Combos stay small: colour + colour clears the board; colour + another special converts every gem of that other special's colour into that special and fires them all (converted line gems copy the partner's direction); two of the same line or blast make one bigger effect, three columns, three rows or a 5x5 square centred where the dragged gem landed; any other pair fires both effects. No other bespoke pair effects.

Rules (the owner decided the table, the combos and the playtest changes; the rest are assumptions to be confirmed):
1. **Specials join matches (changed after playtest, D16).** A special keeps its colour and counts in a run of that colour like a plain gem. The colour gem has no colour and never counts.
2. **Firing and chains (confirmed by the owner, extended in D16).** A special fires when it is swapped, when it is part of a match at any cascade depth, or when another effect covers it. Each special fires once per clear, from where it sits. The owner accepts that a chain can use up a special gem the player meant to save, because chains are what make the game fun. Steps: every `Activate` of a clear comes first, in firing order (the swap's own effects, then matched specials in board order, then chained ones in the order they were reached), followed by one `Clear` that removes every affected gem. A colour gem swapped with another special converts first: its `Activate` lists the converted cells, then one `Activate` follows for each converted gem. There is no separate conversion step. A colour gem covered by an effect takes every gem of the covering gem's colour, and itself (D19).
3. **Creation at any depth (changed after playtest, D16).** A match group makes a special gem at any cascade depth, not only on the player's swap. One group makes at most one special gem. A swap that forms two groups can make two. A clear in which any special fires makes no new special gem.
4. **Where it appears.** At the swapped cell that belongs to the group; if both swapped cells belong to it, at the dragged one. With no swapped cell in the group (a cascade match), at the gem a hanging gem touches, the crossing of an L, T or +, or the middle of a straight run.
5. **Shapes (changed after playtest, D17 and D18).** A group of two or more crossing runs beats everything, including a run of 5 inside it. A group of two crossing runs is an L when the crossing cell is the end of both runs, and a T or + when it is inside at least one run. A group of three or more runs counts as a T if any crossing is inside a run, otherwise as an L. An arm's length is the length of its run, including the corner, so the smallest L is 3 by 3. An L is a cross gem. A T is a short X when every run is exactly 3 and a full X when any run is 4 or more. Without a crossing: a run of 3 with a hanging gem is a line gem, a straight run of 5 or more is the colour gem, and a straight run of 4 is a blast.
6. **Hanging gem (D17, narrowed by D18).** A run of exactly 3 takes at most one gem of the same colour that touches one of its two end cells at a right angle, is in no run itself, and is not taken by another group. The first such gem in board order is used. The hanging gem clears with the run. A gem touching the middle is not taken, so that shape is a plain 3-match. A run of 4 or more takes none. A line gem made this way takes its direction from its run (horizontal run: row, vertical run: column), fixed at creation and saved.
7. **The colour gem has no colour.** It matches nothing, is drawn in a rainbow style, fires on whichever neighbour it is swapped with, and is consumed when it fires (it clears together with the gems it takes).
8. **Scoring.** Normal run and shape scores stay as in assumption 5. A run of 3 with a hanging gem scores like a run of 4 (60) with no shape bonus. Every gem cleared by an effect scores 10 (`POINTS_PER_EFFECT_GEM`, tunable), times the cascade depth. This includes the special gems that fire, and a cell cleared by both a run and an effect scores once. Making a special gem adds nothing. Starting guesses, to tune by playing.
9. **A swap that fires a special.** The swap happens first, then the special fires from the cell it moved to. Any normal match the swap also formed clears in the same Clear step as the effect. Cascades then continue at depth 2 and up. Step order for a clear that makes a special is Clear, Create, Fall, Spawn: the new special gem is placed before gravity, so it falls like any other gem.
10. **Stuck states (hint rule confirmed by the owner).** A board holding any special gem always has a valid move, so it never reshuffles. The idle hint prefers a move that makes a normal match, and points at a special swap only when none exists.
11. **Looks (confirmed by the owner, extended in D17).** The white gem is a pentagon. A special gem keeps its normal shape and colour and gets a white vector overlay with a dark outline (dark on a white gem): blast is an 8-point burst, a line gem is a stripe across the gem (horizontal for a row, vertical for a column), a cross gem is plus-shaped stripes, a short X is a small X, a full X is a larger X inside a ring, and the colour gem is a wheel of the seven gem colours with a white edge. Every special gem also gets a slow shimmer. Effects are animated so you can tell what fired and which gems it took: a ring flash across the 3x3 for blast, a beam along the row or column for line (both for cross), beams along the diagonals for the X gems, and quick streaks to each taken gem for colour.

Save format: `GM1;<score>;<rng>;<64 digits>` becomes `GM2;<score>;<rng>;<64 two-character cells>`. The first character of a cell is the gem digit (`0` to `6`, or `-` for the colour gem). The second is the special kind: `-` none, `B` blast, `R` line (row), `C` line (column), `X` cross, `S` short X, `F` full X, `W` colour. Game encodes only GM2 and still decodes GM1, so a game saved before this version loads into the new one. A corrupt or unknown save returns null (a new game), never a crash. Saves stay under the existing 256-character read limit.

Expected changes: new `Piece.kt`, `Special.kt`, `Creation.kt`, `Activation.kt` and `Combo.kt` in `core`; `Board`, `Matches`, `Move`, `Resolution`, `Step`, `Scoring`, `SaveCodec`, `Reshuffle` and `Game` change. In `app`: `Scene`, `BoardRenderer`, `GemShapes`, `SpecialOverlays`, `Effects`, `IdleHint` and the playback code change.

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
docs/decisions.md           Decision log: why, impact, rollback
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
    data class Activate(val origin: Pos, val special: Special, val cells: Set<Pos>) : Step  // all Activates of a clear come first, then one Clear removes the cells
    data class Create(val pos: Pos, val piece: Piece) : Step                                // after the Clear, before the Fall: a special gem appears
}

enum class Special { BLAST, LINE_ROW, LINE_COL, CROSS, X_SHORT, X_FULL, COLOUR }
data class Piece(val gem: Gem?, val special: Special? = null)   // gem is null only for COLOUR; Board holds Piece, not Gem
```

`isValidSwap` becomes: adjacent, both cells occupied, and either the swap forms a run or one of the two cells holds a special gem. `findRuns` counts a special piece as its colour; the colour gem never counts. A `MatchGroup` can carry one hanging gem.

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
- Special gems, test-first: creation for 4, 5, 6, short L, short T, L, long T and + (including the hanging gem rules, priority and creation cell; a clear that fires a special makes none); specials joining runs, a matched special firing, and creation in cascades; each effect at the centre, an edge and a corner; chain reactions with each gem firing once; every combo including the full-board clear; legality (a special swap is always accepted, a plain no-match swap is still rejected); scoring; GM2 round trip, GM1 still loads, corrupt GM2 returns null; replay of `Activate` and `Create` steps in `TestReplay.kt`.
- Property-style checks with many seeds: every generated board has no matches and at least one move, every accepted move resolves to a board with no matches (also with special gems in play).
- Coverage: `core` line coverage ≥ 80% per `CONSTRAINTS.md`.
- `app`: unit tests for the Android-free classes (`Timeline`, `Scene`, `IdleHint`, session and save logic), as `CONSTRAINTS.md` lists. Rendering, input wiring and lifecycle are verified by hand on the A52: swipe input, animation order, kill-and-resume, rotate and background behavior, frame pacing. Pure helper logic that is testable should move to `core`.
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
  - Changing the scoring constants after the owner has played with them.
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
3. Release APK ≤ 1 MiB (measured 53,686 bytes unsigned, 2026-10-15). The cap is the measured size rounded up to the next MiB.
4. Manifest declares zero permissions. Game works in airplane mode.
5. On the A52: a swap that makes a match plays swap, clear, fall and spawn in order, and cascades chain with rising multiplier.
6. On the A52: an invalid swap animates back and costs nothing.
7. On the A52: force-stop the app mid-cascade, reopen, and see the board and score from the last resolved move. Same future gems appear as would have without the kill.
8. High score persists across New Game and app restarts.
9. A board with no valid moves reshuffles itself, never stalls.
10. New Game asks for confirmation before discarding the current game.
11. Frame time on the A52 feels smooth (informal: no visible stutter during cascades). A measured number is recorded in `CONSTRAINTS.md` once the render loop exists.

12. Pressing home mid-game, then returning: board and score are as left, no hint is showing, and the idle timer restarts from 0.
13. Special gems: a run of 4, a run of 5, a short L, a short T, an L and a long T each leave exactly one special gem of the right kind in the right place, a gem hanging off the middle of a run of 3 makes nothing, and a cascade match makes one too (in tests).
14. Special gems: swapping any special gem with a neighbour is accepted and fires its effect; a matched special fires; colour + colour clears the board; colour + another special fires every converted gem; other pairs fire both effects with the cell counts in `docs/special-gem-rules.md` (in tests).
15. A game saved with special gems on the board loads back identical, and a GM1 save from the current release still loads (in tests).
16. On the A52: a special gem survives pause, and a force-stop and reopen, unchanged. A cascade can match or cover it and use it up.
17. On the A52: each effect is easy to follow by eye. You can tell what fired, from where, and which gems it took.
18. On the A52, after about ten minutes of play, the owner reports that special gems added variety, and that holding one versus using it now felt like a real choice.

## Out of Scope (v1)

Power-ups other than the six special gems, special gems from matches of six or more, bespoke effects for specific pairs of special gems, levels or goals, timers, "best single move" record, sound, haptics, themes, tablets and landscape, tap-tap input, online anything, ads, accounts, leaderboards, iOS, web, Play Store release.

## Decisions

Decisions, with their implications and rollback, live in `docs/decisions.md`. This file says what to build; that one says why it is built this way.

## Open Questions

1. The tunable numbers (30/60/100, +50 shape bonus, 10 per effect gem, 8 s idle, drag threshold) are starting guesses. Tune them by playing on the A52. Agreed?

## Features

Each feature has an intent file with the owner's confirmed reasons. A rule here that looks odd leads back to its reason there.

- Base game: `docs/intent/bejeweled-clone.md`.
- Special gems: `docs/intent/special-gems.md`. Changed assumptions 2, 3, 7, 10, 11, the Special Gems section (revised after playtest: D16, D17; rules in `docs/special-gem-rules.md`), the Core API, success criteria 13 to 18, and Out of Scope.
