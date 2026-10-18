package eu.amadeos.gameengine.loop;

/**
 * The arithmetic of a fixed time step: real time comes in uneven pieces (a frame takes 15 ms, the next
 * 18 ms), the rules advance in equal steps. Time not yet used for a step is kept for the next frame; the
 * rest of a step is the {@link #alpha()} with which the drawing interpolates between the last two steps.
 *
 * <p>No thread and no clock: the caller passes the elapsed time, so tests can feed any sequence.
 */
// --8<-- [start:fixed-step]
public final class FixedStep {
    private final long step;
    private final int maxSteps;
    private long accumulated;

    /**
     * @param stepNanos length of one step in nanoseconds
     * @param maxSteps  most steps per frame; after a long pause (a debugger, a suspended laptop) the game
     *                  does not try to catch up for minutes but simply continues
     */
    public FixedStep(long stepNanos, int maxSteps) {
        if (stepNanos <= 0 || maxSteps < 1) throw new IllegalArgumentException();
        this.step = stepNanos;
        this.maxSteps = maxSteps;
    }

    /** Adds the time since the last frame and returns how many steps to run now. */
    public int advance(long elapsedNanos) {
        accumulated += Math.max(0, elapsedNanos);
        long steps = accumulated / step;
        if (steps > maxSteps) {
            accumulated = 0;
            return maxSteps;
        }
        accumulated -= steps * step;
        return (int) steps;
    }

    /** How far the time is between the last step and the next, 0 to 1, for drawing in between. */
    public double alpha() {
        return (double) accumulated / step;
    }

    /** One step in seconds, the {@code dt} of the rules. */
    public double seconds() {
        return step / 1e9;
    }
}
// --8<-- [end:fixed-step]
