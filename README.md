# amade-game-engine

**amade-game-engine** is the game library of the [amadeOS](https://github.com/datazuul/amadeos) desktop. The games (`amade-<name>-game`) are built on it, and like the other amadeOS applications they run on other operating systems too.

![License](https://img.shields.io/badge/license-EUPL--1.2-blue)
![Java](https://img.shields.io/badge/Java-25-orange)

## What it holds

The library grows game by game: each game adds what it needs and leaves it here for the next one. So far (from Mines, Solitaire, Paddle and Bricks):

- **High scores** (`eu.amadeos.gameengine.score`): the best ten results per table (difficulty or variant), lower or higher wins. Stored in `$XDG_DATA_HOME/amadeos/games/<game>/<table>.scores` (usually `~/.local/share/...`) as plain text, one entry per line. The name typed last time is offered again (`PlayerName`, in the game's configuration directory).
- **Standard menu entries** (`eu.amadeos.gameengine.swing.GameMenus`): *Game → New Game* (F2), *High Scores...*, *Exit* (Ctrl+Q), *Help → About*, with the same texts and mnemonics in every game.
- **High-score dialogs** (`HighScoreDialog`): the tables as tabs, and the question for the name after a good result.
- **Undo and redo** (`eu.amadeos.gameengine.undo.History`): snapshots of the game state before each move; *Undo* (Ctrl+Z) and *Redo* (Ctrl+Y) in `GameMenus`.
- **Animation** (`eu.amadeos.gameengine.anim`): `Easing` curves and `Tween`, the timing of one animation, without Swing; `swing.Animator` calls a frame function about 60 times a second while something moves.
- **Game clock** (`eu.amadeos.gameengine.time.GameClock`): time of one game, with a replaceable time source for tests.
- **Game loop** (`eu.amadeos.gameengine.loop`): `GameLoop` runs commands, fixed update steps and frames on a thread of its own; `FixedStep` is its arithmetic, tested without a thread.
- **Drawing** (`swing.GameCanvas`): active rendering with `BufferStrategy` in fixed logical coordinates, scaled to the window with black bars; unused keys reach Swing's menu shortcuts. Two layers, the world and an overlay for score and texts; two render modes (`RenderMode`: direct, or pixel mode with whole factors and hard edges); a frame-time display (`loop.FrameTimes`, F12). Full screen with F11 (`FullScreen`).
- **Sprites** (`eu.amadeos.gameengine.sprite.Sprite`): pictures with a size in logical coordinates, from image files or drawn in code, scaled once per screen size into compatible images.
- **Collision** (`eu.amadeos.gameengine.collision`): axis-aligned boxes and the swept test of a moving box against a resting one.
- **Particles** (`eu.amadeos.gameengine.fx.Particles`): hundreds of splinters and sparks in arrays, without creating objects while the game runs.
- **Sound** (`eu.amadeos.gameengine.audio`): sound effects made from tones (`Sound`), several at once through one `Mixer` thread; silent without a sound card.
- **Keyboard** (`eu.amadeos.gameengine.input`): `KeyState` (held keys, presses between two looks, X11 key repeat filtered), `Keyboard` (the AWT side, thread-safe), `Controls` (keys mapped to a game's actions).
- **Scenes** (`eu.amadeos.gameengine.scene`): `Scene` and `SceneStack` (only the top scene moves, all are drawn: a pause screen over a frozen match).
- Texts in English, Deutsch, Français, Español and Italiano.

Theme, Motif look, start-up, XDG directories and i18n come from [amade-commons](https://github.com/datazuul/amade-commons), which this library brings along.

## Handbook

The [Amade Game Developer's Handbook](https://datazuul.github.io/amade-game-engine/) teaches game programming with this engine, game by game. Its sources are in [`docs/handbook`](docs/handbook).

## Planned

In the order the games need them: tile maps, camera, pathfinding, music. Game rules stay plain Java without AWT, so they can be tested headless.

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
