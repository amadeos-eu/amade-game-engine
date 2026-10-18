package eu.amadeos.gameengine.undo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;

/**
 * Undo and redo by snapshots: before each move the game records a copy of its state, undo hands the last
 * copy back. This is simpler than undoing each kind of move by hand and cannot go wrong for moves with
 * side effects (a card turned over, a score changed), as long as the copies are complete.
 *
 * <pre>
 * history.record(game.copy());       // before the move
 * game.move(...);
 * history.undo(game.copy()).ifPresent(previous -&gt; game = previous);
 * </pre>
 *
 * @param <S> the state; copies must not share anything mutable with the live state
 */
public final class History<S> {
    /** Steps kept by default; the oldest are dropped beyond. */
    public static final int DEFAULT_LIMIT = 1000;

    private final Deque<S> past = new ArrayDeque<>();
    private final Deque<S> future = new ArrayDeque<>();
    private final int limit;

    public History(int limit) {
        if (limit < 1) throw new IllegalArgumentException("Limit must be positive: " + limit);
        this.limit = limit;
    }

    public History() {
        this(DEFAULT_LIMIT);
    }

    // --8<-- [start:history]
    /** Remembers the state before a move; a new move makes the undone moves unreachable. */
    public void record(S before) {
        past.push(Objects.requireNonNull(before, "before"));
        if (past.size() > limit) past.removeLast();
        future.clear();
    }

    /**
     * Goes one move back.
     *
     * @param current the state now, kept for {@link #redo}
     * @return the state before the last move, empty if there is none
     */
    public Optional<S> undo(S current) {
        if (past.isEmpty()) return Optional.empty();
        future.push(Objects.requireNonNull(current, "current"));
        return Optional.of(past.pop());
    }

    /** Goes forward again after {@link #undo}; empty if nothing was undone. */
    public Optional<S> redo(S current) {
        if (future.isEmpty()) return Optional.empty();
        past.push(Objects.requireNonNull(current, "current"));
        return Optional.of(future.pop());
    }
    // --8<-- [end:history]

    public boolean canUndo() {
        return !past.isEmpty();
    }

    public boolean canRedo() {
        return !future.isEmpty();
    }

    /** Forgets everything, e.g. for a new game. */
    public void clear() {
        past.clear();
        future.clear();
    }
}
