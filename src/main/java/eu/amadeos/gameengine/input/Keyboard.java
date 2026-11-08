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
    // --8<-- [start:sides]
    /**
     * Shift, Control and Alt exist twice; a game that tells them apart (a pinball's flippers on the two Shift
     * keys) asks for these codes: the key's code with the side added. The plain code is down when either is.
     */
    public static final int LEFT_SIDE = 0x1_0000;
    public static final int RIGHT_SIDE = 0x2_0000;
    public static final int LEFT_SHIFT = LEFT_SIDE | KeyEvent.VK_SHIFT;
    public static final int RIGHT_SHIFT = RIGHT_SIDE | KeyEvent.VK_SHIFT;
    public static final int LEFT_CONTROL = LEFT_SIDE | KeyEvent.VK_CONTROL;
    public static final int RIGHT_CONTROL = RIGHT_SIDE | KeyEvent.VK_CONTROL;
    // --8<-- [end:sides]

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
                long now = clock.getAsLong();
                pressed(e.getKeyCode(), now);
                int side = side(e);
                if (side != 0) pressed(side | e.getKeyCode(), now);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                long now = clock.getAsLong();
                released(e.getKeyCode(), now);
                int side = side(e);
                if (side != 0) released(side | e.getKeyCode(), now);
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

    /** {@link #LEFT_SIDE} or {@link #RIGHT_SIDE} for a key that exists on both sides, else 0. */
    static int side(KeyEvent e) {
        return switch (e.getKeyLocation()) {
            case KeyEvent.KEY_LOCATION_LEFT -> LEFT_SIDE;
            case KeyEvent.KEY_LOCATION_RIGHT -> RIGHT_SIDE;
            default -> 0;
        };
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
