package eu.amadeos.gameengine.terrain;

import java.util.Arrays;
import java.util.BitSet;

/**
 * Ground that can be blown away and heaped up: the earth of an artillery duel, or of any game where the
 * landscape changes. A grid of small cells, each empty ({@link #AIR}) or earth of some material, and a rule
 * like sand: earth with air under it falls, a cell at a time, until it rests. A shell carves a hole, the
 * earth above slides in, a tank on top comes down with it.
 *
 * <p>Positions are in game units, like everything else in the game; {@code cell} is how many units one
 * cell is wide, 0.5 for two cells per unit. The materials are numbers the game chooses, 1 to 127, e.g.
 * soil, grass and the dark rim of a crater; the ground only keeps them and moves them as the earth falls.
 * Columns run from left to right, rows from the top down, as on the screen.
 *
 * <p>The ground notes which columns changed, so the picture of it needs to be painted again only there
 * ({@link #takeChanges()}).
 *
 * <pre>
 * Ground ground = new Ground(640, 400, 0.5);
 * ground.fill(x -> 300 + 20 * Math.sin(x / 50), (x, depth) -> depth &lt; 3 ? GRASS : SOIL);
 * ground.carve(320, 290, 15);           // a crater
 * // in each step, until it returns false:
 * ground.settle(2);
 * </pre>
 */
// --8<-- [start:ground]
public final class Ground {
    /** No earth. */
    public static final byte AIR = 0;

    /** What a filled cell is made of, by its place: {@code depth} in units below the surface. */
    @FunctionalInterface
    public interface Layering {
        byte at(double x, double depth);
    }

    /** The columns that changed, {@code from} to {@code to}, both included. */
    public record Changes(int from, int to) {
    }

    private final int columns;
    private final int rows;
    private final double cell;
    /** Column after column: the cell at column c, row r is {@code cells[c * rows + r]}. */
    private final byte[] cells;
    /** Columns in which earth may hang over air. */
    private final BitSet loose;
    private int changedFrom = Integer.MAX_VALUE;
    private int changedTo = -1;

    /** Empty ground, {@code width} by {@code height} units, in cells {@code cell} units wide. */
    public Ground(double width, double height, double cell) {
        if (width <= 0 || height <= 0 || cell <= 0) throw new IllegalArgumentException("empty ground");
        this.cell = cell;
        this.columns = (int) Math.ceil(width / cell);
        this.rows = (int) Math.ceil(height / cell);
        this.cells = new byte[columns * rows];
        this.loose = new BitSet(columns);
    }

    private Ground(Ground other) {
        this.cell = other.cell;
        this.columns = other.columns;
        this.rows = other.rows;
        this.cells = other.cells.clone();
        this.loose = (BitSet) other.loose.clone();
        this.changedFrom = other.changedFrom;
        this.changedTo = other.changedTo;
    }

    /** An independent copy, e.g. to try a shot without touching the real ground. */
    public Ground copy() {
        return new Ground(this);
    }

    /** Fills every column from its {@code surface} (a height in units, larger is lower) down to the bottom. */
    public void fill(java.util.function.DoubleUnaryOperator surface, Layering layering) {
        Arrays.fill(cells, AIR);
        for (int c = 0; c < columns; c++) {
            double x = (c + 0.5) * cell;
            double top = surface.applyAsDouble(x);
            for (int r = 0; r < rows; r++) {
                double y = (r + 0.5) * cell;
                if (y >= top) cells[c * rows + r] = material(layering.at(x, y - top));
            }
        }
        loose.clear();
        changed(0, columns - 1);
    }
    // --8<-- [end:ground]

    private static byte material(byte value) {
        if (value <= AIR) throw new IllegalArgumentException("a material is 1 to 127, not " + value);
        return value;
    }

    // --8<-- [start:carve]
    /** Blows a round hole: every cell within {@code radius} of the centre becomes air. Returns how many were earth. */
    public int carve(double x, double y, double radius) {
        return round(x, y, radius, index -> {
            if (cells[index] == AIR) return false;
            cells[index] = AIR;
            return true;
        });
    }

    /** Heaps earth of a {@code material} into the air within {@code radius}; earth already there stays. Returns the new cells. */
    public int heap(double x, double y, double radius, byte material) {
        byte value = material(material);
        return round(x, y, radius, index -> {
            if (cells[index] != AIR) return false;
            cells[index] = value;
            return true;
        });
    }

    /** Turns the earth within {@code radius} into another material, e.g. scorched round a crater. Air stays air. */
    public int recolour(double x, double y, double radius, byte material) {
        byte value = material(material);
        return round(x, y, radius, index -> {
            if (cells[index] == AIR || cells[index] == value) return false;
            cells[index] = value;
            return true;
        });
    }

