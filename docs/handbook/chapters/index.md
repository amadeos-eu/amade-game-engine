# amadeOS Game Developer's Handbook

This book teaches you how to program games by building them: first a game of Mines, then Solitaire, and step by step action games, a tower defence and finally a small real-time strategy game. While the games grow, so does the engine underneath them, **amade-game-engine**. You will not only use an engine, you will see why each part of it exists, because you write the game that first needs it.

All games are real programs. They run on the [amadeOS](https://github.com/amadeos-eu/amadeos) desktop, follow its look and its colour themes, and they run on Windows and other Linux systems as well. Every piece of code in this book is taken from those games. It compiles, it is tested, and you can check it out and change it.

## Who this book is for

You know **Java**: classes and interfaces, `List` and `Map`, lambdas, `record` and `enum`. You can write a small program and run it.

You have **never written a game**. You do not need to know Swing, Java2D, game loops, sprites, collision or pathfinding. The book introduces each of them at the point where a game needs it.

If you are still learning Java itself, work through a Java beginners' book first and come back. The games will be waiting.

## How the book works

Each game chapter follows the same plan:

1. **What you build**: a screenshot and the rules of the game.
2. **What you will learn**: the new ideas of the chapter.
3. **The rules as a model**: plain Java, no window yet, with tests.
4. **Building it step by step**: drawing, input, the window.
5. **What moves into the engine**, and why only now.
6. **Exercises** to make the game your own.

The chapters build on each other. Read them in order the first time; later the chapter titles and the search help you find things again.

Code excerpts look like this, with the file they come from above them:

```java title="amade-mines-game · domain/Board.java"
--8<-- "amade-mines-game/src/main/java/eu/amadeos/games/mines/domain/Board.java:neighbours"
```

Boxes point out things worth remembering:

!!! tip "Tip"
    A shortcut or a good habit.

!!! warning "Pitfall"
    A mistake that is easy to make, and how to avoid it.

!!! info "Behind the scenes"
    Background you can skip on first reading.

## The games

| Part | Games | You learn |
|---|---|---|
| I. Games that wait for you | Mines, Solitaire | Model and rules, drawing with Java2D, mouse input, Model-View-Presenter, high scores, drag and drop, animation, undo |
| II. Action | Paddle, Bricks, Snake, Rocks, Swarm, Maze, Blast, Jump, Hover Cab, Artillery | Game loop, keyboard, sprites, collision, sound, tile maps, AI, cameras, physics, speech synthesis, destructible ground, ballistics |
| III. Strategy | Towers, Sands, Colony, Town | Mouse in world coordinates, pathfinding for many units, selection, fog of war, saving games, roads and goods, seasons, the isometric view, networks and services |
| V. Stories | The Seal of the Templars | Painted rooms, walking round obstacles, hotspots and an inventory, the story as scripts, conversations, people drawn from a description, a test that plays the story |
| VI. Three Dimensions | Rivet | Raycasting, floors, ceilings and sky, light maps and fog, doors, turning with the mouse, billboards from several sides, models from distance functions, hitscan shots |

Part IV, [the engine manual](40-engine.md), comes after the games of Parts I to III; Part V, *Stories*, and Part VI, *Three Dimensions*, follow it. It describes the engine by subsystem rather than by game: what each part is for, when to choose it, how it works, and which chapter introduced it. Use it to look things up once you have read the games, or when you start a game of your own.

Two appendices close the book: [Java2D and X11 pitfalls](appendix-java2d.md), the rules for fast and correct drawing, and a [glossary](appendix-glossary.md) of the words of game programming, each with the chapter that explains it.

What is written so far and what comes next is listed in [The Road Ahead](roadmap.md).

## Conventions

- The book is about **Java 25**, built with **Gradle**. Commands are for a Linux shell; on Windows use `gradlew.bat` instead of `./gradlew`.
- Names follow the amadeOS projects: repositories `amade-<name>-game`, Java packages `eu.amadeos.games.<name>`, the engine in `eu.amadeos.gameengine`.
- All code is licensed under the [EUPL-1.2](https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12), as is this book. Use it, change it, share it.

Ready? Start with [setting up](00-setup.md) your machine.
