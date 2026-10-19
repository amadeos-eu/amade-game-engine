package eu.amadeos.gameengine.path;

import eu.amadeos.gameengine.tile.Cell;
import eu.amadeos.gameengine.tile.Direction;
import eu.amadeos.gameengine.tile.TileMap;

import java.util.Arrays;

/**
 * For every tile of a map, the number of steps to the nearest goal, found by <i>breadth-first search</i>:
 * the goals have distance 0, their open neighbours 1, the neighbours of those 2, and so on, a wave that
 * spreads through the corridors until it has filled them all. Walls stop it.
 *
 * <p>One search answers the question for every tile at once. That is what makes it the right tool for a
 * place many things want to reach, such as the house of the ghosts: search once, and from anywhere,
 * {@link #downhill} is the next step on a shortest way there. It is also the tool to find the
 * <i>nearest</i> of many things: search from where one stands, and compare the distances of the dots.
 *
 * <p>Steps over an edge of the map arrive on the far side, as in {@link TileMap#step}.
 */
// --8<-- [start:distances]
public final class DistanceMap {
    /** The distance of a tile no way leads to. */
    public static final int UNREACHABLE = Integer.MAX_VALUE;

    private final int width;
    private final int height;
    private final int[] distance;
    private int visited;

    private DistanceMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.distance = new int[width * height];
        Arrays.fill(distance, UNREACHABLE);
    }

    /** The distances to the nearest of {@code goals} over the tiles {@code open} lets one enter. */
    public static DistanceMap search(TileMap<?> map, Passable open, Cell... goals) {
        DistanceMap result = new DistanceMap(map.width(), map.height());
        int[] queue = new int[map.width() * map.height()];
        int head = 0;
        int tail = 0;
        for (Cell goal : goals) {
            int index = goal.row() * map.width() + goal.col();
            if (result.distance[index] == UNREACHABLE) {
                result.distance[index] = 0;
                queue[tail++] = index;
            }
        }
        while (head < tail) {
            int index = queue[head++];
            int col = index % map.width();
            int row = index / map.width();
            for (Direction d : Direction.values()) {
                Cell next = map.step(col, row, d);
                int n = next.row() * map.width() + next.col();
                if (result.distance[n] != UNREACHABLE || !open.test(next.col(), next.row())) continue;
                result.distance[n] = result.distance[index] + 1;
                queue[tail++] = n;
            }
        }
        result.visited = tail;
        return result;
    }
    // --8<-- [end:distances]

    /** Steps from {@code col}, {@code row} to the nearest goal; {@link #UNREACHABLE} if there is no way. */
    public int distance(int col, int row) {
        return distance[row * width + col];
    }

    public int distance(Cell cell) {
        return distance(cell.col(), cell.row());
    }

    // --8<-- [start:downhill]
    /**
     * The first step from {@code col}, {@code row} on a shortest way to a goal: towards the neighbour with
     * the smallest distance. {@code null} at a goal, or where no way leads. Of equally good steps, the first
     * in the order of {@link Direction} wins.
     */
    public Direction downhill(int col, int row) {
        int here = distance(col, row);
        Direction best = null;
        int bestDistance = here;
        for (Direction d : Direction.values()) {
            int next = distance(Math.floorMod(col + d.dx, width), Math.floorMod(row + d.dy, height));
            if (next < bestDistance) {
                best = d;
                bestDistance = next;
            }
        }
        return best;
    }
    // --8<-- [end:downhill]

    /** How many tiles the search reached, the goals included: what it cost. */
    public int visited() {
        return visited;
    }
}
