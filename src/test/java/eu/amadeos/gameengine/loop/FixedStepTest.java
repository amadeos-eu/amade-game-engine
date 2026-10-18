package eu.amadeos.gameengine.loop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FixedStepTest {
    private static final long MS = 1_000_000;

    // --8<-- [start:test-fixed-step]
    @Test
    void unevenFramesBecomeEvenSteps() {
        FixedStep step = new FixedStep(10 * MS, 8);     // steps of 10 ms
        assertEquals(1, step.advance(15 * MS));          // 15 ms: one step, 5 ms left
        assertEquals(0.5, step.alpha(), 1e-9);
        assertEquals(2, step.advance(18 * MS));          // 5 + 18 = 23 ms: two steps, 3 ms left
        assertEquals(0.3, step.alpha(), 1e-9);
        assertEquals(0, step.advance(4 * MS));           // 7 ms: not yet a step
        assertEquals(0.01, step.seconds(), 1e-12);
    }
    // --8<-- [end:test-fixed-step]

    @Test
    void aLongPauseDoesNotCatchUpForever() {
        FixedStep step = new FixedStep(10 * MS, 8);
        assertEquals(8, step.advance(5_000 * MS), "at most 8 steps, the rest is dropped");
        assertEquals(0.0, step.alpha());
        assertEquals(1, step.advance(10 * MS));
    }

    @Test
    void timeNeverRunsBackwards() {
        FixedStep step = new FixedStep(10 * MS, 8);
        assertEquals(0, step.advance(-50 * MS));
        assertEquals(0.0, step.alpha());
    }
}
