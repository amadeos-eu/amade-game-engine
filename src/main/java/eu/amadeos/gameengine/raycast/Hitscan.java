package eu.amadeos.gameengine.raycast;

import java.util.List;

/**
 * A shot that arrives at once, along a straight line: what does it hit first, a wall or one of the targets?
 * Targets are circles on the map, as seen from above; height does not count, the way the first 3D games
 * aimed up and down for the player.
 */
// --8<-- [start:hitscan]
public final class Hitscan {
    /** Something a shot can hit. */
    public interface Target {
        double x();

        double y();

        double radius();
    }

    /** Where the shot ended: at {@code target}, or at a wall if that is {@code null}. */
    public record Hit<T>(double distance, double x, double y, T target) {
        public boolean wall() {
            return target == null;
        }
    }

    private Hitscan() {
    }

    /**
     * Shoots from ({@code x}, {@code y}) at {@code angle}, at most {@code range} far. Returns the nearest
     * target in front of the first wall, or where the shot met the wall (or ran out of range).
     */
    public static <T extends Target> Hit<T> shoot(GridMap map, double x, double y, double angle, double range, List<T> targets) {
        double dx = Math.cos(angle), dy = Math.sin(angle);
        GridMap.Ray ray = new GridMap.Ray();
        map.cast(x, y, dx, dy, range, ray);
        double wall = Math.min(range, ray.distance);
        T best = null;
        double bestDistance = wall;
        for (T t : targets) {
            double d = enter(x, y, dx, dy, t.x(), t.y(), t.radius());
            if (d >= 0 && d < bestDistance) {
                bestDistance = d;
                best = t;
            }
        }
        return new Hit<>(bestDistance, x + dx * bestDistance, y + dy * bestDistance, best);
    }

    /** Where a ray from (x, y) along the unit vector (dx, dy) enters a circle; -1 if it misses or the circle is behind. */
    public static double enter(double x, double y, double dx, double dy, double cx, double cy, double r) {
        double ox = cx - x, oy = cy - y, along = ox * dx + oy * dy;
        if (along < 0) return -1;
        double across2 = ox * ox + oy * oy - along * along;
        if (across2 > r * r) return -1;
        return Math.max(0, along - Math.sqrt(r * r - across2));
    }
}
// --8<-- [end:hitscan]
