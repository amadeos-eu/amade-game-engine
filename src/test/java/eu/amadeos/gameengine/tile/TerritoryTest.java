package eu.amadeos.gameengine.tile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerritoryTest {
    // --8<-- [start:test-territory]
    @Test
    void aTowerPushesTheBorderOutAndTakesItsLandBack() {
        Territory land = new Territory(40, 30);
        land.claim(1, 10, 10, 5);
        assertTrue(land.inside(10, 15));
        assertFalse(land.inside(10, 17));
        assertTrue(land.border(15, 10));
        assertFalse(land.border(10, 10));

        land.claim(2, 16, 10, 5);                      // a lookout tower near the edge
        assertTrue(land.inside(20, 10));
        assertFalse(land.border(15, 10), "inside now, not at the border");
        land.release(2);                               // it burned
        assertFalse(land.inside(20, 10));
        assertTrue(land.inside(15, 10), "the first claim still holds its own land");
    }
    // --8<-- [end:test-territory]

    @Test
    void claimsEndAtTheEdgesOfTheMap() {
        Territory land = new Territory(10, 10);
        land.claim(7, 0, 0, 3);
        assertTrue(land.inside(0, 0));
        assertFalse(land.inside(-1, 0));
        assertFalse(land.inside(9, 9), "no wrapping");
        int size = land.size();
        int version = land.version();
        land.claim(8, 0, 0, 3);
        assertEquals(size, land.size(), "the same land twice is still the same land");
        assertTrue(land.version() > version);
        assertEquals(land.border().size(), land.border().stream().filter(c -> land.border(c.col(), c.row())).count());
    }
}
