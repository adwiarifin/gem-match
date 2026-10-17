# Constraints

Last reviewed: 2026-10-15 by @adwiarifin

Status: live. Every command below runs today. `make check-fast`, `make check-task` and `make check-full` mirror this file.

## Floor (always enforced, blocks)

- No new suppression comments: `@Suppress`, `@SuppressLint`, `// ktlint-disable`, `@file:Suppress`, `detekt:suppress`, `lint-baseline` entries added to hide new findings.
- No unimplemented stubs: `TODO()`, `throw NotImplementedError()`, empty `catch` blocks.
- No `@Ignore`d or deleted tests without a reason in the commit message.
- No secrets in source.
- No signing material in the repo or in an agent session. Release signing runs through `scripts/sign-release.sh` in the owner's own terminal, which prompts for the password.
- This file does not get weakened to make a change pass.

## Enforced with numbers

| Dimension | Rule | Why | Checked by | Runs at | On failure |
|-----------|------|-----|-----------|---------|-----------|
| Compile | Zero errors in `core` | Cheapest signal there is | `./gradlew :core:compileKotlin` | every edit | block |
| Style | Zero ktlint errors in `core` and `app` | Formatting noise stays out of diffs | `./gradlew ktlintCheck` | every edit | block |
| Secrets | No secrets in source | A leaked key in a public repo or blog repo is permanent | `gitleaks detect --redact --no-banner` | every edit | block |
| Floor guard | No new suppression comments, stubs, skipped or deleted tests, removed assertions, new exceptions, or loosened rules in this file | The five moves an agent makes to get to green. Diff-scoped, so it costs milliseconds. A local gate: CI has no diff to check on its own run, so it bites on the working tree and, through `make release-check`, on everything since the last release | `scripts/floor-guard.py` (its own cases: `scripts/test-floor-guard.sh`, run in `make check-fast`) | every edit | block |
| Core boundary | `core` has no `android.*`, `java.*`, `javax.*` imports and applies no Android Gradle plugin | Keeps the core KMP-ready and the presentation layer replaceable (stated project goal) | `scripts/check-core-boundary.sh` | every edit | block |
| Core tests | All `core` tests pass | Game rules are the product | `./gradlew :core:test` | task end | block |
| Test count | No fewer tests run than `scripts/test-count-floor.txt` says (raise it as tests are added, the floor guard flags lowering it) | A test that silently stops running (abstract class, early return, `assumeTrue(false)`) keeps the suite green while the count falls | `scripts/check-test-count.sh` (its own cases: `scripts/test-check-test-count.sh`, run in `make check-fast`) | task end | block |
| App tests | All `app` unit tests pass | The drag, animation, session and save logic is where the playtest bugs were found. Android-free classes only; rendering and wiring stay with the on-device checks | `./gradlew :app:testDebugUnitTest` | task end | block |
| Coverage | `core` line coverage ≥ 80% | Core is pure logic and built test-first, so 80% is reachable from day one. Kover has no diff-scoped mode, so the rule is project-wide on `core` | `./gradlew :core:koverVerify` | task end | block |
| Static analysis | Zero detekt errors in `core` and `app`, zero Android Lint errors | Standard Kotlin and Android tools, external opinion | `./gradlew detekt :app:lint` | review, CI | block |
| APK size: release | ≤ 1,048,576 bytes (1 MiB) | Measured 53,686 bytes on 2026-10-15, rounded up to the next MiB so features have room to grow. The checked file is the unsigned APK; signing adds about 13 KB (RSA-4096 certificate and signature blocks), so the signed APK is about 67 KB | `./gradlew assembleRelease` then `scripts/check-apk-size.sh` | review, CI | block |
| APK size: debug | ≤ 2,097,152 bytes (2 MiB) | Measured 1,053,372 bytes on 2026-10-15, rounded up to the next MiB so features have room to grow | `./gradlew assembleDebug` then `scripts/check-apk-size.sh` | review, CI | block |
| Permissions | The app declares no Android permission | The "zero permissions" promise is part of the product. The check fails closed: if `aapt2` cannot read the APK it fails rather than passing | `scripts/check-permissions.sh` (its own cases: `scripts/test-check-permissions.sh`, run in `make check-fast`) | review, CI | block |
| CI gate | `make check-full` passes on every push to `main` and every pull request. A release is built by a job with no secrets and signed only after the owner approves, and only if the APK's hash, commit, version and certificate match | The key cannot be rotated for installed copies, so build code must never run beside it | `.github/workflows/ci.yml`, `.github/workflows/release.yml`, `scripts/test-sign-ci.sh` (run in `make check-fast`) | every push, every release | block |
| Dependency integrity | Every artifact Gradle resolves, for the app and the build tooling, matches a SHA-256 committed in `gradle/verification-metadata.xml` | A replaced or hijacked dependency fails the build instead of reaching the unsigned APK. Regenerate with `./gradlew --write-verification-metadata sha256 build` after an upgrade and review the diff | Gradle itself, on every build | every build | block |
| Dependencies | Nothing at high severity or above in what ships in the APK | App is meant to have almost no dependencies, so any finding is a signal. The lockfiles (`core/gradle.lockfile`, `app/gradle.lockfile`) cover `runtimeClasspath` and `releaseRuntimeClasspath` only. Build tooling (AGP, detekt, ktlint) is not scanned because it never reaches a player; scanning it reports bundled BouncyCastle and logback findings that cannot be fixed from this repo | `osv-scanner scan source -r --x-exclude gradle .` (the `gradle` directory holds `verification-metadata.xml`, which lists the build tooling's own libraries too) | CI | block |

`make release-check BASE=<ref>` runs the floor guard against everything since `BASE`, then every check above. Run it before calling a version released.

External checks (not judged by this project's own tests): APK size, Android Lint, gitleaks, osv-scanner.

## Time budget (local, task end)

- Aim for under 2 minutes. Hard ceiling 5 minutes.
- Edit loop must stay under about 10 seconds.
- Anything slower than the task-end budget (APK builds, Lint, detekt, osv-scanner) moves to review or CI. If a task-end check creeps past 5 minutes, move it out rather than raising the ceiling.

## Measured, not yet enforced

| Metric | Today | Direction |
|--------|-------|-----------|
| Release APK size | 53,686 bytes unsigned, 66,989 bytes signed (R8 on, 2026-10-15) | must not grow; the cap is the measured value rounded up to the next MiB |
| Debug APK size | 1,053,372 bytes (2026-10-15) | must not grow |
| `core` line coverage | 100% (261 core tests and 104 app unit tests, 2026-10-17) | must not fall (tolerance 0.5%) |
| Frame time on the owner's Galaxy A52 | During move animations (755 frames, `dumpsys SurfaceFlinger --latency`, 2026-10-15): mean 17.2 ms, p99 23.1 ms, max 55.7 ms, 7 frames over 33 ms. The panel runs at 90 Hz (11.1 ms) and the game loop at 60 Hz, so frames arrive in an uneven 11/22 ms rhythm | no tool-backed check yet. Candidate fix: drive the loop from the display's vsync (post-v1) |
| Release APK size, with special gems | 63,942 bytes unsigned (R8 on, 2026-10-17); signing adds about 13 KB | must not grow without the owner's say-so; the cap above is unchanged |
| Debug APK size, with special gems | 1,114,922 bytes (2026-10-17) | must not grow without the owner's say-so; the cap above is unchanged |
| Frame time on the owner's Galaxy A52, with special gems | `dumpsys SurfaceFlinger --latency` on the SurfaceView layer, 2026-10-17, 7 s windows (about 405 frames, the loop draws continuously): a plain match move mean 16.9 ms, p99 22.7 ms, max 33.4 ms; a colour gem plus blast converting nine gems with two chained specials (run twice) mean 16.9 ms, p99 22.6 and 22.7 ms, max 44.5 and 44.6 ms, one frame over 33 ms | no regression against the 2026-10-15 row; the single 44 ms frame is the release frame, where the move resolves and the timeline is built. No tool-backed check yet |

The first two size rows are the v1 baseline of 2026-10-15. Features since then grew both (still far under the caps), so the "with special gems" rows are the current baseline. Moving the v1 rows themselves needs an owner exception that the floor guard cannot express yet: it reads a raised "must not grow" number as loosening.

Ratchet tolerance: 0.5% on coverage.

## Dropped dimensions

- Lighthouse, Core Web Vitals, axe-core: they need a running URL and this is a native app. Revisit only if a web build is ever added.
- Mutation testing (Stryker): JS-only. PIT is the Kotlin equivalent. Add after the core has real tests, if wanted.

## Exceptions

| ID | Rule | Path | Reason | Owner | Expires |
|----|------|------|--------|-------|---------|
| (none) | | | | | |
