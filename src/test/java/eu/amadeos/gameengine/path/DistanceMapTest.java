package eu.amadeos.gameengine.path;

import eu.amadeos.gameengine.data.TextData;
import eu.amadeos.gameengine.tile.Cell;
import eu.amadeos.gameengine.tile.Direction;
import eu.amadeos.gameengine.tile.TileMap;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DistanceMapTest {
    static final TileMap<Character> MAZE = TileMap.read(TextData.parse("maze.txt", new StringReader("""
            row XXXXXXXXX
            row X...X...X
            row X.X.X.X.X
            row X.X...X.X
            row X.XXXXX.X
            row .........
            row XXXXXXXXX
            row XXX.XXXXX
            """)), Map.of('X', 'X', '.', '.'));

    static boolean open(int col, int row) {
        return MAZE.get(col, row) != 'X';
    }

    // --8<-- [start:test-distances]
    @Test
    void theWaveCountsTheStepsToTheGoalFromEveryTile() {
        DistanceMap home = DistanceMap.search(MAZE, DistanceMapTest::open, new Cell(4, 3));
        assertEquals(0, home.distance(4, 3));
        assertEquals(2, home.distance(3, 2));
        assertEquals(3, home.distance(3, 1), "down and round the corner");
        assertEquals(5, home.distance(7, 1), "round the wall, not through it");
        assertEquals(9, home.distance(1, 5));
        assertEquals(DistanceMap.UNREACHABLE, home.distance(3, 7), "walled in");
        // Downhill from anywhere is a shortest way home.
        Cell at = new Cell(0, 5);
        int steps = 0;
        for (Direction d; (d = home.downhill(at.col(), at.row())) != null; steps++) at = MAZE.step(at.col(), at.row(), d);
        assertEquals(new Cell(4, 3), at);
        assertEquals(home.distance(0, 5), steps);
        assertNull(home.downhill(3, 7));
    }
    // --8<-- [end:test-distances]

    @Test
    void theWayLeadsThroughTheTunnelWhenThatIsShorter() {
        DistanceMap right = DistanceMap.search(MAZE, DistanceMapTest::open, new Cell(8, 5));
        assertEquals(1, right.distance(0, 5), "over the edge");
        assertEquals(Direction.LEFT, right.downhill(0, 5));
        assertEquals(2, DistanceMap.search(MAZE, DistanceMapTest::open, new Cell(1, 5), new Cell(7, 5))
                .distance(7, 3), "the nearest of two goals");
    }
}
