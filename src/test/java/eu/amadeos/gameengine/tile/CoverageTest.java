package eu.amadeos.gameengine.tile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoverageTest {
    // --8<-- [start:test-coverage]
    @Test
    void placesReachACircleAndTheNearestCounts() {
        Coverage schools = new Coverage(30, 30);
        schools.add(10, 10, 5);
        schools.add(14, 10, 5);
        assertEquals(2, schools.count(12, 10), "between both");
        assertEquals(1, schools.count(6, 10));
        assertFalse(schools.covered(10, 16), "six tiles away");
        assertEquals(1 - 0 / 6.0, schools.strength(10, 10), 1e-6);
        assertEquals(1 - 2 / 6.0, schools.strength(12, 10), 1e-6, "two tiles from either");
        assertTrue(schools.strength(10, 13) < schools.strength(10, 12));
        schools.clear();
        assertEquals(0, schools.count(10, 10));
    }
    // --8<-- [end:test-coverage]

    @Test
    void aBigPlaceHasItsMiddleBetweenTilesAndCirclesStopAtTheEdge() {
        Coverage parks = new Coverage(8, 8);
        parks.add(1.0, 1.0, 1);      // the corner between tiles (0, 0) and (1, 1)
        assertTrue(parks.covered(0, 0));
        assertTrue(parks.covered(1, 1));
        assertFalse(parks.covered(2, 2));
        assertEquals(0, parks.count(-1, 0), "off the map");
        assertEquals(parks.strength(0, 0), parks.strength(1, 1), 1e-9);
    }
}
