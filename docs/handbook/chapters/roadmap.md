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

## Planned

### Part II: Action

| Chapter | Game (in the style of) | New in the engine |
|---|---|---|
| 20 | **Paddle** (Pong) | Game loop with a fixed time step, `BufferStrategy`, keyboard input, scenes (menu, game, pause), scaled low-resolution drawing, full screen |
| 21 | **Bricks** (Breakout) | Sprites, rectangle collision, levels from files, sound effects |
| 22 | **Snake** | A tick for the rules apart from the frame rate, deterministic random numbers |
| 23 | **Rocks** (Asteroids) | Vectors, rotation, wrap-around, object pools, circle collision, particles |
| 24 | **Swarm** (Galaga) | Sprite sheets and animation, entities and components, flight paths, waves, spatial grid, music |
| 25 | **Maze** (Pac-Man) | Tile maps, movement on a grid, AI as state machines, breadth-first search and A* |
| 26 | **Blast** (Bomberman) | Two players on one keyboard, gamepads, timers, events, chain reactions |
| 27 | **Jump** (platformer) | Scrolling camera, maps from the Tiled editor, gravity and jumping, parallax, saving progress |

### Part III: Strategy

| Chapter | Game (in the style of) | New in the engine |
|---|---|---|
| 30 | **Towers** (tower defence) | Mouse in world coordinates, overlay menus, units and towers as data files, flow fields for many units |
| 31 | **Sands** (Dune II) | Selecting units, group pathfinding, fog of war, minimap, resources and buildings, computer opponent, saving the whole game |

After Sands, an economy game in the spirit of *The Settlers* and a city builder in the spirit of *Pharaoh* need little new engine, but a lot of simulation: production chains, transport, population, trade.

## Appendices, also planned

- **Engine reference**: every package of amade-game-engine, with the chapter that introduced it.
- **Java2D and X11 pitfalls**: stutter without `Toolkit.sync()`, key repeat on X11, HiDPI.
- **Glossary**.

Suggestions and corrections are welcome as [issues](https://github.com/datazuul/amade-game-engine/issues) or pull requests. Each page has an edit button at the top.
