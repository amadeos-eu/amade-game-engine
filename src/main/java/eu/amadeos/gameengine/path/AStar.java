package eu.amadeos.gameengine.path;

import eu.amadeos.gameengine.tile.Cell;
import eu.amadeos.gameengine.tile.Direction;
import eu.amadeos.gameengine.tile.TileMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * The cheapest way from one tile to another, found by <b>A*</b> ("A star", Hart, Nilsson and Raphael, 1968).
 *
 * <p>Breadth-first search spreads evenly in all directions and counts steps. A* differs in two ways. Each
 * tile has a <i>cost</i> to enter, so a way past danger can cost more than a longer way round it. And the
 * search looks first at the tiles that seem to lead to the goal: of all tiles at its border it always
 * takes the one with the smallest <i>g</i> + <i>h</i>, where <i>g</i> is the cost from the start so far,
 * and <i>h</i> a guess of the cost still to come. The guess here is the number of steps without walls,
 * across and along (the <i>Manhattan distance</i>). As long as every tile costs at least 1, the guess is
 * never too high, and then the way A* finds is always one of the cheapest.
 *
 * <p>Steps over an edge of the map arrive on the far side, as in {@link TileMap#step}, and the guess knows
 * it. Use it on one thread at a time.
 */
public final class AStar {
    /** What it costs to enter a tile: at least 1; {@link Double#POSITIVE_INFINITY} where one cannot go. */
    @FunctionalInterface
    public interface Cost {
        double enter(int col, int row);
    }

    /**
     * A way found: the tiles from the start to the goal, both included, the steps between them, the cost,
     * and how many tiles the search had to look at. Without a way, all lists are empty.
     */
    public record Path(List<Cell> cells, List<Direction> steps, double cost, int visited) {
        public boolean found() {
            return !cells.isEmpty();
        }

        /** The first step, {@code null} if there is none: the start is the goal, or there is no way. */
        public Direction first() {
            return steps.isEmpty() ? null : steps.getFirst();
        }
    }

    private record Open(int index, double f, double h) {
    }

    private AStar() {
    }

    // --8<-- [start:astar]
    public static Path find(TileMap<?> map, Cell from, Cell to, Cost cost) {
        int width = map.width();
        int size = width * map.height();
        double[] g = new double[size];
        Arrays.fill(g, Double.POSITIVE_INFINITY);
        int[] cameFrom = new int[size];
        Direction[] cameBy = new Direction[size];
        boolean[] closed = new boolean[size];
        // The border of the search, cheapest g + h first; of equal ones, the one nearer the goal.
        PriorityQueue<Open> border = new PriorityQueue<>((a, b) -> a.f() != b.f()
                ? Double.compare(a.f(), b.f()) : Double.compare(a.h(), b.h()));
        int start = from.row() * width + from.col();
        int goal = to.row() * width + to.col();
        g[start] = 0;
        border.add(new Open(start, guess(map, from, to), guess(map, from, to)));
        int visited = 0;
        while (!border.isEmpty()) {
            int index = border.poll().index();
            if (closed[index]) continue;            // an older, dearer entry of a tile seen again
            closed[index] = true;
            visited++;
            if (index == goal) return path(map, cameFrom, cameBy, start, goal, g[goal], visited);
            int col = index % width;
            int row = index / width;
            for (Direction d : Direction.values()) {
                Cell next = map.step(col, row, d);
                int n = next.row() * width + next.col();
                if (closed[n]) continue;
                double through = g[index] + cost.enter(next.col(), next.row());
                if (through >= g[n]) continue;      // no better than a way known already (or a wall)
                g[n] = through;
                cameFrom[n] = index;
                cameBy[n] = d;
                double h = guess(map, next, to);
                border.add(new Open(n, through + h, h));
            }
        }
        return new Path(List.of(), List.of(), Double.POSITIVE_INFINITY, visited);
    }

    /** The steps without walls, across and along, the short way round over the edges. Never too many. */
    static double guess(TileMap<?> map, Cell a, Cell b) {
        int dx = Math.abs(a.col() - b.col());
        int dy = Math.abs(a.row() - b.row());
        return Math.min(dx, map.width() - dx) + Math.min(dy, map.height() - dy);
    }
    // --8<-- [end:astar]

    /** Follows the way back from the goal to the start, then turns it round. */
    private static Path path(TileMap<?> map, int[] cameFrom, Direction[] cameBy, int start, int goal, double cost,
                             int visited) {
        List<Cell> cells = new ArrayList<>();
        List<Direction> steps = new ArrayList<>();
        for (int index = goal; index != start; index = cameFrom[index]) {
            cells.add(new Cell(index % map.width(), index / map.width()));
            steps.add(cameBy[index]);
        }
        cells.add(new Cell(start % map.width(), start / map.width()));
        Collections.reverse(cells);
        Collections.reverse(steps);
        return new Path(List.copyOf(cells), List.copyOf(steps), cost, visited);
    }
}
