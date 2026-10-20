package eu.amadeos.gameengine.path;

import eu.amadeos.gameengine.math.Vector;
import eu.amadeos.gameengine.tile.Cell;

import java.util.Arrays;

/**
 * For every tile of a map, which way leads to the goal: a field of arrows that many units can follow at
 * once. One search from the goal outwards answers the question for every tile; each unit then only looks up
 * the arrow of the tile it stands on. A hundred units cost one search, not a hundred, and a new wall costs
 * one new search, however many units must find a new way round it.
 *
 * <p>Units walk in eight directions. A step to a side costs 10, a diagonal step 14 (√2 × 10, rounded), so
 * the distances are those of a real walk and not of a chess king. A diagonal step is allowed only where
 * both tiles beside it are open: no unit squeezes between two walls that touch at a corner.
 *
 * <pre>
 * FlowField field = FlowField.toward(map.width(), map.height(), (col, row) -&gt; open(col, row), castle);
 * Vector heading = field.heading(unit.x() / TILE, unit.y() / TILE);   // in tiles, length 1
 * </pre>
 *
 * <p>Unlike {@link DistanceMap}, the field has edges: nothing leads over the side of the map.
 */
// --8<-- [start:flowfield]
public final class FlowField {
    /** The distance of a tile no way leads from. */
    public static final int UNREACHABLE = Integer.MAX_VALUE;
    /** What a step to a side costs, and a diagonal one. */
    public static final int STRAIGHT = 10;
    public static final int DIAGONAL = 14;
    /** The eight neighbours: first the four sides (up, left, down, right), then the corners. */
    private static final int[] DX = {0, -1, 0, 1, -1, -1, 1, 1};
    private static final int[] DY = {-1, 0, 1, 0, -1, 1, 1, -1};
    private static final byte NONE = -1;

    private final int width;
    private final int height;
    private final int[] distance;
    /** Per tile, the neighbour to go to (an index into DX and DY), or NONE at a goal and where no way leads. */
    private final byte[] next;
    private int visited;

    private FlowField(int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("empty field");
        this.width = width;
        this.height = height;
        this.distance = new int[width * height];
        this.next = new byte[width * height];
        Arrays.fill(distance, UNREACHABLE);
        Arrays.fill(next, NONE);
    }

    /** The way to the nearest of {@code goals} from every tile that {@code open} lets one enter. */
    public static FlowField toward(int width, int height, Passable open, Cell... goals) {
        FlowField field = new FlowField(width, height);
        field.search(open, goals);
        return field;
    }
    // --8<-- [end:flowfield]

    // --8<-- [start:search]
    /**
     * Dijkstra's search: the tile nearest to a goal that has not been settled yet is settled next, and its
     * neighbours learn a shorter distance through it. Breadth-first search (chapter 25) is the same with
     * equal steps; here diagonal steps cost more, so the queue must be sorted by distance.
     */
    private void search(Passable open, Cell... goals) {
        Heap queue = new Heap(width * height);
        for (Cell goal : goals) {
            if (!contains(goal.col(), goal.row())) throw new IllegalArgumentException(goal + " is outside the field");
            int index = goal.row() * width + goal.col();
            distance[index] = 0;
            queue.push(0, index);
        }
        while (!queue.isEmpty()) {
            long entry = queue.pop();
            int index = (int) entry;
            int here = (int) (entry >>> 32);
            if (here > distance[index]) continue;     // settled already by a shorter way
            visited++;
            int col = index % width;
            int row = index / width;
            for (int d = 0; d < 8; d++) {
                int c = col + DX[d];
                int r = row + DY[d];
                if (!step(open, col, row, d)) continue;
                int n = r * width + c;
                int through = here + (d < 4 ? STRAIGHT : DIAGONAL);
                if (through < distance[n]) {
                    distance[n] = through;
                    // The way from n leads back the way the search came: to the opposite neighbour.
                    next[n] = (byte) opposite(d);
                    queue.push(through, n);
                }
            }
        }
    }

