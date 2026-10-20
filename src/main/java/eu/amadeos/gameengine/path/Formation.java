package eu.amadeos.gameengine.path;

import eu.amadeos.gameengine.tile.Cell;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;

/**
 * Where a group of units stands when it arrives. Five tanks sent to one tile cannot all stand on it: each
 * needs a place of its own, a <b>slot</b>, near the tile the player clicked, and each must take the slot
 * that suits it, or they cross each other's way at the end and jostle.
 *
 * <p>The slots are the open tiles nearest to the target, counted in steps from it, so they never lie
 * behind a wall: a slot is always reachable from the target. The assignment takes the closest pair of a
 * unit and a slot first, then the closest of what is left, and so on. That is not the best assignment
 * there is (that would be the Hungarian method), but it is close for groups of a dozen, it is easy to
 * follow, and the paths it gives seldom cross: a unit far ahead does not take the slot at the back.
 *
 * <pre>
 * List&lt;Cell&gt; slots = Formation.slots(target, units.size(), map.width(), map.height(), this::open);
 * int[] slotOf = Formation.assign(xs, ys, slots);     // positions in tiles
 * </pre>
 */
public final class Formation {
    private static final int[] DX = {0, -1, 0, 1, -1, -1, 1, 1};
    private static final int[] DY = {-1, 0, 1, 0, -1, 1, 1, -1};

    private Formation() {
    }

    // --8<-- [start:slots]
    /**
     * Up to {@code count} open tiles nearest to {@code target}, the target first if it is open, then
     * outwards by steps in eight directions. Fewer if not so many are reachable. A closed target is
     * replaced by the nearest open tile, so a click on a rock still sends the group next to it.
     */
    public static List<Cell> slots(Cell target, int count, int width, int height, Passable open) {
        List<Cell> slots = new ArrayList<>();
        if (count <= 0 || !inside(target.col(), target.row(), width, height)) return slots;
        Cell start = open.test(target.col(), target.row()) ? target : nearestOpen(target, width, height, open);
        if (start == null) return slots;
        boolean[] seen = new boolean[width * height];
        ArrayDeque<Cell> queue = new ArrayDeque<>();
        queue.add(start);
        seen[start.row() * width + start.col()] = true;
        while (!queue.isEmpty() && slots.size() < count) {
            Cell cell = queue.poll();
            slots.add(cell);
            for (int d = 0; d < 8; d++) {
                int c = cell.col() + DX[d];
                int r = cell.row() + DY[d];
                if (!inside(c, r, width, height) || seen[r * width + c] || !open.test(c, r)) continue;
                // No corner cut: a slot must be walkable from the one before.
                if (d >= 4 && (!open.test(c, cell.row()) || !open.test(cell.col(), r))) continue;
                seen[r * width + c] = true;
                queue.add(new Cell(c, r));
            }
        }
        return slots;
    }
    // --8<-- [end:slots]

