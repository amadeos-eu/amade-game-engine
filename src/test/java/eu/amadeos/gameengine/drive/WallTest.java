package eu.amadeos.gameengine.drive;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WallTest {
    private static final Wall CLIFF = Wall.cliff(2.1, 5000, 42, Color.RED, Color.ORANGE);

    // --8<-- [start:test-wall]
    @Test
    void aCliffStepsBackIntoNichesAndForwardIntoButtresses() {
        double nearest = Double.MAX_VALUE, deepest = 0;
        int flat = 0, steps = 0;
        for (long i = 0; i < 2000; i++) {
            double here = CLIFF.offsetAt(i), change = Math.abs(CLIFF.offsetAt(i + 1) - here);
            nearest = Math.min(nearest, here);
            deepest = Math.max(deepest, here);
            if (change < 0.12) flat++;
            if (change > 0.4) steps++;
        }
        assertTrue(nearest >= 2.1, "never nearer than its offset: " + nearest);
        assertTrue(deepest <= 2.1 + Wall.RELIEF + 1e-9 && deepest > 2.1 + Wall.RELIEF * 0.7, "niches: " + deepest);
        assertTrue(flat > 1700, "mostly it runs along the road: " + flat);
        assertTrue(steps > 20 && steps < 200, "and now and then it steps: " + steps);
    }

    @Test
    void theSecondStepStandsBehindTheFirstAndAboveIt() {
        for (long i = -500; i < 2000; i++) {
            assertTrue(CLIFF.backOffsetAt(i) > CLIFF.offsetAt(i) + 0.5, "behind at " + i);
            assertTrue(CLIFF.backHeightAt(i) > CLIFF.heightAt(i) * 1.1, "above at " + i);
        }
    }

    @Test
    void aCliffBetweenTwoSegmentsRisesOutOfTheGroundAndSinksAgain() {
        Wall gorge = CLIFF.between(100, 400);
        assertEquals(0, gorge.heightAt(100), 1e-9);
        assertEquals(0, gorge.heightAt(400), 1e-9);
        assertEquals(CLIFF.heightAt(250), gorge.heightAt(250), 1e-9);
        assertTrue(gorge.heightAt(105) < gorge.heightAt(100 + Wall.RISE) && gorge.heightAt(105) > 0);
        assertTrue(gorge.offsetAt(100) > gorge.offsetAt(100 + Wall.RISE) - Wall.RELIEF, "it begins further out");
        assertEquals(0, gorge.backHeightAt(100), 1e-9);
    }
    // --8<-- [end:test-wall]

    @Test
    void housesAndTunnelsStandWhereTheyAre() {
        Wall houses = Wall.houses(1.6, 3000, 3, true, Color.GRAY), tunnel = Wall.tunnel(1.35, 1500);
        for (long i = 0; i < 50; i++) {
            assertEquals(1.6, houses.offsetAt(i));
            assertEquals(1.35, tunnel.offsetAt(i));
            assertEquals(1500, tunnel.heightAt(i));
            assertEquals(1, houses.risen(i));
        }
        assertEquals(houses.heightAt(8), houses.between(0, 10).heightAt(8), "only a cliff sinks at its ends");
    }
}
