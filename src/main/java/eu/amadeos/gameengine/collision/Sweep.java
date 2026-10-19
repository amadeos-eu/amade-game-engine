package eu.amadeos.gameengine.collision;

/**
 * Collision of a <i>moving</i> box with a box at rest, tested over the whole way of one step, not only at
 * its end. A fast ball can be in front of a brick in one step and behind it in the next; a test of the end
 * positions alone would let it pass through ("tunnelling").
 *
 * <p>The trick is to shrink the moving box to a point and grow the target by the moving box's size (their
 * <i>Minkowski sum</i>). Then the question is only where a straight line enters a rectangle: for each axis,
 * when does the point enter the target's range and when does it leave it? The box is hit when the point is
 * inside both ranges at once, from the later of the two entries on.
 */
public final class Sweep {
    /**
     * Where the moving box first touches the target.
     *
     * @param time    share of the step, 0 to 1, after which the boxes touch
     * @param normalX -1 or +1 if a left or right face of the target was hit, else 0: the direction to bounce
     * @param normalY -1 or +1 if the top or bottom face was hit, else 0
     */
    public record Hit(double time, int normalX, int normalY) {
    }

    private Sweep() {
    }

    // --8<-- [start:sweep]
    /**
     * The first contact of {@code moving}, moved by {@code dx}, {@code dy} during the step, with
     * {@code target}; {@code null} if there is none within the step, or if the boxes already overlap at the
     * start (then the caller has to separate them first).
     */
    public static Hit sweep(Box moving, double dx, double dy, Box target) {
        // The target grown by the moving box: now the moving box is the point (moving.x, moving.y).
        double left = target.x() - moving.width();
        double right = target.right();
        double top = target.y() - moving.height();
        double bottom = target.bottom();

        double[] x = enterAndLeave(moving.x(), dx, left, right);
        double[] y = enterAndLeave(moving.y(), dy, top, bottom);
        if (x == null || y == null) return null;
        double enter = Math.max(x[0], y[0]);
        double leave = Math.min(x[1], y[1]);
        if (enter >= leave || enter > 1 || enter < 0) return null;
        // The axis entered last is the face that was hit.
        if (x[0] > y[0]) return new Hit(enter, dx > 0 ? -1 : 1, 0);
        return new Hit(enter, 0, dy > 0 ? -1 : 1);
    }

    /**
     * When a point starting at {@code p} and moving by {@code d} enters and leaves the range
     * {@code min..max}, as shares of the step; {@code null} if it never is inside.
     */
    private static double[] enterAndLeave(double p, double d, double min, double max) {
        if (d == 0) {
            // Not moving on this axis: inside the whole time or never.
            return p > min && p < max ? new double[]{Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY} : null;
        }
        double t1 = (min - p) / d;
        double t2 = (max - p) / d;
        return new double[]{Math.min(t1, t2), Math.max(t1, t2)};
    }
    // --8<-- [end:sweep]
}
