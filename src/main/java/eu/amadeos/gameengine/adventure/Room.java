package eu.amadeos.gameengine.adventure;

import eu.amadeos.gameengine.data.TextData;
import eu.amadeos.gameengine.math.Vector;
import eu.amadeos.gameengine.tile.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * One room of an adventure, as a data file describes it: the picture behind everything, the floor, how
 * people shrink towards the back, the things one can point at, the places where people arrive, and the parts
 * of the picture that stand in front of people.
 *
 * <pre>
 * room workshop
 * background workshop.png
 * depth 262 0.85 400 1.15                       # farY farScale nearY nearScale
 * floor 30 268  610 268  640 400  0 400          # corners of the floor
 * hole 214 292  434 292  434 330  214 330        # the workbench stands in the way
 * spot start 300 330 up                          # where people arrive: name x y facing
 * hotspot psalter 292 238 62 16 stand 300 296 up # name, rectangle x y width height, where to stand
 * hotspot window poly 40 44 190 44 190 214 40 214 stand 120 280 up
 * exit door 368 104 64 158 stand 400 275 up to corridor door
 * layer press.png 20 272 baseline 400            # drawn over everyone whose feet are above y 400
 * hidden page                                    # a hotspot that is there only later
 * </pre>
 *
 * <p>A <b>layer</b> solves the oldest problem of painted rooms: the hero walks behind the book press, but
 * in front of the workbench. Each layer has a <i>baseline</i>, the height where it touches the floor; the
 * game draws the actors whose feet are above it first, then the layer, then the actors below it. That is
 * the painter's algorithm, the same as {@link eu.amadeos.gameengine.iso} uses for its tiles.
 *
 * <p>Hotspots defined later lie on top of earlier ones, so a small thing on a large one is written after
 * it. Which hotspots are hidden is the only part of a room that changes while one plays.
 */
public final class Room {
    /** A part of the picture drawn in front of the actors standing behind it. */
    public record Layer(String image, double x, double y, double baseline) {
    }

    /** A named place to stand: where people enter, or where a script sends them. */
    public record Spot(String name, Vector position, Direction facing) {
    }

    private final String id;
    private final String background;
    private final WalkArea floor;
    private final DepthScale depth;
    private final List<Hotspot> hotspots;
    private final Map<String, Spot> spots;
    private final List<Layer> layers;
    private final Set<String> hidden = new HashSet<>();

    public Room(String id, String background, WalkArea floor, DepthScale depth, List<Hotspot> hotspots,
                List<Spot> spots, List<Layer> layers) {
        this.id = Objects.requireNonNull(id, "id");
        this.background = background;
        this.floor = Objects.requireNonNull(floor, "floor");
        this.depth = Objects.requireNonNull(depth, "depth");
        this.hotspots = List.copyOf(hotspots);
        Map<String, Spot> byName = new LinkedHashMap<>();
        for (Spot spot : spots) byName.put(spot.name(), spot);
        this.spots = Collections.unmodifiableMap(byName);
        this.layers = List.copyOf(layers);
    }

    public String id() {
        return id;
    }

    /** The name of the picture behind everything, for the game to load; may be null. */
    public String background() {
        return background;
    }

    public WalkArea floor() {
        return floor;
    }

    public DepthScale depth() {
        return depth;
    }

    /** All hotspots, hidden ones too, from the bottom to the top. */
    public List<Hotspot> hotspots() {
        return hotspots;
    }

    public List<Layer> layers() {
        return layers;
    }

    public Spot spot(String name) {
        Spot spot = spots.get(name);
        if (spot == null) throw new IllegalArgumentException("room " + id + " has no spot " + name);
        return spot;
    }

    public Hotspot hotspot(String name) {
        for (Hotspot h : hotspots) if (h.id().equals(name)) return h;
        throw new IllegalArgumentException("room " + id + " has no hotspot " + name);
    }

    public boolean has(String hotspot) {
        return hotspots.stream().anyMatch(h -> h.id().equals(hotspot));
    }

