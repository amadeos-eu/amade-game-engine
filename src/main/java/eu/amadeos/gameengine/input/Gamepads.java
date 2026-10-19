package eu.amadeos.gameengine.input;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * The gamepads of a game, four slots, filled in the order the pads are plugged in. Java has no gamepads of
 * its own, so each operating system gets a small backend in plain Java:
 *
 * <ul>
 *   <li><b>Linux</b>: the joystick devices {@code /dev/input/js0} and on, which the kernel offers for every
 *       pad; reading them needs no library. A thread waits for their events and looks for new pads every
 *       second ({@link LinuxJoysticks}).</li>
 *   <li><b>Windows</b>: XInput, the gamepad interface of Windows, called through the Foreign Function and
 *       Memory API of Java 22 ({@code java.lang.foreign}), without JNI and without a native library of our
 *       own ({@link XInput}).</li>
 *   <li>Elsewhere, and when the backend cannot start: no pads. The game is played with the keyboard.</li>
 * </ul>
 *
 * <p>{@link #poll} once per update step, on the game loop thread, as {@link Keyboard#poll}.
 */
// --8<-- [start:gamepads]
public final class Gamepads implements AutoCloseable {
    /** As many pads as XInput knows; enough for four players. */
    public static final int SLOTS = 4;

    private static final Logger log = LoggerFactory.getLogger(Gamepads.class);

    /** Where the pads' state comes from. */
    interface Backend extends AutoCloseable {
        /** What it is, for the log and the About dialog. */
        String name();

        /** Fills the pads' state; called from {@link Gamepads#poll}, before the pads take it over. */
        void poll(List<Gamepad> pads);

        @Override
        void close();
    }

    private final List<Gamepad> pads = IntStream.range(0, SLOTS).mapToObj(Gamepad::new).toList();
    private Backend backend;

    private Gamepads() {
    }

    /** The pads of this computer, by the backend of its operating system; no pads if there is none. */
    public static Gamepads open() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.startsWith("windows")) return start(pads -> XInput.open());
        if (os.startsWith("linux")) return start(LinuxJoysticks::open);
        return none();
    }

    /** With a backend made for the four pads; without one if it cannot start, e.g. no XInput. */
    static Gamepads start(Function<List<Gamepad>, Backend> backend) {
        Gamepads gamepads = new Gamepads();
        try {
            gamepads.backend = backend.apply(gamepads.pads);
            log.info("Gamepads: {}", gamepads.backend.name());
        } catch (RuntimeException | LinkageError e) {
            log.warn("No gamepads: {}", e.toString());
        }
        return gamepads;
    }
    // --8<-- [end:gamepads]

    /** Four empty slots without a backend: for tests, which connect pads and press buttons themselves. */
    public static Gamepads none() {
        return new Gamepads();
    }

    /** Takes over the pads' state; once per update step, on the game loop thread. */
    public void poll() {
        if (backend != null) backend.poll(pads);
        for (Gamepad pad : pads) pad.poll();
    }

    /** The pad in slot {@code index}, 0 to 3, connected or not. */
    public Gamepad pad(int index) {
        return pads.get(index);
    }

    public List<Gamepad> pads() {
        return pads;
    }

    /** How many pads are connected, as of the last poll. */
    public int connected() {
        return (int) pads.stream().filter(Gamepad::connected).count();
    }

    /** The backend's name, e.g. "XInput"; "none" without one. */
    public String backend() {
        return backend == null ? "none" : backend.name();
    }

    @Override
    public void close() {
        if (backend != null) backend.close();
    }
}
