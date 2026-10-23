package eu.amadeos.gameengine.physics;

/**
 * How a craft came down on a landing place: gently, hard but whole, or too fast. Speeds in units per second;
 * down is positive, as on the screen. Sideways speed counts double, because a craft that slides along the
 * ground tips over sooner than one that sits down a little hard.
 *
 * <pre>
 * switch (Touchdown.of(cab.vx, cab.vy, 40, 90)) {
 *     case SOFT -> park();
 *     case HARD -> { park(); tip -= 1; }
 *     case CRASH -> crash();
 * }
 * </pre>
 */
// --8<-- [start:touchdown]
public enum Touchdown {
    SOFT, HARD, CRASH;

    /** SOFT up to {@code soft}, HARD up to {@code hard}, CRASH beyond: of the downward speed and twice the sideways one. */
    public static Touchdown of(double vx, double vy, double soft, double hard) {
        if (soft < 0 || hard < soft) throw new IllegalArgumentException("need 0 <= soft <= hard");
        double speed = Math.max(Math.max(0, vy), 2 * Math.abs(vx));
        if (speed <= soft) return SOFT;
        if (speed <= hard) return HARD;
        return CRASH;
    }
}
// --8<-- [end:touchdown]