    /** The topmost visible hotspot under the point, or null. */
    public Hotspot hotspotAt(Vector p) {
        for (int i = hotspots.size() - 1; i >= 0; i--) {
            Hotspot h = hotspots.get(i);
            if (!hidden.contains(h.id()) && h.contains(p)) return h;
        }
        return null;
    }

    public boolean isHidden(String hotspot) {
        return hidden.contains(hotspot);
    }

    public void show(String hotspot) {
        hotspot(hotspot);
        hidden.remove(hotspot);
    }

    public void hide(String hotspot) {
        hotspot(hotspot);
        hidden.add(hotspot);
    }

    /** The hidden hotspots, for a saved game. */
    public Set<String> hidden() {
        return Collections.unmodifiableSet(hidden);
    }

    // --8<-- [start:parse]
    /** Reads a room from the lines of its data file; errors name the file and the line. */
    public static Room parse(List<TextData.Line> lines) {
        String id = null, background = null;
        List<Vector> outline = null;
        List<List<Vector>> holes = new ArrayList<>();
        DepthScale depth = DepthScale.FLAT;
        List<Hotspot> hotspots = new ArrayList<>();
        List<Spot> spots = new ArrayList<>();
        List<Layer> layers = new ArrayList<>();
        List<String> hidden = new ArrayList<>();
        for (TextData.Line line : lines) {
            switch (line.keyword()) {
                case "room" -> id = line.word(1);
                case "background" -> background = line.word(1);
                case "depth" -> depth = new DepthScale(line.number(1), line.number(2), line.number(3),
                        line.number(4));
                case "floor" -> outline = points(line, 1, line.size());
                case "hole" -> holes.add(points(line, 1, line.size()));
                case "spot" -> spots.add(new Spot(line.word(1), new Vector(line.number(2), line.number(3)),
                        direction(line, 4)));
                case "hotspot", "exit" -> hotspots.add(hotspot(line));
                case "layer" -> {
                    if (!line.word(4).equals("baseline")) throw line.error("expected baseline, found " + line.word(4));
                    layers.add(new Layer(line.word(1), line.number(2), line.number(3), line.number(5)));
                }
                case "hidden" -> hidden.add(line.word(1));
                default -> throw line.error("unknown keyword " + line.keyword());
            }
        }
        String where = lines.isEmpty() ? "room" : lines.getFirst().source();
        if (id == null) throw new TextData.DataException(where + ": no room line");
        if (outline == null) throw new TextData.DataException(where + ": room " + id + " has no floor");
        Room room = new Room(id, background, new WalkArea(outline, holes), depth, hotspots, spots, layers);
        for (String name : hidden) room.hide(name);
        return room;
    }
    // --8<-- [end:parse]

    private static Hotspot hotspot(TextData.Line line) {
        String name = line.word(1);
        int at = 2;
        List<Vector> area;
        if (line.word(2).equals("poly")) {
            int end = 3;
            while (end < line.size() && !line.word(end).equals("stand")) end++;
            area = points(line, 3, end);
            at = end;
        } else {
            area = Hotspot.rectangle(line.number(2), line.number(3), line.number(4), line.number(5));
            at = 6;
        }
        if (!line.word(at).equals("stand")) throw line.error("expected stand, found " + line.word(at));
        Vector stand = new Vector(line.number(at + 1), line.number(at + 2));
        Direction facing = direction(line, at + 3);
        if (line.keyword().equals("exit")) {
            if (!line.word(at + 4).equals("to")) throw line.error("expected to, found " + line.word(at + 4));
            return new Hotspot(name, area, stand, facing, line.word(at + 5), line.word(at + 6));
        }
        return new Hotspot(name, area, stand, facing);
    }

    private static List<Vector> points(TextData.Line line, int from, int to) {
        if ((to - from) % 2 != 0) throw line.error("corners need an x and a y each");
        List<Vector> points = new ArrayList<>();
        for (int i = from; i < to; i += 2) points.add(new Vector(line.number(i), line.number(i + 1)));
        if (points.size() < 3) throw line.error("an area needs at least 3 corners");
        return points;
    }

    private static Direction direction(TextData.Line line, int index) {
        String word = line.word(index);
        try {
            return Direction.valueOf(word.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw line.error('"' + word + "\" is no direction (up, down, left, right)");
        }
    }
}
