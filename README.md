# amade-game-engine

**amade-game-engine** is the game library of the [amadeOS](https://github.com/datazuul/amadeos) desktop. The games (`amade-<name>-game`) are built on it, and like the other amadeOS applications they run on other operating systems too.

![License](https://img.shields.io/badge/license-EUPL--1.2-blue)
![Java](https://img.shields.io/badge/Java-25-orange)

## What it holds

The library grows game by game: each game adds what it needs and leaves it here for the next one. So far (from Mines, Solitaire, Paddle, Bricks, Snake, Rocks, Swarm, Maze, Blast and Jump):

- **High scores** (`eu.amadeos.gameengine.score`): the best ten results per table (difficulty or variant), lower or higher wins. Stored in `$XDG_DATA_HOME/amadeos/games/<game>/<table>.scores` (usually `~/.local/share/...`) as plain text, one entry per line. The name typed last time is offered again (`PlayerName`, in the game's configuration directory).
- **Standard menu entries** (`eu.amadeos.gameengine.swing.GameMenus`): *Game → New Game* (F2), *High Scores...*, *Exit* (Ctrl+Q), *Help → About*, with the same texts and mnemonics in every game.
- **High-score dialogs** (`HighScoreDialog`): the tables as tabs, and the question for the name after a good result.
- **Undo and redo** (`eu.amadeos.gameengine.undo.History`): snapshots of the game state before each move; *Undo* (Ctrl+Z) and *Redo* (Ctrl+Y) in `GameMenus`.
- **Animation** (`eu.amadeos.gameengine.anim`): `Easing` curves and `Tween`, the timing of one animation, without Swing; `swing.Animator` calls a frame function about 60 times a second while something moves.
- **Game clock** (`eu.amadeos.gameengine.time.GameClock`): time of one game, with a replaceable time source for tests.
- **Game loop** (`eu.amadeos.gameengine.loop`): `GameLoop` runs commands, fixed update steps and frames on a thread of its own; `FixedStep` is its arithmetic, tested without a thread. `Ticker` gives rules a slower pace of their own (a snake's cell per tick), changeable while the game runs, with the progress between two ticks for smooth drawing.
- **Random numbers that repeat** (`eu.amadeos.gameengine.random.SeededRandom`): the same seed gives the same numbers on every computer and Java version (SplitMix64, written out); `fork()` gives effects a generator of their own that never disturbs the rules'.
- **Replays** (`eu.amadeos.gameengine.replay.Replay`): a game as its seed and the player's actions with their ticks, played back in order, as plain text.
- **Drawing** (`swing.GameCanvas`): active rendering with `BufferStrategy` in fixed logical coordinates, scaled to the window with black bars; unused keys reach Swing's menu shortcuts. Two layers, the world and an overlay for score and texts; two render modes (`RenderMode`: direct, or pixel mode with whole factors and hard edges); a frame-time display (`loop.FrameTimes`, F12). Full screen with F11 (`FullScreen`).
- **Sprites** (`eu.amadeos.gameengine.sprite.Sprite`): pictures with a size in logical coordinates, from image files or drawn in code, scaled once per screen size into compatible images, opaque or with hard-edged transparency where the picture allows it, which copies faster; pixel art keeps hard edges. `drawSeamless` draws tiles that meet without gaps at any scale. `SpriteSheet` cuts one image into many frames, `Animation` says which frame to show when (in a loop, once, or back and forth).
- **Entities and components** (`eu.amadeos.gameengine.entity`): `Entity` (a thing made of components, looked up by their class), `Entities` (all of them, the walks of the systems over those with certain components, and changes that take effect between steps).
- **Flight paths** (`eu.amadeos.gameengine.motion.FlightPath`): curves of cubic Bézier segments and lines, flown at an even speed by distance, mirrored and moved.
- **Data files** (`eu.amadeos.gameengine.data.TextData`): a game's data as plain text, a line of words per thing, with errors that name the file and the line.
- **Vectors and angles** (`eu.amadeos.gameengine.math`): `Vector` (an immutable 2D vector with rotation), `Angles` (the short way round between two angles), `Wrap` (a field without edges: wrapping, the shortest distance, drawing over an edge).
- **Outlines** (`eu.amadeos.gameengine.shape.Outline`): figures of straight lines, turned, scaled and drawn without creating objects.
- **Object pools** (`eu.amadeos.gameengine.pool.Pool`): a fixed number of reusable objects, such as four shots, made once.
- **Collision** (`eu.amadeos.gameengine.collision`): axis-aligned boxes, the swept test of a moving box against a resting one, and circles, also across the edge of a field without edges. `Grid` finds the things near a place without looking at all things.
- **Tile maps** (`eu.amadeos.gameengine.tile`): `TileMap` (a grid of tiles, read from the rows of a data file, with wrapping steps and the neighbour mask for walls that join up), `Direction` and `Cell`, and `Walker`, which walks from tile centre to tile centre and asks at each centre which way to go.
- **State machines** (`eu.amadeos.gameengine.ai.StateMachine`): the states of a character as an enum, the allowed changes between them, the time in the current state and a listener for each change.
- **Pathfinding** (`eu.amadeos.gameengine.path`): `DistanceMap` (breadth-first search from one or more goals: the distance of every tile and the step downhill) and `AStar` (the cheapest way between two tiles, with costs per tile and a guess that knows about wrapping).
- **Particles** (`eu.amadeos.gameengine.fx.Particles`): hundreds of splinters and sparks in arrays, without creating objects while the game runs.
- **Sound** (`eu.amadeos.gameengine.audio`): sound effects made from tones (`Sound`), several at once through one `Mixer` thread, and music on a voice of its own, once or in a loop; tunes written in the Music Macro Language of the home computers (`Tune`), voices mixed into one sound. Silent without a sound card.
- **Keyboard and mouse** (`eu.amadeos.gameengine.input`): `KeyState` (held keys, presses between two looks, X11 key repeat filtered), `Keyboard` (the AWT side, thread-safe), `Controls` (keys mapped to a game's actions), `Mouse` (position in logical coordinates, clicks between two looks, held buttons; `GameCanvas.toLogical` maps like the render mode draws).
- **Gamepads** (`eu.amadeos.gameengine.input`): `Gamepads` (four pads, read from `/dev/input/js*` on Linux and through XInput with `java.lang.foreign` on Windows; plugged in and out while the game runs), `Gamepad` (buttons in the Xbox layout, the left stick with a round dead zone, presses between two looks never lost). `Controls` binds pad buttons to actions like keys. Windows needs `--enable-native-access=ALL-UNNAMED`.
- **Timers** (`eu.amadeos.gameengine.time.Timers`): many countdowns, each with a key, handed over in the order they ran out, also within one step.
- **Events** (`eu.amadeos.gameengine.event.EventQueue`): events that cause events, handled in order, wave by wave, with a limit against endless chains; what happened in a step for sounds and effects.
- **Camera** (`eu.amadeos.gameengine.camera`): `Camera` (the part of a world larger than the screen that is shown: follows a target with a dead zone and smoothing, stops at the world's edges) and `Parallax` (background layers that move slower than the world, repeated side by side).
- **Platform physics** (`eu.amadeos.gameengine.physics`): `Body` (a box with a speed that falls), `TileCollider` (moves a body through a tile map one axis at a time, without tunnelling: solid tiles, and platforms that hold from above only and can be dropped through), `Jump` (variable height, coyote time and a jump buffer; the jump speed for a height).
- **Tiled maps** (`eu.amadeos.gameengine.tiled`): `TiledMap` reads the TMX and TSX files of the [Tiled](https://www.mapeditor.org/) editor (tile layers in CSV, Base64 or zlib/gzip, tilesets in the map or in their own file, tile classes, animations, object layers with classes and properties) into records; errors name the file and the line.
- **Save files** (`eu.amadeos.gameengine.save.SaveFile`): a game's progress as versioned key-value text with a checksum, written to a new file that then replaces the old one, with the last good save kept as a backup.
- **Scenes** (`eu.amadeos.gameengine.scene`): `Scene` and `SceneStack` (only the top scene moves, all are drawn: a pause screen over a frozen match).
- Texts in English, Deutsch, Français, Español and Italiano.

Theme, Motif look, start-up, XDG directories and i18n come from [amade-commons](https://github.com/datazuul/amade-commons), which this library brings along.

## Handbook

The [Amade Game Developer's Handbook](https://datazuul.github.io/amade-game-engine/) teaches game programming with this engine, game by game. Its sources are in [`docs/handbook`](docs/handbook).

## Planned

In the order the games need them: the mouse in world coordinates, overlay menus and flow fields for many units (Towers), then selecting units, group pathfinding, fog of war and a minimap (Sands). Game rules stay plain Java without AWT, so they can be tested headless.

## Using it

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'com.github.datazuul:amade-game-engine:main-SNAPSHOT'
}
```

With a checkout next to the game, `settings.gradle` can build that instead:

```groovy
if (file('../amade-game-engine/settings.gradle').exists()) {
    includeBuild '../amade-game-engine'
}
```

## Building

```bash
./gradlew test
```

Java packages: `eu.amadeos.gameengine.*`; games use `eu.amadeos.games.<name>.*`.

## Licence

[EUPL-1.2](LICENSE)
