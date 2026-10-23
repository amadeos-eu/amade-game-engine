# 43. Input

A game that waits for the player reacts to events: a click arrives, the presenter plays the move. A real-time game asks instead: 120 times a second the rules want to know which keys are held, where the mouse is, whether a button went down since the last look. The input classes of the engine turn Swing's events into answers to those questions.

## One pattern for every device

All input classes work the same way, because two threads meet in them:

1. **Swing reports.** `Keyboard.attach(canvas)` and `Mouse.attach(canvas)` add listeners to the canvas; the gamepad backends report from a thread of their own or when asked. What they report is collected.
2. **The game loop polls.** At the start of each update step the game calls `poll()` on each device it uses. The device takes over what was collected, and **all questions of that step get the same answers**.
3. **The rules ask.** `isDown` (held now), `wasPressed` (went down between the last two polls, however short the press was), and for the mouse `wasReleased` and the position.

The second point matters more than it looks. A key pressed and released between two steps is never "down" when the rules look, but `wasPressed` still sees it. And a step that asks twice cannot get two different answers because an event arrived in between.

| Device | Poll | Introduced in |
|---|---|---|
| Keyboard | `keyboard.poll()` | [20. Paddle](20-paddle.md#step-4-the-keyboard) |
| Mouse | `mouse.poll()` | [21. Bricks](21-bricks.md#the-mouse-as-an-input-device) |
| Gamepads | `gamepads.poll()` | [26. Blast](26-blast.md#step-2-gamepads) |

## The keyboard: KeyState and Keyboard

*Package `input` · introduced in [20. Paddle](20-paddle.md#step-4-the-keyboard) · source: [`KeyState`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/input/KeyState.java), [`Keyboard`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/input/Keyboard.java)*

**What it is for.** Which keys are held, and which went down since the last look. `KeyState` is the bookkeeping, without AWT: key codes and nanosecond times come in, so tests can play any sequence. `Keyboard` connects it to a component and makes it safe between the two threads.

**How it works.** Real keyboards bring two problems, and `KeyState` handles both:

- **Key repeat on X11.** A held key makes X11 send *release, press, release, press*, a few milliseconds apart. A game that believed every release would see a stuttering paddle. A release is therefore only believed when no press of the same key follows within 25 ms (`REPEAT_WINDOW`).
- **Short taps.** A press is remembered until the next poll, so `wasPressed` sees a tap that ended before the rules looked.

```java title="amade-game-engine · input/KeyState.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/input/KeyState.java:key-state"
```

`Keyboard.attach(component)` makes the component focusable, turns off focus traversal (Tab is a key like any other in a game) and forgets all held keys when the window loses the focus, because their releases would never arrive.

!!! warning "Pitfall: two taps of the same key in a test"
    Because a press within 25 ms of a release counts as key repeat, a test that taps the same key twice must let a few steps pass between the taps, or the second tap is taken for the first one still held.

??? example "Test: key repeat is not a release"
    ```java title="amade-game-engine · test/input/KeyStateTest.java"
    --8<-- "amade-game-engine/src/test/java/eu/amadeos/gameengine/input/KeyStateTest.java:test-repeat"
    ```

## Actions instead of keys: Controls

*Package `input` · introduced in [20. Paddle](20-paddle.md#keys-and-actions), pad buttons in [26. Blast](26-blast.md#pads-in-the-controls) · source: [`Controls`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/input/Controls.java)*

**What it is for.** The rules ask "does the right player move up?", never "is the up arrow down?". `Controls` maps the actions of a game, an enum, to keys and gamepad buttons; several keys and buttons can trigger one action.

**When to choose it.** Always, in the rules. The keyboard itself is only asked directly for keys that are no action of the game, such as the number keys that recall a group in Sands. With `Controls`, a second player, a gamepad or keys from the settings need no change in the rules.

```java title="amade-game-engine · input/Controls.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/input/Controls.java:controls"
```

| Method | What it does |
|---|---|
| `bind(action, keyCodes...)` | Adds keys, `KeyEvent.VK_UP` and the like |
| `bind(action, pad, buttons...)` | Adds buttons of one gamepad |
| `rebind(action, keyCodes...)` | Replaces the keys, e.g. from the settings |
| `isDown(action)`, `wasPressed(action)` | Any of its keys or buttons |
| `axis(negative, positive)` | -1, 0 or +1: left and right, up and down |

Each player gets a `Controls` of their own: two players on one keyboard in Paddle, a seat per player with keys or a pad in Blast ([chapter 26](26-blast.md#step-3-seats-and-the-lobby)).

## The mouse: Mouse

*Package `input` · introduced in [21. Bricks](21-bricks.md#step-8-the-mouse); held buttons in [23. Rocks](23-rocks.md#step-7-steering-with-the-mouse), release and inside in [30. Towers](30-towers.md#step-5-the-mouse-in-the-world), the wheel in [33. Town](33-town.md#step-10-a-view-that-zooms) · source: [`Mouse`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/input/Mouse.java)*

**What it is for.** The mouse of a game, like the keyboard: Swing reports, the loop polls and asks. Positions are in the game's **logical** coordinates, so a game never sees the size of the window, the black bars or the render mode.

**How it works.** `attach(canvas)` listens to the canvas and converts every position with `canvas.toLogical`, which places points exactly as the current render mode places the picture ([chapter 41](41-loop-and-drawing.md#the-canvas-gamecanvas-and-rendermode)). A press also asks for the keyboard focus, so a click into the game gives it the keys. Buttons are bits of an int, one per button, for what is held now and for what went down and up since the last poll.

| Method | What it does |
|---|---|
| `x()`, `y()` | The last position, logical; `NaN` until the mouse has been over the canvas |
| `wasMoved()` | It moved between the last two polls |
| `wasPressed(button)`, `wasReleased(button)` | `MouseEvent.BUTTON1` and the others; a release ends a drag |
| `isDown(button)` | Held at the last poll: thrust while the right button is held |
| `isInside()` | Over the canvas: a game that scrolls at the edge stops when the mouse leaves |
| `wheel()` | The notches the wheel turned between the last two polls: positive towards the player, 0 if not at all |

A position outside the picture, on a black bar, lies outside 0 … logical size; check it before you use it. A map larger than the screen needs one more conversion, from the screen to the world: [`Camera.worldX`](45-maps.md#a-world-larger-than-the-screen-camera). A click or a drag on that map becomes a selection with [`BoxSelect`](46-paths-and-decisions.md#choosing-units-selection-and-boxselect).

## Gamepads: Gamepads and Gamepad

*Package `input` · introduced in [26. Blast](26-blast.md#step-2-gamepads) · source: [`Gamepads`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/input/Gamepads.java), [`Gamepad`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/input/Gamepad.java), [`LinuxJoysticks`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/input/LinuxJoysticks.java), [`XInput`](https://github.com/datazuul/amade-game-engine/blob/main/src/main/java/eu/amadeos/gameengine/input/XInput.java)*

**What it is for.** Up to four gamepads, in slots filled in the order the pads are plugged in, plugged in and out while the game runs. Each `Gamepad` answers like the keyboard: `isDown(Button.A)`, `wasPressed(Button.START)`, `anyPressed()` (to join a game), and the left stick as `x()` and `y()`, -1 to 1. The buttons are named as on the pads of the Xbox, which most pads copy: `A`, `B`, `X`, `Y`, `LB`, `RB`, `BACK`, `START` and the d-pad `UP`, `DOWN`, `LEFT`, `RIGHT`.

**When to choose it.** In every game that more than one person plays, or that is played from the sofa. Bind the buttons in `Controls`, next to the keys, and the rules never know which one a player uses. Without a pad, or on a system without a backend, the slots stay empty and the game is played with the keyboard.

**How it works.** Java has no gamepads of its own, so `Gamepads.open()` picks a small backend for the operating system:

- **Linux**: the kernel's joystick devices `/dev/input/js0` and on. Each is a file that delivers eight bytes per event; a thread per pad waits for them, and another looks for new pads every second ([chapter 26](26-blast.md#linux-reading-a-device-file)) Not every joystick device is a pad: laptop accelerometers, the tablet mouse of a virtual machine and some keyboards get one too. Before a device takes a slot, its key and property bitmaps in `/sys/class/input/jsN/device` are read; a device without pad buttons, or with mouse buttons, touch or the accelerometer property, is skipped and logged once ([chapter 26](26-blast.md#devices-that-are-no-gamepads)).
- **Windows**: XInput, the gamepad interface of Windows, a C library called through the Foreign Function and Memory API (`java.lang.foreign`), without JNI and without a native library of our own. It is asked for all four slots in each poll; an empty slot only once a second, because asking it is slow ([chapter 26](26-blast.md#windows-calling-xinput)).

Between the backend's thread and the loop, `Gamepad.poll()` takes over the state, as the keyboard does. The stick has a round **dead zone**: no stick rests exactly in the middle, and a pad lying on the table would otherwise walk the player slowly to the left. Pushed past half its way, the stick also counts as the d-pad, so a game that only asks for `UP` and the like works with either.

```java title="amade-game-engine · input/Gamepad.java"
--8<-- "amade-game-engine/src/main/java/eu/amadeos/gameengine/input/Gamepad.java:gamepad"
```

!!! warning "Pitfall: native access"
    Calling C is a restricted operation in Java. Start a game that opens gamepads with `--enable-native-access=ALL-UNNAMED`, in its tests, its `run` task and its packaged launcher, and put `Enable-Native-Access: ALL-UNNAMED` in its jar's manifest, or Java prints a warning on Windows.

!!! warning "Pitfall: the keyboard must always work"
    A pad the player does not even know about must never lock them out. Let keys and pads join the same way, and give every screen that waits for a pad, such as "Gamepad 2 is not connected", a way out with the keyboard. Blast leaves such a match with Esc.

!!! tip "Tip: pads in tests"
    `Gamepads.none()` gives four empty slots without a backend. A test connects a pad with `pad.connect("Test pad")`, presses buttons with `pad.report(Gamepad.bits(Button.A), 0, 0)`, and polls, as a backend would.
