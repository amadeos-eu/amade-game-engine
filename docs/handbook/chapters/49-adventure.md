# 49. Rooms, Scripts and Dialogs

The parts of the engine for adventures told in pictures: painted rooms the hero walks through, things one points at, what she carries and what the story remembers, and the scripts in which the story is written. They came with the point-and-click adventure *The Seal of the Templars*, [chapter 50](50-templar.md).

Everything here is in the package `adventure`. Apart from the spoken text on the screen, it uses no AWT, so a room, a walk and a whole conversation can be tested without a window.

| The question | Use |
|---|---|
| Where can people walk, and which way round the table? | `WalkArea` |
| How much smaller is she at the back wall? | `DepthScale` |
| Someone who walks along a way and turns where it bends | `Actor` |
| A room as a data file: picture, floor, hotspots, spots, layers in front | `Room`, `Hotspot` |
| What a click means: look, use, talk, leave, use an item on something | `Interaction` |
| What she carries, what the story remembers | `Inventory`, `Flags` |
| What happens when, written as a play | `Script`, `ScriptRunner` |
| Conversations with answers to choose | `Script` options, `ScriptRunner.choices` |
| Words over the speaker's head, and how long they stay | `SpokenText`, `ReadingTime` |

## Where people walk: WalkArea

*Package `adventure` · introduced in [50. The Seal of the Templars](50-templar.md) · source: [`WalkArea`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/adventure/WalkArea.java)*

**What it is for.** The floor of a painted room is not a grid of tiles but a shape: a polygon round the floor, with holes where the workbench, a well or a sleeping dog stands. `contains(p)` says whether a point is on the floor, `nearest(p)` moves a click off the floor to the nearest point on it, and `path(from, to)` returns the shortest way as a list of points to walk to, one after the other.

