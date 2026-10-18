package eu.amadeos.gameengine.input;

import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.LongSupplier;

/**
 * The keyboard of a game: listens to the key events of a component on the Swing thread and answers the game
 * loop's questions on its own thread. The bookkeeping is {@link KeyState}; this class connects it to AWT and
 * makes it safe between the two threads.
 */
public final class Keyboard {
    private final KeyState state = new KeyState();
    private final LongSupplier clock;

    /** @param clock nanoseconds for the key times, {@link System#nanoTime()} in the game, a counter in tests */
    public Keyboard(LongSupplier clock) {
        this.clock = clock;
    }

    public Keyboard() {
        this(System::nanoTime);
    }

    /** Listens to a component's keys; it must be focusable and have the focus to receive them. */
    public void attach(Component component) {
        component.setFocusable(true);
        component.setFocusTraversalKeysEnabled(false);   // Tab is a key like any other in a game
        component.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                pressed(e.getKeyCode(), clock.getAsLong());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                released(e.getKeyCode(), clock.getAsLong());
            }
        });
        // Keys held while the window loses the focus never report their release.
        component.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                releaseAll();
            }
        });
    }

    public synchronized void pressed(int key, long now) {
        state.pressed(key, now);
    }

    public synchronized void released(int key, long now) {
        state.released(key, now);
    }

    /** Call once per update step on the game loop thread, before asking. */
    public synchronized void poll() {
        state.poll(clock.getAsLong());
    }

    public synchronized boolean isDown(int key) {
        return state.isDown(key);
    }

    public synchronized boolean wasPressed(int key) {
        return state.wasPressed(key);
    }

    private synchronized void releaseAll() {
        state.releaseAll();
    }
}
