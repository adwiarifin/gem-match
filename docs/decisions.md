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

## Decisions made after the review and release work (2026-10-16)

**D9. Starting a new game mid-move credits the score of the abandoned move to Best.**
- Why: the player has already seen the points of a move that is still animating, so dropping them from Best would look like a lost score. The on-screen score and the saved state both already include them.
- Impact: `GameSession.newGame` compares Best against the pending game (the resolved state of the move being animated), not the last settled one. Covered by `GameSessionTest`.
- Rollback: change that one line back to the settled game. No save format impact.

**D10. The game loop stops when the surface is destroyed, not in `onPause`.**
- Why: the surface is what the loop draws to, so tying the loop to it avoids drawing to a dead surface. Android destroys the surface whenever the app goes to the background, so time spent backgrounded still does not count toward the idle hint.
- Impact: `SPEC.md` now says this. `MainActivity.onPause` still flushes the save synchronously, and the flush reads the newest game state on the writer thread, so a move that finishes while pausing is saved (`PauseRaceTest`).
- Rollback: start and stop the loop from the activity lifecycle instead, and move the idle reset with it.

**D11. Releases are versioned by release-please and built and signed in separate CI jobs.**
- Why: the owner wants one place to get a signed APK to share, and no Play Store account yet. Conventional commits already drive the changelog and the version, so nothing is bumped by hand. `versionCode` is derived from the version (1.4.2 becomes 10402), so it can only grow. The signing key cannot be rotated for installed copies, so the key must never share a job with build code: `build` has no secrets and a read-only token, `sign` runs only `scripts/sign-ci.sh` after the owner approves, and `publish` has the write token and no secrets. The signer checks the APK's hash, the release commit, the version and the certificate before it keeps anything.
- Impact: minor and patch numbers must stay below 100 (the build fails otherwise). Going to 1.0.0 needs a `Release-As: 1.0.0` footer. The keystore and its password now also live in the `release` GitHub Environment, so losing the GitHub account's protection means losing the key.
- Rollback: Android refuses downgrades on release builds, so roll forward: revert the bad commit, let release-please cut the next version, and install over the old app (the same key keeps the save). Local signing with `make sign` stays as a fallback.

## Decisions made while planning special gems (2026-10-17)

The owner approved the special gems spec and confirmed that special gems are inert in matches (reversed after playtest, see D16). The other three entries are proposed with the plan (`tasks/plan.md`) and wait for the owner's review. The first two are the expensive ones: once real saves with special gems exist on the phone, they are hard to undo.

**D12. `Board` holds `Piece`, not `Gem`.**
- Why: a special gem is a property of the cell (its colour, its kind, and for a line gem its direction). It has to fall, swap, reshuffle and save together with the gem. Putting it in the cell keeps `Fall`, `Spawn`, swap and the save codec uniform. Considered and rejected: a separate map from position to special on `Game` (it goes out of step with every fall), and adding special variants to the `Gem` enum (7 colours times 5 kinds, and the colourless colour gem fits badly).
- Impact: `Board.get` returns `Piece?`. `GemMove` and `Placed` carry `Piece`, so every consumer of those steps changes. Roughly 10 core files plus tests, and in `app` `Scene`, `Sprite`, `Timeline` and `GemShapes`. `Piece.gem` is null only for the colour gem, enforced in the constructor.
- Rollback: cheap before T19 lands (nothing built on it). After T19 every later task depends on it. After GM2 saves with special gems exist on a phone, going back means those saves cannot be represented: they would have to be dropped or converted to plain gems, and the player's board changes.