**When to choose it.** For every room seen as a picture: the floor follows the painting, however crooked. A room built of tiles, as in Maze or Colony, walks better with the searches of [chapter 46](46-paths-and-decisions.md#which-search).

**How it works.** The shortest way round obstacles in the plane is a string pulled tight: it runs straight to a corner, from corner to corner, and straight on to the goal. Only two kinds of corners can be on it: corners of the floor that point *into* the room (where the wall bends inwards) and corners of the holes. The area finds them once, by the turn at each corner, and joins every two that see each other. That is the **visibility graph**. A search adds the start and the goal, joins them to the corners they see, and runs A* through the graph, guessing by the straight distance. A way that can run straight needs no search at all. Points on the edge of the floor count as inside, so the string may run along a wall it touches.

```java title="amade-game-engine · adventure/WalkArea.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/adventure/WalkArea.java:path"
```

```java title="amade-game-engine · adventure/WalkAreaTest.java"
--8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/adventure/WalkAreaTest.java:test-path"
```

## Smaller at the back: DepthScale

*Package `adventure` · introduced in [50. The Seal of the Templars](50-templar.md) · source: [`DepthScale`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/adventure/DepthScale.java)*

**What it is for.** A painted room has depth: whoever walks to the back wall walks up the screen and must become smaller, and slower, or they seem to climb a wall. `DepthScale(farY, farScale, nearY, nearScale)` gives the scale at both heights and a straight line between them; `at(y)` is the scale of someone whose feet are at `y`. `DepthScale.FLAT` keeps everyone the same size, for rooms seen from the side.

```java title="amade-game-engine · adventure/DepthScale.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/adventure/DepthScale.java:depth"
```

## People: Actor

*Package `adventure` · introduced in [50. The Seal of the Templars](50-templar.md) · source: [`Actor`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/adventure/Actor.java)*

**What it is for.** Someone in a room: a position where the feet are, a direction to look in, and a speed. `walkTo(area, target, then)` walks the shortest way and runs `then` on arrival: walk to the psalter, *then* use it. `place` puts the actor somewhere at once, as when she enters a room.

**How it works.** `update(seconds, depth)` walks on at the speed the depth gives where the actor is: far away, a step covers fewer pixels. Within one step it can pass several points of the way. The actor turns to the side it walks to, and because a floor seen at a slant looks flatter than it is, the side views win until the way runs more than twice as much up or down as across. The actor has no pictures: `facing()`, `isWalking()` and `stride()`, the distance walked in steps of full size, tell the game which frame of the walk cycle to draw. So the feet keep in time with the floor at every depth.

```java title="amade-game-engine · adventure/Actor.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/adventure/Actor.java:update"
```

```java title="amade-game-engine · adventure/ActorTest.java"
--8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/adventure/ActorTest.java:test-depth"
```

## Rooms: Room, Hotspot and layers

*Package `adventure` · introduced in [50. The Seal of the Templars](50-templar.md) · sources: [`Room`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/adventure/Room.java), [`Hotspot`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/adventure/Hotspot.java)*

**What it is for.** A room as a [data file](47-data-and-saving.md#data-files-textdata): the picture behind everything, the floor and its holes, the depth, the **hotspots** (things one can point at, each with an outline, a place to stand and a direction to face), the **spots** where people arrive, and the **layers** that stand in front of people.

```
room workshop
background workshop.png
depth 262 0.85 400 1.15
floor 30 268  610 268  640 400  0 400
hole 214 292  434 292  434 330  214 330
spot start 300 340 up
hotspot psalter 292 238 62 16 stand 300 340 up
hotspot window poly 40 44 190 44 190 214 40 214 stand 120 280 up
exit door 368 104 64 158 stand 400 275 up to corridor workshop-door
layer press.png 20 272 baseline 400
hidden page
```

**How it works.** `hotspotAt(p)` looks from the last hotspot to the first, so a small thing written after a large one lies on top of it; hidden hotspots are skipped. An **exit** is a hotspot that names a room and a spot to arrive at. A **layer** solves the oldest problem of painted rooms: the hero walks behind the book press but in front of the workbench. Each layer has a *baseline*, the height where it touches the floor. The game draws the actors whose feet are above it first, then the layer, then the actors below it. That is the painter's algorithm, the same idea as the [depth sort of the isometric view](45-maps.md#what-covers-what-depthsort). Which hotspots are hidden is the only part of a room that changes while one plays.

```java title="amade-game-engine · adventure/Room.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/adventure/Room.java:parse"
```

## What a click means: Interaction

*Package `adventure` · introduced in [50. The Seal of the Templars](50-templar.md) · source: [`Interaction`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/adventure/Interaction.java)*

The early adventures had a row of verbs at the bottom of the screen: *Look at*, *Pick up*, *Use*, *Talk to*. Later ones let the thing decide. The engine does the same with two buttons: the right one **looks**, the left one **talks** to a person, **leaves** through an exit and **uses** everything else; with an item from the inventory in hand, it uses the item *on* the thing. `Interaction.trigger` turns a click into the words a script block starts with.

```java title="amade-game-engine · adventure/Interaction.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/adventure/Interaction.java:trigger"
```

## What she carries, what the story remembers: Inventory and Flags

*Package `adventure` · introduced in [50. The Seal of the Templars](50-templar.md) · sources: [`Inventory`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/adventure/Inventory.java), [`Flags`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/adventure/Flags.java)*

The `Inventory` holds item names in the order they were found, each once. `Flags` are everything the story remembers, as named numbers: `found-page` is 1 once the page is found, `asked` counts the questions. A flag never set is 0. Besides where everyone stands and which hotspots are hidden, these two are the whole state of an adventure, so a [saved game](47-data-and-saving.md#progress-on-disk-savefile) is little more than `inventory.save()` and `flags.all()`.

## The story as a play: Script and ScriptRunner

*Package `adventure` · introduced in [50. The Seal of the Templars](50-templar.md) · sources: [`Script`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/adventure/Script.java), [`ScriptRunner`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/adventure/ScriptRunner.java)*

**What it is for.** What Maria answers when one looks at the psalter is not Java code. It is a line in a script, a text file that reads like a play:

```
on use bonefolder psalter
  if not found-page
    walk maria to psalter
    face maria up
    anim maria reach
    say maria psalter.found
    gain page
    set found-page
  else
    say maria psalter.empty
  end
end
```

A block starts with `on` and the words of its trigger (`look`, `use`, `talk`, `exit`, `enter` and what else the game starts); `*` stands for any word, so `on look *` answers every look the script has no block for. The commands are `say`, `walk` (to a point, or `to` a hotspot or spot), `face`, `anim`, `gain`, `lose`, `set`, `add`, `clear`, `show`, `hide`, `goto`, `title`, `sound`, `wait`, `dialog`, `exit`, `stop` and `if` … `else` … `end`. A condition is a flag, `has` an item, or a flag compared with a number, each optionally after `not`. Texts are keys into the game's text files, so every language gets its own sentences and the script stays the same.

**How it works.** `Script.parse` reads the blocks once and turns `if` and `else` into jumps, the way a compiler does: an `if` jumps past its part when the condition fails, the end of the `if` part jumps past the `else` part. Every mistake names the file and the line, when the script is read, not when a player finally gets there.

```java title="amade-game-engine · adventure/Script.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/adventure/Script.java:parse"
```

Most steps take time: Maria speaks until the player has read the line, walks until she is there. The `ScriptRunner` hands such a step to the game's `Stage` and asks every frame whether it is done; only then does it go on. A block is thus a **cutscene**, written as a list and played as a story. While it plays, `isBusy()` is true and the player only watches.

```java title="amade-game-engine · adventure/ScriptRunner.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/adventure/ScriptRunner.java:stage"
```

```java title="amade-game-engine · adventure/ScriptRunner.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/adventure/ScriptRunner.java:update"
```

```java title="amade-game-engine · adventure/ScriptRunnerTest.java"
--8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/adventure/ScriptRunnerTest.java:test-cutscene"
```

## Conversations: dialog options

A conversation is a list of options, each a block of its own:

```
option lefort coffee lefort.q.coffee once if has coffee
  say lefort lefort.coffee
  lose coffee
end
option lefort bye dialog.bye
  exit
end
```

The words after `option` are the dialog, the option's name and the key of the text the player sees; `once` makes the option disappear after it was chosen, `if` shows it only while the condition holds. The step `dialog lefort` opens the conversation: `choices()` lists the options available now, `choose(id)` plays one, and afterwards the options are offered again, until a block says `exit`. Then the block that opened the dialog goes on, for a last word at the door. The runner remembers a chosen `once` option as a flag, `chosen.lefort.coffee`, so a saved game remembers it too.

## Words over the head: SpokenText and ReadingTime

*Package `adventure` · introduced in [50. The Seal of the Templars](50-templar.md) · sources: [`SpokenText`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/adventure/SpokenText.java), [`ReadingTime`](https://github.com/amadeos-eu/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/adventure/ReadingTime.java)*

Classic adventures show speech without a balloon: the words float over the speaker's head in the speaker's own colour, with a dark rim so they can be read on any part of the picture. `SpokenText.draw` breaks the text into lines that fit, centres them over the head and keeps them inside the screen. `ReadingTime.of(text)` says how long a line stays when nobody clicks it away: a second to notice it plus the time to read it at fifteen characters a second, at least 1.5 and at most 8 seconds. The [speech synthesis of chapter 48](48-scenes-menus-sound.md#speech-speech-and-phoneme) can mumble along, if a game wants voices without recordings.

```java title="amade-game-engine · adventure/SpokenText.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/adventure/SpokenText.java:draw"
```
