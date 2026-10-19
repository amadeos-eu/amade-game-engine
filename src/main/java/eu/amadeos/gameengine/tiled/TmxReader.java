package eu.amadeos.gameengine.tiled;

import eu.amadeos.gameengine.data.TextData.DataException;
import eu.amadeos.gameengine.tiled.TiledMap.Frame;
import eu.amadeos.gameengine.tiled.TiledMap.ObjectGroup;
import eu.amadeos.gameengine.tiled.TiledMap.TileInfo;
import eu.amadeos.gameengine.tiled.TiledMap.TileLayer;
import eu.amadeos.gameengine.tiled.TiledMap.Tileset;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Reads Tiled's XML, a map ({@code .tmx}) or a tileset of its own ({@code .tsx}), element by element with
 * SAX, which knows the line of each element, so that every error can name it.
 */
final class TmxReader extends DefaultHandler {
    private final String source;
    private final Function<String, InputStream> files;
    private Locator locator;
    /** The names of the open elements, innermost first. */
    private final Deque<String> open = new ArrayDeque<>();
    /** The properties of the open elements that can have them, innermost first. */
    private final Deque<Map<String, String>> owners = new ArrayDeque<>();
    /** How deep in {@code <properties>} elements: properties of a class property are inside a second one. */
    private int propertiesDepth;
    private String propertyName;
    private final StringBuilder text = new StringBuilder();
    /** Inside an element of no interest, such as a tile's collision shapes: how deep. */
    private int skipping;

    private int width;
    private int height;
    private int tileWidth;
    private int tileHeight;
    private Map<String, String> mapProperties = Map.of();
    private final List<Tileset> tilesets = new ArrayList<>();
    private final List<TileLayer> layers = new ArrayList<>();
    private final List<ObjectGroup> groups = new ArrayList<>();

    // The tileset, layer, group, object or tile being read.
    private TilesetBuilder tileset;
    private int tilesetFirstGid = 1;
    private String layerName;
    private int layerWidth;
    private int layerHeight;
    private boolean layerVisible;
    private String encoding;
    private String compression;
    private List<Integer> xmlTiles;
    private int[] gids;
    private String groupName;
    private boolean groupVisible;
    private List<TiledObject> objects;
    private Attributes object;
    private int objectLine;
    private TileBuilder tile;

    TmxReader(String source, Function<String, InputStream> files) {
        this.source = source;
        this.files = files;
    }

    TiledMap read(InputStream in) {
        parse(in);
        if (width == 0) throw new DataException(source + ": not a Tiled map, no <map> element");
        return new TiledMap(source, width, height, tileWidth, tileHeight, new TiledProperties(mapProperties),
                tilesets, layers, groups);
    }

    /** Reads a tileset file, whose first tile gets the gid {@code firstGid}. */
    Tileset readTileset(InputStream in, int firstGid) {
        tilesetFirstGid = firstGid;
        parse(in);
        if (tilesets.isEmpty()) throw new DataException(source + ": not a Tiled tileset, no <tileset> element");
        return tilesets.getFirst();
    }

