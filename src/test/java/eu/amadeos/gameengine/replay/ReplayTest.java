package eu.amadeos.gameengine.replay;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplayTest {
    enum Turn { UP, DOWN, LEFT, RIGHT }

    // --8<-- [start:test-replay]
    @Test
    void actionsComeBackAtTheirTicks() {
        Replay<Turn> replay = new Replay<>(Turn.class, 42);
        replay.record(3, Turn.UP);
        replay.record(3, Turn.LEFT);
        replay.record(10, Turn.DOWN);

        Replay<Turn>.Playback playback = replay.playback();
        assertEquals(List.of(), playback.actionsAt(0));
        assertEquals(List.of(Turn.UP, Turn.LEFT), playback.actionsAt(3));
        assertEquals(List.of(), playback.actionsAt(4));
        assertFalse(playback.finished());
        assertEquals(List.of(Turn.DOWN), playback.actionsAt(10));
        assertTrue(playback.finished());
    }

    @Test
    void aReplayIsText() {
        Replay<Turn> replay = Replay.parse(Turn.class, """
                # the best game so far
                seed 42
                3 UP
                11 LEFT
                """);
        assertEquals(42, replay.seed());
        assertEquals(List.of(new Replay.Entry<>(3, Turn.UP), new Replay.Entry<>(11, Turn.LEFT)), replay.entries());
        assertEquals(replay, Replay.parse(Turn.class, replay.toText()));
    }
    // --8<-- [end:test-replay]

    @Test
    void ticksMustNotRunBackwards() {
        Replay<Turn> replay = new Replay<>(Turn.class, 1);
        replay.record(5, Turn.UP);
        assertThrows(IllegalArgumentException.class, () -> replay.record(4, Turn.DOWN));
    }

    @Test
    void mistakesNameTheLine() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Replay.parse(Turn.class, "seed 1\n2 UP\n3 SIDEWAYS\n"));
        assertTrue(e.getMessage().startsWith("line 3"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Replay.parse(Turn.class, "2 UP\n"));
        assertThrows(IllegalArgumentException.class, () -> Replay.parse(Turn.class, ""));
    }
}
