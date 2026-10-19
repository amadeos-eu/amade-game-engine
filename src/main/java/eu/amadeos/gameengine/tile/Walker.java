package eu.amadeos.gameengine.tile;

/**
 * Something that walks the corridors of a {@link TileMap}: a player, a ghost, a unit. It moves from the
 * centre of one tile to the centre of the next, and only at a centre may it turn or stop, so it always
 * stays in the middle of a corridor and never cuts a corner into a wall. Between two centres it can only
 * go on, or turn round ({@link #turnAround}).
 *
 * <p>Positions are in tiles: the centre of the tile in column 3, row 5 is (3, 5), and a walker halfway to
 * the right of it is at (3.5, 5). The drawing multiplies by the size of a tile. Where it goes at a centre
 * is not the walker's business: a {@link Steering} decides, which knows the walls, the keys or the plan.
 */
// --8<-- [start:walker]
public final class Walker {
    /** The decision at the centre of a tile. */
    @FunctionalInterface
    public interface Steering {
        /**
         * The way to go on from the centre of {@code col}, {@code row}, walking in {@code heading} so far
         * ({@code null} if standing); {@code null} to stop here. Only return ways that are open.
         */
        Direction choose(int col, int row, Direction heading);
    }

    private double x;
    private double y;
    private double previousX;
    private double previousY;
    /** The way it walks; {@code null} while it stands at a centre. */
    private Direction heading;

    public Walker(double x, double y, Direction heading) {
        place(x, y, heading);
    }

    /**
     * Walks {@code distance} tiles, asking {@code steering} at every centre it reaches, and returns the
     * distance actually walked: less if it stopped. Steps over an edge of the map arrive on the far side.
     */
    public double walk(double distance, TileMap<?> map, Steering steering) {
        previousX = x;
        previousY = y;
        double left = distance;
        while (left > 0) {
            if (atCentre()) {
                heading = steering.choose(col(), row(), heading);
                if (heading == null) break;
            }
            boolean horizontal = heading.horizontal();
            int sign = heading.dx + heading.dy;
            double along = horizontal ? x : y;
            // The next centre ahead: the next whole number in the walking direction.
            double centre = sign > 0 ? Math.floor(along) + 1 : Math.ceil(along) - 1;
            double gap = Math.abs(centre - along);
            double step = Math.min(gap, left);
            double now = step == gap ? centre : along + sign * step;
            if (horizontal) x = now;
            else y = now;
            left -= step;
            wrap(map.width(), map.height());
        }
        return distance - left;
    }
    // --8<-- [end:walker]

    /** Over an edge: the walker, and its previous position with it, moves to the far side. */
    private void wrap(int width, int height) {
        if (x < -0.5) {
            x += width;
            previousX += width;
        } else if (x >= width - 0.5) {
            x -= width;
            previousX -= width;
        }
        if (y < -0.5) {
            y += height;
            previousY += height;
        } else if (y >= height - 0.5) {
            y -= height;
            previousY -= height;
        }
    }

    /** Puts the walker somewhere, e.g. at its start after a lost life; no movement to draw in between. */
    public void place(double x, double y, Direction heading) {
        this.x = x;
        this.y = y;
        this.previousX = x;
        this.previousY = y;
        this.heading = heading;
    }

    /** Back the way it came, at once, also between two centres. */
    public void turnAround() {
        if (heading != null) heading = heading.opposite();
    }

    /** True at the centre of a tile, the only place to turn or stop. */
    public boolean atCentre() {
        return x == Math.rint(x) && y == Math.rint(y);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    /** Where it was before the last {@link #walk}, for drawing between two steps. */
    public double previousX() {
        return previousX;
    }

    public double previousY() {
        return previousY;
    }

    /** The column of the tile it is in: the nearest centre. */
    public int col() {
        return (int) Math.round(x);
    }

    public int row() {
        return (int) Math.round(y);
    }

    public Cell cell() {
        return new Cell(col(), row());
    }

    public Direction heading() {
        return heading;
    }
}
