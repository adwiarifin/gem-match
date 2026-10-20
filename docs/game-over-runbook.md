# Runbook: checking game over on a phone

A dead board is far too rare to reach by playing, and the release build has no debug switch. So this check loads a save that is one move from the end into the debug build. The save comes from `NearDeadFixtureTest`, which fails if the board stops being one move from dead. Needs `adb` and the phone on USB debugging.

## 1. Install the debug build

```
./gradlew :app:installDebug
```

The debug build is debuggable, which `run-as` below needs. Do not do this on the release build.

## 2. Load the near-dead save

```
PKG=dev.kesatria.gemmatch
START=$(($(date +%s) * 1000))
HEAD='GM3;12430;6923093808337748099;2-5-4-1-3-4-0-1-5-3-2-3-5-6-5-6-4-6-6-4-0-1-0-1-2-3-5-3-4-5-4-6-1-4-1-5-2-1-5-3-4-6-4-6-2-2-6-1-2-3-0-5-1-5-1-4-5-1-2-0-6-0-4-0-;'
STATS='moves=184,cascade=5,moveScore=450,gems=970,match=5,cB=10,cR=22,cC=18,cS=1,cW=1,tB=10,tR=22,tC=18,tS=1,tW=1'
adb shell am force-stop $PKG
printf '%s%s,started=%s,active=600000,end=1' "$HEAD" "$STATS" "$START" | adb shell "run-as $PKG sh -c 'cat > files/game.sav'"
adb shell "run-as $PKG rm -f files/runs.txt files/lifetime.txt"
adb shell am start -n $PKG/.MainActivity
```

The last two lines of the stats part give the run a start time of now and 10 minutes of active time, so the card reads sensibly. `rm` gives the history a clean start; skip it to keep what is there.

## 3. Make the last move

The board is 8 by 8, rows and columns counted from 0 at the top left. Swipe the gem at **row 4, column 1** down one cell, to **row 5, column 1**. It is the only move on the board.

Check:
- [ ] The match plays out: clear, fall, spawn, as in any move.
- [ ] After the last animation the dead board stays up for about half a second, with no card.
- [ ] The card fades in over the dimmed board: "Game over", the score (above 12,430), "New best!", then the stats list, then **Play again** and **Stats** pinned at the bottom.
- [ ] Touching the board behind the card does nothing.
- [ ] The list scrolls by dragging, and a drag never presses a button.
- [ ] Moves reads 185, Active time reads a little over 10:00, Total time is a little longer.

## 4. Files

```
adb shell "run-as $PKG sh -c 'cat files/runs.txt; echo; cat files/lifetime.txt; echo; cat files/game.sav'"
```

- [ ] `runs.txt` has one `RN1;...` line, `lifetime.txt` one `LT1;...` line with `runs=1`.
- [ ] `game.sav` is a different, fresh `GM3` game (low score, few moves), not the dead board.

## 5. Kill on the card

With the card showing: swipe the app away from recents, then open it again.

- [ ] A new game, not the card.
- [ ] `runs.txt` and `lifetime.txt` are unchanged (still one run).

## 6. Play again, Stats

Repeat step 2 and 3 to reach the card again, then:

- [ ] **Play again** starts a new game at once, with no question.
- [ ] Reach the card again and tap **Stats**: the lifetime block shows 2 runs played, and "Last runs" lists 2 rows, newest first. **Close** returns to the card. Back also closes it.
- [ ] On the game screen the **Stats** button next to **New game** opens the same screen.

## 7. Wall time against active time

Reach the card as above, but before the last move press Home and wait a minute, then come back and finish.

- [ ] Total time is about a minute longer than Active time.

## 8. A long list

To see 100 rows, write a `runs.txt` of 100 lines:

```
for i in $(seq 1 100); do echo "RN1;score=$((i*100)),moves=$i,wall=60000,active=45000,started=$i,ended=$((i+60000))"; done | adb shell "run-as $PKG sh -c 'cat > files/runs.txt'"
```

Open Stats from the game screen.

- [ ] The list scrolls smoothly to the end and shows 100 rows under "Last runs".
- [ ] Frame time stays smooth while scrolling.
