package eu.amadeos.gameengine.adventure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * What the hero carries, in the order it was found: the bar at the top of the screen shows it that way.
 * Items are names, as in the scripts; the game knows their pictures and texts. An item is carried once or
 * not at all.
 */
public final class Inventory {
    private final List<String> items = new ArrayList<>();

    /** Adds the item at the end, if it is not carried already. */
    public void gain(String item) {
        Objects.requireNonNull(item, "item");
        if (!items.contains(item)) items.add(item);
    }

    /** Gives the item away, uses it up or puts it down; nothing happens if it is not carried. */
    public void lose(String item) {
        items.remove(item);
    }

    public boolean has(String item) {
        return items.contains(item);
    }

    public List<String> items() {
        return Collections.unmodifiableList(items);
    }

    /** The items as one value for a saved game: names separated by spaces. */
    public String save() {
        return String.join(" ", items);
    }

    /** Carries exactly the items of a value from {@link #save()}. */
    public void restore(String saved) {
        items.clear();
        for (String item : saved.strip().split("\\s+")) if (!item.isEmpty()) gain(item);
    }
}
