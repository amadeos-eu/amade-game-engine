# 45. Maps, Camera and Fog

From Maze on, the games of this book live on maps: a grid of tiles that the rules think in, and a picture of it that the drawing scales. This chapter covers the maps themselves, written as text or drawn in an editor, what spreads over them (land owned, services that reach, power that flows), the isometric view of a map, the camera that shows a part of a world larger than the screen, what one side of a strategy game knows of the map (the fog of war and the minimap), and ground that is not made of tiles at all but of cells that can be blasted away.

## A grid of tiles: TileMap, Cell and Direction

*Package `tile` · introduced in [25. Maze](25-maze.md#step-1-a-map-of-tiles) · source: [`TileMap`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/tile/TileMap.java), [`Cell`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/tile/Cell.java), [`Direction`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/tile/Direction.java)*

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

*Package `tile` · introduced in [25. Maze](25-maze.md#step-2-walking-on-a-grid) · source: [`Walker`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/tile/Walker.java)*

**What it is for.** Something that walks the corridors of a tile map: the muncher and the ghosts of Maze, the bombers of Blast. It moves from the centre of one tile to the centre of the next, and only at a centre may it turn or stop, so it always stays in the middle of a corridor and never cuts a corner into a wall. Between two centres it can only go on, or turn round at once (`turnAround`).

**When to choose it.** For movement in four directions along corridors, the way of maze and arena games. Units of a strategy game that walk freely in eight directions follow a [flow field](46-paths-and-decisions.md#many-units-one-goal-flowfield) instead; a body with gravity moves with a [`TileCollider`](44-movement.md#falling-and-standing-body-and-tilecollider).

**How it works.** Positions are in tiles: the centre of column 3, row 5 is (3, 5), and a walker halfway to the right of it is at (3.5, 5). `walk(distance, map, steering)` walks so many tiles and asks a `Steering` at every centre it reaches which way to go on. The steering knows the walls, the keys or the plan; the walker does not. The player's steering is a key that waits: a turn pressed before the corner is kept until the corner comes ([chapter 25](25-maze.md#a-key-that-waits)); a ghost's steering is its target and the rules of its mode.

```java title="amade-game-engine · tile/Walker.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/tile/Walker.java:walker"
```

`previousX()` and `previousY()` are the position before the last walk, for drawing between steps; over an edge, both move to the far side together.

## The land a side owns: Territory

*Package `tile` · introduced in [32. Colony](32-colony.md#the-border) · source: [`Territory`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/tile/Territory.java)*

**What it is for.** The land inside a border made of circles: the storehouse claims the land around it, every lookout tower more. A building may stand only inside. Claims overlap, and a tower that burns down gives back only what no other claim still holds.

**When to choose it.** For any land that is owned by being near something: a settlement's border, the reach of a city's services, the area a tower guards. Who **sees** what is a different question, answered by the [fog of war](#what-a-side-sees-fogofwar-and-fogpainter).

**How it works.** Every tile counts the claims that hold it; it is inside while the count is above zero. `release` counts down, so nothing has to find out which other claim also covers a tile. A circle is `dc² + dr² ≤ r² + r`: with `r²` alone, single tiles stick out at the four ends. `version` counts the changes, so a picture of the border is made again only when it moved.

```java title="amade-game-engine · tile/Territory.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/tile/Territory.java:territory"
```

| Method | What it does |
|---|---|
| `claim(id, col, row, radius)`, `release(id)` | Claims the land in a circle under a number; gives it back |
| `inside(col, row)` | True if any claim holds the tile |
| `border(col, row)`, `border()` | A tile inside next to one outside; all of them, for the posts |
| `size()`, `has(id)`, `ids()` | How much land; which claims there are, for saving |
| `version()` | Counts every change |

## Places that reach a circle: Coverage

*Package `tile` · introduced in [33. Town](33-town.md#step-5-reach-water-and-land-value) · source: [`Coverage`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/tile/Coverage.java)*

**What it is for.** How well the tiles of a map are served by places that reach a circle around them: schools, fire stations, water towers, parks. A tile knows how many places reach it, and how close the nearest one is, as a **strength** from 1 on the place itself to almost 0 at the edge of its reach.

**When to choose it.** When reach adds up or fades with distance: land value that rises near a park, happiness that counts how many homes a school reaches. When it only matters whether a tile is inside, and claims must be given back one by one, [`Territory`](#the-land-a-side-owns-territory) fits better.

**How it works.** `add` walks the square around the circle and counts every tile whose middle lies within the radius, keeping the strongest strength. A strength cannot be taken back out of a maximum, so there is no `remove`: the game clears the coverage and adds every place again when something changed, or once a month. For a map of 64 × 64 tiles and a few dozen places that costs nothing.

```java title="amade-game-engine · tile/Coverage.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/tile/Coverage.java:coverage"
```

| Method | What it does |
|---|---|
| `clear()` | Forgets all places |
| `add(col, row, radius)` | A place on a tile; `add(double col, double row, radius)` for the middle of a building of several tiles |
| `count(col, row)`, `covered(col, row)` | How many places reach a tile; whether any does |
| `strength(col, row)` | 1 on the place, falling towards 0 at the edge of its reach, 0 outside |

## Networks that carry: SupplyGrid

*Package `tile` · introduced in [33. Town](33-town.md#step-4-power-through-the-town) · source: [`SupplyGrid`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/tile/SupplyGrid.java)*

**What it is for.** A network of tiles that carries something from where it is made to where it is used: power from plants through lines and buildings to homes, water through pipes. Tiles that carry are **conductors**; some are **sources** that put an amount in, others **loads** that take an amount out.

**When to choose it.** Whenever something flows through what the player builds and runs short when there is too little: power, water, gas, data. Something carried by people or vehicles, one piece at a time, is a road network instead ([`RoadNetwork`](46-paths-and-decisions.md#roads-between-flags-roadnetwork)).

**How it works.** `solve` finds the networks, groups of conductors that touch along an edge (not across a corner), with a flood fill. In each network it serves the loads in the order a breadth-first flood from all its sources reaches them, until the network's sources run dry. So a town that builds more than its plant can feed loses power **at the far end**, where the player is looking, and not here and there at random. A load is served wholly or not at all; a load or source on a tile that does not conduct is made a conductor.

```java title="amade-game-engine · tile/SupplyGrid.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/tile/SupplyGrid.java:grid"
```

| Method | What it does |
|---|---|
| `clear()`, then `conductor`, `source`, `load` | Describes the network anew, once per survey |
| `solve()` | Finds the networks and serves the loads |
| `supplied(col, row)` | A load that got what it asked for |
| `connected(col, row)` | A tile in a network with a source, served or not: "no power" or "not enough power"? |
| `network(col, row)`, `networks()` | Which network a tile belongs to, how many there are |
| `supply()`, `demand()`, `served()` | All sources, all loads, what was served: the numbers for the bar at the top |

??? example "Test: the far end goes dark first"
    ```java title="amade-game-engine · test/tile/SupplyGridTest.java"
    --8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/tile/SupplyGridTest.java:test-grid"
    ```

## Maps from an editor: TiledMap

*Package `tiled` · introduced in [27. Jump](27-jump.md#step-1-levels-from-the-tiled-editor) · source: [`TiledMap`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/tiled/TiledMap.java), [`TiledObject`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/tiled/TiledObject.java), [`TiledProperties`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/tiled/TiledProperties.java), [`TmxReader`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/tiled/TmxReader.java)*

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

## The isometric view: IsoProjection

*Package `iso` · introduced in [33. Town](33-town.md#step-1-the-isometric-view) · source: [`IsoProjection`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/iso/IsoProjection.java)*

**What it is for.** A tile map seen from a corner and from above: every square tile becomes a diamond twice as wide as high, and things on it show two walls and a roof. The rules stay flat and square, in the same tile units as every other map of the engine; only the drawing turns.

**When to choose it.** For city builders, strategy games and role-playing games where buildings should look like buildings. It costs more than a flat map in the drawing: things must be sorted ([`DepthSort`](#what-covers-what-depthsort)), and the visible part of the map is no longer a rectangle of tiles.

**How it works.** A point of the world, `height` above the ground, lies at `originX + (x − y) · halfWidth / tile`, `originY + (x + y) · halfHeight / tile − height` of the **picture**, the plane in which the whole map is drawn. The camera moves over that picture as over any big world. The way back works for points on the ground, which is all a mouse click needs: `col` and `row` find the tile under a point of the picture.

```java title="amade-game-engine · iso/IsoProjection.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/iso/IsoProjection.java:projection"
```

The projection is affine, so it is also a transform for the graphics. Under `ground(h)` a rectangle of the world is drawn as the diamond it covers, at height `h`: lawns, roofs, shadows. Under `frontWall(x, y)` and `sideWall(x, y)` a wall is drawn as if it faced the viewer, with its windows and door, and Java2D shears it into place ([chapter 33](33-town.md#walls-and-roofs-through-transforms)).

| Method | What it does |
|---|---|
| `IsoProjection.forMap(cols, rows, tile, tileWidth, tileHeight, above)` | The projection of a whole map, with room `above` it for the tallest building |
| `IsoProjection.at(tile, tileWidth, tileHeight, originX, originY)` | A projection without a map, for drawing one building into a picture of its own |
| `pictureX(x, y)`, `pictureY(x, y, height)` | Where a point of the world lies in the picture |
| `worldX(px, py)`, `worldY(px, py)`, `col(px, py)`, `row(px, py)` | The point of the ground, and the tile, under a point of the picture |
| `ground(height)`, `frontWall(x, y)`, `sideWall(x, y)` | Transforms for drawing flat things, front walls and side walls |
| `visible(left, top, width, height, margin)` | The columns and rows to look at for a view, with `margin` rows below it for tall things |
| `width()`, `height()` | The size of the picture of the whole map: the camera's world |

!!! tip "Tip: zoom with the camera's view"
    The camera works in the picture's units. A view drawn twice as large is a camera with half the view, drawn under `g.scale(2, 2)`, with the mouse divided by 2 before the camera converts it. Sprites then cache themselves at the larger size, so the close view stays sharp ([chapter 33](33-town.md#step-10-a-view-that-zooms)).

??? example "Test: the way there and back"
    ```java title="amade-game-engine · test/iso/IsoProjectionTest.java"
    --8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/iso/IsoProjectionTest.java:test-projection"
    ```

### What covers what: DepthSort

*Package `iso` · introduced in [33. Town](33-town.md#step-2-what-covers-what) · source: [`DepthSort`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/iso/DepthSort.java)*

**What it is for.** The order in which the things of an isometric view are drawn, so what stands in front covers what stands behind: the painter's algorithm for things of any size. For things of one tile, sorting by `x + y` would do; a building of three tiles by two breaks that rule, and a tree behind its end would be drawn over its wall.

**How it works.** Each thing is a **footprint**, a rectangle of the ground, and a box in the picture. A lies behind B if A ends before B begins along `x`, or along `y`. That rule only orders things that touch in the picture, so the sort compares only things whose boxes overlap, found by a sweep along the picture's `x`, and draws them in topological order, the smallest `x + y` first among those that are free. Things behind each other both ways, a cycle no order can draw right, fall back to `x + y`. `sorted()` empties the sort for the next frame and reuses its objects.

```java title="amade-game-engine · iso/DepthSort.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/iso/DepthSort.java:sort"
```

| Method | What it does |
|---|---|
| `add(x0, y0, x1, y1, left, top, right, bottom, thing)` | A thing with its footprint in world units and its box in the picture |
| `sorted()` | All things, back to front; empties the sort |
| `DepthSort.behind(...)` | The rule for two footprints, for a test of your own |

Put only what stands into the sort. Flat things (lawns, car parks, roads) are drawn before it, with the ground; a building of several parts is one item, its parts ordered inside its own picture.

## A world larger than the screen: Camera

*Package `camera` · introduced in [27. Jump](27-jump.md#step-6-the-camera); the mouse, a view below a bar and a camera the player moves in [30. Towers](30-towers.md#step-5-the-mouse-in-the-world) · source: [`Camera`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/camera/Camera.java)*

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

**A view that changes its size.** When the canvas widens on a wide screen, the view grows with it: `resize(width, height)` gives the camera a new view size and keeps the middle of the view where it was, as far as the edges allow. A game calls it in every update step with the size the screen has now; a size that has not changed changes nothing ([chapter 30](30-towers.md#a-wider-screen-a-wider-view)).

```java title="amade-game-engine · camera/Camera.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/camera/Camera.java:resize"
```

**Drawing.** `x(alpha)` and `y(alpha)` give the camera between its last two positions, for the frame. `sees(left, top, width, height, alpha)` is true when a box of the world is at least partly in view: a map of 64 × 64 tiles draws only the 33 × 26 or so the camera sees ([chapter 27](27-jump.md#only-what-the-camera-sees)).

### Depth: Parallax

*Package `camera` · introduced in [27. Jump](27-jump.md#parallax) · source: [`Parallax`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/camera/Parallax.java)*

Backgrounds that scroll slower than the world, the further away they are: distant mountains, nearer hills, clouds. Each layer is one sprite, repeated side by side as far as the view needs and drawn seamlessly, and moved by a part of the camera's way, its *factor*: 0 stays still like the sky, 0.2 creeps along like mountains on the horizon, 1 moves with the world. A second, usually smaller factor moves it vertically, so the horizon stays where it is. `draw(g, camera.x(alpha), camera.y(alpha))` before the world draws all layers, the furthest first. Cut a background with much empty sky into bands, and make the solid ones opaque ([chapter 27](27-jump.md#measured-then-halved)).

## What a side sees: FogOfWar and FogPainter

*Package `fog` · introduced in [31. Sands](31-sands.md#step-3-fog-of-war) · source: [`FogOfWar`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/fog/FogOfWar.java), [`FogPainter`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/fog/FogPainter.java)*

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

*Package `minimap` · introduced in [31. Sands](31-sands.md#step-4-the-minimap) · source: [`Minimap`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/minimap/Minimap.java)*

**What it is for.** The whole map, small, in a corner of the screen: a pixel per tile, the things on it as dots, and a box that shows what the camera sees. A click on it moves the camera there.

**How it works.** The picture is an image of exactly one pixel per tile. The game paints a tile with `paint(col, row, rgb)` when it changes, which is seldom (crystal gone, a building up, fog lifted), and `render(g)` draws that image scaled into the minimap's box with hard edges in every frame. Scaling an image of 64 × 64 pixels into a box of 112 × 112 units is a small copy; drawing 4,096 rectangles in every frame would not be. The dots (`dot`), buildings in their true size (`box`) and the camera's view (`view`) are drawn over it in every frame, because they move.

```java title="amade-game-engine · minimap/Minimap.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/minimap/Minimap.java:convert"
```

`contains(x, y)` tells whether a point of the screen lies on the minimap, and `worldX`, `worldY` where in the world it points: the camera moves there with a left click, a right click can send the chosen units. A map that is not square keeps its proportions and is centred in the box. Draw the minimap in the overlay, so it stays sharp in pixel mode.

### An isometric map in a corner: DiamondMinimap

*Package `iso` · introduced in [33. Town](33-town.md#step-12-everything-else) · source: [`DiamondMinimap`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/iso/DiamondMinimap.java)*

The minimap of an isometric map: the whole map as a small diamond, turned like the view. It is the picture of the `IsoProjection`, scaled down, so the camera's view is a plain box on it and a click on it is a point of the picture to move the camera to: `camera.jumpTo(minimap.pictureX(x), minimap.pictureY(y))`. The tiles are an image of one pixel per tile, as in `Minimap`, drawn through a transform that turns each pixel into a little diamond; `paint(col, row, rgb)` when a tile changes, `render(g)` and `view(g, camera, alpha, colour)` in the overlay, `dot` for things that move. Town also uses it, without a camera, for the previews on the choice of a mission.

```java title="amade-game-engine · iso/DiamondMinimap.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/iso/DiamondMinimap.java:minimap"
```

## Ground that breaks: Ground and Landscape

*Package `terrain` · introduced in [29. Artillery](29-artillery.md#step-1-ground-that-breaks) · source: [`Ground`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/terrain/Ground.java), [`Landscape`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/terrain/Landscape.java)*

**What it is for.** Ground that can be blasted, dug and heaped up anywhere, by any amount, and that falls when its support is gone: an artillery duel, a digging game, worms with bazookas, a mine. A `Ground` is a grid of small cells, each a byte, 0 for air and any other number for a kind of earth the game names. A `Landscape` makes the line of hills to fill it from.

**When to choose it.** When the shape of the ground is not made of tiles a level designer placed, but of holes the players make. A level of walls and platforms is a [`TileMap`](#a-grid-of-tiles-tilemap-cell-and-direction); its tiles are whole or gone, never bitten into.

**How it works.** `new Ground(width, height, cell)` in units; with cells of half a unit, a field of 640 × 400 is about a million bytes. The cells are stored column after column, because nearly every question runs down a column: where is the surface here, does this column hang over air. `fill(surface, layering)` fills each column from a height down, the layering choosing the kind of each cell by its depth. `carve` blows a round hole, `heap` fills the air in a circle with earth, `recolour` changes the kind of the earth in a circle, for a scorched rim.

```java title="amade-game-engine · terrain/Ground.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/terrain/Ground.java:carve"
```

`settle(passes)` lets earth that hangs over air fall by one cell per pass, a whole run at a time; call it every step and the earth falls at a speed you can watch. Only **loose** columns are looked at, those a change touched, so a ground at rest costs nothing. Earth falls straight down; it does not slide into a slope.

```java title="amade-game-engine · terrain/Ground.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/terrain/Ground.java:settle"
```

`takeChanges()` returns the range of columns changed since it was last called, or `null`: the picture of the ground, an image with a pixel per cell, is painted again only there ([chapter 29](29-artillery.md#drawing-only-what-changed)). `copy()` makes an independent ground, to try something or to replay a shot.

`Landscape.generate(samples, base, amplitude, roughness, random)` makes a line of heights by **midpoint displacement**: the ends at random heights, then each midpoint at the average of its neighbours moved by a random amount that shrinks by `roughness` with each halving. 0.5 gives gentle hills, 0.7 jagged peaks. `clamp`, `flatten` and `bump` shape the line, `sample(line, spacing)` turns it into a function of x for `fill`.

| Method | What it does |
|---|---|
| `new Ground(width, height, cell)` | Empty ground, in units, of cells `cell` units wide |
| `fill(surface, layering)` | Earth below a line, of kinds by depth |
| `carve(x, y, r)`, `heap(x, y, r, kind)`, `recolour(x, y, r, kind)` | A crater, a heap, a change of kind; each returns the cells changed |
| `settle(passes)`, `settling()` | Lets earth fall; whether some may still fall |
| `solid(x, y)`, `material(x, y)` | Earth at a point; beside the ground is air, below it earth |
| `surface(x)`, `surface(from, to)` | The highest earth in a column, or over a width: where a thing that wide rests |
| `takeChanges()`, `copy()` | The columns to paint again; an independent copy |
| `Landscape.generate`, `clamp`, `flatten`, `bump`, `sample` | A line of hills, shaped, and as a function of x |

??? example "Test: the roof of a cave falls in"
    ```java title="amade-game-engine · test/terrain/GroundTest.java"
    --8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/terrain/GroundTest.java:test-ground"
    ```

## Seen from inside: GridMap, Raycaster and LightMap

*Package `raycast` · introduced in [60. Rivet](60-rivet.md#step-1-a-world-of-cells) · source: [`GridMap`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/raycast/GridMap.java), [`Door`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/raycast/Door.java), [`Raycaster`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/raycast/Raycaster.java), [`Camera`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/raycast/Camera.java), [`Texture`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/raycast/Texture.java), [`LightMap`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/raycast/LightMap.java), [`Billboard`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/raycast/Billboard.java), [`Views`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/raycast/Views.java)*

**What it is for.** A world seen through the player's eyes: corridors, halls and doors on a grid of square cells, walls of one height, a floor and a ceiling or open sky, things standing in it, all lit by lamps and fading into fog. The `Raycaster` draws it with one ray per column of the picture, fast enough in software for 60 frames a second at 640 × 400.

**When to choose it.** For a first-person view of a world that is flat in plan: every wall as high as every other, no rooms above rooms, no looking up and down. For anything more (slopes, stairs, bridges) a raycaster is the wrong tool; that needs a real 3D renderer, which the engine does not have.

**How it works.** A `GridMap` holds a wall texture, a floor texture and a ceiling texture per cell (`null` ceiling is sky) and the `Door`s. `cast` follows a ray from cell border to cell border (the DDA of Amanatides and Woo) and reports the distance, the side and where on the face it hit; doors are thin walls in the middle of their cell, and a ray passes where the door has slid away. `blocks` and `sees` answer the game's questions with the same ray.

`Raycaster.render(map, lights, camera, billboards)` draws floor and ceiling row by row, then the walls column by column, then the billboards from far to near, each column of a billboard hidden behind a nearer wall. The rays' directions are the camera's direction plus a part of the camera plane, and distances are measured in lengths of that direction, which removes the fisheye. `ZOOM` 1.2 gives about 68 degrees across a picture of 640 × 400. The picture is an image of its own; draw it with `g.drawImage(raycaster.frame(), …)`.

```java title="amade-game-engine · raycast/Raycaster.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/raycast/Raycaster.java:walls"
```

A `Texture` is square, a power of two in size, with a **glow** per texel: glowing texels ignore the room's light. The `LightMap` bakes the light once per level: an ambient colour, plus every lamp that sees a cell's centre, fading with distance, so walls cast shadows; between cell centres it is mixed smoothly. `flash` adds a light for one frame, `beginFrame` removes the last frame's. A `Billboard` is a thing in the world, a picture that faces the eye, with `Views` from several sides chosen by the angle between its facing and the eye ([`ModelRenderer`](42-sprites.md#things-in-3d-sdf-model-and-modelrenderer) renders them).

| Method | What it does |
|---|---|
| `new GridMap(w, h)`, `setWall`, `setFloor`, `setCeiling`, `addDoor` | The world, cell by cell; a `null` ceiling shows the sky |
| `cast(x, y, dx, dy, max, ray)`, `blocks(x, y)`, `sees(...)`, `passable()` | A ray, a body's question, a line of sight, cells for path searches |
| `updateDoors(dt, occupied)` | Moves the doors; an open door waits while its cell is occupied |
| `new Door(x, y, alongX, face, frame).timing(seconds, stayOpen)`, `open()` | A sliding door; a secret door has a wall's face and stays open |
| `Texture.of(image, glowMask)`, `Texture.of(size, argb, glow)` | A texture from a picture, or from texels |
| `new LightMap(map, r, g, b, lights)`, `flash`, `beginFrame`, `sample` | Baked light, flashes for one frame, the light at a point |
| `new Camera(x, y, angle)`, `eye` | Where the eye is, and how high: 0.5 is halfway up a wall |
| `new Raycaster(w, h)`, `setFog`, `setSky`, `render`, `frame()` | The view; fog colour and density; a panorama round the horizon |
| `depth(column)`, `project(camera, x, y, z)` | A column's wall distance; where a point of the world is on the picture |
| `new Billboard(views, x, y, size)`, `lift`, `facing`, `frame`, `flash`, `shadow` | A thing in the world |
| `new Views(textures)`, `Views.of(texture)`, `pick(frame, facing, fromViewer)` | Pictures by frame and direction |

??? example "Test: a wall twice as far is half as high"
    ```java title="amade-game-engine · test/raycast/RaycasterTest.java"
    --8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/raycast/RaycasterTest.java:test-raycaster"
    ```
