package eu.amadeos.gameengine.ai;

import eu.amadeos.gameengine.tile.Cell;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InfluenceMapTest {
    // --8<-- [start:test-influence]
    @Test
    void strengthFallsWithDistanceAndAddsUp() {
        InfluenceMap enemy = new InfluenceMap(30, 20);
        assertNull(enemy.strongest(), "nobody there");
        enemy.add(5, 5, 10, 4);                 // a turret
        enemy.add(7, 5, 6, 4);                  // and a tank two tiles away
        enemy.add(25, 15, 4, 2);                // a lone trike
        assertEquals(10 + 6 * 0.6, enemy.at(5, 5), 1e-9, "all of the turret, three fifths of the tank");
        assertEquals(10 * 0.8 + 6 * 0.8, enemy.at(6, 5), 1e-9, "a tile from both, a fifth less from each");
        assertEquals(0, enemy.at(15, 5), "beyond the reach of both");
        assertEquals(new Cell(5, 5), enemy.strongest(), "at the turret");

        // The refinery by the trike is defended worst: attack there.
        List<Cell> buildings = List.of(new Cell(6, 6), new Cell(24, 15), new Cell(9, 5));
        assertEquals(new Cell(24, 15), enemy.weakest(buildings));
    }
    // --8<-- [end:test-influence]
}
