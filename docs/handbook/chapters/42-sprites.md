# 42. Sprites and Effects

What a game shows, apart from plain shapes: pictures from files and pictures drawn in code, many frames in one image, pictures that turn, figures of lines, movements that ease in and out, and splinters by the hundred. One idea runs through all of it: **work out a picture once, then copy it**. Java2D copies an image of the screen's own pixel format very fast and does almost everything else slowly, so the classes of this chapter are built around caches.

## Pictures: Sprite

*Package `sprite` · introduced in [21. Bricks](21-bricks.md#step-4-sprites); hard edges in [24. Swarm](24-swarm.md#pixel-art-needs-hard-edges), seamless tiles and transparency in [27. Jump](27-jump.md#tiles-without-gaps), opaque drawings in [30. Towers](30-towers.md#a-ground-that-changes) · source: [`Sprite`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/sprite/Sprite.java)*

**What it is for.** A picture with a size in logical coordinates, say a brick of 40 × 16 units, whatever the size of the window. Its source is an image file, usually drawn larger than the logical size so it stays sharp on large screens, or a **drawing in code**, a function that paints into a box of the sprite's size.

**When to choose it.** For everything that looks the same from one frame to the next: bricks, tiles, insects, towers, the faces of panels. Shapes that change every frame (a health bar, a selection box) are drawn directly. A drawing that is expensive (gradients, many curves, text) is the best candidate of all for a sprite: drawn once, then copied ([Sands' panel](31-sands.md#the-panel-and-a-frame-time-measured)).

**How it works.** Scaling an image while drawing it is the classic way to make a Java2D game slow. A sprite scales itself **once** to the size it has on the screen, into a compatible image (the pixel format of the screen, which Java2D can keep in video memory), and from then on copies that image 1:1 to whole screen pixels. When the window changes size, the next frame scales once more.

```java title="amade-game-engine · sprite/Sprite.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/sprite/Sprite.java:draw"
```

The cache also gets the least transparency the picture needs, because copying costs accordingly: an `OPAQUE` picture is copied as it is, a `BITMASK` one skips its invisible pixels, only a `TRANSLUCENT` one is blended pixel by pixel. An image is looked at once to find out; a drawing in code has soft edges and stays translucent, unless you promise with `opaque()` that it covers its whole box ([chapter 27](27-jump.md#measured-then-halved)).

```java title="amade-game-engine · sprite/Sprite.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/sprite/Sprite.java:transparency"
```

| Method | What it does |
|---|---|
| `Sprite.load(owner, "/sprites/ball.png", w, h)` | From an image among the game's resources, shown at `w` × `h` units |
| `Sprite.of(image, w, h)` | From an image in memory |
| `Sprite.drawn(w, h, (g, w, h) -> ...)` | Drawn in code, into a box of `w` × `h` units |
| `hardEdges()` | The same picture, always scaled in square blocks: for pixel art, also in direct mode |
| `opaque()` | The same drawing, promised to cover its box: cached without transparency, copied without blending |
| `draw(g, x, y)` | Top left corner at `x`, `y` |
| `drawSeamless(g, x, y)` | Covers exactly the screen pixels from `x` to `x + width`, each edge rounded: tiles that meet without gaps |

**Seamless tiles.** At 2.7 screen pixels per unit, a tile of 16 units is 43.2 pixels wide. Copied 43 pixels wide at rounded positions, every fifth tile leaves a gap of one pixel, a line that flickers through a scrolling ground. `drawSeamless` works the width out from both rounded edges, 43 or 44, and caches up to four sizes. Use it for anything drawn edge to edge: tile maps, parallax layers, the fog ([chapter 27](27-jump.md#tiles-without-gaps)).

!!! warning "Pitfall: a rotated transform skips the cache"
    Under a rotated transform no cached picture fits, and `draw` scales the source in every frame. For things that turn, use [`Rotations`](#turned-in-advance-rotations) or an [`Outline`](#figures-of-lines-outline).

!!! tip "Tip: measure what the cache is worth"
    `-Damade.sprites.uncached=true` makes every sprite scale in every frame, as if there were no cache. Run a game's benchmark with and without it ([chapter 21](21-bricks.md#fix-1-scale-once-then-copy)).

??? example "Test: one scaling per size"
    ```java title="amade-game-engine · test/sprite/SpriteTest.java"
    --8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/sprite/SpriteTest.java:test-cache"
    ```

## Many pictures in one: SpriteSheet

*Package `sprite` · introduced in [24. Swarm](24-swarm.md#step-1-a-sprite-sheet) · source: [`SpriteSheet`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/sprite/SpriteSheet.java)*

**What it is for.** Many small pictures in one image, in a grid of equal cells: the frames of a flapping insect, the looks of a bomber, the stages of an explosion. One file instead of fifty, drawn and edited side by side, so the frames of an animation line up.

**How it works.** `SpriteSheet.of(image, columns, rows, frameWidth, frameHeight)` cuts the image into cells numbered row by row from the top left; each cell is a view into the image (`getSubimage`), not a copy, and becomes a `Sprite` with its own cache. `frame(index)` or `frame(column, row)` gives one; `hardEdges()` gives the same sheet for pixel art. The same image can be cut twice with cells of different sizes, as Swarm does for its large explosion.

```java title="amade-game-engine · sprite/SpriteSheet.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/sprite/SpriteSheet.java:sheet"
```

## Which frame when: Animation

*Package `sprite` · introduced in [24. Swarm](24-swarm.md#animation) · source: [`Animation`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/sprite/Animation.java)*

**What it is for.** Which frame of a sheet to show a given number of seconds after an animation started: a list of frame numbers, a time per frame, and what happens at the end, `LOOP` (wings, flames), `ONCE` (an explosion, a door) or `PING_PONG` (breathing, swaying).

**How it works.** An animation keeps no time and draws nothing: `frame(seconds)` is arithmetic, so one animation serves forty insects, each asking with its own time, and a test asks without a screen. `finished(seconds)` says when a `ONCE` animation is over, `duration()` how long one run takes. Tiles of a Tiled map can carry an animation of their own, which `TiledMap` reads as a list of frames ([chapter 45](45-maps.md#maps-from-an-editor-tiledmap)).

## Turning: pictures or lines?

Seen from above or from the side in space, things point in every direction. A sprite cannot turn cheaply, so the engine has two other answers. Choose by size and look ([chapter 23](23-rocks.md#pictures-or-lines)):

| The thing | Use | Because |
|---|---|---|
| A figure of straight lines: a ship, a rock of the vector screens | `Outline` | Turning costs a few multiplications per corner, any angle, any size |
| A picture, small or of pixel art: a turret, a tank, an insect | `Rotations` | Drawn once per direction, then copied like any sprite |
| A large, smooth picture turning freely | Avoid it | 64 cached angles of a large picture cost tens of megabytes; draw it as lines, or smaller |

### Turned in advance: Rotations

*Package `sprite` · introduced in [30. Towers](30-towers.md#turned-sprites) · source: [`Rotations`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/sprite/Rotations.java)*

`Rotations.of(steps, size, drawing)` draws a picture, pointing right at angle 0, once for each of `steps` directions, each a square `Sprite` of `size` units with its own cache. `draw(g, x, y, angle)` shows the nearest one with its middle at `x`, `y`; any angle works, negative or above 2π. At 32 directions the steps are 11.25°, and a turret turns smoothly to the eye. Towers and Sands draw their turrets, tanks and creeps this way.

```java title="amade-game-engine · sprite/Rotations.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/sprite/Rotations.java:rotations"
```

### Figures of lines: Outline

*Package `shape` · introduced in [23. Rocks](23-rocks.md#step-2-turning) · source: [`Outline`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/shape/Outline.java)*

A figure given by its corners around its own centre, pairs of x and y, angle 0 pointing right: `Outline.closed(...)` joins the last corner to the first, `Outline.open(...)` does not (a flame behind a ship). `draw(g, x, y, angle, scale)` and `fill(...)` turn every corner with the same formula as `Vector.rotated`, sine and cosine worked out once for all corners, into one `Path2D` that the outline keeps and reuses: drawing creates no objects. `radius()` is the farthest corner, a circle that holds the figure at any angle, which is exactly what [`Circle`](44-movement.md#circles-circle) collision needs.

```java title="amade-game-engine · shape/Outline.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/shape/Outline.java:path"
```

## Movement in time: Easing and Tween

*Package `anim` · introduced in [11. Solitaire](11-solitaire.md#easing-how-a-movement-feels) · source: [`Easing`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/anim/Easing.java), [`Tween`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/anim/Tween.java)*

**What it is for.** A movement from one value to another in a given time that feels natural: a card thrown onto its pile starts fast and lands gently. An `Easing` maps the share of the time that has passed, 0 to 1, to the share of the way: `LINEAR`, `EASE_OUT` (thrown and landing) and `EASE_IN_OUT` (carried). A `Tween` is one such movement in time: a start, a duration, an easing.

**When to choose it.** For movements with a fixed start and end that the rules do not care about: cards flying, a menu growing, a number counting up. Things the rules move step by step (a ball, a unit) are drawn from their positions instead.

**How it works.** `tween.at(now)` is the eased share at a `System.nanoTime()` value, 0 before the start and 1 from the end on; `Tween.lerp(from, to, t)` turns it into a value; `done(now)` says when it is over. The tween knows nothing of what moves, so one idea animates everything: the cards, their turning over, the bounce at the end ([chapter 11](11-solitaire.md#one-idea-animates-everything)). In a game without a loop, an [`Animator`](41-loop-and-drawing.md#animation-on-the-event-thread-animator) asks the tweens 60 times a second.

```java title="amade-game-engine · anim/Easing.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/anim/Easing.java:easing"
```

## Splinters and sparks: Particles

*Package `fx` · introduced in [21. Bricks](21-bricks.md#step-5-particles) · source: [`Particles`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/fx/Particles.java)*

**What it is for.** Many small, short-lived squares: splinters of a brick, sparks of a shot, dust behind a tank. Each flies in a straight line, falls with gravity if you give it one, and fades out during the second half of its life. Every action game from Bricks on uses them.

**When to choose it.** For effects that have no say in the rules. Particles are drawn, never asked: nothing collides with a splinter. Give them a random generator of their own, forked from the rules' one, so effects never change the game ([chapter 22](22-snake.md#effects-need-chance-too)).

**How it works.** Hundreds of particles are not objects but rows in a few arrays, one array per property, and the system never creates an object while the game runs: the colours are made once, sixteen shades of fading for every colour of the palette. A dead particle's row is taken over by the last living one, so the living stay at the front and a loop over `count` rows finds them all. `render(g, alpha)` draws each between its last two positions.

```java title="amade-game-engine · fx/Particles.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/fx/Particles.java:update"
```

| Method | What it does |
|---|---|
| `new Particles(capacity, colours...)` | Room for `capacity` particles; colours are referred to by index |
| `spawn(x, y, vx, vy, seconds, size, colour)` | A new particle; false when all rows are taken |
| `update(seconds, gravity)` | Moves all of them by one step |
| `render(g, alpha)` | Draws them |
| `clear()`, `count()` | Removes all, e.g. for a new level; how many live |
