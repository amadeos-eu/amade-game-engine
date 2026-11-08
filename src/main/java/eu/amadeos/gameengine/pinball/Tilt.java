package eu.amadeos.gameengine.pinball;

/**
 * The tilt bob: nudging the table moves it, and it settles again over time. A nudge while it still swings
 * from the last one is a warning; after {@code warnings} warnings in one ball, the next one tilts the game.
 */
public final class Tilt {
    public enum Result { NUDGE, WARNING, TILT }

    private final int warnings;
    private double swing;
    private int given;
    private boolean tilted;

    public Tilt(int warnings) {
        this.warnings = warnings;
    }

    // --8<-- [start:nudge]
    /** A nudge: what it did. The bob swings by 1 and calms down by 1 a second. */
    public Result nudge() {
        if (tilted) return Result.TILT;
        boolean swinging = swing > 0.4;
        swing += 1;
        if (!swinging) return Result.NUDGE;
        given++;
        if (given > warnings) {
            tilted = true;
            return Result.TILT;
        }
        return Result.WARNING;
    }
    // --8<-- [end:nudge]

    public void settle(double seconds) {
        swing = Math.max(0, swing - seconds);
    }

    public int warningsGiven() {
        return given;
    }

    public boolean tilted() {
        return tilted;
    }

    /** A new ball: warnings and tilt are forgotten. */
    public void reset() {
        swing = 0;
        given = 0;
        tilted = false;
    }
}
