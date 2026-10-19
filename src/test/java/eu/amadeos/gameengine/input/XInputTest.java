package eu.amadeos.gameengine.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class XInputTest {
    @Test
    void xinputButtonsBecomeOurs() {
        assertEquals(Gamepad.bits(Gamepad.Button.A, Gamepad.Button.START), XInput.buttons(0x1000 | 0x0010));
        // Y is the highest bit of the WORD: read as a Java short, it is negative.
        assertEquals(Gamepad.bits(Gamepad.Button.Y, Gamepad.Button.LEFT), XInput.buttons((short) 0x8004));
        assertEquals(-1, XInput.axis(Short.MIN_VALUE));
        assertEquals(1, XInput.axis(Short.MAX_VALUE));
    }

    /** On Windows the real XInput: it loads, and asking for pads that are not there does no harm. */
    @Test
    void onWindowsXInputLoadsAndPolls() {
        assumeTrue(System.getProperty("os.name").startsWith("Windows"), "Windows only");
        try (Gamepads gamepads = Gamepads.open()) {
            assertNotEquals("none", gamepads.backend());
            gamepads.poll();
            gamepads.poll();
        }
    }
}