    /** Whether a unit may step from a tile to its neighbour {@code d}: open, and no corner cut. */
    private boolean step(Passable open, int col, int row, int d) {
        int c = col + DX[d];
        int r = row + DY[d];
        if (!contains(c, r) || !open.test(c, r)) return false;
        if (d < 4) return true;
        return open.test(c, row) && open.test(col, r);
    }
    // --8<-- [end:search]

    private static int opposite(int d) {
        return switch (d) {
            case 0 -> 2;
            case 1 -> 3;
            case 2 -> 0;
            case 3 -> 1;
            case 4 -> 6;    // up left  -> down right
            case 5 -> 7;    // down left -> up right
            case 6 -> 4;
            default -> 5;
        };
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean contains(int col, int row) {
        return col >= 0 && col < width && row >= 0 && row < height;
    }

    /** The cost of the way from {@code col}, {@code row} to the goal, 10 per tile; {@link #UNREACHABLE} if none. */
    public int distance(int col, int row) {
        return distance[row * width + col];
    }

    /** True if a way leads from the tile to a goal; true at a goal. */
    public boolean reaches(int col, int row) {
        return contains(col, row) && distance(col, row) != UNREACHABLE;
    }

    public boolean reaches(Cell cell) {
        return reaches(cell.col(), cell.row());
    }

    /** True at a goal itself. */
    public boolean isGoal(int col, int row) {
        return distance(col, row) == 0;
    }

    /** The tile to go to next on a shortest way; {@code null} at a goal and where no way leads. */
    public Cell next(int col, int row) {
        byte d = next[row * width + col];
        return d == NONE ? null : new Cell(col + DX[d], row + DY[d]);
    }

    // --8<-- [start:heading]
    /**
     * Where a unit at {@code x}, {@code y} (in tiles: 2.5 is the middle of column 2) should walk: towards
     * the middle of the next tile, length 1. On a goal it walks to the goal's middle, and is there when the
     * heading becomes the zero vector; where no way leads, it stays.
     *
     * <p>A unit that walks this way never touches a closed tile. It is somewhere in its tile and walks
     * straight to the middle of a neighbour; the two tiles together are a rectangle, or with a diagonal step
     * and both tiles beside it open, a square of four, and a straight line between two points of a
     * rectangle stays inside it. That holds for any point of the next tile, so a unit may aim a little to
     * the side of the middle, to walk beside the others instead of behind them.
     */
    public Vector heading(double x, double y) {
        int col = (int) Math.floor(x);
        int row = (int) Math.floor(y);
        if (!reaches(col, row)) return Vector.ZERO;
        Cell to = next(col, row);
        if (to == null) to = new Cell(col, row);
        Vector way = new Vector(to.col() + 0.5 - x, to.row() + 0.5 - y);
        return way.lengthSquared() < 1e-12 ? Vector.ZERO : way.normalized();
    }
    // --8<-- [end:heading]

    /** How many tiles the search settled: what it cost. */
    public int visited() {
        return visited;
    }

    /** A queue of tiles sorted by distance, as a binary heap of longs: the distance above, the tile below. */
    private static final class Heap {
        private long[] entries;
        private int size;

        Heap(int capacity) {
            entries = new long[Math.max(16, capacity)];
        }

        boolean isEmpty() {
            return size == 0;
        }

        void push(int distance, int index) {
            if (size == entries.length) entries = Arrays.copyOf(entries, size * 2);
            long entry = ((long) distance << 32) | index;
            int i = size++;
            while (i > 0) {
                int parent = (i - 1) / 2;
                if (entries[parent] <= entry) break;
                entries[i] = entries[parent];
                i = parent;
            }
            entries[i] = entry;
        }

        long pop() {
            long top = entries[0];
            long last = entries[--size];
            int i = 0;
            while (true) {
                int child = 2 * i + 1;
                if (child >= size) break;
                if (child + 1 < size && entries[child + 1] < entries[child]) child++;
                if (entries[child] >= last) break;
                entries[i] = entries[child];
                i = child;
            }
            entries[i] = last;
            return top;
        }
    }
}
