# Task List: Gem Match

Details, acceptance criteria and verification: `tasks/plan.md`. Spec: `SPEC.md`.

## Phase 0: Foundation
- [x] T1: Root Gradle project and `core` module (Kover, ktlint, detekt, smoke test)
- [x] T2: `app` module, empty SurfaceView, installed on the A52, first APK size recorded
- [x] T3: Constraint scripts (core boundary, APK size) and check aliases
- [x] Checkpoint A: foundation green, size baseline recorded, owner review

## Phase 1: Core (test-first)
- [x] T4: Board, positions, gems, RNG
- [x] T5: Match detection
- [x] T6: Valid moves
- [x] T7: Cascade resolution and tiered scoring
- [x] T8: `Game`: new game, trySwap, reshuffle
- [x] T9: Save codec
- [x] T10: Swipe geometry
- [x] Checkpoint B: core complete, coverage ≥ 80%, owner review of API

## Phase 2: App
- [x] T11: Draw the board (game loop, surface lifecycle)
- [x] T12: Distinct gem shapes
- [x] T13: Input, swap and invalid revert
- [x] T14: Play back clear, fall, spawn (cascades)
- [x] T15: HUD, New Game confirm, reshuffle animation
- [x] Checkpoint C: playable on the A52, owner plays a session, tune numbers

## Phase 3: Persistence
- [x] T16: Autosave, resume, high score

## Phase 4: Hint and Release
- [x] T17: Idle hint (8 s, foreground time only)
- [x] T18: Signed release build, icon, size ratchet, airplane-mode check
- [x] Checkpoint D: all SPEC success criteria verified, owner sign-off

## Open items after the review and release work (2026-10-16)

Left open on purpose. None blocks the first release. Items marked "owner" need an action only the owner can do.

Release steps:
- [ ] Merge release PR #1, approve the `sign` job, check the published APK's certificate against `RELEASE_CERT_SHA256` (owner)
- [ ] Remove `release-as` from `release-please-config.json` right after the first release
- [ ] Install the CI-signed APK over the old build on the A52 and check the game resumes the saved board (owner plugs in the phone)
- [ ] Add a tag ruleset on `v*` (restrict updates and deletions) in the repository settings (owner)
- [ ] Back up the keystore and both passwords outside this machine (owner)

Testing and robustness:
- [ ] `GameController`, the renderer and the lifecycle wiring have no JVM tests. Either split the controller into a testable core and a thin Android shell, or keep it frozen and record a manual check per release
- [ ] A transient read failure on the save can lead to a new game that overwrites a good save. Rename an unreadable save to `.bad` instead of ignoring it (needs a design call)
- [ ] `GameStore` catches only `IOException` (a deliberate choice so unexpected errors stay visible; revisit if a crash is ever seen)
- [ ] Per-frame allocations in the render path. Measure before changing anything
- [ ] The floor guard's regex checks have known blind spots; `check-fast` and `check-task` do not run detekt or Lint (they run in `check-full` and CI, as `CONSTRAINTS.md` says)

Build and supply chain:
- [ ] Gradle dependency verification: commit `gradle/verification-metadata.xml` so a replaced dependency fails the build. Do it before the second release
- [ ] Pin the build-tools version the signing job uses

Product (see also `docs/ideas.md`):
- [ ] No screen-reader (TalkBack) support
- [ ] The 60 Hz game loop on the A52's 90 Hz panel presents unevenly (ideas F1 and T6: drive the loop from vsync)
- [ ] Black strip at the camera cutout (idea F2)
