package eu.amadeos.gameengine.adventure;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Everything the story remembers, as named numbers: whether Lena has read the plaque ({@code read-plaque}
 * is 1), how often she asked the archivist ({@code asked-lefort} is 3). A flag never set is 0. Flags are the
 * whole memory of an adventure besides the items and where everyone is, so a saved game is little more than
 * this map.
 */
public final class Flags {
    private final Map<String, Integer> values = new TreeMap<>();

    public int get(String name) {
        return values.getOrDefault(name, 0);
    }

    /** Whether the flag is set: anything but 0. */
    public boolean is(String name) {
        return get(name) != 0;
    }

    public void set(String name, int value) {
        Objects.requireNonNull(name, "name");
        if (value == 0) values.remove(name);
        else values.put(name, value);
    }

    public void set(String name) {
        set(name, 1);
    }

    public void clear(String name) {
        values.remove(name);
    }

    /** Adds to a counter, e.g. how often something was tried. */
    public void add(String name, int amount) {
        set(name, get(name) + amount);
    }

    /** All flags that are set, sorted by name, for a saved game. */
    public Map<String, Integer> all() {
        return Collections.unmodifiableMap(values);
    }

    /** Forgets everything, then takes the flags of a saved game. */
    public void restore(Map<String, Integer> saved) {
        values.clear();
        saved.forEach(this::set);
    }
}
