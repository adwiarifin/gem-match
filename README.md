# Gem Match

An offline match-3 game for Android, in the spirit of Bejeweled. No ads, no accounts, no network, zero permissions. The release APK is about 54 KB unsigned (about 67 KB signed).

Gems swap by dragging. Match three or more in a row or column, intersecting runs (L, T, +) clear together, gems fall, new ones drop in, and chains score more. The board saves after every move, so killing the app at any moment brings back the same board and score. There is no game over: when no move is left the board reshuffles.

This is a hobby project and a portfolio piece, built to show how far framework Android gets without libraries.

## Design

Two modules.

- **`core`** is pure Kotlin with no `android.*`, `java.*` or `javax.*` imports and no Android Gradle plugin, so it is ready for Kotlin Multiplatform. It holds the rules: board, match detection, valid moves, cascades, tiered scoring, reshuffle, swipe geometry and a versioned save format. A move resolves instantly and returns a list of steps (swap, clear, fall, spawn, reshuffle); the game state is immutable and deterministic, driven by its own SplitMix64 random generator whose state is part of the save.
- **`app`** is the presentation layer: a `SurfaceView` drawn with `Canvas` from scratch, a fixed-timestep game loop, vector gem shapes (seven distinct silhouettes, so gems differ by shape as well as colour), and a timeline that plays the core's steps back as animation. It depends on `core` and nothing else: no AndroidX, no Compose, no bitmap assets.

The core never knows about animation or drawing, so a second UI (Compose, a ready-made game library) or another platform can reuse it unchanged.

## Requirements

- JDK 21
- Android SDK with platform 37 and build-tools (set `sdk.dir` in an untracked `local.properties`)
- For the checks: [gitleaks](https://github.com/gitleaks/gitleaks), [osv-scanner](https://github.com/google/osv-scanner) and Python 3

## Build and install

```
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Signed release

Signing never happens in an automated or agent session. `scripts/sign-release.sh` builds the unsigned release APK, then runs `apksigner`, which prompts for the key password in your own terminal.

```
make sign
adb install app/build/outputs/apk/release/gemmatch-release.apk
```

The script refuses to sign with uncommitted changes, or an unsigned APK built from a different revision than `HEAD`, so a signed build always matches a commit. It prints that commit and the signing certificate's SHA-256 when it finishes.

It expects the keystore at `~/Private/keys/gemmatch/gemmatch-release.jks` with alias `gemmatch`; override with `GEMMATCH_KEYSTORE` and `GEMMATCH_KEY_ALIAS`. The keystore is never committed. Lose it and a later update cannot install over the old app.

## Checks

`CONSTRAINTS.md` is the quality bar. The `Makefile` mirrors it:

| Command | What it runs | Budget |
|---|---|---|
| `make check-fast` | core compile, ktlint, core boundary, floor guard, gitleaks | seconds |
| `make check-task` | the above, plus core tests, the 80% coverage gate and the app unit tests | about a minute |
| `make check-full` | the above, plus detekt, Android Lint, APK builds, APK size caps, osv-scanner | a few minutes |

`make release-check BASE=<last reviewed commit or tag>` is the gate before calling a version released: the floor guard against everything since `BASE`, then `make check-full`. `check-full` also fails if the app ever declares an Android permission.

The floor guard (`scripts/floor-guard.py`) looks at the working diff for new suppression comments, stubs, skipped or deleted tests, removed assertions and loosened rules.

## Layout

```
core/    pure Kotlin game rules and tests
app/     Android UI: SurfaceView, game loop, animation, save files
scripts/ core boundary, APK size, floor guard and release signing
SPEC.md  what the game is and must do
docs/    decision log (why it is built this way) and the original intent
tasks/   plan and task list
```

## Saves

Two small files in the app's private storage: `game.sav` (score, random-generator state and the 64 cells, in a versioned text format) and `best.txt`. Both are written to a temporary file, synced, and renamed over the real one, so a kill mid-write cannot leave a half-written save. A corrupt or empty save starts a new game.

## License

MIT, see `LICENSE`.
