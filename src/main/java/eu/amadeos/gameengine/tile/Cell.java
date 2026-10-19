package eu.amadeos.gameengine.tile;

/** One tile of a map, by its column and row; column 0 is on the left, row 0 at the top. */
public record Cell(int col, int row) {
    /** The neighbour in {@code direction}, without regard to the map's size. */
    public Cell next(Direction direction) {
        return new Cell(col + direction.dx, row + direction.dy);
    }
}
