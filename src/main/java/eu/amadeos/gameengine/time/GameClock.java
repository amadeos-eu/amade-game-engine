package eu.amadeos.gameengine.time;

import java.util.function.LongSupplier;

/**
 * Time of one game: starts with the first move, stops when the game ends. It reads the time from a
 * {@link LongSupplier} of nanoseconds, {@link System#nanoTime()} in the game and a number the test moves
 * forward in tests.
 */
// --8<-- [start:clock]
public final class GameClock {
    private final LongSupplier nanoTime;
    private long started = -1;
    private long stopped = -1;

    public GameClock(LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
    }

    public GameClock() {
        this(System::nanoTime);
    }

    public void reset() {
        started = -1;
        stopped = -1;
    }

    public void start() {
        if (started < 0) started = nanoTime.getAsLong();
    }

    public void stop() {
        if (started >= 0 && stopped < 0) stopped = nanoTime.getAsLong();
    }

    public boolean running() {
        return started >= 0 && stopped < 0;
    }

    public long millis() {
        if (started < 0) return 0;
        long end = stopped >= 0 ? stopped : nanoTime.getAsLong();
        return (end - started) / 1_000_000;
    }

    /**
     * Seconds as games show them and high scores record them: a started second counts, so the clock shows 1
     * right after the first move.
     */
    public int seconds() {
        if (started < 0) return 0;
        return (int) Math.min(Integer.MAX_VALUE, millis() / 1000 + 1);
    }
}
// --8<-- [end:clock]
