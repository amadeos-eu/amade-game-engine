# 46. Paths, Groups and Decisions

How things find their way, and how the computer decides what to do. The searches of this chapter work on any grid the game describes with one function, "may this tile be entered?"; the decisions are small tools that keep a character's modes in order, give the computer a picture of the battle, and let consequences spread in a fixed order. Choosing units with the mouse is here too, because it is the start of every order in a strategy game, and so is the order of turns in a game played in turns.

## What may be entered: Passable

*Package `path` · introduced in [25. Maze](25-maze.md#step-5-breadth-first-search-the-way-home) · source: [`Passable`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/path/Passable.java)*

Every search asks the game, not the map, whether a tile can be entered: `(col, row) -> boolean`. The same map gives different answers to different walkers. A ghost flying home may pass the door of its house, which is a wall to everyone else; a vehicle of Sands drives on sand and rock, the burrower only on sand; a tower that is only planned closes its tile for the question "would the way stay open?". So the searches never need to know what a tile is.

## Which search?

| The question | Use | Cost |
|---|---|---|
| The way from everywhere to one place, in four directions: the ghosts' way home, the nearest safe tile | `DistanceMap` | One breadth-first search for every tile at once |
| The cheapest way from here to there, with costs per tile: a computer player that avoids danger | `AStar` | One search per question, looking first towards the goal |
| The way from everywhere to one place, in eight directions, for many units walking freely | `FlowField` | One search for every tile; each unit only looks up an arrow |
| Many such goals, one per order | `FieldCache` | A flow field per goal, kept until the map changes |
| The next road for a ware on a network of roads and flags | `RoadNetwork.next` | One search from the goal over the flags, kept until the network changes |
| Where each unit of a group stops | `Formation.slots` and `assign` | A small search around the target |
| May a unit drive straight from here to there? | `Formation.clearLine` | One walk along the line |

The rule of thumb: when **many** things want to reach **one** place, search once from that place, outwards (`DistanceMap`, `FlowField`). When **one** thing wants to reach a place of its own, search from the thing towards the goal (`AStar`).

## One goal, every tile: DistanceMap

*Package `path` · introduced in [25. Maze](25-maze.md#step-5-breadth-first-search-the-way-home) · source: [`DistanceMap`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/path/DistanceMap.java)*

**What it is for.** For every tile of a map, the number of steps to the nearest of one or more goals, found by **breadth-first search**: the goals have distance 0, their open neighbours 1, the neighbours of those 2, a wave that spreads through the corridors until it has filled them. `downhill(col, row)` is then the first step on a shortest way from anywhere. Search from where one stands instead, and the distances of the dots say which is the **nearest** of many things.

**How it works.** A queue in an `int` array, four directions in the order of `Direction`, steps over an edge of the map arriving on the far side as in `TileMap.step`. Of equally good steps downhill, the first direction wins, so the result never depends on chance. `visited()` is what the search cost. Maze sends its eaten ghosts home this way, and Blast's computer players find the nearest tile no flame will reach ([chapter 26](26-blast.md#a-map-of-danger)). The map need not be a level of tiles: Hover Cab's autopilot lays a grid of possible cab positions four units apart over a free-form scene, marks each open or not with a pixel mask, and flies down the distances to its pad ([chapter 28](28-hover-cab.md#step-7-an-autopilot)).

```java title="amade-game-engine · path/DistanceMap.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/path/DistanceMap.java:distances"
```

## The cheapest way for one: AStar

*Package `path` · introduced in [25. Maze](25-maze.md#step-6-a-the-computer-player) · source: [`AStar`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/path/AStar.java)*

**What it is for.** The cheapest way from one tile to another when tiles cost differently to enter: a way past a ghost can cost more than a longer way round it. `AStar.find(map, from, to, cost)` returns a `Path`: the cells, the steps, the cost and how many tiles the search looked at; `found()` and `first()` for the step to take now.

**How it works.** Breadth-first search spreads evenly in all directions and counts steps. A* adds a cost per tile and looks first at the tiles that seem to lead to the goal: of all tiles at its border it takes the one with the smallest *g* + *h*, the cost so far plus a guess of the cost still to come. The guess is the number of steps without walls, across and along, the short way round over the edges. As long as every tile costs at least 1, the guess is never too high, and the way found is always one of the cheapest. A tile one cannot enter costs `Double.POSITIVE_INFINITY`.

```java title="amade-game-engine · path/AStar.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/path/AStar.java:astar"
```

The two searches work well together. The pilot of Maze, the computer player, plans again on every new tile: a `DistanceMap` from where it stands finds the nearest dot, and A* finds the way there, with costs that rise near the ghosts ([chapter 25](25-maze.md#the-pilot)).

## Many units, one goal: FlowField

*Package `path` · introduced in [30. Towers](30-towers.md#step-1-the-flow-field) · source: [`FlowField`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/path/FlowField.java)*

**What it is for.** For every tile of a map, which way leads to the goal: a field of arrows that any number of units follow at once. A hundred orcs cost one search, not a hundred, and a new tower costs one new search, however many orcs must find a new way round it. The field also answers whether every tile still reaches the goal, which is how Towers refuses a tower that would close the way ([chapter 30](30-towers.md#may-a-tower-stand-here)).

**How it works.** Units walk in **eight** directions. A step to a side costs 10, a diagonal one 14 (√2 × 10), so the distances are those of a real walk and not of a chess king. Because the steps differ, the search is Dijkstra's, with a queue sorted by distance, a binary heap of `long`s with the distance above and the tile below. A diagonal step is allowed only where both tiles beside it are open: no unit squeezes between two walls that touch at a corner. Unlike `DistanceMap`, a flow field has edges.

```java title="amade-game-engine · path/FlowField.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/path/FlowField.java:search"
```

**Walking it.** `heading(x, y)`, positions in tiles (2.5 is the middle of column 2), is the direction, length 1, towards the middle of the next tile; the zero vector at the goal's middle or where no way leads. A unit that walks this way never touches a closed tile, and that holds for any point of the next tile, so a unit may aim a little to the side of the middle to walk beside the others instead of behind them ([chapter 30](30-towers.md#walking)).

| Method | What it does |
|---|---|
| `FlowField.toward(width, height, open, goals...)` | The field towards the nearest goal |
| `heading(x, y)` | Where a unit at `x`, `y` (in tiles) walks |
| `next(col, row)` | The next tile on a shortest way; `null` at a goal and where no way leads |
| `distance(col, row)`, `reaches(col, row)`, `isGoal(col, row)` | The cost, 10 per tile; whether a way leads to a goal |
| `visited()` | How many tiles the search settled |

??? example "Test: a wall in the way"
    ```java title="amade-game-engine · test/path/FlowFieldTest.java"
    --8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/path/FlowFieldTest.java:test-blocked"
    ```

### Many fields: FieldCache

*Package `path` · introduced in [31. Sands](31-sands.md#one-field-for-the-group) · source: [`FieldCache`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/path/FieldCache.java)*

In a strategy game every order has a goal of its own. `toward(goal)` gives the field towards a tile, from the cache or searched now: a group shares one field, two groups sent to the same place share it too, and the harvesters, all driving home to the same refinery, share theirs for the whole game. When a building goes up or comes down, `invalidate()` forgets every field, and each is searched again when it is next asked for. Only the 32 fields used last are kept, an access-ordered `LinkedHashMap` that drops its eldest entry, because a field of a map of 64 × 64 is 20 KB. `searches()` counts what the cache did not save: a test proves that six tanks with one order cost one search.

## Roads between flags: RoadNetwork

*Package `road` · introduced in [32. Colony](32-colony.md#step-1-roads-from-flag-to-flag) · source: [`RoadNetwork`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/road/RoadNetwork.java)*

**What it is for.** Roads that the player lays from flag to flag, and the way of a ware over them. A road joins two flags and runs over the cells between; a new flag on a road makes two roads of it. A ware at a flag asks which road it takes next to reach the flag of the house it is meant for.

**When to choose it.** When things travel on a network the player builds, in hops between fixed points: the roads of a settlement, rails between stations, pipes between pumps. Things that walk freely over the map use the searches above.

**How it works.** The flags are the nodes of a graph and the roads its edges, each as long as its steps. The way to a goal is found by Dijkstra's search **from the goal outwards**, once, which gives every flag at once its distance and the road it takes: `next` is then a lookup. The searches are kept for the 64 goals used most recently and forgotten when a flag or road is added or removed. Ties go to the lower number, so the answer never depends on the order of a hash map, and a loaded game goes on exactly. Flags and roads may be added with a given number, and `restore` sets the counters, for loading.

```java title="amade-game-engine · road/RoadNetwork.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/road/RoadNetwork.java:next"
```

| Method | What it does |
|---|---|
| `addFlag(cell)`, `removeFlag(flag)` | A new flag and its number; removes one with its roads |
| `connect(from, to, cells)`, `remove(road)` | A road over the cells from one flag to another, checked; removes one |
| `split(cell)` | A flag in the middle of a road, two roads of one; the new flag |
| `next(from, to)`, `distance(from, to)`, `connected(from, to)`, `way(from, to)` | The next road on the shortest way, its length, whether there is one, all of its roads |
| `flagAt(cell)`, `roadAt(cell)`, `cell(flag)`, `road(id)` | What is where, for the mouse and the drawing |
| `flags()`, `roads()`, `roads(flag)` | Everything, and the roads at one flag |
| `version()`, `searches()` | Counts changes for the drawing; counts searches for tests |
| `nextFlag()`, `nextRoad()`, `restore(nextFlag, nextRoad)` | The counters, for a save file |

## Groups on the move: Formation

*Package `path` · introduced in [31. Sands](31-sands.md#a-slot-for-each) · source: [`Formation`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/path/Formation.java)*

**What it is for.** Where a group of units stands when it arrives. Five tanks sent to one tile cannot all stand on it: each needs a **slot** of its own near the tile the player clicked, and each must take the slot that suits it, or they cross each other's way at the end and jostle.

**How it works.** In three static methods:

- `slots(target, count, width, height, open)`: the open tiles nearest the target, counted in steps from it by a breadth-first search in eight directions, so a slot never lies behind a wall. A closed target, such as a click on a rock, is replaced by the nearest open tile.
- `assign(xs, ys, slots)`: which slot each unit takes. The closest pair of unit and slot first, then the closest of what is left. That is the *greedy* assignment, not the best one there is (the Hungarian method), but it is close for groups of a dozen, it is a few lines, and the paths it gives seldom cross.
- `clearLine(x0, y0, x1, y1, width, height, open)`: whether a unit can drive straight from one point to another. It walks the line through the grid tile by tile, as a ray is traced through a grid, and refuses to cut a corner. A unit follows the group's flow field until it is near the goal and then drives straight to its own slot ([chapter 31](31-sands.md#driving)).

```java title="amade-game-engine · path/Formation.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/path/Formation.java:assign"
```

Slots keep a group apart at the end; on the way, the game pushes overlapping units apart itself, with a [`Grid`](44-movement.md#many-things-grid).

## Choosing units: Selection and BoxSelect

*Package `select` · introduced in [31. Sands](31-sands.md#step-1-choosing-and-commanding) · source: [`Selection`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/select/Selection.java), [`BoxSelect`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/select/BoxSelect.java)*

**What it is for.** The conventions of strategy games since the nineties. A click picks one thing, a drag opens a box and picks all in it, Shift adds to the choice (and a Shift-click on something already picked takes it out), Ctrl and a number keep the choice as a group that the number alone calls back. `Selection` keeps what is picked and the nine groups; `BoxSelect` tells a click from a box.

**How it works.** `BoxSelect` is arithmetic in screen or world coordinates: `press`, `move` and `release` with the mouse's position, in the update step. A hand that clicks moves the mouse a little, so the press only becomes a box after more than 4 units of movement (`SLOP`). `release` returns a sealed `Result`, a `Click` or a `Box` with sorted edges, so the game's `switch` must handle both, and Java checks that it does. While `dragging()`, the game draws the box.

```java title="amade-game-engine · select/BoxSelect.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/select/BoxSelect.java:box"
```

`Selection` keeps the picked things in a `LinkedHashSet`: none twice, in the order of picking, so the panel shows the first picked first. The game decides what may be picked: in Sands only the player's own vehicles, while a click on an enemy only shows it.

| Method | What it does |
|---|---|
| `pick(things, adding)`, `click(thing, adding)`, `clear()` | Picks; with `adding`, as with Shift |
| `store(number)`, `recall(number)`, `group(number)` | Groups 1 to 9 |
| `keepOnly(alive)` | Takes the destroyed out of the selection and every group: call it after every step |
| `all()`, `contains`, `size`, `isEmpty` | What is picked |
| `version()` | Changes with the selection: a panel redraws only then |

## One state at a time: StateMachine

*Package `ai` · introduced in [25. Maze](25-maze.md#step-3-ghosts-as-state-machines) · source: [`StateMachine`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/ai/StateMachine.java)*

**What it is for.** Something that is always in exactly one of a few states, an enum, and changes between them only along ways allowed in advance: a ghost that waits, hunts, flees or flies home; a door; the phases of a round.

**When to choose it.** When the rules of a thing depend on its mode and the modes are few. The table of allowed changes is the point: the classic bug is a combination nobody meant, a ghost eaten while it already flies home, and with a table such a change is an `IllegalStateException` the moment it happens, naming both states, instead of a strange sight minutes later.

**How it works.** `allow(from, to...)` fills the table, `to(state)` changes and calls the `onChange` listener with the old and the new state (to start a sound, turn round, reset something), `update(seconds)` lets time pass and `seconds()` says how long the thing has been in its state. `reset(state)` goes back without asking the table, for a new level.

```java title="amade-game-engine · ai/StateMachine.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/ai/StateMachine.java:machine"
```

## Strength on the map: InfluenceMap

*Package `ai` · introduced in [31. Sands](31-sands.md#whom-to-attack-the-influence-map) · source: [`InfluenceMap`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/ai/InfluenceMap.java)*

**What it is for.** The computer opponent's picture of the battle. Every unit and building spreads its strength around it, full on its own tile and less further away, nothing beyond its reach; added up, the map shows where a side is strong and where it is weak, without looking at single units. With one per side, an opponent's questions become arithmetic: where is the enemy strongest (`strongest()`)? Which of its buildings is defended worst (`weakest(candidates)`)? Is this crystal field safe for a harvester (`at(col, row)` is 0)?

**How it works.** `clear()`, then `add(col, row, strength, reach)` for each thing: the strength falls in a straight line to nothing at `reach` tiles, as the crow flies. Sands builds the map anew each time the computer chooses whom to attack, with the damage per second of each weapon as its strength. What an influence map decides can be **seen**: draw it over the map, as Sands does with F3, and the computer's choice is no longer a mystery.

## Aiming by trying: ShotSolver

*Package `ai` · introduced in [29. Artillery](29-artillery.md#step-5-the-computer-aims) · source: [`ShotSolver`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/ai/ShotSolver.java)*

**What it is for.** A computer that aims a shot set by two numbers, an angle and a power, when no formula says which: a hill in the way, wind, a target higher than the gun. Instead of solving the physics, it **tries** shots in thought with the game's own rules and keeps the best.

**How it works.** The game hands `solve` a `Trial`: for an angle and a power, how far from the target the shot lands, usually by flying it with [`Ballistics.fly`](44-movement.md#shells-in-flight-projectile-and-ballistics) over the real ground. `solve` tries a coarse grid of angles and powers, then `rounds` times looks around the best one found with half the steps. Artillery's search is about five hundred flights, about ten milliseconds. A good shot is never alone, since a slightly different angle lands slightly elsewhere, so the coarse grid does not miss it. The search is exact; a computer opponent adds its mistakes afterwards, of a size that is its level ([chapter 29](29-artillery.md#a-level-is-the-size-of-a-mistake)).

```java title="amade-game-engine · ai/ShotSolver.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/ai/ShotSolver.java:solver"
```

??? example "Test: over a hill, against the wind"
    ```java title="amade-game-engine · test/ai/ShotSolverTest.java"
    --8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/ai/ShotSolverTest.java:test-solver"
    ```

## Events that cause events: EventQueue

*Package `event` · introduced in [26. Blast](26-blast.md#step-5-events-and-chain-reactions) · source: [`EventQueue`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/event/EventQueue.java)*

**What it is for.** Consequences that spread: a bomb explodes, its flames reach a second bomb, which explodes too, and its flames reach a player. Each event is handled by one method of the rules, and the handler may `post` further events; `process(handler)` handles them all, in the order they were posted, until none is left.

**When to choose it.** When one event can cause others in the same step, and the order matters. Games in which an event only reports what happened (a brick broke, play a sound) need no queue: the rules collect such events in a list for the user interface, as every game does from Paddle on.

**How it works.** The order is first in, first out, as in breadth-first search: everything the first event caused is handled before anything those events caused. A chain reaction runs in waves, the bombs hit by the first blast before those hit by theirs, and the order never depends on how deep a handler happens to call itself ([chapter 26](26-blast.md#before-and-after)). Every handled event stays in `happened()` until `clear()`, so the user interface can show and play what happened in a step. A chain without end would hang the game; after `limit` events in one `process`, 10,000 by default, the queue gives up with an exception that names the last event.

```java title="amade-game-engine · event/EventQueue.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/event/EventQueue.java:queue"
```

## Whose turn: TurnOrder

*Package `turn` · introduced in [29. Artillery](29-artillery.md#whose-turn) · source: [`TurnOrder`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/turn/TurnOrder.java)*

**What it is for.** Games played in turns: whose turn it is, who comes next, and when it is over. The players are anything (tanks, seats, colours); a test says whether one is still in play.

**How it works.** `new TurnOrder(players, inPlay, first)`. `current()` is whose turn it is, `next()` passes the turn to the next player still in play, in order and round again, skipping the others; `decided()` is true when one or none is left, and `winner()` names the one. Being in play is asked each time, never stored, so a player destroyed in someone else's turn simply drops out. `turns()` counts the turns taken.

```java title="amade-game-engine · turn/TurnOrder.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/turn/TurnOrder.java:turns"
```

??? example "Test: players out of the game are skipped"
    ```java title="amade-game-engine · test/turn/TurnOrderTest.java"
    --8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/turn/TurnOrderTest.java:test-turns"
    ```
