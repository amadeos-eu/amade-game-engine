package eu.amadeos.gameengine.road;

import eu.amadeos.gameengine.tile.Cell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * Roads between flags, as an economy game has them: the player lays a road from one flag to another over
 * the tiles between, and the goods travel on them from flag to flag. A flag is a point of the network, a
 * road one piece of way between two flags, with the tiles it runs over. That is a <i>graph</i>: the flags
 * are its nodes, the roads its edges, each as long as its steps.
 *
 * <p>A ware at a flag asks the network only one question: which road do I take next, on the shortest way
 * to the flag I am going to? The answer comes from a search from the goal outwards, <b>Dijkstra</b>'s
 * (1959): the goal has distance 0, and the flag nearest to everything reached so far is always the next
 * to be settled, so every flag gets its shortest distance and the road that leads on from it. One search
 * answers the question for every flag at once, as a flow field does for every tile; the network keeps the
 * search of each goal until a road is laid or removed.
 *
 * <pre>
 * RoadNetwork roads = new RoadNetwork();
 * int home = roads.addFlag(new Cell(10, 12));
 * int mill = roads.addFlag(new Cell(14, 12));
 * roads.connect(home, mill, List.of(new Cell(10, 12), new Cell(11, 12), ..., new Cell(14, 12)));
 * RoadNetwork.Road first = roads.next(home, mill);   // the road a ware at home takes towards the mill
 * </pre>
 *
 * <p>Flags and roads are numbered in the order they are made; with the same numbers, the same searches
 * give the same answers, which a saved game that goes on exactly as before needs.
 */
// --8<-- [start:network]
public final class RoadNetwork {
    /** A road from flag {@code from} to flag {@code to} over {@code cells}, the first at {@code from}, the last at {@code to}. */
    public record Road(int id, int from, int to, List<Cell> cells) {
        public Road {
            cells = List.copyOf(cells);
            if (cells.size() < 2) throw new IllegalArgumentException("a road needs two cells at least");
        }

        /** Steps from one end to the other. */
        public int length() {
            return cells.size() - 1;
        }

        /** The flag at the other end from {@code flag}. */
        public int other(int flag) {
            if (flag == from) return to;
            if (flag == to) return from;
            throw new IllegalArgumentException("flag " + flag + " is not an end of road " + id);
        }

        /** The cells walked from {@code flag} to the other end. */
        public List<Cell> from(int flag) {
            if (flag == from) return cells;
            if (flag != to) throw new IllegalArgumentException("flag " + flag + " is not an end of road " + id);
            List<Cell> back = new ArrayList<>(cells);
            Collections.reverse(back);
            return back;
        }
    }

    /** The shortest distance of every flag to one goal, and the road each takes on its way there. */
    private record Search(Map<Integer, Integer> distance, Map<Integer, Road> next) {
    }

    private static final int SEARCHES = 64;

