package eu.amadeos.gameengine.raycast;

/**
 * Walking on a {@link GridMap}: a body is a circle of a given radius that may not overlap a wall or a closed
 * door. A move that runs into a wall keeps the part along it, so the body slides along walls instead of
 * sticking to them; that is what makes running through corridors feel smooth.
 */
// --8<-- [start:mover]
public final class Mover {
    /** Where a body ended up. */
    public record Position(double x, double y) { }

    private Mover() {
    }

    /** True if a body at ({@code x}, {@code y}) would overlap a blocked cell. */
    public static boolean overlaps(GridMap map, double x, double y, double radius) {
        int x0 = (int) Math.floor(x - radius), x1 = (int) Math.floor(x + radius);
        int y0 = (int) Math.floor(y - radius), y1 = (int) Math.floor(y + radius);
        for (int cy = y0; cy <= y1; cy++) for (int cx = x0; cx <= x1; cx++) {
            if (!map.blocks(cx, cy)) continue;
            // the nearest point of the cell's square to the centre
            double nx = Math.max(cx, Math.min(x, cx + 1)), ny = Math.max(cy, Math.min(y, cy + 1));
            if ((nx - x) * (nx - x) + (ny - y) * (ny - y) < radius * radius) return true;
        }
        return false;
    }

    /**
     * Moves a body by ({@code dx}, {@code dy}): first along x, then along y, each in small steps so a fast
     * body cannot jump through a thin wall, each stopped by what it runs into.
     */
    public static Position slide(GridMap map, double x, double y, double dx, double dy, double radius) {
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / (radius * 0.5)));
        double sx = dx / steps, sy = dy / steps;
        for (int i = 0; i < steps; i++) {
            if (sx != 0 && !overlaps(map, x + sx, y, radius)) x += sx;
            if (sy != 0 && !overlaps(map, x, y + sy, radius)) y += sy;
        }
        return new Position(x, y);
    }
}
// --8<-- [end:mover]
