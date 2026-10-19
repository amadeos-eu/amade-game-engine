package eu.amadeos.gameengine.math;

/**
 * Angles in radians, as {@link Vector} uses them. An angle has many names: π/2, 5π/2 and -3π/2 point the same
 * way. Turning towards a target must take the short way round, so these methods bring angles into one range.
 */
public final class Angles {
    public static final double FULL_TURN = 2 * Math.PI;

    private Angles() {
    }

    /** The same direction as {@code angle}, between -π (exclusive) and π (inclusive). */
    public static double normalize(double angle) {
        double a = angle % FULL_TURN;     // -2π … 2π
        if (a <= -Math.PI) a += FULL_TURN;
        else if (a > Math.PI) a -= FULL_TURN;
        return a;
    }

    // --8<-- [start:difference]
    /**
     * How far to turn from {@code from} to {@code to} the short way round, -π to π: positive is clockwise on
     * the screen. From 170° to -170° is +20°, not -340°.
     */
    public static double difference(double from, double to) {
        return normalize(to - from);
    }
    // --8<-- [end:difference]

    /** Turns {@code from} towards {@code to} by at most {@code maxStep}, the short way, without overshooting. */
    public static double turnTowards(double from, double to, double maxStep) {
        double difference = difference(from, to);
        if (Math.abs(difference) <= maxStep) return from + difference;
        return from + Math.copySign(maxStep, difference);
    }
}
