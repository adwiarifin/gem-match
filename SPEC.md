# Spec: Gem Match (Bejeweled-style clone, Android)

Status: living document. The base game was approved by the owner and updated 2026-10-15 after the core module (T4-T10) was built. The special gems additions (marked below) are a draft waiting for the owner's approval.
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
3. A match is 3 or more in a row or column. Intersecting runs (L, T, +) clear as one union. Special gems never count toward a run.
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

## Special Gems (draft, waiting for approval)

Intent: `docs/intent/special-gems.md`. Four special gems give more variety in moves, fewer stuck states, and a "use it now or save it" decision.

| Made by | Gem | When swapped with a neighbor |
|---|---|---|
| Run of 4 | Blast | Clears the 3x3 around it |
| Run of 5 or more | Colour | Clears every gem of the neighbor's colour |
| L-shape | Line | Clears its row or its column, along the L's longer arm |
| T-shape or + | Cross | Clears its row and its column |

A special gem stays on the board until the owner swaps it with an orthogonal neighbor. That swap is always legal, even when no match would form. Combos stay small: colour + colour clears the board; colour + another special converts every gem of that other special's colour into that special and fires them all; any other pair fires both effects. No bespoke pair effects.

Rules (the owner decided the table and the combos; the rest are my assumptions, to be confirmed):
1. **Inert in matches (confirmed by the owner).** Special gems keep their colour but never count toward a run, so a cascade cannot use one up by accident. Another special's effect can still destroy one.
2. **Chain reactions (confirmed by the owner).** If a firing effect covers another special gem, that gem fires too, from where it sits. Each gem fires once per move. Only the player's own swap can start a chain, and nothing outside the player's move ever fires a special gem. The owner accepts that a chain can use up a special gem the player meant to save, because chains are what make the game fun.
3. **Creation comes from the player's move only (confirmed by the owner).** A special gem is made only by a match group that the player's own swap formed (cascade depth 1). Matches that form later in a cascade, or by newly spawned gems, clear normally and make no special gem, so luck never hands one out. One group makes at most one special gem. If a group qualifies for several, the order is colour, cross, line, blast. A swap that forms two groups can make two special gems. A swap that fires a special gem makes no new one.
4. **Where it appears.** At the swapped cell that belongs to the group. If both swapped cells belong to the same group, at the cell of the gem the player dragged.
5. **L against T.** A group of two crossing runs is an L when the crossing cell is the end of both runs, and a T or + when it is inside at least one run. A group of three or more runs counts as a T if any crossing is inside a run, otherwise as an L.
6. **Line direction.** The longer arm of the L decides: a longer horizontal arm clears the row, a longer vertical arm clears the column. Equal arms clear the row. Fixed at creation and saved.
7. **The colour gem has no colour.** It matches nothing, is drawn in a rainbow style, and fires on whichever neighbor it is swapped with.
8. **Scoring.** Normal run and shape scores stay as in assumption 5. Every gem cleared by an effect scores 10 (`POINTS_PER_EFFECT_GEM`, tunable), times the cascade depth. Making a special gem adds nothing. Starting guesses, to tune by playing.
9. **A swap that fires a special.** The swap happens first, then the special fires from the cell it moved to. Any normal match the swap also formed clears in the same Clear step as the effect. Cascades then continue at depth 2 and up.
10. **Stuck states (hint rule confirmed by the owner).** A board holding any special gem always has a valid move, so it never reshuffles. The idle hint prefers a move that makes a normal match, and points at a special swap only when none exists.

11. **Looks (option B, confirmed by the owner).** The white gem changes from a plus to a pentagon, which frees the plus shape. A special gem keeps its normal shape and colour and gets a white vector overlay with a dark outline: blast is an 8-point burst, a line gem is a stripe across the gem (horizontal for a row, vertical for a column), a cross gem is plus-shaped stripes, and the colour gem is a wheel of the seven gem colours with a white edge. Every special gem also gets a slow shimmer. Effects are animated so you can tell what fired and which gems it took: a ring flash across the 3x3 for blast, a beam along the row or column for line (both for cross), and quick streaks to each taken gem for colour. Mockup: `/tmp/gemlooks-out/special-gem-looks.html` (not kept in the repo).

