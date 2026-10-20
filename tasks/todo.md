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

## Phase 5: Special gems, core (test-first)

Details: `tasks/plan.md` ("Special gems"). Spec: `SPEC.md` "Special Gems". Every task: tests first, then verify with `make check-task`.

- [x] T19: `Piece`, `Special`, `Board` stores `Piece` (shims keep other files compiling). Done when: invalid `Piece` rejected, board round-trips pieces, all old tests green with assertions intact. Verify: `make check-task`
- [x] T20: Call sites, step payloads and app adapters use `Piece`; shims removed. Done when: `Board.get` returns `Piece?`, a hand-built special moves as a unit through gravity, swap and refill, all old tests green. Verify: `make check-task`
- [x] T21: Specials inert in matches; a special swap is always legal; `findMatchMoves`. Done when: no run through a special, special swap valid without a match, board with a special always has a move. Verify: `make check-task`
- [x] T22: Classify groups (4 blast, 5+ colour, L line with direction, T/+ cross, priority). Done when: table tests cover every shape and priority. Verify: `make check-task`
- [x] T23: Creation in `resolveSwap` (depth 1 only, `Step.Create`). Done when: SC13 holds in tests, cascade matches make none, gem count stays 64, step order is Clear, Create, Fall, Spawn. Verify: `make check-task`
- [x] Checkpoint E: creation green, owner reviews `Piece` and `Step` shape
- [x] T24: Blast, line, cross fire on swap (`Step.Activate`, 10 per effect gem). Done when: exact cells incl. edges, same-`Clear` normal match, cascade at depth 2. Verify: `make check-task`
- [x] T25: Colour gem fires on its partner's colour. Done when: all of that colour and itself clear, forms no run. Verify: `make check-task`
- [x] T26: Chain reactions (each piece once, player's swap only). Done when: chain and loop tests pass, a special beside a cascade match is untouched, all `Activate` steps come before the single `Clear`. Verify: `make check-task`
- [x] T27: Combos (colour+colour clears board, colour+special converts and fires, other pairs fire both). Done when: SC14 holds in tests, colour gem's `Activate` lists converted cells then one `Activate` per converted piece. Verify: `make check-task`
- [x] Checkpoint F: activation green (the open questions were decided on 2026-10-17)
- [x] T28: Scoring pass (literal expected scores, no double count, sum of `Clear.points` equals score). Verify: `make check-task`
- [x] T29: Save `GM2` with a `GM1` reader. Done when: SC15 holds, malformed input returns null, worst case under 256 characters. Verify: `make check-task`
- [x] T30: Game-level soak over many seeds with specials. Done when: invariants hold after every move, no reshuffle with a special on the board. Verify: `make check-task`
- [x] Checkpoint G: `make check-full` green, coverage at least 80% (measured 100% must not fall more than 0.5%), owner reviews the core API

## Phase 6: Special gems, app

Each task ends with `make check-task` and a look on the A52.

- [x] T31: White gem becomes a pentagon. Done when: seven silhouettes distinct in grayscale. Verify: `make check-task`, install and look
- [x] T32: Sprites carry `Piece`; created special appears and survives cascades and resume. Done when: rendered board equals core board, SC16 holds. Verify: `make check-task`, play on the A52
- [x] T33: Overlays (burst, stripe, cross, colour wheel) and shimmer. Done when: four specials told apart on the A52. Verify: `make check-task`, look on the A52
- [x] Checkpoint H: `make check-full` green, owner sees all four specials on the A52
- [x] T34: Effect animations (ring, beam, streaks; chains in order). Done when: SC17 holds, board matches core at the end, input blocked. Verify: `make check-task`, fire each effect on the A52
- [x] T35: Idle hint prefers a match move, falls back to a special swap; GM1 save resumes after update. Done when: hint tests pass, install-over resumes. Verify: `make check-task`, install over the old build
- [x] Checkpoint I: `make check-full` green, ten-minute owner session (SC16 to 18), tune constants
- [x] T36: Docs close-out (SPEC status, README, decisions done; CONSTRAINTS measured values left for the owner, see the note below). Owner approval, separate docs commit. Verify: `make check-fast`

Note on T36: the `CONSTRAINTS.md` "Measured, not yet enforced" rows are out of date (release APK 63,634 bytes unsigned, debug APK 1,114,297 bytes, `core` coverage 99.77%, measured 2026-10-17). `scripts/floor-guard.py` treats raising a "must not grow" number or lowering a "must not fall" number as loosening the bar, so the update needs the owner. All three are still inside their caps and tolerance.

## Phase 7: Game over and run stats, core

Details: `tasks/plan.md` ("Game over and run stats"). Spec: `SPEC.md` "Game Over and Run Stats". Every `core` task: tests first, then `make check-task`.

- [x] T37: Measure run length (random and greedy play, 1,000 seeds each, until the first dead board). Done when: median, p10, p90 and the cap rate are in `docs/decisions.md` as D22, and the helper stays as the base of the soak test. Verify: `make check-task`
- [x] Checkpoint J: owner read D22 and chose a harsher spawn (2026-10-19)
- [x] T37b: Harsher spawn (anti-clustering refill, `SPAWN_RETRIES` 1, chosen by the owner). Done when: refill is deterministic and always full, soak invariants hold, and the T37 harness shows greedy median under 1,500 moves with under 5% capped (D23). Verify: `make check-task`, full measurement
- [x] T38: `Game.isOver`; delete the reshuffle (core and app). Done when: a plain dead board is over, a board with a special is not, `trySwap` is `Rejected` when over, no `Reshuffle` reference remains. Commit message states why reshuffle tests go; test-count floor adjusted. Verify: `make check-task`
- [x] T39: `RunStats` from the steps (moves, cascade, moveScore, gems, match, created and fired by kind). Done when: hand-computed values match on crafted moves and `score` equals the sum of `Clear.points` across seeds. Verify: `make check-task`
- [x] T40: Key-value codec and `GM3`. Done when: round trip is equal, `GM2` and `GM1` load with zero stats, bad stats part returns null, worst case under 1024 characters. Verify: `make check-task`
- [x] T41: `FinishedRun`, `Lifetime`, `RN1` and `LT1` lines, keep newest 100. Done when: new best rules hold, lifetime keeps dropped runs, sums saturate, unknown key ignored. Verify: `make check-task`
- [x] Checkpoint K: `make check-full` green, `core` coverage at least 80% (measured value must not fall more than 0.5%), owner reviews the core API

## Phase 8: Game over and run stats, app

Each task ends with `make check-task`; drawing and wiring are checked on the A52.

- [ ] T42: `RunClock` (wall and foreground-only active time) and `GM3` in the session. Done when: pause adds no active time, a reload keeps `started` and `active`, New Game resets. Verify: `make check-task`
- [ ] T43: `runs.txt`, `lifetime.txt`, record once, `best.txt` seeds the best score. Done when: a run is never counted twice across a kill, the 101st run drops the oldest, damaged lines cost only themselves, abandoned games record nothing. Verify: `make check-task`
- [ ] T44: Game-over card (600 ms beat, fade-in, every stat, scroll, Play Again). Done when: it appears after the animation, blocks the board, scrolls and keeps buttons pinned. Verify: `make check-task`, the T46 runbook on the A52
- [ ] T45: Stats screen (lifetime block, last 100 runs, HUD and card buttons, Back). Done when: 101 runs show 100 rows newest first, empty history reads plainly. Verify: `make check-task`, look on the A52
- [ ] T46: Near-dead fixture and device runbook (`docs/game-over-runbook.md`). Done when: the fixture test pins the board and the runbook reaches the card on the A52. Verify: `make check-task`, run the runbook once
- [ ] Checkpoint L: `make check-full` green, SC9 to SC12 on the A52, frame time recorded, owner plays a session
- [ ] T47: Docs close-out (README, D22 and D23, SPEC status, CONSTRAINTS measured rows by the owner, open items). Owner approval, separate docs commit. Verify: `make check-fast`

## Open items after the review and release work (2026-10-16)

Left open on purpose. None blocks the first release. Items marked "owner" need an action only the owner can do.

Release steps:
- [x] Merge release PR #1, approve the `sign` job, check the published APK's certificate against `RELEASE_CERT_SHA256` (owner). Done: #1 merged 2026-10-16 and v0.1.0 to v0.1.2 are published; the v0.1.2 APK verifies, its SHA-256 file matches, signer `CN=Adwi Arifin, O=Kesatria Dev`, certificate SHA-256 `b8b8af6e4ba9e5faf7d34cecffdd9bc3c6ed4bf9e125ed42555131a2d7bb5a81` (compare it with your `RELEASE_CERT_SHA256`).
- [x] Remove `release-as` from `release-please-config.json` right after the first release. Done: it is no longer in the file.
- [x] Install the CI-signed APK over the old build on the A52 and check the game resumes the saved board (owner plugs in the phone). Done 2026-10-19: v0.2.0 (SHA-256 matches the published file, pinned cert) installed with `adb install -r` over a locally signed build holding a game in progress; score and board resumed. Before that, a v0.1.2 GM1 save also resumed under a locally signed build of the same code.
- [x] Add a tag ruleset on `v*` (restrict updates and deletions) in the repository settings (owner)
- [x] Back up the keystore and both passwords outside this machine (owner; backed up to cloud storage, location not recorded)

Testing and robustness:
- [ ] `GameController`, the renderer and the lifecycle wiring have no JVM tests. Either split the controller into a testable core and a thin Android shell, or keep it frozen and record a manual check per release (Partly, 2026-10-17: the tap-skip decision is now a pure, tested function and `GameSession` is tested with specials; the renderer, overlays and controller wiring are still checked by hand, with no written per-release checklist yet.)
- [ ] A transient read failure on the save can lead to a new game that overwrites a good save. Rename an unreadable save to `.bad` instead of ignoring it (needs a design call)
- [ ] `GameStore` catches only `IOException` (a deliberate choice so unexpected errors stay visible; revisit if a crash is ever seen)
- [ ] Per-frame allocations in the render path. Measure before changing anything (Partly, 2026-10-17: the effect and overlay drawing allocates nothing per frame and A52 frame time was measured, see `CONSTRAINTS.md`; the base renderer was not profiled.)
- [ ] The floor guard's regex checks have known blind spots; `check-fast` and `check-task` do not run detekt or Lint (they run in `check-full` and CI, as `CONSTRAINTS.md` says) (Narrowed, 2026-10-17: the guard now reads multi-line calls, rejects neutered and commented-out assertions, moved tests and `-diff` attributes, and a test-count floor catches tests that stop running. A wrapper or lambda it does not know can still change meaning, and `check-fast` and `check-task` still skip detekt and Lint by design.)

Build and supply chain:
- [x] Gradle dependency verification: `gradle/verification-metadata.xml` is committed and enforced (PR #2). Regenerate it on a clean `GRADLE_USER_HOME` after an upgrade, or CI on a cold cache fails
- [ ] Pin the build-tools version the signing job uses (Still open, 2026-10-17: `scripts/sign-ci.sh` picks the newest installed build-tools.)

Product (see also `docs/ideas.md`):
- [ ] No screen-reader (TalkBack) support
- [ ] The 60 Hz game loop on the A52's 90 Hz panel presents unevenly (ideas F1 and T6: drive the loop from vsync)
- [ ] Black strip at the camera cutout (idea F2)
