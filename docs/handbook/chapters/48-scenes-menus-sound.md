# 48. Scenes, Menus and Sound

What surrounds the rules: the screens a game shows one after another, the menus every game has, the high-score dialogs, menus drawn into the picture of the game, and sound. These parts make a set of rules feel like a finished game, and they are the same in every game, which is why they are in the engine.

## Screens: Scene and SceneStack

*Package `scene` · introduced in [20. Paddle](20-paddle.md#step-6-scenes); the overlay in [21. Bricks](21-bricks.md#two-layers-the-world-and-the-overlay) · source: [`Scene`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/scene/Scene.java), [`SceneStack`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/scene/SceneStack.java)*

**What it is for.** The screens of a real-time game: a title, the match, a pause screen, the end. A `Scene` advances in fixed steps (`update(seconds)`) and draws itself in logical coordinates (`render(g, alpha)`), with an optional `renderOverlay` for what lies on top of the world, such as the score. Both run on the game loop thread.

**How it works.** A `SceneStack` is itself a scene, and the game loop's update and render functions call it. Only the **top** scene is updated, but all are drawn from the bottom up: a pause screen pushed onto the match freezes it and still shows it underneath. The overlays of all scenes come after all worlds, so a pause text lies over the match's score.

```java title="amade-game-engine · scene/SceneStack.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/scene/SceneStack.java:stack"
```

| Method | What it does |
|---|---|
| `push(scene)` | On top; the one below stops moving: pause, a menu over the game |
| `pop()` | Removes the top; the one below moves again |
| `replace(scene)` | All scenes replaced by one: from the title to the match, from the match to its end |
| `top()`, `size()` | The scene that moves |

A scene is the right place for what belongs to one screen only: the title's demo game, the end screen's result. Changes that come from Swing, such as *New Game* in the menu, reach the stack through `GameLoop.post` ([chapter 41](41-loop-and-drawing.md#the-game-loop-gameloop-and-fixedstep)).

## The standard menus: GameMenus and GameTexts

*Package `swing` · introduced in [10. Mines](10-mines.md#step-10-high-scores); undo and redo in [11. Solitaire](11-solitaire.md#step-11-what-moved-into-the-engine), full screen in [20. Paddle](20-paddle.md#full-screen), graphics and frame times in [21. Bricks](21-bricks.md#a-display-for-frame-times) · source: [`GameMenus`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/swing/GameMenus.java), [`GameTexts`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/swing/GameTexts.java)*

**What it is for.** The menu entries every game has, with the same texts, mnemonics and keys in all of them, in the five languages of amadeOS (English, German, French, Spanish and Italian). The game adds its own entries between them.

| Method | Entry | Key |
|---|---|---|
| `game()`, `help()` | The menus *Game* and *Help* | |
| `newGame(action)` | *New Game* | F2 |
| `undo(action)`, `redo(action)` | *Undo*, *Redo*; enable them while there is something to undo | Ctrl+Z, Ctrl+Y |
| `fullScreen(action)` | *Full Screen*, usually `FullScreen::toggle` | F11 |
| `graphics(selected, action)` | The submenu *Graphics*, one entry per `RenderMode` | |
| `frameTimes(action)` | *Frame Times*, ticked while they show | F12 |
| `highScores(action)` | *High Scores...* | |
| `exit(action)` | *Exit* | Ctrl+Q |
| `about(title, action)` | *About &lt;title&gt;* | |

Every item has a component name (`game-new`, `game-full-screen`, …), so tests of the window find it. `GameTexts.load()` gives the engine's texts in the desktop's language; `GameTexts.BUNDLE` is the bundle's name for amade-commons' `TranslationCheck`, which a game's tests run on its own texts and the engine's together, so two entries of one menu never share a mnemonic ([chapter 2](02-structure.md#texts-and-languages)).

### High-score dialogs: HighScoreDialog

*Package `swing` · introduced in [10. Mines](10-mines.md#step-10-high-scores) · source: [`HighScoreDialog`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/swing/HighScoreDialog.java)*

`HighScoreDialog.show(parent, scores, tables, format, selected, highlight)` shows the [high scores](47-data-and-saving.md#high-scores-highscores), one tab per table, with a new entry highlighted and a *Reset* button that asks first; `format` turns a value into text, `42 s` or `1,250`. `HighScoreDialog.askName(parent, message, playerName)` asks for the name after a good result, offering the one typed last time, and remembers the new one. Both are modal Swing dialogs and must be called on the event thread. A real-time game that runs in full screen shows its own end screen as a scene instead, with the place in the table drawn into the picture.

## Menus over the game: OverlayMenu

*Package `menu` · introduced in [30. Towers](30-towers.md#step-6-menus-over-the-game) · source: [`OverlayMenu`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/menu/OverlayMenu.java)*

**What it is for.** A menu inside the game's picture that opens beside the place it belongs to, such as the tile the player clicked, and offers a few things to choose: the towers to build there and what they cost. Each `Item` has a value, an optional icon, a label, an optional detail on the right (a price) and whether it can be chosen; a tower the player cannot pay shows, but greyed.

**When to choose it.** For commands given on the map. A Swing popup cannot do it: the canvas covers Swing's lightweight popups, a heavyweight one is a window of its own over a game in full screen, and neither looks like the game. Settings and everything else outside the play stay in the Swing menu bar.

**How it works.** The menu is arithmetic first: where it lies, which row is under the mouse, what a click or a key chooses. `open(title, items, anchorX, anchorY, anchorWidth, anchorHeight)` places it to the right of the anchor, vertically centred, to its left when there is no room, and inside the bounds given to the constructor, the part of the screen it may cover. The game hands it the mouse and the keys in its update step, so the menu needs no listeners and its tests no screen:

| Method | What it does |
|---|---|
| `hover(x, y)` | Highlights the row under the mouse |
| `click(x, y)` | The value of a row that can be chosen; outside the menu, it closes without a choice; `null` when nothing was chosen |
| `key(number)` | The keys 1, 2, 3 for the first rows |
| `move(+1 or -1)`, `chooseHighlighted()` | The arrow keys and Enter |
| `contains(x, y)` | A click on the menu belongs to the menu, not to the map below |
| `update(seconds)`, `render(g, style)` | It opens in a tenth of a second, growing and fading in; drawn in the overlay with a `Style`, `Style.DEFAULT` being dark wood and brass |

```java title="amade-game-engine · menu/OverlayMenu.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/menu/OverlayMenu.java:open"
```

??? example "Test: chosen by click and by key"
    ```java title="amade-game-engine · test/menu/OverlayMenuTest.java"
    --8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/menu/OverlayMenuTest.java:test-choose"
    ```

## Sound effects: Sound

*Package `audio` · introduced in [20. Paddle](20-paddle.md#step-8-sound) · source: [`Sound`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/audio/Sound.java)*

**What it is for.** A short sound as raw samples: 44,100 numbers per second, each the position of the loudspeaker's membrane between -32768 and 32767. That is all a sound card plays. The effects of the early arcade games were not recordings but tones the machine made, and so are the sounds of this book.

**How it works.** `Sound.tone(wave, fromHz, toHz, seconds, volume)` makes a tone that slides from one pitch to another, with a fade of 5 ms in and out so the speaker does not click. The wave is `SQUARE` (the hard blip of the arcade), `TRIANGLE` (softer) or `NOISE` (crashes and explosions; the same noise every time, from a fixed seed). `then(next)` joins sounds into a little melody, `Sound.mix(sounds...)` plays several at once as one sound, `Sound.of(samples)` takes samples from anywhere ([chapter 20](20-paddle.md#the-shape-of-the-wave)).

```java title="amade-game-engine · audio/Sound.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/audio/Sound.java:tone"
```

Make the sounds once, at the start, in a sound bank of the game: a sound is a few kilobytes of samples, and making it costs more than playing it.

### Tunes as text: Tune

*Package `audio` · introduced in [24. Swarm](24-swarm.md#tunes-as-text) · source: [`Tune`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/audio/Tune.java)*

A melody written as text in the *Music Macro Language* of the home computers of the 1980s, one voice per string: `Tune.voice(Sound.Wave.SQUARE, 0.3, "t140 o4 l8 c e g > c")`. Notes `c` to `b` with `+`, `#` or `-` and an optional length (`4` a quarter, a dot half as long again), `r` a rest, `l` the default length, `o` the octave (in octave 4, `a` is 440 Hz), `>` and `<` an octave up and down, `t` the tempo in quarter notes per minute; spaces and bars `|` are only for the eye. Each note sounds a little shorter than its length, so repeated notes are heard apart, and notes are placed by their beat, so voices of different rhythm stay together. `Sound.mix` plays melody and bass as one sound.

```java title="amade-game-engine · test/audio/TuneTest.java"
--8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/audio/TuneTest.java:test-tune"
```

### Playing: Mixer

*Package `audio` · introduced in [20. Paddle](20-paddle.md#the-mixer); music in [24. Swarm](24-swarm.md#a-voice-for-the-music) · source: [`Mixer`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/audio/Mixer.java)*

**What it is for.** Sound effects, several at once: a brick breaks while the ball hits the wall, and both must be heard together without either waiting for the other. And one piece of music, once or again and again, on a voice of its own that no effect can push out.

**How it works.** The mixer opens **one** line to the sound card and feeds it from a thread of its own, block by block of 512 samples (11.6 ms, the longest a new sound waits). For every block it adds up the samples of all sounds that are playing, which is all mixing is, and clips the sum to the range of a sample. At most 16 effects play at once; a new one replaces the oldest. `play(sound)` only puts the sound in a queue and returns, from any thread: the game loop never waits for the sound card.

```java title="amade-game-engine · audio/Mixer.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/audio/Mixer.java:mix"
```

| Method | What it does |
|---|---|
| `start()`, `close()`, `running()` | Opens and closes the sound card; without one, the mixer stays silent and the game plays on |
| `play(sound)` | An effect, as soon as possible |
| `playMusic(sound, loop)`, `stopMusic()` | The music, from its start; once or in a loop |
| `setMuted(muted)` | New effects are dropped and the music turns silent but goes on, so it is where it would be when sound comes back |

!!! info "Behind the scenes: a hiss nobody hears"
    The mixer adds a hiss of one or two steps out of 32,768 to every block, far below hearing, so the line never carries pure silence. Some wireless headsets switch off after a few seconds of zeros and take longer to wake up again than a game can wait: the first sounds of a round would be lost ([appendix](appendix-java2d.md#sound)).
