# 44. Movement, Physics and Collision

Things move in a game in a few typical ways: in a straight line at an angle, along a curve someone designed, falling and jumping on tiles, flying on jets, from tile centre to tile centre. Then the game must find out what hit what, often for hundreds of things in every step, and keep track of many things whose behaviour changes while they live. This chapter covers the arithmetic for all of it. Walking on a grid is in [chapter 45](45-maps.md#walking-corridors-walker), finding a way in [chapter 46](46-paths-and-decisions.md).

None of the classes here needs a screen. They are the rules' tools, and they are tested without one. Only `PixelMask` uses AWT, to draw its shapes into an image in memory, which works headless on a build server too.

## Points and directions: Vector and Angles

*Package `math` · introduced in [23. Rocks](23-rocks.md#step-1-vectors) · source: [`Vector`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/math/Vector.java), [`Angles`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/math/Angles.java)*

**What it is for.** A `Vector` is a point or a direction in the plane: where a ship is, how fast it flies and where to, where its nose points. It is an immutable record; every operation returns a new vector, so a vector can be passed around and kept without anyone changing it behind your back. `Angles` brings angles into one range and finds the short way round between two of them.

**How it works.** Angles are radians. Angle 0 points right, along x; because y points **down** on the screen, a positive angle turns **clockwise**, and -π/2 points up. `Vector.ofAngle(angle, length)` makes a vector from an angle, `angle()` gives it back.

| Method | What it does |
|---|---|
| `plus`, `minus`, `times` | Add, subtract, scale |
| `length()`, `lengthSquared()` | The length; squared is enough to compare and needs no root |
| `normalized()`, `limited(max)` | Length 1; no longer than `max`, a speed limit |
| `dot(other)` | Positive if both point roughly the same way, 0 at a right angle |
| `rotated(angle)`, `distance(other)` | Turned round the origin; the distance between two points |
| `Angles.normalize(a)` | The same direction between -π and π |
| `Angles.difference(from, to)` | How far to turn, the short way: from 170° to -170° is +20° |
| `Angles.turnTowards(from, to, maxStep)` | Turns by at most `maxStep`, without overshooting: a turret that follows its target |

```java title="amade-game-engine · math/Angles.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/math/Angles.java:difference"
```

!!! tip "Tip: vectors in the hot loop"
    A `Vector` is a small record, and the JVM makes short-lived records very cheaply. Classes that run hundreds of times per frame, such as `Particles` and `Outline`, still keep plain `double` fields, because they must create nothing at all.

## A field without edges: Wrap

*Package `math` · introduced in [23. Rocks](23-rocks.md#step-3-a-field-without-edges) · source: [`Wrap`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/math/Wrap.java)*

**What it is for.** A field on which what leaves on the right comes back on the left: the space of Rocks. Three things change on such a field, and `Wrap` answers each: a position must be brought back into the field (`wrap`), the distance between two things is the **shortest** one, which may lead across an edge (`delta`, `distance`), and a thing that sticks out over an edge must be drawn a second time on the other side (`forEachCopy`).

```java title="amade-game-engine · math/Wrap.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/math/Wrap.java:delta"
```

**Drawing between steps.** A thing that crossed an edge in the last step must not be drawn flying back across the whole screen. `between(previous, current, alpha)` interpolates along the short way, which may put it just outside the field, and `forEachCopy` then draws it on both sides ([chapter 23](23-rocks.md#drawing-between-steps-across-the-edge)).

**On a grid.** Tile maps wrap the same way with whole cells: `TileMap.step` and the searches of `DistanceMap` and `AStar` arrive on the far side, which makes the tunnel of Maze ([chapter 45](45-maps.md#a-grid-of-tiles-tilemap-cell-and-direction)).

## Curves to fly along: FlightPath

*Package `motion` · introduced in [24. Swarm](24-swarm.md#step-3-flight-paths) · source: [`FlightPath`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/motion/FlightPath.java)*

**What it is for.** A curve designed in advance and flown at an even speed: the swoop of an insect into its formation, the loop of a dive, anything on rails. A path is built from cubic Bézier curves and straight lines:

| Method | What it does |
|---|---|
| `FlightPath.from(x, y).curveTo(c1x, c1y, c2x, c2y, x, y).lineTo(x, y).build()` | Builds a path: a curve bent by two control points, a line |
| `at(distance)` | Where one is after flying `distance` units along it |
| `angleAt(distance)` | The direction of flight there, for the nose |
| `length()`, `start()`, `end()` | Its length in units, its ends |
| `mirrored(width)`, `translated(dx, dy)` | The same path mirrored in the middle of a field, or moved: one path for both sides |

**When to choose it.** When a designer decides how something flies. When the thing decides itself (it chases, it flees), give it a velocity and steer it with `Vector` and `Angles`.

**How it works.** A Bézier curve is easy to compute for a parameter *t* from 0 to 1, but equal steps of *t* are not equal steps along the curve: they crowd where the curve bends sharply. Flown by *t*, an insect would rush and crawl. So the path measures itself once at 48 points per curve, and `at` finds the *t* for a distance in that table by binary search ([chapter 24](24-swarm.md#an-even-speed)).

```java title="amade-game-engine · motion/FlightPath.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/motion/FlightPath.java:measure"
```

Paths are data: Swarm writes them in its stage file, `path swoop-left from 300 -20 curve ...`, and reads them with [`TextData`](47-data-and-saving.md#data-files-textdata).

## Grazing: Wander

*Package `motion` · introduced in [32. Colony](32-colony.md#grazing) · source: [`Wander`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/motion/Wander.java)*

**What it is for.** Something that idles around a home: sheep in a pasture, hens in a yard, people on a square. It walks to a spot, stands there for one to four seconds, and walks to the next; frightened, it runs to the far side of its circle at twice the speed.

**When to choose it.** For life in the picture that the rules do not need to steer. Something that must reach a place follows a path ([46](46-paths-and-decisions.md#which-search)); something that flies a set course follows a `FlightPath`.

**How it works.** A spot is drawn evenly from the circle: an angle, and a radius times the **square root** of a random number. Without the root, half of the spots fall in the inner quarter of the circle, and the herd crowds in the middle. The random numbers come from the game's `SeededRandom`, so grazing repeats in a replay and goes on exactly after loading: `state()` is six numbers, and `restore` sets them.

```java title="amade-game-engine · motion/Wander.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/motion/Wander.java:wander"
```

| Method | What it does |
|---|---|
| `new Wander(homeX, homeY, radius, speed)` | Starts at its home |
| `update(seconds, random)` | One step; true while it walks |
| `flee(x, y)` | Runs away from a point, to the far side of the circle, twice as fast |
| `home(x, y)` | Moves the circle; the next spot is in the new one |
| `x()`, `y()`, `walking()`, `facingLeft()` | For drawing |
| `state()`, `restore(state)` | For a save file |

## Falling and standing: Body and TileCollider

*Package `physics` · introduced in [27. Jump](27-jump.md#step-2-falling-and-standing) · source: [`Body`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/physics/Body.java), [`Solidity`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/physics/Solidity.java), [`TileCollider`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/physics/TileCollider.java)*

**What it is for.** Things that move through a world of tiles and stand on it: a player, an enemy that walks. A `Body` is a box with a velocity and what it touched in its last move: `onGround`, `hitCeiling`, `hitLeft`, `hitRight`, `onPlatform`. A `TileCollider` moves bodies and stops them where the tiles are solid. Each tile is one of three `Solidity` values: `EMPTY` (air), `SOLID` (ground and walls) or `PLATFORM` (holds a body that comes down onto it from above, lets it pass from below, and can be dropped through on purpose).

**When to choose it.** For a side view with gravity: a platformer. A top-down game on a grid walks with a [`Walker`](45-maps.md#walking-corridors-walker) or follows a [flow field](46-paths-and-decisions.md#many-units-one-goal-flowfield); a ball in an open field bounces with [`Sweep`](#boxes-box-and-sweep).

**How it works.** The trick of every platformer: **move one axis at a time**. First sideways, and if a wall is in the way, stop at it; then up or down, and if the ground is in the way, stand on it. Moved together, a body that falls diagonally onto the corner of a tile cannot tell whether it hit a wall or landed; moved apart, each axis has one answer. Each move looks at **every** column or row the body's edge passes, not only where it ends up, so a body falling 20 units in a step through tiles of 16 cannot fall through a floor.

```java title="amade-game-engine · physics/TileCollider.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/physics/TileCollider.java:collider"
```

The collider asks the game what a tile is, through a function `(col, row) -> Solidity`, also for tiles outside the map. So the tiles can be anything (a `TileMap`, a Tiled level, a formula), and the game decides what lies beyond the map: usually walls at the sides and air above and below, so a player can fall out of the world.

| Method | What it does |
|---|---|
| `body.fall(gravity, maxFall, seconds)` | Gravity, up to a speed the eye can follow and the tiles can catch |
| `collider.move(body, seconds, dropThrough)` | Sideways, then up or down; sets the contact flags and stops the velocity towards what was hit |
| `collider.forEachCell(body, visitor)` | Every tile the body overlaps: spikes, coins, water |
| `collider.groundAt(x, body)` | Ground under the body's feet at `x`: an enemy that turns at a ledge |
| `body.place(x, y)` | Somewhere at once, at rest, with no gliding there in the drawing |
| `body.x(alpha)`, `body.y(alpha)` | Between the last two steps, for drawing |

??? example "Test: one axis at a time"
    ```java title="amade-game-engine · test/physics/TileColliderTest.java"
    --8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/physics/TileColliderTest.java:test-axes"
    ```

### Jumping: Jump

*Package `physics` · introduced in [27. Jump](27-jump.md#step-3-running-and-jumping) · source: [`Jump`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/physics/Jump.java)*

The jump of a platformer, with the three small kindnesses that make it feel right rather than exact: **coyote time** (a press just after running off a ledge still jumps), a **jump buffer** (a press just before landing is carried out on landing) and a **variable height** (letting go while rising cuts the speed, so a tap is a hop and a held button the full jump). Call `update(body, pressed, held, seconds)` in each step after the last move and before this step's fall; it returns true in the step a jump starts, for the sound.

The speed of a jump follows from the height it should reach: rising at *v* against gravity *g*, a body stops at the height *v*² / 2*g*. `Jump.speedFor(height, gravity)` turns "four tiles high" into a speed, and `heightOf` goes the other way, so the level designer can rely on it and a test can prove every level can be finished ([chapter 27](27-jump.md#step-5-can-the-level-be-finished)). `launch(body, speed, cuttable)` throws a body up without the button: the bounce off a stomped enemy, a spring.

## Flying on jets: Thrusters, drag and Touchdown

*Package `physics` · introduced in [28. Hover Cab](28-hover-cab.md#step-1-jets-against-gravity) · source: [`Thrusters`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/physics/Thrusters.java), [`Touchdown`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/physics/Touchdown.java), [`Body`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/physics/Body.java)*

**What it is for.** A craft that flies against gravity on jets and has to land: a lunar lander, a flying cab, a jetpack, a helicopter. `Thrusters` know how hard the jets push, up (the main jet), to the sides and down, and how much fuel they burn; `Body.drag` is the air that slows the craft; `Touchdown` decides whether a landing was soft, hard or a crash.

**When to choose it.** When the player steers by **pushing**, not by setting a speed: a jet changes the speed a little in every step for as long as it burns. A character that runs and jumps sets its speed at once, with a [`Jump`](#jumping-jump); a ship in a field without edges turns and pushes with a [`Vector`](#points-and-directions-vector-and-angles) and needs no gravity.

**How it works.** `fire(body, x, y, fuel, seconds)` adds the pushes of the jets that fire to the body's velocity, as accelerations like gravity, and returns the fuel burned, never more than is left: with too little for the whole step, the jets push for the part of the step the fuel lasts. The side and down jets burn in proportion to their push.

```java title="amade-game-engine · physics/Thrusters.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/physics/Thrusters.java:thrusters"
```

A main jet of about twice gravity feels right: the craft rises about as fast as it falls. Without **drag**, a push sets the craft drifting until an equal push the other way stops it, which feels like steering soap. `drag(perSecond, seconds)` takes away that part of the speed each second, smoothly and whatever the step ([chapter 28](28-hover-cab.md#why-a-cab-needs-air)):

```java title="amade-game-engine · physics/Body.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/physics/Body.java:drag"
```

`Touchdown.of(vx, vy, soft, hard)` grades a landing by the larger of the downward speed and twice the sideways one: sliding onto a deck tips a craft over sooner than sitting down hard. On a moving deck, pass the speed against the deck.

| Method | What it does |
|---|---|
| `new Thrusters(up, side, down, burn)` | The pushes in units per second per second, and the fuel the main jet burns in a second |
| `fire(body, x, y, fuel, seconds)` | Fires for a step: `x` -1 left, 1 right; `y` -1 up, 1 down; returns the fuel burned |
| `rate(x, y)` | The fuel a second the jets burn, fired so: for a gauge or a planner |
| `body.drag(perSecond, seconds)` | Air: the velocity loses that part of itself each second |
| `Touchdown.of(vx, vy, soft, hard)` | `SOFT`, `HARD` or `CRASH` |

??? example "Test: jets that burn what is left"
    ```java title="amade-game-engine · test/physics/ThrustersTest.java"
    --8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/physics/ThrustersTest.java:test-thrusters"
    ```

## Shells in flight: Projectile and Ballistics

*Package `physics` · introduced in [29. Artillery](29-artillery.md#step-3-shells-in-the-air) · source: [`Projectile`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/physics/Projectile.java), [`Ballistics`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/physics/Ballistics.java)*

**What it is for.** Things that are fired or thrown and then fly on their own: shells, cannonballs, grenades, arrows, a stone from a catapult. A `Projectile` is a position and a velocity; `Ballistics` is the air it flies through, with gravity, wind and drag, and moves it step by step until it hits something.

**When to choose it.** When nothing steers the thing after it has left: no jets, no player input in flight. A thing that walks or jumps on tiles is a [`Body`](#falling-and-standing-body-and-tilecollider); a craft on jets uses [`Thrusters`](#flying-on-jets-thrusters-drag-and-touchdown).

**How it works.** `Projectile.launch(x, y, degrees, speed)` fires at an angle counted as on a protractor, 0 to the right and 90 up. `step(p, seconds, solid)` first changes the velocity by gravity and wind and takes away the drag, then moves with the new velocity. It does not jump to the end of the step: it looks at points along the way, `sample` units apart, and stops the projectile at the first one where the game's `Solid` says there is something. So a fast shell never passes through a thin ridge. What is solid is the game's business: ground, tanks, walls, all at once.

```java title="amade-game-engine · physics/Ballistics.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/physics/Ballistics.java:ballistics"
```

`fly(start, seconds, maxSeconds, field, solid)` flies a copy to the end: until it hits, leaves the field at a side or below it (above it, it may come back), or the time is up. A flight is nothing but steps of the same size, so a shot tried with `fly` lands exactly where the same shot fired step by step lands. That is what a computer that aims by trying needs ([`ShotSolver`](46-paths-and-decisions.md#aiming-by-trying-shotsolver)).

| Method | What it does |
|---|---|
| `Projectile.launch(x, y, degrees, speed)` | A projectile fired at an angle; `x`, `y`, `vx`, `vy` are public fields |
| `p.x(alpha)`, `p.y(alpha)`, `p.heading()` | Where to draw it between two steps, and which way it points |
| `new Ballistics(gravity, wind, drag, sample)` | The air: accelerations in units per second per second, the share of speed lost per second, the distance between the points looked at |
| `withWind(wind)` | The same air with another wind |
| `step(p, seconds, solid)` | One step; returns the `Hit` or `null` |
| `fly(start, seconds, maxSeconds, field, solid)` | A whole flight on a copy: `HIT`, `LEFT_THE_FIELD` or `TIME_UP`, where and when |

??? example "Test: a tried shot lands where the fired one lands"
    ```java title="amade-game-engine · test/physics/BallisticsTest.java"
    --8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/physics/BallisticsTest.java:test-ballistics"
    ```

## Walking and shooting on a grid of walls: Mover and Hitscan

*Package `raycast` · introduced in [60. Rivet](60-rivet.md#step-6-walking-and-turning) · source: [`Mover`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/raycast/Mover.java), [`Hitscan`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/raycast/Hitscan.java)*

**What it is for.** Moving a round body through a [`GridMap`](45-maps.md#seen-from-inside-gridmap-raycaster-and-lightmap) of walls and doors, sliding along walls instead of sticking to them; and shots that arrive at once along a line, stopped by the first wall or the first target.

**When to choose it.** In a first-person game on a grid, or any game seen from above whose bodies are circles among square walls. Bodies that fall and jump on tiles use [`Body` and `TileCollider`](#falling-and-standing-body-and-tilecollider); shots that fly visibly are [`Projectile`](#shells-in-flight-projectile-and-ballistics)s or the game's own.

**How it works.** `Mover.slide(map, x, y, dx, dy, radius)` cuts the move into pieces no longer than half the radius and moves each along x, then along y, each stopped if the circle would overlap a blocked cell. A move into a wall at an angle keeps its part along the wall. `Mover.overlaps` is the test itself. `Hitscan.shoot(map, x, y, angle, range, targets)` casts a ray for the wall, then checks every target, a circle seen from above, and returns the nearest one in front of the wall, or where the shot met the wall. Height does not count. `Hitscan.enter` is the ray-circle test, for a game's own use.

```java title="amade-game-engine · raycast/Hitscan.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/raycast/Hitscan.java:hitscan"
```

| Method | What it does |
|---|---|
| `Mover.slide(map, x, y, dx, dy, radius)` | Where the body ends up, as a `Position` |
| `Mover.overlaps(map, x, y, radius)` | Whether a body there would overlap a wall or a closed door |
| `Hitscan.shoot(map, x, y, angle, range, targets)` | The `Hit`: distance, point, target, or `wall()` |
| `Hitscan.enter(x, y, dx, dy, cx, cy, r)` | Where a ray enters a circle, or -1 |

## Which collision test?

| The things | Use | Because |
|---|---|---|
| Boxes that do not turn, one of them fast: a ball and bricks | `Sweep` | Tests the whole way of a step; a fast ball cannot pass through a brick |
| Boxes at rest, or slow: a player and a coin | `Box.overlaps` | One comparison per axis |
| Round things, things that turn | `Circle` | A circle looks the same at every angle; works across the edge of a `Wrap` |
| A body against tiles | `TileCollider` | Above |
| Shapes that are neither boxes nor circles: a cab and a neon sign, a rock face | `PixelMask` | Exact to the pixel, 64 pixels per comparison |
| Many things against many | `Grid`, then one of the above | Each thing meets only its neighbours |

### Boxes: Box and Sweep

*Package `collision` · introduced in [21. Bricks](21-bricks.md#step-2-collision-of-moving-boxes) · source: [`Box`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/collision/Box.java), [`Sweep`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/collision/Sweep.java)*

A `Box` is an axis-aligned rectangle, its top left corner at `x`, `y`; boxes that only touch do not overlap. `Sweep.sweep(moving, dx, dy, target)` tests a **moving** box against one at rest over the whole way of a step and returns where they first touch: the share of the step, 0 to 1, and the face that was hit as a normal, the direction to bounce. Without it, a ball in front of a brick in one step and behind it in the next would pass through ("tunnelling").

The trick is to shrink the moving box to a point and grow the target by the moving box's size (their *Minkowski sum*). Then the question is only where a straight line enters a rectangle: for each axis, when does the point enter the target's range and when does it leave it?

```java title="amade-game-engine · collision/Sweep.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/collision/Sweep.java:sweep"
```

A ball that hits a brick early in a step moves on, bounced, for the rest of it, and may hit a second brick in the same step: the game sweeps again with what is left ([chapter 21](21-bricks.md#bouncing-within-a-step)). Boxes that already overlap at the start give no hit; separate them first.

### Circles: Circle

*Package `collision` · introduced in [23. Rocks](23-rocks.md#step-4-circles) · source: [`Circle`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/collision/Circle.java)*

Two circles overlap when their centres are closer than both radii together, compared squared, without a root. `overlaps(other, wrap)` measures across the edges of a field without edges; `contains(x, y)` is for a click on a round thing. A turning figure's hit circle is its `Outline.radius()`, or a little less, so that only a real touch counts.

### Exact to the pixel: PixelMask

*Package `collision` · introduced in [28. Hover Cab](28-hover-cab.md#step-3-collisions-as-exact-as-the-pictures) · source: [`PixelMask`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/collision/PixelMask.java)*

**What it is for.** Collisions as exact as the pictures, for shapes a box or a circle fits badly: a cab with thin legs, a slanted strut, a round sign, the jagged wall of a cave. A `PixelMask` says which pixels of a thing are solid, one bit each; two masks collide when they have a solid pixel in the same place.

**When to choose it.** When players would see the difference: a near miss that a box would count as a hit. For many small things, or things that turn every step, circles are cheaper; a mask is made once and does not turn (keep one per direction or per few degrees, made in advance).

**How it works.** A row is kept as `long`s of 64 pixels, the leftmost in the lowest bit. To compare a row of one mask with a row of another, each of its longs is shifted to where it lies in the other row, where it falls into one long or across two, and ANDed: 64 pixels in one instruction. A cab of 32 × 16 pixels against a whole scene costs a few dozen ANDs. `firstOverlap` also returns the first pixel where both are solid, top row first, to tell what was hit.

```java title="amade-game-engine · collision/PixelMask.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/collision/PixelMask.java:overlaps"
```

The easiest masks to get right are drawn: `PixelMask.painted(width, height, g -> ...)` fills the same shapes the painter draws into an image without antialiasing, so what is seen is what collides. `PixelMask.of(image)` takes the opaque pixels of a picture.

| Method | What it does |
|---|---|
| `PixelMask.painted(w, h, painter)`, `PixelMask.of(image)` | A mask of shapes, or of a picture's pixels with alpha 128 or more |
| `overlaps(other, dx, dy)`, `firstOverlap(other, dx, dy)` | Whether, and where first, `other` at `dx`, `dy` touches this mask |
| `anyIn(x, y, w, h)` | Whether a box has a solid pixel: a mask against something that is a box |
| `solid`, `set`, `clear`, `add(other, dx, dy)`, `count()` | Single pixels; another mask added in |

??? example "Test: a round sign is hit only where it is round"
    ```java title="amade-game-engine · test/collision/PixelMaskTest.java"
    --8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/collision/PixelMaskTest.java:test-mask"
    ```

### Many things: Grid

*Package `collision` · introduced in [24. Swarm](24-swarm.md#step-5-collisions-on-a-grid) · source: [`Grid`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/collision/Grid.java)*

**What it is for.** Finding the things near a place without looking at all things. Testing each of *n* things against each other costs *n*² / 2 tests: 40 insects and 2 shots are 80, nothing; 2,000 shots, sparks and units are two million, in every step. With a grid, each thing meets only the few in its neighbourhood.

**How it works.** Each step, the game empties the grid (`clear()`) and puts each thing into every cell its circle touches (`add(thing, x, y, radius)`). A question, `forEachNear(x, y, radius, visitor)`, then visits each thing in the cells around a place, each once; whether a candidate really touches is the caller's test. Choose the cell a little larger than most things, so a thing touches one to four cells. Things outside the field count to the cells at its edge. The entries live in arrays that only grow, so after the first steps, filling and asking create no objects.

```java title="amade-game-engine · collision/Grid.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/collision/Grid.java:grid"
```

Besides collisions, the grid answers "who is near?" in general. Sands uses it to push vehicles apart that drive into each other, so that a group spreads out instead of meeting in one point.

## A fixed number of things: Pool

*Package `pool` · introduced in [23. Rocks](23-rocks.md#step-5-four-shots-from-a-pool) · source: [`Pool`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/pool/Pool.java)*

**What it is for.** A fixed number of reusable objects, such as shots, all made at the start: `obtain()` takes one (or `null` when all are in use), `release(index)` or `releaseIf(done)` gives it back.

**When to choose it.** When the capacity is a **rule**: the ship of the old arcade games could have four shots in the air, no more, and a full pool says so. A game that creates nothing while it runs also gives the garbage collector nothing to do. Where there is no natural limit, a plain list of the game's own objects is simpler, as Towers and Sands keep their shots.

**How it works.** The objects in use are always the first `size()` ones. Giving one back moves the last one in use into its place, as `Particles` does with its rows, so the order changes: walk backwards when you give back while walking, as `releaseIf` does.

```java title="amade-game-engine · pool/Pool.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/pool/Pool.java:pool"
```

## Entities and components

*Package `entity` · introduced in [24. Swarm](24-swarm.md#step-2-entities-and-components) · source: [`Entity`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/entity/Entity.java), [`Entities`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/entity/Entities.java)*

**What it is for.** Things whose behaviour changes while they live. An `Entity` is little more than a number and a bag of **components**: a body, a flight along a path, a place in a formation, hit points. What it is and what it does follows from the components it has right now. An insect that leaves the formation to dive gets a flight, and loses it again when it is back; its class never changes, its components do. Each rule, a *system*, walks over the entities that have the components it needs: the flight system moves whatever flies, insect, shot or ship, and never asks which it is.

**When to choose it.** When things share behaviour in changing combinations, as the insects, their shots and the fighter of Swarm do. When there are a few kinds of things, each with a fixed set of rules, plain classes are clearer: Towers and Sands keep their creeps, units and buildings as classes of their own, in lists.

**How it works.** A component is any object, looked up by its class; an entity holds at most one of each class. `Entities.create()` and `entity.destroy()` take effect only at `flush()`, which the game calls once per step after all systems ran, so no system changes the list under another's feet. The entities stay in the order they were made, so systems visit them in the same order in every run, and a game with a fixed seed repeats exactly.

```java title="amade-game-engine · entity/Entities.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/entity/Entities.java:entities"
```

| Method | What it does |
|---|---|
| `entity.add(component)`, `get(Type.class)`, `has`, `remove` | The components |
| `entities.forEach(Type.class, (entity, a) -> ...)` | A system over one component |
| `entities.forEach(A.class, B.class, (entity, a, b) -> ...)` | A system over entities that have both |
| `count(Type.class)`, `with(Type.class)` | How many have it; a list of them |
| `flush()`, `clear()` | Takes in the new and drops the destroyed; removes all |
