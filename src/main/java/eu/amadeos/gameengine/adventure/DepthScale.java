package eu.amadeos.gameengine.adventure;

/**
 * How large people appear at a height of the picture. A painted room has depth: whoever walks to the back
 * wall walks up the screen and must become smaller, and slower, or they seem to climb a wall instead of
 * walking away. Two heights are enough for a floor seen from the front: the scale at the back
 * ({@code farY}) and at the front ({@code nearY}). Between them the scale changes in a straight line,
 * beyond them it stays as at the nearer end.
 *
 * <pre>
 * DepthScale depth = new DepthScale(262, 0.85, 400, 1.15);   // smaller at the wall, larger at the front
 * double k = depth.at(maria.position().y());
 * sprite.draw(g, x - width * k / 2, y - height * k);           // feet at the position
 * </pre>
 */
// --8<-- [start:depth]
public record DepthScale(double farY, double farScale, double nearY, double nearScale) {
    /** Everyone the same size everywhere: for rooms seen from the side. */
    public static final DepthScale FLAT = new DepthScale(0, 1, 1, 1);

    public DepthScale {
        if (farY == nearY) throw new IllegalArgumentException("far and near need different heights");
        if (farScale <= 0 || nearScale <= 0) throw new IllegalArgumentException("scales must be positive");
    }

    /** The scale of someone whose feet are at height {@code y}. */
    public double at(double y) {
        double t = (y - farY) / (nearY - farY);
        t = Math.max(0, Math.min(1, t));
        return farScale + (nearScale - farScale) * t;
    }
}
// --8<-- [end:depth]
