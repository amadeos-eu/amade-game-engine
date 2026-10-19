package eu.amadeos.gameengine.swing;

import eu.amadeos.gameengine.loop.FrameTimes;

import javax.swing.SwingUtilities;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferStrategy;
import java.awt.image.VolatileImage;
import java.util.Locale;

/**
 * The drawing surface of a real-time game. The game draws in fixed <i>logical</i> coordinates, e.g.
 * 640 × 400, as if the screen always had that size; the canvas scales the picture to its real size, keeps
 * the proportions and fills the rest with black bars.
 *
 * <p>Drawing is <i>active</i>: the game loop calls {@link #render(double)} whenever it wants a frame, and
 * the canvas draws into a hidden buffer and shows it at once ({@link BufferStrategy}). Swing's repaint
 * mechanism is not involved, so nothing waits for the event thread.
 *
 * <p>A frame has two layers: the game's world (playing field, sprites, particles) and an overlay on top of
 * it (score, texts). How the world reaches the screen is the {@link RenderMode}; the overlay is always drawn
 * last, straight onto the screen, so text stays sharp even over a world of big pixels. With
 * {@link #setShowFrameTimes} the canvas shows how long its frames take.
 *
 * <p>A heavyweight {@link Canvas} in a Swing window covers lightweight popups: menus must be heavyweight,
 * {@code JPopupMenu.setDefaultLightWeightPopupEnabled(false)}. And Swing's key bindings (menu shortcuts
 * such as F2, mnemonics such as Alt+G) only work for Swing components; the canvas hands the keys it does
 * not use on to them.
 */
public final class GameCanvas extends Canvas {
    /** Draws a frame in logical coordinates. */
    @FunctionalInterface
    public interface Painter {
        void paint(Graphics2D g, double alpha);
    }

    private static final Painter NOTHING = (g, alpha) -> { };
    private static final Color FRAME_TIMES_BACKGROUND = new Color(0, 0, 0, 180);
    private static final Color IN_TIME = new Color(0x7CFC00);
    private static final Color LATE = new Color(0xFF5050);

    private final int logicalWidth;
    private final int logicalHeight;
    private final Painter painter;
    private final Painter overlay;
    /** Held while a frame is drawn, so the native window cannot vanish in the middle of one. */
    private final Object frameLock = new Object();
    private final FrameTimes frameTimes = new FrameTimes();
    private final Font frameTimesFont = new Font(Font.MONOSPACED, Font.BOLD, 14);
    private volatile RenderMode mode = RenderMode.DIRECT;
    private volatile boolean showFrameTimes;
    /** The world of {@link RenderMode#PIXEL}; used by the loop thread only. */
    private VolatileImage backBuffer;

    /** A canvas whose painter draws everything, world and texts alike. */
    public GameCanvas(int logicalWidth, int logicalHeight, Painter painter) {
        this(logicalWidth, logicalHeight, painter, NOTHING);
    }

