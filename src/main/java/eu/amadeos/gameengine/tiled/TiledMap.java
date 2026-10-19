package eu.amadeos.gameengine.tiled;

import eu.amadeos.gameengine.data.TextData.DataException;
import eu.amadeos.gameengine.tile.TileMap;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * A level drawn in <a href="https://www.mapeditor.org">Tiled</a>, the free map editor most 2D games are
 * made with, read from its own file format, TMX. A text file in a game's own format (chapter 25's maze)
 * is fine for a maze of 28 × 31; a level of 200 × 30 tiles in three layers, with enemies, coins and doors,
 * wants to be <i>painted</i>, with the pictures of the tiles under the mouse. Tiled does that, and saves:
 *
 * <ul>
 *   <li><b>tile layers</b>: a grid of numbers per layer, the <i>global tile ids</i> (gids). 0 is empty;
 *   any other number is a picture in one of the tilesets.</li>
 *   <li><b>tilesets</b>: an image cut into tiles, each tile numbered from 0. A map can use several
 *   tilesets; each starts at a {@code firstgid}, so gid 1 is the first tile of the first set. A tile can
 *   have a <i>class</i> (in older Tiled versions: a <i>type</i>), such as {@code solid} or {@code coin}:
 *   what the rules make of it. And an animation: the tiles it shows in turn.</li>
 *   <li><b>object layers</b>: things that are not tiles, placed freely: start, enemies, the goal. Each
 *   object has a class, a box and properties.</li>
 *   <li><b>properties</b> on the map, the layers, the objects and the tiles: what the game needs to know
 *   and Tiled does not, such as the name of the level.</li>
 * </ul>
 *
 * <p>Read are orthogonal maps that are not <i>infinite</i> (Tiled's Map → Map Properties), with layers
 * encoded as CSV or Base64, uncompressed, zlib or gzip, and tilesets inside the map or in their own
 * {@code .tsx} files next to it. Errors name the file and the line, as in {@code level1.tmx:12: ...}.
 */
// --8<-- [start:map]
public final class TiledMap {
    /** The top bits of a gid say whether the tile is flipped; the rest is the tile. */
    public static final int FLIPPED_HORIZONTALLY = 0x80000000;
    public static final int FLIPPED_VERTICALLY = 0x40000000;
    public static final int FLIPPED_DIAGONALLY = 0x20000000;
    private static final int TILE_BITS = 0x0FFFFFFF;

    /** One frame of a tile's animation: which tile of the set, for how long. */
    public record Frame(int tileId, double seconds) {
    }

    /** What the tileset says about one of its tiles: its class, properties and animation. */
    public record TileInfo(int id, String type, TiledProperties properties, List<Frame> animation) {
        public TileInfo {
            animation = List.copyOf(animation);
        }
    }

    /**
     * A tileset: the image {@code image} (a path relative to the map), cut into {@code columns} columns of
     * {@code tileWidth} × {@code tileHeight} pixels. Its tiles have the gids {@code firstGid} to
     * {@code firstGid + tileCount - 1}.
     */
    public record Tileset(int firstGid, String name, int tileWidth, int tileHeight, int tileCount, int columns,
                          String image, int imageWidth, int imageHeight, Map<Integer, TileInfo> tiles) {
        public Tileset {
            tiles = Map.copyOf(tiles);
        }

        public boolean contains(int gid) {
            return gid >= firstGid && gid < firstGid + tileCount;
        }

        public int rows() {
            return (tileCount + columns - 1) / columns;
        }
    }

    /** A grid of gids, row by row. The gids keep their flip bits; {@link #gid} takes them off. */
    public record TileLayer(String name, int width, int height, int[] gids, boolean visible, TiledProperties properties) {
        /** The tile at {@code col}, {@code row}, without flip bits; 0 for none. */
        public int gid(int col, int row) {
            return gids[row * width + col] & TILE_BITS;
        }

        /** The flip bits at {@code col}, {@code row}: {@link #FLIPPED_HORIZONTALLY} and the others. */
        public int flips(int col, int row) {
            return gids[row * width + col] & ~TILE_BITS;
        }
    }

    /** An object layer. */
    public record ObjectGroup(String name, List<TiledObject> objects, boolean visible, TiledProperties properties) {
        public ObjectGroup {
            objects = List.copyOf(objects);
        }
    }

    private final String source;
    private final int width;
    private final int height;
    private final int tileWidth;
    private final int tileHeight;
    private final TiledProperties properties;
    private final List<Tileset> tilesets;
    private final List<TileLayer> layers;
    private final List<ObjectGroup> objectGroups;

    TiledMap(String source, int width, int height, int tileWidth, int tileHeight, TiledProperties properties,
             List<Tileset> tilesets, List<TileLayer> layers, List<ObjectGroup> objectGroups) {
        this.source = source;
        this.width = width;
        this.height = height;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.properties = properties;
        this.tilesets = List.copyOf(tilesets);
        this.layers = List.copyOf(layers);
        this.objectGroups = List.copyOf(objectGroups);
    }

    /**
     * Reads a map among the classes of {@code owner}, e.g. {@code "/levels/level1.tmx"}; tilesets in their
     * own files are looked for next to it.
     */
    public static TiledMap load(Class<?> owner, String resource) {
        int slash = resource.lastIndexOf('/');
        String folder = resource.substring(0, slash + 1);
        InputStream in = owner.getResourceAsStream(resource);
        if (in == null) throw new IllegalArgumentException("no map " + resource);
        return parse(resource.substring(slash + 1), in, file -> owner.getResourceAsStream(folder + file));
    }

    /**
     * Reads a map from a stream; {@code source} names it in errors, {@code files} opens the files it
     * refers to, by their path relative to the map (null if there is none).
     */
    public static TiledMap parse(String source, InputStream in, Function<String, InputStream> files) {
        return new TmxReader(source, files).read(in);
    }
    // --8<-- [end:map]

    public String source() {
        return source;
    }

    /** Width and height in tiles. */
    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** The size of a tile in the map's pixels. */
    public int tileWidth() {
        return tileWidth;
    }

    public int tileHeight() {
        return tileHeight;
    }

    public TiledProperties properties() {
        return properties;
    }

    public List<Tileset> tilesets() {
        return tilesets;
    }

    /** The tile layers, back to front, as in Tiled's layer list read from the bottom. */
    public List<TileLayer> layers() {
        return layers;
    }

    public List<ObjectGroup> objectGroups() {
        return objectGroups;
    }

    // --8<-- [start:lookup]
    /** The tile layer called {@code name}; an error naming the map if there is none. */
    public TileLayer layer(String name) {
        for (TileLayer layer : layers) {
            if (layer.name().equals(name)) return layer;
        }
        throw new DataException(source + ": no tile layer \"" + name + "\"");
    }

    /** The objects of the object layer called {@code name}; an error naming the map if there is none. */
    public List<TiledObject> objects(String name) {
        for (ObjectGroup group : objectGroups) {
            if (group.name().equals(name)) return group.objects();
        }
        throw new DataException(source + ": no object layer \"" + name + "\"");
    }

    /** The tileset a gid belongs to; empty for 0 or a gid of no set. */
    public Optional<Tileset> tileset(int gid) {
        int tile = gid & TILE_BITS;
        for (Tileset set : tilesets) {
            if (set.contains(tile)) return Optional.of(set);
        }
        return Optional.empty();
    }

    /** What the tileset says about a gid's tile; empty if it says nothing. */
    public Optional<TileInfo> info(int gid) {
        int tile = gid & TILE_BITS;
        return tileset(tile).map(set -> set.tiles().get(tile - set.firstGid()));
    }

    /** The class of a gid's tile, such as {@code "solid"}; {@code ""} for none and for empty cells. */
    public String type(int gid) {
        return info(gid).map(TileInfo::type).orElse("");
    }

    /**
     * A tile layer as a {@link TileMap} of the game's own tiles: {@code tiles} turns each gid (0 for an
     * empty cell) into what the rules know, usually by its {@link #type class}.
     */
    public <T> TileMap<T> tiles(String layer, IntFunction<T> tiles) {
        TileLayer source = layer(layer);
        TileMap<T> map = new TileMap<>(width, height, null);
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) map.set(col, row, tiles.apply(source.gid(col, row)));
        }
        return map;
    }
    // --8<-- [end:lookup]
}
