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

## Post-v1 candidates (not planned)

Sound, "best single move" record, special gems, Compose or ready-made-lib UI as a second perspective, KMP port. Each is a separate spec.

## Decisions (owner-approved)

1. Swipe geometry lives in `core` (T10). Testable and portable. The owner notes it is arguable whether it belongs in core or in an input layer.
2. New Game confirm is a hand-drawn Canvas dialog, not `AlertDialog`, for style consistency and control over layering (T15).
3. Commit policy: one conventional one-line commit per implementation task, no per-commit approval needed. Anything about decisions, planning or specs (SPEC, plan, todo, CONSTRAINTS, intent docs) still needs the owner's approval first.

## Open Questions

None.
