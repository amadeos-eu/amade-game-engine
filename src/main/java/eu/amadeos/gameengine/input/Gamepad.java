package eu.amadeos.gameengine.input;

/**
 * One gamepad, in one of the four slots of {@link Gamepads}: its buttons, its d-pad and its left stick, as a
 * game asks for them, "is A down?", "was Start pressed?", in the same way as {@link KeyState} answers for
 * keys. Which pad of which maker it is does not matter: the buttons are named as on the pads of the Xbox,
 * which most pads copy, and the {@link Gamepads} backend translates.
 *
 * <p>Two threads meet here. The backend {@linkplain #report reports} what the pad does, from a thread of its
 * own on Linux or in the loop's {@link Gamepads#poll} on Windows; the game asks on the game loop thread.
 * {@link #poll} in between takes over the state, so all questions of one step get the same answers, and a
 * button pressed and released between two steps is still seen as pressed once.
 *
 * <p>The stick has a <i>dead zone</i>: no stick rests exactly in the middle, and a pad lying on the table
 * would otherwise walk the player slowly to the left. Pushed far enough, the stick also counts as the d-pad,
 * so a game that only asks for {@link Button#UP} and the like works with either.
 */
// --8<-- [start:gamepad]
public final class Gamepad {
    /** The buttons, named as on an Xbox pad. UP to RIGHT are the d-pad, or the left stick pushed far enough. */
    public enum Button {
        A, B, X, Y, LB, RB, BACK, START, UP, DOWN, LEFT, RIGHT;

        final int bit = 1 << ordinal();
    }

    /** Stick movements smaller than this, as a part of the full way, count as none. */
    public static final double DEAD_ZONE = 0.25;
    /** From here on, the stick also presses the d-pad in its direction. */
    public static final double AS_DPAD = 0.5;

    private final int index;
    // Written by the backend, under the lock.
    private boolean reportedConnected;
    private String reportedName = "";
    private int reportedButtons;
    private int pressedSincePoll;
    private double reportedX;
    private double reportedY;
    // As of the last poll, for the game loop.
    private boolean connected;
    private String name = "";
    private int down;
    private int pressed;
    private double x;
    private double y;

    public Gamepad(int index) {
        this.index = index;
    }

    /** The backend's side: the pad is there, with this name. */
    public synchronized void connect(String name) {
        reportedConnected = true;
        reportedName = name;
    }

    /** The backend's side: the pad is gone; its buttons count as released. */
    public synchronized void disconnect() {
        reportedConnected = false;
        reportedButtons = 0;
        reportedX = 0;
        reportedY = 0;
    }

    /**
     * The backend's side: the buttons held now, as bits of {@link Button}, and the left stick, -1 to 1 on
     * both axes, positive to the right and down, as on the screen.
     */
    public synchronized void report(int buttons, double stickX, double stickY) {
        pressedSincePoll |= buttons & ~reportedButtons;
        reportedButtons = buttons;
        reportedX = stickX;
        reportedY = stickY;
    }

    /** Takes over the state the backend reported; once per update step, on the game loop thread. */
    public synchronized void poll() {
        connected = reportedConnected;
        name = reportedName;
        double[] stick = deadZone(reportedX, reportedY);
        x = stick[0];
        y = stick[1];
        int dpad = 0;
        if (x <= -AS_DPAD) dpad |= Button.LEFT.bit;
        if (x >= AS_DPAD) dpad |= Button.RIGHT.bit;
        if (y <= -AS_DPAD) dpad |= Button.UP.bit;
        if (y >= AS_DPAD) dpad |= Button.DOWN.bit;
        int held = reportedButtons | dpad;
        // Pressed: reported as going down since the last poll, also if released again already; or, for the
        // stick's d-pad, held now and not before.
        pressed = pressedSincePoll | (held & ~down);
        down = held;
        pressedSincePoll = 0;
    }

    /**
     * A round dead zone: inside it the stick is at rest; outside, the way from its edge to the full way is
     * stretched to 0 to 1 again, so the player still gets every speed, and the direction is kept.
     */
    static double[] deadZone(double x, double y) {
        double length = Math.hypot(x, y);
        if (length < DEAD_ZONE) return new double[]{0, 0};
        double scaled = Math.min(1, (length - DEAD_ZONE) / (1 - DEAD_ZONE));
        return new double[]{x / length * scaled, y / length * scaled};
    }
    // --8<-- [end:gamepad]

    /** The slot, 0 to 3: the first pad plugged in is 0. */
    public int index() {
        return index;
    }

    public synchronized boolean connected() {
        return connected;
    }

    /** What the system calls the pad, e.g. "Xbox Wireless Controller"; empty if it does not say. */
    public synchronized String name() {
        return name;
    }

    public synchronized boolean isDown(Button button) {
        return (down & button.bit) != 0;
    }

    /** True if the button went down between the last two polls, however short the press was. */
    public synchronized boolean wasPressed(Button button) {
        return (pressed & button.bit) != 0;
    }

    /** True if any button went down between the last two polls: to join a game. */
    public synchronized boolean anyPressed() {
        return pressed != 0;
    }

    /** The left stick, -1 (left) to 1 (right), after the dead zone. */
    public synchronized double x() {
        return x;
    }

    /** The left stick, -1 (up) to 1 (down), after the dead zone. */
    public synchronized double y() {
        return y;
    }

    /** The bits of buttons, for backends and tests: {@code Gamepad.bits(Button.A, Button.START)}. */
    public static int bits(Button... buttons) {
        int bits = 0;
        for (Button button : buttons) bits |= button.bit;
        return bits;
    }
}
