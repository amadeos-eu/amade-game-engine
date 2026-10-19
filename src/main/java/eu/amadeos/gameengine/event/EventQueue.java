package eu.amadeos.gameengine.event;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Events that cause events: a bomb explodes, its flames reach a second bomb, which explodes too, and its
 * flames reach a player. Each event is handled by one method of the rules, and the handler may {@link #post}
 * further events; {@link #process} handles them all, in the order they were posted, until none is left.
 *
 * <p>The order is first in, first out, a queue, as in breadth-first search: everything the first event
 * caused is handled before anything those events caused. A chain reaction thus runs in waves, the bombs hit
 * by the first blast before the bombs hit by theirs, and the order never depends on how deep a handler
 * happens to call itself, as it would if handlers called each other directly.
 *
 * <p>Every handled event is kept in {@link #happened()} until {@link #clear()}, so the user interface can
 * show and play what happened in a step, as the earlier games did with their lists of events.
 *
 * <p>A chain without end, two events that keep posting each other, would hang the game. After
 * {@code limit} events in one {@link #process} the queue gives up with an exception that names the last
 * event, which a test finds at once.
 *
 * @param <E> the events, usually a sealed interface of records
 */
// --8<-- [start:queue]
public final class EventQueue<E> {
    private final ArrayDeque<E> pending = new ArrayDeque<>();
    private final List<E> happened = new ArrayList<>();
    private final int limit;

    /** @param limit most events one {@link #process} may handle */
    public EventQueue(int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit must be positive: " + limit);
        this.limit = limit;
    }

    public EventQueue() {
        this(10_000);
    }

    /** Adds an event to the end of the queue; also from within a handler. */
    public void post(E event) {
        pending.addLast(event);
    }

    /** Hands every waiting event to {@code handler}, also those it posts, oldest first, until none is left. */
    public void process(Consumer<? super E> handler) {
        int handled = 0;
        while (!pending.isEmpty()) {
            E event = pending.removeFirst();
            if (++handled > limit) {
                pending.clear();
                throw new IllegalStateException("more than " + limit + " events in one go, the last " + event);
            }
            happened.add(event);
            handler.accept(event);
        }
    }
    // --8<-- [end:queue]

    /** The events handled since the last {@link #clear()}, in the order they were handled. Read only. */
    public List<E> happened() {
        return Collections.unmodifiableList(happened);
    }

    /** Events posted but not yet handled. */
    public int waiting() {
        return pending.size();
    }

    /** Forgets what happened, and drops events still waiting: at the start of a step. */
    public void clear() {
        happened.clear();
        pending.clear();
    }
}
