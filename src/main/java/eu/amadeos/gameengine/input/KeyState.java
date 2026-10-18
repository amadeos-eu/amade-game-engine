package eu.amadeos.gameengine.input;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Which keys are held, and which went down since the last look, for games that ask "is this key down?"
 * many times a second instead of reacting to events.
 *
 * <p>Two problems of real keyboards are handled here:
 * <ul>
 *   <li><b>Key repeat on X11.</b> A held key makes X11 send release and press again and again, a few
 *       milliseconds apart. A release is therefore only believed when no press of the same key follows
 *       within {@link #REPEAT_WINDOW}.</li>
 *   <li><b>Short taps.</b> A key pressed and released between two looks would be missed by "is it down?".
 *       {@link #wasPressed} remembers every press until the next {@link #poll}.</li>
 * </ul>
 *
 * <p>No AWT: events come in as key codes and nanosecond times, so tests can play any sequence. Not
 * thread-safe; {@link Keyboard} synchronises.
 */
// --8<-- [start:key-state]
public final class KeyState {
    /** How long a release waits for the press that would make it a key repeat. */
    public static final long REPEAT_WINDOW = 25_000_000;

    private final Set<Integer> down = new HashSet<>();
    private final Map<Integer, Long> releasing = new HashMap<>();
    private final Set<Integer> pressedSincePoll = new HashSet<>();
    private Set<Integer> pressed = Set.of();

    public void pressed(int key, long now) {
        if (releasing.remove(key) != null) return;    // release and press of a key repeat: still held
        if (down.add(key)) pressedSincePoll.add(key);  // a new press (repeated presses are ignored)
    }

    public void released(int key, long now) {
        if (down.contains(key)) releasing.put(key, now);
    }

    /** Takes the presses since the last poll and lets releases older than the window take effect. */
    public void poll(long now) {
        for (Iterator<Map.Entry<Integer, Long>> it = releasing.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Long> release = it.next();
            if (now - release.getValue() >= REPEAT_WINDOW) {
                down.remove(release.getKey());
                it.remove();
            }
        }
        pressed = Set.copyOf(pressedSincePoll);
        pressedSincePoll.clear();
    }

    /** Forgets all held keys at once, e.g. when the window loses the focus and their releases never come. */
    public void releaseAll() {
        down.clear();
        releasing.clear();
    }

    /** True while the key is held, as of the last poll and the events since. */
    public boolean isDown(int key) {
        return down.contains(key);
    }

    /** True if the key went down between the last two polls, however short the press was. */
    public boolean wasPressed(int key) {
        return pressed.contains(key);
    }
}
// --8<-- [end:key-state]
