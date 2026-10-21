package eu.amadeos.gameengine.tile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplyGridTest {
    // --8<-- [start:test-grid]
    @Test
    void thePlantFeedsTheNearestHomesFirst() {
        // A plant of 5 at the left end of a line of ten tiles, a home of 2 on every other tile.
        SupplyGrid power = new SupplyGrid(12, 3);
        power.clear();
        for (int col = 0; col < 10; col++) power.conductor(col, 1);
        power.source(0, 1, 5);
        for (int col = 2; col < 10; col += 2) power.load(col, 1, 2);
        power.solve();
        assertTrue(power.supplied(2, 1));
        assertTrue(power.supplied(4, 1));
        assertFalse(power.supplied(6, 1), "5 feeds two homes of 2, not three");
        assertFalse(power.supplied(8, 1));
        assertTrue(power.connected(8, 1), "connected, but the plant is too small");
        assertEquals(5, power.supply());
        assertEquals(8, power.demand());
        assertEquals(4, power.served());
    }
    // --8<-- [end:test-grid]

    @Test
    void aGapInTheLineMakesTwoNetworks() {
        SupplyGrid power = new SupplyGrid(10, 1);
        power.clear();
        for (int col = 0; col < 10; col++) if (col != 5) power.conductor(col, 0);
        power.source(0, 0, 100);
        power.load(8, 0, 1);
        power.load(3, 0, 1);
        power.solve();
        assertTrue(power.supplied(3, 0));
        assertFalse(power.supplied(8, 0), "across the gap");
        assertFalse(power.connected(8, 0));
        assertEquals(2, power.networks());
        assertEquals(-1, power.network(5, 0));
    }

    @Test
    void tilesTouchAlongEdgesNotAcrossCorners() {
        SupplyGrid power = new SupplyGrid(3, 3);
        power.clear();
        power.source(0, 0, 10);
        power.load(1, 1, 1);
        power.solve();
        assertFalse(power.supplied(1, 1));
        power.conductor(1, 0);
        power.solve();
        assertTrue(power.supplied(1, 1));
    }
}
