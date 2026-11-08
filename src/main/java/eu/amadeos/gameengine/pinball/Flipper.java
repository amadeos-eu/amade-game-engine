package eu.amadeos.gameengine.pinball;

/**
 * A flipper: a bat that turns about its pivot when its button is held. Its outline is a tapered capsule,
 * {@code baseRadius} round at the pivot and {@code tipRadius} at the tip, {@code length} units apart.
 *
 * <p>Angles are in radians, 0 pointing right and growing clockwise, since y points down the table. A left
 * flipper rests pointing down to the right (about 0.5) and swings up to about -0.5; a right flipper is its
 * mirror image, resting at π - 0.5 and swinging to π + 0.5. The flipper turns at {@code speed} radians per
 * second, up while the button is held and back down when it is let go, a little slower.
 *
 * <p>What makes a flipper a flipper is that its surface moves: a point at distance d from the pivot moves at
 * {@code d × angularVelocity}. The {@link Playfield} bounces the ball off the flipper relative to that
 * moving surface, so the tip throws harder than the base and a ball caught while the flipper rises leaves
 * fast. See {@link #surfaceVelocity}.
 */
public final class Flipper {
    public final String id;
    public final double px, py;
    public final double length, baseRadius, tipRadius;
    public final double rest, up;
    public final double speed;
    public final int layer;
    private double angle;
    private double angularVelocity;
    private boolean held;

    public Flipper(String id, double px, double py, double length, double baseRadius, double tipRadius, double rest,
                   double up, double speed, int layer) {
        this.id = id;
        this.px = px;
        this.py = py;
        this.length = length;
        this.baseRadius = baseRadius;
        this.tipRadius = tipRadius;
        this.rest = rest;
        this.up = up;
        this.speed = speed;
        this.layer = layer;
        this.angle = rest;
    }

    /** A left flipper (tip to the right) or right flipper (tip to the left), resting {@code drop} radians below level. */
    public static Flipper of(String id, boolean left, double px, double py, double length, double drop, double swing,
                             int layer) {
        double rest = left ? drop : Math.PI - drop;
        double up = left ? drop - swing : Math.PI - drop + swing;
        return new Flipper(id, px, py, length, 5.5, 2.8, rest, up, 28, layer);
    }

    public void hold(boolean held) {
        this.held = held;
    }

    public boolean held() {
        return held;
    }

    public double angle() {
        return angle;
    }

    public double angularVelocity() {
        return angularVelocity;
    }

    /** How far up the flipper is: 0 at rest, 1 fully up. */
    public double raised() {
        return (angle - rest) / (up - rest);
    }

    // --8<-- [start:turn]
    /** Turns the flipper for a step: towards {@code up} while held, back to {@code rest} when let go. */
    public void turn(double seconds) {
        double target = held ? up : rest;
        double max = (held ? speed : speed * 0.8) * seconds;
        double diff = target - angle;
        double step = Math.max(-max, Math.min(max, diff));
        angle += step;
        angularVelocity = step / seconds;
    }
    // --8<-- [end:turn]

    public double tipX() {
        return px + Math.cos(angle) * length;
    }

    public double tipY() {
        return py + Math.sin(angle) * length;
    }

    /** The point on the flipper's centre line nearest to {@code (x, y)}, as {x, y, radius there}. */
    double[] closest(double x, double y) {
        double dx = Math.cos(angle) * length, dy = Math.sin(angle) * length;
        double t = ((x - px) * dx + (y - py) * dy) / (length * length);
        t = Math.max(0, Math.min(1, t));
        return new double[]{px + dx * t, py + dy * t, baseRadius + (tipRadius - baseRadius) * t};
    }

    // --8<-- [start:surface]
    /** How fast the flipper's surface moves at {@code (x, y)}: the angular velocity times the arm, at right angles. */
    public double[] surfaceVelocity(double x, double y) {
        return new double[]{-angularVelocity * (y - py), angularVelocity * (x - px)};
    }
    // --8<-- [end:surface]
}
