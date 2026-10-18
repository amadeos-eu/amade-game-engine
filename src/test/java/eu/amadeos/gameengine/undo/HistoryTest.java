package eu.amadeos.gameengine.undo;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoryTest {
    @Test
    void undoAndRedoWalkThroughTheStates() {
        History<String> history = new History<>();
        assertFalse(history.canUndo());
        history.record("a");                     // a → b
        history.record("b");                     // b → c
        assertEquals(Optional.of("b"), history.undo("c"));
        assertEquals(Optional.of("a"), history.undo("b"));
        assertEquals(Optional.empty(), history.undo("a"));
        assertTrue(history.canRedo());
        assertEquals(Optional.of("b"), history.redo("a"));
        assertEquals(Optional.of("c"), history.redo("b"));
        assertEquals(Optional.empty(), history.redo("c"));
    }

    @Test
    void aNewMoveForgetsTheUndoneOnes() {
        History<String> history = new History<>();
        history.record("a");
        history.undo("b");
        history.record("a");                     // a → x instead of b
        assertFalse(history.canRedo());
        assertEquals(Optional.of("a"), history.undo("x"));
    }

    @Test
    void dropsTheOldestBeyondTheLimit() {
        History<Integer> history = new History<>(2);
        history.record(1);
        history.record(2);
        history.record(3);
        assertEquals(Optional.of(3), history.undo(4));
        assertEquals(Optional.of(2), history.undo(3));
        assertFalse(history.canUndo());
        history.clear();
        assertFalse(history.canRedo());
        assertThrows(IllegalArgumentException.class, () -> new History<>(0));
    }
}