**D13. Save format `GM2`: two characters per cell, `GM1` still readable.**
- Why: the save must record which cells are special and of what kind. A fixed two-character cell (gem digit or `-`, then `-`, `B`, `R`, `C`, `X`, `W`) keeps the hand-written, dependency-free codec and a fixed length of 64 cells. Worst case is about 170 characters, under the 256 limit. Considered: a sparse list of specials after the existing digit string (shorter, keeps the GM1 prefix, but variable-length parsing and an easier place for bugs).
- Impact: `Game` encodes only GM2 and decodes both. A game saved by this version cannot be read by an older build: it decodes to null and starts a new game. Android refuses downgrades on release builds, so in practice the path is forward only. The GM1 reader must stay as long as any GM1 save could still exist; the GM2 reader must stay forever once shipped. `SPEC.md` already says a format change after the first release needs a version byte and a migration; this is that migration.
- Rollback: encode GM1 again, dropping specials (lossy: the specials vanish from a resumed game). Keep both readers. No other data is affected, because the score and RNG state keep the same meaning.

**D14. Special gems are inert in matches (owner-confirmed). Superseded by D16 on 2026-10-17.**
- Why: if a special could be matched away, a cascade could use it up by accident, and the "use it now or save it" choice would be taken out of the player's hands. Inert pieces also keep creation and activation rules simple: a special leaves the board only through the player's own swap or another special's effect.
- Impact: `findRuns` skips special pieces, so three same-colour pieces in a row that include a special do not clear. The overlay (T33) has to make that readable. A swap touching a special is always legal, so a board holding a special always has a valid move and never reshuffles. The hint prefers a normal match and falls back to a special swap. Specials can pile up on the board if the player keeps saving them.
- Rollback: one function (`findRuns`) changes, but creation, activation and the hint rules in `SPEC.md` (rules 1, 3, 10) change with it, and so do their tests. No save format impact.

**D15. Scoring constants for special gems.**
- Why: effects should feel good without outscoring planned matches by luck. Every gem cleared by an effect scores `POINTS_PER_EFFECT_GEM = 10`, times cascade depth, the same per-gem rate as a run of 3 (30 for 3 gems). Making a special adds nothing, so saving one costs no points. Colour + colour clears the board and scores 640 at depth 1. All four numbers are starting guesses to tune by playing, as the spec says.
- Impact: score balance only. The constant lives in `Scoring.kt` with the other tier constants. Tests pin literal scores so a changed constant fails loudly and gets reviewed. A high score earned under other constants is not strictly comparable, as with D2.
- Rollback: change the constant. No save format impact, because scores are stored as plain numbers.

## Decisions made after the owner played the special gems (2026-10-17)

**D16. Specials join matches, fire when matched, and matches make specials at any depth. Supersedes D14.**
- Why: in play, the owner could not match a line gem of the same colour in a row of three, and a match of four formed in a cascade made nothing. Both felt wrong. Inert specials and player-only creation (SPEC rules 1 and 3, originally "confirmed by the owner") kept the "use it now or save it" choice, but cost more fun than they protected.
- Impact: a special counts in a run of its colour (the colour gem still never does). A special that is part of a match fires from where it sits, at any depth, and chains. A cascade can now use up a special the player was saving, and a cascade can fire chains on its own. Matches at any depth make specials; a clear in which something fires makes none. `findRuns` no longer skips specials, `resolveSwap` fires and creates in every iteration, and the old tests that asserted inertness now assert the new behaviour. A board holding a special still always has a valid move (a special swap is always legal), so it still never reshuffles. Specials appear much more often, so the reshuffle is now rare in play.
- Rollback: restore `takeUnless { it.isSpecial }` in `findRuns`, fire specials and create only when `depth == 1`, and put the inertness tests back from git history. No save format impact.

