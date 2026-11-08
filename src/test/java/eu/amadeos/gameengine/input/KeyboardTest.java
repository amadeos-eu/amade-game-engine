package eu.amadeos.gameengine.input;

import org.junit.jupiter.api.Test;

import java.awt.Canvas;
import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyboardTest {
    @Test
    void tellsTheTwoShiftKeysApart() {
        long[] now = {0};
        Keyboard keyboard = new Keyboard(() -> now[0]);
        Canvas canvas = new Canvas();
        keyboard.attach(canvas);
        canvas.getKeyListeners()[0].keyPressed(new KeyEvent(canvas, KeyEvent.KEY_PRESSED, 0, KeyEvent.SHIFT_DOWN_MASK, KeyEvent.VK_SHIFT,
                KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_RIGHT));
        keyboard.poll();
        assertTrue(keyboard.isDown(Keyboard.RIGHT_SHIFT));
        assertTrue(keyboard.isDown(KeyEvent.VK_SHIFT));
        assertFalse(keyboard.isDown(Keyboard.LEFT_SHIFT));
        canvas.getKeyListeners()[0].keyReleased(new KeyEvent(canvas, KeyEvent.KEY_RELEASED, 0, 0, KeyEvent.VK_SHIFT,
                KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_RIGHT));
        now[0] += 1_000_000_000L;
        keyboard.poll();
        assertFalse(keyboard.isDown(Keyboard.RIGHT_SHIFT));
    }
}