    private interface CellChange {
        boolean apply(int index);
    }

    private int round(double x, double y, double radius, CellChange change) {
        int c0 = Math.max(0, (int) Math.floor((x - radius) / cell));
        int c1 = Math.min(columns - 1, (int) Math.floor((x + radius) / cell));
        int r0 = Math.max(0, (int) Math.floor((y - radius) / cell));
        int r1 = Math.min(rows - 1, (int) Math.floor((y + radius) / cell));
        double rr = radius * radius;
        int count = 0;
        for (int c = c0; c <= c1; c++) {
            double dx = (c + 0.5) * cell - x;
            boolean touched = false;
            for (int r = r0; r <= r1; r++) {
                double dy = (r + 0.5) * cell - y;
                if (dx * dx + dy * dy > rr) continue;
                if (change.apply(c * rows + r)) {
                    count++;
                    touched = true;
                }
            }
            if (touched) {
                loose.set(c);
                changed(c, c);
            }
        }
        return count;
    }
    // --8<-- [end:carve]

    // --8<-- [start:settle]
    /**
     * Lets the earth fall: every run of earth with air under it moves down by one cell, {@code passes} times.
     * Called in each step, the earth slides at {@code passes} cells a step. Returns whether anything moved;
     * once it returns false, all earth rests.
     */
    public boolean settle(int passes) {
        boolean moved = false;
        for (int c = loose.nextSetBit(0); c >= 0; c = loose.nextSetBit(c + 1)) {
            boolean columnMoved = false;
            for (int pass = 0; pass < passes; pass++) {
                if (!fallOne(c)) break;
                columnMoved = true;
            }
            if (columnMoved) {
                moved = true;
                changed(c, c);
            } else {
                loose.clear(c);
            }
        }
        return moved;
    }

    /** One cell down for every cell that hangs over air in a column; from the bottom up, so a run moves together. */
    private boolean fallOne(int c) {
        int base = c * rows;
        boolean moved = false;
        for (int r = rows - 2; r >= 0; r--) {
            if (cells[base + r] != AIR && cells[base + r + 1] == AIR) {
                cells[base + r + 1] = cells[base + r];
                cells[base + r] = AIR;
                moved = true;
            }
        }
        return moved;
    }
    // --8<-- [end:settle]

    /** Whether some earth may still fall. */
    public boolean settling() {
        return !loose.isEmpty();
    }

    /** Whether there is earth at a point. Beside the ground is air; below it, earth. */
    public boolean solid(double x, double y) {
        if (x < 0 || y < 0) return false;
        int c = (int) (x / cell);
        if (c >= columns) return false;
        int r = (int) (y / cell);
        if (r >= rows) return true;
        return cells[c * rows + r] != AIR;
    }

    /** The material at a point; {@link #AIR} outside. */
    public byte material(double x, double y) {
        if (x < 0 || y < 0) return AIR;
        int c = (int) (x / cell), r = (int) (y / cell);
        if (c >= columns || r >= rows) return AIR;
        return cells[c * rows + r];
    }

    /** The material of a cell. */
    public byte cell(int column, int row) {
        return cells[column * rows + row];
    }

    /** The height of the highest earth in the column at {@code x}: the top of its first cell. {@link #height()} if there is none. */
    public double surface(double x) {
        int c = (int) Math.floor(x / cell);
        if (c < 0 || c >= columns) return height();
        return topRow(c) * cell;
    }

    /** The highest surface between {@code from} and {@code to}: where something that wide comes to rest. */
    public double surface(double from, double to) {
        int c0 = Math.max(0, (int) Math.floor(from / cell));
        int c1 = Math.min(columns - 1, (int) Math.floor(to / cell));
        int top = rows;
        for (int c = c0; c <= c1; c++) top = Math.min(top, topRow(c));
        return top * cell;
    }

    private int topRow(int c) {
        int base = c * rows;
        for (int r = 0; r < rows; r++) if (cells[base + r] != AIR) return r;
        return rows;
    }

    /** The columns that changed since the last call, or {@code null} if none did. */
    public Changes takeChanges() {
        if (changedTo < 0) return null;
        Changes changes = new Changes(changedFrom, changedTo);
        changedFrom = Integer.MAX_VALUE;
        changedTo = -1;
        return changes;
    }

    private void changed(int from, int to) {
        changedFrom = Math.min(changedFrom, from);
        changedTo = Math.max(changedTo, to);
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }

    /** How many units one cell is wide and high. */
    public double cellSize() {
        return cell;
    }

    public double width() {
        return columns * cell;
    }

    public double height() {
        return rows * cell;
    }
}
