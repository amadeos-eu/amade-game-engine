package eu.amadeos.gameengine.tile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalkerTest {
    /** Corridors: a row from left to right with a tunnel at both ends, and a way down from column 3. */
    static final TileMap<TileMapTest.Tile> MAP = TileMapTest.map("""
            row XXXXXXX
            row _______
            row XXX_XXX
            row XXX_XXX
            row XXXXXXX
            """);

    static boolean open(int col, int row) {
        return MAP.get(Math.floorMod(col, MAP.width()), Math.floorMod(row, MAP.height())) != TileMapTest.Tile.WALL;
    }

    /** Goes on while it can, turns to {@code wanted} where that is open, stops at a wall. */
    static Walker.Steering wanting(Direction wanted) {
        return (col, row, heading) -> {
            if (wanted != null && open(col + wanted.dx, row + wanted.dy)) return wanted;
            if (heading != null && open(col + heading.dx, row + heading.dy)) return heading;
            return null;
        };
    }

    // --8<-- [start:test-walker]
    @Test
    void aWalkerTurnsOnlyAtTheCentreOfATile() {
        Walker walker = new Walker(1, 1, Direction.RIGHT);
        walker.walk(1.5, MAP, wanting(Direction.DOWN));
        assertEquals(2.5, walker.x(), 1e-9, "no way down at column 2: on to the right");
        assertEquals(1, walker.y());
        walker.walk(1.0, MAP, wanting(Direction.DOWN));
        assertEquals(3, walker.x(), 1e-9, "at the centre of column 3 it turned");
        assertEquals(1.5, walker.y(), 1e-9);
        assertEquals(Direction.DOWN, walker.heading());
        assertEquals(1.5, walker.walk(5, MAP, wanting(Direction.DOWN)), 1e-9, "a wall: it stops after 1.5");
        assertEquals(3, walker.y());
        assertNull(walker.heading());
    }

    @Test
    void turningRoundNeedsNoCentre() {
        Walker walker = new Walker(1, 1, Direction.RIGHT);
        walker.walk(0.3, MAP, wanting(null));
        walker.turnAround();
        walker.walk(0.5, MAP, wanting(null));
        assertEquals(0.8, walker.x(), 1e-9);
        assertEquals(Direction.LEFT, walker.heading());
    }
    // --8<-- [end:test-walker]

    @Test
    void theTunnelLeadsToTheOtherSideAndTheDrawingFollows() {
        Walker walker = new Walker(0, 1, Direction.LEFT);
        walker.walk(0.75, MAP, wanting(null));
        assertEquals(6.25, walker.x(), 1e-9, "out on the left, in on the right");
        assertEquals(7, walker.previousX(), 1e-9, "the previous position moved with it: a short way to draw");
        assertEquals(6, walker.col());
        walker.walk(0.25, MAP, wanting(null));
        assertTrue(walker.atCentre());
    }

    @Test
    void aStandingWalkerStartsWhenTheSteeringSaysSo() {
        Walker walker = new Walker(3, 3, null);
        assertEquals(0, walker.walk(1, MAP, wanting(Direction.LEFT)), "a wall to the left");
        assertEquals(1, walker.walk(1, MAP, wanting(Direction.UP)), 1e-9);
        assertEquals(new Cell(3, 2), walker.cell());
    }
}
