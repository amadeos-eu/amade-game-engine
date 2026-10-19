package eu.amadeos.gameengine.loop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TickerTest {
    private static final double STEP = 1.0 / 120;

    // --8<-- [start:test-ticker]
    @Test
    void ticksAreSlowerThanSteps() {
        Ticker ticker = new Ticker(0.1);    // ten ticks a second
        int ticks = 0;
        for (int i = 0; i < 120; i++) ticks += ticker.advance(STEP);    // one second of update steps
        assertEquals(10, ticks);
        assertEquals(10, ticker.count());
    }

    @Test
    void progressRunsFromOneTickToTheNext() {
        Ticker ticker = new Ticker(0.1);
        assertEquals(0, ticker.advance(0.025));
        assertEquals(0.25, ticker.progress(0), 1e-9);
        assertEquals(0.5, ticker.progress(0.025), 1e-9, "a frame drawn between two steps looks ahead");
        assertEquals(1, ticker.advance(0.1));
        assertEquals(0.25, ticker.progress(0), 1e-9, "the rest of the time carries over");
    }
    // --8<-- [end:test-ticker]

    @Test
    void aFasterPaceKeepsTheTimeSinceTheLastTick() {
        Ticker ticker = new Ticker(0.2);
        ticker.advance(0.15);
        ticker.setInterval(0.1);
        assertEquals(0.1, ticker.interval());
        assertEquals(1, ticker.advance(0), "0.15 s have passed, more than the new interval");
        assertEquals(0.5, ticker.progress(0), 1e-9);
    }

    @Test
    void aLongStepCanHoldSeveralTicks() {
        Ticker ticker = new Ticker(0.05);
        assertEquals(3, ticker.advance(0.16));
        assertEquals(1.0, ticker.progress(1), "progress never passes the next tick");
    }

    @Test
    void resetStartsAgain() {
        Ticker ticker = new Ticker(0.1);
        ticker.advance(0.35);
        ticker.reset();
        assertEquals(0, ticker.count());
        assertEquals(0.0, ticker.progress(0));
    }

    @Test
    void theIntervalMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new Ticker(0));
        assertThrows(IllegalArgumentException.class, () -> new Ticker(Double.NaN));
    }
}
