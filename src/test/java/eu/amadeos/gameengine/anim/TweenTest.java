package eu.amadeos.gameengine.anim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TweenTest {
    @Test
    void linearMovesEvenly() {
        Tween tween = new Tween(1000, 100, Easing.LINEAR);
        assertEquals(0.0, tween.at(900));
        assertEquals(0.0, tween.at(1000));
        assertEquals(0.25, tween.at(1025), 1e-9);
        assertEquals(1.0, tween.at(1100));
        assertEquals(1.0, tween.at(5000));
        assertFalse(tween.done(1099));
        assertTrue(tween.done(1100));
    }

    @Test
    void easingsStartAtZeroAndEndAtOne() {
        for (Easing e : new Easing[]{Easing.LINEAR, Easing.EASE_OUT, Easing.EASE_IN_OUT}) {
            assertEquals(0.0, e.apply(0), 1e-9);
            assertEquals(1.0, e.apply(1), 1e-9);
        }
        assertTrue(Easing.EASE_OUT.apply(0.5) > 0.5, "ease-out covers most of the way early");
        assertEquals(0.5, Easing.EASE_IN_OUT.apply(0.5), 1e-9);
    }

    @Test
    void lerpInterpolates() {
        assertEquals(15.0, Tween.lerp(10, 20, 0.5));
        assertEquals(20.0, Tween.lerp(10, 20, 1));
        assertEquals(1_000_000, Tween.millis(1));
    }
}
