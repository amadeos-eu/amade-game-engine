# 40. The Engine at a Glance

The game chapters told the engine's story in the order the games needed it: high scores with Mines, the game loop with Paddle, flow fields with Towers. That is the right order to learn it in, and the wrong order to look something up in. Part IV is the manual: the same engine, sorted by what it is for.

Each chapter of this part covers one area. Each section in it describes one feature: what it is for, when to choose it and when something else fits better, how it works inside, and its methods. A line at the top of each section names the package, the chapter that introduced the feature, and the source files on GitHub, so you can go from the manual to the story and to the code. The tests of the engine are its best examples; most sections show one.

| Chapter | Packages | For |
|---|---|---|
| [41. Loop, Time and Drawing](41-loop-and-drawing.md) | `loop`, `time`, `swing` | The game loop, rules with a pace of their own, countdowns, the canvas, full screen, frame times |
| [42. Sprites and Effects](42-sprites.md) | `sprite`, `shape`, `anim`, `fx` | Pictures that copy fast, sheets and animation, turned pictures, figures of lines, tweens, particles |
| [43. Input](43-input.md) | `input` | Keyboard, actions instead of keys, mouse, gamepads |
| [44. Movement, Physics and Collision](44-movement.md) | `math`, `motion`, `physics`, `collision`, `pool`, `entity` | Vectors and angles, flight paths, falling and jumping, what hits what, many things at once |
| [45. Maps, Camera and Fog](45-maps.md) | `tile`, `tiled`, `camera`, `fog`, `minimap` | Tile maps, maps from an editor, worlds larger than the screen, what a side sees |
| [46. Paths, Groups and Decisions](46-paths-and-decisions.md) | `path`, `select`, `ai`, `event` | Finding the way for one or for many, choosing units, states, the computer's view of the map, chain reactions |
| [47. Chance, Data and Saving](47-data-and-saving.md) | `random`, `replay`, `undo`, `data`, `economy`, `save`, `score` | Repeatable chance, replays, undo, data files, money and production, saving, high scores |
| [48. Scenes, Menus and Sound](48-scenes-menus-sound.md) | `scene`, `swing`, `menu`, `audio` | Screens of a game, the standard menus and dialogs, menus over the game, sound and music |

## What the engine is, and is not

The engine is a library, not a framework. It never calls your game unless you hand it something to call, it has no base class to extend and no main loop you must fit into, except the `GameLoop` if you want one. It knows nothing about bricks, ghosts or crystal: every class in it is used by at least one game, and none of them knows which.

It also has no editor, no scene graph and no physics engine in the usual sense. What it has is small classes, most of them a page or two long, that each solve one problem completely and are tested. A game takes the ones it needs and writes the rest itself.

## Ideas that run through the engine

A few rules hold for nearly every class. Knowing them makes the rest of the manual shorter.

