package eu.amadeos.gameengine.math;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class WrapTest {
    private final Wrap field = new Wrap(640, 400);

    // --8<-- [start:test-wrap]
    @Test
    void whatLeavesOnOneSideComesBackOnTheOther() {
        assertEquals(new Vector(5, 390), field.wrap(new Vector(645, -10)));
        assertEquals(0, field.x(640));
        assertEquals(639.5, field.x(-0.5));
    }

    @Test
    void theShortestWayMayLeadAcrossAnEdge() {
        assertEquals(new Vector(20, 0), field.delta(new Vector(630, 100), new Vector(10, 100)));
        assertEquals(new Vector(-20, 30), field.delta(new Vector(10, 385), new Vector(630, 15)));
        assertEquals(new Vector(100, 0), field.delta(new Vector(100, 100), new Vector(200, 100)), "else straight");
    }

    @Test
    void aThingAcrossTheEdgeIsDrawnBetweenItsPositionsTheShortWay() {
        // From 638 to 2 in one step, across the edge: a quarter of the way is 639, given as -1, just
        // outside the field on the side the thing is on now; not 479, flying back across the screen.
        assertEquals(-1, field.between(new Vector(638, 50), new Vector(2, 50), 0.25).x(), 1e-9);
    }
    // --8<-- [end:test-wrap]

    @Test
    void aThingInsideIsNotCopied() {
        Vector inside = new Vector(1, 2);
        assertSame(inside, field.wrap(inside));
        assertEquals(List.of("320.0 200.0"), copies(320, 200, 30));
    }

    // --8<-- [start:test-copies]
    @Test
    void aThingOverAnEdgeIsDrawnTwiceAndInACornerFourTimes() {
        assertEquals(List.of("630.0 200.0", "-10.0 200.0"), copies(630, 200, 30));
        assertEquals(List.of("5.0 395.0", "645.0 395.0", "5.0 -5.0", "645.0 -5.0"), copies(5, 395, 30));
    }
    // --8<-- [end:test-copies]

    @Test
    void positionsJustOutsideAreWrappedFirst() {
        assertEquals(List.of("639.0 200.0", "-1.0 200.0"), copies(-1, 200, 10));
    }

    private List<String> copies(double x, double y, double radius) {
        List<String> at = new ArrayList<>();
        field.forEachCopy(x, y, radius, (cx, cy) -> at.add(cx + " " + cy));
        return at;
    }
}