    /** The open tile nearest to a closed one, as the crow flies, ring by ring; {@code null} if none. */
    static Cell nearestOpen(Cell target, int width, int height, Passable open) {
        int most = Math.max(width, height);
        for (int ring = 1; ring <= most; ring++) {
            Cell best = null;
            double bestDistance = Double.MAX_VALUE;
            for (int dr = -ring; dr <= ring; dr++) {
                for (int dc = -ring; dc <= ring; dc++) {
                    if (Math.max(Math.abs(dc), Math.abs(dr)) != ring) continue;
                    int c = target.col() + dc;
                    int r = target.row() + dr;
                    if (!inside(c, r, width, height) || !open.test(c, r)) continue;
                    double distance = Math.hypot(dc, dr);
                    if (distance < bestDistance) {
                        best = new Cell(c, r);
                        bestDistance = distance;
                    }
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    // --8<-- [start:assign]
    /**
     * Which slot each unit takes: {@code result[unit]} is an index into {@code slots}, or -1 for a unit
     * left without one when there are fewer slots than units. Positions are in tiles, 2.5 being the middle
     * of column 2. The closest pair of unit and slot is taken first, then the closest of the rest.
     */
    public static int[] assign(double[] xs, double[] ys, List<Cell> slots) {
        int units = xs.length;
        int[] slotOf = new int[units];
        Arrays.fill(slotOf, -1);
        int pairs = units * slots.size();
        double[] distance = new double[pairs];
        for (int u = 0; u < units; u++) {
            for (int s = 0; s < slots.size(); s++) {
                int pair = u * slots.size() + s;
                distance[pair] = Math.hypot(slots.get(s).col() + 0.5 - xs[u], slots.get(s).row() + 0.5 - ys[u]);
            }
        }
        // Sort the pairs by distance; ties keep the order of the units, then of the slots.
        Integer[] sorted = new Integer[pairs];
        for (int i = 0; i < pairs; i++) sorted[i] = i;
        Arrays.sort(sorted, (a, b) -> {
            int nearer = Double.compare(distance[a], distance[b]);
            return nearer != 0 ? nearer : Integer.compare(a, b);
        });
        boolean[] taken = new boolean[slots.size()];
        int left = Math.min(units, slots.size());
        for (int i = 0; i < pairs && left > 0; i++) {
            int u = sorted[i] / slots.size();
            int s = sorted[i] % slots.size();
            if (slotOf[u] >= 0 || taken[s]) continue;
            slotOf[u] = s;
            taken[s] = true;
            left--;
        }
        return slotOf;
    }
    // --8<-- [end:assign]

    // --8<-- [start:line]
    /**
     * Whether a unit can walk straight from one point to another, positions in tiles: every tile the line
     * passes through is open, and where it passes a corner, both tiles beside the corner are. Walked
     * through tile by tile, as a ray is traced through a grid (Amanatides and Woo, 1987).
     */
    public static boolean clearLine(double x0, double y0, double x1, double y1, int width, int height, Passable open) {
        int col = (int) Math.floor(x0);
        int row = (int) Math.floor(y0);
        int endCol = (int) Math.floor(x1);
        int endRow = (int) Math.floor(y1);
        if (!inside(col, row, width, height) || !open.test(col, row)) return false;
        double dx = x1 - x0;
        double dy = y1 - y0;
        int stepCol = dx > 0 ? 1 : -1;
        int stepRow = dy > 0 ? 1 : -1;
        // How far along the line, 0 to 1, the next vertical and the next horizontal tile edge lie.
        double tCol = dx == 0 ? Double.POSITIVE_INFINITY : ((dx > 0 ? col + 1 : col) - x0) / dx;
        double tRow = dy == 0 ? Double.POSITIVE_INFINITY : ((dy > 0 ? row + 1 : row) - y0) / dy;
        double deltaCol = dx == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dx);
        double deltaRow = dy == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dy);
        int guard = Math.abs(endCol - col) + Math.abs(endRow - row) + 2;
        while ((col != endCol || row != endRow) && guard-- > 0) {
            if (Math.abs(tCol - tRow) < 1e-12) {
                // Through a corner: the two tiles beside it must be open as well.
                if (!passable(col + stepCol, row, width, height, open) || !passable(col, row + stepRow, width, height, open)) {
                    return false;
                }
                col += stepCol;
                row += stepRow;
                tCol += deltaCol;
                tRow += deltaRow;
            } else if (tCol < tRow) {
                col += stepCol;
                tCol += deltaCol;
            } else {
                row += stepRow;
                tRow += deltaRow;
            }
            if (!passable(col, row, width, height, open)) return false;
        }
        return true;
    }
    // --8<-- [end:line]

    private static boolean passable(int col, int row, int width, int height, Passable open) {
        return inside(col, row, width, height) && open.test(col, row);
    }

    private static boolean inside(int col, int row, int width, int height) {
        return col >= 0 && row >= 0 && col < width && row < height;
    }
}
