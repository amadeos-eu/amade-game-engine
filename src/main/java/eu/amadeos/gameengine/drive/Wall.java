package eu.amadeos.gameengine.drive;

import java.awt.Color;

/**
 * A wall beside the road, facing it: the cliff of a canyon, a row of houses, the wall of a tunnel. It runs
 * along the road at {@code offset} road half-widths from the centre (on the side of the segment it belongs
 * to) and is about {@code height} units high. A cliff's top is rough and its face banded in the colours given,
 * from the bottom up; a row of houses changes height every few segments and has windows; a tunnel wall is
 * tiled below and concrete above, and holds up the ceiling.
 */
// --8<-- [start:wall]
public record Wall(Kind kind, double offset, double height, Color[] colours, int seed, boolean litWindows) {
    public enum Kind { CLIFF, HOUSES, TUNNEL }

    public static Wall cliff(double offset, double height, int seed, Color... strata) {
        return new Wall(Kind.CLIFF, offset, height, strata, seed, false);
    }

    public static Wall houses(double offset, double height, int seed, boolean litWindows, Color... facades) {
        return new Wall(Kind.HOUSES, offset, height, facades, seed, litWindows);
    }

    public static Wall tunnel(double offset, double height) {
        return new Wall(Kind.TUNNEL, offset, height, new Color[]{new Color(170, 168, 150), new Color(110, 108, 100)}, 0, true);
    }

    /** The wall's height at the start of segment {@code index}: rough for a cliff, stepped for houses. */
    public double heightAt(long index) {
        return switch (kind) {
            case TUNNEL -> height;
            case HOUSES -> height * (0.6 + 0.8 * Noise.hash(index / 4, seed));
            case CLIFF -> height * (0.55 + 0.75 * Noise.fbm(index * 0.11, seed, 4));
        };
    }
}
// --8<-- [end:wall]
