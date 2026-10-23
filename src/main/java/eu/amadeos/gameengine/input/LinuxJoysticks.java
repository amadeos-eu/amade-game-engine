package eu.amadeos.gameengine.input;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Gamepads on Linux, read from the kernel's joystick devices {@code /dev/input/js0}, {@code js1} and on.
 * Each is a file that delivers eight bytes per event: a time, a value, a type (button or axis) and the
 * number of the button or axis. Reading them needs nothing but a file stream; the desktop gives the user at
 * the screen the right to read them.
 *
 * <p>A read waits until the pad does something, so each pad gets a thread of its own, which reports to its
 * {@link Gamepad}. Another thread looks for new pads every second. When a pad is unplugged, its read fails,
 * and its slot is free for the next pad.
 *
 * <p>The numbers of buttons and axes are the driver's. Those of the Xbox pads ({@code xpad}) are the most
 * common, and the table below follows them: buttons 0 to 7 are A, B, X, Y, LB, RB, Back and Start, axes 0
 * and 1 the left stick, axes 6 and 7 the d-pad. Simple pads deliver their d-pad as axes 0 and 1, which the
 * stick's d-pad of {@link Gamepad} turns into directions too.
 *
 * <p>Not every joystick device is a gamepad. The kernel also makes one for laptop accelerometers, for the
 * absolute mouse of a virtual machine (the tablet of QEMU and VirtualBox) and for some keyboards and mice
 * with extra keys. Such a device would take a slot and, worse, "press" buttons when the mouse is clicked or
 * the laptop tilted. So before a device gets a slot, {@link #isGamepad} looks at what its driver says it can
 * do, and a device that is no gamepad is skipped.
 */
final class LinuxJoysticks implements Gamepads.Backend {
    static final Path DEVICES = Path.of("/dev/input");
    static final Path SYS = Path.of("/sys/class/input");
    private static final Pattern JOYSTICK = Pattern.compile("js(\\d+)");
    private static final Logger log = LoggerFactory.getLogger(LinuxJoysticks.class);

    private final List<Gamepad> pads;
    private final Path devices;
    private final Path sys;
    private final Map<Path, Gamepad> open = new HashMap<>();
    private final Set<Path> refused = new HashSet<>();
    private final Thread scanner;
    private volatile boolean running = true;

    private LinuxJoysticks(List<Gamepad> pads, Path devices, Path sys, long scanMillis) {
        this.pads = pads;
        this.devices = devices;
        this.sys = sys;
        scanner = Thread.ofPlatform().daemon().name("gamepads").start(() -> {
            while (running) {
                scan();
                try {
                    Thread.sleep(scanMillis);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
    }

    static LinuxJoysticks open(List<Gamepad> pads) {
        return open(pads, DEVICES, SYS, 1000);
    }

    /** With other directories, for tests: a named pipe in a temporary folder makes a pad. */
    static LinuxJoysticks open(List<Gamepad> pads, Path devices, Path sys, long scanMillis) {
        return new LinuxJoysticks(pads, devices, sys, scanMillis);
    }

    @Override
    public String name() {
        return "Linux joystick devices";
    }

    /** Nothing to do: the pads' threads report as events come. */
    @Override
    public void poll(List<Gamepad> pads) {
    }

    @Override
    public void close() {
        running = false;
        scanner.interrupt();
    }

    /** New joystick devices get a free slot and a thread that reads them, the lowest number first. */
    private void scan() {
        List<Path> found = new ArrayList<>();
        try (Stream<Path> files = Files.list(devices)) {
            files.filter(p -> JOYSTICK.matcher(p.getFileName().toString()).matches()).forEach(found::add);
        } catch (IOException e) {
            return;             // no /dev/input: no pads
        }
        found.sort(Comparator.comparingInt(p -> Integer.parseInt(p.getFileName().toString().substring(2))));
        synchronized (this) {
            for (Path device : found) {
                if (open.containsKey(device) || refused.contains(device)) continue;
                if (!isGamepad(device)) {
                    log.info("Not a gamepad, skipped: {} ({})", name(device), device);
                    refused.add(device);
                    continue;
                }
                Gamepad pad = pads.stream().filter(p -> !open.containsValue(p)).findFirst().orElse(null);
                if (pad == null) return;
                open.put(device, pad);
                Thread.ofPlatform().daemon().name("gamepad " + device.getFileName()).start(() -> read(device, pad));
            }
        }
    }

    // --8<-- [start:read]
    /** Reads the events of one pad until it is unplugged. */
    private void read(Path device, Gamepad pad) {
        State state = new State();
        byte[] event = new byte[State.EVENT_SIZE];
        try (InputStream in = Files.newInputStream(device)) {
            pad.connect(name(device));
            log.info("Gamepad {}: {} ({})", pad.index() + 1, name(device), device);
            while (running && in.readNBytes(event, 0, event.length) == event.length) {
                state.apply(event);
                pad.report(state.buttons(), state.x(), state.y());
            }
        } catch (java.nio.file.AccessDeniedException e) {
            log.warn("Gamepad {} cannot be read: no permission", device);
            synchronized (this) {
                refused.add(device);
            }
        } catch (IOException e) {
            // Unplugged: the read fails.
        } finally {
            pad.disconnect();
            synchronized (this) {
                open.remove(device);
            }
        }
    }
    // --8<-- [end:read]

    /** The name the driver gives the pad, from {@code /sys/class/input/js0/device/name}. */
    private String name(Path device) {
        try {
            return Files.readString(sys.resolve(device.getFileName().toString()).resolve("device/name")).strip();
        } catch (IOException e) {
            return "Gamepad";
        }
    }

    /** Buttons from {@code BTN_JOYSTICK} (0x120) to {@code BTN_THUMBR} (0x13e): joysticks and gamepads. */
    static final int FIRST_PAD_BUTTON = 0x120;
    static final int LAST_PAD_BUTTON = 0x13e;
    /** {@code BTN_LEFT}, the left mouse button: a mouse, or the tablet of a virtual machine. */
    static final int MOUSE_BUTTON = 0x110;
    /** {@code BTN_TOUCH}: a touchpad, a touch screen or a graphics tablet. */
    static final int TOUCH = 0x14a;
    /** {@code INPUT_PROP_ACCELEROMETER}: the device measures how the computer is held. */
    static final int ACCELEROMETER = 0x06;
    /** The kernel writes its bitmaps as {@code long}s: 64 bits on a 64-bit system. */
    private static final int WORD_BITS = System.getProperty("os.arch", "").contains("64") ? 64 : 32;

    /**
     * Whether the device behind {@code js0} is a gamepad, from the bitmaps of its keys and properties in
     * {@code /sys/class/input/js0/device/capabilities/key} and {@code .../device/properties}. If they cannot
     * be read, it counts as one.
     */
    private boolean isGamepad(Path device) {
        Path input = sys.resolve(device.getFileName().toString()).resolve("device");
        try {
            String keys = Files.readString(input.resolve("capabilities/key"));
            Path props = input.resolve("properties");
            return isGamepad(keys, Files.exists(props) ? Files.readString(props) : "0", WORD_BITS);
        } catch (IOException | NumberFormatException e) {
            return true;
        }
    }

    // --8<-- [start:is-gamepad]
    /** A gamepad has pad buttons, neither mouse buttons nor touch, and is no accelerometer. */
    static boolean isGamepad(String keys, String properties, int wordBits) {
        if (bit(properties, ACCELEROMETER, wordBits)) return false;
        if (bit(keys, MOUSE_BUTTON, wordBits) || bit(keys, TOUCH, wordBits)) return false;
        for (int button = FIRST_PAD_BUTTON; button <= LAST_PAD_BUTTON; button++) {
            if (bit(keys, button, wordBits)) return true;
        }
        return false;
    }

    /**
     * One bit of a bitmap as the kernel writes it to sysfs: words in hex, separated by spaces, the highest
     * first, and without the zero words in front.
     */
    static boolean bit(String bitmap, int bit, int wordBits) {
        String[] words = bitmap.strip().split("\\s+");
        int word = words.length - 1 - bit / wordBits;
        if (word < 0) return false;
        return (Long.parseUnsignedLong(words[word], 16) >>> (bit % wordBits) & 1) == 1;
    }
    // --8<-- [end:is-gamepad]

    /**
     * The state of one pad, from its events: which buttons are held, where the axes are. No thread and no
     * file, so tests can feed it bytes.
     */
    // --8<-- [start:state]
    static final class State {
        /** {@code struct js_event}: u32 time in milliseconds, s16 value, u8 type, u8 number. */
        static final int EVENT_SIZE = 8;
        static final int BUTTON = 0x01;
        static final int AXIS = 0x02;
        /** Added to the type of the events that report the state at the moment the device is opened. */
        static final int INIT = 0x80;
        /** The xpad driver's buttons; with the driver's option {@code dpad_to_buttons}, 11 to 14 are the d-pad. */
        private static final Gamepad.Button[] BUTTONS = {
                Gamepad.Button.A, Gamepad.Button.B, Gamepad.Button.X, Gamepad.Button.Y,
                Gamepad.Button.LB, Gamepad.Button.RB, Gamepad.Button.BACK, Gamepad.Button.START,
                null, null, null,                // Guide and the two stick buttons
                Gamepad.Button.LEFT, Gamepad.Button.RIGHT, Gamepad.Button.UP, Gamepad.Button.DOWN};
        private static final int STICK_X = 0;
        private static final int STICK_Y = 1;
        private static final int DPAD_X = 6;
        private static final int DPAD_Y = 7;

        private final int[] axes = new int[8];
        private int buttons;

        void apply(byte[] event) {
            ByteBuffer data = ByteBuffer.wrap(event).order(ByteOrder.LITTLE_ENDIAN);
            data.getInt();                           // the time: the game has its own
            int value = data.getShort();
            int type = (data.get() & 0xFF) & ~INIT;  // the opening state counts like any other event
            int number = data.get() & 0xFF;
            if (type == BUTTON && number < BUTTONS.length && BUTTONS[number] != null) {
                int bit = Gamepad.bits(BUTTONS[number]);
                buttons = value != 0 ? buttons | bit : buttons & ~bit;
            } else if (type == AXIS && number < axes.length) {
                axes[number] = value;
            }
        }

        /** The buttons held, with the d-pad's axes as buttons. */
        int buttons() {
            int held = buttons;
            if (axes[DPAD_X] < 0) held |= Gamepad.bits(Gamepad.Button.LEFT);
            if (axes[DPAD_X] > 0) held |= Gamepad.bits(Gamepad.Button.RIGHT);
            if (axes[DPAD_Y] < 0) held |= Gamepad.bits(Gamepad.Button.UP);
            if (axes[DPAD_Y] > 0) held |= Gamepad.bits(Gamepad.Button.DOWN);
            return held;
        }

        /** The driver gives axes from -32767 to 32767, down positive as on the screen. */
        double x() {
            return Math.clamp(axes[STICK_X] / 32767.0, -1, 1);
        }

        double y() {
            return Math.clamp(axes[STICK_Y] / 32767.0, -1, 1);
        }
    }
    // --8<-- [end:state]

    /** The eight bytes of an event, for tests. */
    static byte[] event(int type, int number, int value) {
        return ByteBuffer.allocate(State.EVENT_SIZE).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(0).putShort((short) value).put((byte) type).put((byte) number).array();
    }
}
