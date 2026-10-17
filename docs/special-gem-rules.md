# Special gem rules

One page with every rule about special gems, so a rule can be changed in one place. `SPEC.md` says what to build and why; `docs/decisions.md` records the choices. Last updated 2026-10-17, after the owner's playtest feedback and the multi-perspective review (D16 to D18).

## The six specials

| Special | Made by | Gems cleared when made | Effect when it fires |
|---|---|---|---|
| Blast | Straight run of 4 | 4 | The 3x3 around it |
| Line gem | Short L: a run of 3 with one gem hanging off an end | 4 | Its row (horizontal run) or its column (vertical run) |
| Short X | Short T: two runs of exactly 3 crossing inside one of them (a bar of 3 with a stem of 2, or a 3 by 3 `+`) | 5 | Both diagonals through it, 2 cells out (9 cells) |
| Cross gem | L: two runs crossing at the end of both, each 3 or more | 5 or more | Its row and its column |
| Full X | Long T: two runs crossing inside one of them, with a run of 4 or more | 6 or more | Both diagonals through it, across the whole board |
| Colour gem | Straight run of 5 or more | 5 or more | Every gem of its partner's colour (see combos) |

Looks: blast an 8-point burst; line gem a stripe along its row or column; short X a small X; cross gem a plus; full X a larger X inside a ring; colour gem a wheel of the seven gem colours with a white edge. Every special has a slow shimmer. White gems carry a dark mark so it stays visible.

## Making a special

- A group is the runs that share a cell. Each group makes at most one special.
- **Priority:** a group of two or more crossing runs beats everything, even a run of 5 inside it (an L with a long arm is still a cross gem, a T with a bar of 5 is a full X). Otherwise: a run of 3 with a hanging gem is a line gem; a straight run of 5 or more is the colour gem; a straight run of 4 is a blast.
- **Hanging gem.** A run of exactly 3 takes one gem of the same colour that touches one of its two end cells at a right angle, if that gem is in no run itself and no other group took it. If several qualify, the first in board order (row, then column) is taken. A gem touching the middle is not taken: that shape stays a plain 3-match and the gem stays on the board. A run of 4 or more takes none.
- **L against T** for two crossing runs: the crossing cell is the end of both runs for an L, and inside at least one run for a T or +. A group of three or more runs is a T if any crossing is inside a run, otherwise an L. A T is short when every run is exactly 3 and long when any run is 4 or more. A run's length includes the corner cell, so the smallest L is 3 by 3.
- **Where it appears:** on the swapped cell that belongs to the group (the dragged one if both do). With no swapped cell (a cascade match, or a group the swap did not touch): at the gem the hanging gem touches, the crossing of an L, T or +, or the middle of a straight run.
- **Any depth:** matches in a cascade make specials the same way. A clear in which something fires makes no special at all, not even for a group that fired nothing.
- A made special is placed after the Clear and before gravity, so it falls like any gem.

## Firing

A special fires, from the cell it sits on, when:
1. **it is swapped** (a swap that touches a special is always legal, even with no match);
2. **it is part of a match** at any cascade depth: it counts in a run of its colour like a plain gem;
3. **another effect covers it** (a chain). Each special fires once per clear.

Order inside one clear: the swap's own effects, then matched specials in board order, then chained ones in the order they were reached. Every `Activate` of a clear comes before its single `Clear`. A colour gem that is only covered by an effect has no partner: it is cleared and fires nothing. The colour gem never counts in a run.

## Combos (two specials swapped together)

Each special fires its own effect from the cell it moved to. There are no bespoke pair effects except the colour gem's.

Cells cleared on an open board, both gems in the middle (pinned by `PairCombosTest`). "Along a row" means the swap is sideways, "along a column" up or down.

| Pair | Along a row | Along a column |
|---|---|---|
| Blast + Blast | 12 | 12 |
| Blast + Row line | 14 | 14 |
| Blast + Column line | 14 | 14 |
| Blast + Cross | 19 | 19 |
| Blast + Short X | 15 | 15 |
| Blast + Full X | 20 | 20 |
| Row line + Row line | 8 | 16 |
| Row line + Column line | 15 | 15 |
| Row line + Cross | 15 | 22 |
| Row line + Short X | 16 | 15 |
| Row line + Full X | 21 | 20 |
| Column line + Column line | 16 | 8 |
| Column line + Cross | 22 | 15 |
| Column line + Short X | 15 | 16 |
| Column line + Full X | 20 | 21 |
| Cross + Cross | 22 | 22 |
| Cross + Short X | 21 | 21 |
| Cross + Full X | 26 | 26 |
| Short X + Short X | 18 | 18 |
| Short X + Full X | 23 | 23 |
| Full X + Full X | 28 | 28 |

Edges and corners clip every effect to the board. Any other special inside the cleared cells fires too.

**Colour gem combos**

| Pair | Result |
|---|---|
| Colour + plain gem | Every gem of that gem's colour clears, and the colour gem |
| Colour + Colour | The whole board clears |
| Colour + any other special | Every gem of that special's colour becomes that special (lines keep the partner's direction) and all of them fire |

## Scoring

- Runs: 3 gems 30, 4 gems 60, 5 or more 100. An L, T or + group adds a flat 50. A run of 3 with a hanging gem scores 60 and no shape bonus.
- Every gem cleared by an effect scores 10 (`POINTS_PER_EFFECT_GEM`). A cell cleared by both a run and an effect scores once. Making a special adds nothing.
- Everything is multiplied by the cascade depth (1 for the player's own match).
- Colour + Colour at depth 1 is 640.

## Save format

`GM2`: two characters per cell, the gem digit (`-` for the colour gem) then the special: `-` none, `B` blast, `R` line along the row, `C` line along the column, `X` cross, `S` short X, `F` full X, `W` colour. `GM1` saves still load.

## Where each rule lives

| Rule | Code |
|---|---|
| Runs, groups, the hanging gem | `Matches.kt` |
| Which special a group makes, where it goes | `Creation.kt` |
| What each special clears, firing order, chains | `Activation.kt` |
| Colour gem combos | `Combo.kt` |
| Turning one move into steps (swap, fire, clear, create, fall, spawn, cascade) | `Resolution.kt` |
| Points | `Scoring.kt` |
| Save letters | `SaveCodec.kt` |
| Overlays and shimmer | `SpecialOverlays.kt` (app) |
| Effect animations (a clear's effects overlap, a tap skips to the end of the move) | `Effects.kt`, `ActivateAnimation.kt`, `Timeline.kt` (app) |

To change a rule: edit the code above, then update this page. `PairCombosTest` fails when a pair's cell count moves, `CreationTest` when a shape makes a different special, and `SpecialScoringTest` when a score changes.