    /**
     * @param painter draws the world
     * @param overlay draws what lies on top of the world, such as the score and texts; in the same logical
     *                coordinates, but always straight onto the screen
     */
    public GameCanvas(int logicalWidth, int logicalHeight, Painter painter, Painter overlay) {
        this.logicalWidth = logicalWidth;
        this.logicalHeight = logicalHeight;
        this.painter = painter;
        this.overlay = overlay;
        setIgnoreRepaint(true);
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(logicalWidth, logicalHeight));
    }

    public int logicalWidth() {
        return logicalWidth;
    }

    public int logicalHeight() {
        return logicalHeight;
    }

    public RenderMode renderMode() {
        return mode;
    }

    /** Takes effect with the next frame; safe to call from any thread. */
    public void setRenderMode(RenderMode mode) {
        this.mode = mode;
    }

    public boolean showsFrameTimes() {
        return showFrameTimes;
    }

    /** Shows how long the frames take, in the top left corner. */
    public void setShowFrameTimes(boolean show) {
        showFrameTimes = show;
    }

    /** The times of the last second's frames. */
    public FrameTimes frameTimes() {
        return frameTimes;
    }

    // --8<-- [start:keys]
    /** Keys go to the game's listeners first; what they leave unconsumed reaches the window's menu shortcuts. */
    @Override
    protected void processKeyEvent(KeyEvent e) {
        super.processKeyEvent(e);
        if (!e.isConsumed()) SwingUtilities.processKeyBindings(e);
    }
    // --8<-- [end:keys]

    /** Scale from logical to real pixels: as large as fits, the same in both directions. */
    public static double scale(int width, int height, int logicalWidth, int logicalHeight) {
        return Math.min((double) width / logicalWidth, (double) height / logicalHeight);
    }

    // --8<-- [start:whole-scale]
    /** The largest whole factor by which the logical picture fits into {@code width} × {@code height}; at least 1. */
    public static int wholeScale(int width, int height, int logicalWidth, int logicalHeight) {
        return Math.max(1, (int) Math.floor(scale(width, height, logicalWidth, logicalHeight) + 1e-9));
    }
    // --8<-- [end:whole-scale]

    // --8<-- [start:to-logical]
    /**
     * Where a point of the component (a mouse event's {@code x} and {@code y}) lies in logical coordinates,
     * placed exactly as the current render mode places the picture. Outside the picture, e.g. on a black
     * bar, the result lies outside 0 … logical size.
     */
    public Point2D.Double toLogical(double x, double y) {
        GraphicsConfiguration gc = getGraphicsConfiguration();
        double ui = gc == null ? 1 : gc.getDefaultTransform().getScaleX();
        return toLogical(x * ui, y * ui, (int) Math.round(getWidth() * ui), (int) Math.round(getHeight() * ui),
                logicalWidth, logicalHeight, mode);
    }

    /** {@link #toLogical(double, double)} for a screen of {@code width} × {@code height} pixels. */
    public static Point2D.Double toLogical(double x, double y, int width, int height,
                                           int logicalWidth, int logicalHeight, RenderMode mode) {
        if (mode == RenderMode.PIXEL) {
            // Whole pixels, as in paintPixels: the picture starts at a whole pixel, each logical unit is n pixels.
            int n = wholeScale(width, height, logicalWidth, logicalHeight);
            int left = (width - logicalWidth * n) / 2;
            int top = (height - logicalHeight * n) / 2;
            return new Point2D.Double((x - left) / n, (y - top) / n);
        }
        double scale = scale(width, height, logicalWidth, logicalHeight);
        double left = (width - logicalWidth * scale) / 2;
        double top = (height - logicalHeight * scale) / 2;
        return new Point2D.Double((x - left) / scale, (y - top) / scale);
    }
    // --8<-- [end:to-logical]

    // --8<-- [start:render]
    /** Draws one frame; called by the game loop on its thread. Does nothing while the canvas is not shown. */
    public void render(double alpha) {
        synchronized (frameLock) {
            if (!isDisplayable() || getWidth() <= 0 || getHeight() <= 0) return;
            BufferStrategy buffers = getBufferStrategy();
            if (buffers == null) {
                createBufferStrategy(2);
                return;
            }
            long start = System.nanoTime();
            // The buffers live in video memory and can be lost (another program, a mode switch): draw again.
            do {
                do {
                    Graphics2D g = (Graphics2D) buffers.getDrawGraphics();
                    try {
                        paintFrame(g, alpha);
                    } finally {
                        g.dispose();
                    }
                } while (buffers.contentsRestored());
                buffers.show();
            } while (buffers.contentsLost());
            // Without this, X11 may collect frames and show them in bursts.
            Toolkit.getDefaultToolkit().sync();
            frameTimes.record(start, System.nanoTime());
        }
    }
    // --8<-- [end:render]

    /**
     * Runs a change on the Swing thread that takes the canvas off the screen for a moment, such as
     * {@link FullScreen}'s switch. The game loop draws no frame meanwhile and continues with new buffers.
     */
    public void withoutFrames(Runnable change) {
        synchronized (frameLock) {
            change.run();
        }
    }

    private void paintFrame(Graphics2D g, double alpha) {
        // A copy for the game: its transform and clip must not reach the frame-time display.
        Graphics2D game = (Graphics2D) g.create();
        try {
            switch (mode) {
                case DIRECT -> paintScaled(game, alpha);
                case PIXEL -> paintPixels(game, alpha);
            }
        } finally {
            game.dispose();
        }
        if (showFrameTimes) paintFrameTimes(g);
    }

    // --8<-- [start:scaled]
    private void paintScaled(Graphics2D g, double alpha) {
        int w = getWidth();
        int h = getHeight();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);
        double scale = scale(w, h, logicalWidth, logicalHeight);
        double x = (w - logicalWidth * scale) / 2;
        double y = (h - logicalHeight * scale) / 2;
        g.translate(x, y);
        g.scale(scale, scale);
        g.clipRect(0, 0, logicalWidth, logicalHeight);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        painter.paint(g, alpha);
        overlay.paint(g, alpha);
    }
    // --8<-- [end:scaled]

    // --8<-- [start:pixels]
    /** The world at the logical size, enlarged by a whole factor with hard edges; black bars around it. */
    private void paintPixels(Graphics2D g, double alpha) {
        Dimension screen = toScreenPixels(g);
        int n = wholeScale(screen.width, screen.height, logicalWidth, logicalHeight);
        VolatileImage world = drawWorld(alpha);
        int x = (screen.width - logicalWidth * n) / 2;
        int y = (screen.height - logicalHeight * n) / 2;
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, screen.width, screen.height);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(world, x, y, logicalWidth * n, logicalHeight * n, null);
        paintOverlay(g, x, y, n, alpha);
    }
    // --8<-- [end:pixels]

    // --8<-- [start:world]
    /**
     * Draws the world into the back buffer, at the logical size. The back buffer is a {@link VolatileImage}:
     * it lives in video memory where there is one, and like the screen's buffers it can be lost, so it is
     * checked before and after drawing.
     */
    private VolatileImage drawWorld(double alpha) {
        GraphicsConfiguration gc = getGraphicsConfiguration();
        do {
            // A new monitor or a lost graphics card: make the buffer anew.
            if (backBuffer == null || backBuffer.validate(gc) == VolatileImage.IMAGE_INCOMPATIBLE) {
                if (backBuffer != null) backBuffer.flush();
                backBuffer = gc.createCompatibleVolatileImage(logicalWidth, logicalHeight);
            }
            Graphics2D g = backBuffer.createGraphics();
            try {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, logicalWidth, logicalHeight);
                // Hard pixels: no antialiasing, and sprites shrunk without blending.
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                painter.paint(g, alpha);
            } finally {
                g.dispose();
            }
        } while (backBuffer.contentsLost());
        return backBuffer;
    }
    // --8<-- [end:world]

    /** The overlay in logical coordinates, placed and scaled like the world, drawn straight onto the screen. */
    private void paintOverlay(Graphics2D g, double x, double y, double scale, double alpha) {
        Graphics2D o = (Graphics2D) g.create();
        try {
            o.translate(x, y);
            o.scale(scale, scale);
            o.clipRect(0, 0, logicalWidth, logicalHeight);
            o.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            o.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            overlay.paint(o, alpha);
        } finally {
            o.dispose();
        }
    }

    /**
     * Sets {@code g} to draw in the screen's own pixels and returns their number. On a HiDPI screen Java
     * draws a component of 1000 units on 2000 pixels; the picture must be placed in pixels, not units.
     */
    private Dimension toScreenPixels(Graphics2D g) {
        AffineTransform t = g.getTransform();
        int w = (int) Math.round(getWidth() * t.getScaleX());
        int h = (int) Math.round(getHeight() * t.getScaleY());
        g.setTransform(new AffineTransform());
        return new Dimension(w, h);
    }

    // --8<-- [start:frame-times]
    private void paintFrameTimes(Graphics2D g) {
        Dimension screen = toScreenPixels(g);
        int w = screen.width;
        int h = screen.height;
        String label = mode == RenderMode.DIRECT
                ? String.format(Locale.ROOT, "direct %.2f", scale(w, h, logicalWidth, logicalHeight))
                : "pixel " + wholeScale(w, h, logicalWidth, logicalHeight);
        String text = String.format(Locale.ROOT, "%d x %d  %s  %5.1f ms  max %5.1f ms  %3d fps",
                w, h, label, frameTimes.averageMillis(), frameTimes.maxMillis(),
                frameTimes.framesPerSecond());
        g.setFont(frameTimesFont);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int width = g.getFontMetrics().stringWidth(text);
        g.setColor(FRAME_TIMES_BACKGROUND);
        g.fillRect(0, 0, width + 16, 26);
        // Green while a frame fits into 1/60 s, else red.
        g.setColor(frameTimes.maxMillis() < 1000.0 / 60 ? IN_TIME : LATE);
        g.drawString(text, 8, 18);
    }
    // --8<-- [end:frame-times]
}
