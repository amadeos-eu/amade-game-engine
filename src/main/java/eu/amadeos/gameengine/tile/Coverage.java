package eu.amadeos.gameengine.tile;

import java.util.Arrays;

/**
 * How well the tiles of a map are served by places that reach a circle around them: schools, fire stations,
 * water towers, parks. Each place adds its circle; a tile then knows how many places reach it and how close
 * the nearest one is, as a strength from 1 on the place itself down to almost 0 at the edge of its reach.
 *
 * <p>Places are built and torn down, and the strength is not something that can be taken back out of a
 * sum, so the coverage is not kept up to date place by place: the game clears it and adds every place
 * again when something changed, or once a month. For a map of 64 × 64 tiles and a few dozen places that is
 * nothing.
 *
 * <pre>
 * Coverage schools = new Coverage(map.cols(), map.rows());
 * schools.clear();
 * for (Building s : schoolsBuilt) schools.add(s.col(), s.row(), 12);
 * if (schools.count(col, row) == 0) ... ;            // no school within reach
 * landValue += 20 * schools.strength(col, row);       // the nearer, the better
 * </pre>
 *
 * <p>The circles are round: a tile is reached if its middle is within {@code radius} tiles of the place's
 * middle. The map does not wrap.
 */
// --8<-- [start:coverage]
public final class Coverage {
    private final int cols;
    private final int rows;
    private final int[] counts;
    private final float[] strengths;

    public Coverage(int cols, int rows) {
        if (cols <= 0 || rows <= 0) throw new IllegalArgumentException("empty map");
        this.cols = cols;
        this.rows = rows;
        this.counts = new int[cols * rows];
        this.strengths = new float[cols * rows];
    }

    /** Forgets every place. */
    public void clear() {
        Arrays.fill(counts, 0);
        Arrays.fill(strengths, 0);
    }

    /** A place on tile {@code col}, {@code row} that reaches {@code radius} tiles around. */
    public void add(int col, int row, double radius) {
        add(col + 0.5, row + 0.5, radius);
    }

    /**
     * A place whose middle is at {@code col}, {@code row} in tiles, which need not be the middle of a
     * tile: a building of two tiles by two has its middle on the corner between them.
     */
    public void add(double col, double row, double radius) {
        int r = (int) Math.ceil(radius);
        int c0 = Math.max(0, (int) Math.floor(col - r));
        int c1 = Math.min(cols - 1, (int) Math.ceil(col + r));
        int r0 = Math.max(0, (int) Math.floor(row - r));
        int r1 = Math.min(rows - 1, (int) Math.ceil(row + r));
        for (int y = r0; y <= r1; y++) {
            for (int x = c0; x <= c1; x++) {
                double distance = Math.hypot(x + 0.5 - col, y + 0.5 - row);
                if (distance > radius) continue;
                int i = y * cols + x;
                counts[i]++;
                float strength = (float) (1 - distance / (radius + 1));
                if (strength > strengths[i]) strengths[i] = strength;
            }
        }
    }

    /** How many places reach a tile; 0 off the map. */
    public int count(int col, int row) {
        return contains(col, row) ? counts[row * cols + col] : 0;
    }

    /** True if any place reaches a tile. */
    public boolean covered(int col, int row) {
        return count(col, row) > 0;
    }

    /** How close the nearest place is: 1 on it, falling towards 0 at the edge of its reach, 0 out of reach. */
    public double strength(int col, int row) {
        return contains(col, row) ? strengths[row * cols + col] : 0;
    }
    // --8<-- [end:coverage]

    public boolean contains(int col, int row) {
        return col >= 0 && row >= 0 && col < cols && row < rows;
    }

    public int cols() {
        return cols;
    }

    public int rows() {
        return rows;
    }
}
