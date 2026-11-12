# 2D Cube Puzzle

The whole Rubik's Cube in 2D — the dots diagram from the
["Rubik's Cube as Permutations"](https://www.instagram.com/reel/DeBrTWChFxy/) video,
as a playable puzzle. No 3D cube, no cube net: 54 coloured dots on 9 circles,
exactly like the video's diagram. Tap a circle and it turns like a cube move.

## Download

Get the ready-to-install APK from the
[latest release](https://github.com/Bhavesh-suthar-1-1-1/2d-cube-puzzle/releases/latest)
(`rings-2.2-signed.apk`, Android 8.0 and up, no permissions, fully offline).
Installs over earlier versions.

## How to play

- **Tap a circle** to turn it. Its 12 dots rotate 3 slots along the ring,
  the way the video's highlighted circle does. The turning circle goes white.
- **Forward / Prime** switches the tap direction (a move or its opposite).
- The **move names** build up in a line at the bottom, like the video's strip.
- **Scramble** shuffles the dots. **Level** picks how hard: 3, 6, 12 or 24
  turns (shown as Par). **Reset** goes back to solved.
- **Undo** takes back your last move. **Hint** shows the next move back
  toward solved and highlights its circle in yellow. Runs that used a hint
  don't count for the records.
- **Win** by getting every dot back to its colour group — the video's solved frame.
- Moves counter, timer from the first move, and best time / moves are saved
  on the phone — separately for each level. Rotating the phone doesn't lose
  the game.

## The board

- **54 dots** — the cube's 54 stickers, in the video's six colours.
- **9 circles** — one per cube move: `U D F B L R M E S` (6 face turns + 3 slices).
  Every dot sits on exactly 2 circles, so a turn moves dots between circles.
- The slot positions, the circles and the solved colours were measured from
  the video's own frames. One of the video's moves (`R'`) was checked against
  this board and reproduces it exactly.
- Small arrows sit on each circle's empty arc and show which way its dots
  will move. They flip when you switch to Prime.

## Build it yourself

- Open the project folder in Android Studio and run it, or build from the
  command line with a local Gradle 9.6.1: `gradle assembleDebug`
  (needs `local.properties` with `sdk.dir` pointing at your Android SDK).
- JDK 17, Android SDK with compileSdk 37, minSdk 26, targetSdk 33.
- The debug APK lands in `app/build/outputs/apk/debug/`.

## Versions

- **v2.2** — Undo, Hint (with the circle highlighted), Levels 3/6/12/24 with
  Par and per-level bests, game survives rotating the phone.
- **v2.1** — bigger board filling the screen, header up top, all controls in
  one line at the bottom, direction arrows on the empty arcs.
- **v2.0** — the dots diagram: 54 dots on 9 circles, tap a circle to turn it.
- **v1.0** — two overlapping rings only. Replaced by v2.0: it wasn't the whole puzzle.

Made with the offline Gradle recipe — no network needed for the build itself
once the dependencies are cached.
