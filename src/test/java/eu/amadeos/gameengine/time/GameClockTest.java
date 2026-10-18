package eu.amadeos.gameengine.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameClockTest {
    @Test
    void countsStartedSeconds() {
        long[] now = {5_000_000_000L};
        GameClock clock = new GameClock(() -> now[0]);
        assertEquals(0, clock.seconds());
        assertFalse(clock.running());
        clock.start();
        assertTrue(clock.running());
        assertEquals(1, clock.seconds());
        now[0] += 2_500_000_000L;
        assertEquals(3, clock.seconds());
        assertEquals(2500, clock.millis());
        clock.start();
        assertEquals(3, clock.seconds(), "a second start changes nothing");
    }

    @Test
    void stopsAndResets() {
        long[] now = {0};
        GameClock clock = new GameClock(() -> now[0]);
        clock.start();
        now[0] += 4_000_000_000L;
        clock.stop();
        now[0] += 10_000_000_000L;
        assertEquals(5, clock.seconds());
        assertFalse(clock.running());
        clock.reset();
        assertEquals(0, clock.seconds());
    }
}
