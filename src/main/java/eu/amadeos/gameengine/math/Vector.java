package eu.amadeos.gameengine.math;

/**
 * A point or a direction in the plane, in logical coordinates: where a ship is, how fast and where to it
 * flies, where its nose points. Immutable: every operation returns a new vector, so a vector can be passed
 * around and kept without anyone changing it behind your back.
 *
 * <p>Angles are in radians. Angle 0 points right, along x; because y points <i>down</i> on the screen, a
 * positive angle turns <i>clockwise</i>, and -π/2 points up.
 *
 * <pre>
 * Vector nose = Vector.ofAngle(ship.angle(), 1);       // where the ship points, length 1
 * velocity = velocity.plus(nose.times(THRUST * seconds));
 * position = position.plus(velocity.times(seconds));
 * </pre>
 */
// --8<-- [start:vector]
public record Vector(double x, double y) {
    public static final Vector ZERO = new Vector(0, 0);

    /** A vector of {@code length} pointing at {@code angle}. */
    public static Vector ofAngle(double angle, double length) {
        return new Vector(Math.cos(angle) * length, Math.sin(angle) * length);
    }

    public Vector plus(Vector other) {
        return new Vector(x + other.x, y + other.y);
    }

    public Vector minus(Vector other) {
        return new Vector(x - other.x, y - other.y);
    }

    public Vector times(double factor) {
        return new Vector(x * factor, y * factor);
    }
    // --8<-- [end:vector]

    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    /** The length squared: enough to compare lengths, and without the square root. */
    public double lengthSquared() {
        return x * x + y * y;
    }

    /** The angle this vector points at, -π to π; 0 for the zero vector. */
    public double angle() {
        return Math.atan2(y, x);
    }

    /** The same direction with length 1; the zero vector stays zero. */
    public Vector normalized() {
        double length = length();
        return length == 0 ? ZERO : new Vector(x / length, y / length);
    }

    /** The same direction, but no longer than {@code max}: a speed limit. */
    public Vector limited(double max) {
        double squared = lengthSquared();
        return squared <= max * max ? this : times(max / Math.sqrt(squared));
    }

    /** The dot product: positive if both point roughly the same way, 0 at a right angle, negative if opposite. */
    public double dot(Vector other) {
        return x * other.x + y * other.y;
    }

    // --8<-- [start:rotated]
    /** This vector turned by {@code angle} around the origin. */
    public Vector rotated(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vector(x * cos - y * sin, x * sin + y * cos);
    }
    // --8<-- [end:rotated]

    public double distance(Vector other) {
        return Math.hypot(x - other.x, y - other.y);
    }
}
