package eu.amadeos.gameengine.path;

import eu.amadeos.gameengine.tile.Cell;
import eu.amadeos.gameengine.tile.Direction;
import eu.amadeos.gameengine.tile.TileMap;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AStarTest {
    static final TileMap<Character> MAZE = DistanceMapTest.MAZE;

    static double steps(int col, int row) {
        return DistanceMapTest.open(col, row) ? 1 : Double.POSITIVE_INFINITY;
    }

    // --8<-- [start:test-astar]
    @Test
    void aStarFindsAShortestWayAndLooksAtFewerTiles() {
        Cell from = new Cell(1, 1);
        Cell to = new Cell(7, 1);
        AStar.Path path = AStar.find(MAZE, from, to, AStarTest::steps);
        DistanceMap wave = DistanceMap.search(MAZE, DistanceMapTest::open, to);
        assertEquals(wave.distance(from), path.cost(), "as short as breadth-first search finds");
        assertEquals(path.steps().size() + 1, path.cells().size());
        assertEquals(from, path.cells().getFirst());
        assertEquals(to, path.cells().getLast());
        assertEquals(Direction.RIGHT, path.first());
        assertTrue(path.visited() < wave.visited(), path.visited() + " tiles against " + wave.visited());
    }

    @Test
    void dangerMakesTheLongerWayCheaper() {
        // Through the tunnel, or along the row: the row is shorter, but a ghost sits in it.
        Cell from = new Cell(2, 5);
        Cell to = new Cell(6, 5);
        assertEquals(Direction.RIGHT, AStar.find(MAZE, from, to, AStarTest::steps).first());
        AStar.Path safe = AStar.find(MAZE, from, to, (col, row) ->
                col == 4 && row == 5 ? 50 : steps(col, row));
        assertEquals(Direction.LEFT, safe.first(), "the long way round, over the edge");
        assertEquals(List.of(new Cell(2, 5), new Cell(1, 5), new Cell(0, 5), new Cell(8, 5), new Cell(7, 5),
                new Cell(6, 5)), safe.cells());
    }
    // --8<-- [end:test-astar]

    @Test
    void noWayIsAnEmptyPath() {
        AStar.Path none = AStar.find(MAZE, new Cell(1, 1), new Cell(3, 7), AStarTest::steps);
        assertFalse(none.found());
        assertEquals(null, none.first());
        assertEquals(0, AStar.find(MAZE, new Cell(1, 1), new Cell(1, 1), AStarTest::steps).cost());
    }
}
