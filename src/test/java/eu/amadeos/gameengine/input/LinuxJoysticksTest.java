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

    // --8<-- [start:test-is-gamepad]
    /** Bitmaps as a 64-bit kernel writes them to /sys/class/input/js0/device/capabilities/key. */
    @Test
    void onlyGamepadsAreGamepads() {
        assertTrue(LinuxJoysticks.isGamepad("7cdb000000000000 0 0 0 0", "0", 64));           // Xbox pad
        assertTrue(LinuxJoysticks.isGamepad("ffff00000000 0 0 0 0", "0", 64));               // joystick
        assertFalse(LinuxJoysticks.isGamepad("70000 0 0 0 0", "0", 64));                     // QEMU tablet
        assertFalse(LinuxJoysticks.isGamepad("0", "40", 64));                                // accelerometer
        assertFalse(LinuxJoysticks.isGamepad("400 7cdb000000000000 0 0 0 0", "0", 64));      // touch
        assertFalse(LinuxJoysticks.isGamepad("0", "0", 64));
    }
    // --8<-- [end:test-is-gamepad]

    @Test
    void bitmapsOf32BitKernels() {
        assertTrue(LinuxJoysticks.isGamepad("7cdb0000 0 0 0 0 0 0 0 0 0", "0", 32));
        assertFalse(LinuxJoysticks.isGamepad("70000 0 0 0 0 0 0 0 0", "0", 32));
    }

    /** A device that is no gamepad gets no slot: the pad behind it gets the first one. */
    @Test
    void aTabletIsSkipped() throws Exception {
        assumeTrue(System.getProperty("os.name").startsWith("Linux"), "named pipes as on Linux");
        for (String js : List.of("js0", "js1")) {
            assertEquals(0, new ProcessBuilder("mkfifo", dir.resolve(js).toString()).start().waitFor());
            Files.createDirectories(dir.resolve("sys/" + js + "/device/capabilities"));
        }
        Files.writeString(dir.resolve("sys/js0/device/name"), "QEMU QEMU USB Tablet\n");
        Files.writeString(dir.resolve("sys/js0/device/capabilities/key"), "70000 0 0 0 0\n");
        Files.writeString(dir.resolve("sys/js1/device/name"), "Test Pad\n");
        Files.writeString(dir.resolve("sys/js1/device/capabilities/key"), "7cdb000000000000 0 0 0 0\n");
        List<Gamepad> pads = IntStream.range(0, Gamepads.SLOTS).mapToObj(Gamepad::new).toList();
        LinuxJoysticks joysticks = LinuxJoysticks.open(pads, dir, dir.resolve("sys"), 20);
        try (OutputStream out = Files.newOutputStream(dir.resolve("js1"))) {
            out.write(LinuxJoysticks.event(BUTTON, 0, 1));
            out.flush();
            Gamepad pad = pads.getFirst();
            waitFor(() -> {
                pad.poll();
                return pad.isDown(Gamepad.Button.A);
            });
            assertEquals("Test Pad", pad.name());
            pads.get(1).poll();
            assertFalse(pads.get(1).connected());
        } finally {
            joysticks.close();
        }
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
