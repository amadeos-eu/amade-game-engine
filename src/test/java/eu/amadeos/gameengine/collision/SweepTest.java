package eu.amadeos.gameengine.collision;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SweepTest {
    private static final Box BRICK = new Box(100, 100, 40, 16);

    // --8<-- [start:test-tunnel]
    @Test
    void aBallFasterThanTheBrickIsThickStillHitsIt() {
        // An 8 × 8 ball below the brick, moving 60 units up in one step: it would end above the brick.
        Box ball = new Box(116, 130, 8, 8);
        assertFalse(ball.moved(0, -60).overlaps(BRICK), "the end position alone misses the brick");
        Sweep.Hit hit = Sweep.sweep(ball, 0, -60, BRICK);
        assertNotNull(hit);
        assertEquals(14.0 / 60, hit.time(), 1e-9);  // after 14 units it touches the bottom face
        assertEquals(0, hit.normalX());
        assertEquals(1, hit.normalY());              // bounce down
    }
    // --8<-- [end:test-tunnel]

    @Test
    void theFaceEnteredLastIsTheOneHit() {
        // Coming from the left and slightly from above, it reaches the left face.
        Sweep.Hit hit = Sweep.sweep(new Box(80, 104, 8, 8), 20, 2, BRICK);
        assertNotNull(hit);
        assertEquals(-1, hit.normalX());
        assertEquals(0, hit.normalY());
        assertEquals(12.0 / 20, hit.time(), 1e-9);
        // Coming from above at the corner, it reaches the top face.
        hit = Sweep.sweep(new Box(96, 80, 8, 8), 4, 20, BRICK);
        assertNotNull(hit);
        assertEquals(0, hit.normalX());
        assertEquals(-1, hit.normalY());
    }

    @Test
    void noHitWhenPassingByOrTooShortOrMovingAway() {
        assertNull(Sweep.sweep(new Box(60, 130, 8, 8), 0, -60, BRICK), "beside the brick");
        assertNull(Sweep.sweep(new Box(116, 130, 8, 8), 0, -5, BRICK), "does not get there in this step");
        // Touching the bottom face, moving away.
        assertNull(Sweep.sweep(new Box(116, 116, 8, 8), 0, 5, BRICK));
        // Already inside: the caller must separate first.
        assertNull(Sweep.sweep(new Box(110, 104, 8, 8), 3, 3, BRICK));
    }

    @Test
    void touchingEdgesDoNotOverlap() {
        assertFalse(new Box(0, 0, 10, 10).overlaps(new Box(10, 0, 10, 10)));
        assertTrue(new Box(0, 0, 10, 10).overlaps(new Box(9.5, 9.5, 10, 10)));
    }
}
