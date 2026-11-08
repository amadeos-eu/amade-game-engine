package eu.amadeos.gameengine.pinball;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The lamps under the playfield inserts, by name: off, on, blinking slowly or fast. The rules set them, the
 * drawing asks how bright each one is now ({@link #level}), which fades in and out a little like a real
 * bulb instead of jumping. A lamp show — the chase of the attract mode, a flash for a jackpot — overrides
 * the states while it runs.
 */
public final class Lamps {
    public enum State { OFF, ON, SLOW, FAST }

    private final Map<String, State> states = new HashMap<>();
    private final Map<String, Double> levels = new HashMap<>();
    private double time;
    private List<String> chase = List.of();
    private double flashUntil;

    public void set(String lamp, State state) {
        states.put(lamp, state);
    }

    public void set(String lamp, boolean on) {
        set(lamp, on ? State.ON : State.OFF);
    }

    public State state(String lamp) {
        return states.getOrDefault(lamp, State.OFF);
    }

    /** All lamps off. */
    public void clear() {
        states.clear();
    }

    /** Runs a chase through these lamps, one after another, until {@link #stopChase()}. */
    public void chase(List<String> lamps) {
        this.chase = List.copyOf(lamps);
    }

    public void stopChase() {
        chase = List.of();
    }

    /** Every lamp flashes for {@code seconds}. */
    public void flash(double seconds) {
        flashUntil = time + seconds;
    }

    // --8<-- [start:update]
    public void update(double seconds) {
        time += seconds;
        for (String lamp : states.keySet()) target(lamp, seconds);
        for (String lamp : chase) target(lamp, seconds);
    }

    private void target(String lamp, double seconds) {
        boolean on = switch (state(lamp)) {
            case OFF -> false;
            case ON -> true;
            case SLOW -> (time % 0.8) < 0.4;
            case FAST -> (time % 0.24) < 0.12;
        };
        int i = chase.indexOf(lamp);
        if (i >= 0) on = (int) (time * 12) % chase.size() == i || (int) (time * 12 + chase.size() / 2) % chase.size() == i;
        if (time < flashUntil) on = (time % 0.16) < 0.08;
        double now = levels.getOrDefault(lamp, 0.0);
        double next = on ? Math.min(1, now + seconds * 18) : Math.max(0, now - seconds * 9);
        levels.put(lamp, next);
    }
    // --8<-- [end:update]

    /** How bright the lamp is now, 0 to 1. */
    public double level(String lamp) {
        return levels.getOrDefault(lamp, 0.0);
    }
}
