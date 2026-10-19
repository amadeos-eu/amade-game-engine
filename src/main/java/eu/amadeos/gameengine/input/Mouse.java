package eu.amadeos.gameengine.input;

import eu.amadeos.gameengine.swing.GameCanvas;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

/**
 * The mouse of a game, like the {@link Keyboard}: the Swing thread reports what the mouse does, the game loop
 * asks once per update step. Positions are in the game's logical coordinates, so a game never sees the size
 * of the window, the black bars or the render mode.
 *
 * <pre>
 * mouse.poll();
 * if (mouse.wasMoved()) paddle.aim(mouse.x());
 * if (mouse.wasPressed(MouseEvent.BUTTON1)) launch();
 * </pre>
 */
// --8<-- [start:mouse]
public final class Mouse {
    private double x = Double.NaN;
    private double y = Double.NaN;
    private boolean movedSincePoll;
    private boolean moved;
    private int pressedSincePoll;   // one bit per button
    private int pressed;

    /** Listens to the canvas; positions are converted the way the canvas currently draws. */
    public void attach(GameCanvas canvas) {
        MouseAdapter listener = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point2D.Double p = canvas.toLogical(e.getX(), e.getY());
                moved(p.x, p.y);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseMoved(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                canvas.requestFocusInWindow();   // a click into the game gives the keys to the game, too
                pressed(e.getButton());
            }
        };
        canvas.addMouseListener(listener);
        canvas.addMouseMotionListener(listener);
    }

    public synchronized void moved(double x, double y) {
        this.x = x;
        this.y = y;
        movedSincePoll = true;
    }

    public synchronized void pressed(int button) {
        if (button > 0 && button < 32) pressedSincePoll |= 1 << button;
    }

    /** Call once per update step on the game loop thread, before asking. */
    public synchronized void poll() {
        moved = movedSincePoll;
        pressed = pressedSincePoll;
        movedSincePoll = false;
        pressedSincePoll = 0;
    }

    /** The last position in logical coordinates; {@code NaN} until the mouse has moved over the canvas. */
    public synchronized double x() {
        return x;
    }

    public synchronized double y() {
        return y;
    }

    /** True if the mouse moved between the last two polls. */
    public synchronized boolean wasMoved() {
        return moved;
    }

    /** True if the button ({@link MouseEvent#BUTTON1} …) went down between the last two polls. */
    public synchronized boolean wasPressed(int button) {
        return button > 0 && button < 32 && (pressed & 1 << button) != 0;
    }
}
// --8<-- [end:mouse]
