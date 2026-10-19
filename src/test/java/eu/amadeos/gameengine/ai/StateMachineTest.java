package eu.amadeos.gameengine.ai;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateMachineTest {
    enum Mode { WAIT, HUNT, FLEE, HOME }

    // --8<-- [start:test-machine]
    @Test
    void onlyTheAllowedChangesHappen() {
        List<String> changes = new ArrayList<>();
        StateMachine<Mode> ghost = new StateMachine<>(Mode.WAIT)
                .allow(Mode.WAIT, Mode.HUNT)
                .allow(Mode.HUNT, Mode.FLEE)
                .allow(Mode.FLEE, Mode.HUNT, Mode.HOME)
                .allow(Mode.HOME, Mode.WAIT)
                .onChange((from, to) -> changes.add(from + " → " + to));
        ghost.to(Mode.HUNT);
        ghost.update(2.5);
        assertEquals(2.5, ghost.seconds(), "time in the state");
        ghost.to(Mode.FLEE);
        assertEquals(0, ghost.seconds(), "a new state starts at zero");
        assertEquals(Mode.HUNT, ghost.previous());
        ghost.to(Mode.HOME);
        assertFalse(ghost.canChange(Mode.FLEE));
        var error = assertThrows(IllegalStateException.class, () -> ghost.to(Mode.FLEE));
        assertEquals("not allowed: HOME → FLEE", error.getMessage(), "eaten twice: a bug, found at once");
        assertTrue(ghost.is(Mode.HOME));
        assertEquals(List.of("WAIT → HUNT", "HUNT → FLEE", "FLEE → HOME"), changes);
    }
    // --8<-- [end:test-machine]

    @Test
    void aResetAsksNoTable() {
        StateMachine<Mode> ghost = new StateMachine<>(Mode.HOME);
        ghost.update(1);
        ghost.reset(Mode.HUNT);
        assertEquals(Mode.HUNT, ghost.state());
        assertEquals(0, ghost.seconds());
    }
}
