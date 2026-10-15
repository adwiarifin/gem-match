# Intent: Bejeweled Clone

Status: confirmed by the owner.

## Outcome

An offline, native Android match-3 game (Bejeweled-style) that is playable first and has a tiny APK.

## User

The owner, playing on a Samsung Galaxy A52 in free time. No ads, no internet requirement, no accounts.

## Why now

The owner is tired of modern games that need internet and show many ads. The project is also a hands-on learning exercise, a portfolio piece, and a source of blog posts for kesatriadev.

## Success

- The game runs on the owner's phone.
- The owner can pause, quit, or be interrupted at any time and resume exactly where they left off.
- The game stays enjoyable on repeat.

## Constraints

- Tiny APK. Target about 1-2 MB, never hundreds of MB.
- Fully offline.
- Core logic is pure Kotlin with no Android imports, and KMP-ready (Kotlin stdlib only, injected time and randomness).

## Architecture

- Core module: board, match detection, cascade, scoring, seeded RNG.
- Presentation layer: SurfaceView + Canvas, built from scratch with an explicit game loop.
- Later: a second UI (Jetpack Compose or a ready-made library) and/or a KMP port reusing the same core. Each is a separate blog perspective.

## v1 scope

- Swap, match, cascade, scoring.
- Endless play with no game over. The board auto-reshuffles when no valid moves remain.
- Autosave on every move and on pause. Resume on launch.
- Saved high score.

## Out of scope for v1

- Special gems and power-ups.
- Levels.
- Online features, ads, accounts, leaderboards.
- iOS and web.
- Sound (first candidate for a post-v1 addition).
