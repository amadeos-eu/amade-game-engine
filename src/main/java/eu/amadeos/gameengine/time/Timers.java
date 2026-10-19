package eu.amadeos.gameengine.time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Many countdowns at once, each belonging to a key: the fuse of every bomb, the flames on every tile, the
 * blinking of a power-up. {@link #update} lets the time pass for all of them and hands over those that ran
 * out, <b>in the order they ran out</b>, even when several did so within the same step.
 *
 * <p>That order is the point. A step of 1/120 second is short, but a game with many timers will see two of
 * them end in the same step, and what happens first can matter: the bomb laid first explodes first. Of two
 * that ran out at exactly the same moment, the one started first comes first, so the order never depends on
 * a hash code or on the order of a loop elsewhere.
 *
 * <pre>
 * Timers&lt;Bomb&gt; fuses = new Timers&lt;&gt;();
 * fuses.start(bomb, 2.5);
 * // in each update step:
 * fuses.update(seconds, this::explode);
 * </pre>
 *
 * <p>No thread and no clock, like {@link eu.amadeos.gameengine.loop.Ticker}: the caller passes the time.
 *
 * @param <K> what a timer belongs to; keys are compared with {@code equals}
 */
// --8<-- [start:timers]
public final class Timers<K> {
    /** A nanosecond of tolerance, as in the Ticker: 300 steps of 1/120 s leave 2.5 s at 1e-15, not at 0. */
    private static final double EPSILON = 1e-9;

    private static final class Timer {
        double left;
        long order;
    }

    private final Map<K, Timer> timers = new LinkedHashMap<>();
    private final List<Map.Entry<K, Timer>> due = new ArrayList<>();
    private long started;

    /** Starts the timer of {@code key} with {@code seconds}; a running one starts again. */
    public void start(K key, double seconds) {
        Timer timer = timers.computeIfAbsent(key, k -> new Timer());
        timer.left = seconds;
        timer.order = started++;
    }

    /**
     * Lets {@code seconds} pass. The timers that ran out are removed and handed to {@code expired}, the one
     * that ran out first first. Timers the handler starts count from the next step on.
     */
    public void update(double seconds, Consumer<? super K> expired) {
        due.clear();
        for (Map.Entry<K, Timer> entry : timers.entrySet()) {
            Timer timer = entry.getValue();
            timer.left -= seconds;
            if (timer.left <= EPSILON) due.add(Map.entry(entry.getKey(), timer));
        }
        if (due.isEmpty()) return;
        // The most negative ran out earliest within the step; equal times by the order they were started.
        due.sort((a, b) -> {
            int byTime = Double.compare(a.getValue().left, b.getValue().left);
            return byTime != 0 ? byTime : Long.compare(a.getValue().order, b.getValue().order);
        });
        for (Map.Entry<K, Timer> entry : due) timers.remove(entry.getKey(), entry.getValue());
        for (Map.Entry<K, Timer> entry : due) expired.accept(entry.getKey());
    }
    // --8<-- [end:timers]

    public boolean running(K key) {
        return timers.containsKey(key);
    }

    /** Seconds left on the timer of {@code key}; 0 if it is not running. */
    public double left(K key) {
        Timer timer = timers.get(key);
        return timer == null ? 0 : Math.max(0, timer.left);
    }

    /** Stops the timer of {@code key} without handing it over; true if it was running. */
    public boolean cancel(K key) {
        return timers.remove(key) != null;
    }

    /** The keys of all running timers, in the order they were first started. Read only. */
    public Iterable<K> keys() {
        return Collections.unmodifiableSet(timers.keySet());
    }

    public int size() {
        return timers.size();
    }

    public boolean isEmpty() {
        return timers.isEmpty();
    }

    /** Stops all timers. */
    public void clear() {
        timers.clear();
    }
}
