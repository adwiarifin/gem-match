# Task List: Gem Match

Details, acceptance criteria and verification: `tasks/plan.md`. Spec: `SPEC.md`.

## Phase 0: Foundation
- [x] T1: Root Gradle project and `core` module (Kover, ktlint, detekt, smoke test)
- [x] T2: `app` module, empty SurfaceView, installed on the A52, first APK size recorded
- [x] T3: Constraint scripts (core boundary, APK size) and check aliases
- [ ] Checkpoint A: foundation green, size baseline recorded, owner review

## Phase 1: Core (test-first)
- [x] T4: Board, positions, gems, RNG
- [x] T5: Match detection
- [x] T6: Valid moves
- [x] T7: Cascade resolution and tiered scoring
- [x] T8: `Game`: new game, trySwap, reshuffle
- [x] T9: Save codec
- [x] T10: Swipe geometry
- [ ] Checkpoint B: core complete, coverage ≥ 80%, owner review of API

## Phase 2: App
- [x] T11: Draw the board (game loop, surface lifecycle)
- [x] T12: Distinct gem shapes
- [x] T13: Input, swap and invalid revert
- [x] T14: Play back clear, fall, spawn (cascades)
- [x] T15: HUD, New Game confirm, reshuffle animation
- [ ] Checkpoint C: playable on the A52, owner plays a session, tune numbers

## Phase 3: Persistence
- [ ] T16: Autosave, resume, high score

## Phase 4: Hint and Release
- [ ] T17: Idle hint (8 s, foreground time only)
- [ ] T18: Signed release build, icon, size ratchet, airplane-mode check
- [ ] Checkpoint D: all SPEC success criteria verified, owner sign-off
