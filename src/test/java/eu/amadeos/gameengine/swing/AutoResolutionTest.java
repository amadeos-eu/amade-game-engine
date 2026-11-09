package eu.amadeos.gameengine.swing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoResolutionTest {
    /** Feeds frames for {@code seconds}, each taking what {@code cost} says for the divisor of the moment. */
    private static void run(AutoResolution auto, double seconds, java.util.function.IntToDoubleFunction cost) {
        for (double t = 0; t < seconds * 1000; ) {
            double ms = cost.applyAsDouble(auto.divisor());
            auto.frame(ms);
            t += Math.max(ms, 1000.0 / 60);
        }
    }

    @Test
    void staysSharpWhileTheFramesAreInTime() {
        AutoResolution auto = new AutoResolution();
        auto.limit(3);
        run(auto, 30, d -> 12);
        assertEquals(1, auto.divisor());
    }

    @Test
    void goesCoarserStepByStepUntilTheFramesAreInTime() {
        AutoResolution auto = new AutoResolution();
        auto.limit(3);
        run(auto, 1.5, d -> 68.0 / (d * d) + 8);
        assertEquals(2, auto.divisor(), "68 + 8 ms is late, 17 + 8 ms is too");
        run(auto, 30, d -> 68.0 / (d * d) + 8);
        assertEquals(3, auto.divisor());
    }

    @Test
    void neverGoesBelowTheLimit() {
        AutoResolution auto = new AutoResolution();
        auto.limit(2);
        run(auto, 20, d -> 90);
        assertEquals(2, auto.divisor());
        auto.limit(1);
        assertEquals(1, auto.divisor());
    }

    @Test
    void oneSlowFrameChangesNothing() {
        AutoResolution auto = new AutoResolution();
        auto.limit(3);
        int[] n = {0};
        run(auto, 20, d -> n[0]++ % 25 == 0 ? 120 : 9);
        assertEquals(1, auto.divisor());
    }

    @Test
    void goesSharperAgainWhenTheSceneGetsLighter() {
        AutoResolution auto = new AutoResolution();
        auto.limit(3);
        double[] scene = {40};
        run(auto, 5, d -> scene[0] / (d * d) + 2);
        assertEquals(2, auto.divisor());
        scene[0] = 6;
        run(auto, 10, d -> scene[0] / (d * d) + 2);
        assertEquals(1, auto.divisor(), "8 ms at full size is in time");
    }

    @Test
    void doesNotSwingBackAndForth() {
        AutoResolution auto = new AutoResolution();
        auto.limit(2);
        // 26 ms at full size is late, 8 ms at half size looks easy: a step sharper is not worth trying,
        // because the step coarser showed what it costs.
        int[] steps = {0};
        int[] last = {1};
        run(auto, 300, d -> {
            if (d != last[0]) {
                steps[0]++;
                last[0] = d;
            }
            return d == 1 ? 26 : 8;
        });
        assertEquals(2, auto.divisor());
        assertEquals(1, steps[0]);
    }

    @Test
    void aStepTakenBackTeachesWhatItCosts() {
        AutoResolution auto = new AutoResolution();
        auto.limit(2);
        run(auto, 3, d -> d == 1 ? 30 : 4);
        assertEquals(2, auto.divisor());
        // The scene changes while the picture is coarse, so what was learned no longer holds: the sharper
        // picture is tried, is late, is taken back, and is not tried again while nothing changes.
        int[] steps = {0};
        int[] last = {2};
        run(auto, 360, d -> {
            if (d != last[0]) {
                steps[0]++;
                last[0] = d;
            }
            return d == 1 ? 24 : 1.5;
        });
        assertEquals(2, auto.divisor());
        assertEquals(2, steps[0], "once sharper and back");
    }
}
