package eu.amadeos.gameengine.input;

import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlsTest {
    enum Action { UP, DOWN, PAUSE }

    @Test
    void actionsFollowTheirKeys() {
        Keyboard keyboard = new Keyboard();
        Controls<Action> controls = new Controls<>(Action.class, keyboard)
                .bind(Action.UP, KeyEvent.VK_UP, KeyEvent.VK_W)
                .bind(Action.DOWN, KeyEvent.VK_DOWN)
                .bind(Action.PAUSE, KeyEvent.VK_P);
        keyboard.pressed(KeyEvent.VK_W, 0);
        keyboard.poll();
        assertTrue(controls.isDown(Action.UP));
        assertEquals(-1, controls.axis(Action.UP, Action.DOWN));
        keyboard.pressed(KeyEvent.VK_DOWN, 0);
        assertEquals(0, controls.axis(Action.UP, Action.DOWN), "both held: no movement");
        assertFalse(controls.wasPressed(Action.PAUSE));
        controls.rebind(Action.UP, KeyEvent.VK_I);
        assertFalse(controls.isDown(Action.UP), "W no longer moves up");
    }

    // --8<-- [start:test-pad]
    @Test
    void aGamepadTriggersTheSameActions() {
        Keyboard keyboard = new Keyboard();
        Gamepad pad = new Gamepad(0);
        Controls<Action> controls = new Controls<>(Action.class, keyboard)
                .bind(Action.UP, KeyEvent.VK_UP)
                .bind(Action.UP, pad, Gamepad.Button.UP)
                .bind(Action.PAUSE, pad, Gamepad.Button.START);
        pad.connect("test pad");
        pad.report(0, 0, -0.9);           // the stick pushed up
        pad.poll();
        keyboard.poll();
        assertTrue(controls.isDown(Action.UP), "the stick counts as the d-pad");
        pad.report(Gamepad.bits(Gamepad.Button.START), 0, 0);
        pad.report(0, 0, 0);              // pressed and released between two polls
        pad.poll();
        assertTrue(controls.wasPressed(Action.PAUSE));
        assertFalse(controls.isDown(Action.UP));
    }
    // --8<-- [end:test-pad]
}
