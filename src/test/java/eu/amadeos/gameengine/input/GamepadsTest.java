package eu.amadeos.gameengine.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GamepadsTest {
    @Test
    void fourSlotsWhateverTheComputerHas() {
        try (Gamepads gamepads = Gamepads.open()) {
            gamepads.poll();
            assertEquals(Gamepads.SLOTS, gamepads.pads().size());
            assertTrue(gamepads.connected() >= 0);
        }
    }

    @Test
    void aBackendThatCannotStartMeansNoPads() {
        Gamepads gamepads = Gamepads.start(pads -> {
            throw new UnsatisfiedLinkError("no such library");
        });
        assertEquals("none", gamepads.backend());
        gamepads.poll();
        assertEquals(0, gamepads.connected());
    }

    @Test
    void testsConnectPadsThemselves() {
        Gamepads gamepads = Gamepads.none();
        Gamepad pad = gamepads.pad(1);
        assertSame(pad, gamepads.pads().get(1));
        pad.connect("Test");
        gamepads.poll();
        assertEquals(1, gamepads.connected());
    }
}
