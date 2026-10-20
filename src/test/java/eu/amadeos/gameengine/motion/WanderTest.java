package eu.amadeos.gameengine.motion;

import eu.amadeos.gameengine.random.SeededRandom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WanderTest {
    // --8<-- [start:test-wander]
    @Test
    void aSheepGrazesWithinItsPastureAndWalksAndStands() {
        Wander sheep = new Wander(100, 50, 20, 8);
        SeededRandom random = new SeededRandom(3);
        int walking = 0;
        for (int i = 0; i < 120 * 60; i++) {
            if (sheep.update(1.0 / 120, random)) walking++;
            assertTrue(Math.hypot(sheep.x() - 100, sheep.y() - 50) <= 20 + 1e-9, "never beyond the fence");
        }
        assertTrue(walking > 120 * 5, "it walks some of the time");
        assertTrue(walking < 120 * 55, "and stands some of the time");
    }
    // --8<-- [end:test-wander]

    @Test
    void aSavedSheepGoesOnExactlyAsBefore() {
        Wander sheep = new Wander(0, 0, 10, 5);
        SeededRandom random = new SeededRandom(9);
        for (int i = 0; i < 500; i++) sheep.update(1.0 / 120, random);
        sheep.flee(-5, 0);
        Wander copy = new Wander(0, 0, 10, 5);
        copy.restore(sheep.state());
        SeededRandom again = SeededRandom.resume(9, random.state());
        for (int i = 0; i < 2000; i++) {
            sheep.update(1.0 / 120, random);
            copy.update(1.0 / 120, again);
        }
        assertEquals(sheep.x(), copy.x());
        assertEquals(sheep.y(), copy.y());
    }
}
