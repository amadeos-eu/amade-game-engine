# 47. Chance, Data and Saving

The state of a game, where it comes from and where it goes: random numbers that repeat, a game kept as its input or as snapshots for undo, the data files a designer writes, the money and production of a strategy game, and what is written to disk, from high scores to a whole running battle.

## Chance that repeats: SeededRandom

*Package `random` · introduced in [22. Snake](22-snake.md#step-4-chance-that-repeats); saving its state in [31. Sands](31-sands.md#step-9-saving-the-whole-game) · source: [`SeededRandom`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/random/SeededRandom.java)*

**What it is for.** Random numbers that repeat exactly: the same seed gives the same numbers, on every computer, with every Java version. Replays, tests of whole games, a game of the day and loaded saves all depend on it.

**When to choose it.** For all chance in the rules of every game. `java.util.Random` repeats too, but its sequence belongs to the JDK; `SeededRandom` writes its algorithm out, so no update can ever change a sequence and break a recorded replay. `SeededRandom.freshSeed()` gives a game that should differ from the last one a new seed from the clock.

**How it works.** The algorithm is **SplitMix64**: a counter that grows by a constant, and a few multiplications and shifts that mix it. Its whole state is one `long`. The methods a game uses are written out too: `nextInt(bound)` (without the bias that `%` alone gives), `nextInt(origin, bound)`, `nextDouble()`, `nextBoolean()`. The other methods of `RandomGenerator` work, but their results depend on Java's implementation.

```java title="amade-game-engine · random/SeededRandom.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/random/SeededRandom.java:seeded-random"
```

| Method | What it does |
|---|---|
| `fork()` | A second generator, seeded from this one: its numbers never change what this one draws next |
| `copy()` | A generator at exactly this one's position |
| `seed()` | The seed it started with |
| `state()`, `SeededRandom.resume(seed, state)` | Where it stands, and a generator that goes on from there: saved with a game, the loaded game draws the very numbers the saved one would have |

!!! warning "Pitfall: effects that change the game"
    Splinters and sparks need chance too. Drawn from the rules' generator, a particle more or less changes every number the rules draw after it, and a replay goes another way when the player turns effects off. Give the effects a generator of their own, `rules.fork()` ([chapter 22](22-snake.md#effects-need-chance-too)).

## A game as its input: Replay

*Package `replay` · introduced in [22. Snake](22-snake.md#step-5-replays) · source: [`Replay`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/replay/Replay.java)*

**What it is for.** A whole game as a seed and the player's actions, each with the tick at which the rules received it. When the rules are deterministic (all chance from a `SeededRandom` with this seed, all time from ticks), feeding the same actions at the same ticks plays the same game again, to the last point. A title screen that replays a good game, a bug report that can be played back, a test that plays a whole game: all of them are a replay.

**How it works.** `record(tick, action)` while playing; `playback()` gives a `Playback` whose `actionsAt(tick)` hands the actions for each tick in order, and `finished()` says when all are played. As text, one line per action after the seed, so a replay can be saved, read and written by hand in a test ([chapter 22](22-snake.md#replays-as-text)):

```java title="amade-game-engine · replay/Replay.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/replay/Replay.java:text"
```

!!! warning "Pitfall: record ticks, not time"
    "UP after 3.2 seconds" depends on the frame rate, the computer and the pauses the player took. The tick count does not, and a replay recorded on a fast computer plays the same on a slow one ([chapter 22](22-snake.md#step-5-replays)).

**When a replay is not enough.** A replay plays the game again from the start. That suits a game of a few minutes with one player's actions. A battle of an hour, or a game the player wants to continue tomorrow, is saved as a [`Snapshot`](#a-whole-game-snapshot) of its state instead.

## Undo and redo: History

*Package `undo` · introduced in [11. Solitaire](11-solitaire.md#step-4-undo-by-snapshots) · source: [`History`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/undo/History.java)*

**What it is for.** Undo and redo in a game of moves. Before each move the game records a copy of its state; undo hands the last copy back. This is simpler than undoing each kind of move by hand, and it cannot go wrong for moves with side effects (a card turned over, a score changed), as long as the copies are complete.

**How it works.** Two stacks. `record(before)` pushes the state before a move and forgets everything undone, because a new move makes it unreachable; `undo(current)` keeps the current state for redo and returns the one before; `redo(current)` goes forward again. At most 1,000 steps are kept by default. The copies must share nothing mutable with the live state: a `copy()` method of the domain that copies deeply, tested once, is the whole price. `GameMenus.undo` and `redo` give the menu entries with Ctrl+Z and Ctrl+Y.

```java title="amade-game-engine · undo/History.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/undo/History.java:history"
```

## Data files: TextData

*Package `data` · introduced in [24. Swarm](24-swarm.md#step-4-stages-as-data) · source: [`TextData`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/data/TextData.java)*

**What it is for.** A game's data as plain text, one thing per line: words separated by spaces, the first word saying what the line is. Stages and their flight paths in Swarm, the maze of Maze, the arena of Blast, the waves of Towers. A designer, or a player, changes the game without a compiler.

**How it works.** `TextData.load(owner, "/data/waves.txt")` reads a resource in UTF-8; everything after `#` is a comment and empty lines are skipped. Each `Line` keeps its file name and number, so every question can fail with an error that says **where**: `word(i)`, `number(i)` and `integer(i)` throw a `DataException` like `waves.txt:12: "fast" is not a number`, and `line.error(message)` makes one for the game's own checks.

```java title="amade-game-engine · data/TextData.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/data/TextData.java:textdata"
```

**What a data file is good for.** Lines of a few words each, in an order that matters: a stage's groups, a wave's creeps, the rows of a map ([`TileMap.read`](45-maps.md#a-grid-of-tiles-tilemap-cell-and-direction)). Things with many named properties read better as a [`Catalog`](#things-with-properties-catalog); a map larger than a screen is drawn in [Tiled](45-maps.md#maps-from-an-editor-tiledmap).

!!! tip "Tip: read every data file in a test"
    Errors in data arrive at run time. A test that reads every data file of the game, and a test that plays each level, find them at build time ([chapter 30](30-towers.md#can-every-level-be-won)).

## Things with properties: Catalog

*Package `data` · introduced in [30. Towers](30-towers.md#step-3-units-towers-and-waves-as-data) · source: [`Catalog`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/data/Catalog.java)*

**What it is for.** The units, towers, weapons and buildings of a game, one block each in a data file: a line naming the thing, then its properties, one per line. The numbers a designer changes again and again while the game is played. With `like`, a block starts with all properties of an earlier one of its kind and changes some:

```text
unit orc
  health 60
  speed 1.1        # tiles per second

unit orc-chief like orc
  health 180       # everything else as the orc
```

**How it works.** `Catalog.read(lines, "unit", "tower")` reads the blocks of the kinds the game names, in the order of the file, as `Entry`s. The game asks each entry for the properties it knows (`number`, `integer`, `text`, each with or without a fallback, `lines` for a property given several times, such as a tower's levels) and then calls `finish()`. Whatever the game did not ask for is an error with the file and the line: a property typed `rnage` is not silently ignored while the tower keeps its old range, the classic mistake of every data file read without such a check.

```java title="amade-game-engine · data/Catalog.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/data/Catalog.java:read"
```

??? example "Test: errors that name the line"
    ```java title="amade-game-engine · test/data/CatalogTest.java"
    --8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/data/CatalogTest.java:test-errors"
    ```

## Money and production: Credits and Production

*Package `economy` · introduced in [31. Sands](31-sands.md#step-5-crystals-credits-and-power) · source: [`Credits`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/economy/Credits.java), [`Production`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/economy/Production.java)*

**What it is for.** The money of one side and what it builds. `Credits` is money with a **store**: what the harvesters bring in fills the refineries and silos, and what they cannot hold is lost, which makes the player spend instead of saving up for ever. `Production` is a queue of orders, the first one in the works, **paid while it is made**: a tank of 300 credits that takes 20 seconds costs 15 credits a second. Without money the work stops and waits; with too little power it goes slower; a cancelled order gives back what was paid so far.

**When to choose them.** For any game in which things cost and take time: a strategy game's buildings and units, the workshops of an economy game. Neither knows what is made or where the money comes from.

**How it works.** Credits are whole: a game shows its money as a number, and a number that grows in fractions is a puzzle. The payment per step is not: 15 credits a second at 120 steps a second is an eighth of a credit. So `update(seconds, credits, speed)` pays a whole credit as soon as the work reaches it, and the work only grows as far as it has been paid: no credit, no progress. It returns the item finished in this step, or `null`.

```java title="amade-game-engine · economy/Production.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/economy/Production.java:production"
```

| Method | What it does |
|---|---|
| `credits.pay(price)`, `earn(income)`, `refund(credits)` | Pays if there is enough; brings in what fits and returns the rest; gives back, past the limit |
| `credits.setCapacity(capacity)`, `full()` | A silo built or destroyed; credits above a lowered limit stay |
| `production.order(item, cost, seconds)` | Adds an order |
| `hold(on)`, `cancel(item, credits)` | Stops and goes on, keeping what was paid; removes the last order of an item, with a refund if it was in the works |
| `keep(item)`, `ready()`, `take()` | A finished building that waits to be placed; the queue waits with it |
| `progress()`, `paid()`, `current()`, `queue()`, `count(item)` | For the panel |
| `restore(paid, done, held)` | The first order's state from a save file |

## Progress on disk: SaveFile

*Package `save` · introduced in [27. Jump](27-jump.md#step-8-saving-progress) · source: [`SaveFile`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/save/SaveFile.java)*

**What it is for.** A player's progress on disk: which levels are open, the best times, what was collected; in Sands also the whole running battle. Settings may get lost without harm, progress may not. So a save file is written with more care than settings:

- **Never half written.** The new state goes into a temporary file next to the save, is forced onto the disk, and only then takes the save's name in one atomic rename. A crash leaves the old file or the new one, never a mixture.
- **A backup.** The file before the last save is kept as `<name>.bak`. If the save is damaged, `load()` reads the backup.
- **A checksum.** The last line is a CRC-32 of everything above it. A file cut short is recognised.
- **A version.** The first line names the format's version, so a later game can read and convert what an earlier one wrote.

The content is keys and values, sorted, as in a properties file, so a person can read it and a diff can compare two saves.

| Method | What it does |
|---|---|
| `SaveFile.of(xdg, "jump", "progress")` | `$XDG_DATA_HOME/amadeos/games/jump/progress.save` |
| `store(version, values)` | Writes a new state; the one before becomes the backup |
| `load()` | The last state saved, `Optional<Saved>` with the version and the values; empty on the first start |
| `delete()` | A fresh start |

### A whole game: Snapshot

*Package `save` · introduced in [31. Sands](31-sands.md#step-9-saving-the-whole-game) · source: [`Snapshot`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/save/Snapshot.java)*

Progress is a handful of numbers. A running battle is a few hundred things, each with a dozen values, in lists. A `Snapshot.Writer` gives them names a save file can hold and a person can read, `unit.count=2`, `unit.0.kind=tank`, `unit.0.x=212.5`, nested as deep as needed with `child(key)` and `add(key)`; a `Snapshot.Reader` reads them back with `text`, `integer`, `whole`, `number`, `bool`, `choice` (an enum), `child` and `list`, and says which key is missing or malformed when a file does not fit.

The goal is a loaded game that goes on **exactly** as the saved one would have, step for step. Three rules make that possible ([chapter 31](31-sands.md#step-9-saving-the-whole-game)):

- **Numbers to the bit.** The writer uses `Double.toString`, the shortest text that reads back as exactly the same double; never a format with a fixed number of digits.
- **The random numbers.** Save `random.state()` and the seed, and `SeededRandom.resume` them.
- **Nothing derived.** What follows from other values (walls of buildings on the map, power, storage, flow fields) is worked out again after loading. What is not saved cannot be saved wrong.

```java title="amade-game-engine · test/save/SnapshotTest.java"
--8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/save/SnapshotTest.java:test-snapshot"
```

The test a game writes on top of that: run a game, save it, load it, run both on, and compare their snapshots key by key. When it fails, it names the first key that differs, nearly always the field someone forgot to save ([Sands' test](31-sands.md#step-9-saving-the-whole-game)).

### Which kind of keeping?

| What | Use | Where |
|---|---|---|
| Choices of the player: render mode, sound, keys | A properties file of the game's `repository` package (amade-commons' `PropertiesFile`) | `~/.config/amadeos/<game>/` |
| The best results | `HighScores` | `~/.local/share/amadeos/games/<game>/*.scores` |
| Progress: levels open, best times | `SaveFile` | `~/.local/share/amadeos/games/<game>/*.save` |
| A running game, to continue later | `Snapshot` in a `SaveFile` | the same |
| A game to play again from the start | `Replay` | wherever the game keeps it, as text |
| The moves of this game, for undo | `History` | memory only |

## High scores: HighScores

*Package `score` · introduced in [10. Mines](10-mines.md#step-10-high-scores) · source: [`HighScores`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/score/HighScores.java), [`Score`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/score/Score.java), [`Ranking`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/score/Ranking.java), [`PlayerName`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/score/PlayerName.java)*

**What it is for.** The best ten results of a game, one **table** per difficulty or variant. A `Ranking` says whether lower wins (times, moves) or higher (points); on a tie, the older entry stays in front. `PlayerName` remembers the name typed last time and offers it again, the login name the first time.

**How it works.** Each table is a text file `<table>.scores` in `$XDG_DATA_HOME/amadeos/games/<game>/`, one entry per line: value, date and name, separated by tabs. Lines that cannot be read are skipped, so a damaged file loses entries but never stops the game; a table is written to a temporary file and renamed, like a save. `qualifies(table, value)` says whether a result enters the table, `add(table, score)` stores it and returns its place (0 for the best, -1 if it did not make it), `list(table)` gives the entries best first, `clear(table)` empties it. A `Score`'s name is cleaned of tabs and line breaks and cut to 32 characters.

The dialogs that show the tables and ask for the name are [`HighScoreDialog`](48-scenes-menus-sound.md#high-score-dialogs-highscoredialog).
