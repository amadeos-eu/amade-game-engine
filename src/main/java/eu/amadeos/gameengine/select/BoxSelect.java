package eu.amadeos.gameengine.select;

/**
 * What the left mouse button means on a map of units: a <b>click</b> picks the one thing under the mouse,
 * a <b>drag</b> draws a box and picks everything inside it. The difference is a few units of movement
 * between pressing and releasing: a hand that clicks moves the mouse a little, and that must not become a
 * box of one pixel that picks nothing.
 *
 * <p>The box is arithmetic in screen coordinates. The game feeds it the mouse in its update step and asks
 * what came out when the button is released; it draws the box while {@link #dragging()}.
 *
 * <pre>
 * if (mouse.wasPressed(BUTTON1)) box.press(mouse.x(), mouse.y());
 * box.move(mouse.x(), mouse.y());
 * if (mouse.wasReleased(BUTTON1)) {
 *     switch (box.release(mouse.x(), mouse.y())) {
 *         case BoxSelect.Click click -&gt; pickAt(click.x(), click.y());
 *         case BoxSelect.Box b -&gt; pickInside(b.left(), b.top(), b.right(), b.bottom());
 *         case null -&gt; { }
 *     }
 * }
 * </pre>
 */
// --8<-- [start:box]
public final class BoxSelect {
    /** How far the mouse may move between press and release and still be a click, in logical units. */
    public static final double SLOP = 4;

    /** What a press and a release came to. */
    public sealed interface Result {
    }

    /** The button went down and up on nearly the same spot. */
    public record Click(double x, double y) implements Result {
    }

    /** A box dragged open, its edges sorted: left is less than right, top less than bottom. */
    public record Box(double left, double top, double right, double bottom) implements Result {
        public boolean contains(double x, double y) {
            return x >= left && x <= right && y >= top && y <= bottom;
        }
    }

    private boolean pressed;
    private boolean dragging;
    private double startX;
    private double startY;
    private double x;
    private double y;

    /** The left button went down here. */
    public void press(double px, double py) {
        pressed = true;
        dragging = false;
        startX = x = px;
        startY = y = py;
    }

    /** The mouse is here now; once it has gone further than {@link #SLOP} from the press, it drags a box. */
    public void move(double px, double py) {
        if (!pressed || Double.isNaN(px)) return;
        x = px;
        y = py;
        if (Math.abs(x - startX) > SLOP || Math.abs(y - startY) > SLOP) dragging = true;
    }

    /** The button came up here: a click or a box; {@code null} if it was never pressed. */
    public Result release(double px, double py) {
        if (!pressed) return null;
        move(px, py);
        pressed = false;
        if (!dragging) return new Click(startX, startY);
        dragging = false;
        return box();
    }
    // --8<-- [end:box]

    /** Forgets a press, as when the press opened a menu or the scene changed. */
    public void cancel() {
        pressed = false;
        dragging = false;
    }

    public boolean pressed() {
        return pressed;
    }

    /** True while a box is being dragged open: draw it. */
    public boolean dragging() {
        return dragging;
    }

    /** The box as it is now, from the press to the mouse. */
    public Box box() {
        return new Box(Math.min(startX, x), Math.min(startY, y), Math.max(startX, x), Math.max(startY, y));
    }
}
