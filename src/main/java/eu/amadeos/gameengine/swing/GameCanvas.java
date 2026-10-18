package eu.amadeos.gameengine.swing;

import javax.swing.SwingUtilities;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;

/**
 * The drawing surface of a real-time game. The game draws in fixed <i>logical</i> coordinates, e.g.
 * 640 × 400, as if the screen always had that size; the canvas scales the picture to its real size, keeps
 * the proportions and fills the rest with black bars.
 *
 * <p>Drawing is <i>active</i>: the game loop calls {@link #render(double)} whenever it wants a frame, and
 * the canvas draws into a hidden buffer and shows it at once ({@link BufferStrategy}). Swing's repaint
 * mechanism is not involved, so nothing waits for the event thread.
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

    private final int logicalWidth;
    private final int logicalHeight;
    private final Painter painter;

    public GameCanvas(int logicalWidth, int logicalHeight, Painter painter) {
        this.logicalWidth = logicalWidth;
        this.logicalHeight = logicalHeight;
        this.painter = painter;
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

    // --8<-- [start:render]
    /** Draws one frame; called by the game loop on its thread. Does nothing while the canvas is not shown. */
    public void render(double alpha) {
        if (!isDisplayable() || getWidth() <= 0 || getHeight() <= 0) return;
        BufferStrategy buffers = getBufferStrategy();
        if (buffers == null) {
            createBufferStrategy(2);
            return;
        }
        // The buffers live in video memory and can be lost (another program, a mode switch): draw again.
        do {
            do {
                Graphics2D g = (Graphics2D) buffers.getDrawGraphics();
                try {
                    paintScaled(g, alpha);
                } finally {
                    g.dispose();
                }
            } while (buffers.contentsRestored());
            buffers.show();
        } while (buffers.contentsLost());
        // Without this, X11 may collect frames and show them in bursts.
        Toolkit.getDefaultToolkit().sync();
    }
    // --8<-- [end:render]

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
    }
    // --8<-- [end:scaled]
}
