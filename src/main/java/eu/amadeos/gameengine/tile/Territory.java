package eu.amadeos.gameengine.tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The land a side owns: circles around its storehouse and its towers, each claim a centre and a radius.
 * A tile is inside if any claim reaches it, so a tower built near the border pushes the border out, and a
 * tower that burns takes its land back unless another claim still holds it.
 *
 * <pre>
 * Territory land = new Territory(map.cols(), map.rows());
 * land.claim(1, store.col(), store.row(), 8);      // claim number 1, eight tiles around the store
 * if (land.inside(col, row)) build(...);
 * land.release(1);
 * </pre>
 *
 * <p>The tiles inside are counted once per claim that reaches them, so releasing a claim only touches its
 * own circle. {@link #border(int, int)} says whether a tile inside has a neighbour outside, which is where
 * a game puts its border posts. The map does not wrap: claims end at its edges.
 */
// --8<-- [start:territory]
public final class Territory {
    private record Claim(int col, int row, int radius) {
    }

    private final int cols;
    private final int rows;
    private final int[] claims;
    private final Map<Integer, Claim> byId = new TreeMap<>();
    private int version;

    public Territory(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        this.claims = new int[cols * rows];
    }

    /** Claims every tile within {@code radius} of {@code col}, {@code row}, under the number {@code id}. */
    public void claim(int id, int col, int row, int radius) {
        if (byId.containsKey(id)) throw new IllegalArgumentException("claim " + id + " exists");
        Claim claim = new Claim(col, row, radius);
        byId.put(id, claim);
        mark(claim, 1);
    }

    /** Gives back the land of claim {@code id}, where no other claim holds it. */
    public void release(int id) {
        Claim claim = byId.remove(id);
        if (claim == null) throw new IllegalArgumentException("no claim " + id);
        mark(claim, -1);
    }

    private void mark(Claim claim, int by) {
        int r = claim.radius();
        for (int row = Math.max(0, claim.row() - r); row <= Math.min(rows - 1, claim.row() + r); row++) {
            for (int col = Math.max(0, claim.col() - r); col <= Math.min(cols - 1, claim.col() + r); col++) {
                int dc = col - claim.col();
                int dr = row - claim.row();
                if (dc * dc + dr * dr <= r * r + r) claims[row * cols + col] += by;
            }
        }
        version++;
    }

    public boolean inside(int col, int row) {
        return col >= 0 && row >= 0 && col < cols && row < rows && claims[row * cols + col] > 0;
    }
    // --8<-- [end:territory]

    /** True if the tile is inside and one of its four neighbours is not. */
    public boolean border(int col, int row) {
        if (!inside(col, row)) return false;
        for (Direction d : Direction.values()) {
            if (!inside(col + d.dx, row + d.dy)) return true;
        }
        return false;
    }

    /** The tiles of the border, row by row, for the posts. */
    public List<Cell> border() {
        List<Cell> cells = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (border(col, row)) cells.add(new Cell(col, row));
            }
        }
        return cells;
    }

    /** How many tiles are inside. */
    public int size() {
        int size = 0;
        for (int count : claims) if (count > 0) size++;
        return size;
    }

    public boolean has(int id) {
        return byId.containsKey(id);
    }

    /** The claim numbers, in order. Read only. */
    public java.util.Set<Integer> ids() {
        return Collections.unmodifiableSet(byId.keySet());
    }

    /** Counts every change, for a picture of the border made again only when it moved. */
    public int version() {
        return version;
    }

    public int cols() {
        return cols;
    }

    public int rows() {
        return rows;
    }
}
