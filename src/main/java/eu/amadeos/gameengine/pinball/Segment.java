package eu.amadeos.gameengine.pinball;

/**
 * A straight piece of wall from {@code (ax, ay)} to {@code (bx, by)}, {@code thickness} units thick, on one
 * floor of the table. Walls, guides, rubbers stretched between posts, the kicking side of a slingshot, drop
 * targets and standups are all segments; they differ in their {@link Material} and in what the game does
 * when the {@link Playfield} reports a hit by their {@code id}.
 *
 * <p>A segment can be switched off ({@link #down}): a drop target that was hit falls below the field and the
 * ball rolls over it. A gate lets the ball through one way only: give it a {@link #passes} direction, and a
 * ball moving that way goes through.
 */
public final class Segment {
    public final String id;
    public final double ax, ay, bx, by;
    public final double thickness;
    public final Material material;
    public final int layer;
    /** A dropped drop target: no longer there for the ball. */
    public boolean down;
    /** For a one-way gate: the direction a ball may pass; null for a wall. */
    public final double[] passes;

    public Segment(String id, double ax, double ay, double bx, double by, double thickness, Material material, int layer) {
        this(id, ax, ay, bx, by, thickness, material, layer, null);
    }

    public Segment(String id, double ax, double ay, double bx, double by, double thickness, Material material, int layer,
                   double[] passes) {
        this.id = id;
        this.ax = ax;
        this.ay = ay;
        this.bx = bx;
        this.by = by;
        this.thickness = thickness;
        this.material = material;
        this.layer = layer;
        this.passes = passes;
    }

    public double length() {
        return Math.hypot(bx - ax, by - ay);
    }

    /** The point of the segment nearest to {@code (x, y)}, as {x, y, t} with t from 0 at a to 1 at b. */
    // --8<-- [start:closest]
    public double[] closest(double x, double y) {
        double dx = bx - ax, dy = by - ay;
        double len2 = dx * dx + dy * dy;
        double t = len2 == 0 ? 0 : ((x - ax) * dx + (y - ay) * dy) / len2;
        t = Math.max(0, Math.min(1, t));
        return new double[]{ax + dx * t, ay + dy * t, t};
    }
    // --8<-- [end:closest]
}