    private void parse(InputStream in) {
        try (in) {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            SAXParser parser = factory.newSAXParser();
            parser.parse(in, this);
        } catch (SAXParseException e) {
            throw new DataException(source + ":" + e.getLineNumber() + ": " + e.getMessage());
        } catch (SAXException | ParserConfigurationException e) {
            if (e.getCause() instanceof DataException data) throw data;
            throw new DataException(source + ": " + e.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + source, e);
        }
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    private DataException error(String message) {
        return new DataException(source + ":" + (locator == null ? 0 : locator.getLineNumber()) + ": " + message);
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes a) {
        String parent = open.peek();
        if (skipping > 0 || "tile".equals(parent) && name.equals("objectgroup") || name.equals("wangsets")) {
            // A tile's collision shapes and the terrain sets: Tiled's own, nothing for the game.
            skipping++;
            return;
        }
        open.push(name);
        text.setLength(0);
        switch (name) {
            case "map" -> {
                if (!"orthogonal".equals(a.getValue("orientation"))) {
                    throw error("only orthogonal maps can be read, this one is " + a.getValue("orientation"));
                }
                if ("1".equals(a.getValue("infinite"))) {
                    throw error("infinite maps cannot be read: untick Infinite under Map → Map Properties");
                }
                width = positive(a, "width");
                height = positive(a, "height");
                tileWidth = positive(a, "tilewidth");
                tileHeight = positive(a, "tileheight");
                owners.push(new LinkedHashMap<>());
            }
            case "tileset" -> {
                String file = a.getValue("source");
                int firstGid = a.getValue("firstgid") != null ? positive(a, "firstgid") : tilesetFirstGid;
                if (file != null) {
                    InputStream in = files.apply(file);
                    if (in == null) throw error("the tileset " + file + " is missing");
                    Tileset set = new TmxReader(file, path -> files.apply(sibling(file, path))).readTileset(in, firstGid);
                    // The image path in the tileset file is relative to that file; here it is relative to the map.
                    tilesets.add(new Tileset(set.firstGid(), set.name(), set.tileWidth(), set.tileHeight(),
                            set.tileCount(), set.columns(), set.image() == null ? null : sibling(file, set.image()),
                            set.imageWidth(), set.imageHeight(), set.tiles()));
                    owners.push(new LinkedHashMap<>());     // a source tileset has no properties of its own here
                } else {
                    tileset = new TilesetBuilder(firstGid, a.getValue("name"), positive(a, "tilewidth"),
                            positive(a, "tileheight"), positive(a, "tilecount"), positive(a, "columns"));
                    owners.push(new LinkedHashMap<>());
                }
            }
            case "image" -> {
                if ("tileset".equals(parent) && tileset != null) {
                    tileset.image = a.getValue("source");
                    tileset.imageWidth = integer(a, "width", 0);
                    tileset.imageHeight = integer(a, "height", 0);
                }
            }
            case "tile" -> {
                if ("tileset".equals(parent)) {
                    tile = new TileBuilder(integer(a, "id", -1), typeOf(a));
                    owners.push(tile.properties);
                } else if ("data".equals(parent)) {
                    xmlTiles.add((int) Long.parseLong(a.getValue("gid") == null ? "0" : a.getValue("gid")));
                }
            }
            case "frame" -> {
                if (tile != null) {
                    tile.animation.add(new Frame(integer(a, "tileid", 0), integer(a, "duration", 100) / 1000.0));
                }
            }
            case "layer" -> {
                layerName = a.getValue("name");
                layerWidth = integer(a, "width", width);
                layerHeight = integer(a, "height", height);
                if (layerWidth != width || layerHeight != height) {
                    throw error("the layer \"" + layerName + "\" is " + layerWidth + " × " + layerHeight
                            + ", the map " + width + " × " + height);
                }
                layerVisible = !"0".equals(a.getValue("visible"));
                gids = null;
                owners.push(new LinkedHashMap<>());
            }
            case "data" -> {
                encoding = a.getValue("encoding");
                compression = a.getValue("compression");
                xmlTiles = new ArrayList<>();
            }
            case "chunk" -> throw error("chunks belong to infinite maps, which cannot be read");
            case "objectgroup" -> {
                groupName = a.getValue("name");
                groupVisible = !"0".equals(a.getValue("visible"));
                objects = new ArrayList<>();
                owners.push(new LinkedHashMap<>());
            }
            case "object" -> {
                if (a.getValue("template") != null) {
                    throw error("object templates cannot be read: detach the object from its template");
                }
                object = new org.xml.sax.helpers.AttributesImpl(a);
                objectLine = locator == null ? 0 : locator.getLineNumber();
                owners.push(new LinkedHashMap<>());
            }
            case "properties" -> propertiesDepth++;
            case "property" -> {
                if (propertiesDepth == 1 && !owners.isEmpty()) {
                    propertyName = a.getValue("name");
                    String value = a.getValue("value");
                    // Multi-line text properties keep their value as the element's text instead.
                    if (value != null) owners.peek().put(propertyName, value);
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        text.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String name) {
        if (skipping > 0) {
            skipping--;
            return;
        }
        open.pop();
        switch (name) {
            case "map" -> mapProperties = owners.pop();
            case "tileset" -> {
                owners.pop();
                if (tileset != null) {
                    tilesets.add(tileset.build());
                    tileset = null;
                }
            }
            case "tile" -> {
                if (tile != null && "tileset".equals(open.peek())) {
                    owners.pop();
                    if (tile.id < 0 || tile.id >= tileset.tileCount) {
                        throw error("tile " + tile.id + " is not in the tileset of " + tileset.tileCount);
                    }
                    tileset.tiles.put(tile.id, new TileInfo(tile.id, tile.type,
                            new TiledProperties(tile.properties), tile.animation));
                    tile = null;
                }
            }
            case "data" -> gids = decode(text.toString().strip());
            case "layer" -> {
                Map<String, String> properties = owners.pop();
                if (gids == null) throw error("the layer \"" + layerName + "\" has no data");
                layers.add(new TileLayer(layerName, layerWidth, layerHeight, gids, layerVisible,
                        new TiledProperties(properties)));
            }
            case "objectgroup" -> groups.add(new ObjectGroup(groupName, objects, groupVisible,
                    new TiledProperties(owners.pop())));
            case "object" -> objects.add(object(new TiledProperties(owners.pop())));
            case "properties" -> propertiesDepth--;
            case "property" -> {
                if (propertiesDepth == 1 && propertyName != null && !owners.isEmpty()
                        && !owners.peek().containsKey(propertyName)) {
                    owners.peek().put(propertyName, text.toString());
                }
                propertyName = null;
            }
            default -> {
            }
        }
    }

    // --8<-- [start:decode]
    /** The gids of a layer's {@code <data>}, in one of Tiled's encodings. */
    private int[] decode(String data) {
        int count = width * height;
        int[] result = new int[count];
        if (encoding == null) {
            // Old Tiled: one <tile gid="..."/> element per cell.
            if (xmlTiles.size() != count) throw error(xmlTiles.size() + " tiles in the layer, the map has " + count);
            for (int i = 0; i < count; i++) result[i] = xmlTiles.get(i);
        } else if (encoding.equals("csv")) {
            String[] cells = data.split("\\s*,\\s*");
            if (cells.length != count) throw error(cells.length + " tiles in the layer, the map has " + count);
            for (int i = 0; i < count; i++) {
                try {
                    // Up to 2³² - 1: the flip bits make many gids too large for an int, not for a long.
                    result[i] = (int) Long.parseLong(cells[i]);
                } catch (NumberFormatException e) {
                    throw error("\"" + cells[i] + "\" is not a tile number");
                }
            }
        } else if (encoding.equals("base64")) {
            byte[] bytes = inflate(Base64.getMimeDecoder().decode(data));
            if (bytes.length != count * 4) throw error(bytes.length / 4 + " tiles in the layer, the map has " + count);
            // Four bytes per tile, the lowest first.
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(result);
        } else {
            throw error("the encoding " + encoding + " cannot be read; choose CSV under Map → Map Properties");
        }
        return result;
    }
    // --8<-- [end:decode]

    private byte[] inflate(byte[] bytes) {
        if (compression == null || compression.isEmpty()) return bytes;
        try (InputStream in = switch (compression) {
            case "zlib" -> new InflaterInputStream(new ByteArrayInputStream(bytes));
            case "gzip" -> new GZIPInputStream(new ByteArrayInputStream(bytes));
            default -> throw error("the compression " + compression + " cannot be read; choose zlib, gzip or none");
        }) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            in.transferTo(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw error("the layer's data is damaged: " + e.getMessage());
        }
    }

    // --8<-- [start:object]
    private TiledObject object(TiledProperties properties) {
        int raw = (int) Long.parseLong(object.getValue("gid") == null ? "0" : object.getValue("gid"));
        int gid = raw & 0x0FFFFFFF;
        double x = number(object, "x", 0);
        double y = number(object, "y", 0);
        double w = number(object, "width", 0);
        double h = number(object, "height", 0);
        if (gid != 0) {
            // A tile object: Tiled places it by its bottom left corner, and without a size it is the tile's.
            Tileset set = tilesets.stream().filter(s -> s.contains(gid)).findFirst()
                    .orElseThrow(() -> new DataException(source + ":" + objectLine + ": the object shows tile "
                            + gid + ", which is in no tileset"));
            if (w == 0) w = set.tileWidth();
            if (h == 0) h = set.tileHeight();
            y -= h;
        }
        String name = object.getValue("name");
        return new TiledObject(integer(object, "id", 0), name == null ? "" : name, typeOf(object), x, y, w, h,
                gid, properties);
    }
    // --8<-- [end:object]

    /** The class of a tile or object: {@code class} since Tiled 1.9, {@code type} before and since 1.10. */
    private static String typeOf(Attributes a) {
        String type = a.getValue("type");
        if (type == null) type = a.getValue("class");
        return type == null ? "" : type;
    }

    /** A path next to {@code file}: the image of a tileset in another folder lies next to the tileset. */
    private static String sibling(String file, String path) {
        int slash = file.lastIndexOf('/');
        return slash < 0 ? path : file.substring(0, slash + 1) + path;
    }

    private int positive(Attributes a, String name) {
        int value = integer(a, name, 0);
        if (value <= 0) throw error("<" + open.peek() + "> needs a " + name + " above 0");
        return value;
    }

    private int integer(Attributes a, String name, int fallback) {
        String value = a.getValue(name);
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException e) {
            throw error(name + "=\"" + value + "\" is not a whole number");
        }
    }

    private double number(Attributes a, String name, double fallback) {
        String value = a.getValue(name);
        if (value == null) return fallback;
        try {
            return Double.parseDouble(value.strip());
        } catch (NumberFormatException e) {
            throw error(name + "=\"" + value + "\" is not a number");
        }
    }

    private static final class TilesetBuilder {
        final int firstGid;
        final String name;
        final int tileWidth;
        final int tileHeight;
        final int tileCount;
        final int columns;
        String image;
        int imageWidth;
        int imageHeight;
        final Map<Integer, TileInfo> tiles = new HashMap<>();

        TilesetBuilder(int firstGid, String name, int tileWidth, int tileHeight, int tileCount, int columns) {
            this.firstGid = firstGid;
            this.name = name == null ? "" : name;
            this.tileWidth = tileWidth;
            this.tileHeight = tileHeight;
            this.tileCount = tileCount;
            this.columns = columns;
        }

        Tileset build() {
            return new Tileset(firstGid, name, tileWidth, tileHeight, tileCount, columns, image, imageWidth,
                    imageHeight, tiles);
        }
    }

    private static final class TileBuilder {
        final int id;
        final String type;
        final Map<String, String> properties = new LinkedHashMap<>();
        final List<Frame> animation = new ArrayList<>();

        TileBuilder(int id, String type) {
            this.id = id;
            this.type = type;
        }
    }
}
