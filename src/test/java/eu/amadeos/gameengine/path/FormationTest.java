package eu.amadeos.gameengine.path;

import eu.amadeos.gameengine.tile.Cell;
import eu.amadeos.gameengine.tile.TileMap;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormationTest {
    private static Passable open(TileMap<Character> map) {
        return (col, row) -> map.get(col, row) != 'X';
    }

    // --8<-- [start:test-slots]
    @Test
    void slotsAreTheNearestOpenTilesAndNeverBehindAWall() {
        TileMap<Character> map = FlowFieldTest.map("""
                row ........
                row ...X....
                row ...X....
                row ...X....
                row ........
                """);
        List<Cell> slots = Formation.slots(new Cell(4, 2), 5, map.width(), map.height(), open(map));
        assertEquals(new Cell(4, 2), slots.getFirst(), "the target first");
        assertEquals(5, slots.size());
        assertEquals(5, new HashSet<>(slots).size(), "one unit per slot");
        for (Cell slot : slots) assertTrue(slot.col() >= 4, slot + ": none behind the wall, though it is near");

        List<Cell> onRock = Formation.slots(new Cell(3, 2), 1, map.width(), map.height(), open(map));
        assertEquals(1, Math.abs(onRock.getFirst().col() - 3), "a click on the wall: the tile beside it");
    }
    // --8<-- [end:test-slots]

    // --8<-- [start:test-assign]
    @Test
    void eachUnitTakesTheSlotThatSuitsIt() {
        // Three units coming from the left, in a column; three slots in a column on the right.
        double[] xs = {1.5, 1.5, 1.5};
        double[] ys = {1.5, 3.5, 5.5};
        List<Cell> slots = List.of(new Cell(9, 5), new Cell(9, 1), new Cell(9, 3));
        int[] slotOf = Formation.assign(xs, ys, slots);
        assertArrayEquals(new int[]{1, 2, 0}, slotOf, "the top unit to the top slot: no crossing");

        int[] tooFew = Formation.assign(xs, ys, List.of(new Cell(9, 3)));
        assertArrayEquals(new int[]{-1, 0, -1}, tooFew, "the nearest one gets the only slot");
    }
    // --8<-- [end:test-assign]

    // --8<-- [start:test-line]
    @Test
    void aStraightLineIsClearOnlyIfEveryTileOnItIsOpen() {
        TileMap<Character> map = FlowFieldTest.map("""
                row ......
                row ..X...
                row ......
                row ...X..
                """);
        Passable open = open(map);
        assertTrue(Formation.clearLine(0.5, 0.5, 5.5, 0.5, 6, 4, open), "along the top row");
        assertFalse(Formation.clearLine(0.5, 1.5, 5.5, 1.5, 6, 4, open), "through the wall");
        assertTrue(Formation.clearLine(0.5, 2.5, 5.5, 2.2, 6, 4, open));
        // Exactly through the corner between (2, 2) and (3, 3), beside the walls at (3, 3) itself.
        assertFalse(Formation.clearLine(2.5, 2.5, 3.5, 3.5, 6, 4, open), "into the wall");
        // From (1, 1) up and right to (2, 0): both are open, but the line squeezes past the corner of (2, 1).
        assertFalse(Formation.clearLine(1.5, 1.5, 2.5, 0.5, 6, 4, open), "no corner cut");
    }
    // --8<-- [end:test-line]

    // --8<-- [start:test-cache]
    @Test
    void aFieldIsSearchedOnceUntilTheMapChanges() {
        TileMap<Character> map = FlowFieldTest.map("""
                row ......
                row ......
                """);
        FieldCache fields = new FieldCache(map.width(), map.height(), open(map), 2);
        FlowField a = fields.toward(new Cell(5, 0));
        assertEquals(a, fields.toward(new Cell(5, 0)), "a second group to the same place");
        assertEquals(1, fields.searches());
        fields.toward(new Cell(0, 0));
        fields.toward(new Cell(0, 1));
        assertEquals(2, fields.size(), "the least recently used field is dropped");
        fields.invalidate();
        fields.toward(new Cell(0, 1));
        assertEquals(4, fields.searches(), "after a change everything is searched again");
    }
    // --8<-- [end:test-cache]
}
