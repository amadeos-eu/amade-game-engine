package eu.amadeos.gameengine.collision;

import eu.amadeos.gameengine.random.SeededRandom;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridTest {
    @Test
    void onlyTheThingsInTheCellsAroundAreVisited() {
        Grid<String> grid = new Grid<>(640, 400, 32);
        assertEquals(20, grid.columns());
        assertEquals(13, grid.rows());
        grid.add("near", 100, 100, 6);                  // in the cell of x 96 to 127
        grid.add("across a cell edge", 126, 100, 6);    // in that cell and the next, from x 128
        grid.add("far", 500, 300, 6);
        List<String> found = new ArrayList<>();
        assertEquals(2, grid.forEachNear(100, 100, 0, found::add), "both in the cell, each once");
        assertEquals(Set.of("near", "across a cell edge"), new HashSet<>(found));
        found.clear();
        grid.forEachNear(140, 100, 0, found::add);
        assertEquals(List.of("across a cell edge"), found);
    }

    @Test
    void thingsOutsideTheFieldAreInTheCellsAtItsEdge() {
        Grid<String> grid = new Grid<>(640, 400, 32);
        grid.add("coming in from above", 320, -40, 8);
        List<String> found = new ArrayList<>();
        grid.forEachNear(325, -35, 0, found::add);
        assertEquals(List.of("coming in from above"), found);
        grid.clear();
        found.clear();
        grid.forEachNear(325, -35, 0, found::add);
        assertEquals(List.of(), found);
        assertEquals(0, grid.size());
    }

    // --8<-- [start:test-grid]
    @Test
    void theGridFindsEveryHitWithAFractionOfTheTests() {
        SeededRandom random = new SeededRandom(24);
        int n = 2000;
        double[] x = new double[n];
        double[] y = new double[n];
        Grid<Integer> grid = new Grid<>(640, 400, 16);
        for (int i = 0; i < n; i++) {
            x[i] = random.nextDouble() * 640;
            y[i] = random.nextDouble() * 400;
            grid.add(i, x[i], y[i], 3);
        }
        // Every pair, the slow way: n × (n - 1) / 2 tests.
        Set<Long> slow = new HashSet<>();
        for (int a = 0; a < n; a++) {
            for (int b = a + 1; b < n; b++) {
                if (new Circle(x[a], y[a], 3).overlaps(new Circle(x[b], y[b], 3))) slow.add((long) a * n + b);
            }
        }
        // With the grid: only the neighbours of each.
        Set<Long> fast = new HashSet<>();
        int[] tests = {0};
        for (int a = 0; a < n; a++) {
            int me = a;
            grid.forEachNear(x[a], y[a], 3, b -> {
                if (b <= me) return;
                tests[0]++;
                if (new Circle(x[me], y[me], 3).overlaps(new Circle(x[b], y[b], 3))) fast.add((long) me * n + b);
            });
        }
        assertEquals(slow, fast, "the same hits");
        assertTrue(tests[0] < n * (n - 1) / 2 / 100, tests[0] + " tests instead of " + n * (n - 1) / 2);
    }
    // --8<-- [end:test-grid]
}
