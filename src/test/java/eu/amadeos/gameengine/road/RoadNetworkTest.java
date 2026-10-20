package eu.amadeos.gameengine.road;

import eu.amadeos.gameengine.tile.Cell;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadNetworkTest {
    /** The cells of a straight or L-shaped road: first along the row, then along the column. */
    private static List<Cell> line(Cell from, Cell to) {
        List<Cell> cells = new ArrayList<>();
        int col = from.col();
        int row = from.row();
        cells.add(new Cell(col, row));
        while (col != to.col()) {
            col += Integer.signum(to.col() - col);
            cells.add(new Cell(col, row));
        }
        while (row != to.row()) {
            row += Integer.signum(to.row() - row);
            cells.add(new Cell(col, row));
        }
        return cells;
    }

    private static RoadNetwork.Road connect(RoadNetwork roads, int a, int b) {
        return roads.connect(a, b, line(roads.cell(a), roads.cell(b)));
    }

    // --8<-- [start:test-next]
    @Test
    void aWareTakesTheShortestWayFromFlagToFlag() {
        RoadNetwork roads = new RoadNetwork();
        int home = roads.addFlag(new Cell(0, 0));
        int east = roads.addFlag(new Cell(10, 0));
        int south = roads.addFlag(new Cell(0, 4));
        int mill = roads.addFlag(new Cell(10, 4));
        RoadNetwork.Road top = connect(roads, home, east);      // 10 steps
        connect(roads, east, mill);                              // 4
        RoadNetwork.Road left = connect(roads, home, south);    // 4
        connect(roads, south, mill);                             // 10
        assertEquals(14, roads.distance(home, mill));
        // Both ways are 14 steps long: the tie goes to the road made first.
        assertEquals(top, roads.next(home, mill));

        roads.remove(top.id());
        assertEquals(left, roads.next(home, mill), "the other way now");
        assertEquals(14, roads.distance(home, mill));
        int middle = roads.split(new Cell(5, 4));                // a flag on the southern road
        assertEquals(3, roads.way(home, mill).size(), "three roads now");
        assertEquals(9, roads.distance(home, middle));
        assertNull(roads.next(mill, mill), "there already");
        assertEquals(left.id(), roads.way(south, home).getFirst().id());
    }
    // --8<-- [end:test-next]

    // --8<-- [start:test-split]
    @Test
    void aFlagInTheMiddleMakesTwoRoadsOfOne() {
        RoadNetwork roads = new RoadNetwork();
        int a = roads.addFlag(new Cell(0, 0));
        int b = roads.addFlag(new Cell(6, 0));
        RoadNetwork.Road road = connect(roads, a, b);
        assertEquals(road, roads.roadAt(new Cell(3, 0)));
        assertNull(roads.roadAt(new Cell(0, 0)), "the flags are not part of the road");

        int middle = roads.split(new Cell(3, 0));
        assertEquals(2, roads.roads().size());
        assertEquals(3, roads.next(a, b).length());
        assertEquals(middle, roads.next(a, b).other(a));
        assertEquals(6, roads.distance(a, b), "the way is as long as before");
        assertEquals(middle, roads.flagAt(new Cell(3, 0)));
    }
    // --8<-- [end:test-split]

    @Test
    void removingARoadCutsTheWayAndTheSearchIsMadeAgain() {
        RoadNetwork roads = new RoadNetwork();
        int a = roads.addFlag(new Cell(0, 0));
        int b = roads.addFlag(new Cell(4, 0));
        int c = roads.addFlag(new Cell(4, 3));
        RoadNetwork.Road ab = connect(roads, a, b);
        connect(roads, b, c);
        assertTrue(roads.connected(a, c));
        roads.next(a, c);
        roads.next(b, c);
        assertEquals(1, roads.searches(), "one search answers every flag for one goal");
        int version = roads.version();
        roads.remove(ab.id());
        assertTrue(roads.version() > version);
        assertFalse(roads.connected(a, c));
        assertNull(roads.next(a, c));
        assertTrue(roads.way(a, c).isEmpty());
        assertEquals(2, roads.searches());

        roads.removeFlag(b);
        assertTrue(roads.roads().isEmpty(), "a flag takes its roads with it");
    }

    @Test
    void roadsMustBeStepsAndMustNotCross() {
        RoadNetwork roads = new RoadNetwork();
        int a = roads.addFlag(new Cell(0, 2));
        int b = roads.addFlag(new Cell(4, 2));
        int c = roads.addFlag(new Cell(2, 0));
        int d = roads.addFlag(new Cell(2, 4));
        connect(roads, a, b);
        assertThrows(IllegalArgumentException.class,
                () -> roads.connect(c, d, line(new Cell(2, 0), new Cell(2, 4))), "crosses at 2,2");
        assertThrows(IllegalArgumentException.class,
                () -> roads.connect(c, d, List.of(new Cell(2, 0), new Cell(2, 4))), "a jump");
        assertThrows(IllegalArgumentException.class, () -> roads.addFlag(new Cell(1, 2)), "split it instead");
    }

    @Test
    void theSameNumbersGiveTheSameNetwork() {
        RoadNetwork first = new RoadNetwork();
        int a = first.addFlag(new Cell(0, 0));
        int b = first.addFlag(new Cell(5, 0));
        connect(first, a, b);
        int middle = first.split(new Cell(2, 0));

        RoadNetwork copy = new RoadNetwork();
        for (int flag : first.flags()) copy.addFlag(flag, first.cell(flag));
        for (RoadNetwork.Road road : first.roads()) copy.connect(road.id(), road.from(), road.to(), road.cells());
        copy.restore(first.nextFlag(), first.nextRoad());
        assertEquals(first.next(a, b), copy.next(a, b));
        assertEquals(first.addFlag(new Cell(9, 9)), copy.addFlag(new Cell(9, 9)));
        assertEquals(middle, copy.flagAt(new Cell(2, 0)));
    }
}
