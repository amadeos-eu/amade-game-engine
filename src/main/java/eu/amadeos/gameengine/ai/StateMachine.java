package eu.amadeos.gameengine.ai;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A finite state machine: something that is always in exactly one of a few states, such as a ghost that
 * waits, hunts, flees or flies home, and changes between them only along the ways allowed in advance.
 *
 * <p>The table of allowed changes is the point. A ghost's rules depend on a flag here and a timer there,
 * and the classic bug is a combination nobody meant: a ghost that is eaten while it is already flying home,
 * a frightened ghost still in its house. With a table, such a change is an {@link IllegalStateException}
 * at the moment it happens, naming both states, instead of a strange sight minutes later.
 *
 * <pre>
 * StateMachine&lt;Mode&gt; mode = new StateMachine&lt;&gt;(Mode.WAIT)
 *         .allow(Mode.WAIT, Mode.HUNT)
 *         .allow(Mode.HUNT, Mode.FLEE)
 *         .allow(Mode.FLEE, Mode.HUNT);
 * mode.to(Mode.HUNT);
 * </pre>
 *
 * @param <S> the states, an enum
 */
// --8<-- [start:machine]
public final class StateMachine<S extends Enum<S>> {
    private final Map<S, EnumSet<S>> allowed;
    private S state;
    private S previous;
    private double seconds;
    private BiConsumer<S, S> onChange = (from, to) -> { };

    public StateMachine(S initial) {
        this.allowed = new EnumMap<>(initial.getDeclaringClass());
        this.state = initial;
        this.previous = initial;
    }

    /** Allows the changes from {@code from} to each of {@code to}. */
    @SafeVarargs
    public final StateMachine<S> allow(S from, S... to) {
        EnumSet<S> targets = allowed.computeIfAbsent(from, s -> EnumSet.noneOf(s.getDeclaringClass()));
        for (S s : to) targets.add(s);
        return this;
    }

    /** Called after each change with the old and the new state: to start a sound, turn round, reset a timer. */
    public StateMachine<S> onChange(BiConsumer<S, S> listener) {
        this.onChange = listener;
        return this;
    }

    public boolean canChange(S to) {
        EnumSet<S> targets = allowed.get(state);
        return targets != null && targets.contains(to);
    }

    /** Changes to {@code to}, which must be allowed from the current state. */
    public void to(S to) {
        if (!canChange(to)) throw new IllegalStateException("not allowed: " + state + " → " + to);
        previous = state;
        state = to;
        seconds = 0;
        onChange.accept(previous, to);
    }

    /** Lets time pass in the current state. */
    public void update(double seconds) {
        this.seconds += seconds;
    }
    // --8<-- [end:machine]

    public S state() {
        return state;
    }

    public boolean is(S state) {
        return this.state == state;
    }

    /** The state before the last change; the initial state before the first. */
    public S previous() {
        return previous;
    }

    /** Seconds in the current state. */
    public double seconds() {
        return seconds;
    }

    /** Back to {@code state} without asking the table, e.g. a new level; no listener is called. */
    public void reset(S state) {
        this.state = state;
        this.previous = state;
        this.seconds = 0;
    }
}
