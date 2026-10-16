# Ideas for what to build next

Captured 2026-10-15, after v1 (T1-T18 and Checkpoint D). Four research passes looked at the game through different lenses and returned these ideas. Nothing here is chosen, ranked or approved. Choosing is a separate step; this file only keeps the ideas so they are not lost.

Each idea has an ID (G = gameplay, F = feel, P = product, T = technical, B = blog) so later notes and specs can point at it. Sizes: S is under a day, M a few days, L a week or more.

Caveats about the sources. The passes were done by subagents, not by the owner. Some did no web search, so claims about KMP, Compose Multiplatform, libGDX and match-3 history come from background knowledge and should be checked before relying on them. Some also did not read every file, so details about the save format and the Android theme are partly inferred. The vsync idea (F1 and T6) and the black-strip idea (F2) are the only ones backed by a measurement or an observation from this project.

Where an idea conflicts with the Out of Scope list in `SPEC.md` (special gems and power-ups, levels or goals, timers, best single move, sound, haptics, themes, tablets and landscape), the entry says so. Dropping the conflict was not the goal; it is a flag for the decision.

## Gameplay: mechanics and modes

**G1. Best single move record.** Track the most points from one accepted swap (the sum of its Clear steps) and show it next to the best score with a short "new best move" flourish. Size S. App mostly; the per-move total already exists. A separate number in a preferences file, so the save format is untouched. SPEC calls it a "possible later addition", so the conflict with Out of Scope is mild. See also P1.

**G2. Deep-cascade bonus and chain banner.** A flat bonus or steeper multiplier from depth 4 up, plus a "Chain x5" banner driven by `Step.Clear.depth`. Alternatively a streak of consecutive moves that each cascade. Size S. Scoring constant in core, banner in app. Old high scores become less comparable (same caveat as D2). A steeper curve may let luck beat planning, so it needs tuning.

**G3. Four-match and five-match special gems.** A run of 4 leaves a blast gem (clears 3x3), a run of 5 leaves a colour gem (clears every gem of one type), and L, T and + shapes could leave a line gem. Size L. Both: cells need a modifier, new spawn rule in `Matches.kt`, chain reactions in `Resolution.kt`, a new Step type, new shapes, and a save format bump to GM2 with a GM1 reader. Conflicts with Out of Scope (special gems). The explosions must read clearly, in line with the owner's preference for visible transitions.

**G4. Zen mode.** No score pressure, a calmer palette, a cosmetic running total of gems cleared, maybe an option to never reshuffle without a hint. Size S. App mostly (mode flag in preferences). Risk: it may feel the same as the existing endless game.

**G5. Timed sprint mode.** A 60 or 90 second round with a countdown that excludes animation time, ending with a score card and a per-mode best. Size M. Both. If the clock lives in `Game`, GM1 needs a bump; if the app owns it, no migration. Conflicts with Out of Scope (timers). Open question: whether the clock pauses during animations and when backgrounded.

**G6. Move-limited mode.** A fixed number of accepted swaps (for example 30) with a score target shown as stars; rejected swaps cost nothing. Size M. Both. Makes "no game over" (SPEC assumption 7) mode-specific. Fits the planned-move scoring idea better than a timer.

**G7. Daily seeded board.** Seed derived from the local date so every install gets the same opening board and spawns; one run per day and a per-date best table. Size M. Core already has `Game.new(seed)`; the app needs a second save slot and history. Needs a bound such as a fixed move count to be comparable, which conflicts with Out of Scope (timers, goals). Daily results stay comparable only while RNG consumption by the rules does not change. See also P5, T3.

**G8. Goal levels with collect-N objectives.** A ladder of levels such as "clear 20 red gems in 15 moves", "reach 3000 points", "make a chain of depth 4", each a seed, a goal and a limit stored as a small table in core. Size L. Needs a Goal type, level select and result screens, and saved progress. Conflicts with Out of Scope (levels or goals). Balancing is the slow part.

