# Appendix: Java2D and X11 Pitfalls

Java2D draws the games of this book, and on Linux it draws through the X Window System (X11, or XWayland on a Wayland desktop). Most of the time this simply works. This appendix collects the cases where it does not, or not fast enough, with the rule that avoids each one and the chapter where it first came up.

## Measure first

Every rule below is a rule of thumb. Your computer, your graphics driver and your game decide what is actually slow. Show the frame times (**F12** in every game since [Bricks](21-bricks.md#a-display-for-frame-times)), run `./gradlew benchmark` in the game's repository, and change one thing at a time. Keep a change only if the numbers say so; [chapter 21](21-bricks.md#a-fix-that-was-not-one) removed a render mode that sounded convincing and measured slower.

## Drawing

**A frame costs pixels.** Java2D's time grows with the area it fills, much less with the number of calls. Full screen on a 4K monitor is about thirty times the area of 640 × 400. Every effect that covers the whole screen (a veil, a gradient background, a full-screen image) costs a full screen's worth each frame.

**Scale pictures once, then copy them.** `drawImage(image, x, y, width, height)` with a size other than the image's scales it in every frame, and with interpolation each screen pixel is computed from several source pixels. Scale once into an image of the screen size and copy that, as `Sprite` does. In chapter 21 this alone made the game four to six times faster.

**Use compatible images.** An image made with `GraphicsConfiguration.createCompatibleImage` has the screen's pixel format, so copying it needs no conversion, and Java2D may keep a copy of it in video memory (a *managed image*). An image read with `ImageIO` usually has another format; draw it once into a compatible image.

**Don't touch an image's pixel array.** `((DataBufferInt) image.getRaster().getDataBuffer()).getData()` gives you the pixels to change directly, and Java2D can no longer know when they change. From then on it stops caching that image for good, and every copy of it is slow. Change pixels with drawing calls, or keep such images out of the frame loop.

**Turned pictures skip every cache.** A sprite drawn with a rotated transform is scaled and turned pixel by pixel in every frame, and a cached copy is only good for one angle. For things that turn freely, draw outlines of lines, which cost a few multiplications per corner, or turn the pictures in advance at a fixed number of angles and count the memory first ([chapter 23](23-rocks.md#pictures-or-lines)).

**Say what a picture's pixels are.** A cached image with an alpha channel is blended pixel by pixel when it is copied, even where every pixel is opaque. Make the cache `OPAQUE` when the picture has no transparent pixel and `BITMASK` when its pixels are either fully opaque or fully transparent, as pixel art scaled with hard edges is; both are plain copies. And don't draw what can't be seen: a background picture whose rows are mostly empty sky is cheaper cut into bands, with the empty rows left out and the solid ones opaque. In Jump these two halved the frame time at full screen ([chapter 27](27-jump.md#measured-then-halved)).

**Antialiasing costs.** Smooth edges for shapes and text are computed per pixel, and on most Linux setups in software. Keep it for what needs it (text, round shapes), or draw such shapes once into a cached image. Turn it off for straight-edged fills: a full-screen black rectangle cost three times as much with antialiasing on, and in Swarm that was half of each frame ([chapter 24](24-swarm.md#a-black-rectangle)).

**Gradients are painted pixel by pixel.** A `GradientPaint` works out the colour of every pixel it fills, in software, in every frame. Panels and buttons that look the same from one frame to the next are drawn once into sprites and copied after that. In Sands the panel's gradients were 70 % of each frame; cached, the frame took a third less time ([chapter 31](31-sands.md#the-panel-and-a-frame-time-measured)).

**Neighbours must round alike.** At a window size that is no whole multiple of the logical one, a sprite's edge and a filled rectangle's edge fall between screen pixels and are rounded, and two ways of rounding leave a line of pixels uncovered between things that should touch. Fill what lies next to sprites with the same rounding as `Sprite.drawSeamless`: the edges worked out in screen pixels, then filled without a transform ([chapter 31](31-sands.md#soft-edges)).

**Don't create objects in the frame loop.** `new Color`, `new Font`, `deriveFont`, `new BasicStroke`, `String.format`: each is cheap, but hundreds per frame, sixty times a second, keep the garbage collector busy and cause the occasional long frame. Make them once (chapter 21, `Particles` and `Hud`).

## Buffers

**Draw actively with `BufferStrategy`.** Swing's `repaint()` paints when the event thread gets round to it. A game draws from its own loop into a `BufferStrategy` of a `Canvas` ([chapter 20](20-paddle.md#step-2-drawing-actively)).

**Buffers in video memory can be lost.** A `BufferStrategy`'s buffers and a `VolatileImage` live in video memory where there is one, and lose their contents when the system needs that memory (another program, a mode switch, a suspended laptop). Check them with the documented loops: `contentsRestored()` and `contentsLost()` for the strategy, `validate()` and `contentsLost()` for a volatile image, and draw again.

**Copy the buffers, don't flip them.** `createBufferStrategy(2)` flips the buffers where it can, on X11 with the double-buffer extension (XDBE). In full screen on the amadeOS desktop, a flipped picture with text more than about a hundred pixels high stays black. Ask for copied buffers with `BufferCapabilities` whose flip contents are `null`; in a test they were no slower ([chapter 20](20-paddle.md#full-screen)).

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

With a real graphics card and a good OpenGL driver, the OpenGL pipeline can win, and with a bad one it can draw wrong pictures. Keep the default, and try the others with the benchmark when a game is too slow on a particular machine. On Windows the default is Direct3D, on macOS Metal. Bricks on a Windows computer, 2560 × 1600, pixel mode, needed 6.5 ms per frame ([chapter 21](21-bricks.md#pixel-mode)).

## Screens

**HiDPI.** On a screen with twice the usual density, Java scales the whole user interface by 2 (it reads `GDK_SCALE` on Linux; `-Dsun.java2d.uiScale=1` turns it off). A canvas of 1000 units then has 2000 pixels, and the `Graphics2D` it hands out already contains that scale. Anything that must be sized in real pixels (a back buffer, a cached sprite) takes the scale from the transform, as `GameCanvas` and `Sprite` do.

**Full screen without a mode switch.** `setDisplayMode` can lower the screen's resolution, the old trick for speed. On Linux it is often unsupported, flickers, and can leave the desktop's windows rearranged. `GraphicsDevice.setFullScreenWindow` without a mode change marks the window as full screen, which lets the compositor get out of the way ([chapter 20](20-paddle.md#full-screen)).

**Black bars or a wider picture.** Most monitors today are 16:9, the games' logical size is 16:10. Scaled to keep its proportions, the picture leaves bars on the left and right (96 pixels each at 1920 × 1080). A fixed field keeps them. A world larger than the screen should fill it: let the canvas widen (`setMaxLogicalWidth`), give the camera the extra width, and anchor panels and buttons at the right edge rather than at 640. Otherwise the edge of the screen, where the player pushes the mouse to scroll, is not the edge of the map ([chapter 30](30-towers.md#a-wider-screen-a-wider-view)).

**Several monitors.** Each monitor has its own `GraphicsConfiguration`. A compatible image made for one may be slower on the other. When a window moves to another monitor, `VolatileImage.validate` reports `IMAGE_INCOMPATIBLE`, and the canvas makes a new buffer.

## Input and windows

**Key repeat on X11.** Holding a key sends *release, press, release, press* to the program. A game that believes every release sees a stuttering paddle. The engine's `KeyState` waits 25 ms before it believes a release ([chapter 20](20-paddle.md#step-4-the-keyboard)).

**Heavyweight and lightweight.** A `Canvas` is a native (heavyweight) window inside the Swing window. Lightweight popup menus open *behind* it unless `JPopupMenu.setDefaultLightWeightPopupEnabled(false)`, and Swing's menu shortcuts never see the keys the canvas receives unless it passes them on ([chapter 20](20-paddle.md#a-heavyweight-in-a-swing-window)).

**A disposed window sends `windowClosed`.** Switching to full screen disposes the window to remove its frame. End the program on `FullScreen.whenClosed`, not on `windowClosed` ([chapter 20](20-paddle.md#full-screen)).

**A disposed window loses the focus.** The same switch sends `windowDeactivated`, and a game that pauses on it pauses on every F11. Pause on `FullScreen.whenDeactivated`, which leaves out the moment of the switch ([chapter 20](20-paddle.md#full-screen)).

## Sound

**No sound card is normal.** Servers, build machines and some desktops have none, or another program holds it. Catch `LineUnavailableException` and play on in silence, as the engine's `Mixer` does ([chapter 20](20-paddle.md#the-mixer)).

**Never feed the line pure silence.** Some wireless headsets switch off after a few seconds of zeros and lose the next sounds while they wake up. An inaudible hiss of one or two steps keeps them awake ([chapter 20](20-paddle.md#the-mixer)).
