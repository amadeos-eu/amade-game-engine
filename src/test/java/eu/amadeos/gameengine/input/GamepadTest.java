package eu.amadeos.gameengine.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GamepadTest {
    // --8<-- [start:test-dead-zone]
    @Test
    void aStickAtRestIsInTheMiddleAndAFullPushStaysFull() {
        assertEquals(0, Gamepad.deadZone(0.2, -0.1)[0], "a worn stick that does not quite come back");
        assertEquals(1, Gamepad.deadZone(1, 0)[0], 1e-9);
        assertEquals(0.5, Gamepad.deadZone(0, 0.625)[1], 1e-9, "halfway between the dead zone and the edge");
        double[] diagonal = Gamepad.deadZone(0.6, 0.6);
        assertEquals(diagonal[0], diagonal[1], 1e-9, "the direction is kept");
    }
    // --8<-- [end:test-dead-zone]

    @Test
    void pressesAreSeenOnceAndTheStateOnlyChangesAtAPoll() {
        Gamepad pad = new Gamepad(2);
        assertFalse(pad.connected());
        pad.connect("Pad");
        pad.report(Gamepad.bits(Gamepad.Button.A), 0, 0);
        assertFalse(pad.isDown(Gamepad.Button.A), "not before the poll");
        pad.poll();
        assertTrue(pad.connected());
        assertEquals("Pad", pad.name());
        assertTrue(pad.isDown(Gamepad.Button.A));
        assertTrue(pad.wasPressed(Gamepad.Button.A));
        assertTrue(pad.anyPressed());
        pad.poll();
        assertTrue(pad.isDown(Gamepad.Button.A), "still held");
        assertFalse(pad.wasPressed(Gamepad.Button.A), "but pressed only once");
        pad.disconnect();
        pad.poll();
        assertFalse(pad.connected());
        assertFalse(pad.isDown(Gamepad.Button.A), "an unplugged pad holds nothing");
    }

    @Test
    void theStickPressesTheDpadFromHalfWay() {
        Gamepad pad = new Gamepad(0);
        pad.report(0, -0.6, 0.3);
        pad.poll();
        assertTrue(pad.isDown(Gamepad.Button.LEFT));
        assertTrue(pad.wasPressed(Gamepad.Button.LEFT));
        assertFalse(pad.isDown(Gamepad.Button.DOWN), "0.3 is not far enough");
        assertTrue(pad.x() < 0);
        pad.poll();
        assertFalse(pad.wasPressed(Gamepad.Button.LEFT), "held, not pressed again");
    }
}
