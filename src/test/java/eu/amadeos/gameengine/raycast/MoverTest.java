package eu.amadeos.gameengine.raycast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoverTest {
    private final GridMap map = Maps.of(
            "#####",
            "#...#",
            "#.#.#",
            "#...#",
            "#####");

    @Test
    void aBodyStopsAtAWall() {
        Mover.Position p = Mover.slide(map, 1.5, 1.5, -2, 0, 0.25);
        assertEquals(1.25, p.x(), 0.13);
        assertEquals(1.5, p.y(), 1e-9);
    }

    @Test
    void aBodySlidesAlongAWall() {
        Mover.Position p = Mover.slide(map, 1.5, 1.5, 0.6, -0.5, 0.25);   // into the top wall, at a slant
        assertEquals(2.1, p.x(), 1e-9);
        assertTrue(p.y() >= 1.25 && p.y() < 1.4, "y = " + p.y());
    }

    @Test
    void aFastBodyCannotJumpThroughAWall() {
        Mover.Position p = Mover.slide(map, 1.5, 2.5, 3, 0, 0.25);
        assertTrue(p.x() < 2, "x = " + p.x());
    }

    @Test
    void cornersAreRound() {
        // the pillar's corner is at (2, 2): a box round the body would touch it, the circle does not
        assertTrue(Mover.overlaps(map, 1.85, 1.85, 0.25));
        assertFalse(Mover.overlaps(map, 1.8, 1.8, 0.25));
    }
}
