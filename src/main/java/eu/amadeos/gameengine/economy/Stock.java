package eu.amadeos.gameengine.economy;

import java.util.EnumMap;
import java.util.Map;

/**
 * Goods of many kinds and how many of each: the logs and planks in a storehouse, the grain a mill waits
 * for. Where {@link Credits} is one number, a stock is one number per kind of good, and each kind can have
 * its own limit: a mill keeps a few sacks of grain at its door, a storehouse keeps hundreds.
 *
 * <pre>
 * Stock&lt;Good&gt; mill = new Stock&lt;&gt;(Good.class);
 * mill.limit(Good.GRAIN, 4);                 // more is not brought
 * mill.add(Good.GRAIN, 1);                   // a carrier put a sack down
 * if (mill.take(Good.GRAIN, 1)) grind();
 * int wanted = mill.room(Good.GRAIN);        // what the carriers should still bring
 * </pre>
 *
 * @param <G> the kinds of goods, an enum
 */
// --8<-- [start:stock]
public final class Stock<G extends Enum<G>> {
    private final Class<G> kinds;
    private final EnumMap<G, Integer> counts;
    private final EnumMap<G, Integer> limits;

    public Stock(Class<G> kinds) {
        this.kinds = kinds;
        this.counts = new EnumMap<>(kinds);
        this.limits = new EnumMap<>(kinds);
    }

    public int count(G good) {
        return counts.getOrDefault(good, 0);
    }

    /** How many {@code good} may be kept; {@link Integer#MAX_VALUE} unless a limit was set. */
    public int limit(G good) {
        return limits.getOrDefault(good, Integer.MAX_VALUE);
    }

    public void limit(G good, int limit) {
        if (limit < 0) throw new IllegalArgumentException("negative limit " + limit);
        limits.put(good, limit);
    }

    /** How many more {@code good} fit. */
    public int room(G good) {
        return Math.max(0, limit(good) - count(good));
    }

    /** Adds {@code count} of {@code good}, as far as they fit, and returns how many did not. */
    public int add(G good, int count) {
        if (count < 0) throw new IllegalArgumentException("negative count " + count);
        int taken = Math.min(count, room(good));
        if (taken > 0) counts.merge(good, taken, Integer::sum);
        return count - taken;
    }

    /** Takes {@code count} of {@code good} if there are that many; true if they were taken. */
    public boolean take(G good, int count) {
        if (count < 0) throw new IllegalArgumentException("negative count " + count);
        int have = count(good);
        if (have < count) return false;
        if (have == count) counts.remove(good);
        else counts.put(good, have - count);
        return true;
    }
    // --8<-- [end:stock]

    /** True if there are at least the counts of {@code price}. */
    public boolean has(Map<G, Integer> price) {
        for (Map.Entry<G, Integer> entry : price.entrySet()) {
            if (count(entry.getKey()) < entry.getValue()) return false;
        }
        return true;
    }

    /** Takes all of {@code price}, or nothing if a part is missing; true if it was taken. */
    public boolean pay(Map<G, Integer> price) {
        if (!has(price)) return false;
        for (Map.Entry<G, Integer> entry : price.entrySet()) take(entry.getKey(), entry.getValue());
        return true;
    }

    /** Every good there is, with its count, in the order of the enum. Read only. */
    public Map<G, Integer> counts() {
        return java.util.Collections.unmodifiableMap(counts);
    }

    /** The sum of all counts. */
    public int total() {
        int total = 0;
        for (int count : counts.values()) total += count;
        return total;
    }

    /** Sets the count of {@code good} directly, past its limit, as a saved game had it. */
    public void set(G good, int count) {
        if (count < 0) throw new IllegalArgumentException("negative count " + count);
        if (count == 0) counts.remove(good);
        else counts.put(good, count);
    }

    public Class<G> kinds() {
        return kinds;
    }
}