Save format: `GM1;<score>;<rng>;<64 digits>` becomes `GM2;<score>;<rng>;<64 two-character cells>`. The first character of a cell is the gem digit (`0` to `6`, or `-` for the colour gem). The second is the special kind: `-` none, `B` blast, `R` line (row), `C` line (column), `X` cross, `W` colour. Game encodes only GM2 and still decodes GM1, so a game saved before this version loads into the new one. A corrupt or unknown save returns null (a new game), never a crash. Saves stay under the existing 256-character read limit.

Expected changes: new `Piece.kt`, `Special.kt`, `Creation.kt` and `Activation.kt` in `core`; `Board`, `Matches`, `Move`, `Resolution`, `Step`, `Scoring`, `SaveCodec`, `Reshuffle` and `Game` change. In `app`: `Scene`, `BoardRenderer`, `GemShapes`, `Palette`, `IdleHint` and the playback code change. Names are a starting point.

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
    data class Activate(val origin: Pos, val special: Special, val cells: Set<Pos>) : Step  // before the Clear that removes them
    data class Create(val pos: Pos, val piece: Piece) : Step                                // after a Clear, a special gem appears
}

enum class Special { BLAST, LINE_ROW, LINE_COL, CROSS, COLOUR }
data class Piece(val gem: Gem?, val special: Special? = null)   // gem is null only for COLOUR; Board holds Piece, not Gem
```

`isValidSwap` becomes: adjacent, both cells occupied, and either the swap forms a run or one of the two cells holds a special gem. `findRuns` ignores special pieces.

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
- Special gems, test-first: creation for 4, 5, 6, L, T and + (including L arm direction, equal arms, priority and creation cell; no special gem is made by a cascade match, by a spawned gem, or by a swap that fires a special); inertness; each effect at the centre, an edge and a corner; chain reactions with each gem firing once; every combo including the full-board clear; legality (a special swap is always accepted, a plain no-match swap is still rejected); scoring; GM2 round trip, GM1 still loads, corrupt GM2 returns null; replay of `Activate` and `Create` steps in `TestReplay.kt`.
- Property-style checks with many seeds: every generated board has no matches and at least one move, every accepted move resolves to a board with no matches (also with special gems in play).
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
  - Changing the scoring constants after the owner has played with them.
  - Making special gems join normal matches (Special Gems rule 1).
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
13. Special gems: a run of 4, a run of 5, an L and a T each leave exactly one special gem of the right kind in the right place (in tests).
14. Special gems: swapping any special gem with a neighbor is accepted and fires its effect; colour + colour clears the board; colour + another special fires every converted gem; other pairs fire both effects (in tests).
15. A game saved with special gems on the board loads back identical, and a GM1 save from the current release still loads (in tests).
16. On the A52: a special gem sits unchanged on the board through cascades, pause, and a force-stop and reopen.
17. On the A52: each effect is easy to follow by eye. You can tell what fired, from where, and which gems it took.
18. On the A52, after about ten minutes of play, the owner reports that special gems added variety, and that holding one versus using it now felt like a real choice.

## Out of Scope (v1)

Power-ups other than the four special gems, special gems from matches of six or more, bespoke effects for specific pairs of special gems, levels or goals, timers, "best single move" record, sound, haptics, themes, tablets and landscape, tap-tap input, online anything, ads, accounts, leaderboards, iOS, web, Play Store release.

## Decisions

Decisions, with their implications and rollback, live in `docs/decisions.md`. This file says what to build; that one says why it is built this way.

## Open Questions

1. The tunable numbers (30/60/100, +50 shape bonus, 10 per effect gem, 8 s idle, drag threshold) are starting guesses. Tune them by playing on the A52. Agreed?

## Features

Each feature has an intent file with the owner's confirmed reasons. A rule here that looks odd leads back to its reason there.

- Base game: `docs/intent/bejeweled-clone.md`.
- Special gems: `docs/intent/special-gems.md`. Changed assumptions 2, 3, 7, 10, 11, the Special Gems section, the Core API, success criteria 13 to 18, and Out of Scope.
