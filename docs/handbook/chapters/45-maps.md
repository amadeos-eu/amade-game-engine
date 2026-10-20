# 45. Maps, Camera and Fog

From Maze on, the games of this book live on maps: a grid of tiles that the rules think in, and a picture of it that the drawing scales. This chapter covers the maps themselves, written as text or drawn in an editor, the camera that shows a part of a world larger than the screen, and what one side of a strategy game knows of the map: the fog of war and the minimap.

## A grid of tiles: TileMap, Cell and Direction

*Package `tile` · introduced in [25. Maze](25-maze.md#step-1-a-map-of-tiles) · source: [`TileMap`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/tile/TileMap.java), [`Cell`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/tile/Cell.java), [`Direction`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/tile/Direction.java)*

**What it is for.** Columns and rows of squares, each holding one value, usually an enum of the game such as `WALL` or `DOT`. The rules of a maze game, a puzzle or a strategy game live on such a map, in whole cells; only the drawing knows how many pixels a tile has. A `Cell` is a column and a row; a `Direction` is one of `UP`, `LEFT`, `DOWN`, `RIGHT`.

**How it works.** The map has no edges of its own: `step(col, row, direction)` over the right edge arrives at the left. A maze with walls all round never notices, and a row without walls at its ends becomes a tunnel from one side to the other ([chapter 25](25-maze.md#step-1-a-map-of-tiles)). `get` and `set` outside the map are an error that names the cell and the map's size.

The order of `Direction` is a rule of its own: when two ways are equally good, the one listed first wins (up, left, down, right). A choice then never depends on chance or on the order of a loop somewhere else, and two runs of Maze with the same input are the same game.

| Method | What it does |
|---|---|
| `new TileMap<>(width, height, fill)` | A map filled with one value |
| `get(col, row)`, `set(col, row, tile)`, `contains(col, row)` | One tile |
| `step(col, row, direction)` | The neighbour, over the edge on the far side |
| `count(test)`, `cells(test)` | How many tiles, and which: the dots still to eat |
| `copy()` | A fresh map for each level, all its dots in place |
| `mask(col, row, same, outside)` | Which neighbours belong together, as four bits: the picture of a wall piece |
| `TileMap.read(rows, legend)` | A map from the lines of a data file, a letter per tile |

**Maps as text.** `TileMap.read` takes lines such as `row XX..XX` from a [`TextData`](47-data-and-saving.md#data-files-textdata) file, the second word of each a row, and a legend from letters to tiles. Every row must have the same length, and every letter must be in the legend; an error names the line and the column, as `maze.txt:7: column 12: 'Q' is not a tile`. Maze and Blast write their maps this way ([chapter 25](25-maze.md#the-maze-is-a-text-file)).

!!! warning "Pitfall: `#` is a comment"
    `TextData` drops everything after `#` on a line, so `#` cannot stand for a tile in a map written as text. Maze uses `X` for its walls.

**Walls that join up.** `mask` gives sixteen values, one per shape a wall piece can take: alone, an end, a corner, a straight piece, a T, a cross. Choosing a tile's picture from its neighbours this way is called *autotiling*, and it is how Maze draws its walls from one letter ([chapter 25](25-maze.md#step-8-drawing-the-maze)).

```java title="amade-game-engine · tile/TileMap.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/tile/TileMap.java:mask"
```

## Walking corridors: Walker

*Package `tile` · introduced in [25. Maze](25-maze.md#step-2-walking-on-a-grid) · source: [`Walker`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/tile/Walker.java)*

**What it is for.** Something that walks the corridors of a tile map: the muncher and the ghosts of Maze, the bombers of Blast. It moves from the centre of one tile to the centre of the next, and only at a centre may it turn or stop, so it always stays in the middle of a corridor and never cuts a corner into a wall. Between two centres it can only go on, or turn round at once (`turnAround`).

**When to choose it.** For movement in four directions along corridors, the way of maze and arena games. Units of a strategy game that walk freely in eight directions follow a [flow field](46-paths-and-decisions.md#many-units-one-goal-flowfield) instead; a body with gravity moves with a [`TileCollider`](44-movement.md#falling-and-standing-body-and-tilecollider).

**How it works.** Positions are in tiles: the centre of column 3, row 5 is (3, 5), and a walker halfway to the right of it is at (3.5, 5). `walk(distance, map, steering)` walks so many tiles and asks a `Steering` at every centre it reaches which way to go on. The steering knows the walls, the keys or the plan; the walker does not. The player's steering is a key that waits: a turn pressed before the corner is kept until the corner comes ([chapter 25](25-maze.md#a-key-that-waits)); a ghost's steering is its target and the rules of its mode.

```java title="amade-game-engine · tile/Walker.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/tile/Walker.java:walker"
```

`previousX()` and `previousY()` are the position before the last walk, for drawing between steps; over an edge, both move to the far side together.

## Maps from an editor: TiledMap

*Package `tiled` · introduced in [27. Jump](27-jump.md#step-1-levels-from-the-tiled-editor) · source: [`TiledMap`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/tiled/TiledMap.java), [`TiledObject`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/tiled/TiledObject.java), [`TiledProperties`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/tiled/TiledProperties.java), [`TmxReader`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/tiled/TmxReader.java)*

**What it is for.** Levels drawn in [Tiled](https://www.mapeditor.org), the free map editor, read from its own TMX format. A maze of 28 × 31 is fine as text; a level of 200 × 30 tiles in three layers, with enemies, coins and doors, wants to be painted with the tiles' pictures under the mouse. Jump, Towers and Sands draw their maps in Tiled.

**When to choose it.** When a map is large, has several layers, or has things placed freely on it. A small grid of a few kinds of tiles reads more easily as [text](#a-grid-of-tiles-tilemap-cell-and-direction), and can be changed in any editor.

**What it reads.** Orthogonal maps that are not *infinite*, with:

- **tile layers**: a grid of *global tile ids* (gids) per layer, 0 for empty, encoded as CSV or Base64, uncompressed, zlib or gzip. The top bits of a gid say whether the tile is flipped; `gid(col, row)` takes them off, `flips(col, row)` gives them.
- **tilesets**, in the map or in their own `.tsx` file next to it: an image cut into tiles, starting at a `firstGid`. A tile can have a **class** (in older Tiled versions a *type*), such as `solid` or `coin`, properties and an animation.
- **object layers**: things placed freely, start, enemies, the goal. Each `TiledObject` has a class, a box in the map's pixels and properties. An object that shows a tile is placed by Tiled at its bottom left corner; the reader has already turned that into the top left, so all objects are alike.
- **properties** on the map, the layers, the objects and the tiles, read with a fallback in the type the game expects: `string`, `number`, `integer`, `bool` (`true`, `yes` or `1`). A map made before a property existed still works.

Errors name the file and the line, as in `level1.tmx:12: ...`, because the reader works element by element with SAX, which knows where it is.

```java title="amade-game-engine · tiled/TiledMap.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/tiled/TiledMap.java:lookup"
```

**From map to rules.** The rules do not want gids. `map.tiles("ground", gid -> ...)` turns a layer into a `TileMap` of the game's own tiles, usually by the tile's class, and the objects of a layer become the game's things by their class ([chapter 27](27-jump.md#from-map-to-rules)). Whatever is only a picture stays in the drawing.

??? example "Test: a map with objects"
    ```java title="amade-game-engine · test/tiled/TiledMapTest.java"
    --8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/tiled/TiledMapTest.java:test-objects"
    ```

## A world larger than the screen: Camera

*Package `camera` · introduced in [27. Jump](27-jump.md#step-6-the-camera); the mouse, a view below a bar and a camera the player moves in [30. Towers](30-towers.md#step-5-the-mouse-in-the-world) · source: [`Camera`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/camera/Camera.java)*

**What it is for.** The part of a world larger than the screen that is shown: a view of the screen's logical size whose top left corner lies somewhere in a world of, say, 2400 × 400. The game draws the world shifted by the camera's position. The camera never shows anything outside the world: at its edges it stops, and a world smaller than the view is centred.

**Two ways to move it.**

- **It follows a target**, usually the player: `follow(x, y, seconds)` in each step. Two rules make the scrolling calm. A **dead zone** in the middle of the view, in which the target may move without the camera moving at all, so a player who jumps on the spot does not shake the world. And **smoothing**: once the target leaves the dead zone, the camera closes a part of the distance per second (`catchUp`), worked out so it is the same at 60 or 120 steps a second. `jumpTo` puts the target in the middle at once, at the start of a level.
- **The player moves it**, by keys, by the mouse at the edge of the screen or by dragging the minimap: `moveBy(dx, dy)` in each step, also with 0, 0 when nothing moves, so that the drawing between steps does not drift.

```java title="amade-game-engine · camera/Camera.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/camera/Camera.java:camera"
```

**The mouse in the world.** A strategy game asks where a click lies in the world. `worldX(screenX)` and `worldY(screenY)` answer, for the camera after the last step, where the rules live. `placedAt(left, top)` says where the view lies on the screen, below a bar of 22 units for example; `shows(x, y)` is true when a point of the screen lies in the view and not on the bar beside it ([chapter 30](30-towers.md#step-5-the-mouse-in-the-world)).

```java title="amade-game-engine · camera/Camera.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/camera/Camera.java:to-world"
```

**Drawing.** `x(alpha)` and `y(alpha)` give the camera between its last two positions, for the frame. `sees(left, top, width, height, alpha)` is true when a box of the world is at least partly in view: a map of 64 × 64 tiles draws only the 33 × 26 or so the camera sees ([chapter 27](27-jump.md#only-what-the-camera-sees)).

### Depth: Parallax

*Package `camera` · introduced in [27. Jump](27-jump.md#parallax) · source: [`Parallax`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/camera/Parallax.java)*

Backgrounds that scroll slower than the world, the further away they are: distant mountains, nearer hills, clouds. Each layer is one sprite, repeated side by side as far as the view needs and drawn seamlessly, and moved by a part of the camera's way, its *factor*: 0 stays still like the sky, 0.2 creeps along like mountains on the horizon, 1 moves with the world. A second, usually smaller factor moves it vertically, so the horizon stays where it is. `draw(g, camera.x(alpha), camera.y(alpha))` before the world draws all layers, the furthest first. Cut a background with much empty sky into bands, and make the solid ones opaque ([chapter 27](27-jump.md#measured-then-halved)).

## What a side sees: FogOfWar and FogPainter

*Package `fog` · introduced in [31. Sands](31-sands.md#step-3-fog-of-war) · source: [`FogOfWar`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/fog/FogOfWar.java), [`FogPainter`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/fog/FogPainter.java)*

**What it is for.** What one side knows of the map. Every tile is in one of three states: **unseen** (nobody of this side has looked there: black, the *shroud*), **seen** (someone looked once, but nobody looks now: the ground is known and dimmed, enemies there are not shown) and **visible** (a unit or building of this side sees it now). Each side has a fog of its own. In Sands the computer opponent plays by its fog as the player does, with one exception: it knows where the enemy's buildings are.

**How it works.** Each step, the game calls `begin()`, then `look(col, row, radius)` for everything of the side that sees, then `end()`. Looking marks the tiles within the radius as visible and as seen for ever. The circles are worked out once per radius and kept as lists of offsets, so a hundred units looking cost a hundred loops over a few dozen tiles each, without a square root. `version()` changes whenever a tile changes its state, so the picture of the fog and the minimap are drawn again only then.

```java title="amade-game-engine · fog/FogOfWar.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/fog/FogOfWar.java:fog"
```

| Method | What it does |
|---|---|
| `visible(col, row)`, `seen(col, row)`, `state(col, row)` | What the side knows of a tile |
| `revealAll(on)` | The whole map visible: a cheat, or the end of a game |
| `seenAsText()`, `restoreSeen(text)` | The tiles seen so far, one bit per tile as Base64, for a save file; what is visible needs no saving |

**Soft edges.** Drawn tile by tile, the fog would show the grid: a staircase at the edge of everything a unit sees. `FogPainter` gives each **corner** of a tile a state, the darkest of the four tiles that meet there, and shades the tile smoothly from corner to corner, with a little noise that frays the edge like a real fog. Three states at four corners make 3⁴ = 81 kinds of tile, and that is all there are: each is drawn once as a sprite and copied after that, the same trick as the 32 turned copies of a turret. Taking the darkest tile at a corner means the fog reaches half a tile into what is visible, never the other way: nothing that stands on a hidden tile shows through ([chapter 31](31-sands.md#soft-edges)).

`painter.render(g, fog, fromCol, fromRow, toCol, toRow)` draws the fog over the tiles in view, after the world and before the marks that must stay readable. Runs of black tiles are filled as one rectangle, rounded to the screen's pixels exactly as `drawSeamless` rounds, so no line of pixels shows between the black and a soft tile.

What the player saw of the enemy, a building as it was when someone last looked, is the game's business: Sands keeps *ghosts* of enemy buildings in the seen fog ([chapter 31](31-sands.md#step-3-fog-of-war)).

## The map in a corner: Minimap

*Package `minimap` · introduced in [31. Sands](31-sands.md#step-4-the-minimap) · source: [`Minimap`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/minimap/Minimap.java)*

**What it is for.** The whole map, small, in a corner of the screen: a pixel per tile, the things on it as dots, and a box that shows what the camera sees. A click on it moves the camera there.

**How it works.** The picture is an image of exactly one pixel per tile. The game paints a tile with `paint(col, row, rgb)` when it changes, which is seldom (crystal gone, a building up, fog lifted), and `render(g)` draws that image scaled into the minimap's box with hard edges in every frame. Scaling an image of 64 × 64 pixels into a box of 112 × 112 units is a small copy; drawing 4,096 rectangles in every frame would not be. The dots (`dot`), buildings in their true size (`box`) and the camera's view (`view`) are drawn over it in every frame, because they move.

```java title="amade-game-engine · minimap/Minimap.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/minimap/Minimap.java:convert"
```

`contains(x, y)` tells whether a point of the screen lies on the minimap, and `worldX`, `worldY` where in the world it points: the camera moves there with a left click, a right click can send the chosen units. A map that is not square keeps its proportions and is centred in the box. Draw the minimap in the overlay, so it stays sharp in pixel mode.
