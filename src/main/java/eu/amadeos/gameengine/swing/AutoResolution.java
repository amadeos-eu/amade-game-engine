package eu.amadeos.gameengine.swing;

import java.util.Arrays;

/**
 * Chooses how small {@link RenderMode#AUTO} draws the world: at the screen's own size (a divisor of 1), or
 * at half or a third of it, enlarged afterwards. It watches how long the frames take and goes one step
 * coarser when they are late, and one step sharper again when it expects the sharper picture to be in time.
 *
 * <p>The rules are those of a careful driver. Half a second of frames decides, by their median, so that one
 * slow frame (a cache being filled, the garbage collector) changes nothing. Going coarser is quick; going
 * sharper needs the prediction to lie well below the limit, and after a step that had to be taken back the
 * next attempt waits twice as long. What a step is worth is learned from the last time it was taken: the
 * frames before against the frames after.
 *
 * <p>No clock and no screen of its own: the canvas passes each frame's time, so tests can feed any sequence.
 */
// --8<-- [start:auto-resolution]
public final class AutoResolution {
    /** Frames slower than this, in milliseconds, are late: 50 frames per second. */
    public static final double LATE = 20;
    /** A sharper picture is tried when its frames are expected to take no longer than this. */
    public static final double EASY = 15;
    private static final int WINDOW = 30;
    private static final double WINDOW_MILLIS = 500;
    private static final int FEWEST = 6;
    /** Frames after a step that do not count: the sprite copies of the new size are being made. */
    private static final int SETTLE = 8;
    private static final double FIRST_HOLD = 4_000, LONGEST_HOLD = 120_000, TAKEN_BACK = 10_000;

    private final double[] window = new double[WINDOW];
    private int frames;
    private double millis;
    private int settling;
    private int divisor = 1;
    private int most = 1;
    /** {@code worth[d]}: how many times longer the frames at divisor {@code d - 1} took than at {@code d}; 0 unknown. */
    private double[] worth = new double[8];
    private double beforeStep;
    /** Milliseconds of frames since the last step, and how many must pass before the next sharper one. */
    private double sinceStep;
    private double hold = FIRST_HOLD;
    private boolean wentSharper;

    /** The world is drawn at the screen's size divided by this. */
    public int divisor() {
        return divisor;
    }

    /**
     * The coarsest step allowed: the screen's scale, rounded, so that the world is never drawn much smaller
     * than its logical size. A new screen size starts the watching afresh.
     */
    public void limit(int mostDivisor) {
        int m = Math.clamp(mostDivisor, 1, worth.length - 1);
        if (m == most) return;
        most = m;
        divisor = Math.min(divisor, most);
        Arrays.fill(worth, 0);
        hold = FIRST_HOLD;
        wentSharper = false;
        beforeStep = 0;
        restart();
    }

    /** One frame took {@code frameMillis} from the first drawing call to the screen. */
    public void frame(double frameMillis) {
        sinceStep += frameMillis;
        if (settling > 0) {
            settling--;
            return;
        }
        window[frames++] = frameMillis;
        millis += frameMillis;
        if (frames < WINDOW && (millis < WINDOW_MILLIS || frames < FEWEST)) return;
        Arrays.sort(window, 0, frames);
        double median = window[frames / 2];
        frames = 0;
        millis = 0;
        if (beforeStep > 0) {
            // The first window after a step: what the step was worth.
            int coarser = wentSharper ? divisor + 1 : divisor;
            worth[coarser] = wentSharper ? median / beforeStep : beforeStep / median;
            beforeStep = 0;
        }
        if (median > LATE && divisor < most) {
            // A sharper step that has to be taken back at once was too optimistic: wait longer before the next.
            if (wentSharper && sinceStep < TAKEN_BACK) hold = Math.min(LONGEST_HOLD, hold * 2);
            step(divisor + 1, median, false);
        } else if (divisor > 1 && sinceStep >= hold && median * worth(divisor) <= EASY) {
            step(divisor - 1, median, true);
        }
    }

    /** How many times longer a frame at {@code d - 1} takes than at {@code d}: learned, or guessed from the pixels. */
    private double worth(int d) {
        if (worth[d] > 0) return worth[d];
        // Twice the pixels in each direction, but not everything a frame costs grows with them.
        return Math.pow(d / (d - 1.0), 1.5);
    }

    private void step(int to, double median, boolean sharper) {
        divisor = to;
        beforeStep = median;
        wentSharper = sharper;
        restart();
    }

    private void restart() {
        frames = 0;
        millis = 0;
        sinceStep = 0;
        settling = SETTLE;
    }
}
// --8<-- [end:auto-resolution]