**The rules never see Swing.** Everything the domain of a game uses (vectors, tile maps, paths, timers, the economy) is plain Java without `java.awt`, so the rules can be tested in milliseconds without a screen ([chapter 2](02-structure.md#domain-the-rules)). Only `swing`, `sprite`, `fx`, `shape`, `fog.FogPainter`, `minimap`, `menu` drawing and `audio` touch the screen or the sound card.

**No class reads the clock by itself.** `FixedStep`, `Ticker`, `Timers`, `Jump`, `Camera.follow`, `Production` and the others are told how many seconds passed. `GameClock`, `Keyboard` and `Tween` take their time from a function you pass, `System::nanoTime` in the game and a number the test moves in tests. So a test can play a minute of a game in a millisecond, and every second of it is the same each time.

**Logical coordinates.** A game draws as if the screen were always the same size, 640 × 400 units in most games of this book. The `GameCanvas` scales that picture to the window, and the `Mouse` reports positions in the same units. No rule, and no drawing code, ever sees the window's real size ([chapter 20](20-paddle.md#logical-coordinates)).

**Two threads, one owner.** Swing calls the game on its event thread; the game loop runs the rules and the drawing on a thread of its own. The input classes stand between them: the event thread reports, the game loop takes over the state with `poll()` once per step. Anything else that must cross from Swing to the game goes through `GameLoop.post`. Everything the rules touch then belongs to the loop thread alone and needs no locks ([chapter 20](20-paddle.md#step-7-two-threads-one-game)).

**Draw between two steps.** The rules move in fixed steps, the screen shows frames whenever it can. Things that move keep their previous position (`Body`, `Walker`, `Camera`, `Particles`), and the drawing places them `alpha` of the way from the last step to the current one ([chapter 20](20-paddle.md#drawing-between-steps)).

**The same game every time.** Chance comes from a `SeededRandom` with a seed, ties are broken by a fixed order (`Direction`, the order things were made or started in), and events are handled first in, first out. With the same seed and the same input, a game runs the same way on every computer: that is what makes replays, tests of whole games and loaded saves work ([chapter 22](22-snake.md#step-4-chance-that-repeats)).

**No garbage in the frame loop.** Classes that run hundreds of times per frame (`Particles`, `Pool`, `Grid`, `Outline`, `Minimap`, `FogPainter`) make their objects once and reuse them. The garbage collector then has nothing to do while the game runs, and never pauses it ([chapter 21](21-bricks.md#step-5-particles)).

**Errors name the place.** Data written by people is read with errors that say where the mistake is: `waves.txt:12: "swoop-lef" is not a path`. `TextData`, `Catalog`, `TileMap.read`, `TiledMap` and `Snapshot` all do this ([chapter 24](24-swarm.md#step-4-stages-as-data)).

**Use it on one thread.** Unless a class says otherwise, it is not thread-safe and belongs to the game loop. The classes that cross threads say so: `Keyboard`, `Mouse`, `Gamepad`, `FrameTimes`, `GameLoop.post`, `Mixer.play` and `GameCanvas.setRenderMode`.

## I need … : use …

| I need | Use | Section |
|---|---|---|
| Rules that run 120 times a second, frames as fast as the screen allows | `GameLoop` | [41](41-loop-and-drawing.md#the-game-loop-gameloop-and-fixedstep) |
| Rules that move in ticks of their own (a snake's cell, a falling row) | `Ticker` | [41](41-loop-and-drawing.md#a-pace-of-its-own-ticker) |
| Many countdowns: fuses, cooldowns, build times | `Timers` | [41](41-loop-and-drawing.md#many-countdowns-timers) |
| The time of one game, for a high score | `GameClock` | [41](41-loop-and-drawing.md#the-time-of-a-game-gameclock) |
| A drawing surface in fixed logical coordinates | `GameCanvas` | [41](41-loop-and-drawing.md#the-canvas-gamecanvas-and-rendermode) |
| Animation in a Swing component of a game that waits for the player | `Animator`, `Tween` | [41](41-loop-and-drawing.md#animation-on-the-event-thread-animator), [42](42-sprites.md#movement-in-time-easing-and-tween) |
| A picture from a file or drawn in code, fast at any size | `Sprite` | [42](42-sprites.md#pictures-sprite) |
| Many frames in one image, and which one to show | `SpriteSheet`, `Animation` | [42](42-sprites.md#many-pictures-in-one-spritesheet), [42](42-sprites.md#which-frame-when-animation) |
| A picture that turns | `Rotations` or `Outline` | [42](42-sprites.md#turning-pictures-or-lines) |
| Splinters, sparks, dust | `Particles` | [42](42-sprites.md#splinters-and-sparks-particles) |
| "Does the player move up?" from keys or a pad | `Controls` | [43](43-input.md#actions-instead-of-keys-controls) |
| The mouse in game units | `Mouse` | [43](43-input.md#the-mouse-mouse) |
| Gamepads on Linux and Windows | `Gamepads`, `Gamepad` | [43](43-input.md#gamepads-gamepads-and-gamepad) |
| Things that fly at an angle | `Vector`, `Angles` | [44](44-movement.md#points-and-directions-vector-and-angles) |
| A field without edges | `Wrap` | [44](44-movement.md#a-field-without-edges-wrap) |
| A smooth curve to fly along at an even speed | `FlightPath` | [44](44-movement.md#curves-to-fly-along-flightpath) |
| Falling, standing, jumping on tiles | `Body`, `TileCollider`, `Jump` | [44](44-movement.md#falling-and-standing-body-and-tilecollider) |
| Collision of fast boxes, of circles, of many things | `Sweep`, `Circle`, `Grid` | [44](44-movement.md#which-collision-test) |
| Shots with a limit, without garbage | `Pool` | [44](44-movement.md#a-fixed-number-of-things-pool) |
| Things whose behaviour changes while they live | `Entity`, `Entities` | [44](44-movement.md#entities-and-components) |
| A level on a grid, written as text | `TileMap` | [45](45-maps.md#a-grid-of-tiles-tilemap-cell-and-direction) |
| Walking corridors from tile centre to tile centre | `Walker` | [45](45-maps.md#walking-corridors-walker) |
| Levels drawn in the Tiled editor | `TiledMap` | [45](45-maps.md#maps-from-an-editor-tiledmap) |
| A world larger than the screen | `Camera`, `Parallax` | [45](45-maps.md#a-world-larger-than-the-screen-camera) |
| What a side has seen and sees now | `FogOfWar`, `FogPainter` | [45](45-maps.md#what-a-side-sees-fogofwar-and-fogpainter) |
| The whole map in a corner | `Minimap` | [45](45-maps.md#the-map-in-a-corner-minimap) |
| The way for one, or for many to one goal | `AStar`, `DistanceMap`, `FlowField` | [46](46-paths-and-decisions.md#which-search) |
| Groups that arrive side by side | `Formation`, `FieldCache` | [46](46-paths-and-decisions.md#groups-on-the-move-formation) |
| Click and box selection, groups on number keys | `Selection`, `BoxSelect` | [46](46-paths-and-decisions.md#choosing-units-selection-and-boxselect) |
| A character with modes | `StateMachine` | [46](46-paths-and-decisions.md#one-state-at-a-time-statemachine) |
| Where a side is strong or weak | `InfluenceMap` | [46](46-paths-and-decisions.md#strength-on-the-map-influencemap) |
| Chain reactions | `EventQueue` | [46](46-paths-and-decisions.md#events-that-cause-events-eventqueue) |
| Chance that repeats | `SeededRandom` | [47](47-data-and-saving.md#chance-that-repeats-seededrandom) |
| Replays, undo | `Replay`, `History` | [47](47-data-and-saving.md#a-game-as-its-input-replay), [47](47-data-and-saving.md#undo-and-redo-history) |
| Levels, waves, units as text files | `TextData`, `Catalog` | [47](47-data-and-saving.md#data-files-textdata), [47](47-data-and-saving.md#things-with-properties-catalog) |
| Money and things that take time to make | `Credits`, `Production` | [47](47-data-and-saving.md#money-and-production-credits-and-production) |
| Progress, or a whole game, on disk | `SaveFile`, `Snapshot` | [47](47-data-and-saving.md#progress-on-disk-savefile) |
| High scores | `HighScores` | [47](47-data-and-saving.md#high-scores-highscores) |
| Title, match, pause and end screens | `Scene`, `SceneStack` | [48](48-scenes-menus-sound.md#screens-scene-and-scenestack) |
| New Game, Full Screen, Exit with the usual keys | `GameMenus` | [48](48-scenes-menus-sound.md#the-standard-menus-gamemenus-and-gametexts) |
| A menu on the map, beside what it is about | `OverlayMenu` | [48](48-scenes-menus-sound.md#menus-over-the-game-overlaymenu) |
| Sound effects and music | `Sound`, `Tune`, `Mixer` | [48](48-scenes-menus-sound.md#sound-effects-sound) |

## The packages by chapter

The order in which the engine grew, and where each part is explained step by step:

| Chapter | Game | Packages and classes it brought |
|---|---|---|
| [10](10-mines.md#what-moved-into-the-engine) | Mines | `score` (`HighScores`, `Score`, `Ranking`, `PlayerName`), `swing.GameMenus`, `swing.HighScoreDialog`, `swing.GameTexts` |
| [11](11-solitaire.md#step-11-what-moved-into-the-engine) | Solitaire | `undo.History`, `anim` (`Easing`, `Tween`), `swing.Animator`, `time.GameClock` |
| [20](20-paddle.md#what-moved-into-the-engine) | Paddle | `loop` (`GameLoop`, `FixedStep`), `swing.GameCanvas`, `swing.FullScreen`, `input` (`KeyState`, `Keyboard`, `Controls`), `scene`, `audio` (`Sound`, `Mixer`) |
| [21](21-bricks.md#what-moved-into-the-engine) | Bricks | `collision` (`Box`, `Sweep`), `sprite.Sprite`, `fx.Particles`, `loop.FrameTimes`, `swing.RenderMode`, `input.Mouse` |
| [22](22-snake.md#what-moved-into-the-engine) | Snake | `loop.Ticker`, `random.SeededRandom`, `replay.Replay` |
| [23](23-rocks.md#what-moved-into-the-engine) | Rocks | `math` (`Vector`, `Angles`, `Wrap`), `shape.Outline`, `pool.Pool`, `collision.Circle` |
| [24](24-swarm.md#what-moved-into-the-engine) | Swarm | `sprite` (`SpriteSheet`, `Animation`), `entity`, `motion.FlightPath`, `data.TextData`, `collision.Grid`, `audio.Tune` |
| [25](25-maze.md#what-moved-into-the-engine) | Maze | `tile` (`TileMap`, `Cell`, `Direction`, `Walker`), `ai.StateMachine`, `path` (`DistanceMap`, `AStar`, `Passable`) |
| [26](26-blast.md#what-moved-into-the-engine) | Blast | `input` (`Gamepad`, `Gamepads`, `LinuxJoysticks`, `XInput`), `time.Timers`, `event.EventQueue` |
| [27](27-jump.md#what-moved-into-the-engine) | Jump | `camera` (`Camera`, `Parallax`), `physics` (`Body`, `Solidity`, `TileCollider`, `Jump`), `tiled`, `save.SaveFile` |
| [30](30-towers.md#what-moved-into-the-engine) | Towers | `path.FlowField`, `data.Catalog`, `menu.OverlayMenu`, `sprite.Rotations` |
| [31](31-sands.md#what-moved-into-the-engine) | Sands | `select`, `path` (`Formation`, `FieldCache`), `fog`, `minimap`, `economy`, `ai.InfluenceMap`, `save.Snapshot` |

Many classes grew later, when another game needed a little more: `Camera` learned the mouse in Towers, `Sprite` its opaque caches in Jump and Towers, `SeededRandom` to be saved in Sands. The sections of this part describe each class as it is now.

## Using the engine in a game

A game declares the engine as an ordinary dependency, `com.github.datazuul:amade-game-engine` from [JitPack](https://jitpack.io), pinned to a commit of the engine. Its `settings.gradle` builds a checkout of the engine next to the game instead, when there is one, so a change in the engine is tried in a game before it is merged ([chapter 0](00-setup.md#getting-the-source-code)). A game and its engine change therefore go in as two pull requests: the engine's first, then the game's, which pins the engine's new commit.

The engine brings [amade-commons](https://github.com/datazuul/amade-commons) along: the theme, the start-up, the XDG directories and the texts. Gamepads on Windows call XInput through `java.lang.foreign`, so a game that uses them runs with `--enable-native-access=ALL-UNNAMED`, in its tests, its `run` task, its packaged launcher and its jar's manifest ([chapter 26](26-blast.md#windows-calling-xinput)).
