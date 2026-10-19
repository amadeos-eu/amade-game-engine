package eu.amadeos.gameengine.input;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * Gamepads on Windows, through XInput, the interface of Windows for pads of the Xbox kind, which nearly
 * every pad sold today speaks. XInput is a C library ({@code xinput1_4.dll}); Java calls it with the
 * Foreign Function and Memory API, final since Java 22: find the function in the library, describe its
 * parameters, and call it like a Java method. No JNI, no C compiler, no library of our own.
 *
 * <p>The function is {@code DWORD XInputGetState(DWORD dwUserIndex, XINPUT_STATE *pState)}: it fills a
 * structure of 16 bytes with the pad's buttons and sticks, and returns 0, or an error if no pad is in that
 * slot. XInput is polled, not told: {@link #poll} asks for all four slots in each update step. Asking for an
 * empty slot is slow, though, up to a millisecond, so an empty slot is asked again only once a second.
 *
 * <p>Calling C is a <i>restricted</i> operation in Java: it can crash the JVM if the description is wrong.
 * Java warns once unless the program is started with {@code --enable-native-access=ALL-UNNAMED}, or its jar
 * says {@code Enable-Native-Access: ALL-UNNAMED} in the manifest.
 */
// --8<-- [start:xinput]
final class XInput implements Gamepads.Backend {
    /** {@code XINPUT_STATE}: DWORD packet number, then {@code XINPUT_GAMEPAD}. */
    static final int STATE_SIZE = 16;
    static final int BUTTONS = 4;         // WORD wButtons
    static final int THUMB_LX = 8;        // SHORT sThumbLX, then sThumbLY
    static final int THUMB_LY = 10;
    private static final int ERROR_SUCCESS = 0;
    /** An empty slot is asked again after this long. */
    static final long RECHECK_NANOS = 1_000_000_000L;

    private final String library;
    private final MethodHandle getState;
    private final MemorySegment state = Arena.global().allocate(STATE_SIZE);
    private final boolean[] present = new boolean[Gamepads.SLOTS];
    private final long[] nextCheck = new long[Gamepads.SLOTS];
    private final LongSupplier clock = System::nanoTime;

    private XInput(String library, MethodHandle getState) {
        this.library = library;
        this.getState = getState;
    }

    /** XInput 1.4 comes with Windows 8 and later, 9.1.0 with older versions. */
    static XInput open() {
        for (String library : new String[]{"xinput1_4.dll", "xinput9_1_0.dll", "xinput1_3.dll"}) {
            try {
                SymbolLookup lookup = SymbolLookup.libraryLookup(library, Arena.global());
                MemorySegment function = lookup.find("XInputGetState").orElseThrow();
                MethodHandle getState = Linker.nativeLinker().downcallHandle(function,
                        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                return new XInput(library, getState);
            } catch (IllegalArgumentException e) {
                // This version is not installed; try the next.
            }
        }
        throw new IllegalStateException("no XInput library");
    }

    @Override
    public void poll(List<Gamepad> pads) {
        long now = clock.getAsLong();
        for (int i = 0; i < Gamepads.SLOTS; i++) {
            if (!present[i] && now < nextCheck[i]) continue;
            Gamepad pad = pads.get(i);
            if (call(i) == ERROR_SUCCESS) {
                if (!present[i]) pad.connect("XInput " + (i + 1));
                present[i] = true;
                pad.report(buttons(state.get(ValueLayout.JAVA_SHORT, BUTTONS)),
                        axis(state.get(ValueLayout.JAVA_SHORT, THUMB_LX)),
                        -axis(state.get(ValueLayout.JAVA_SHORT, THUMB_LY)));   // XInput counts up as positive
            } else {
                if (present[i]) pad.disconnect();
                present[i] = false;
                nextCheck[i] = now + RECHECK_NANOS;
            }
        }
    }

    private int call(int slot) {
        try {
            return (int) getState.invokeExact(slot, state);
        } catch (Throwable e) {
            throw new IllegalStateException("XInputGetState failed", e);
        }
    }
    // --8<-- [end:xinput]

    // --8<-- [start:buttons]
    /** XInput's button bits, in the order of {@link Gamepad.Button}. */
    private static final int[] BITS = {
            0x1000, 0x2000, 0x4000, 0x8000,      // A, B, X, Y
            0x0100, 0x0200, 0x0020, 0x0010,      // LB, RB, Back, Start
            0x0001, 0x0002, 0x0004, 0x0008};     // d-pad up, down, left, right

    /** XInput's {@code wButtons} as bits of {@link Gamepad.Button}. */
    static int buttons(int xinput) {
        int bits = 0;
        for (Gamepad.Button button : Gamepad.Button.values()) {
            if ((xinput & BITS[button.ordinal()]) != 0) bits |= Gamepad.bits(button);
        }
        return bits;
    }

    /** A stick axis, -32768 to 32767, as -1 to 1. */
    static double axis(short value) {
        return Math.max(-1, value / 32767.0);
    }
    // --8<-- [end:buttons]

    @Override
    public String name() {
        return "XInput (" + library + ")";
    }

    @Override
    public void close() {
    }
}
