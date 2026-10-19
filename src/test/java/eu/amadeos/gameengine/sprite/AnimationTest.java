package eu.amadeos.gameengine.sprite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationTest {
    /** The frames shown at 0, 0.1, 0.2 ... seconds. */
    private static int[] frames(Animation animation, int count) {
        int[] shown = new int[count];
        for (int i = 0; i < count; i++) shown[i] = animation.frame(i * 0.1 + 0.05);
        return shown;
    }

    // --8<-- [start:test-animation]
    @Test
    void theThreeWaysToGoOn() {
        assertArrayEquals(new int[]{4, 5, 6, 4, 5, 6, 4},
                frames(new Animation(Animation.Mode.LOOP, 0.1, 4, 5, 6), 7));
        assertArrayEquals(new int[]{4, 5, 6, 6, 6, 6, 6},
                frames(new Animation(Animation.Mode.ONCE, 0.1, 4, 5, 6), 7));
        assertArrayEquals(new int[]{4, 5, 6, 7, 6, 5, 4, 5, 6},
                frames(new Animation(Animation.Mode.PING_PONG, 0.1, 4, 5, 6, 7), 9));
    }
    // --8<-- [end:test-animation]

    @Test
    void anAnimationOnceKnowsWhenItIsOver() {
        Animation boom = new Animation(Animation.Mode.ONCE, 0.25, 0, 1, 2, 3);
        assertEquals(1.0, boom.duration(), 1e-12);
        assertFalse(boom.finished(0.99));
        assertTrue(boom.finished(1.0));
        assertFalse(new Animation(Animation.Mode.LOOP, 0.25, 0, 1).finished(100));
    }

    @Test
    void oneFrameAndTimesBeforeTheStartAreFine() {
        assertEquals(9, new Animation(Animation.Mode.PING_PONG, 0.1, 9).frame(5));
        assertEquals(1, new Animation(Animation.Mode.LOOP, 0.1, 1, 2).frame(-3));
        assertThrows(IllegalArgumentException.class, () -> new Animation(Animation.Mode.LOOP, 0.1));
    }
}
