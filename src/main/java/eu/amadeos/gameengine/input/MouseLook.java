package eu.amadeos.gameengine.input;

import java.awt.AWTException;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Turning with the mouse, as in a first-person game: the game asks how far the mouse moved since the last
 * update step, not where it is. Java cannot lock the pointer, so while the look is {@linkplain #capture
 * captured} the pointer is hidden and put back into the middle of the component after every move (with a
 * {@link Robot}); the distance from the middle is the movement. Where no robot is allowed (some Wayland
 * desktops), the differences between positions are used instead, which stop at the screen's edge.
 *
 * <pre>
 * look.poll();
 * camera.angle += look.dx() * sensitivity;
 * </pre>
 */
// --8<-- [start:mouselook]
public final class MouseLook {
    private static final Cursor HIDDEN = blankCursor();

    private final Robot robot;
    private Component component;
    private boolean captured;
    private double dxSincePoll;
    private double dySincePoll;
    private double dx;
    private double dy;
    private int lastX = Integer.MIN_VALUE;
    private int lastY;
    private Point warpedTo;

    public MouseLook() {
        this(makeRobot());
    }

    /** For tests: with {@code null}, no pointer is moved. */
    MouseLook(Robot robot) {
        this.robot = robot;
    }

    private static Robot makeRobot() {
        if (GraphicsEnvironment.isHeadless()) return null;
        try {
            return new Robot();
        } catch (AWTException | SecurityException e) {
            return null;
        }
    }

    private static Cursor blankCursor() {
        if (GraphicsEnvironment.isHeadless()) return Cursor.getDefaultCursor();
        try {
            return Toolkit.getDefaultToolkit().createCustomCursor(
                    new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "hidden");
        } catch (RuntimeException e) {
            return Cursor.getDefaultCursor();
        }
    }

    /** Listens to the mouse over the component, the game's canvas. */
    public void attach(Component component) {
        this.component = component;
        MouseAdapter listener = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                moved(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                moved(e.getX(), e.getY());
            }
        };
        component.addMouseMotionListener(listener);
    }

    /** Captures the mouse (hidden, kept in the middle) or lets it go again; call on the Swing thread. */
    public void capture(boolean on) {
        synchronized (this) {
            captured = on;
            lastX = Integer.MIN_VALUE;
            warpedTo = null;
        }
        if (component == null) return;
        component.setCursor(on ? HIDDEN : Cursor.getDefaultCursor());
        if (on) centre();
    }

    public synchronized boolean captured() {
        return captured;
    }

    /** True if the pointer is put back into the middle; false if only position differences are used. */
    public boolean recentres() {
        return robot != null;
    }

    /** The pointer moved to ({@code x}, {@code y}) in the component's pixels; called by the listener and by tests. */
    void moved(int x, int y) {
        boolean warp;
        synchronized (this) {
            if (!captured) return;
            if (warpedTo != null && warpedTo.x == x && warpedTo.y == y) {   // the move the robot made
                warpedTo = null;
                lastX = x;
                lastY = y;
                return;
            }
            if (lastX != Integer.MIN_VALUE) {
                dxSincePoll += x - lastX;
                dySincePoll += y - lastY;
            }
            lastX = x;
            lastY = y;
            warp = robot != null && component != null;
        }
        if (warp) centre();
    }

    private void centre() {
        if (robot == null || component == null || !component.isShowing()) return;
        int cx = component.getWidth() / 2, cy = component.getHeight() / 2;
        Point screen = component.getLocationOnScreen();
        synchronized (this) {
            warpedTo = new Point(cx, cy);
            lastX = cx;
            lastY = cy;
        }
        robot.mouseMove(screen.x + cx, screen.y + cy);
    }

    /** Call once per update step on the game loop thread, before asking. */
    public synchronized void poll() {
        dx = dxSincePoll;
        dy = dySincePoll;
        dxSincePoll = 0;
        dySincePoll = 0;
    }

    /** How far the mouse moved to the right between the last two polls, in screen pixels. */
    public synchronized double dx() {
        return dx;
    }

    /** How far the mouse moved down between the last two polls, in screen pixels. */
    public synchronized double dy() {
        return dy;
    }
}
// --8<-- [end:mouselook]
