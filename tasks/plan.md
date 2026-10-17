# Implementation Plan: Gem Match (Android match-3)

Spec: `SPEC.md`. Quality bar: `CONSTRAINTS.md`. Tasks tracked in `tasks/todo.md` (markdown, no external tracker).
Status: DRAFT, awaiting owner approval.

## Overview

Build an offline native Android match-3 in 5 phases: scaffold and prove the toolchain on the A52, build the pure-Kotlin `core` test-first, then build the SurfaceView `app` layer on top, add persistence and the idle hint, and finish with a signed release build. Each app task ends with something visible on the phone.

## Architecture Decisions

- **Core before app.** Core has no UI dependencies and is where the rules and most tests live. The app then only plays back `Step`s. Risk of rework is lowest this way.
- **But de-risk the toolchain first.** Phase 0 gets an empty SurfaceView app installed on the real A52 and a first release APK size measured. If the size budget or the `adb` flow is a surprise, we want to know before writing game code.
- **Vertical within layers.** Core tasks are rule slices, each fully tested (match, moves, cascade and score, game and reshuffle, save). App tasks are visible slices (draw board, input, animate, persist, hint).
- **Swipe-to-move geometry lives in `core`.** Input geometry (touch-down point, current point, cell size → `Pos` and `Direction` or "none") is platform-independent math. Putting it in `core` makes it unit-testable and portable. This is a small addition to the spec's core list. Needs owner approval.
- **No app unit-test target in v1** (per spec). App behavior is verified by hand on the A52 with a written checklist per task.
- **Animation lives in `app`.** A small timeline player turns `Step`s into timed visual states. Time comes from the game loop's frame delta, never wall clock.
- **Warn-then-block dates.** ktlint, coverage, detekt, Lint and osv-scanner are warn-only until 2026-10-29 per `CONSTRAINTS.md`. Aim to be green anyway.

## Dependency Graph

```
T1 root+core scaffold ── T3 constraint scripts ──┐
        │                                         │
        └── T2 app scaffold (A52 install, size) ──┤   Checkpoint A
                                                  │
T4 board/rng ── T5 matches ── T6 moves ── T7 cascade+score ── T8 game+reshuffle ── T9 save codec ── T10 swipe geometry
                                                                                      │                     │
                                                                  Checkpoint B (core complete)              │
                                                                                      │                     │
T11 draw board ── T12 gem shapes ── T13 input+swap/revert ── T14 clear/fall/spawn ── T15 HUD+reshuffle anim ── T16 persistence ── T17 idle hint ── T18 release
```

T2 and T3 can run in parallel after T1. T4 onward is strictly sequential. T10 can move anywhere after T4.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Release APK is much bigger than expected (AGP and R8 defaults) | High | Measure in T2 on an empty app. Enable R8, resource shrinking, no AndroidX. Record the real number and ratchet the cap. |
| `adb` or USB debugging on the A52 not set up | Medium | T2 includes the real-device install. Fails early. |
| SurfaceView lifecycle bugs (surface destroyed, thread not stopped, crash on resume) | High | Game loop thread is started and stopped only from `surfaceCreated`/`surfaceDestroyed`/`onPause`/`onResume`. Verified in T11 and again in T16. |
| Animation sequencing gets tangled with input and state | Medium | Core resolves instantly, so animation is playback only. Input is blocked while a timeline plays. Keep one state machine: Idle, Animating. |
| Save corruption or a half-written file | Medium | Atomic write (temp + rename) and a decode that returns null on garbage. Tested in T9 and killed mid-play in T16. |
| Gem shapes unreadable on a 6.5" screen | Low | T12 verified on device. Shapes plus colors by design. |
| Kover has no diff coverage mode | Low | Project-wide 80% on small `core`. Accepted in `CONSTRAINTS.md`. |

## Phase 0: Foundation

### T1: Root Gradle project and `core` module
**Description:** Create Gradle wrapper, root build, version catalog, and a `core` plain-Kotlin module with Kover, ktlint, detekt, and one smoke test. Add `.gitignore` (build outputs, `local.properties`, keystores).
**Acceptance:**
- [ ] `./gradlew :core:test :core:koverVerify ktlintCheck detekt` runs and passes on a smoke test.
- [ ] `core` applies no Android plugin and imports nothing from `android.*`/`java.*`.
- [ ] `.gitignore` excludes `*.jks`, `*.keystore`, `local.properties`, `build/`.
**Verify:** the command above; `gitleaks detect --redact --no-banner`.
**Files:** `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `core/build.gradle.kts`, `.gitignore` (+ wrapper files, generated).
**Scope:** Medium. **Depends on:** none.

### T2: `app` module: empty SurfaceView on the A52
**Description:** Add the Android app module with a framework `Activity`, a `SurfaceView` that clears to a color on a game-loop thread, manifest with zero permissions, portrait only, R8 and resource shrinking on for release.
**Acceptance:**
- [ ] `./gradlew :app:assembleDebug :app:assembleRelease` succeeds.
- [ ] APK installs and runs on the Galaxy A52 via `adb install -r`.
- [ ] Manifest declares no permissions. Home and return does not crash.
- [ ] First release APK size measured and written into `CONSTRAINTS.md` "Measured" table.
**Verify:** build commands; run on device; `aapt2 dump permissions` (or manifest inspection) shows none.
**Files:** `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `MainActivity.kt`, `GameSurfaceView.kt`, `CONSTRAINTS.md`.
**Scope:** Medium. **Depends on:** T1.

