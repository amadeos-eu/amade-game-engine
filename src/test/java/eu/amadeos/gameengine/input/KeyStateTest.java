package eu.amadeos.gameengine.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyStateTest {
    private static final int UP = 38;
    private static final long MS = 1_000_000;

    // --8<-- [start:test-repeat]
    @Test
    void x11KeyRepeatDoesNotLookLikeReleases() {
        KeyState keys = new KeyState();
        keys.pressed(UP, 0);
        keys.poll(1 * MS);
        assertTrue(keys.wasPressed(UP));
        // Holding the key: X11 sends release and press again, 1 ms apart, every 30 ms.
        for (long t = 500 * MS; t < 1000 * MS; t += 30 * MS) {
            keys.released(UP, t);
            keys.pressed(UP, t + MS);
            keys.poll(t + 10 * MS);
            assertTrue(keys.isDown(UP));
            assertFalse(keys.wasPressed(UP), "a repeat is not a new press");
        }
        keys.released(UP, 2000 * MS);                     // really let go
        keys.poll(2010 * MS);
        assertTrue(keys.isDown(UP), "not yet: a press may still follow");
        keys.poll(2030 * MS);
        assertFalse(keys.isDown(UP));
    }
    // --8<-- [end:test-repeat]

    @Test
    void aTapBetweenTwoPollsIsNotLost() {
        KeyState keys = new KeyState();
        keys.pressed(UP, 0);
        keys.released(UP, 5 * MS);
        keys.poll(50 * MS);
        assertTrue(keys.wasPressed(UP));
        assertFalse(keys.isDown(UP));
        keys.poll(60 * MS);
        assertFalse(keys.wasPressed(UP), "reported once");
    }

    @Test
    void repeatedPressesWithoutReleaseCountOnce() {
        KeyState keys = new KeyState();
        keys.pressed(UP, 0);
        keys.poll(MS);
        keys.pressed(UP, 30 * MS);
        keys.poll(31 * MS);
        assertFalse(keys.wasPressed(UP));
        keys.releaseAll();
        assertFalse(keys.isDown(UP));
    }
}
