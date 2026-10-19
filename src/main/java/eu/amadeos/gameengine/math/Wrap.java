package eu.amadeos.gameengine.math;

/**
 * A field without edges: what leaves it on the right comes back on the left, what leaves at the bottom
 * comes back at the top. Mathematicians call that shape a <i>torus</i>, a doughnut; the screen is the
 * doughnut cut open and laid flat.
 *
 * <p>Three things change on such a field, and this class answers each of them. A position must be brought
 * back into the field ({@link #wrap}). The distance between two things is the <i>shortest</i> one, which may
 * lead across an edge ({@link #delta}). And a thing that sticks out over an edge must be drawn a second
 * time on the other side ({@link #forEachCopy}).
 */
public record Wrap(double width, double height) {
    public Wrap {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("empty field");
    }

    /** Called for each place a thing has to be drawn. */
    @FunctionalInterface
    public interface Copy {
        void at(double x, double y);
    }

    // --8<-- [start:wrap]
    /** {@code x} brought into 0 (inclusive) … width (exclusive). */
    public double x(double x) {
        return x - width * Math.floor(x / width);
    }

    public double y(double y) {
        return y - height * Math.floor(y / height);
    }

    /** The position brought into the field. */
    public Vector wrap(Vector position) {
        double x = x(position.x());
        double y = y(position.y());
        return x == position.x() && y == position.y() ? position : new Vector(x, y);
    }
    // --8<-- [end:wrap]

    // --8<-- [start:delta]
    /**
     * The shortest way from {@code from} to {@code to}, perhaps across an edge. On a field 640 wide, from
     * x = 630 to x = 10 is +20, not -620.
     */
    public Vector delta(Vector from, Vector to) {
        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        dx -= width * Math.round(dx / width);
        dy -= height * Math.round(dy / height);
        return new Vector(dx, dy);
    }
    // --8<-- [end:delta]

    public double distance(Vector a, Vector b) {
        return delta(a, b).length();
    }

    // --8<-- [start:between]
    /**
     * The point {@code alpha} (0 to 1) of the way from {@code previous} to {@code current}, for drawing between
     * two update steps. A thing that just crossed an edge is drawn on the short way, just outside the field,
     * not flying back across the whole screen; {@link #forEachCopy} then draws it on both sides.
     */
    public Vector between(Vector previous, Vector current, double alpha) {
        Vector step = delta(previous, current);
        return new Vector(current.x() - step.x() * (1 - alpha), current.y() - step.y() * (1 - alpha));
    }
    // --8<-- [end:between]

    // --8<-- [start:copies]
    /**
     * Calls {@code copy} for every place a circle at {@code x}, {@code y} with {@code radius} must be drawn:
     * once where it is, and again on the far side of each edge it reaches over. Near a corner that makes
     * four places. The position may lie a little outside the field, as {@link #between} gives it.
     */
    public void forEachCopy(double x, double y, double radius, Copy copy) {
        x = x(x);
        y = y(y);
        double otherX = x - radius < 0 ? x + width : x + radius > width ? x - width : Double.NaN;
        double otherY = y - radius < 0 ? y + height : y + radius > height ? y - height : Double.NaN;
        copy.at(x, y);
        if (!Double.isNaN(otherX)) copy.at(otherX, y);
        if (!Double.isNaN(otherY)) copy.at(x, otherY);
        if (!Double.isNaN(otherX) && !Double.isNaN(otherY)) copy.at(otherX, otherY);
    }
    // --8<-- [end:copies]
}
