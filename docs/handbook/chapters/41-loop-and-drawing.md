# 41. Loop, Time and Drawing

A real-time game is a loop: read the input, advance the rules, draw a frame, again and again. This chapter covers the parts of the engine that keep that loop going and bring its picture to the screen: the loop and its fixed steps, the clocks for rules that need a pace or a countdown of their own, the canvas, full screen and the measuring of frames. At the end comes the other way to animate, for games that wait for the player.

## The game loop: GameLoop and FixedStep

*Package `loop` · introduced in [20. Paddle](20-paddle.md#step-1-the-game-loop) · source: [`GameLoop`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/loop/GameLoop.java), [`FixedStep`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/loop/FixedStep.java)*

**What it is for.** Every game from Paddle to Sands runs on a `GameLoop`: a thread of its own that runs commands posted from Swing, advances the rules in **fixed steps**, usually 120 a second, and draws a frame, at most 60 a second. You give it two functions: `update(seconds)`, which advances the rules by exactly one step, and `render(alpha)`, which draws.

**When to choose it.** For any game in which something moves while the player does nothing. A game that only reacts to clicks (Mines, Solitaire) needs no loop: Model-View-Presenter and Swing's own painting fit it better, with an [`Animator`](#animation-on-the-event-thread-animator) for the few moments something moves.

**How it works.** Real time comes in uneven pieces: one frame took 15 ms, the next 18. The rules want even steps, because a ball that moves 18/15 as far in one step as in another bounces differently, and because tests must be able to repeat a game step for step. `FixedStep` is the arithmetic between the two: it collects the elapsed time, says how many whole steps are due, and keeps the rest for the next frame.

```java title="amade-game-engine · loop/FixedStep.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/loop/FixedStep.java:fixed-step"
```

The rest of a step is `alpha()`, 0 to 1: how far the time is between the last step and the next. `render(alpha)` gets it, so the drawing can place a moving thing between its last two positions and the movement looks smooth at any frame rate ([drawing between steps](20-paddle.md#drawing-between-steps)). After a long pause (a debugger, a suspended laptop) the loop runs at most 8 steps and forgets the rest, instead of racing through minutes of game.

The loop itself is short. Commands come first, so a menu's *New Game* takes effect before the next step; an exception in a frame is logged and the loop goes on; the rest of the frame is slept away.

```java title="amade-game-engine · loop/GameLoop.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/loop/GameLoop.java:run"
```

| Method | What it does |
|---|---|
| `new GameLoop(updatesPerSecond, framesPerSecond, update, render)` | The loop; 120 and 60 in every game of this book |
| `start()`, `stop()`, `running()` | Starts the thread, stops it and waits for it to end |
| `post(command)` | Runs `command` on the loop thread before the next step; from any thread |
| `FixedStep.advance(elapsedNanos)`, `alpha()`, `seconds()` | The arithmetic, usable without the loop in tests |

!!! warning "Pitfall: touching the game from Swing"
    Everything the rules own belongs to the loop thread. A menu action that changes the game directly races with the step that is running. Wrap it in `loop.post(...)`. The input classes need no posting: they are made to be written by Swing and read by the loop ([chapter 20](20-paddle.md#step-7-two-threads-one-game)).

??? example "Test: a fixed step in uneven frames"
    ```java title="amade-game-engine · test/loop/FixedStepTest.java"
    --8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/loop/FixedStepTest.java:test-fixed-step"
    ```

## A pace of its own: Ticker

*Package `loop` · introduced in [22. Snake](22-snake.md#step-2-a-pace-of-its-own) · source: [`Ticker`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/loop/Ticker.java)*

**What it is for.** Rules that move in ticks slower than the update step: the snake that moves one cell every 150 ms, the walls that fall one by one at the end of a round of Blast, the heartbeat of Rocks that beats faster the longer a wave lasts. The loop still steps 120 times a second, for input and effects; the ticker counts the steps' time and says when a tick is due.

**When to choose it.** For something that happens **regularly**, at a pace that may change. For something that happens **once**, after a while, use [`Timers`](#many-countdowns-timers); for things that move smoothly, the update step itself.

**How it works.** `advance(seconds)` adds the time and returns how many ticks are due, usually 0 or 1, and keeps the remainder. A nanosecond of tolerance makes 120 steps of 1/120 s count as the full second that floating point misses by 1e-15. `setInterval` changes the pace from the next tick on, keeping the time since the last one, so a snake that speeds up does not jump. `progress(alpha * seconds)` says how far the time is between two ticks, for drawing a snake that glides from cell to cell ([chapter 22](22-snake.md#drawing-between-ticks)).

```java title="amade-game-engine · loop/Ticker.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/loop/Ticker.java:ticker"
```

## Many countdowns: Timers

*Package `time` · introduced in [26. Blast](26-blast.md#step-4-timers) · source: [`Timers`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/time/Timers.java)*

**What it is for.** Many countdowns at once, each belonging to a key: the fuse of every bomb, the flames on every tile, the time a power-up blinks. `start(key, seconds)` starts or restarts one, `update(seconds, expired)` lets time pass and hands over the keys whose time ran out.

**When to choose it.** When things end after a while and **the order of their ending matters**. Two bombs whose fuses end within the same step must explode in the order they would in real time, or a chain reaction comes out differently. A field `double fuse` in each bomb, counted down in a loop over the bombs, hands them over in the order of the loop instead.

**How it works.** All timers lose the step's seconds; those at or below zero are sorted by how far below zero they went, which is how early in the step they ran out, and on a tie by the order they were started. Only then are they removed and handed over, so a handler that starts a new timer starts it for the next step.

```java title="amade-game-engine · time/Timers.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/time/Timers.java:timers"
```

Besides `start` and `update` there are `running(key)`, `left(key)`, `cancel(key)`, `keys()` (in the order they were first started), `size()` and `clear()`.

??? example "Test: timers in the order they run out"
    ```java title="amade-game-engine · test/time/TimersTest.java"
    --8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/time/TimersTest.java:test-timers"
    ```

## The time of a game: GameClock

*Package `time` · introduced in [10. Mines](10-mines.md#time-you-can-test), moved into the engine with [11. Solitaire](11-solitaire.md#step-11-what-moved-into-the-engine) · source: [`GameClock`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/time/GameClock.java)*

**What it is for.** The time one game took, as a high score records it: it starts with the first move (`start()` only counts the first call), stops when the game ends, and shows whole seconds the way a player expects, a started second counting as one.

**When to choose it.** For games that wait for the player, in which real time passes between the moves. A real-time game measures its time in steps instead: the step count times the step length is exact and repeats in a replay, the wall clock does not.

**How it works.** The clock reads the time from a `LongSupplier` of nanoseconds: `System::nanoTime` in the game, a number the test moves forward in tests, so a test can win a game of Mines in 4.2 seconds without waiting ([chapter 10](10-mines.md#time-you-can-test)).

## The seasons of a year: Seasons

*Package `time` · introduced in [32. Colony](32-colony.md#seasons) · source: [`Seasons`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/time/Seasons.java)*

**What it is for.** A year of seasons, each as long as the game wants: Colony's mission 3 has a long winter of 170 seconds and a short spring. It answers which season it is, which year, how far the season is, and, after each step, how many seasons began in it, so that "Winter" is said exactly once.

**When to choose it.** For any game time that goes round: seasons, day and night, the shifts of a factory, the tides. It only counts; what a season means (no growing, snow, more fires) is the game's.

**How it works.** The only state is the time since the start, so a save file keeps one number and `restore` sets it. Season and year are worked out from it every time they are asked: whole years first, then the seasons of the year begun. `update` compares the number of seasons begun before and after the step; a step longer than a season, as when a test runs fast, reports all of them.

```java title="amade-game-engine · time/Seasons.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/time/Seasons.java:seasons"
```

| Method | What it does |
|---|---|
| `new Seasons(150, 150, 150, 90)` | Four seasons of these lengths, in seconds |
| `update(seconds)` | Lets time pass; how many seasons began |
| `season()`, `year()` | The current season from 0, the current year from 0 |
| `progress()`, `left()` | How far the season is, 0 to 1; the seconds left in it |
| `count()`, `length(season)` | The number of seasons, the length of one |
| `time()`, `restore(time)` | The whole state, for a save file |

## Which clock?

| The question | Use |
|---|---|
| How far did the ball move in this step? | The `seconds` of `update`, from `GameLoop` |
| Is it time for the snake's next cell? | `Ticker` |
| Which bombs explode now, and in which order? | `Timers` |
| How long has this ghost been frightened? | `StateMachine.seconds()` ([chapter 46](46-paths-and-decisions.md#one-state-at-a-time-statemachine)) |
| How long did the player need for this game of Mines? | `GameClock` |
| Is it winter yet? | `Seasons` |
| How far along is this card's flight? | `Tween` ([chapter 42](42-sprites.md#movement-in-time-easing-and-tween)) |

## The canvas: GameCanvas and RenderMode

*Package `swing` · introduced in [20. Paddle](20-paddle.md#step-2-drawing-actively), render modes and the overlay in [21. Bricks](21-bricks.md#pixel-mode) · source: [`GameCanvas`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/swing/GameCanvas.java), [`RenderMode`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/swing/RenderMode.java)*

**What it is for.** The drawing surface of every real-time game. The game draws in fixed **logical coordinates**, 640 × 400 in most games, as if the screen always had that size; the canvas scales the picture to its real size, keeps the proportions and fills the rest with black bars. Drawing is **active**: the game loop calls `render(alpha)` whenever it wants a frame, and the canvas draws into a hidden buffer of a `BufferStrategy` and shows it at once by copying it onto the screen, never by flipping, which can leave the screen black in full screen on X11 ([appendix](appendix-java2d.md#buffers)). Swing's `repaint()` is not involved, so nothing waits for the event thread.

**When to choose it.** For a game with a `GameLoop`. A game that waits for the player draws in a plain `JComponent` with `paintComponent`, as Mines and Solitaire do.

**How it works.** A frame has two layers, drawn by two painters you give the canvas: the **world** (field, sprites, particles) and the **overlay** (score, texts, menus), both in the same logical coordinates. How the world reaches the screen is the `RenderMode`:

```java title="amade-game-engine · swing/RenderMode.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/swing/RenderMode.java:modes"
```

In `DIRECT` mode, world and overlay are drawn through one scaling transform, with antialiasing on. In `PIXEL` mode, the world is drawn into a `VolatileImage` of the logical size, with antialiasing off and hard interpolation, and that image is enlarged by the largest **whole** factor that fits (`wholeScale`), so every logical pixel becomes a square block. The overlay is then drawn straight onto the screen at the same place and scale, so the score stays sharp over a world of big pixels ([chapter 21](21-bricks.md#two-layers-the-world-and-the-overlay)).

Both the screen's buffers and the pixel-mode buffer live in video memory where there is one and can be lost; the canvas draws again until they are not, and calls `Toolkit.sync()` after every frame so X11 does not deliver frames in bursts ([appendix](appendix-java2d.md#buffers)).

```java title="amade-game-engine · swing/GameCanvas.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/swing/GameCanvas.java:render"
```

| Method | What it does |
|---|---|
| `new GameCanvas(width, height, painter)` | A canvas whose painter draws everything |
| `new GameCanvas(width, height, painter, overlay)` | World and overlay painted separately |
| `render(alpha)` | Draws one frame; the loop's render function |
| `setRenderMode(mode)`, `renderMode()` | `DIRECT` or `PIXEL`, from the next frame on; from any thread |
| `toLogical(x, y)` | A point of the component, such as a mouse event's, in logical coordinates, placed as the current mode places the picture |
| `setMaxLogicalWidth(max)` | Lets the picture widen up to `max` units to fill a wide screen; from the next frame on |
| `logicalWidth()`, `minLogicalWidth()` | The width of the current frame, and the width the canvas was made with |
| `widthFor(width, height, min, max, logicalHeight, mode)` | The logical width for a screen of that many pixels |
| `setShowFrameTimes(show)`, `frameTimes()` | The frame-time display, and its numbers |
| `withoutFrames(change)` | Runs a change that takes the canvas off the screen for a moment, with no frame drawn meanwhile |

!!! warning "Pitfall: a heavyweight in a Swing window"
    The canvas is an AWT `Canvas`, a native window inside the Swing window. Lightweight popup menus open behind it unless `JPopupMenu.setDefaultLightWeightPopupEnabled(false)`, and Swing's menu shortcuts never see keys the canvas receives. The canvas therefore hands every key the game does not consume on to Swing's key bindings ([chapter 20](20-paddle.md#a-heavyweight-in-a-swing-window)).

**Wide screens.** The canvas keeps the proportions of its logical size, so a 640 × 400 picture has black bars on a 16:9 screen. A game whose world is larger than the screen calls `setMaxLogicalWidth(max)`: the height stays, and the logical width grows with the screen, up to `max`, until the picture fills it. The game asks `logicalWidth()` in each update step and lays out its view and panels from it; the width is set at the start of each frame on the loop thread, so the step and the frame after it agree, and `toLogical` uses the same width for the mouse ([chapter 30](30-towers.md#a-wider-screen-a-wider-view)). Games on a fixed field (Paddle to Jump) keep their bars.

```java title="amade-game-engine · swing/GameCanvas.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/swing/GameCanvas.java:width-for"
```

**Which mode?** `DIRECT` is the default and sharp at every size. `PIXEL` costs 640 × 400 pixels on any screen, which makes it the fast choice for very large screens and slow drivers, and gives the look of old games. Every game offers both in its *Graphics* menu ([chapter 48](48-scenes-menus-sound.md#the-standard-menus-gamemenus-and-gametexts)) and keeps the choice in its settings.

??? example "Test: the mouse where pixel mode puts the picture"
    ```java title="amade-game-engine · test/swing/GameCanvasTest.java"
    --8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/swing/GameCanvasTest.java:test-to-logical"
    ```

??? example "Test: the width on wide screens"
    ```java title="amade-game-engine · test/swing/GameCanvasTest.java"
    --8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/swing/GameCanvasTest.java:test-width-for"
    ```

## Full screen: FullScreen

*Package `swing` · introduced in [20. Paddle](20-paddle.md#full-screen) · source: [`FullScreen`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/swing/FullScreen.java)*

**What it is for.** Switching the game window between a normal window and full screen, with F11 from `GameMenus.fullScreen`. In full screen the window has no frame and no menu bar, and the menu shortcuts keep working.

**How it works.** `enter()` uses `GraphicsDevice.setFullScreenWindow` on the monitor the window is on, but never changes the screen's resolution: the canvas scales the picture itself. Where the platform has no full-screen mode, the window simply covers the monitor. A frame can only lose its frame while it is not displayable, so the switch disposes the window and shows it again, inside `canvas.withoutFrames` so the loop draws nothing into a window that is gone. A hidden menu bar ignores its shortcuts; while it is hidden, `FullScreen` copies each item's accelerator to the root pane, which clicks the item.

```java title="amade-game-engine · swing/FullScreen.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/swing/FullScreen.java:enter"
```

!!! warning "Pitfall: the game ends on F11"
    Disposing the window sends `windowClosed`. A game that stops its loop in a window listener of its own ends when the player presses F11. End the game with `fullScreen.whenClosed(action)`, which runs only when the window is really closed ([chapter 20](20-paddle.md#full-screen)).

!!! warning "Pitfall: the game pauses on F11"
    Disposing the window also takes the focus away for a moment. A game that pauses in a `windowDeactivated` listener of its own pauses whenever the player switches. Pause with `fullScreen.whenDeactivated(action)`: it runs at once when the focus goes to another program, and after a switch only if the window has not got the focus back within a second ([chapter 20](20-paddle.md#full-screen)).

    ```java title="amade-game-engine · swing/FullScreen.java"
    --8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/swing/FullScreen.java:deactivated"
    ```

## Frame times: FrameTimes

*Package `loop` · introduced in [21. Bricks](21-bricks.md#a-display-for-frame-times) · source: [`FrameTimes`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/loop/FrameTimes.java)*

**What it is for.** The numbers behind the frame-time display that every game shows with F12: how long the frames of the last second took to draw, on average and at worst, and how many there were. At 60 frames a second a frame has 16.7 ms; the display turns red when the worst frame took longer.

**When to use it.** Before making a game faster. Every performance chapter of this book started with these numbers and kept a change only when they improved: the sprite cache in [Bricks](21-bricks.md#measuring), the antialiasing in [Swarm](24-swarm.md#a-black-rectangle), the opaque caches in [Jump](27-jump.md#measured-then-halved), the panel in [Sands](31-sands.md#the-panel-and-a-frame-time-measured). The rules for drawing fast are collected in the [appendix](appendix-java2d.md).

**How it works.** The canvas records the start and end of every frame in a ring of 1,000 entries and drops those older than a second. The methods are `synchronized`, because the loop records and the display may read from elsewhere.

## Animation on the event thread: Animator

*Package `swing` · introduced in [11. Solitaire](11-solitaire.md#animator-frames-on-the-event-thread) · source: [`Animator`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/swing/Animator.java)*

**What it is for.** Movement in a game that has no game loop: a card that flies to its pile, the cards that bounce after a won game of Solitaire. The animator calls a frame function about 60 times a second on the event thread, with the current `System.nanoTime()`, for as long as the function says there is more to show.

**When to choose it.** In a Swing component of a game that waits for the player, where something moves for a moment and then everything rests again. It costs nothing while nothing moves. A game in which something always moves wants a [`GameLoop`](#the-game-loop-gameloop-and-fixedstep).

**How it works.** A Swing `Timer` of 16 ms, coalescing, with `Toolkit.sync()` after each frame. `start()` while it runs does nothing, so every change can simply call it. The positions themselves come from [`Tween`s](42-sprites.md#movement-in-time-easing-and-tween): the frame function asks each tween how far it is at `now`, repaints, and returns whether any is still running ([chapter 11](11-solitaire.md#one-idea-animates-everything)).
