package eu.amadeos.gameengine.tile;

/**
 * The four ways to go on a map of tiles. The order is a rule of its own: when two ways are equally good,
 * the one listed first wins (up, then left, then down, then right), so a choice never depends on chance
 * or on the order of a loop somewhere else.
 */
// --8<-- [start:direction]
public enum Direction {
    UP(0, -1), LEFT(-1, 0), DOWN(0, 1), RIGHT(1, 0);

    /** One step in this direction, in columns and rows; rows count downwards, as on the screen. */
    public final int dx;
    public final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public Direction opposite() {
        return switch (this) {
            case UP -> DOWN;
            case LEFT -> RIGHT;
            case DOWN -> UP;
            case RIGHT -> LEFT;
        };
    }

    public boolean horizontal() {
        return dx != 0;
    }

    /** The angle on the screen, for drawing: 0 to the right, π/2 downwards. */
    public double angle() {
        return Math.atan2(dy, dx);
    }
}
// --8<-- [end:direction]
