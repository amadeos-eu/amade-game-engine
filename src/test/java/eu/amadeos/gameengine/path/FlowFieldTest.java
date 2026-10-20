package eu.amadeos.gameengine.path;

import eu.amadeos.gameengine.data.TextData;
import eu.amadeos.gameengine.math.Vector;
import eu.amadeos.gameengine.random.SeededRandom;
import eu.amadeos.gameengine.tile.Cell;
import eu.amadeos.gameengine.tile.TileMap;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowFieldTest {
    static TileMap<Character> map(String rows) {
        return TileMap.read(TextData.parse("field.txt", new StringReader(rows)), Map.of('X', 'X', '.', '.'));
    }

    static FlowField field(TileMap<Character> map, Cell... goals) {
        return FlowField.toward(map.width(), map.height(), (col, row) -> map.get(col, row) != 'X', goals);
    }

    // --8<-- [start:test-distances]
    @Test
    void diagonalStepsCostMoreAndCornersAreNotCut() {
        TileMap<Character> map = map("""
                row ......
                row ..X...
                row ...X..
                row ......
                """);
        FlowField field = field(map, new Cell(5, 3));
        assertEquals(0, field.distance(5, 3));
        assertEquals(10, field.distance(4, 3), "one step to the side");
        assertEquals(14, field.distance(4, 2), "one diagonal step");
        assertEquals(28 + 30, field.distance(0, 1), "two diagonal steps and three straight ones");
        // From (2, 2) the diagonal to (3, 3) would cut the corner of the wall at (3, 2): down first.
        assertEquals(new Cell(2, 3), field.next(2, 2));
        assertNull(field.next(5, 3), "at the goal");
    }
    // --8<-- [end:test-distances]

    // --8<-- [start:test-heading]
    @Test
    void unitsFollowTheArrowsAndNeverEnterAClosedTile() {
        TileMap<Character> map = map("""
                row ..........
                row .XXXXXXXX.
                row .X......X.
                row .X.XX.XXX.
                row ...X..X...
                row XXXX..XXXX
                """);
        FlowField field = field(map, new Cell(4, 4));
        SeededRandom random = new SeededRandom(7);
        for (int unit = 0; unit < 50; unit++) {
            // Anywhere in the open tile (0, 0), aiming up to 0.3 tiles beside the middle of each next tile.
            double x = random.nextDouble();
            double y = random.nextDouble();
            double aside = (random.nextDouble() - 0.5) * 0.6;
            for (int step = 0; step < 4000; step++) {
                int col = (int) Math.floor(x);
                int row = (int) Math.floor(y);
                assertTrue(map.get(col, row) != 'X', "unit " + unit + " in a wall at " + col + ", " + row);
                if (field.isGoal(col, row)) break;
                Cell next = field.next(col, row);
                Vector way = new Vector(next.col() + 0.5 + aside - x, next.row() + 0.5 + aside - y);
                Vector move = way.normalized().times(Math.min(0.02, way.length()));
                x += move.x();
                y += move.y();
            }
            assertTrue(field.isGoal((int) Math.floor(x), (int) Math.floor(y)), "unit " + unit + " arrived");
        }
        assertEquals(new Vector(0, -1), field.heading(4.5, 4.9), "on the goal: to its middle");
        assertEquals(Vector.ZERO, field.heading(4.5, 4.5));
    }
    // --8<-- [end:test-heading]

    // --8<-- [start:test-blocked]
    @Test
    void aNewWallCanCutTheWayAndTheFieldSaysSo() {
        TileMap<Character> map = map("""
                row ......
                row XX.XXX
                row ......
                """);
        Cell goal = new Cell(5, 2);
        assertTrue(field(map, goal).reaches(0, 0));
        map.set(2, 1, 'X');      // a tower in the only gap
        FlowField cut = field(map, goal);
        assertFalse(cut.reaches(0, 0));
        assertEquals(FlowField.UNREACHABLE, cut.distance(0, 0));
        assertEquals(Vector.ZERO, cut.heading(0.5, 0.5), "nowhere to go");
        assertTrue(cut.reaches(0, 2));
    }
    // --8<-- [end:test-blocked]

    // --8<-- [start:test-many]
    @Test
    void oneFieldServesAllUnitsWhereAStarSearchesForEach() {
        TileMap<Character> map = map("""
                row ................................
                row ..XXXXXX........XXXXXXXX........
                row ........X..............X........
                row ..XXXX..X...XXXXXXXX...X..XXXX..
                row .....X..X..........X...X........
                row .....X..XXXXXXXX...X...XXXXXX...
                row .....X.............X............
                row ................................
                """);
        Cell castle = new Cell(16, 6);
        FlowField field = field(map, castle);
        int settled = field.visited();
        int searched = 0;
        int units = 0;
        for (int row = 0; row < map.height(); row++) {
            if (map.get(0, row) == 'X') continue;
            for (int i = 0; i < 25; i++) {
                AStar.Path path = AStar.find(map, new Cell(0, row), castle,
                        (c, r) -> map.get(c, r) == 'X' ? Double.POSITIVE_INFINITY : 1);
                searched += path.visited();
                units++;
            }
        }
        assertEquals(200, units);
        // 200 searches of about 37 tiles each, against one search of all 200 open tiles.
        assertTrue(searched > 30 * settled, searched + " tiles for A*, " + settled + " for the field");
    }
    // --8<-- [end:test-many]
}
