package eu.amadeos.gameengine.drive;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadTest {
    static final RoadLook LOOK = RoadLook.of(new Color(90, 140, 60), new Color(85, 132, 56), new Color(210, 200, 180), 30000);

    // --8<-- [start:test-builder]
    @Test
    void aSectionEasesInAndOutOfItsCurveAndClimbsAlongAnS() {
        Road road = new Road();
        new RoadBuilder(road, LOOK).straight(10).section(40, 4, 1000);
        assertEquals(50, road.end());
        assertEquals(0, road.segment(9).curve, 1e-9);
        assertTrue(Math.abs(road.segment(10).curve) < 0.4, "starts gently");
        assertEquals(4, road.segment(30).curve, 0.05);   // the middle is the sharpest
        assertTrue(Math.abs(road.segment(49).curve) < 0.4, "ends gently");
        assertEquals(1000, road.segment(49).y, 1e-9);
        assertEquals(500, road.segment(29).y, 50);        // half way up in the middle
        // the climb is flat at both ends: the first and last steps are the smallest
        double first = road.segment(10).y - road.segment(9).y, middle = road.segment(30).y - road.segment(29).y;
        assertTrue(first < middle / 5);
    }
    // --8<-- [end:test-builder]

    @Test
    void heightsJoinAndAreInterpolatedAlongASegment() {
        Road road = new Road();
        new RoadBuilder(road, LOOK).section(10, 0, 400).section(10, 0, -400);
        for (long i = 1; i < road.end(); i++) assertEquals(road.segment(i - 1).y, road.segment(i).startY(), 1e-9);
        Segment s = road.segment(5);
        assertEquals((s.startY() + s.y) / 2, road.heightAt(5.5 * Road.SEGMENT), 1e-9);
    }

    // --8<-- [start:test-endless]
    @Test
    void anEndlessRoadDropsWhatLiesBehindAndKeepsItsNumbers() {
        Road road = new Road();
        RoadBuilder b = new RoadBuilder(road, LOOK);
        for (int i = 0; i < 100; i++) {
            b.section(50, 2, 300);
            road.dropBefore(road.end() - 120);
        }
        assertEquals(5000, road.end());
        assertTrue(road.size() <= 170, "only the last stretch is kept: " + road.size());
        assertFalse(road.has(10));
        assertTrue(road.has(4999));
        assertEquals(4999, road.segment(4999).index());
        assertSame(road.segment(road.first()), road.segment(3), "a dropped segment gives the first one left");
    }
    // --8<-- [end:test-endless]
}
