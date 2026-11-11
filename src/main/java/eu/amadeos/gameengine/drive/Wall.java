package eu.amadeos.gameengine.drive;

import java.awt.Color;

/**
 * A wall beside the road, facing it: the cliff of a canyon, a row of houses, the wall of a tunnel. It runs
 * along the road at {@code offset} road half-widths from the centre (on the side of the segment it belongs
 * to) and is about {@code height} units high. A row of houses changes height every few segments and has
 * windows; a tunnel wall is tiled below and concrete above, and holds up the ceiling.
 *
 * <p>A cliff is a mass of rock, not a sheet: {@code offset} is the nearest its face comes to the road, and
 * from there it steps back into niches up to {@link #RELIEF} half-widths deep and forward again into
 * buttresses ({@link #offsetAt}). Its top is a row of flat heads of different heights ({@link #heightAt}),
 * and behind it, further from the road, stands a second, higher step of the same rock
 * ({@link #backOffsetAt}, {@link #backHeightAt}). Its face is banded in the colours given, from the bottom
 * up, and the bands are broken where the rock is ({@link #faultAt}). All of this follows from the seed and
 * the number of the segment alone, so every segment may hold the same {@code Wall}, and the pieces drawn
 * for neighbouring segments meet exactly. A cliff {@link #between} two segments rises out of the ground at
 * its ends.
 */
// --8<-- [start:wall]
public record Wall(Kind kind, double offset, double height, Color[] colours, int seed, boolean litWindows,
                   long from, long to) {
    public enum Kind { CLIFF, HOUSES, TUNNEL }

    /** How deep the niches of a cliff are, in road half-widths. */
    public static final double RELIEF = 2.6;
    /** Over this many segments a cliff with an end rises out of the ground. */
    public static final int RISE = 18;

    public static Wall cliff(double offset, double height, int seed, Color... strata) {
        return new Wall(Kind.CLIFF, offset, height, strata, seed, false, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public static Wall houses(double offset, double height, int seed, boolean litWindows, Color... facades) {
        return new Wall(Kind.HOUSES, offset, height, facades, seed, litWindows, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public static Wall tunnel(double offset, double height) {
        return new Wall(Kind.TUNNEL, offset, height, new Color[]{new Color(170, 168, 150), new Color(110, 108, 100)},
                0, true, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    /** The same wall, beginning at segment {@code from} and ending at {@code to}: a cliff rises and sinks there. */
    public Wall between(long from, long to) {
        return new Wall(kind, offset, height, colours, seed, litWindows, from, to);
    }
    // --8<-- [end:wall]

    // --8<-- [start:cliff]
    /** How much of a cliff stands at the start of segment {@code index}: 0 at its ends, 1 well inside. */
    public double risen(long index) {
        if (kind != Kind.CLIFF) return 1;
        double a = from == Long.MIN_VALUE ? 1 : (index - from) / (double) RISE;
        double b = to == Long.MAX_VALUE ? 1 : (to - index) / (double) RISE;
        double t = Math.max(0, Math.min(1, Math.min(a, b)));
        return t * t * (3 - 2 * t);
    }

    /**
     * Smooth noise cut into {@code levels} flat steps: long stretches of one value and sudden changes
     * between them, which is what rock looks like from the side and from above.
     */
    private static double stepped(double x, int seed, int levels) {
        double n = Noise.fbm(x, seed, 2);
        // noise gathers round the middle: spread it, so the lowest and the highest step occur too
        n = Math.max(0, Math.min(0.9999, (n - 0.5) * 2.1 + 0.5));
        return Math.floor(n * levels) / (levels - 1);
    }

    /** How far a cliff's face stands back from its nearest point here, 0 to 1: a buttress, a niche. */
    private double relief(long index) {
        return 0.82 * stepped(index * 0.043, seed + 11, 4) + 0.18 * Noise.noise(index * 0.23, seed + 12);
    }

    /** The wall's distance from the centre at the start of segment {@code index}, in road half-widths. */
    public double offsetAt(long index) {
        if (kind != Kind.CLIFF) return offset;
        return offset + RELIEF * relief(index) + 2.5 * (1 - risen(index));
    }

    /** The wall's height at the start of segment {@code index}: flat heads for a cliff, steps for houses. */
    public double heightAt(long index) {
        return switch (kind) {
            case TUNNEL -> height;
            case HOUSES -> height * (0.6 + 0.8 * Noise.hash(index / 4, seed));
            case CLIFF -> height * risen(index)
                    * (0.5 + 0.5 * stepped(index * 0.031, seed + 13, 4) + 0.1 * Noise.noise(index * 0.19, seed + 14));
        };
    }

    /** The second step of a cliff, behind the first: its distance from the centre. */
    public double backOffsetAt(long index) {
        return offset + RELIEF + 0.9 + 2.4 * stepped(index * 0.027, seed + 21, 3) + 0.3 * Noise.noise(index * 0.17, seed + 22)
                + 3 * (1 - risen(index));
    }

    /** The height of the second step: always above the first. */
    public double backHeightAt(long index) {
        return height * risen(index)
                * (1.3 + 0.9 * stepped(index * 0.021, seed + 23, 4) + 0.12 * Noise.noise(index * 0.15, seed + 24));
    }

    /** How far the bands of the rock are shifted up or down at segment {@code index}: a fault every few segments. */
    public double faultAt(long index) {
        long block = Math.floorDiv(index + (long) (Noise.hash(seed, 31) * 40), 23);
        return height * 0.09 * (Noise.hash(block, seed + 32) - 0.5);
    }

    /** Where layer {@code band} of {@code bands} begins, as a part of the rock's greatest height: thick and thin layers. */
    public double layer(int band, int bands) {
        if (band <= 0) return 0;
        if (band >= bands) return 1;
        return (band + 0.7 * (Noise.hash(band, seed + 33) - 0.5)) / bands;
    }
    // --8<-- [end:cliff]
}
