package eu.amadeos.gameengine.loop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;
import java.util.function.DoubleConsumer;

/**
 * The heart of a real-time game: a thread of its own that, again and again, runs the commands posted from
 * other threads, advances the rules in fixed steps and draws a frame.
 *
 * <pre>
 * GameLoop loop = new GameLoop(120, 60, scenes::update, canvas::render);
 * loop.start();
 * loop.post(() -&gt; scenes.replace(new MatchScene(...)));   // from the Swing thread, e.g. a menu
 * </pre>
 *
 * <p>Everything the game state is touched by happens on this one thread: updates, drawing and the posted
 * commands. So the state needs no locks.
 */
public final class GameLoop {
    private static final Logger LOG = LoggerFactory.getLogger(GameLoop.class);

    private final FixedStep step;
    private final long frameNanos;
    private final DoubleConsumer update;
    private final DoubleConsumer render;
    private final Queue<Runnable> commands = new ConcurrentLinkedQueue<>();
    private volatile boolean running;
    private Thread thread;

    /**
     * @param updatesPerSecond steps of the rules per second, e.g. 120
     * @param framesPerSecond  frames drawn per second at most, e.g. 60
     * @param update           advances the rules by the given seconds, one fixed step
     * @param render           draws a frame; the argument is {@link FixedStep#alpha()}
     */
    public GameLoop(int updatesPerSecond, int framesPerSecond, DoubleConsumer update, DoubleConsumer render) {
        this.step = new FixedStep(1_000_000_000L / updatesPerSecond, 8);
        this.frameNanos = 1_000_000_000L / framesPerSecond;
        this.update = update;
        this.render = render;
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(this::run, "game-loop");
        thread.setDaemon(true);
        thread.start();
    }

    /** Stops the loop and waits for the thread to end. */
    public synchronized void stop() {
        running = false;
        if (thread != null && thread != Thread.currentThread()) {
            try {
                thread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean running() {
        return running;
    }

    /** Runs a command on the loop thread before the next update; safe to call from any thread. */
    public void post(Runnable command) {
        commands.add(command);
    }

    // --8<-- [start:run]
    private void run() {
        long last = System.nanoTime();
        while (running) {
            long frameStart = System.nanoTime();
            int steps = step.advance(frameStart - last);
            last = frameStart;
            try {
                for (Runnable command; (command = commands.poll()) != null; ) command.run();
                for (int i = 0; i < steps; i++) update.accept(step.seconds());
                render.accept(step.alpha());
            } catch (RuntimeException e) {
                // One broken frame must not end the game; the log tells what happened.
                LOG.error("Error in the game loop", e);
            }
            // Sleep away the rest of the frame instead of burning a CPU core.
            long rest = frameNanos - (System.nanoTime() - frameStart);
            if (rest > 0) LockSupport.parkNanos(rest);
        }
    }
    // --8<-- [end:run]
}
