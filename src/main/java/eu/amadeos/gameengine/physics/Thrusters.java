package eu.amadeos.gameengine.physics;

/**
 * The jets of a craft that flies against gravity: a lunar lander, a cab in the air, a jetpack. Each jet
 * pushes the {@link Body} one way, as hard as it can or not at all, and burns fuel while it does; an empty
 * tank pushes nothing. The jet under the craft is the strong one, since it has to beat gravity; the side
 * jets only steer.
 *
 * <p>The pushes are accelerations in units per second per second, like gravity. A main jet of 1.6 times
 * gravity lifts a craft at 0.6 g; one weaker than gravity only slows the fall.
 *
 * <pre>
 * Thrusters jets = new Thrusters(1.6 * GRAVITY, 0.8 * GRAVITY, 0.6 * GRAVITY, 4);
 * // in each step, before the body moves:
 * fuel -= jets.fire(cab, controls.axis(LEFT, RIGHT), up ? -1 : down ? 1 : 0, fuel, seconds);
 * cab.fall(GRAVITY, MAX_FALL, seconds);
 * </pre>
 */
// --8<-- [start:thrusters]
public final class Thrusters {
    private final double up;
    private final double side;
    private final double down;
    private final double burn;

    /**
     * Jets pushing up (the main one, under the craft), to the sides and down, and the fuel the main jet burns
     * in a second; the side and down jets burn in proportion to their push.
     */
    public Thrusters(double up, double side, double down, double burn) {
        if (up < 0 || side < 0 || down < 0 || burn < 0) throw new IllegalArgumentException("negative thrust or burn");
        this.up = up;
        this.side = side;
        this.down = down;
        this.burn = burn;
    }

    /**
     * Fires the jets for a step: {@code x} -1 pushes left, 1 right; {@code y} -1 pushes up (the main jet),
     * 1 down; 0 neither. Returns the fuel burned, at most {@code fuel}; with less fuel left than the step
     * needs, the jets push for that part of the step only.
     */
    public double fire(Body body, double x, double y, double fuel, double seconds) {
        double wanted = rate(x, y) * seconds;
        if (wanted <= 0 || fuel <= 0) return 0;
        double part = Math.min(1, fuel / wanted);
        double time = seconds * part;
        body.vx += Math.signum(x) * Math.min(1, Math.abs(x)) * side * time;
        if (y < 0) body.vy -= Math.min(1, -y) * up * time;
        else if (y > 0) body.vy += Math.min(1, y) * down * time;
        return wanted * part;
    }
    // --8<-- [end:thrusters]

    /** The fuel the jets burn in a second, fired so. */
    public double rate(double x, double y) {
        if (up == 0) return 0;
        double perUnit = burn / up;
        double vertical = y < 0 ? Math.min(1, -y) * up : Math.min(1, Math.max(0, y)) * down;
        return (Math.min(1, Math.abs(x)) * side + vertical) * perUnit;
    }

    public double up() {
        return up;
    }

    public double side() {
        return side;
    }

    public double down() {
        return down;
    }
}
