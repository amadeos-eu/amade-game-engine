package eu.amadeos.gameengine.motion;

import eu.amadeos.gameengine.math.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightPathTest {
    private static final double EPS = 1e-6;

    @Test
    void aLineIsAsLongAsItIsAndPointsItsWay() {
        FlightPath line = FlightPath.from(10, 20).lineTo(40, 60).build();
        assertEquals(50, line.length(), EPS);
        assertEquals(new Vector(25, 40), round(line.at(25)));
        assertEquals(Math.atan2(40, 30), line.angleAt(10), EPS);
        assertEquals(new Vector(10, 20), line.at(-5), "before the start: the start");
        assertEquals(new Vector(40, 60), line.at(500), "after the end: the end");
    }

    // --8<-- [start:test-path]
    @Test
    void aCurveIsFlownAtAnEvenSpeed() {
        // A swoop from the top down and round to the left: sharp at the bottom, nearly straight at the top.
        FlightPath swoop = FlightPath.from(320, 0).curveTo(320, 300, 0, 300, 100, 100).build();
        double step = swoop.length() / 40;
        double shortest = Double.MAX_VALUE;
        double longest = 0;
        for (int i = 0; i < 40; i++) {
            double gap = swoop.at(i * step).distance(swoop.at((i + 1) * step));
            shortest = Math.min(shortest, gap);
            longest = Math.max(longest, gap);
        }
        // Equal distances along the curve: equal steps on the screen, to a hundredth.
        assertEquals(step, shortest, step * 0.01);
        assertEquals(step, longest, step * 0.01);
        assertEquals(Math.PI / 2, swoop.angleAt(0), EPS, "starts straight down");
    }
    // --8<-- [end:test-path]

    @Test
    void curvesFollowOneAnother() {
        FlightPath path = FlightPath.from(0, 0).lineTo(100, 0).curveTo(150, 0, 150, 50, 150, 100).build();
        assertEquals(new Vector(100, 0), round(path.at(100)));
        assertEquals(0, path.angleAt(99), EPS);
        assertEquals(Math.PI / 2, path.angleAt(path.length()), EPS, "ends going down");
        assertTrue(path.length() > 100 + 100 && path.length() < 100 + 150);
    }

    @Test
    void aPathCanBeMirroredAndMoved() {
        FlightPath right = FlightPath.from(400, 0).curveTo(400, 100, 600, 100, 600, 200).build();
        FlightPath left = right.mirrored(640);
        assertEquals(new Vector(240, 0), left.start());
        assertEquals(new Vector(40, 200), left.end());
        assertEquals(right.length(), left.length(), EPS);
        assertEquals(Math.PI - right.angleAt(80), left.angleAt(80), EPS);
        FlightPath moved = right.translated(-400, 10);
        assertEquals(new Vector(0, 10), moved.start());
        assertEquals(new Vector(200, 210), moved.end());
    }

    @Test
    void aControlPointOnTheEndStillGivesADirection() {
        FlightPath path = FlightPath.from(0, 0).curveTo(0, 0, 100, 0, 100, 0).build();
        assertEquals(0, path.angleAt(0), EPS);
        assertEquals(0, path.angleAt(path.length()), EPS);
        assertThrows(IllegalArgumentException.class, () -> FlightPath.from(1, 1).build());
    }

    private static Vector round(Vector v) {
        return new Vector(Math.round(v.x() * 1e6) / 1e6, Math.round(v.y() * 1e6) / 1e6);
    }
}