### T3: Constraint scripts and check aliases
**Description:** Write `scripts/check-core-boundary.sh` and `scripts/check-apk-size.sh`, plus `Makefile` targets `check-fast`, `check-task`, `check-full` mirroring `CONSTRAINTS.md`.
**Acceptance:**
- [ ] Boundary script fails if a `core` file imports `android.`, `java.`, or `javax.` (proven by a temporary bad import) and passes otherwise.
- [ ] Size script fails above the caps in `CONSTRAINTS.md` and prints actual size.
- [ ] Aliases run the commands listed in `CONSTRAINTS.md`.
**Verify:** run each script in pass and fail case; `make check-fast`.
**Files:** `scripts/check-core-boundary.sh`, `scripts/check-apk-size.sh`, `Makefile`.
**Scope:** Small. **Depends on:** T1 (T2 for size script's input).

### Checkpoint A: Foundation
- [ ] All Phase 0 commands pass. APK runs on the A52. Size baseline recorded, cap ratcheted.
- [ ] Review with owner before core work.

## Phase 1: Core (test-first)

### T4: Board, positions, gems, RNG
**Description:** `Pos`, `Gem` (7 types), immutable `Board` (8x8), `Rng` (SplitMix64 with exposed state).
**Acceptance:**
- [ ] Same seed gives the same sequence; saved state restores the same future values.
- [ ] Board access, replacement and bounds behave; out-of-range is handled without exceptions where the API says so.
**Verify:** `./gradlew :core:test`.
**Files:** `Pos.kt`, `Gem.kt`, `Board.kt`, `Rng.kt`, tests.
**Scope:** Small-Medium. **Depends on:** T1.

### T5: Match detection
**Description:** Find all runs of 3+ in rows and columns, merge intersecting runs into clear groups.
**Acceptance:**
- [ ] Detects 3, 4, 5+ in row and column; L, T, + shapes yield intersecting runs.
- [ ] No false positives on 2-in-a-row or gaps.
**Verify:** `:core:test` (table-driven cases).
**Files:** `Matches.kt`, `MatchesTest.kt`.
**Scope:** Small. **Depends on:** T4.

### T6: Valid moves
**Description:** `isValidSwap`, `findValidMoves`, `hasValidMove`.
**Acceptance:**
- [ ] Only adjacent swaps that create a match are valid.
- [ ] Non-adjacent and out-of-bounds are invalid.
- [ ] `findValidMoves` agrees with brute-force try-every-swap on random boards.
**Verify:** `:core:test`.
**Files:** `Moves.kt`, `MovesTest.kt`.
**Scope:** Small. **Depends on:** T5.

### T7: Cascade resolution and scoring
**Description:** Given a board and a valid swap, produce `Step`s: Swap, Clear (with tiered points and depth multiplier), Fall, Spawn, repeating until stable.
**Acceptance:**
- [ ] Scoring: 3=30, 4=60, 5+=100, +50 for intersecting runs, × depth, as in the spec.
- [ ] Final board has no matches. Gem count stays 64.
- [ ] Same seed and swap give identical steps.
**Verify:** `:core:test` including many-seed property check.
**Files:** `Step.kt`, `Scoring.kt`, `Cascade.kt`, tests.
**Scope:** Medium. **Depends on:** T6.

### T8: `Game`: new game, trySwap, reshuffle
**Description:** `Game.new(seed)` builds a board with no matches and at least one move. `trySwap` returns `Rejected` or `Accepted(game, steps)`. After a cascade, if no valid move remains, emit a `Reshuffle` step and a valid board.
**Acceptance:**
- [ ] 1000 seeds: initial board valid; after many random accepted moves a valid move always exists.
- [ ] Rejected swap changes nothing, costs nothing.
**Verify:** `:core:test`.
**Files:** `Game.kt`, `Reshuffle.kt`, `GameTest.kt`.
**Scope:** Medium. **Depends on:** T7.

### T9: Save codec
**Description:** `Game.encode()` and `Game.decode()` including board, score, and RNG state, with a version byte.
**Acceptance:**
- [ ] Round trip equals original and produces the same future gems.
- [ ] Truncated, empty, wrong-version, and garbage inputs return null.
**Verify:** `:core:test`.
**Files:** `SaveCodec.kt`, `SaveCodecTest.kt`.
**Scope:** Small. **Depends on:** T8.

### T10: Swipe geometry
**Description:** Pure function: touch-down point, current point, board origin and cell size → selected `Pos` and `Direction`, or none, using drag threshold and dominant axis; ignores swipes off the board edge.
**Acceptance:**
- [ ] Dominant-axis ties and sub-threshold drags are handled.
- [ ] Touch outside board selects nothing.
- [ ] Swipe toward the edge gives none.
**Verify:** `:core:test`.
**Files:** `Swipe.kt`, `SwipeTest.kt`.
**Scope:** Small. **Depends on:** T4.

### Checkpoint B: Core complete
- [ ] `check-task` green. Core coverage ≥ 80%. Boundary script green.
- [ ] Owner review of API shape before the UI is built on it.
- [ ] Blog hook: core posts can be written here.

## Phase 2: App

### T11: Draw the board
**Description:** Game loop with fixed timestep. Render an 8x8 grid from a `Game` as colored circles, scaled to the screen with margins. Surface lifecycle handled correctly.
**Acceptance:**
- [ ] Board shows on the A52 in portrait, sized to fit.
- [ ] Home and return, screen off and on, and rotation attempts do not crash or leak the render thread.
**Verify:** build, install, manual checklist on A52.
**Files:** `GameLoop.kt`, `BoardRenderer.kt`, `GameSurfaceView.kt`, `MainActivity.kt`.
**Scope:** Medium. **Depends on:** T2, T8.

### T12: Distinct gem shapes
**Description:** Seven vector-drawn shapes plus colors drawn with Canvas paths, no assets.
**Acceptance:**
- [ ] 7 gems distinguishable by shape alone (grayscale screenshot test by eye).
- [ ] No frame-time regression noticeable.
**Verify:** on device; release APK size check.
**Files:** `GemShapes.kt`, `BoardRenderer.kt`.
**Scope:** Small. **Depends on:** T11.

### T13: Input, swap and invalid revert
**Description:** Wire swipe geometry to `trySwap`. Accepted swaps animate the swap. Rejected swaps animate there and back. Input blocked while animating.
**Acceptance:**
- [ ] Swipe on a gem moves it in the swiped direction.
- [ ] Invalid swap goes and returns, score unchanged.
- [ ] Touches during animation are ignored.
**Verify:** on device.
**Files:** `InputHandler.kt`, `Timeline.kt`, `BoardRenderer.kt`, `GameSurfaceView.kt`.
**Scope:** Medium. **Depends on:** T10, T12.

### T14: Play back clear, fall, and spawn
**Description:** Timeline plays `Clear` (shrink and fade), `Fall` (eased drop), `Spawn` (enter from top) for every step including cascades.
**Acceptance:**
- [ ] A match visibly clears, gems fall, new ones enter, and cascades chain in order.
- [ ] Rendered board always equals the core board when the timeline ends.
**Verify:** on device with several moves, including a cascade.
**Files:** `Timeline.kt`, `Easing.kt`, `BoardRenderer.kt`.
**Scope:** Medium. **Depends on:** T13.

### T15: HUD, New Game, reshuffle animation
**Description:** Score and high score text (default typeface), New Game button, a hand-drawn Canvas confirm dialog that matches the game's style and layers above the board, reshuffle animation. Draw order is board, HUD, dialog; while the dialog is open it consumes all touches and the board gets none.
**Acceptance:**
- [ ] Score updates as `Clear` steps play.
- [ ] New Game opens the Canvas dialog. Cancel (button or system back) keeps the game; Confirm starts a new one.
- [ ] Touches never reach the board while the dialog is open.
- [ ] A reshuffled board animates and remains playable.
**Verify:** on device.
**Files:** `Hud.kt`, `ConfirmDialog.kt`, `BoardRenderer.kt`, `GameSurfaceView.kt`, `MainActivity.kt`.
**Scope:** Medium. **Depends on:** T14.

### Checkpoint C: Playable
- [ ] Owner plays a full session on the A52. Fun check. Tune numbers (scoring, timings).
- [ ] `check-full` green apart from still-warn items.

## Phase 3: Persistence

### T16: Autosave, resume, high score
**Description:** Save encoded game after every resolved move and in `onPause`/`onStop`, atomic write to app-private storage. Load on launch; corrupt means new game. Persist high score separately.
**Acceptance:**
- [ ] Force-stop mid-cascade and reopen: board and score are from the last resolved move; subsequent gems match what would have appeared.
- [ ] Corrupted file (manually damaged via `adb`) leads to a new game, no crash.
- [ ] High score survives New Game and restart.
**Verify:** kill and resume test, `adb shell am force-stop`, file damage test.
**Files:** `SaveStore.kt`, `HighScoreStore.kt`, `MainActivity.kt`, `GameSurfaceView.kt`.
**Scope:** Medium. **Depends on:** T9, T15.

## Phase 4: Hint and Release

### T17: Idle hint
**Description:** Idle timer driven by foreground frame deltas. After 8 s settled with no touch, pulse one valid move. Touch clears. Background pauses and resume resets.
**Acceptance:**
- [ ] Hint appears after about 8 s idle, disappears on touch.
- [ ] Home for 60 s then return: no hint immediately, timer restarts at 0.
**Verify:** on device with a stopwatch.
**Files:** `IdleHint.kt`, `BoardRenderer.kt`, `GameSurfaceView.kt`.
**Scope:** Small-Medium. **Depends on:** T16.

### T18: Release build, icon, size ratchet
**Description:** Local signing config reading keystore path and passwords from untracked `local.properties`/env, vector launcher icon, final R8 verification, ratchet the size cap to the measured value + 10%.
**Acceptance:**
- [ ] Signed release APK installs on the A52 via `adb`.
- [ ] Zero permissions. Works in airplane mode.
- [ ] `CONSTRAINTS.md` updated with measured values (APK size, coverage, frame feel).
- [ ] `check-full` green with warn-only items either fixed or turned to block.
- [ ] `gitleaks` clean. No keystore in git.
**Verify:** `make check-full`, install, airplane-mode play.
**Files:** `app/build.gradle.kts`, `app/src/main/res/...` (icon), `CONSTRAINTS.md`, `README.md`.
**Scope:** Medium. **Depends on:** T17.

### Checkpoint D: v1 complete
- [ ] All SPEC success criteria 1-12 verified. Owner signs off.

## Special gems (second feature, planned 2026-10-17)

Spec: `SPEC.md` ("Special Gems", assumptions 2, 3, 7, 10, 11, success criteria 13-18). Intent: `docs/intent/special-gems.md`. Decisions: `docs/decisions.md` D12-D15. Tasks tracked in `tasks/todo.md`, T19 onward. Commit policy as above: one-line conventional commits, selective staging, docs separate from code, nothing committed until the owner says so.

### Overview

Add four special gems (blast, line, cross, colour) that the player's own match creates and a swap fires. Core first and test-first: types, creation, activation, chains, combos, scoring, save format `GM2` with a `GM1` reader. Then the app: white gem becomes a pentagon, overlays, effect animations, idle hint fallback. Every core task ends green on `make check-task`. Every app task ends with something visible on the A52.

### Architecture decisions

- **`Board` holds `Piece`, not `Gem`** (D12). A special gem is a property of the cell, so it falls, swaps and saves with it. Costly to reverse once GM2 saves exist.
- **Specials are inert in matches** (D14, owner-confirmed). `findRuns` skips them. A swap that touches one is always legal.
- **A board that holds a special always has a valid move**, so reshuffle never runs while one is on the board. `isValidSwap` carries that rule, `ensureMoveExists` needs no change.
- **Creation is a pure function of the player's own match groups** (depth 1 only), kept apart from activation so each is testable alone.
- **Activation is a pure function** `(board, origin, special, partner) -> cells`, and `resolveSwap` orchestrates it: swap, fire, clear, fall, spawn, cascade at depth 2+. Chains and combos extend the same function, not a second path.
- **`Step` gains `Activate` and `Create`**; `GemMove` and `Placed` carry `Piece`. Every new step needs animation code, so this is the second costly-to-reverse surface after the board.
- **Save `GM2`** (D13): two characters per cell, `GM1` still decodes. Encoder emits GM2 only.
- **Scoring constants stay in `Scoring.kt`** (D15), one new constant `POINTS_PER_EFFECT_GEM = 10`.
- **App keeps the sprite model.** A `Sprite` gets a `Piece`. Overlays are vector paths drawn over the gem shape in `GemShapes`. Effect animations are new `Timeline` animations driven by `Step.Activate`. No assets, no dependency.
- **Idle hint** keeps using core move lists, with one new core function for "moves that make a match" so the app does not duplicate rules.

### Dependency graph

```
T19 Piece/Special, Board stores Piece ── T20 call sites, steps, app adapters ── T21 inert in matches + legal special swap
        │                                      │
        │                              T22 classify groups ── T23 creation in resolveSwap ── Checkpoint E (SC13)
        │                                                              │
        │                              T24 blast/line/cross fire on swap ── T25 colour gem ── T26 chains ── T27 combos ── Checkpoint F (SC14)
        │                                                                                                      │
        └── T29 GM2 codec + GM1 reader (needs only T20; can run beside T22-T27)                        T28 scoring pass
                                                                                                               │
                                                                      T30 game-level soak ── Checkpoint G (core done, SC15)
                                                                                                               │
T31 pentagon (independent, any time) ── T32 sprites carry Piece, Create playback ── T33 overlays + shimmer ── Checkpoint H
                                                                                                               │
                                                                      T34 effect animations ── T35 idle hint fallback + resume check ── Checkpoint I (SC16-18)
                                                                                                               │
                                                                                                   T36 docs close-out
```

T29 may run in parallel with T22-T27 (it needs only the `Piece` type). T31 may run any time. Everything else is sequential.

### Risks and mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Board-holds-Piece refactor (T19, T20) touches many core files and the app | High | Do it first, behaviour unchanged, all existing tests stay green and keep their assertions (floor guard forbids removing them). Split into T19 (types and `Board` storage, about 5 files) and T20 (call sites, steps and app adapters, about 8 mechanical files). |
| `Resolution.kt` becomes tangled with creation, activation, chains and combos | High | Pure helpers in `Creation.kt` and `Activation.kt`; `resolveSwap` only sequences. Add tests per layer before wiring. |
| detekt/ktlint size and complexity limits on `Timeline.kt` (343 lines) and `Resolution.kt` | Medium | New animations go in a new file. No suppressions allowed (floor). |
| Under-specified combo and chain edge cases (decided 2026-10-17, see Decided questions) | Medium | Pin each as a named test when decided, and record the answer in SPEC. |
| GM2 save over the 256-character read limit | Low | Worst case is about 170 characters. A test asserts the worst case. |
| Older app version reads a GM2 save | Medium | It decodes null and starts a new game. Android blocks downgrades on release builds, so roll forward only. Recorded in D13. |
| Effects unreadable on a 6.5" screen | Medium | Checkpoint H and I are owner-eyes on the A52 (SC17). |
| APK grows past 1 MiB | Low | Today about 67 KB signed. Measure at Checkpoint I. |

### Decided questions (owner, 2026-10-17)

1. **T25:** the colour gem is consumed when it fires. Written into `SPEC.md` rule 7.
2. **T27:** colour + special converts every gem of the special's colour; converted line gems copy the partner's direction. Written into `SPEC.md` combos.
3. **T28:** a fired special gem scores 10 like any other cleared gem, and a cell cleared by both a run and an effect scores once. Written into `SPEC.md` rule 8.
4. **T23:** the L/T rule in spec rule 5 for groups of three or more runs is taken as written.

No open questions remain for the special gems.

### Phase 5: Special gems, core (test-first)

Every core task: write the failing tests first (red), then the code (green), then tidy. Verify with `make check-task` (compile, ktlint, boundary, floor guard, gitleaks, core and app tests, 80% coverage). `check-task` does not run detekt or Lint, so Checkpoints G and H also run `make check-full`: `Resolution.kt` and `Timeline.kt` are the likely size and complexity offenders and suppressions are banned. Existing tests are migrated, never weakened or deleted.

#### T19: `Piece`, `Special`, and `Board` stores `Piece`
**Description:** Add `Special` (BLAST, LINE_ROW, LINE_COL, CROSS, COLOUR) and `Piece(gem: Gem?, special: Special?)`, where `gem` is null only for COLOUR (enforced in `init`). `Board` stores `Piece?` internally and gains `piece(pos)` and `with(pos, Piece?)`. For now `Board.get` still returns `Gem?` and `with(pos, Gem?)` stays, as temporary shims so every other file compiles unchanged. No special gem exists yet, so behaviour is identical.
**Acceptance:**
- [ ] `Piece(null, BLAST)` and `Piece(RED, COLOUR)` are rejected; `Piece(null, COLOUR)` and every plain gem are accepted.
- [ ] A board round-trips `piece` and `with`, and equality and swap work on pieces.
- [ ] Every pre-existing core and app test passes with its assertions intact.
**Verify:** `make check-task`
**Files:** `Piece.kt`, `Special.kt`, `Board.kt`, `TestBoards.kt`, `PieceTest.kt`, `BoardTest.kt`.
**Scope:** Small-Medium. **Depends on:** none.

#### T20: Call sites, step payloads and app adapters use `Piece`
**Description:** Remove the shims: `Board.get` returns `Piece?`. `GemMove` and `Placed` carry `Piece`. Matches, moves, reshuffle, save codec, gravity and refill move a piece as a unit. The app (`Scene`, `Sprite`) adapts minimally; no special exists yet, so behaviour is identical. Mechanical, but about 8 files.
**Acceptance:**
- [ ] `Board.get`, `with(pos, Gem?)` and the other shims are gone.
- [ ] Gravity, swap and refill move a hand-built special as a unit (test).
- [ ] Every pre-existing core and app test passes with its assertions intact.
**Verify:** `make check-task`
**Files:** `Board.kt`, `Step.kt`, `Matches.kt`, `Move.kt`, `Resolution.kt`, `Reshuffle.kt`, `SaveCodec.kt`, `Scene.kt` (+ tests).
**Scope:** Medium-Large (watch it; stop and re-split if it passes 10 files). **Depends on:** T19.

#### T21: Specials are inert in matches, and a special swap is always legal
**Description:** `findRuns` ignores special pieces. `isValidSwap` accepts any adjacent, occupied swap that forms a run or touches a special. Add `findMatchMoves` (swaps that form a run) for the hint; `findValidMoves` and `hasValidMove` include special swaps.
**Acceptance:**
- [ ] Three same-colour pieces in a row, one special, form no run.
- [ ] A swap touching a special is valid with no match; a swap of two plain gems still needs a match.
- [ ] A board holding a special has a valid move even when no plain match exists.
- [ ] `findMatchMoves` never returns a swap that only works through a special.
**Verify:** `make check-task`
**Files:** `Matches.kt`, `Move.kt`, `MatchesTest.kt`, `MoveTest.kt`.
**Scope:** Small. **Depends on:** T20.

#### T22: Classify match groups
**Description:** Pure function from a `MatchGroup` to the special it makes: run of 4 blast, 5 or more colour, L line (direction from the longer arm, equal arms row), T or + cross. Priority colour, cross, line, blast. L against T per spec rule 5, including groups of three or more runs.
**Acceptance:**
- [ ] Table-driven tests for 4, 5, 6, L (both arm orders, equal arms; arm length is the run length including the corner), T, +, and a 3-run group.
- [ ] A plain run of 3 makes nothing.
- [ ] A group that qualifies for several kinds returns the highest priority.
**Verify:** `make check-task`
**Files:** `Creation.kt`, `CreationTest.kt`.
**Scope:** Small. **Depends on:** T21.

#### T23: Creation inside `resolveSwap`
**Description:** After the player's own match (depth 1) clears, place at most one special per group at the swapped cell that belongs to the group (the dragged one if both do). Add `Step.Create(pos, piece)` after the `Clear`. Later cascade matches create nothing. Spawned gems are always plain.
**Acceptance:**
- [ ] A swap forming a run of 4, a run of 5, an L and a T each leave exactly one special of the right kind at the right cell (SC13).
- [ ] A swap forming two groups can leave two specials.
- [ ] A match at cascade depth 2 or more makes no special.
- [ ] `TestReplay` rebuilds the final board from the steps, including `Create`.
- [ ] Gem count stays 64 on every resolved board.
- [ ] Step order is Clear, Create, Fall, Spawn: a special made by a vertical run of 4 falls like any gem once the cells below it clear.
**Verify:** `make check-task`
**Files:** `Resolution.kt`, `Step.kt`, `TestReplay.kt`, `CascadeTest.kt`.
**Scope:** Medium. **Depends on:** T22.

#### Checkpoint E: Creation
- [ ] `make check-task` green. SC13 covered by tests.
- [ ] Owner review of `Piece` and `Step` shape before activation builds on them.

#### T24: Blast, line and cross fire on swap
**Description:** Pure `effectCells` for blast (3x3), line (row or column), cross (both), clipped to the board. `resolveSwap` swaps first, fires the special from the cell it moved to, emits `Step.Activate(origin, special, cells)`, then one `Clear` that also holds any normal match the swap formed. Cascades continue at depth 2 and up. A fired swap creates no special. Effect gems score `POINTS_PER_EFFECT_GEM` times depth.
**Acceptance:**
- [ ] Each effect clears exactly its cells, including at edges and corners.
- [ ] A swap with a special and no match is accepted; the same swap is rejected for two plain gems.
- [ ] A normal match formed by the same swap clears in the same `Clear` step.
- [ ] Cascade after the effect runs at depth 2.
**Verify:** `make check-task`
**Files:** `Activation.kt`, `Resolution.kt`, `Step.kt`, `Scoring.kt`, `ActivationTest.kt`.
**Scope:** Medium. **Depends on:** T23.

#### T25: Colour gem
**Description:** A swapped colour gem clears every piece of the partner's colour (plus itself). A colour gem swapped with a plain gem matches nothing else.
**Acceptance:**
- [ ] Clears every gem of the partner's colour and the colour gem itself.
- [ ] Partner colour count of zero or one still works.
- [ ] The colour gem forms no run, even beside gems of any colour.
**Verify:** `make check-task`
**Files:** `Activation.kt`, `ActivationTest.kt`.
**Scope:** Small. **Depends on:** T24.

#### T26: Chain reactions
**Description:** If an effect covers another special, that piece fires too, from where it sits. Each piece fires once per move. Only the player's own swap starts a chain; nothing at depth 2 or more fires a special. One `Activate` step per fired piece, in a deterministic order.
**Acceptance:**
- [ ] A blast covering a line gem fires the line gem; a loop between two specials fires each once.
- [ ] A special beside a cascade match is untouched and falls intact (specials are inert, so a cascade never fires or clears one).
- [ ] All `Activate` steps of a move come before its single `Clear`, in firing order.
- [ ] Order of `Activate` steps is stable for the same input.
**Verify:** `make check-task`
**Files:** `Activation.kt`, `Resolution.kt`, `ActivationTest.kt`.
**Scope:** Medium. **Depends on:** T25.

#### T27: Combos
**Description:** Colour + colour clears the board. Colour + another special converts every piece of that special's colour into that special, then fires them all (chains apply). Any other pair fires both effects at once.
**Acceptance:**
- [ ] Colour + colour clears all 64 cells.
- [ ] Colour + each of blast, line, cross converts the right pieces and fires them. The colour gem's `Activate` lists the converted cells, then one `Activate` follows per converted piece. No separate conversion step.
- [ ] Each remaining pair fires both effects, from their own cells.
**Verify:** `make check-task`
**Files:** `Activation.kt`, `Combo.kt`, `ComboTest.kt`.
**Scope:** Medium. **Depends on:** T26.

#### Checkpoint F: Activation
- [ ] `make check-task` green. SC14 covered by tests.

#### T28: Scoring pass
**Description:** T24 introduces `POINTS_PER_EFFECT_GEM`; this task pins the rules across creation, chains and combos with literal expected scores and adds the property check. Constants live in `Scoring.kt`. Every effect-cleared gem scores `POINTS_PER_EFFECT_GEM` once, times depth; creating a special adds nothing; the total never wraps (existing saturating add).
**Acceptance:**
- [ ] Literal expected scores for a blast, a colour + colour (640 at depth 1), and a chain, so a changed constant fails a test.
- [ ] A cell cleared by both a run and an effect scores once.
- [ ] Sum of `Clear.points` equals the score added to the game, across many seeds.
**Verify:** `make check-task`
**Files:** `Scoring.kt`, `ScoringTest.kt`, `CascadeTest.kt`.
**Scope:** Small. **Depends on:** T27.

#### T29: Save format `GM2` with a `GM1` reader
**Description:** `GM2;<score>;<rng>;<64 two-character cells>`: gem digit or `-`, then `-`, `B`, `R`, `C`, `X` or `W`. The encoder emits GM2 only; the decoder reads GM1 and GM2. Anything malformed returns null.
**Acceptance:**
- [ ] A game with every special on the board round-trips equal and plays the same future (SC15).
- [ ] A GM1 string from the current release loads, and re-encodes as GM2.
- [ ] Truncated, wrong length, unknown special letter, `-` gem on a non-colour, colour gem with a digit, and garbage all return null.
- [ ] Worst-case save stays under the 256-character read limit.
**Verify:** `make check-task`
**Files:** `SaveCodec.kt`, `SaveCodecTest.kt`.
**Scope:** Small. **Depends on:** T20 (parallel with T22-T28).

#### T30: Game-level soak
**Description:** Property-style checks over many seeds with specials in play: random legal moves, including special swaps, on seeded games.
**Acceptance:**
- [ ] After every move: 64 pieces, no runs, at least one valid move, encode/decode equals the game.
- [ ] A board holding a special never emits `Reshuffle`.
- [ ] Same seed and moves give identical steps and score.
**Verify:** `make check-task`
**Files:** `GameTest.kt`, `SoakTest.kt`.
**Scope:** Small. **Depends on:** T28, T29.

#### Checkpoint G: Core complete
- [ ] `make check-full` green. Core coverage at least 80% (enforced); the measured 100% must not fall by more than 0.5% (`CONSTRAINTS.md`).
- [ ] SC13, 14, 15 covered. Owner review of the final core API before the UI.

### Phase 6: Special gems, app

App tasks end with `make check-task` and an on-device check on the A52 (build and install with the project's usual flow). `app` unit tests cover the Android-free classes (`Timeline`, `Scene`, `IdleHint`).

#### T31: White gem becomes a pentagon
**Description:** Replace the white plus in `GemShapes` with a regular pentagon, freeing the plus for the cross overlay.
**Acceptance:**
- [ ] The seven gem silhouettes are distinct in grayscale.
- [ ] No other visual or timing change.
**Verify:** `make check-task`; install on the A52 and look at a board.
**Files:** `GemShapes.kt`.
**Scope:** XS. **Depends on:** none.

#### T32: Sprites carry `Piece`; a created special appears and stays
**Description:** `Sprite` and `Scene` hold a `Piece`. `Timeline` plays `Step.Create` as a pop-in, and `Fall` and `Spawn` move pieces as units. A special is drawn as its plain gem for now.
**Acceptance:**
- [ ] After a 4-run the special appears at the right cell and the rendered board equals the core board when the timeline ends.
- [ ] The special survives cascades, pause and resume unchanged (SC16, with GM2 save from T29).
**Verify:** `make check-task`; play on the A52 until a special appears, background and reopen.
**Files:** `Scene.kt`, `Timeline.kt` (or a new `CreateAnimation.kt`), `GameSession.kt`, `SceneTest.kt`, `TimelineTest.kt`.
**Scope:** Medium. **Depends on:** T23, T29.

#### T33: Overlays and shimmer
**Description:** Draw a white vector overlay with a dark outline on special pieces: blast 8-point burst, line stripe (horizontal for row, vertical for column), cross plus-shaped stripes, colour a wheel of the seven gem colours with a white edge. Add a slow shimmer.
**Acceptance:**
- [ ] The four specials are told apart by overlay alone, and by their base gem colour where they have one.
- [ ] Shimmer is visible but does not hide the overlay.
- [ ] Frame feel on the A52 unchanged.
**Verify:** `make check-task`; look at all four on the A52.
**Files:** `GemShapes.kt`, `Palette.kt`, `BoardRenderer.kt`.
**Scope:** Medium. **Depends on:** T32, T31.

#### Checkpoint H: Specials visible
- [ ] `make check-full` green (detekt and Lint on the new drawing code).
- [ ] Owner sees all four specials on the A52 and can tell them apart.

#### T34: Effect animations
**Description:** `Timeline` plays `Step.Activate` before its `Clear`: ring flash over the 3x3 for blast, beam along the row or column for line (both for cross), quick streaks from the colour gem to each taken gem. Chains play in step order. Input blocked while playing.
**Acceptance:**
- [ ] You can tell what fired, from where, and which gems it took (SC17).
- [ ] Rendered board equals the core board when the timeline ends, after a chain and after colour + colour.
- [ ] Touches during effects are ignored.
**Verify:** `make check-task`; fire each effect, a chain and a combo on the A52.
**Files:** `Effects.kt` (new), `Timeline.kt`, `BoardRenderer.kt`, `TimelineTest.kt`.
**Scope:** Medium. **Depends on:** T33, T27.

#### T35: Idle hint fallback and resume check
**Description:** `IdleHint` picks a move from `findMatchMoves`; only when there is none does it point at a special swap from `findValidMoves`. Confirm a GM1 save from the installed release resumes after the update.
**Acceptance:**
- [ ] With a plain match available, the hint is a match move even if a special exists.
- [ ] With only special swaps left, the hint points at one.
- [ ] Installing over the current release resumes the GM1 board and score.
**Verify:** `make check-task`; install over the old build on the A52.
**Files:** `IdleHint.kt`, `IdleHintTest.kt`.
**Scope:** Small. **Depends on:** T21, T34.

#### Checkpoint I: Special gems complete
- [ ] `make check-full` green (APK size, permissions, detekt, Lint, osv-scanner).
- [ ] Owner plays about ten minutes on the A52: SC16, 17, 18 verified.
- [ ] Tune `POINTS_PER_EFFECT_GEM` and other constants by feel, via the constants only.

#### T36: Docs close-out
**Description:** Update `SPEC.md` status line, `CONSTRAINTS.md` measured table (coverage, size), `README.md` features, and `docs/decisions.md` with anything decided during build. Docs commit, separate from code, owner approval first.
**Acceptance:**
- [ ] Spec status says approved; measured values match the last `make check-full`.
- [ ] Every decision made during build has a why, impact and rollback.
**Verify:** `make check-fast`
**Files:** `SPEC.md`, `CONSTRAINTS.md`, `README.md`, `docs/decisions.md`.
**Scope:** Small. **Depends on:** Checkpoint I.

## Post-v1 candidates (not planned)

Sound, "best single move" record, Compose or ready-made-lib UI as a second perspective, KMP port. Each is a separate spec.

## Decisions (owner-approved)

1. Swipe geometry lives in `core` (T10). Testable and portable. The owner notes it is arguable whether it belongs in core or in an input layer.
2. New Game confirm is a hand-drawn Canvas dialog, not `AlertDialog`, for style consistency and control over layering (T15).
3. Commit policy: one conventional one-line commit per implementation task, no per-commit approval needed. Anything about decisions, planning or specs (SPEC, plan, todo, CONSTRAINTS, intent docs) still needs the owner's approval first.

## Open Questions

None.
