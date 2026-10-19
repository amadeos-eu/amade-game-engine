package eu.amadeos.gameengine.collision;

/**
 * An axis-aligned rectangle in logical coordinates: a brick, a paddle, a ball. {@code x} and {@code y} are
 * the top left corner. Plain arithmetic, no AWT, so rules that use it stay testable without a screen.
 */
public record Box(double x, double y, double width, double height) {
    public Box {
        if (width < 0 || height < 0) throw new IllegalArgumentException("negative size");
    }

    public double right() {
        return x + width;
    }

    public double bottom() {
        return y + height;
    }

    public double centerX() {
        return x + width / 2;
    }

    public double centerY() {
        return y + height / 2;
    }

    /** The same box, moved by {@code dx}, {@code dy}. */
    public Box moved(double dx, double dy) {
        return new Box(x + dx, y + dy, width, height);
    }

    // --8<-- [start:overlaps]
    /** True if the two boxes share an area; boxes that only touch at an edge do not overlap. */
    public boolean overlaps(Box other) {
        return x < other.right() && other.x < right() && y < other.bottom() && other.y < bottom();
    }
    // --8<-- [end:overlaps]
}