**D17. Shape table: short L and short T, L is the cross gem, long T is a full X, and the short X. Partly superseded by D18.**
- Why: the owner found the smallest L (3 by 3) always gave a row gem, a column gem needed a 3 by 4 L that is rare, and a 3 by 2 L is the common shape. The owner also wanted the short T and long T to differ, with a size relation between their effects. Result: a run of 3 with one hanging gem is a short L (line gem along the run) or a short T (short X, diagonals 2 cells out), an L with both arms of 3 or more is the cross gem, and a long T or + is a full X (both diagonals across the board). Straight 4 stays blast and straight 5 or more stays the colour gem.
- Impact: two new special kinds (`X_SHORT`, `X_FULL`) with save letters `S` and `F`, an effect, an overlay and an animation each, and a bigger pair table (`docs/special-gem-rules.md`, pinned by `PairCombosTest`). `MatchGroup` gains an optional hanging gem and `findMatchGroups` attaches it, so some matches now clear four gems instead of three. A run of 3 with a hanging gem scores like a run of 4 (60, no shape bonus). Specials become more frequent, so pace and balance need tuning by play. A `GM2` save with `S` or `F` cannot be read by a build from before this decision (it decodes to null and starts a new game); only test devices hold such saves.
- Rollback: map `classify` back (L to line gem by the longer arm, T to cross) and stop attaching the hanging gem in `findMatchGroups`. Keep the `S` and `F` letters in the reader so saves that hold them still load.

**D18. T tiers by size: short T (5 gems) is the short X, long T is the full X, and a crossing shape beats the colour gem.**
- Why: the owner pictured the short T as a bar of 3 with a stem of 2 and the long T as a bar of 5 with a stem of 2, and asked that both get their own gem instead of the long one becoming the colour gem. D17 had named a 4-gem shape the short T. The multi-perspective review also found that a gem hanging off the middle made a different special from one hanging off an end for no reason a player could guess.
- Impact: a group of two or more crossing runs now beats a run of 5 (an L of any size is a cross gem; a T is a short X when every run is exactly 3 and a full X when any run is 4 or more). Only a straight run of 5 or more is the colour gem. A run of 3 takes a hanging gem only at an end (a short L, a line gem); a gem touching the middle is left behind and the run is a plain 3-match. The save letters for the X gems are `S` (short X) and `F` (full X), added to D13's `B R C X W`. The effect animations of one clear now overlap (the first plays at full length, the rest start 0.12 s apart and run quicker, capped so a chain adds at most 0.7 s), a tap during playback skips to the end of the move, a gem a colour gem converts shows its new kind when its effect starts, and the full X is drawn inside a ring.
- Rollback: restore the earlier `classify` order (colour gem first) in `Creation.kt` and let `hangingGem` in `Matches.kt` accept the middle cell again. The `S` and `F` letters stay readable either way.

**D19. A colour gem covered by an effect takes the colour of the gem that covered it.**
- Why: the first version cleared a covered colour gem and did nothing else, because a colour gem has no colour of its own and the spec was silent. In play the owner saw a colour gem blasted by a row gem do nothing, which looked like a bug. Considered: converting and firing every gem of the covering colour (stronger, longer playback) and a random or most common colour (unpredictable); the owner chose the plain take.
- Impact: in a chain, a covered colour gem adds an `Activate` that lists every gem of the covering gem's colour plus itself, and those gems clear in the same clear (and can chain further). It fires once. It needs a coloured covering gem, which is always so (an effect gem is never a colour gem, and a colour gem's own cells never include another colour gem).
- Rollback: make `firing` in `Activation.kt` return null for a colour gem with no swap partner, and the old test comes back from git history.

**D20. Two of the same line or blast make one bigger effect.**
- Why: swapped up or down, two column lines share a column and cleared only 8 cells, the weakest pair in the table, and two blasts cleared only 12. The owner called these rare situations that should pay off, and chose a three-column version.
- Impact: two column lines clear three columns, two row lines three rows (each centred on the cell the dragged gem landed on, so they always include both gems' own), and two blasts clear a 5x5 square. That is 24, 24 and 25 cells on an open board. Both gems fire as one `Activate` from the landing cell. Cross + Cross, Short X + Short X and Full X + Full X are unchanged (22, 18 and 28). The app draws one beam per line and a ring scaled to the reach. `PairCombosTest` and the rules page carry the new numbers.
- Rollback: remove the `first == second && first in DOUBLED` branch in `initialActivations` (`Activation.kt`) and restore the three table rows.
