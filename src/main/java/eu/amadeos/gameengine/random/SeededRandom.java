package eu.amadeos.gameengine.random;

import java.util.random.RandomGenerator;

/**
 * Random numbers that repeat exactly: the same seed gives the same numbers, on every computer, with every
 * Java version. That is what replays, tests of whole games and a "game of the day" need.
 *
 * <p>The algorithm is SplitMix64, a few lines of arithmetic written out here, so no library update can ever
 * change a sequence. The methods a game uses ({@link #nextInt(int)}, {@link #nextDouble()},
 * {@link #nextBoolean()} …) are written out too; the other methods of {@link RandomGenerator} work, but their
 * results depend on Java's implementation.
 *
 * <pre>
 * SeededRandom rules = new SeededRandom(seed);   // where the food appears
 * SeededRandom looks = rules.fork();             // splinters and sparks, which must not change the game
 * </pre>
 *
 * <p>Not thread-safe; use one per thread, usually the game loop's.
 */
// --8<-- [start:seeded-random]
public final class SeededRandom implements RandomGenerator {
    private final long seed;
    private long state;

    public SeededRandom(long seed) {
        this.seed = seed;
        this.state = seed;
    }

    /** A new seed from the clock, for a game that should differ from the last one. */
    public static long freshSeed() {
        return new SeededRandom(System.nanoTime() ^ System.currentTimeMillis()).nextLong();
    }

    /** The seed this generator started with; with it, the same numbers can be drawn again. */
    public long seed() {
        return seed;
    }

    @Override
    public long nextLong() {
        long z = state += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** 0 inclusive to {@code bound} exclusive, every value equally likely. */
    @Override
    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive: " + bound);
        // 31 random bits; the few largest values that would favour small results are drawn again.
        int bits;
        int value;
        do {
            bits = (int) (nextLong() >>> 33);
            value = bits % bound;
        } while (bits - value + (bound - 1) < 0);
        return value;
    }
    // --8<-- [end:seeded-random]

    @Override
    public int nextInt() {
        return (int) (nextLong() >>> 32);
    }

    @Override
    public int nextInt(int origin, int bound) {
        if (origin >= bound) throw new IllegalArgumentException("empty range " + origin + ".." + bound);
        return origin + nextInt(bound - origin);
    }

    /** 0 inclusive to 1 exclusive, in steps of 2⁻⁵³. */
    @Override
    public double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    @Override
    public boolean nextBoolean() {
        return nextLong() < 0;
    }

    // --8<-- [start:fork]
    /**
     * A second generator whose seed is drawn from this one: its numbers are independent of this one's, and
     * drawing from it never changes what this one draws next.
     */
    public SeededRandom fork() {
        return new SeededRandom(nextLong());
    }
    // --8<-- [end:fork]

    /** A generator at exactly this one's position: both draw the same numbers from here on. */
    public SeededRandom copy() {
        SeededRandom copy = new SeededRandom(seed);
        copy.state = state;
        return copy;
    }
}