    private final Map<Integer, Cell> flags = new TreeMap<>();
    private final Map<Cell, Integer> flagAt = new HashMap<>();
    private final Map<Integer, Road> roads = new TreeMap<>();
    /** Every cell a road runs over, but not its two flags: where a road may be split by a new flag. */
    private final Map<Cell, Road> roadAt = new HashMap<>();
    private final Map<Integer, List<Road>> touching = new TreeMap<>();
    private final Map<Integer, Search> searches = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Search> eldest) {
            return size() > SEARCHES;
        }
    };
    private int nextFlag;
    private int nextRoad;
    private int version;
    private int searched;
    // --8<-- [end:network]

    /** A new flag on {@code cell}; its number. */
    public int addFlag(Cell cell) {
        return addFlag(nextFlag, cell);
    }

    /** A flag with a given number, as a saved game had it. */
    public int addFlag(int id, Cell cell) {
        if (flags.containsKey(id)) throw new IllegalArgumentException("flag " + id + " exists");
        if (flagAt.containsKey(cell)) throw new IllegalArgumentException("a flag stands on " + cell);
        if (roadAt.containsKey(cell)) throw new IllegalArgumentException("a road runs over " + cell + ": split it");
        flags.put(id, cell);
        flagAt.put(cell, id);
        touching.put(id, new ArrayList<>());
        nextFlag = Math.max(nextFlag, id + 1);
        changed();
        return id;
    }

    /** Removes a flag and every road that ends at it. */
    public void removeFlag(int flag) {
        Cell cell = flags.remove(flag);
        if (cell == null) throw new IllegalArgumentException("no flag " + flag);
        for (Road road : List.copyOf(touching.get(flag))) remove(road.id());
        touching.remove(flag);
        flagAt.remove(cell);
        changed();
    }

    // --8<-- [start:connect]
    /**
     * A road from {@code from} to {@code to} over {@code cells}: the first cell is {@code from}'s, the last
     * {@code to}'s, each one step from the one before, and none taken by another road or flag.
     */
    public Road connect(int from, int to, List<Cell> cells) {
        return connect(nextRoad, from, to, cells);
    }

    public Road connect(int id, int from, int to, List<Cell> cells) {
        if (roads.containsKey(id)) throw new IllegalArgumentException("road " + id + " exists");
        if (from == to) throw new IllegalArgumentException("a road joins two different flags");
        if (!flags.containsKey(from) || !flags.containsKey(to)) throw new IllegalArgumentException("no such flag");
        if (!cells.getFirst().equals(flags.get(from)) || !cells.getLast().equals(flags.get(to))) {
            throw new IllegalArgumentException("a road runs from its first flag to its second");
        }
        for (int i = 1; i < cells.size(); i++) {
            Cell a = cells.get(i - 1);
            Cell b = cells.get(i);
            if (Math.abs(a.col() - b.col()) + Math.abs(a.row() - b.row()) != 1) {
                throw new IllegalArgumentException("the cells of a road are steps: " + a + " to " + b);
            }
            if (i < cells.size() - 1 && (roadAt.containsKey(b) || flagAt.containsKey(b))) {
                throw new IllegalArgumentException("the road would cross another at " + b);
            }
        }
        Road road = new Road(id, from, to, cells);
        roads.put(id, road);
        for (Cell cell : cells.subList(1, cells.size() - 1)) roadAt.put(cell, road);
        touching.get(from).add(road);
        touching.get(to).add(road);
        nextRoad = Math.max(nextRoad, id + 1);
        changed();
        return road;
    }
    // --8<-- [end:connect]

    /** Removes a road; its flags stay. */
    public void remove(int road) {
        Road removed = roads.remove(road);
        if (removed == null) throw new IllegalArgumentException("no road " + road);
        for (Cell cell : removed.cells().subList(1, removed.cells().size() - 1)) roadAt.remove(cell);
        touching.get(removed.from()).remove(removed);
        touching.get(removed.to()).remove(removed);
        changed();
    }

    // --8<-- [start:split]
    /**
     * Puts a new flag on {@code cell}, in the middle of a road, and makes two roads of the one: a long road
     * becomes two short ones, each with a carrier of its own. Returns the new flag.
     */
    public int split(Cell cell) {
        Road road = roadAt.get(cell);
        if (road == null) throw new IllegalArgumentException("no road runs over " + cell);
        int at = road.cells().indexOf(cell);
        remove(road.id());
        int flag = addFlag(cell);
        connect(road.from(), flag, road.cells().subList(0, at + 1));
        connect(flag, road.to(), road.cells().subList(at, road.cells().size()));
        return flag;
    }
    // --8<-- [end:split]

    // --8<-- [start:next]
    /**
     * The road a ware at flag {@code from} takes next on a shortest way to flag {@code to}; {@code null}
     * when it is there already or no way leads there.
     */
    public Road next(int from, int to) {
        if (from == to) return null;
        return search(to).next().get(from);
    }

    /** Steps on the shortest way from one flag to another; {@link Integer#MAX_VALUE} without a way. */
    public int distance(int from, int to) {
        if (!flags.containsKey(from) || !flags.containsKey(to)) return Integer.MAX_VALUE;
        return search(to).distance().getOrDefault(from, Integer.MAX_VALUE);
    }

    public boolean connected(int from, int to) {
        return distance(from, to) != Integer.MAX_VALUE;
    }

    /** Dijkstra's search from the goal outwards, kept until the network changes. */
    private Search search(int goal) {
        Search search = searches.get(goal);
        if (search != null) return search;
        Map<Integer, Integer> distance = new HashMap<>();
        Map<Integer, Road> next = new HashMap<>();
        // Ties go to the lower flag number, so the answer never depends on the order of a hash map.
        PriorityQueue<int[]> open = new PriorityQueue<>((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0])
                : Integer.compare(a[1], b[1]));
        if (flags.containsKey(goal)) {
            distance.put(goal, 0);
            open.add(new int[]{0, goal});
        }
        while (!open.isEmpty()) {
            int[] head = open.poll();
            int flag = head[1];
            if (head[0] > distance.get(flag)) continue;     // an older, longer entry of a flag settled since
            for (Road road : touching.get(flag)) {
                int other = road.other(flag);
                int d = head[0] + road.length();
                Integer known = distance.get(other);
                if (known == null || d < known || d == known && road.id() < next.get(other).id()) {
                    distance.put(other, d);
                    next.put(other, road);
                    open.add(new int[]{d, other});
                }
            }
        }
        search = new Search(distance, next);
        searches.put(goal, search);
        searched++;
        return search;
    }
    // --8<-- [end:next]

    /** The roads from {@code from} to {@code to} in the order they are walked; empty without a way. */
    public List<Road> way(int from, int to) {
        List<Road> way = new ArrayList<>();
        if (!connected(from, to)) return way;
        int at = from;
        while (at != to) {
            Road road = next(at, to);
            way.add(road);
            at = road.other(at);
        }
        return way;
    }

    private void changed() {
        version++;
        searches.clear();
    }

    public boolean hasFlag(int flag) {
        return flags.containsKey(flag);
    }

    public Cell cell(int flag) {
        Cell cell = flags.get(flag);
        if (cell == null) throw new IllegalArgumentException("no flag " + flag);
        return cell;
    }

    /** The flag on {@code cell}, or -1. */
    public int flagAt(Cell cell) {
        return flagAt.getOrDefault(cell, -1);
    }

    /** The road that runs over {@code cell} between its flags, or {@code null}. */
    public Road roadAt(Cell cell) {
        return roadAt.get(cell);
    }

    public Road road(int id) {
        return roads.get(id);
    }

    /** All flags, by number. Read only. */
    public Collection<Integer> flags() {
        return Collections.unmodifiableSet(flags.keySet());
    }

    /** All roads, by number. Read only. */
    public Collection<Road> roads() {
        return Collections.unmodifiableCollection(roads.values());
    }

    /** The roads that end at {@code flag}. */
    public List<Road> roads(int flag) {
        List<Road> list = touching.get(flag);
        return list == null ? List.of() : List.copyOf(list);
    }

    /** Counts every change, for whoever keeps something worked out from the roads. */
    public int version() {
        return version;
    }

    /** How many searches the network has made: for the tests. */
    public int searches() {
        return searched;
    }

    /** The numbers the next flag and road get; a saved game keeps them. */
    public int nextFlag() {
        return nextFlag;
    }

    public int nextRoad() {
        return nextRoad;
    }

    /** Takes back the numbers the next flag and road get, as a saved game had them. */
    public void restore(int nextFlag, int nextRoad) {
        this.nextFlag = Math.max(this.nextFlag, nextFlag);
        this.nextRoad = Math.max(this.nextRoad, nextRoad);
    }
}
