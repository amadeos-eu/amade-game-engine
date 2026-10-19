# Appendix: Java2D and X11 Pitfalls

Java2D draws the games of this book, and on Linux it draws through the X Window System (X11, or XWayland on a Wayland desktop). Most of the time this simply works. This appendix collects the cases where it does not, or not fast enough, with the rule that avoids each one and the chapter where it first came up.

## Measure first

Every rule below is a rule of thumb. Your computer, your graphics driver and your game decide what is actually slow. Show the frame times (**F12** in every game since [Bricks](21-bricks.md#a-display-for-frame-times)), run `./gradlew benchmark` in the game's repository, and change one thing at a time. Keep a change only if the numbers say so; [chapter 21](21-bricks.md#a-fix-that-was-not-one) removed a render mode that sounded convincing and measured slower.

## Drawing

**A frame costs pixels.** Java2D's time grows with the area it fills, much less with the number of calls. Full screen on a 4K monitor is about thirty times the area of 640 × 400. Every effect that covers the whole screen (a veil, a gradient background, a full-screen image) costs a full screen's worth each frame.

**Scale pictures once, then copy them.** `drawImage(image, x, y, width, height)` with a size other than the image's scales it in every frame, and with interpolation each screen pixel is computed from several source pixels. Scale once into an image of the screen size and copy that, as `Sprite` does. In chapter 21 this alone made the game four to six times faster.

**Use compatible images.** An image made with `GraphicsConfiguration.createCompatibleImage` has the screen's pixel format, so copying it needs no conversion, and Java2D may keep a copy of it in video memory (a *managed image*). An image read with `ImageIO` usually has another format; draw it once into a compatible image.

**Don't touch an image's pixel array.** `((DataBufferInt) image.getRaster().getDataBuffer()).getData()` gives you the pixels to change directly, and Java2D can no longer know when they change. From then on it stops caching that image for good, and every copy of it is slow. Change pixels with drawing calls, or keep such images out of the frame loop.

**Antialiasing costs.** Smooth edges for shapes and text are computed per pixel, and on most Linux setups in software. Keep it for what needs it (text, round shapes), or draw such shapes once into a cached image.

**Don't create objects in the frame loop.** `new Color`, `new Font`, `deriveFont`, `new BasicStroke`, `String.format`: each is cheap, but hundreds per frame, sixty times a second, keep the garbage collector busy and cause the occasional long frame. Make them once (chapter 21, `Particles` and `Hud`).

## Buffers

**Draw actively with `BufferStrategy`.** Swing's `repaint()` paints when the event thread gets round to it. A game draws from its own loop into a `BufferStrategy` of a `Canvas` ([chapter 20](20-paddle.md#step-2-drawing-actively)).

**Buffers in video memory can be lost.** A `BufferStrategy`'s buffers and a `VolatileImage` live in video memory where there is one, and lose their contents when the system needs that memory (another program, a mode switch, a suspended laptop). Check them with the documented loops: `contentsRestored()` and `contentsLost()` for the strategy, `validate()` and `contentsLost()` for a volatile image, and draw again.

**`Toolkit.getDefaultToolkit().sync()` after every frame.** X11 collects drawing commands and sends them in batches. Without `sync()`, frames can arrive in bursts and the game stutters although every frame was drawn in time.

## Pipelines

Java2D can reach the screen through different *pipelines*. On Linux there are three, chosen with a system property at start:

| Pipeline | Switch | What it is |
|---|---|---|
| XRender | the default | Drawing commands go to the X server's RENDER extension; the X server, or the graphics driver behind it, does the work |
| X11 | `-Dsun.java2d.xrender=false` | The old way: plain X11 calls, much is computed by Java itself |
| OpenGL | `-Dsun.java2d.opengl=true` | Java2D draws with OpenGL on the graphics card (`=True` with a capital T also prints whether it worked) |

Which is fastest depends on the driver. In our measurement for chapter 21 (Bricks, demo, 3840 × 2160, an X server without a graphics card, OpenGL in software through Mesa), the default was clearly ahead:

| Pipeline | Drawing time per frame |
|---|---|
| XRender | 26.0 ms |
| OpenGL (software) | 77.4 ms |
| X11 | 195.1 ms |

With a real graphics card and a good OpenGL driver, the OpenGL pipeline can win, and with a bad one it can draw wrong pictures. Keep the default, and try the others with the benchmark when a game is too slow on a particular machine. On Windows the default is Direct3D, on macOS Metal.

## Screens

**HiDPI.** On a screen with twice the usual density, Java scales the whole user interface by 2 (it reads `GDK_SCALE` on Linux; `-Dsun.java2d.uiScale=1` turns it off). A canvas of 1000 units then has 2000 pixels, and the `Graphics2D` it hands out already contains that scale. Anything that must be sized in real pixels (a back buffer, a cached sprite) takes the scale from the transform, as `GameCanvas` and `Sprite` do.

**Full screen without a mode switch.** `setDisplayMode` can lower the screen's resolution, the old trick for speed. On Linux it is often unsupported, flickers, and can leave the desktop's windows rearranged. `GraphicsDevice.setFullScreenWindow` without a mode change marks the window as full screen, which lets the compositor get out of the way ([chapter 20](20-paddle.md#full-screen)).

**Several monitors.** Each monitor has its own `GraphicsConfiguration`. A compatible image made for one may be slower on the other. When a window moves to another monitor, `VolatileImage.validate` reports `IMAGE_INCOMPATIBLE`, and the canvas makes a new buffer.

## Input and windows

**Key repeat on X11.** Holding a key sends *release, press, release, press* to the program. A game that believes every release sees a stuttering paddle. The engine's `KeyState` waits 25 ms before it believes a release ([chapter 20](20-paddle.md#step-4-the-keyboard)).

**Heavyweight and lightweight.** A `Canvas` is a native (heavyweight) window inside the Swing window. Lightweight popup menus open *behind* it unless `JPopupMenu.setDefaultLightWeightPopupEnabled(false)`, and Swing's menu shortcuts never see the keys the canvas receives unless it passes them on ([chapter 20](20-paddle.md#a-heavyweight-in-a-swing-window)).

**A disposed window sends `windowClosed`.** Switching to full screen disposes the window to remove its frame. End the program on `FullScreen.whenClosed`, not on `windowClosed` ([chapter 20](20-paddle.md#full-screen)).

## Sound

**No sound card is normal.** Servers, build machines and some desktops have none, or another program holds it. Catch `LineUnavailableException` and play on in silence, as the engine's `Mixer` does ([chapter 21](21-bricks.md#the-mixer)).
