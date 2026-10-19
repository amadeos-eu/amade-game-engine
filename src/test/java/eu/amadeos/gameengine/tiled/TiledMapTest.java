package eu.amadeos.gameengine.tiled;

import eu.amadeos.gameengine.data.TextData.DataException;
import eu.amadeos.gameengine.tile.TileMap;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TiledMapTest {
    enum Tile { AIR, GROUND, COIN, PLATFORM }

    // --8<-- [start:test-read]
    @Test
    void aMapIsReadWithItsLayersTilesetsAndProperties() {
        TiledMap map = TiledMap.load(TiledMapTest.class, "small.tmx");
        assertEquals(4, map.width());
        assertEquals(3, map.height());
        assertEquals(16, map.tileWidth());
        assertEquals("Test Meadow", map.properties().string("name", ""));
        assertEquals(90, map.properties().integer("time", 0));

        TiledMap.TileLayer ground = map.layer("ground");
        assertEquals(2, ground.gid(2, 0));
        assertEquals("solid", map.type(ground.gid(0, 2)));
        assertEquals("coin", map.type(ground.gid(2, 0)));
        assertEquals("", map.type(0));
        // 2147483649 is tile 1, flipped horizontally: the top bit.
        assertEquals(1, ground.gid(2, 2));
        assertEquals(TiledMap.FLIPPED_HORIZONTALLY, ground.flips(2, 2));

        TiledMap.TileInfo coin = map.info(2).orElseThrow();
        assertEquals(10, coin.properties().integer("points", 0));
        assertEquals(List.of(new TiledMap.Frame(1, 0.15), new TiledMap.Frame(3, 0.15)), coin.animation());
    }

    @Test
    void aLayerBecomesATileMapOfTheGamesOwnTiles() {
        TiledMap map = TiledMap.load(TiledMapTest.class, "small.tmx");
        TileMap<Tile> tiles = map.tiles("ground", gid -> switch (map.type(gid)) {
            case "solid" -> Tile.GROUND;
            case "coin" -> Tile.COIN;
            default -> Tile.AIR;
        });
        assertEquals(Tile.COIN, tiles.get(2, 0));
        assertEquals(4, tiles.count(t -> t == Tile.GROUND));
    }
    // --8<-- [end:test-read]

    // --8<-- [start:test-objects]
    @Test
    void objectsHaveAClassABoxAndProperties() {
        TiledMap map = TiledMap.load(TiledMapTest.class, "small.tmx");
        List<TiledObject> things = map.objects("things");
        assertEquals(3, things.size(), "the coin tile's collision box is no object of the map");
        TiledObject start = things.get(0);
        assertEquals("start", start.type());
        assertEquals(16, start.centerX(), 1e-9);
        TiledObject walker = things.get(1);
        assertEquals("walker", walker.type(), "class, as Tiled 1.9 wrote it");
        assertEquals("left", walker.properties().string("direction", "right"));
        assertEquals("two\nlines", walker.properties().string("note", ""));
        TiledObject coin = things.get(2);
        assertEquals(2, coin.gid());
        assertEquals(16, coin.y(), 1e-9, "a tile object stands on its y: turned into the top");
    }
    // --8<-- [end:test-objects]

    @Test
    void compressedLayersAndTilesetsInTheirOwnFilesAreRead() {
        TiledMap map = TiledMap.load(TiledMapTest.class, "packed.tmx");
        TiledMap.TileLayer ground = map.layer("ground");
        assertEquals(3, ground.gid(2, 0));
        assertEquals("platform", map.type(3));
        assertEquals("solid", map.type(ground.gid(3, 2)));
        TiledMap.Tileset blocks = map.tilesets().getFirst();
        assertEquals("blocks", blocks.name());
        assertEquals("sets/blocks.png", blocks.image(), "relative to the map, not to the tileset");
        assertEquals(2, blocks.rows());
    }

    // --8<-- [start:test-errors]
    @Test
    void whatCannotBeReadIsAnErrorNamingTheFileAndLine() {
        var infinite = assertThrows(DataException.class, () -> parse("""
                <?xml version="1.0" encoding="UTF-8"?>
                <map orientation="orthogonal" width="4" height="3" tilewidth="16" tileheight="16" infinite="1">
                </map>
                """));
        assertEquals("level.tmx:2: infinite maps cannot be read: untick Infinite under Map → Map Properties",
                infinite.getMessage());
        var short_ = assertThrows(DataException.class, () -> parse("""
                <?xml version="1.0" encoding="UTF-8"?>
                <map orientation="orthogonal" width="2" height="2" tilewidth="16" tileheight="16">
                 <layer name="ground" width="2" height="2">
                  <data encoding="csv">1,1,1</data>
                 </layer>
                </map>
                """));
        assertEquals("level.tmx:4: 3 tiles in the layer, the map has 4", short_.getMessage());
        var missing = assertThrows(DataException.class, () -> TiledMap.load(TiledMapTest.class, "small.tmx").layer("back"));
        assertEquals("small.tmx: no tile layer \"back\"", missing.getMessage());
    }
    // --8<-- [end:test-errors]

    @Test
    void aMissingTilesetFileIsNamed() {
        var missing = assertThrows(DataException.class, () -> parse("""
                <?xml version="1.0" encoding="UTF-8"?>
                <map orientation="orthogonal" width="2" height="2" tilewidth="16" tileheight="16">
                 <tileset firstgid="1" source="nowhere.tsx"/>
                </map>
                """));
        assertTrue(missing.getMessage().contains("nowhere.tsx is missing"), missing.getMessage());
    }

    private static TiledMap parse(String xml) {
        return TiledMap.parse("level.tmx", new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), file -> null);
    }
}