**G9. Blocker cells and jelly tiles.** A second board layer: ice that needs a match on its cell to break, or locked cells gems cannot enter. Size L. Both. Gravity around obstacles complicates the column-by-column fall in `Resolution.kt` (diagonal slides may be needed). GM2 migration. Needs full tests to hold coverage.

**G10. Board variants.** Choose a board at New Game, for example 6x6 with 5 gem types (easier) or 9x9 with 8 types (harder), by turning `BOARD_SIZE` and the gem count into parameters. Size M. Both. Save format needs size and colour fields. Fewer colours means more accidental cascades; reshuffle must handle the changed density. Does not override the owner's 7-gem choice.

**G11. Risk gems with countdowns.** An occasional bomb gem whose countdown ticks each move and ends the run if it reaches zero unless matched first. Size L. Needs a per-cell countdown, a tick step, a game-over state and a save format change. Conflicts with SPEC assumption 7 (no game over). Probably only fits an optional hard mode.

**G12. Hint cost and assisted mode.** A manual hint button that costs points or a charge refilled by chains, and a choice between auto-hint (today's idle hint) and no hint. Size S. Both; the valid-move finder already exists. Hint-using runs may need to be kept off the best score. See also P4.

**G13. Combo-charged power-up.** Chain depth fills a meter; a full meter grants one action such as clearing one gem type or a row, or undoing a move. Size M. Both. Can reuse the existing Clear, Fall and Spawn steps. GM bump for the meter. Conflicts with Out of Scope (power-ups), and swipe-only input makes target selection awkward.

## Game feel, polish, visuals, audio

**F1. Vsync-driven frame pacing.** Replace the sleep-based 60 Hz fixed-step thread with Choreographer callbacks, feeding real frame deltas to animation; optionally request a frame rate with `Surface.setFrameRate` on API 30+. Size M. App (`GameLoop.kt`, maybe `GameSurfaceView.kt`; needs a Looper thread). Fixes the measured uneven 11/22 ms rhythm on the 90 Hz A52 and would give `CONSTRAINTS.md` a tool-backed frame row. Idle timer deltas must still be clamped. Same idea as T6.

**F2. Fix the black top strip.** Set `windowLayoutInDisplayCutoutMode=shortEdges` in a `values-v28` theme overlay, make the status bar transparent or use immersive mode, let the background fill behind the cutout, and offset the HUD and board by the window insets. Size S. App (theme, `MainActivity`, `Hud`, `GameSurfaceView`). This is the open cosmetic issue from T18. The board centring in `surfaceChanged` must account for the top inset.

**F3. Procedural sound effects.** Generate short PCM buffers in code (swap blip, clear chime whose pitch rises with cascade depth, landing thud, reject buzz, reshuffle sweep) and play them with `AudioTrack`; a mute toggle in saved settings. Size M. App only. No assets, no permission, code size only. Conflicts with Out of Scope (sound). Quality needs tuning by ear; decide on ringer-mode handling.

**F4. Haptic ticks.** `performHapticFeedback` on the SurfaceView for pickup, accepted swap, reject (API 30+) and big matches, with an on/off toggle. Size S. App. Needs no VIBRATE permission (only `Vibrator` calls do), so zero permissions holds. Conflicts with Out of Scope (haptics). Varies by device and the system touch-feedback setting.

**F5. Particle bursts on clear.** A pooled, fixed-array particle system spawning tinted shards from each cleared cell, bigger for 4 and 5 matches and shapes. Size M. App (a `Particles` class owned by `Scene`, drawn after sprites). Code only. Cap the count and profile on the A52 against the 90 Hz budget.

**F6. Score pop-ups and combo callouts.** Float the awarded points up from each cleared run, brighter for higher depth, a "x3 chain" banner and a count-up score pulse. Size S. App; `Step.Clear` already carries points and depth. Reuse paints and prebuilt strings to avoid per-frame allocation.

**F7. Screen shake and landing squash.** A short decaying board offset on big events (5-gem runs, shapes, depth 3+) and a squash-and-stretch on landing gems, reusing `Scene` fields like `dx`. Size S. App. Needs amplitude caps and a reduce-motion option (F14).

**F8. Gem shine and depth shading.** Highlight paths, gradients or a darker lower edge on the vector shapes, optional idle sparkle on one gem at a time. Size M. App (`GemShapes.kt`, `Palette.kt`). Still vector, no bitmaps. Must not blur the shape differences that act as the colour-blind aid; shaders cost more than flat fills at 64 sprites.

**F9. Backgrounds and theme packs.** An animated or gradient backdrop behind a translucent board panel, and a Palette turned into a theme object with a few themes (dark, light, high contrast), following system dark/light for a free first version. Size M. App; hard-coded colours in `Hud`, `BoardRenderer` and `ConfirmDialog` must be collected first. Conflicts with Out of Scope (themes). See also P8.

**F10. Colour-blind-safe palette and in-shape markings.** A second tint set (for example Okabe-Ito based) plus small inner marks per shape, behind a toggle. Size S. App (`Palette.kt`, `GemShapes.kt`). Overlaps themes in the Out of Scope list. Red, orange and yellow are probably the worst pair today. See also P8.

**F11. Smoother state transitions.** Fade or scale the board in at launch and resume, fade the dialog dim layer and pop its buttons in, tint-pulse the New Game button, ease HUD numbers. Size S. App. Extends the owner's visible-transition preference (D7, D8). Resume must stay cheap, and criterion 12 (no hint after resume) must still hold.

**F12. Richer hint and select feedback.** A glowing ring or arrow trail on the hint, a soft shadow under the held gem, a red shake on a rejected swap. Size S. App. The drag preview must look identical for valid and invalid swaps (SPEC), so any glow cannot reveal valid moves.

**F13. Procedural music loop.** A generative ambient pad synthesised at runtime on its own audio thread, shifting with cascade depth, with a separate music mute. Size L. App; shares the F3 audio path. Conflicts with Out of Scope (sound). Adds CPU and battery use and needs audio-focus handling; may get annoying fast.

**F14. Reduce-motion and effects intensity.** One setting (Full, Reduced, Off) scaling particles, shake, pop-ups and rattle, defaulting to the system animation scale. Size S. App. Only matters once the effects exist, and it implies a shared settings panel (see P3).

## Product features and meta

**P1. Records and lifetime stats.** Best single move, longest cascade, biggest shape, games played, total moves and foreground play time in a new `stats.txt`, folded from the `Step` list by a pure core function and shown in a panel. Size S to M. Both. A separate file means no GM1 bump. Play time must come from loop deltas, not wall-clock. Brings G1 in from Out of Scope.

**P2. Pause and menu screen.** An in-canvas menu (Resume, New Game, Stats, Settings, How to Play) opened from the HUD, reusing `ConfirmDialog` for destructive choices. Size M. App only. It is the home for most other items. The idle timer and loop must treat the menu as paused.

**P3. Settings file.** A `settings.txt` with toggles for hints on/off, hint delay, reduced motion and swipe sensitivity, written atomically like the other files. Size S to M. App; core only if the swipe threshold becomes a parameter. Lets the owner tune the "starting guess" constants on the phone. Start with two or three toggles.

**P4. Undo last move and on-demand hint.** Keep the previous immutable `Game` for a one-step undo and a "show hint now" button reusing the idle-hint finder. Size S in memory, M if undo must survive a kill (GM2). Softens the planned-move design; options are a cost, one use per game, or no best-score update after an undo. See also G12.

**P5. Seed sharing.** Start from a typed or shared seed, alongside the daily board (G7). Size M. Both. A fair comparison needs a fixed move count. See also T3.

**P6. Share a score as text.** A Share button firing `ACTION_SEND` text/plain ("I scored 4,820 in Gem Match, best move 640"). Size S. App; the text could come from a testable core function. Zero permissions; the receiving app does the sending. Keep it text; an image needs a FileProvider.

**P7. Manual export and import of progress.** Encode `game.sav`, `best.txt` and stats as one short checksummed text string, shared or copied to the clipboard, and import by paste. Size M. A pure core bundle codec plus app plumbing. Closes the gap from `allowBackup=false` and sideloading, and is the safety net for the lost-keystore case. Treat import as untrusted input and cap the length.

**P8. Accessibility pack.** A high-contrast palette, reduced motion, scalable HUD text, and `announceForAccessibility` for score and reshuffle messages. Size M (palette, motion and text are S; real TalkBack play is L). App. "Themes" is on the Out of Scope list, so frame it as an accessibility setting. Lint may flag the SurfaceView's content description and the floor ban on suppressions means it must be fixed, not suppressed. Realistic scope is visual and motion options, not full screen-reader play. Overlaps F9 and F10.

**P9. First-run how-to-play.** A drawn overlay on first launch (flag in settings) showing the swipe and scoring tiers, reopenable from the menu, possibly reusing the renderer with a scripted mini-board. Size S to M. App only.

**P10. Indonesian localisation.** Move the app label and in-game text into `res/values/strings.xml` and `res/values-in/strings.xml` and format numbers per locale. Size S now, growing with the menu. App only. Do it after the menu and how-to text exist so each feature is not translated twice.

**P11. Achievements.** 10 to 15 offline badges ("First 5-gem match", "Chain of 5", "Score 10,000") evaluated from the same per-move summary as stats, with a trophy panel and a bitmask line in `stats.txt`. Size M. Both. Store unlocked badges rather than recomputing, since scoring constants may change. Close to "levels or goals", so frame as meta-goals.

**P12. Launcher shortcuts and a visible version.** Static app shortcuts ("New game", "Daily") via `res/xml/shortcuts.xml`, a version name shown on the menu, and a documented `adb install -r` update path with the same keystore. Size S. App and docs. A widget is possible but heavier.

**P13. Landscape and tablet layout.** Drop the portrait lock and compute layout from the surface size, HUD beside the board in landscape. Size M. App only; `BoardLayout` already takes a board position. Conflicts with Out of Scope (tablets and landscape) and SPEC assumption 12. Rotation recreates the Activity, which exercises the save-on-pause path. Mostly portfolio value, since the owner plays only on the A52.

## Technical, engineering and portfolio

**T1. Headless bot and fuzzer.** Play tens of thousands of seeded games against core with several policies (random valid move, greedy by score, lowest-scoring) and assert invariants after every move: board full, no standing matches, a valid move exists, score never decreases, `replay(start, steps)` equals the new board, encode then decode round-trips. Size M. A test source set or a small JVM tool, plus a `make fuzz` target; reuses `TestReplay.kt`. Keep the default run fast and the large run opt-in so it is not mistaken for a skipped test.

**T2. Balance simulator.** Reuse the bot to measure average cascade depth, score per move, reshuffle frequency and fallback-board rate, and the score distribution with D2 reverted. Size S. Gives evidence for D2 and D3, which were approved before their impact was known. Bot policies are not human play, so the numbers describe the rules, not the feel.

**T3. Replay recorder and playback.** Record the seed plus the list of swaps (enough to reproduce a game exactly because the core is deterministic) in a text format (for example `GMR1`) and add a replay mode that feeds the moves to the existing `Timeline`. Size M. Both. One log serves as a bug report, a regression fixture, a blog demo and a cross-UI conformance check. Endless play makes the log grow, so cap it or keep the last N moves. See also G7, P5.

**T4. Seed-visible bug report.** Keep the start seed and a bounded move log in the save and offer a long-press "copy debug info" that a core test can rebuild a state from. Size S. Core codec (GM2 or an optional trailing section that GM1 readers ignore) plus a HUD hook.

**T5. App module unit tests.** Put the Android-free parts (`DragGesture`, `Timeline`, `IdleHint`, the controller state machine, `GameStore`) behind small seams and test them on the JVM. Size M. The app is where the playtest bugs were found yet it has no tests. Test dependencies stay out of the runtime classpath, so the lockfile and osv scope are unchanged. Keep Android types out of the tested classes instead of masking them with default return values. `CONSTRAINTS.md` could gain an app coverage row.

**T6. Choreographer frame pacing plus a frame-time check.** The F1 change plus a debug-build overlay and a script wrapping `dumpsys SurfaceFlinger --latency` into a p99 pass/fail. Size M. Needs a device and adb, so it can only be a local review-time check, not CI. Same underlying work as F1.

**T7. Instrumented smoke test and goldens.** Drive a debug build with a fixed seed through `adb shell input swipe`, pull `screencap`, and compare to goldens kept outside the APK; or render the scene to an off-screen bitmap on the JVM. Size M to L. Pixel goldens flake across devices, so compare board state or use a tolerance. The debug seed hook adds a few hundred bytes to the debug APK only.

**T8. GitHub Actions CI.** Install JDK 21, the Android SDK, gitleaks and osv-scanner and run `make check-full`, with pinned action SHAs, read-only permissions and the APK size posted to the job summary; a nightly fuzz job. Size S to M. `CONSTRAINTS.md` already names "CI" as a runner but no workflow exists. CI must only build the unsigned release and never see the keystore. Needs the repo on GitHub first.

**T9. Reproducible build.** Pin the toolchain, strip timestamps and add a script that builds the unsigned release twice and diffs the bytes; publish the unsigned APK's SHA-256 per tag. Size M. May turn out reproducible only on the same build-tools version.

**T10. F-Droid and offline distribution package.** A `fastlane/metadata` tree (description, changelog, screenshots, icon), tagged releases, a metadata YAML draft and a GitHub Releases flow with the signed APK and checksum. Size M. F-Droid builds with its own key unless reproducible builds are accepted, which interacts with the keystore warning in the README.

**T11. Supply-chain hardening.** Gradle dependency verification metadata, pinned actions, a CycloneDX SBOM from the two runtime lockfiles, osv-scanner on build tooling as informational, and a check that the app's runtime classpath is nothing beyond the Kotlin stdlib. Size S to M. Dependency verification for AGP and detekt is large and noisy to maintain.

**T12. Conformance vectors for every UI.** Language-neutral test vectors (seed, moves, expected steps or final save string, score) that core tests check today and each future UI or port consumes. Pins SplitMix64 bit-for-bit across JVM, Native and wasm. Size M. Needs a version tag because an intentional rule change regenerates them.

**T13. Compose Canvas UI module.** An `app-compose` module drawing the same scene with Compose, reusing core, the timeline logic and the shape paths, as a separate APK so the 54 KB build is untouched. Size L. A shared Android-free presentation module would have to be cut out of `app` first (`Timeline.kt` and `Scene` live there today). Needs its own size cap row in `CONSTRAINTS.md`. Delivers the size, frame-time, lines-of-code and build-time comparison the owner wants for the blog.

**T14. libGDX UI module, then the KMP port.** First a libGDX desktop and Android perspective using core as a JVM dependency; then migrate core to `commonMain` with targets for JVM, iOS, desktop and wasm. Size L. The core boundary check already guarantees the hard part. Reconciling the boundary script and the "no Android Gradle plugin in core" rule with KMP needs care so the rule is not weakened. Native and wasm toolchains may not fit the 2-minute task budget and would move to CI. Check current Compose Multiplatform and KMP plugin state before starting.

## Blog post ideas

Working titles with a one-line hook. Each comes from a concrete piece of this repo. The blog repo is separate and is not touched from here.

- **B1. The game that fits in 54 KB.** How a match-3 ships as a 54 KB release APK, what R8 removed, and what the 1 MiB cap protects against.
- **B2. Constraints as code, and the five moves an agent makes to get to green.** The floor guard, the core boundary script and the size cap, and why the quality bar lives in a file the agent must not weaken.
- **B3. Steps, not frames.** A game core that returns a list of events, and how one deterministic core drives animation, replay and tests, with the independent `replay()` cross-check.
- **B4. Determinism you can paste into a bug report.** SplitMix64, seeds in the save, and a move log that reproduces any game exactly (needs T3 or T4).
- **B5. I let a bot play 100,000 games.** Fuzzing a match-3 core, the invariants checked, and what the rare reshuffle fallback looks like in numbers (needs T1).
- **B6. The 11 ms / 22 ms stutter.** Profiling frame pacing on a 90 Hz Galaxy A52 with `dumpsys SurfaceFlinger`, then fixing it with Choreographer (needs F1 or T6). The measurement half already exists.
- **B7. Eight decisions I could not judge yet.** A tour of `docs/decisions.md` D1 to D8, with the rollback cost written down before the code existed.
- **B8. Same core, three screens.** From-scratch Canvas, Compose and libGDX compared on APK size, frame time, lines of code and build time (needs T13 and T14).
- **B9. Can I trust my own APK?** Reproducible builds, dependency verification, an SBOM and a path to F-Droid for an app with zero permissions (needs T9 to T11).
- **B10. Kotlin Multiplatform for a game that never wanted it.** What it took, and what broke, especially 64-bit math and number formatting, to move a pure-Kotlin core to iOS, desktop and wasm (needs T14).
- **B11. Keeping an agent away from the signing key.** From plaintext properties file to an interactive signing script, and why a gitignored file is not enough once an agent can read your disk. Already has all its material; no build work needed.
- **B12. Finding the drag-preview feel.** Why a move should fire on release, the three input approaches compared and the playtest log analysis behind D6. Already has its material.
- **B13. Eight ways a release pipeline failed on its first run.** From merging a release PR to a signed APK with release-please and three jobs: `build` with no secrets, `sign` with the key behind an approval, `publish`. Each failure has a real log behind it. The tag ruleset blocked the release bot, and GitHub has no bypass for the Actions app. A fail-safe certificate check deleted a good APK because I guessed apksigner's output format wrong twice. A cold Gradle cache exposed missing `aapt2` entries in the dependency metadata. The release PR went stale because `build:` and `docs:` commits did not count as releases. Two empty releases are stuck because a protected tag cannot be moved. Links to B9 and B11, and already has all its material.
- **B14. Choosing what counts as a release.** `feat` and `fix` only, or every conventional commit type? The cost of a patch release with an identical APK, the 100-patch cap in `versionCode`, why `chore` stays hidden, and how a single open release PR absorbs a patch and then a minor bump. Already has its material.

## Cross-cutting notes

These are patterns across the ideas, for whoever does the prioritising. They are not rankings.

**Conflicts with the Out of Scope list in `SPEC.md`.** Special gems and power-ups: G3, G13. Levels or goals: G8 (G7 and P11 are close). Timers: G5, G7. Best single move: G1, P1. Sound: F3, F13. Haptics: F4. Themes: F9, F10, P8. Tablets and landscape: P13. Each would need a `SPEC.md` edit and the owner's approval first. Game over (G6, G11) conflicts with SPEC assumption 7.

**Ideas that change the save format (GM1 to GM2 with a reader for GM1).** G3, G9, G10, G11, G13 when the state lives in the game, P4 if undo must survive a kill, T4 unless done as a trailing section. Several of these could share one migration. Ideas that avoid it by using a separate file: G1, P1, P3, P11.

**Shared prerequisites.** A menu or settings panel (P2, P3) hosts F3, F4, F9, F10, F14, P1, P8, P9, P12. A stats file (P1) feeds G1, P11 and P6. A single audio path serves F3 and F13. `Palette` becoming a theme object serves F9 and F10. A presentation module split out of `app` (T13) would also help T5 and T14.

**Ideas that overlap or are one piece of work.** F1 and T6 (vsync). G1 and P1 (best move). G7, P5 and T3 (seeds and replay). G12 and P4 (hint and undo). F9, F10 and P8 (appearance and accessibility). F14 and P3 (settings that scale effects).

**Needs the GitHub repo.** T8, T10 and any blog post that links to the code.

**No cost against the current constraints.** Most of F and P, and all of T except T13 and T14, touch only code size. The 1 MiB cap leaves roughly 1,000 KB of room, so APK size is unlikely to decide anything. Permissions stay at zero for every idea here, including haptics and audio.
