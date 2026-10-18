package eu.amadeos.gameengine.anim;

import java.util.Objects;

/**
 * One animation in time: when it started, how long it takes and how it eases. It knows nothing about what
 * moves; ask it how far along it is and interpolate your values with {@link #lerp}. Times are
 * {@link System#nanoTime()} values, so tests can pass their own.
 *
 * <pre>
 * Tween tween = new Tween(now, Tween.millis(200), Easing.EASE_OUT);
 * ... every frame:
 * double t = tween.at(System.nanoTime());
 * x = Tween.lerp(fromX, toX, t);
 * </pre>
 */
public final class Tween {
    private final long start;
    private final long duration;
    private final Easing easing;

    public Tween(long start, long duration, Easing easing) {
        if (duration <= 0) throw new IllegalArgumentException("Duration must be positive: " + duration);
        this.start = start;
        this.duration = duration;
        this.easing = Objects.requireNonNull(easing, "easing");
    }

    /** Nanoseconds in a number of milliseconds. */
    public static long millis(long millis) {
        return millis * 1_000_000;
    }

    // --8<-- [start:at]
    /** The eased share of the way at a time: 0 before the start, 1 from the end on. */
    public double at(long now) {
        double t = (double) (now - start) / duration;
        return easing.apply(Math.clamp(t, 0.0, 1.0));
    }

    public boolean done(long now) {
        return now - start >= duration;
    }

    /** The value a share {@code t} of the way from {@code from} to {@code to}. */
    public static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }
    // --8<-- [end:at]
}
