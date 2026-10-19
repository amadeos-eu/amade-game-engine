package eu.amadeos.gameengine.collision;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * A field divided into square cells, to find the things near a place without looking at all things. Every
 * step, the game puts each thing into the cells its area touches; a question about one place then only
 * looks into the few cells around it.
 *
 * <p>Testing each of <i>n</i> things against each other costs <i>n</i>² / 2 tests: 40 insects and 2 shots
 * are 80, nothing; 2,000 shots, sparks and units are two million, in every step. With a grid, each thing
 * meets only the few in its neighbourhood, and the cost grows with <i>n</i>, not <i>n</i>². Choose the cell
 * a little larger than most things, so a thing touches one to four cells.
 *
 * <pre>
 * grid.clear();
 * for (Insect insect : insects) grid.add(insect, insect.x, insect.y, insect.radius);
 * grid.forEachNear(shot.x, shot.y, 0, insect -&gt; { if (hit(shot, insect)) ... });
 * </pre>
 *
 * <p>Things outside the field count to the cells at its edge, so a thing that flies in from above is
 * still found. The grid keeps its entries in arrays that only grow: after the first steps, filling and
 * asking create no objects. Use it on one thread only, the game loop's.
 *
 * @param <T> the things in the grid
 */
// --8<-- [start:grid]
public final class Grid<T> {
    private final double cellSize;
    private final int columns;
    private final int rows;
    /** Per cell, the first of its entries, or -1. */
    private final int[] firstEntry;
    /** Per entry, the next entry of the same cell, or -1, and the thing it stands for. */
    private int[] nextEntry = new int[64];
    private int[] entryItem = new int[64];
    private int entries;
    private Object[] items = new Object[32];
    private int size;
    /** Per thing, the number of the last question that visited it: no thing is visited twice. */
    private int[] visited = new int[32];
    private int question;

    public Grid(double width, double height, double cellSize) {
        if (width <= 0 || height <= 0 || cellSize <= 0) throw new IllegalArgumentException("empty grid");
        this.cellSize = cellSize;
        this.columns = (int) Math.ceil(width / cellSize);
        this.rows = (int) Math.ceil(height / cellSize);
        this.firstEntry = new int[columns * rows];
        clear();
    }

    /** Empties all cells; the game calls it at the start of each step, before it adds the things anew. */
    public void clear() {
        Arrays.fill(firstEntry, -1);
        Arrays.fill(items, 0, size, null);
        entries = 0;
        size = 0;
    }

    /** Puts a thing into every cell its area, a circle around {@code x}, {@code y}, touches. */
    public void add(T item, double x, double y, double radius) {
        if (size == items.length) {
            items = Arrays.copyOf(items, 2 * size);
            visited = Arrays.copyOf(visited, 2 * size);
        }
        int index = size++;
        items[index] = item;
        visited[index] = question;
        for (int row = row(y - radius); row <= row(y + radius); row++) {
            for (int column = column(x - radius); column <= column(x + radius); column++) {
                if (entries == nextEntry.length) {
                    nextEntry = Arrays.copyOf(nextEntry, 2 * entries);
                    entryItem = Arrays.copyOf(entryItem, 2 * entries);
                }
                int cell = row * columns + column;
                entryItem[entries] = index;
                nextEntry[entries] = firstEntry[cell];      // in front of the cell's other entries
                firstEntry[cell] = entries++;
            }
        }
    }

    /**
     * Visits every thing in the cells that a circle around {@code x}, {@code y} touches, each once: the
     * candidates. Whether a candidate really touches is the caller's test. Returns how many were visited.
     */
    @SuppressWarnings("unchecked")
    public int forEachNear(double x, double y, double radius, Consumer<? super T> visitor) {
        question++;
        int count = 0;
        for (int row = row(y - radius); row <= row(y + radius); row++) {
            for (int column = column(x - radius); column <= column(x + radius); column++) {
                for (int e = firstEntry[row * columns + column]; e >= 0; e = nextEntry[e]) {
                    int index = entryItem[e];
                    if (visited[index] == question) continue;    // already met in another cell
                    visited[index] = question;
                    count++;
                    visitor.accept((T) items[index]);
                }
            }
        }
        return count;
    }

    private int column(double x) {
        return Math.clamp((long) Math.floor(x / cellSize), 0, columns - 1);
    }

    private int row(double y) {
        return Math.clamp((long) Math.floor(y / cellSize), 0, rows - 1);
    }
    // --8<-- [end:grid]

    /** The things added since the last {@link #clear()}. */
    public int size() {
        return size;
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }
}
