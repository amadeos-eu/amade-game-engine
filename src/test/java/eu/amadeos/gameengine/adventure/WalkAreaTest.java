package eu.amadeos.gameengine.adventure;

import eu.amadeos.gameengine.math.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalkAreaTest {
    static Vector v(double x, double y) {
        return new Vector(x, y);
    }

    /** A floor 0..100 x 0..100 with a table 40..60 x 20..80 in the middle. */
    static final WalkArea ROOM = new WalkArea(
            List.of(v(0, 0), v(100, 0), v(100, 100), v(0, 100)),
            List.of(List.of(v(40, 20), v(60, 20), v(60, 80), v(40, 80))));

    /** An L-shaped floor: the corner at (50, 50) points into the room. */
    static final WalkArea CORRIDOR = new WalkArea(
            List.of(v(0, 0), v(50, 0), v(50, 50), v(100, 50), v(100, 100), v(0, 100)));

    static double length(List<Vector> way) {
        double sum = 0;
        for (int i = 1; i < way.size(); i++) sum += way.get(i - 1).distance(way.get(i));
        return sum;
    }

    @Test
    void insideOutsideAndTheEdge() {
        assertTrue(ROOM.contains(v(10, 50)));
        assertFalse(ROOM.contains(v(50, 50)), "on the table");
        assertTrue(ROOM.contains(v(40, 50)), "the edge of the table belongs to the floor");
        assertTrue(ROOM.contains(v(0, 0)));
        assertFalse(ROOM.contains(v(-1, 50)));
        assertFalse(CORRIDOR.contains(v(75, 25)), "the cut-out corner of the L");
    }

    @Test
    void aClickOffTheFloorWalksToTheNearestPointOnIt() {
        assertEquals(v(100, 30), ROOM.nearest(v(130, 30)));
        assertEquals(v(40, 50), ROOM.nearest(v(45, 50)), "from the table to its nearer side");
        assertEquals(v(10, 10), ROOM.nearest(v(10, 10)), "a point on the floor stays");
    }

    // --8<-- [start:test-path]
    @Test
    void theWayRunsStraightWhereItCanAndRoundTheTableWhereItMust() {
        assertEquals(List.of(v(10, 10), v(90, 10)), ROOM.path(v(10, 10), v(90, 10)), "past the table");
        List<Vector> round = ROOM.path(v(20, 50), v(80, 50));
        assertEquals(4, round.size(), "two corners of the table");
        assertTrue(round.get(1).equals(v(40, 20)) || round.get(1).equals(v(40, 80)), round.toString());
        assertEquals(20 + 2 * Math.hypot(20, 30), length(round), 1e-9, "as short as a string pulled tight");
        List<Vector> corner = CORRIDOR.path(v(25, 10), v(90, 75));
        assertEquals(List.of(v(25, 10), v(50, 50), v(90, 75)), corner, "round the inner corner of the L");
    }
    // --8<-- [end:test-path]

    @Test
    void aGoalOffTheFloorIsWalkedToItsNearestPoint() {
        List<Vector> way = ROOM.path(v(10, 50), v(48, 50));
        assertEquals(v(40, 50), way.getLast());
        assertEquals(2, way.size());
    }

    @Test
    void anOnlyTouchingCornerDoesNotHideTheWay() {
        // From the table's top left corner along its top edge: along the edge, not through the table.
        assertTrue(ROOM.sees(v(40, 20), v(60, 20)));
        assertFalse(ROOM.sees(v(40, 20), v(60, 80)), "the diagonal crosses the table");
    }
}
