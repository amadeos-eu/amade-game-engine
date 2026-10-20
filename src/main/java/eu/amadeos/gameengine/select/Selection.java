package eu.amadeos.gameengine.select;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The things the player has picked, and the groups they have kept. The rules of strategy games since the
 * nineties:
 * <ul>
 *   <li>A pick replaces the selection; with Shift held it is <b>added</b> to it, and a click with Shift on
 *   something already picked takes it out again.</li>
 *   <li><b>Ctrl and a number</b> keeps the selection as a group; the number alone picks the group again.
 *   Groups survive anything else the player picks in between.</li>
 *   <li>What is destroyed leaves the selection and every group: {@link #keepOnly}.</li>
 * </ul>
 * The order is the order of picking, so the first picked thing is the one the panel shows first. The
 * things are whatever the game picks, units for example; they are compared with {@code equals}.
 *
 * @param <T> what is picked
 */
// --8<-- [start:selection]
public final class Selection<T> {
    /** Groups 1 to 9, as on the keys; index 0 is unused. */
    public static final int GROUPS = 10;

    private final Set<T> picked = new LinkedHashSet<>();
    private final List<Set<T>> groups = new ArrayList<>();
    private int version;

    public Selection() {
        for (int i = 0; i < GROUPS; i++) groups.add(new LinkedHashSet<>());
    }

    /** Picks {@code things}; with {@code adding}, as with Shift, keeps what was picked before. */
    public void pick(Collection<? extends T> things, boolean adding) {
        if (!adding) picked.clear();
        picked.addAll(things);
        version++;
    }

    /** A click on one thing; with {@code adding} it toggles: a thing picked already is taken out. */
    public void click(T thing, boolean adding) {
        if (adding && picked.contains(thing)) picked.remove(thing);
        else pick(List.of(thing), adding);
        version++;
    }

    public void clear() {
        picked.clear();
        version++;
    }

    /** Keeps the selection as group {@code number}, 1 to 9: Ctrl and the number. */
    public void store(int number) {
        Set<T> group = groups.get(check(number));
        group.clear();
        group.addAll(picked);
    }

    /** Picks group {@code number} again; true if it has anything in it. */
    public boolean recall(int number) {
        Set<T> group = groups.get(check(number));
        if (group.isEmpty()) return false;
        picked.clear();
        picked.addAll(group);
        version++;
        return true;
    }

    /** Takes everything out of the selection and the groups that {@code alive} says is gone. */
    public void keepOnly(Predicate<? super T> alive) {
        if (picked.removeIf(alive.negate())) version++;
        for (Set<T> group : groups) group.removeIf(alive.negate());
    }
    // --8<-- [end:selection]

    private static int check(int number) {
        if (number < 1 || number >= GROUPS) throw new IllegalArgumentException("no group " + number);
        return number;
    }

    public boolean contains(T thing) {
        return picked.contains(thing);
    }

    public boolean isEmpty() {
        return picked.isEmpty();
    }

    public int size() {
        return picked.size();
    }

    /** What is picked, in the order of picking. */
    public List<T> all() {
        return List.copyOf(picked);
    }

    /** The members of a group, in the order they were picked. */
    public List<T> group(int number) {
        return Collections.unmodifiableList(new ArrayList<>(groups.get(check(number))));
    }

    /** Changes whenever the selection does: a panel that shows it redraws only then. */
    public int version() {
        return version;
    }
}
