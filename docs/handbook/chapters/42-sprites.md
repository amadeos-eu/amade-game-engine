# 42. Sprites and Effects

What a game shows, apart from plain shapes: pictures from files and pictures drawn in code, many frames in one image, pictures that turn, figures of lines, movements that ease in and out, splinters by the hundred, tables whose parts stand up with their shadows, a mirror ball, and the lamps and dot-matrix displays of a machine. One idea runs through all of it: **work out a picture once, then copy it**. Java2D copies an image of the screen's own pixel format very fast and does almost everything else slowly, so the classes of this chapter are built around caches.

## Pictures: Sprite

*Package `sprite` · introduced in [21. Bricks](21-bricks.md#step-4-sprites); hard edges in [24. Swarm](24-swarm.md#pixel-art-needs-hard-edges), seamless tiles and transparency in [27. Jump](27-jump.md#tiles-without-gaps), opaque drawings in [30. Towers](30-towers.md#the-ground) · source: [`Sprite`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/sprite/Sprite.java)*

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

## A ground painted once: ScrollingLayer

*Package `sprite` · introduced in [33. Town](33-town.md#a-ground-that-is-painted-only-where-it-changed) · source: [`ScrollingLayer`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/sprite/ScrollingLayer.java)*

**What it is for.** A layer of the world that rarely changes and costs much to draw: the ground of a big map with a thousand tiles in view, and the shadows on it. The layer keeps the view as one opaque picture in screen pixels and copies it, instead of drawing the tiles again in every frame.

**When to choose it.** When a frame measures slow and most of the time goes into something that looks the same from one frame to the next. Sprites already make each tile cheap; the layer makes the thousand copies one. A ground that changes in most frames (snow falling, water that ripples everywhere) gains nothing; neither does a small map that fits the screen. Things that move are never part of it: they are drawn over it.

**How it works.** The layer remembers where its picture lies in the world, in whole screen pixels. When the view stands still and the game passes the same **key**, `draw` copies the picture. When the view has moved, the picture is shifted by `copyArea` and only the strips that came into view are painted, by a painter the game hands in, clipped to each strip. When the key changes (the game folds into it whatever the layer shows), or the view jumped further than the picture is wide, or the window or zoom changed the scale, the whole view is painted again.

```java title="amade-game-engine · sprite/ScrollingLayer.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/sprite/ScrollingLayer.java:layer"
```

| Method | What it does |
|---|---|
| `new ScrollingLayer(viewWidth, viewHeight)` | A layer for a view of that many world units |
| `draw(g, screenLeft, screenTop, left, top, key, painter)` | Draws the view whose top left world point is `left`, `top`, painting first what is missing |
| `invalidate()` | The next frame paints the whole view |
| `ScrollingLayer.snap(world, scale)` | A position rounded to the screen pixel the layer puts it on |
| `painted()` | How many regions were painted so far: what the layer saved |

The painter must cover every pixel of the region it gets, so fill a background first: the picture is opaque and is copied without blending. Because the layer puts the view at whole screen pixels, draw the rest of the world at `snap`ped camera positions too, or things that stand on the ground slide half a pixel against it while the view scrolls ([chapter 33](33-town.md#a-ground-that-is-painted-only-where-it-changed)).

??? example "Test: a moved view paints only its new strips, and looks as if painted whole"
    ```java title="amade-game-engine · test/sprite/ScrollingLayerTest.java"
    --8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/sprite/ScrollingLayerTest.java:test-scroll"
    ```

## Many pictures in one: SpriteSheet

*Package `sprite` · introduced in [24. Swarm](24-swarm.md#step-1-a-sprite-sheet) · source: [`SpriteSheet`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/sprite/SpriteSheet.java)*

**What it is for.** Many small pictures in one image, in a grid of equal cells: the frames of a flapping insect, the looks of a bomber, the stages of an explosion. One file instead of fifty, drawn and edited side by side, so the frames of an animation line up.

**How it works.** `SpriteSheet.of(image, columns, rows, frameWidth, frameHeight)` cuts the image into cells numbered row by row from the top left; each cell is a view into the image (`getSubimage`), not a copy, and becomes a `Sprite` with its own cache. `frame(index)` or `frame(column, row)` gives one; `hardEdges()` gives the same sheet for pixel art. The same image can be cut twice with cells of different sizes, as Swarm does for its large explosion.

```java title="amade-game-engine · sprite/SpriteSheet.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/sprite/SpriteSheet.java:sheet"
```

## Which frame when: Animation

*Package `sprite` · introduced in [24. Swarm](24-swarm.md#animation) · source: [`Animation`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/sprite/Animation.java)*

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

*Package `sprite` · introduced in [30. Towers](30-towers.md#turned-sprites) · source: [`Rotations`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/sprite/Rotations.java)*

`Rotations.of(steps, size, drawing)` draws a picture, pointing right at angle 0, once for each of `steps` directions, each a square `Sprite` of `size` units with its own cache. `draw(g, x, y, angle)` shows the nearest one with its middle at `x`, `y`; any angle works, negative or above 2π. At 32 directions the steps are 11.25°, and a turret turns smoothly to the eye. Towers and Sands draw their turrets, tanks and creeps this way.

```java title="amade-game-engine · sprite/Rotations.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/sprite/Rotations.java:rotations"
```

### Figures of lines: Outline

*Package `shape` · introduced in [23. Rocks](23-rocks.md#step-2-turning) · source: [`Outline`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/shape/Outline.java)*

A figure given by its corners around its own centre, pairs of x and y, angle 0 pointing right: `Outline.closed(...)` joins the last corner to the first, `Outline.open(...)` does not (a flame behind a ship). `draw(g, x, y, angle, scale)` and `fill(...)` turn every corner with the same formula as `Vector.rotated`, sine and cosine worked out once for all corners, into one `Path2D` that the outline keeps and reuses: drawing creates no objects. `radius()` is the farthest corner, a circle that holds the figure at any angle, which is exactly what [`Circle`](44-movement.md#circles-circle) collision needs.

```java title="amade-game-engine · shape/Outline.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/shape/Outline.java:path"
```

## Things in 3D: Sdf, Model and ModelRenderer

*Package `raycast.model` · introduced in [60. Rivet](60-rivet.md#step-8-models-from-distances) · source: [`Sdf`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/raycast/model/Sdf.java), [`Model`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/raycast/model/Model.java), [`Material`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/raycast/model/Material.java), [`ModelRenderer`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/raycast/model/ModelRenderer.java)*

**What it is for.** Pictures of solid things, rendered by the engine instead of drawn by hand: a machine from eight sides for a first-person view, a tool held in front of the camera, a turret seen from any angle. A `Model` is described by **signed distance functions**: for a point in space, how far it is from the surface, negative inside. The `ModelRenderer` turns it into pictures, with lamps, soft shadows, occlusion in the creases, highlights and a rim along the outline.

**When to choose it.** When a thing must look three-dimensional from several sides and you would rather describe it in a few dozen lines than paint it eight times. Rendering is far too slow for every frame (it marches up to 160 steps per pixel), so render at the start, on a thread of its own, and keep the pictures, in memory and in a cache file. A flat thing seen from one side is a plain [`Sprite`](#pictures-sprite); a thing seen from above that turns is [`Rotations`](#turned-in-advance-rotations).

**How it works.** `Sdf` has the shapes (sphere, rounded box, capsule, cylinder, torus, cone) and `smoothUnion`; all sit at the origin, and a shape is moved by moving the point the other way. A model overrides `shape(x, y, z)` and reports each part with `part(distance, number)`; the nearest part wins and `material(number, x, y, z)` gives its `Material`: a colour, how much it shines, whether it glows. Model space is -1 … 1, y up, the front facing +z; for a billboard the feet stand at y = -1.

```java title="amade-game-engine · raycast/model/Model.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/raycast/model/Model.java:model"
```

`billboard(model, size, pitch)` renders the model from the front, seen a little from above, into a [`Texture`](45-maps.md#seen-from-inside-gridmap-raycaster-and-lightmap) with holes where the model is not and glow where it glows. `views(frame, frames, directions, size, pitch)` renders every animation frame from every direction, in parallel on all cores, as `Views` for a `Billboard`. `picture(model, width, height, focal, axisX, axisY, placement, lamps)` renders it in perspective, as a camera at the origin looking along +z sees it at a `Placement`: a tool in the hand. The camera's axis may meet the picture anywhere, even outside it, so a picture can be just the window of the view where the tool is.

| Method | What it does |
|---|---|
| `Sdf.sphere`, `box`, `capsule`, `cylinder`, `torus`, `cone`, `smoothUnion` | Distances to shapes, and a joint with a fillet |
| `Model.shape`, `part`, `material` | What the model is made of; called from many threads at once |
| `model.distance(x, y, z)`, `materialAt`, `turned(degrees)` | The model's distance and material at a point; the model turned about its upright axis |
| `Material.paint`, `metal`, `matte`, `light` | Common surfaces; `new Material(rgb, shine, glow)` for any other |
| `new ModelRenderer().lamps(lamps, ambient)` | The lights; `STUDIO` is a key light from the upper left front and a cool fill |
| `billboard(model, size, pitch)`, `views(...)` | Pictures for billboards, from one or from many sides |
| `picture(model, w, h, focal, [axisX, axisY,] at, lamps)` | A picture in perspective; `Lamp.at` for a point light such as a muzzle flash |

!!! warning "Pitfall: a model is asked from many threads"
    The renderer asks one model from every core at once. Keep nothing in fields of a model while `shape` runs; `Model` keeps its own scratch state in a `ThreadLocal`, so `part` is safe.

## Tables with heights: Relief and RampArt

*Package `pinball` · introduced in [70. Pinball](70-pinball.md#step-8-relief) · source: [`Relief`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/pinball/Relief.java), [`RampArt`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/pinball/RampArt.java)*

**What it is for.** A flat picture of a board or table seen from straight above in which every part stands up from the floor with its height, lit from one side, and throws a soft shadow: posts, bumpers, walls, plastic ramps and wire forms. Tilted into the view by [`Perspective`](45-maps.md#a-flat-world-at-a-slant-perspective), it looks like a model.

**When to choose it.** When the parts of a flat world have heights and you draw them with Java2D shapes. The picture is baked once (it takes most of a second for a whole table), so it holds what does not move; the moving parts are drawn every frame through `Perspective.local`. Things that must look solid from every side are [models](#things-in-3d-sdf-model-and-modelrenderer).

**How it works.** A `Relief` covers a rectangle of the world at some pixels per unit and has three layers: `floor()` for the printed artwork, the shadows, and `parts()`. `extrude(footprint, from, to, side, top)` lifts a shape by drawing it a quarter unit higher each time, darker at the foot and lit from the left, with its top on the last slice. `shadow(footprint, h)` puts the footprint, moved away from the light by its height (`SHADOW_X`, `SHADOW_Y` per unit), on the shadow layer. `bake()` blurs the shadows, lays them over the floor and the parts on top. `above()` gives a second relief for a higher level whose shadows fall on the first: a ball rolls between the two pictures, under the ramps.

```java title="amade-game-engine · pinball/Relief.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/pinball/Relief.java:extrude"
```

`RampArt.plastic(relief, track, width, colour, wall)` builds a ramp of clear plastic along a [`Track`](44-movement.md#a-ball-on-a-table-playfield-and-its-parts): its far wall, its bed, its near wall lit by how much it faces the light, three lines of light along the edges, and the supports down to the floor. `RampArt.wire(relief, track, width)` draws a wire form, two rails on hoops.

| Method | What it does |
|---|---|
| `new Relief(left, top, right, bottom, pixelsPerUnit)`, `above()` | The picture's world rectangle and sharpness; a level above it |
| `floor()`, `parts()` | The graphics to draw on, in world units |
| `extrude(footprint, from, to, side, top)`, `shadow(footprint, h)` | A part standing up, and its shadow |
| `bake()`, `dispose()` | The finished picture; free the graphics |
| `RampArt.plastic(relief, track, width, colour, wall)`, `RampArt.wire(relief, track, width)` | A ramp and a wire form along a track |

## A mirror ball: ChromeBall

*Package `pinball` · introduced in [70. Pinball](70-pinball.md#the-chrome-ball) · source: [`ChromeBall`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/pinball/ChromeBall.java)*

**What it is for.** A steel ball that reflects the room: bright above, the floor's colour and its lamps below, two highlights, a darker rim. **How it works.** `new ChromeBall(floor, lamps…)` takes the floor's colour and the colours of the lamps it should mirror; `draw(g, cx, cy, r)` draws it at any radius. Each radius in pixels is rendered once, per pixel, by reflecting the view ray about the sphere's normal, and kept.

```java title="amade-game-engine · pinball/ChromeBall.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/pinball/ChromeBall.java:render"
```

## Lamps and displays: Lamps and DotMatrix

*Package `pinball` · introduced in [70. Pinball](70-pinball.md#step-5-the-game-and-its-rules) and [its backglass](70-pinball.md#step-10-the-backglass-and-its-displays) · source: [`Lamps`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/pinball/Lamps.java), [`DotMatrix`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/pinball/DotMatrix.java)*

**What it is for.** The lights of a machine: named lamps that are off, on, blinking slowly or fast, that chase along a row and all flash together, and that fade up and down like bulbs; and a dot-matrix display of glowing round dots.

**How it works.** The rules set each lamp's `State` by name; `update(seconds)` moves every lamp's brightness towards on or off, quickly up and slower down; `level(name)` is the brightness from 0 to 1 to draw with. `chase(names)` runs a light along a row, `flash(seconds)` blinks them all. A `DotMatrix` is a grid of dots (128 × 32 on a pinball): `line(text, baseline, size, bold)` writes into it with Java2D, `show(big, small)` writes two lines, `graphics()` and `changed()` let you draw anything. `paint(g, x, y, w, h, colour)` draws each dot as a round light with its dark shade, and makes that picture again only when the dots or the size change.

```java title="amade-game-engine · pinball/Lamps.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/pinball/Lamps.java:update"
```

| Method | What it does |
|---|---|
| `set(name, state)`, `set(name, on)`, `state(name)`, `clear()` | What a lamp should do |
| `chase(names)`, `stopChase()`, `flash(seconds)` | Light shows |
| `update(seconds)`, `level(name)` | Brightness, faded |
| `new DotMatrix(columns, rows)`, `clear()`, `line(...)`, `show(big, small)`, `on(x, y)` | The dots and what they say |
| `paint(g, x, y, w, h, colour)` | The display on the screen |

## Sprites at every size: RoadSprite

*Package `drive` · introduced in [61. Cruise](61-cruise.md#step-3-walls-tunnels-and-things-beside-the-road) · source: [`RoadSprite`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/drive/RoadSprite.java)*

**What it is for.** A picture drawn at any size, from a few pixels far away to half the screen close by, faded into fog and darkened at night: the trees, houses and cars of a road, hundreds in a frame.

**When to choose it.** When one picture is drawn at many sizes and tints in every frame. A picture at one size is a [`Sprite`](#pictures-sprite); a thing seen from several sides is a [model](#things-in-3d-sdf-model-and-modelrenderer).

**How it works.** `new RoadSprite(art, worldWidth, glow)` takes a large picture (transparent around the thing), its width in world units, and how much it glows (0 not at all, 1 keeps its colours in fog and dark). `copy(pixels, fog, fogColour, light, shade)` returns a copy at least `pixels` wide: scaled to one of a few fixed widths (each about 1.19 times the one before), halving step by step and then one smooth step so small trees do not shimmer, and tinted to one of 16 steps of fog and 8 of light. The caller draws it a little smaller. All sprites share one budget of pixels (`BUDGET`, about 64 MB); beyond it, the copy used longest ago goes.

```java title="amade-game-engine · drive/RoadSprite.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/drive/RoadSprite.java:copy"
```

| Method | What it does |
|---|---|
| `new RoadSprite(art, worldWidth)`, `new RoadSprite(art, worldWidth, glow)` | A picture standing on the ground at its bottom centre |
| `copy(pixels, fog, fogColour, light, shade)` | The finished copy for this size, fog and light |
| `art()`, `worldWidth()`, `worldHeight()`, `glow()` | The picture and its size in the world |

!!! tip "Draw copies with the nearest pixel"
    A copy is already smooth and at most a fifth too wide, so the last little scale does not need bilinear interpolation, which costs four times as much in Java2D's software loops. `RoadRenderer` sets `VALUE_INTERPOLATION_NEAREST_NEIGHBOR` for its sprites; do the same when drawing copies yourself ([appendix](appendix-java2d.md)).

??? example "Test: copies come in fixed widths and are reused"
    ```java title="amade-game-engine · test/drive/RoadSpriteTest.java"
    --8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/drive/RoadSpriteTest.java:test-copies"
    ```

## Movement in time: Easing and Tween

*Package `anim` · introduced in [11. Solitaire](11-solitaire.md#easing-how-a-movement-feels) · source: [`Easing`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/anim/Easing.java), [`Tween`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/anim/Tween.java)*

**What it is for.** A movement from one value to another in a given time that feels natural: a card thrown onto its pile starts fast and lands gently. An `Easing` maps the share of the time that has passed, 0 to 1, to the share of the way: `LINEAR`, `EASE_OUT` (thrown and landing) and `EASE_IN_OUT` (carried). A `Tween` is one such movement in time: a start, a duration, an easing.

**When to choose it.** For movements with a fixed start and end that the rules do not care about: cards flying, a menu growing, a number counting up. Things the rules move step by step (a ball, a unit) are drawn from their positions instead.

**How it works.** `tween.at(now)` is the eased share at a `System.nanoTime()` value, 0 before the start and 1 from the end on; `Tween.lerp(from, to, t)` turns it into a value; `done(now)` says when it is over. The tween knows nothing of what moves, so one idea animates everything: the cards, their turning over, the bounce at the end ([chapter 11](11-solitaire.md#one-idea-animates-everything)). In a game without a loop, an [`Animator`](41-loop-and-drawing.md#animation-on-the-event-thread-animator) asks the tweens 60 times a second.

```java title="amade-game-engine · anim/Easing.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/anim/Easing.java:easing"
```

## Splinters and sparks: Particles

*Package `fx` · introduced in [21. Bricks](21-bricks.md#step-5-particles) · source: [`Particles`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/fx/Particles.java)*

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
