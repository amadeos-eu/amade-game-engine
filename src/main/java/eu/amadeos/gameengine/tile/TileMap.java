package eu.amadeos.gameengine.tile;

import eu.amadeos.gameengine.data.TextData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A field of tiles: columns and rows of squares, each holding one value, usually an enum such as
 * {@code WALL} or {@code DOT}. The rules of a maze game, a puzzle or a strategy game live on such a map,
 * in whole cells; only the drawing knows how many pixels a tile has.
 *
 * <p>A map is written as text, a letter per tile and a line per row, so it can be drawn in an editor:
 *
 * <pre>
 * row XXXXXXX
 * row X.....X
 * row X.XXX.X
 * </pre>
 *
 * <p>The map has no edges of its own: a step over the right edge arrives at the left, a step over the
 * bottom at the top ({@link #step}), as on the field of Rocks. A maze with walls all round never notices; a
 * row without a wall at its ends becomes a tunnel from one side to the other.
 *
 * @param <T> what a tile holds
 */
// --8<-- [start:tilemap]
public final class TileMap<T> {
    private final int width;
    private final int height;
    private final Object[] tiles;

    public TileMap(int width, int height, T fill) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("empty map");
        this.width = width;
        this.height = height;
        this.tiles = new Object[width * height];
        Arrays.fill(tiles, fill);
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

    @SuppressWarnings("unchecked")
    public T get(int col, int row) {
        return (T) tiles[index(col, row)];
    }

    public void set(int col, int row, T tile) {
        tiles[index(col, row)] = tile;
    }

    private int index(int col, int row) {
        if (!contains(col, row)) {
            throw new IndexOutOfBoundsException("(" + col + ", " + row + ") is outside " + width + " × " + height);
        }
        return row * width + col;
    }

    /** The cell one step from {@code col}, {@code row}: over an edge, it is on the far side. */
    public Cell step(int col, int row, Direction direction) {
        return new Cell(Math.floorMod(col + direction.dx, width), Math.floorMod(row + direction.dy, height));
    }
    // --8<-- [end:tilemap]

    public T get(Cell cell) {
        return get(cell.col(), cell.row());
    }

    /** How many tiles hold something that {@code test} accepts, e.g. the dots still to eat. */
    public int count(Predicate<? super T> test) {
        int count = 0;
        for (Object tile : tiles) {
            @SuppressWarnings("unchecked") T t = (T) tile;
            if (test.test(t)) count++;
        }
        return count;
    }

    /** All cells whose tile {@code test} accepts, row by row. */
    public List<Cell> cells(Predicate<? super T> test) {
        List<Cell> cells = new ArrayList<>();
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (test.test(get(col, row))) cells.add(new Cell(col, row));
            }
        }
        return cells;
    }

    /** An independent copy, e.g. a fresh maze for each level, all its dots in place. */
    public TileMap<T> copy() {
        TileMap<T> copy = new TileMap<>(width, height, null);
        System.arraycopy(tiles, 0, copy.tiles, 0, tiles.length);
        return copy;
    }

    // --8<-- [start:mask]
    /**
     * Which of the four neighbours {@code same} accepts, as four bits: 1 up, 2 left, 4 down, 8 right (the
     * bit of a {@link Direction} is {@code 1 << ordinal}). Neighbours outside the map count as {@code outside}.
     * Sixteen values, one per shape a wall piece can take: the classic way to choose the picture of a tile
     * from its neighbours, called <i>autotiling</i>.
     */
    public int mask(int col, int row, Predicate<? super T> same, boolean outside) {
        int mask = 0;
        for (Direction d : Direction.values()) {
            int c = col + d.dx;
            int r = row + d.dy;
            boolean match = contains(c, r) ? same.test(get(c, r)) : outside;
            if (match) mask |= 1 << d.ordinal();
        }
        return mask;
    }
    // --8<-- [end:mask]

    // --8<-- [start:read]
    /**
     * Reads a map from lines such as {@code row X..X}: the second word of each line is a row, one letter per
     * tile, and {@code legend} says what each letter stands for. All rows must be equally long. An error
     * names the line and the column, as {@code maze.txt:7: column 12: 'Q' is not a tile}.
     */
    public static <T> TileMap<T> read(List<TextData.Line> rows, Map<Character, T> legend) {
        if (rows.isEmpty()) throw new IllegalArgumentException("a map needs at least one row");
        int width = rows.getFirst().word(1).length();
        TileMap<T> map = new TileMap<>(width, rows.size(), null);
        for (int row = 0; row < rows.size(); row++) {
            TextData.Line line = rows.get(row);
            String text = line.word(1);
            if (line.size() > 2) throw line.error("a row is one word, without spaces");
            if (text.length() != width) {
                throw line.error("the row has " + text.length() + " tiles, the first has " + width);
            }
            for (int col = 0; col < width; col++) {
                T tile = legend.get(text.charAt(col));
                if (tile == null) throw line.error("column " + col + ": '" + text.charAt(col) + "' is not a tile");
                map.set(col, row, tile);
            }
        }
        return map;
    }
    // --8<-- [end:read]
}
