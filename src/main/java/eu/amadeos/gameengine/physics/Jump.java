package eu.amadeos.gameengine.physics;

/**
 * The jump of a platformer, with the three small kindnesses that make it feel right rather than exact:
 *
 * <ul>
 *   <li><b>Coyote time</b>: a player who presses jump a moment <i>after</i> running off a ledge still
 *   jumps, as the cartoon coyote keeps running on air until he looks down. The eye sees the feet at the
 *   edge; the rules already let them fall.</li>
 *   <li><b>A jump buffer</b>: a press a moment <i>before</i> landing is kept and carried out on landing.
 *   Without it, a press four hundredths of a second too early does nothing, and the game feels deaf.</li>
 *   <li><b>A variable height</b>: letting go of the button while still rising cuts the rising speed, so a
 *   tap is a hop and a held button the full jump.</li>
 * </ul>
 *
 * <p>The speed of a jump follows from the height it should reach: rising at {@code v} against gravity
 * {@code g}, a body stops after {@code v / g} seconds, at the height {@code v² / 2g}. So
 * {@link #speedFor(double, double)} turns "four tiles high" into a speed, and the level designer can rely
 * on it.
 *
 * <pre>
 * Jump jump = new Jump(Jump.speedFor(4.5 * 16, GRAVITY), 0.08, 0.1, 0.4);
 * // in each update step, before the body falls and moves:
 * if (jump.update(player, controls.wasPressed(JUMP), controls.isDown(JUMP), seconds)) sounds.jump();
 * </pre>
 */
// --8<-- [start:jump]
public final class Jump {
    /** Steps of 1/120 s add up with a remainder; a nanosecond of tolerance, as in the Ticker. */
    private static final double EPSILON = 1e-9;

    private final double speed;
    private final double coyote;
    private final double buffer;
    private final double cut;
    /** Seconds since the body last stood on the ground, and since the button was last pressed. */
    private double sinceGround = Double.POSITIVE_INFINITY;
    private double sinceRequest = Double.POSITIVE_INFINITY;
    /** Rising from a jump, while letting go may still cut it. */
    private boolean rising;

    /**
     * @param speed  the upward speed a jump starts with, units per second
     * @param coyote seconds after leaving the ground in which a jump is still allowed
     * @param buffer seconds a press is kept before landing
     * @param cut    the part of the rising speed kept when the button is let go, 0 … 1; 1 for a fixed height
     */
    public Jump(double speed, double coyote, double buffer, double cut) {
        this.speed = speed;
        this.coyote = coyote;
        this.buffer = buffer;
        this.cut = cut;
    }

    /** The starting speed that reaches {@code height} against {@code gravity}: v = √(2gh). */
    public static double speedFor(double height, double gravity) {
        return Math.sqrt(2 * gravity * height);
    }

    /** The height a starting speed reaches: h = v² / 2g. */
    public static double heightOf(double speed, double gravity) {
        return speed * speed / (2 * gravity);
    }

    /**
     * One update step, after the last move (so the body knows whether it stands) and before this step's
     * fall and move. Starts a jump if one is due and returns true in that step.
     *
     * @param pressed the button went down in this step
     * @param held    the button is down
     */
    public boolean update(Body body, boolean pressed, boolean held, double seconds) {
        sinceGround = body.onGround ? 0 : sinceGround + seconds;
        sinceRequest = pressed ? 0 : sinceRequest + seconds;
        if (body.onGround) rising = false;
        if (sinceRequest <= buffer + EPSILON && sinceGround <= coyote + EPSILON) {
            body.vy = -speed;
            // Both used up: no second jump from the same ground or the same press.
            sinceGround = Double.POSITIVE_INFINITY;
            sinceRequest = Double.POSITIVE_INFINITY;
            rising = true;
            return true;
        }
        if (rising && body.vy >= 0) rising = false;      // the top is reached
        if (rising && !held) {
            body.vy *= cut;
            rising = false;
        }
        return false;
    }
    // --8<-- [end:jump]

    /**
     * Throws the body up without the button: the bounce off a stomped enemy, a spring.
     *
     * @param cuttable true if letting go cuts it as it cuts a jump, so that holding the button bounces
     *                 higher; false for the full height in any case
     */
    public void launch(Body body, double launchSpeed, boolean cuttable) {
        body.vy = -launchSpeed;
        sinceGround = Double.POSITIVE_INFINITY;
        rising = cuttable;
    }

    /** Forgets presses and ground: a new life, a new level. */
    public void reset() {
        sinceGround = Double.POSITIVE_INFINITY;
        sinceRequest = Double.POSITIVE_INFINITY;
        rising = false;
    }

    public double speed() {
        return speed;
    }

    /** True while rising from a jump or a launch and the button still decides how high. */
    public boolean rising() {
        return rising;
    }
}
