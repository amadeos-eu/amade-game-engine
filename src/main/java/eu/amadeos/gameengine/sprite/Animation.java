package eu.amadeos.gameengine.sprite;

import java.util.Arrays;

/**
 * Which frame of a {@link SpriteSheet} to show at a given time: the frames of an animation in order, each
 * shown for the same number of seconds. It only counts; it draws nothing and holds no clock, so one
 * animation can serve forty insects, each asking with its own time, and a test can ask without a screen.
 *
 * <pre>
 * Animation flap = new Animation(Animation.Mode.LOOP, 0.25, 0, 1);    // frames 0 and 1, a quarter second each
 * sheet.frame(flap.frame(seconds)).draw(g, x, y);
 * </pre>
 */
// --8<-- [start:animation]
public final class Animation {
    /** What happens after the last frame. */
    public enum Mode {
        /** Starts again with the first: wings, flames, blinking lights. */
        LOOP,
        /** Stays on the last: an explosion, a door that opens. */
        ONCE,
        /** Runs back to the first and forth again, without showing the ends twice: breathing, swaying. */
        PING_PONG
    }

    private final Mode mode;
    private final double secondsPerFrame;
    private final int[] frames;

    /** @param frames the numbers of the frames in the sheet, in the order they are shown */
    public Animation(Mode mode, double secondsPerFrame, int... frames) {
        if (frames.length == 0 || secondsPerFrame <= 0) throw new IllegalArgumentException("empty animation");
        this.mode = mode;
        this.secondsPerFrame = secondsPerFrame;
        this.frames = frames.clone();
    }

    /** The frame to show {@code seconds} after the animation started. */
    public int frame(double seconds) {
        int step = (int) Math.floor(Math.max(0, seconds) / secondsPerFrame);
        int n = frames.length;
        int index = switch (mode) {
            case LOOP -> step % n;
            case ONCE -> Math.min(step, n - 1);
            case PING_PONG -> {
                if (n == 1) yield 0;
                int round = 2 * (n - 1);         // 0 1 2 3 2 1 | 0 1 2 3 2 1 | ...
                int at = step % round;
                yield at < n ? at : round - at;
            }
        };
        return frames[index];
    }
    // --8<-- [end:animation]

    /** True once an animation that runs {@link Mode#ONCE} has shown its last frame to the end. */
    public boolean finished(double seconds) {
        return mode == Mode.ONCE && seconds >= duration();
    }

    /** Seconds for one run through all frames. */
    public double duration() {
        return frames.length * secondsPerFrame;
    }

    public Mode mode() {
        return mode;
    }

    @Override
    public String toString() {
        return "Animation[" + mode + ", " + secondsPerFrame + " s, " + Arrays.toString(frames) + "]";
    }
}
