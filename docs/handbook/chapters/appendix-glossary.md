# Appendix: Glossary

The words this book uses for the ideas of game programming, each with a short explanation and where to read more: first the chapter where the idea came up in a game, then the section of the [engine manual](40-engine.md) that describes the engine's part for it. The engine's classes are listed by purpose in [chapter 40](40-engine.md#i-need-use).

## A

**A\* (A star).** A search for the cheapest way from one tile to another when tiles cost differently to enter. It looks first at the tiles that seem closest to the goal, so it finds the way while looking at few tiles. [Maze](25-maze.md#step-6-a-the-computer-player), [manual](46-paths-and-decisions.md#the-cheapest-way-for-one-astar).

**Action.** What a player wants to do, such as "move up" or "fire", as the rules see it. Keys and gamepad buttons are bound to actions, so the rules never ask for a key. [Paddle](20-paddle.md#keys-and-actions), [manual](43-input.md#actions-instead-of-keys-controls).

**Active rendering.** The game draws a frame when its loop wants one, into a hidden buffer that is then shown at once, instead of asking Swing to `repaint()` some time later. [Paddle](20-paddle.md#step-2-drawing-actively), [manual](41-loop-and-drawing.md#the-canvas-gamecanvas-and-rendermode).

**Alpha (interpolation).** How far the time of a frame lies between the last two steps of the rules, from 0 to 1. Things that move are drawn that share of the way from their previous to their current position, so movement looks smooth at any frame rate. [Paddle](20-paddle.md#drawing-between-steps), [manual](41-loop-and-drawing.md#the-game-loop-gameloop-and-fixedstep).

**Animation.** A picture that changes over time: the frames of a sprite sheet shown one after the other, or a thing moved from one place to another by a tween. [Solitaire](11-solitaire.md#step-7-animation), [Swarm](24-swarm.md#animation), [manual](42-sprites.md#which-frame-when-animation).

**Antialiasing.** Smooth edges for shapes and text, computed pixel by pixel. It looks better and costs time; turn it off for straight-edged fills. [Swarm](24-swarm.md#a-black-rectangle), [appendix](appendix-java2d.md#drawing).

**ARPAbet.** A way of writing the sounds of speech in plain letters, one name per phoneme: `k ae1 b` is "cab". American speech research has used it since the 1970s; the engine's speech is written in it. [Hover Cab](28-hover-cab.md#how-a-voice-is-made), [manual](48-scenes-menus-sound.md#speech-speech-and-phoneme).

## B

**Back buffer.** The hidden picture a frame is drawn into before it is shown on the screen, so the player never sees a half-drawn frame. [Paddle](20-paddle.md#step-2-drawing-actively), [appendix](appendix-java2d.md#buffers).

**Ballistics.** How a thing flies once it has been fired or thrown: gravity pulls it down, the wind pushes it sideways, the air slows it. [Artillery](29-artillery.md#step-3-shells-in-the-air), [manual](44-movement.md#shells-in-flight-projectile-and-ballistics).

**Bang-bang control.** Steering with switches that are only on or off: in each step a jet fires if that brings the speed nearer the wanted one. Switched often enough, the pushes average out to just what is needed. [Hover Cab](28-hover-cab.md#which-jets).

**Baseline (layer).** The height where a part of a painted room touches the floor. People whose feet are above it are drawn before it and disappear behind it; the others walk in front. [Manual](49-adventure.md#rooms-room-hotspot-and-layers).

**Bézier curve.** A smooth curve defined by four points: it starts at the first, ends at the last, and the two in between pull it into shape. Flight paths are built from them. [Swarm](24-swarm.md#step-3-flight-paths), [manual](44-movement.md#curves-to-fly-along-flightpath).

**Box selection.** A drag with the mouse opens a rectangle, and every unit of the player's side inside it is chosen. [Sands](31-sands.md#step-1-choosing-and-commanding), [manual](46-paths-and-decisions.md#choosing-units-selection-and-boxselect).

**Breadth-first search.** A search that spreads from the goals like a wave, one step at a time, and so finds for every tile the number of steps to the nearest goal. [Maze](25-maze.md#step-5-breadth-first-search-the-way-home), [manual](46-paths-and-decisions.md#one-goal-every-tile-distancemap).

## C

**Camera.** The part of a world larger than the screen that is shown. The game draws the world shifted by the camera's position, and turns mouse positions on the screen back into positions in the world. [Jump](27-jump.md#step-6-the-camera), [Towers](30-towers.md#step-5-the-mouse-in-the-world), [manual](45-maps.md#a-world-larger-than-the-screen-camera).

**Carrier.** A settler who takes wares along one stretch of road, from flag to flag. [Colony](32-colony.md#step-2-carriers).

**Catmull-Rom curve.** A smooth curve that passes through every point it is given, each piece shaped by the points before and after it. A closed one through a dozen points makes a soft body outline. [Templars](50-templar.md#step-4-people-drawn-from-a-description).

**Cell.** One square of a tile map, given by its column and row. [Mines](10-mines.md#step-1-the-field-in-memory), [manual](45-maps.md#a-grid-of-tiles-tilemap-cell-and-direction).

**Collision detection.** Finding out which things touch or overlap: boxes, circles, or anything near something else on a grid. [Paddle](20-paddle.md#swept-collision), [manual](44-movement.md#which-collision-test).

**Compatible image.** An image in the screen's own pixel format, so copying it to the screen needs no conversion. [Bricks](21-bricks.md#fix-1-scale-once-then-copy), [appendix](appendix-java2d.md#drawing).

**Component.** One part of an entity: a body, a flight along a path, hit points. What an entity is and does follows from the components it has right now. [Swarm](24-swarm.md#step-2-entities-and-components), [manual](44-movement.md#entities-and-components).

**Computer opponent.** A player whose decisions the program makes. In this book it gets the same information and the same controls as a human player wherever possible. [Paddle](20-paddle.md#step-5-the-computer-opponent), [Sands](31-sands.md#step-8-the-computer-opponent).

**Coverage.** How well a tile is served by places that reach a circle around them, such as schools, fire stations and parks: how many reach it, and how strongly. [Town](33-town.md#step-5-reach-water-and-land-value), [manual](45-maps.md#places-that-reach-a-circle-coverage).

**Cutscene.** A short scene the player watches instead of plays: people walk, speak and act one step after the other. In the engine a cutscene is a block of a script. [Templars](50-templar.md#step-2-from-a-click-to-an-action), [manual](49-adventure.md#the-story-as-a-play-script-and-scriptrunner).

## D

**Data file.** A plain text file that holds what a designer changes without a compiler: levels, waves, stages, the properties of units and buildings. [Swarm](24-swarm.md#step-4-stages-as-data), [Towers](30-towers.md#step-3-units-towers-and-waves-as-data), [manual](47-data-and-saving.md#data-files-textdata).

**Demand.** What a game's places need and ask for: a building site that needs planks, a town that wants more homes or shops. [Colony](32-colony.md#step-3-demand-who-needs-what), [Town](33-town.md#step-6-demand-mood-and-the-budget).

**Depth scale.** How much smaller, and slower, a person in a painted room becomes towards the back wall, given by the scale at two heights of the picture. [Manual](49-adventure.md#smaller-at-the-back-depthscale).

**Depth sorting.** Drawing the things of a view from back to front, so what stands in front covers what stands behind; also called the painter's algorithm. [Town](33-town.md#step-2-what-covers-what), [manual](45-maps.md#what-covers-what-depthsort).

**Destructible ground.** Ground made of small cells that shots can blast away or heap up anywhere, and that falls when its support is gone. [Artillery](29-artillery.md#step-1-ground-that-breaks), [manual](45-maps.md#ground-that-breaks-ground-and-landscape).

**Determinism.** The same seed and the same input give the same game, step by step, on every computer. Replays, tests of whole games and loaded saves depend on it. [Snake](22-snake.md#step-4-chance-that-repeats), [manual](40-engine.md#ideas-that-run-through-the-engine).

**Dialog tree.** A conversation in which the player picks what to say from a few options; each answer can open or close other options. [Templars](50-templar.md#step-1-the-story-as-data), [manual](49-adventure.md#conversations-dialog-options).

**Distance map.** For every tile, the number of steps to the nearest of one or more goals; the first step of a shortest way is then always downhill. [Maze](25-maze.md#step-5-breadth-first-search-the-way-home), [manual](46-paths-and-decisions.md#one-goal-every-tile-distancemap).

**Domain.** The rules of a game as plain Java, without Swing, so they can be tested without a screen. [Structure](02-structure.md#domain-the-rules).

**Drag.** The slowing of a thing by the air it moves through: each second it loses a part of its speed. It makes a craft on jets steerable. [Hover Cab](28-hover-cab.md#why-a-cab-needs-air), [manual](44-movement.md#flying-on-jets-thrusters-drag-and-touchdown).

## E

**Easing.** How a movement feels over its time: starting fast and landing gently, or slow at both ends. An easing maps the share of the time to the share of the way. [Solitaire](11-solitaire.md#easing-how-a-movement-feels), [manual](42-sprites.md#movement-in-time-easing-and-tween).

**Entity.** A thing of the game world that is little more than a number and a bag of components. [Swarm](24-swarm.md#step-2-entities-and-components), [manual](44-movement.md#entities-and-components).

**Event queue.** A list of things that happened and must be handled, first in, first out. Handling one event may add new ones, which is how chain reactions work. [Blast](26-blast.md#step-5-events-and-chain-reactions), [manual](46-paths-and-decisions.md#events-that-cause-events-eventqueue).

## F

**Fixed time step.** The rules always advance by the same amount of time, usually 1/120 of a second, however fast the computer draws. That makes the rules the same on every computer and testable. [Paddle](20-paddle.md#step-1-the-game-loop), [manual](41-loop-and-drawing.md#the-game-loop-gameloop-and-fixedstep).

**Flag.** A point on the map where roads meet and wares wait to be carried on. A new flag on a road splits it in two. [Colony](32-colony.md#step-1-roads-from-flag-to-flag), [manual](46-paths-and-decisions.md#roads-between-flags-roadnetwork).

**Flag (story).** A named number the story remembers, such as whether a page was found or how often a question was asked. Never set means 0. [Templars](50-templar.md#step-1-the-story-as-data), [manual](49-adventure.md#what-she-carries-what-the-story-remembers-inventory-and-flags).

**Flood fill.** Opening or colouring all connected cells from one start, as Mines opens an empty area. [Mines](10-mines.md#flood-fill-without-recursion).

**Flow field.** For every tile of a map, the direction that leads to a goal. Any number of units follow the arrows, and the whole field costs one search. [Towers](30-towers.md#step-1-the-flow-field), [manual](46-paths-and-decisions.md#many-units-one-goal-flowfield).

**Fog of war.** What one side knows of the map: tiles never seen are black, tiles seen once are dimmed, and only what the side's units see now shows enemies. [Sands](31-sands.md#step-3-fog-of-war), [manual](45-maps.md#what-a-side-sees-fogofwar-and-fogpainter).

**Formant.** A frequency at which the mouth and throat ring while speaking. Where the first two formants are decides which vowel is heard. [Hover Cab](28-hover-cab.md#how-a-voice-is-made), [manual](48-scenes-menus-sound.md#speech-speech-and-phoneme).

**Formation.** Where each unit of a group stands when it arrives: a slot of its own near the target, chosen so that the units do not cross each other's way. [Sands](31-sands.md#a-slot-for-each), [manual](46-paths-and-decisions.md#groups-on-the-move-formation).

**Frame.** One picture on the screen. A game draws up to 60 frames a second; at that rate a frame has 16.7 ms. [Paddle](20-paddle.md#step-1-the-game-loop).

**Frame time.** How long a frame took to draw. The frame-time display, F12 in every game, shows the average and the worst frame of the last second. [Bricks](21-bricks.md#a-display-for-frame-times), [manual](41-loop-and-drawing.md#frame-times-frametimes).

## G

**Game loop.** The heart of a real-time game: a thread that runs the rules in fixed steps and draws frames, again and again, until the game ends. [Paddle](20-paddle.md#step-1-the-game-loop), [manual](41-loop-and-drawing.md#the-game-loop-gameloop-and-fixedstep).

**Garbage collection.** Java frees objects nobody uses any more by itself, and may pause the program to do it. A game avoids making objects in every frame, so the collector has nothing to do while it runs. [Bricks](21-bricks.md#step-5-particles), [appendix](appendix-java2d.md#drawing).

**Gamepad.** A game controller with buttons and sticks, read from a device file on Linux and through XInput on Windows. [Blast](26-blast.md#step-2-gamepads), [manual](43-input.md#gamepads-gamepads-and-gamepad).

## H

**Hard edges.** Scaling a picture without blending neighbouring pixels, so pixel art stays crisp instead of blurred. [Swarm](24-swarm.md#pixel-art-needs-hard-edges), [manual](42-sprites.md#pictures-sprite).

**Heavyweight component.** An AWT component such as `Canvas` that is a native window of its own inside the Swing window. It is drawn over Swing's lightweight components, which needs care with popup menus and keys. [Paddle](20-paddle.md#a-heavyweight-in-a-swing-window).

**Hotspot.** A part of a room picture the player can point at: it has a name, an outline, and a place where the hero stands to use it. [Templars](50-templar.md#step-1-the-story-as-data), [manual](49-adventure.md#rooms-room-hotspot-and-layers).

## I

**Influence map.** A map of where a side is strong: each unit and building spreads its strength around it, and the sums show strong and weak places at a glance. [Sands](31-sands.md#whom-to-attack-the-influence-map), [manual](46-paths-and-decisions.md#strength-on-the-map-influencemap).

**Intonation.** The tune of a spoken phrase: the pitch sinks as the breath runs out, rises on stressed syllables, and rises at the end of a question. [Hover Cab](28-hover-cab.md#a-phrase).

**Inventory.** What the hero of an adventure carries. Items are used on things in the room or on each other. [Templars](50-templar.md#the-rules), [manual](49-adventure.md#what-she-carries-what-the-story-remembers-inventory-and-flags).

**Ink line.** The dark outline around every object of a hand-drawn picture. In code: draw the object on a layer of its own, then put its shape, grown by the line width, underneath in a dark colour. [Templars](50-templar.md#step-3-painting-rooms-in-code).

**Isometric view.** A tile map seen from a corner and from above: each square tile becomes a diamond twice as wide as high, and buildings show two walls and a roof. The rules stay square. [Town](33-town.md#step-1-the-isometric-view), [manual](45-maps.md#the-isometric-view-isoprojection).

## K

**Key repeat.** A held key is sent again and again; on X11 even as release, press, release, press. The engine believes a release only when no press of the same key follows within 25 ms, so a held key stays held. [Paddle](20-paddle.md#step-4-the-keyboard), [manual](43-input.md#the-keyboard-keystate-and-keyboard).

## L

**Logical coordinates.** The fixed size a game draws in, 640 × 400 in most games of this book, whatever the size of the window. The canvas scales the picture; the rules and the mouse never see real pixels. [Paddle](20-paddle.md#logical-coordinates), [manual](41-loop-and-drawing.md#the-canvas-gamecanvas-and-rendermode).

## M

**Managed image.** An image that Java2D keeps a copy of in video memory, so it is copied fast. Touching its pixel array directly ends that for good. [Appendix](appendix-java2d.md#drawing).

**Midpoint displacement.** A way to make natural-looking hills: put a point between two heights at their average, moved up or down at random, and do the same for each half with smaller moves. [Artillery](29-artillery.md#step-2-a-landscape), [manual](45-maps.md#ground-that-breaks-ground-and-landscape).

**Minimap.** The whole map, small, in a corner of the screen, with a box for what the camera shows. A click on it moves the camera. [Sands](31-sands.md#step-4-the-minimap), [manual](45-maps.md#the-map-in-a-corner-minimap).

**Mixer.** The part that adds several sounds into one stream for the sound card, so effects and music play at the same time. [Paddle](20-paddle.md#the-mixer), [manual](48-scenes-menus-sound.md#playing-mixer).

**Model-View-Presenter (MVP).** The rules (model) and the window (view) never talk to each other directly; a presenter without Swing sits between them. [Structure](02-structure.md#model-view-presenter).

## O

**Object pool.** A fixed number of objects made at the start and used again and again, such as shots, instead of new objects for each. [Rocks](23-rocks.md#step-5-four-shots-from-a-pool), [manual](44-movement.md#a-fixed-number-of-things-pool).

**Overlay.** What is drawn on top of the world: score, texts, menus. In pixel mode it stays sharp over a world of big pixels. [Bricks](21-bricks.md#two-layers-the-world-and-the-overlay), [manual](41-loop-and-drawing.md#the-canvas-gamecanvas-and-rendermode).

**Overlay menu.** A menu drawn inside the game's picture, beside the thing it is about, such as the towers that may be built on a tile. [Towers](30-towers.md#step-6-menus-over-the-game), [manual](48-scenes-menus-sound.md#menus-over-the-game-overlaymenu).

## P

**Parallax.** Background layers that scroll slower the further away they are, which gives a flat game depth. [Jump](27-jump.md#parallax), [manual](45-maps.md#depth-parallax).

**Particle.** A small, short-lived square that flies, falls and fades: a splinter, a spark, dust. [Bricks](21-bricks.md#step-5-particles), [manual](42-sprites.md#splinters-and-sparks-particles).

**Passable.** Whether a tile may be entered, asked by every search for a way. [Maze](25-maze.md#step-5-breadth-first-search-the-way-home), [manual](46-paths-and-decisions.md#what-may-be-entered-passable).

**Phoneme.** One sound of speech, such as the *k*, *ae* and *b* of "cab". A speech synthesizer makes words from phonemes. [Hover Cab](28-hover-cab.md#how-a-voice-is-made), [manual](48-scenes-menus-sound.md#speech-speech-and-phoneme).

**Pixel mask.** Which pixels of a thing are solid, one bit each. Two masks collide where both have a solid pixel, which is as exact as the pictures. [Hover Cab](28-hover-cab.md#step-3-collisions-as-exact-as-the-pictures), [manual](44-movement.md#exact-to-the-pixel-pixelmask).

**Pixel mode.** A render mode that draws the world at its logical size and enlarges it by a whole factor, so each logical pixel becomes a square block. Fast on large screens, and the look of old games. [Bricks](21-bricks.md#pixel-mode), [manual](41-loop-and-drawing.md#the-canvas-gamecanvas-and-rendermode).

**Playthrough test.** A test that plays a whole story with clicks, as a player would, so a puzzle that cannot be solved fails the build. [Templars](50-templar.md#step-5-a-test-that-plays-the-game).

**Point-and-click adventure.** A game told in pictures, in which the player walks people through rooms, looks at things, uses them and talks, with nothing but the mouse. [Templars](50-templar.md).

**Polling.** Asking an input device for its state at a fixed moment, once per step, instead of acting on each event when it arrives. All questions of one step get the same answers. [Paddle](20-paddle.md#step-7-two-threads-one-game), [manual](43-input.md#one-pattern-for-every-device).

**Pose.** The angles of a figure's joints at one moment: lean, hips, knees, shoulders, elbows. A walk is a pose computed from the phase of the step. [Templars](50-templar.md#step-4-people-drawn-from-a-description).

**Production queue.** Orders worked off one after the other, each paid while it is made. [Sands](31-sands.md#paying-while-it-is-made), [manual](47-data-and-saving.md#money-and-production-credits-and-production).

## R

**Recipe.** What a workshop makes of what: its inputs, the good made and the time it takes. [Colony](32-colony.md#step-4-goods-and-recipes), [manual](47-data-and-saving.md#goods-and-recipes-stock-and-recipe).

**Render mode.** How the world reaches the screen: `DIRECT`, scaled to any size and sharp, or `PIXEL`. [Bricks](21-bricks.md#pixel-mode), [manual](41-loop-and-drawing.md#the-canvas-gamecanvas-and-rendermode).

**Replay.** A whole game kept as a seed and the player's actions with the tick of each. Fed to the rules again, they play the same game. [Snake](22-snake.md#step-5-replays), [manual](47-data-and-saving.md#a-game-as-its-input-replay).

**Resonator.** A filter that rings at one frequency: a tone at that frequency builds up in it, others die away. Five of them in a row are the mouth of the speech synthesizer. [Hover Cab](28-hover-cab.md#the-resonator).

## S

**Save file.** A player's progress, or a whole running game, on disk, written so that a crash while writing never loses the old file. [Jump](27-jump.md#step-8-saving-progress), [Sands](31-sands.md#step-9-saving-the-whole-game), [manual](47-data-and-saving.md#progress-on-disk-savefile).

**Scene.** One screen of a real-time game: the title, the match, the pause screen, the end. Scenes lie on a stack; the top one runs. [Paddle](20-paddle.md#step-6-scenes), [manual](48-scenes-menus-sound.md#screens-scene-and-scenestack).

**Script.** The story of an adventure as a text file, a list of blocks that say what happens when the player looks at, uses or talks to something. [Templars](50-templar.md#step-1-the-story-as-data), [manual](49-adventure.md#the-story-as-a-play-script-and-scriptrunner).

**Seed.** The number a random generator starts from. The same seed gives the same numbers. [Snake](22-snake.md#step-4-chance-that-repeats), [manual](47-data-and-saving.md#chance-that-repeats-seededrandom).

**Shroud.** The black over the parts of the map a side has never seen. [Sands](31-sands.md#step-3-fog-of-war), [manual](45-maps.md#what-a-side-sees-fogofwar-and-fogpainter).

**Snapshot.** A whole game written down as keys and values, exact enough that a loaded game goes on as the saved one would have. [Sands](31-sands.md#step-9-saving-the-whole-game), [manual](47-data-and-saving.md#a-whole-game-snapshot).

**Source-filter model.** Speech as a sound source, the vocal folds' puffs and the hiss of narrow places, shaped by a filter, the mouth and throat. The source sets the pitch, the filter the vowel. [Hover Cab](28-hover-cab.md#how-a-voice-is-made).

**Spatial grid.** Things sorted into the squares of a coarse grid, so each one is tested only against its neighbours instead of against everything. [Swarm](24-swarm.md#step-5-collisions-on-a-grid), [manual](44-movement.md#many-things-grid).

**Speech synthesis.** Making speech by computation instead of playing recordings. The engine's synthesizer models the voice with formants, as the talking machines of the early 1980s did. [Hover Cab](28-hover-cab.md#step-6-passengers-who-talk), [manual](48-scenes-menus-sound.md#speech-speech-and-phoneme).

**Sprite.** A picture with a size in logical coordinates, scaled once to the screen and then copied in every frame. [Bricks](21-bricks.md#step-4-sprites), [manual](42-sprites.md#pictures-sprite).

**Sprite sheet.** Many small pictures in one image, in a grid of equal cells, such as the frames of an animation. [Swarm](24-swarm.md#step-1-a-sprite-sheet), [manual](42-sprites.md#many-pictures-in-one-spritesheet).

**State machine.** Something that is always in exactly one of a few states and changes between them only along ways allowed in advance, such as a ghost that waits, hunts, flees or flies home. [Maze](25-maze.md#step-3-ghosts-as-state-machines), [manual](46-paths-and-decisions.md#one-state-at-a-time-statemachine).

**Supply network.** Tiles that carry something from where it is made to where it is used, such as power from plants through lines to homes. [Town](33-town.md#step-4-power-through-the-town), [manual](45-maps.md#networks-that-carry-supplygrid).

**Swept collision.** A collision test for a thing that moves: not "do they overlap now?" but "when during this step did they first touch?", so a fast ball never passes through a paddle. [Paddle](20-paddle.md#swept-collision), [Bricks](21-bricks.md#step-2-collision-of-moving-boxes), [manual](44-movement.md#boxes-box-and-sweep).

## T

**Territory.** The land inside a border of circles that a side's buildings claim. Buildings may stand only inside. [Colony](32-colony.md#the-border), [manual](45-maps.md#the-land-a-side-owns-territory).

**Thrust.** The push of a jet: an acceleration, like gravity, for as long as the jet burns. [Hover Cab](28-hover-cab.md#step-1-jets-against-gravity), [manual](44-movement.md#flying-on-jets-thrusters-drag-and-touchdown).

**Tick.** One step of rules that move at a pace of their own, slower than the game loop, such as the snake moving one cell. [Snake](22-snake.md#step-2-a-pace-of-its-own), [manual](41-loop-and-drawing.md#a-pace-of-its-own-ticker).

**Tile map.** Columns and rows of squares, each holding one value such as `WALL` or `ROAD`. The rules of maze, puzzle and strategy games live on it in whole cells. [Maze](25-maze.md#step-1-a-map-of-tiles), [manual](45-maps.md#a-grid-of-tiles-tilemap-cell-and-direction).

**Tiled.** A free map editor whose TMX files the engine reads. [Jump](27-jump.md#step-1-levels-from-the-tiled-editor), [manual](45-maps.md#maps-from-an-editor-tiledmap).

**Tint.** A colour multiplied into a picture, so people drawn in daylight colours take on the light of a candlelit or moonlit room. [Templars](50-templar.md#drawn-once-kept).

**Touchdown.** The moment a craft comes down on a landing place, graded by its speed as soft, hard or a crash. [Hover Cab](28-hover-cab.md#step-2-touching-down), [manual](44-movement.md#flying-on-jets-thrusters-drag-and-touchdown).

**Turn order.** Who plays now and who next, in a game played in turns, skipping the players who are out; the round is decided when one is left. [Artillery](29-artillery.md#whose-turn), [manual](46-paths-and-decisions.md#whose-turn-turnorder).

**Tween.** One movement from one value to another in a given time, with an easing. [Solitaire](11-solitaire.md#tween-one-animation-in-time), [manual](42-sprites.md#movement-in-time-easing-and-tween).

## U

**Undo by snapshots.** Before each move the game keeps a copy of its state; undo hands the last copy back. [Solitaire](11-solitaire.md#step-4-undo-by-snapshots), [manual](47-data-and-saving.md#undo-and-redo-history).

## V

**Vector.** A point or a direction in the plane, with operations such as adding, scaling and turning. [Rocks](23-rocks.md#step-1-vectors), [manual](44-movement.md#points-and-directions-vector-and-angles).

**Visibility graph.** The corners of the obstacles in a room, each joined to every corner it can see in a straight line. The shortest way round obstacles runs along it, from corner to corner. [Manual](49-adventure.md#where-people-walk-walkarea).

**Volatile image.** An image in video memory that can lose its contents when the system needs the memory, and must be checked and drawn again. [Appendix](appendix-java2d.md#buffers).

## W

**Walk area.** The floor of a painted room as a polygon with holes, where people can walk; older adventures called its parts walkboxes. [Templars](50-templar.md#step-1-the-story-as-data), [manual](49-adventure.md#where-people-walk-walkarea).

**Wrap-around.** A field without edges: what leaves on one side comes back on the other, and distances take the short way across an edge. [Rocks](23-rocks.md#step-3-a-field-without-edges), [manual](44-movement.md#a-field-without-edges-wrap).

## Z

**Zone.** Land the player marks for homes, shops or industry. The town builds on it by itself when there is demand. [Town](33-town.md#step-3-zones-lots-and-growth).
