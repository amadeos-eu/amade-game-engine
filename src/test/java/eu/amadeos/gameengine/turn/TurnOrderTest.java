package eu.amadeos.gameengine.turn;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnOrderTest {
    // --8<-- [start:test-turns]
    @Test
    void playersOutOfTheGameAreSkippedAndTheLastOneWins() {
        Set<String> out = new HashSet<>();
        TurnOrder<String> turns = new TurnOrder<>(List.of("red", "blue", "green"), p -> !out.contains(p), 1);
        assertEquals("blue", turns.current());
        assertEquals("green", turns.next());
        out.add("red");                                      // destroyed by green's shot
        assertEquals("blue", turns.next(), "red is skipped");
        assertFalse(turns.decided());
        out.add("green");
        assertTrue(turns.decided());
        assertEquals("blue", turns.winner());
    }
    // --8<-- [end:test-turns]

    @Test
    void aFirstPlayerOutOfPlayPassesAtOnce() {
        TurnOrder<String> turns = new TurnOrder<>(List.of("a", "b", "c"), p -> !p.equals("a"), 0);
        assertEquals("b", turns.current());
        assertEquals(0, turns.turns());
    }

    @Test
    void nobodyLeftIsADrawn() {
        TurnOrder<String> turns = new TurnOrder<>(List.of("a", "b"), p -> false, 0);
        assertTrue(turns.decided());
        assertNull(turns.winner());
        assertEquals(List.of(), turns.inPlay());
    }

    @Test
    void countsTheTurns() {
        TurnOrder<Integer> turns = new TurnOrder<>(List.of(1, 2), p -> true, 0);
        turns.next();
        turns.next();
        assertEquals(1, turns.current());
        assertEquals(2, turns.turns());
    }
}
