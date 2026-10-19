package eu.amadeos.gameengine.tile;

import eu.amadeos.gameengine.data.TextData;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TileMapTest {
    enum Tile { WALL, FLOOR, DOT }

    static final Map<Character, Tile> LEGEND = Map.of('X', Tile.WALL, '_', Tile.FLOOR, '.', Tile.DOT);

    static TileMap<Tile> map(String text) {
        return TileMap.read(TextData.parse("test.txt", new StringReader(text)), LEGEND);
    }

    // --8<-- [start:test-read]
    @Test
    void aMapIsReadFromRowsOfLetters() {
        TileMap<Tile> map = map("""
                row XXXXX
                row X._.X
                row XXXXX
                """);
        assertEquals(5, map.width());
        assertEquals(3, map.height());
        assertEquals(Tile.DOT, map.get(1, 1));
        assertEquals(Tile.FLOOR, map.get(2, 1));
        assertEquals(2, map.count(t -> t == Tile.DOT));
        assertEquals(List.of(new Cell(1, 1), new Cell(3, 1)), map.cells(t -> t == Tile.DOT));
    }

    @Test
    void aWrongLetterOrALengthIsAnErrorNamingTheLine() {
        var unknown = assertThrows(TextData.DataException.class, () -> map("""
                row XXXXX
                # a comment line still counts
                row X.Q.X
                """));
        assertEquals("test.txt:3: column 2: 'Q' is not a tile", unknown.getMessage());
        var length = assertThrows(TextData.DataException.class, () -> map("row XXXXX\nrow XXXX\n"));
        assertEquals("test.txt:2: the row has 4 tiles, the first has 5", length.getMessage());
    }
    // --8<-- [end:test-read]

    @Test
    void aStepOverAnEdgeArrivesOnTheFarSide() {
        TileMap<Tile> map = new TileMap<>(7, 4, Tile.FLOOR);
        assertEquals(new Cell(6, 2), map.step(0, 2, Direction.LEFT));
        assertEquals(new Cell(0, 2), map.step(6, 2, Direction.RIGHT));
        assertEquals(new Cell(3, 3), map.step(3, 0, Direction.UP));
        assertEquals(new Cell(4, 2), map.step(3, 2, Direction.RIGHT));
        assertThrows(IndexOutOfBoundsException.class, () -> map.get(7, 0));
    }

    // --8<-- [start:test-mask]
    @Test
    void theMaskSaysWhichNeighboursAreWalls() {
        TileMap<Tile> map = map("""
                row XXX_
                row X.X_
                row X___
                """);
        // Up 1, left 2, down 4, right 8.
        assertEquals(8 | 4, map.mask(0, 0, t -> t == Tile.WALL, false), "a corner: right and down");
        assertEquals(2 | 4, map.mask(2, 0, t -> t == Tile.WALL, false), "a corner: left and down");
        assertEquals(1, map.mask(2, 1, t -> t == Tile.WALL, false), "the end of a wall: only up");
        assertEquals(1 | 4, map.mask(0, 1, t -> t == Tile.WALL, false), "a straight piece, up and down");
        assertEquals(1 | 2 | 4 | 8, map.mask(0, 0, t -> t == Tile.WALL, true), "outside counts as wall here");
    }
    // --8<-- [end:test-mask]

    @Test
    void aCopyChangesAlone() {
        TileMap<Tile> map = map("row X..X\n");
        TileMap<Tile> copy = map.copy();
        copy.set(1, 0, Tile.FLOOR);
        assertEquals(Tile.DOT, map.get(1, 0));
        assertEquals(Tile.FLOOR, copy.get(1, 0));
    }
}
