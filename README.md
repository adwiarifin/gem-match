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

## Releases

Versions follow [Semantic Versioning](https://semver.org), 0.x until the game feels finished. [release-please](https://github.com/googleapis/release-please) reads the conventional commits on `main` and keeps a release pull request open: `feat` bumps the minor version, `fix` bumps the patch version, and the changelog is built from them. `versionCode` is derived from the version (1.4.2 becomes 10402), so nothing is bumped by hand.

Merging the release pull request tags `vX.Y.Z` and starts the signing job. It rebuilds from the tagged commit, runs `make check-full`, then waits for the owner's approval in the `release` environment before it gets the signing key. The signed `gemmatch-X.Y.Z.apk` and its `.sha256` are attached to the GitHub release.

Setup, once, in the repository settings: allow Actions to create pull requests, create an environment named `release` with a required reviewer, and add the secrets `KEYSTORE_BASE64`, `KEY_ALIAS`, `KEYSTORE_PASSWORD` and `KEY_PASSWORD` to it. Version `0.1.0` is forced by `release-as` in `release-please-config.json`; delete that line after the first release.

### Installing a release

Download the APK from the latest release, check it with `shasum -a 256 -c gemmatch-X.Y.Z.apk.sha256`, and open it on the phone (Android asks to allow installs from that app). Play Protect may warn that the developer is unknown, because the app is not on Google Play. Update by installing the newer APK over the old one; uninstalling first deletes the saved game.

### Signing on your own machine

`scripts/sign-release.sh` does the same locally and prompts for the key password in your own terminal. Signing never happens in an agent session.

```
make sign
adb install app/build/outputs/apk/release/gemmatch-release.apk
```

The script refuses to run with uncommitted changes and always rebuilds the APK itself (there is no way to sign an existing file), then checks that the APK records the current `HEAD`, so a signed build always matches a commit. It prints that commit and the signing certificate's SHA-256 when it finishes.

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
