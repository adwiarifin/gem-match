# Intent: Special Gems

Status: confirmed by the owner.

## Outcome

Four special gems for the Android match-3, created from matches and triggered by swapping with a neighbor.

- A run of 4 makes a blast gem that clears the 3x3 around it.
- A run of 5 makes a colour gem that clears every gem of the colour it is swapped with.
- An L-shape makes a line gem that clears one row or column, along the L's longer arm.
- A T-shape (and +) makes a cross gem that clears its row and column.

## Triggering

A special gem stays on the board until the player swaps it with any neighbor. That swap is always legal, even when no match forms.

## Combos

- Colour + colour clears the board.
- Colour + another special converts every gem of that colour into that special and fires them all.
- Any other pair fires both effects at once.
- No bespoke pair effects. They can be added later if play feels flat.

## User

The owner, playing on a Samsung Galaxy A52.

## Why now

After several play sessions the game feels flat. The owner wants more variety in moves, fewer stuck states, and a "use it now or save it for later" decision.

## Success

- The owner can feel the now-or-later choice in play.
- Explosions read clearly, in line with the preference for visible transitions.
- Saved games keep their special gems across pause and resume.

## Constraints

- The core stays pure Kotlin with no Android imports.
- The APK stays tiny and the game stays fully offline.
- `SPEC.md` Out of Scope currently excludes special gems, so it must be edited first.
- The save format needs a bump (GM1 to GM2, with a GM1 reader).

## Out of scope

- Any change to high-score persistence. The reset on install is most likely caused by mixing differently signed APKs, and the owner will observe it first.
- Matches of six or more, and any extra gem types.
- Bespoke effects for specific pairs of special gems.
- Sound, haptics and the other ideas in `docs/ideas.md`.

## Open detail for the spec

An L with equal arms has no longer arm. Default: clear the row.
