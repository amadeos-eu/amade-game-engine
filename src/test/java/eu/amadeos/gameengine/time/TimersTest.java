package eu.amadeos.gameengine.time;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimersTest {
    private static final double STEP = 1.0 / 120;

    // --8<-- [start:test-timers]
    @Test
    void timersRunOutInTheOrderOfTheirTimeEvenWithinOneStep() {
        Timers<String> fuses = new Timers<>();
        fuses.start("late", 0.5);
        fuses.start("early", 0.3);
        fuses.start("same as early, started later", 0.3);
        List<String> expired = new ArrayList<>();
        fuses.update(0.2, expired::add);
        assertEquals(List.of(), expired);
        assertEquals(0.1, fuses.left("early"), 1e-9);
        fuses.update(0.4, expired::add);     // a long step: all three run out in it
        assertEquals(List.of("early", "same as early, started later", "late"), expired);
        assertTrue(fuses.isEmpty());
    }

    @Test
    void aHandlerMayStartTimersThatCountFromTheNextStep() {
        Timers<String> timers = new Timers<>();
        timers.start("fuse", STEP);
        List<String> expired = new ArrayList<>();
        timers.update(STEP, key -> {
            expired.add(key);
            if (key.equals("fuse")) timers.start("flames", 0.5);
        });
        assertEquals(List.of("fuse"), expired);
        assertEquals(0.5, timers.left("flames"), 1e-9, "no time taken from the new timer in this step");
    }
    // --8<-- [end:test-timers]

    @Test
    void stepsThatAddUpToTheTimeEndIt() {
        Timers<String> fuses = new Timers<>();
        fuses.start("bomb", 2.5);
        List<String> expired = new ArrayList<>();
        for (int i = 0; i < 300; i++) fuses.update(STEP, expired::add);     // 300 steps are 2.5 seconds
        assertEquals(List.of("bomb"), expired);
    }

    @Test
    void startingAgainReplacesTheTime() {
        Timers<Integer> timers = new Timers<>();
        timers.start(7, 1);
        timers.update(0.75, key -> { });
        timers.start(7, 1);
        assertEquals(1, timers.left(7), 1e-9);
        assertEquals(1, timers.size());
    }

    @Test
    void cancelledTimersNeverRunOut() {
        Timers<Integer> timers = new Timers<>();
        timers.start(1, 0.1);
        timers.start(2, 0.1);
        assertTrue(timers.cancel(1));
        assertFalse(timers.cancel(1));
        List<Integer> expired = new ArrayList<>();
        timers.update(1, expired::add);
        assertEquals(List.of(2), expired);
        assertFalse(timers.running(2));
        assertEquals(0, timers.left(2));
    }

    @Test
    void keysAreInStartOrderAndClearStopsAll() {
        Timers<String> timers = new Timers<>();
        timers.start("b", 3);
        timers.start("a", 1);
        List<String> keys = new ArrayList<>();
        timers.keys().forEach(keys::add);
        assertEquals(List.of("b", "a"), keys);
        timers.clear();
        assertTrue(timers.isEmpty());
    }
}
