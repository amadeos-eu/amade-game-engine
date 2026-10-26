package eu.amadeos.gameengine.physics;

/**
 * Something thrown or fired that flies freely: a shell, a stone, a grenade. A point with a velocity, moved
 * by {@link Ballistics}. Like a {@link Body} it keeps where it was before the last step, so it can be drawn
 * between two steps.
 *
 * <p>Angles are in degrees as on a protractor laid on the screen: 0 points right, 90 straight up, 180 left.
 * Since y grows downwards on the screen, "up" is a negative vertical speed.
 */
public final class Projectile {
    public double x;
    public double y;
    /** Velocity in units per second. */
    public double vx;
    public double vy;
    private double previousX;
    private double previousY;

    public Projectile(double x, double y, double vx, double vy) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        previousX = x;
        previousY = y;
    }

    // --8<-- [start:launch]
    /** Fired from {@code x}, {@code y} at {@code degrees} (0 right, 90 up) with {@code speed} units per second. */
    public static Projectile launch(double x, double y, double degrees, double speed) {
        double rad = Math.toRadians(degrees);
        return new Projectile(x, y, Math.cos(rad) * speed, -Math.sin(rad) * speed);
    }
    // --8<-- [end:launch]

    /** An independent copy. */
    public Projectile copy() {
        Projectile p = new Projectile(x, y, vx, vy);
        p.previousX = previousX;
        p.previousY = previousY;
        return p;
    }

    /** Remembers the position as the one before the coming step; {@link Ballistics#step} calls it. */
    public void remember() {
        previousX = x;
        previousY = y;
    }

    /** Where to draw it, {@code alpha} of the way from the last step to the current one. */
    public double x(double alpha) {
        return previousX + (x - previousX) * alpha;
    }

    public double y(double alpha) {
        return previousY + (y - previousY) * alpha;
    }

    /** The direction it flies in, in radians as Java2D turns: 0 right, positive clockwise on the screen. */
    public double heading() {
        return Math.atan2(vy, vx);
    }

    public double speed() {
        return Math.hypot(vx, vy);
    }
}
