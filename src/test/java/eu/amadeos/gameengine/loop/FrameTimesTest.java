package eu.amadeos.gameengine.loop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrameTimesTest {
    private static final long MS = 1_000_000;

    // --8<-- [start:test-frame-times]
    @Test
    void averageWorstAndFramesOfTheLastSecond() {
        FrameTimes times = new FrameTimes();
        // 60 frames in a second, each 4 ms of drawing, one of them 20 ms.
        for (int i = 0; i < 60; i++) {
            long start = i * 1000 * MS / 60;
            times.record(start, start + (i == 30 ? 20 : 4) * MS);
        }
        assertEquals(60, times.framesPerSecond());
        assertEquals(4 + 16.0 / 60, times.averageMillis(), 1e-9);
        assertEquals(20, times.maxMillis(), 1e-9);
    }
    // --8<-- [end:test-frame-times]

    @Test
    void framesOlderThanASecondAreForgotten() {
        FrameTimes times = new FrameTimes();
        times.record(0, 30 * MS);
        times.record(500 * MS, 502 * MS);
        times.record(1200 * MS, 1203 * MS);
        assertEquals(2, times.framesPerSecond());
        assertEquals(3, times.maxMillis(), 1e-9);
    }

    @Test
    void emptyIsZero() {
        FrameTimes times = new FrameTimes();
        assertEquals(0, times.framesPerSecond());
        assertEquals(0, times.averageMillis());
        assertEquals(0, times.maxMillis());
    }
}
