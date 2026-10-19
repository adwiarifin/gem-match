# Intent: Game Over and Run Stats

Status: confirmed by the owner.

## Outcome

A run ends when no valid move remains. There is no rescue and no reshuffle. A game-over card shows the run, then Play Again.

## User

The owner, as a player and as the person who cares about metrics.

## Why now

The game has only a timeless mode and no finish state, so there is nothing to chase.

## Success

- Every run ends.
- The game-over card shows the run's stats, the best score and a new-best flag.
- A stats screen shows lifetime records and totals plus the last 100 runs.

## Run

A run is one session: from new game to game over. A run saved and resumed across app launches is still one run.

## Stats per run

- Score, moves made
- Longest cascade (chain reaction within a single move)
- Biggest single-move score
- Gems cleared
- Specials created and triggered (by kind, if cheap)
- Biggest match
- Best score per move
- Wall-clock duration (new game to game over, including time the app was closed)
- Active duration (app in the foreground)
- End timestamp
- New-best flag

The game-over card shows the top five. The stats screen shows all.

Lifetime stats keep the same data as totals, plus bests for score, cascade, move score, match size and longest run (both clocks), and the number of runs played. Average score is derived, not stored.

## Storage

- `runs.txt`: the last 100 runs, oldest dropped, one key-value line per run with a version tag (`RN1;score=1200;moves=40;...`).
- `lifetime.txt`: one line covering every run ever played, including runs dropped from `runs.txt`. Updated on every game over, never recomputed from `runs.txt`.
- Both use the `GameStore` atomic write (temp file, then rename, on the writer thread).
- The codecs live in `core`, behind a thin history interface in the app layer so the backend can be swapped later.
- No database, Room, DataStore or JSON library.
- A missing key reads as 0 and an unknown key is ignored, so adding a stat needs no migration. The version tag changes only when a key changes meaning or the line structure changes.
- A new stat cannot be backfilled: old runs show 0 and lifetime starts counting from the day it is added.

## Constraints

- Release APK stays within 1 MiB.
- The app declares zero permissions.
- No new dependencies.
- `core` stays Android-free.
- `SPEC.md` may list game over or stats as out of scope. Check it and edit it first.

## Out of scope

- Daily, weekly or other time-bucketed stats.
- Reshuffle or rescue boosters.
- Share, leaderboard, cloud sync.
- A database.
- Backfilling new stats into old runs.

## Open details for the spec

- How to detect "no valid move" cheaply, and whether specials count as valid moves (a special can always be swapped with a neighbour).
- What happens to a saved game that is already in a dead state when the app updates.
- How the active-time clock treats backgrounding and the process being killed.
- Which five stats the card shows.
