package eu.amadeos.gameengine.input;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Keys mapped to the actions of a game, so that the rules ask "does the right player move up?" and never
 * "is the up arrow down?". Several keys can trigger one action; the mapping can come from settings. The
 * buttons of a {@link Gamepad} can trigger actions too, so a player's controls are the same to the game
 * whether they come from the keyboard or from a pad.
 *
 * <pre>
 * enum Action { LEFT_UP, LEFT_DOWN, RIGHT_UP, RIGHT_DOWN, PAUSE }
 * Controls&lt;Action&gt; controls = new Controls&lt;&gt;(Action.class, keyboard);
 * controls.bind(Action.RIGHT_UP, KeyEvent.VK_UP);
 * controls.bind(Action.PAUSE, KeyEvent.VK_P, KeyEvent.VK_ESCAPE);
 * if (controls.isDown(Action.RIGHT_UP)) ...
 * </pre>
 */
// --8<-- [start:controls]
public final class Controls<A extends Enum<A>> {
    private record PadButton(Gamepad pad, Gamepad.Button button) {
    }

    private final Map<A, List<Integer>> keys;
    private final Map<A, List<PadButton>> buttons;
    private final Keyboard keyboard;

    public Controls(Class<A> actions, Keyboard keyboard) {
        this.keys = new EnumMap<>(actions);
        this.buttons = new EnumMap<>(actions);
        this.keyboard = keyboard;
    }

    /** Adds keys that trigger an action. */
    public Controls<A> bind(A action, int... keyCodes) {
        List<Integer> list = keys.computeIfAbsent(action, a -> new ArrayList<>());
        for (int key : keyCodes) list.add(key);
        return this;
    }

    /** Adds buttons of a gamepad that trigger an action. */
    public Controls<A> bind(A action, Gamepad pad, Gamepad.Button... padButtons) {
        List<PadButton> list = buttons.computeIfAbsent(action, a -> new ArrayList<>());
        for (Gamepad.Button button : padButtons) list.add(new PadButton(pad, button));
        return this;
    }

    /** Replaces the keys of an action. */
    public Controls<A> rebind(A action, int... keyCodes) {
        keys.remove(action);
        return bind(action, keyCodes);
    }

    public boolean isDown(A action) {
        for (int key : keys.getOrDefault(action, List.of())) if (keyboard.isDown(key)) return true;
        for (PadButton b : buttons.getOrDefault(action, List.of())) if (b.pad().isDown(b.button())) return true;
        return false;
    }

    public boolean wasPressed(A action) {
        for (int key : keys.getOrDefault(action, List.of())) if (keyboard.wasPressed(key)) return true;
        for (PadButton b : buttons.getOrDefault(action, List.of())) if (b.pad().wasPressed(b.button())) return true;
        return false;
    }

    /** -1 while only the first action is held, +1 while only the second, else 0: e.g. up and down. */
    public int axis(A negative, A positive) {
        return (isDown(positive) ? 1 : 0) - (isDown(negative) ? 1 : 0);
    }
}
// --8<-- [end:controls]
