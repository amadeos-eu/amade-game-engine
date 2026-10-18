package eu.amadeos.gameengine.swing;

import javax.swing.Timer;
import java.awt.Toolkit;
import java.util.function.LongPredicate;

/**
 * Drives animations in a Swing component: calls a frame function about 60 times a second on the event
 * thread with the current {@link System#nanoTime()}, for as long as the function says there is more to
 * show. Starting it while it runs does nothing, so every change can simply call {@link #start()}.
 *
 * <pre>
 * Animator animator = new Animator(now -&gt; {
 *     boolean more = updatePositions(now);
 *     repaint();
 *     return more;
 * });
 * </pre>
 */
public final class Animator {
    /** Milliseconds between frames: about 60 frames a second. */
    public static final int FRAME = 16;

    private final Timer timer;

    /** @param frame updates the animated values for the time given and returns false once all are done */
    public Animator(LongPredicate frame) {
        timer = new Timer(FRAME, null);
        timer.addActionListener(e -> {
            boolean more = frame.test(System.nanoTime());
            // Without this, X11 may hold the drawing back and the animation stutters.
            Toolkit.getDefaultToolkit().sync();
            if (!more) timer.stop();
        });
        timer.setCoalesce(true);
    }

    public void start() {
        if (!timer.isRunning()) timer.start();
    }

    public void stop() {
        timer.stop();
    }

    public boolean running() {
        return timer.isRunning();
    }
}
