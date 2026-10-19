package eu.amadeos.gameengine.collision;

import eu.amadeos.gameengine.math.Wrap;

/**
 * A circle in logical coordinates: the hit area of a round thing, a rock, a ship, a bullet. Things that turn
 * are best caught by circles, because a circle looks the same at every angle; a {@link Box} would have to
 * turn with them. Plain arithmetic, no AWT, like {@link Box}.
 */
// --8<-- [start:circle]
public record Circle(double x, double y, double radius) {
    public Circle {
        if (radius < 0) throw new IllegalArgumentException("negative radius");
    }

    /**
     * True if the two circles share an area: their centres are closer than both radii together. Compared
     * squared, so no square root is needed; circles that only touch do not overlap.
     */
    public boolean overlaps(Circle other) {
        return overlaps(other.x - x, other.y - y, radius + other.radius);
    }

    /** {@link #overlaps(Circle)} on a field without edges: the circles may touch across an edge. */
    public boolean overlaps(Circle other, Wrap wrap) {
        double dx = other.x - x;
        double dy = other.y - y;
        dx -= wrap.width() * Math.round(dx / wrap.width());
        dy -= wrap.height() * Math.round(dy / wrap.height());
        return overlaps(dx, dy, radius + other.radius);
    }

    /** True if the point lies inside the circle. */
    public boolean contains(double px, double py) {
        return overlaps(px - x, py - y, radius);
    }

    private static boolean overlaps(double dx, double dy, double distance) {
        return dx * dx + dy * dy < distance * distance;
    }
}
// --8<-- [end:circle]
