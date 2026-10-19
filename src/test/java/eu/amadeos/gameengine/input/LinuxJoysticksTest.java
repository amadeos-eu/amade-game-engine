package eu.amadeos.gameengine.input;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class LinuxJoysticksTest {
    private static final int BUTTON = LinuxJoysticks.State.BUTTON;
    private static final int AXIS = LinuxJoysticks.State.AXIS;
    private static final int INIT = LinuxJoysticks.State.INIT;

    @TempDir
    Path dir;

    // --8<-- [start:test-events]
    @Test
    void eventsBecomeButtonsAndAxes() {
        LinuxJoysticks.State state = new LinuxJoysticks.State();
        state.apply(LinuxJoysticks.event(BUTTON | INIT, 7, 0));      // Start, released, when opened
        state.apply(LinuxJoysticks.event(BUTTON, 0, 1));             // A down
        state.apply(LinuxJoysticks.event(AXIS, 1, -32767));          // left stick fully up
        state.apply(LinuxJoysticks.event(AXIS, 6, 32767));           // d-pad right
        assertEquals(Gamepad.bits(Gamepad.Button.A, Gamepad.Button.RIGHT), state.buttons());
        assertEquals(-1, state.y(), 1e-9);
        state.apply(LinuxJoysticks.event(BUTTON, 0, 0));
        state.apply(LinuxJoysticks.event(AXIS, 6, 0));
        assertEquals(0, state.buttons());
    }
    // --8<-- [end:test-events]

    @Test
    void unknownButtonsAndAxesAreIgnored() {
        LinuxJoysticks.State state = new LinuxJoysticks.State();
        state.apply(LinuxJoysticks.event(BUTTON, 8, 1));     // Guide
        state.apply(LinuxJoysticks.event(BUTTON, 40, 1));
        state.apply(LinuxJoysticks.event(AXIS, 12, 1000));
        assertEquals(0, state.buttons());
    }

    /** A named pipe in place of /dev/input/js0: the whole way from the bytes to the pad's buttons. */
    @Test
    void aPadIsFoundReadAndLetGoWhenUnplugged() throws Exception {
        assumeTrue(System.getProperty("os.name").startsWith("Linux"), "named pipes as on Linux");
        Path device = dir.resolve("js0");
        assertEquals(0, new ProcessBuilder("mkfifo", device.toString()).start().waitFor());
        Files.createDirectories(dir.resolve("sys/js0/device"));
        Files.writeString(dir.resolve("sys/js0/device/name"), "Test Pad\n");
        List<Gamepad> pads = IntStream.range(0, Gamepads.SLOTS).mapToObj(Gamepad::new).toList();
        LinuxJoysticks joysticks = LinuxJoysticks.open(pads, dir, dir.resolve("sys"), 20);
        try {
            Gamepad pad = pads.getFirst();
            try (OutputStream out = Files.newOutputStream(device)) {
                out.write(LinuxJoysticks.event(BUTTON | INIT, 0, 0));
                out.write(LinuxJoysticks.event(BUTTON, 0, 1));
                out.flush();
                waitFor(() -> {
                    pad.poll();
                    return pad.isDown(Gamepad.Button.A);
                });
                assertTrue(pad.connected());
                assertEquals("Test Pad", pad.name());
                assertFalse(pads.get(1).connected());
            }
            // The writer is gone, as when the pad is pulled out: the slot is free again.
            waitFor(() -> {
                pad.poll();
                return !pad.connected();
            });
            assertFalse(pad.isDown(Gamepad.Button.A));
        } finally {
            joysticks.close();
        }
    }

    private static void waitFor(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) throw new AssertionError("timed out");
            Thread.sleep(10);
        }
    }
}
