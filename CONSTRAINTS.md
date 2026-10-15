# Constraints

Last reviewed: 2026-10-15 by @adwiarifin

Status: written, not yet runnable. The repo has no Gradle project yet. Every command below becomes live when the scaffold lands (see "Pending setup").

## Floor (always enforced, blocks)

- No new suppression comments: `@Suppress`, `@SuppressLint`, `// ktlint-disable`, `@file:Suppress`, `detekt:suppress`, `lint-baseline` entries added to hide new findings.
- No unimplemented stubs: `TODO()`, `throw NotImplementedError()`, empty `catch` blocks.
- No `@Ignore`d or deleted tests without a reason in the commit message.
- No secrets in source.
- This file does not get weakened to make a change pass.

## Enforced with numbers

| Dimension | Rule | Why | Checked by | Runs at | On failure |
|-----------|------|-----|-----------|---------|-----------|
| Compile | Zero errors in `core` | Cheapest signal there is | `./gradlew :core:compileKotlin` | every edit | block |
| Style | Zero ktlint errors | Formatting noise stays out of diffs | `./gradlew ktlintCheck` | every edit | warn until 2026-10-29, then block |
| Secrets | No secrets in source | A leaked key in a public repo or blog repo is permanent | `gitleaks detect --redact --no-banner` | every edit | block |
| Core boundary | `core` has no `android.*`, `java.*`, `javax.*` imports and applies no Android Gradle plugin | Keeps the core KMP-ready and the presentation layer replaceable (stated project goal) | `scripts/check-core-boundary.sh` | every edit | block |
| Core tests | All `core` tests pass | Game rules are the product | `./gradlew :core:test` | task end | block |
| Coverage | `core` line coverage ≥ 80% | Core is pure logic and built test-first, so 80% is reachable from day one. Kover has no diff-scoped mode, so the rule is project-wide on `core` | `./gradlew :core:koverVerify` | task end | warn until 2026-10-29, then block |
| Static analysis | Zero detekt errors, zero Android Lint errors | Standard Kotlin and Android tools, external opinion | `./gradlew detekt lint` | review, CI | warn until 2026-10-29, then block |
| APK size: release | ≤ 10,485,760 bytes (10 MiB) | Placeholder guess from the owner. Real target is ~1-2 MB, so ratchet down after the first release build | `./gradlew assembleRelease` then `scripts/check-apk-size.sh` | review, CI | block |
| APK size: debug | ≤ 26,214,400 bytes (25 MiB) | Placeholder guess from the owner | `./gradlew assembleDebug` then `scripts/check-apk-size.sh` | review, CI | block |
| Dependencies | Nothing at high severity or above | App is meant to have almost no dependencies, so any finding is a signal | `osv-scanner scan source -r .` (needs Gradle dependency lockfiles) | CI | warn until 2026-10-29, then block |

External checks (not judged by this project's own tests): APK size, Android Lint, gitleaks, osv-scanner.

## Time budget (local, task end)

- Aim for under 2 minutes. Hard ceiling 5 minutes.
- Edit loop must stay under about 10 seconds.
- Anything slower than the task-end budget (APK builds, Lint, detekt, osv-scanner) moves to review or CI. If a task-end check creeps past 5 minutes, move it out rather than raising the ceiling.

## Measured, not yet enforced

| Metric | Today | Direction |
|--------|-------|-----------|
| Release APK size | not built yet | must not grow; tighten the cap to the first measured value plus 10% |
| Debug APK size | not built yet | must not grow |
| `core` line coverage | no code yet | must not fall |
| Frame time on the owner's Galaxy A52 | not measured | record manually once the render loop exists. No tool-backed check yet |

Ratchet tolerance: 0.5% on coverage.

## Dropped dimensions

- Lighthouse, Core Web Vitals, axe-core: they need a running URL and this is a native app. Revisit only if a web build is ever added.
- Mutation testing (Stryker): JS-only. PIT is the Kotlin equivalent. Add after the core has real tests, if wanted.

## Pending setup (do when the scaffold exists)

1. Create Gradle project with a `core` module (plain `kotlin("jvm")`, no Android plugin) and an `app` module.
2. Add Kover, ktlint, detekt, and Android Lint config. Turn on Gradle dependency locking for osv-scanner.
3. Write `scripts/check-core-boundary.sh` and `scripts/check-apk-size.sh`.
4. Install gitleaks (`brew install gitleaks`) and osv-scanner (`brew install osv-scanner`).
5. Add a floor guard (diff-scoped check for the Floor rules above) and wire `check:fast`, `check:task`, `check:full` as a `Makefile` or Gradle aliases.
6. After the first release build, record the measured APK size above and lower the cap.

## Exceptions

| ID | Rule | Path | Reason | Owner | Expires |
|----|------|------|--------|-------|---------|
| (none) | | | | | |
