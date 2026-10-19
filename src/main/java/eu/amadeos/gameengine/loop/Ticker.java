package eu.amadeos.gameengine.loop;

/**
 * A clock for rules that move in ticks of their own, slower than the update steps: a snake that moves one
 * cell every 150 ms, a piece that falls one row every half second. The game loop still steps 120 times a
 * second, for the input and the effects; the ticker counts those steps' time and says when a tick is due.
 *
 * <pre>
 * Ticker ticker = new Ticker(0.15);
 * // in each update step:
 * for (int i = ticker.advance(seconds); i &gt; 0; i--) game.tick();
 * ticker.setInterval(game.interval());   // the rules may change their pace
 * // when drawing:
 * double between = ticker.progress(alpha * seconds);   // 0 just after a tick, 1 just before the next
 * </pre>
 *
 * <p>No thread and no clock, like {@link FixedStep}: the caller passes the time, so tests can feed any.
 */
// --8<-- [start:ticker]
public final class Ticker {
    private static final double EPSILON = 1e-9;

    private double interval;
    private double elapsed;
    private long count;

    /** @param interval seconds between two ticks */
    public Ticker(double interval) {
        setInterval(interval);
    }

    /** Adds {@code seconds} of game time and returns how many ticks are due now, usually 0 or 1. */
    public int advance(double seconds) {
        elapsed += Math.max(0, seconds);
        int due = 0;
        // A nanosecond of tolerance: 120 steps of 1/120 s add up to 0.99999999 s in floating point, and a
        // tick due at exactly one second must not wait for the next step.
        while (elapsed + EPSILON >= interval) {
            elapsed = Math.max(0, elapsed - interval);
            due++;
        }
        count += due;
        return due;
    }

    /**
     * How far the time is from the last tick to the next, 0 to 1, for drawing in between.
     *
     * @param ahead time that has passed since the last update step, usually {@code alpha} times the step
     */
    public double progress(double ahead) {
        return Math.clamp((elapsed + Math.max(0, ahead)) / interval, 0, 1);
    }

    /** Changes the pace from the next tick on; the time since the last tick is kept. */
    public void setInterval(double interval) {
        if (!(interval > 0)) throw new IllegalArgumentException("interval must be positive: " + interval);
        this.interval = interval;
    }

    public double interval() {
        return interval;
    }

    /** The ticks so far. */
    public long count() {
        return count;
    }

    /** Back to the start: no ticks, no time. */
    public void reset() {
        elapsed = 0;
        count = 0;
    }
}
// --8<-- [end:ticker]
