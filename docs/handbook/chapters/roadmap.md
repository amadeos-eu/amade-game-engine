# The Road Ahead

The handbook grows with the games: a chapter is written when its game is finished. Here is where it stands and where it goes. Each game adds a few new ideas, and the engine grows with them.

## Written

| Chapter | Game | New in the engine |
|---|---|---|
| [0. Setting Up](00-setup.md) | | |
| [1. Why Our Own Engine](01-why-own-engine.md) | | |
| [2. How a Game Is Built](02-structure.md) | | |
| [10. Mines](10-mines.md) | Mines | High scores, game menus, high-score dialogs |
| [11. Solitaire](11-solitaire.md) | Solitaire (Klondike) | Undo and redo, animation (easing, tweens, an animation timer), the game clock |
| [20. Paddle](20-paddle.md) | Paddle (Pong) | Game loop with a fixed time step, active rendering in logical coordinates, full screen, keyboard input and controls, scenes, sound effects and a mixer |
| [21. Bricks](21-bricks.md) | Bricks (Breakout) | Swept box collision, sprites with a cache per screen size, particles, frame times, pixel mode and an overlay layer |
| [22. Snake](22-snake.md) | Snake | A ticker for rules with a pace of their own, random numbers that repeat, replays |
| [23. Rocks](23-rocks.md) | Rocks (Asteroids) | Vectors and angles, outlines that turn, a field without edges, object pools, circle collision, held mouse buttons |
| [24. Swarm](24-swarm.md) | Swarm (Galaga) | Sprite sheets and animation, entities and components, flight paths, data files, a spatial grid, music |
| [25. Maze](25-maze.md) | Maze (Pac-Man) | Tile maps and walking on a grid, state machines, breadth-first search and A* |
| [26. Blast](26-blast.md) | Blast (Bomberman) | Gamepads on Linux and Windows, timers, an event queue for chain reactions |
| [27. Jump](27-jump.md) | Jump (platformer) | Scrolling camera, maps from the Tiled editor, gravity and jumping, parallax, saving progress |
| [28. Hover Cab](28-hover-cab.md) | Hover Cab (flying taxi) | Speech synthesis from phonemes, pixel-exact collision masks, jets and drag, soft and hard touchdowns |
| [30. Towers](30-towers.md) | Towers (tower defence) | Flow fields for many units, the mouse in world coordinates, overlay menus, units and towers as data files, turned sprites |
| [31. Sands](31-sands.md) | Sands (real-time strategy) | Selecting units, group moves and a cache of flow fields, fog of war, minimap, credits and production, an influence map for the computer opponent, saving the whole game |
| [32. Colony](32-colony.md) | Colony (economy and settlement) | Roads from flag to flag and the shortest way over them, carriers, goods and recipes, demand answered by the storehouses, a border that grows, seasons, grazing |
| [33. Town](33-town.md) | Town (city builder) | The isometric view and its drawing order, a minimap turned like the view, networks that carry power from plants to loads, services that reach in circles, the mouse wheel, a ground layer painted only where it changed |
| [40. The Engine at a Glance](40-engine.md) | | Part IV, the engine manual: what each part of the engine is for, when to choose it and how it works |
| [41. Loop, Time and Drawing](41-loop-and-drawing.md) to [49. Rooms, Scripts and Dialogs](49-adventure.md) | | One chapter per subsystem, each section naming the game that introduced it |
| [50. The Seal of the Templars](50-templar.md) | The Seal of the Templars (point-and-click adventure) | Painted rooms and walk areas with the shortest way round obstacles, depth, hotspots and layers, an inventory and story flags, scripts for cutscenes and conversations, speech over the speaker's head |
| [Java2D and X11 Pitfalls](appendix-java2d.md) | | The rules for fast and correct drawing, collected |
| [Glossary](appendix-glossary.md) | | The words of game programming used in this book, each with the chapter that explains it |

## What comes next

With *The Seal of the Templars*, Part V has begun: a story told in painted rooms, written as data and played by the engine's `adventure` package.

The game is complete: Act 2 in La Rochelle and Act 3 in Tomar, with the epilogue, are part of chapter 50 now. The next story of Part V has not been chosen yet. What it adds to the engine will be told in its chapter, and as always in Part IV, the tables of [chapter 40](40-engine.md) and the [glossary](appendix-glossary.md).

Suggestions and corrections are welcome as [issues](https://github.com/datazuul/amade-game-engine/issues) or pull requests. Each page has an edit button at the top.
