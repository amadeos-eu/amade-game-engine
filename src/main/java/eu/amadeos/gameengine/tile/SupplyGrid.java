package eu.amadeos.gameengine.tile;

import java.util.Arrays;

/**
 * A network of tiles that carries something from where it is made to where it is used: power from plants
 * through lines and buildings to homes, or water through pipes. Tiles that carry are <i>conductors</i>;
 * some of them are <i>sources</i> that put an amount into the network, others <i>loads</i> that take an
 * amount out.
 *
 * <p>{@link #solve()} finds the networks, the groups of conductors that touch along an edge (not across a
 * corner), and in each network serves the loads in the order a flood from the sources reaches them, the
 * nearest first, until the network's sources run dry. So a town that builds more than its plant can feed
 * loses power at the far end, where the player sees it, and not here and there at random.
 *
 * <pre>
 * SupplyGrid power = new SupplyGrid(map.cols(), map.rows());
 * power.clear();
 * for each tile with a line or a building: power.conductor(col, row);
 * power.source(plant.col(), plant.row(), 120);       // a plant's tile, 120 MW
 * power.load(col, row, 2);                            // a house's tile, 2 MW
 * power.solve();
 * if (!power.supplied(col, row)) showNoPowerSign(col, row);
 * </pre>
 *
 * <p>A load or source on a tile that does not conduct is made a conductor. A load is served wholly or not
 * at all. The map does not wrap.
 */
// --8<-- [start:grid]
public final class SupplyGrid {
    private final int cols;
    private final int rows;
    private final boolean[] conducts;
    private final double[] sources;
    private final double[] loads;
    private final int[] network;
    private final boolean[] supplied;
    private final int[] queue;
    private double supply;
    private double demand;
    private double served;
    private int networks;
    /** The networks with a source come first: numbers below this one. */
    private int fed;

    public SupplyGrid(int cols, int rows) {
        if (cols <= 0 || rows <= 0) throw new IllegalArgumentException("empty map");
        this.cols = cols;
        this.rows = rows;
        int n = cols * rows;
        conducts = new boolean[n];
        sources = new double[n];
        loads = new double[n];
        network = new int[n];
        supplied = new boolean[n];
        queue = new int[n];
    }

    /** Forgets every conductor, source and load. */
    public void clear() {
        Arrays.fill(conducts, false);
        Arrays.fill(sources, 0);
        Arrays.fill(loads, 0);
        Arrays.fill(network, -1);
        Arrays.fill(supplied, false);
        supply = demand = served = 0;
        networks = fed = 0;
    }

    /** A tile that carries. */
    public void conductor(int col, int row) {
        conducts[row * cols + col] = true;
    }

    /** A tile that puts {@code amount} into its network; added to what the tile puts in already. */
    public void source(int col, int row, double amount) {
        int i = row * cols + col;
        conducts[i] = true;
        sources[i] += amount;
    }

    /** A tile that takes {@code amount} out of its network; added to what the tile takes already. */
    public void load(int col, int row, double amount) {
        int i = row * cols + col;
        conducts[i] = true;
        loads[i] += amount;
    }

    /** Finds the networks and serves the loads, nearest to a source first. */
    public void solve() {
        Arrays.fill(network, -1);
        Arrays.fill(supplied, false);
        supply = demand = served = 0;
        networks = fed = 0;
        int n = cols * rows;
        for (int i = 0; i < n; i++) {
            supply += sources[i];
            demand += loads[i];
        }
        // Each network with a source: flood from all its sources at once, serving loads as they are reached.
        for (int start = 0; start < n; start++) {
            if (sources[start] <= 0 || network[start] >= 0) continue;
            int id = networks++;
            double left = 0;
            int head = 0;
            int tail = 0;
            // First the whole network, to know what its sources make...
            queue[tail++] = start;
            network[start] = id;
            while (head < tail) {
                int i = queue[head++];
                left += sources[i];
                tail = spread(i, id, tail);
            }
            // ...then from the sources outwards, in breadth-first order of distance from the nearest source.
            int[] order = breadthFromSources(id, tail);
            for (int k = 0; k < tail; k++) {
                int i = order[k];
                if (loads[i] <= 0) continue;
                if (loads[i] <= left + 1e-9) {
                    left -= loads[i];
                    supplied[i] = true;
                    served += loads[i];
                }
            }
        }
        fed = networks;
        // Networks without a source are networks too, for network().
        for (int start = 0; start < n; start++) {
            if (!conducts[start] || network[start] >= 0) continue;
            int id = networks++;
            int head = 0;
            int tail = 0;
            queue[tail++] = start;
            network[start] = id;
            while (head < tail) tail = spread(queue[head++], id, tail);
        }
    }
    // --8<-- [end:grid]

    private int spread(int i, int id, int tail) {
        int col = i % cols;
        int row = i / cols;
        if (col > 0) tail = visit(i - 1, id, tail);
        if (col < cols - 1) tail = visit(i + 1, id, tail);
        if (row > 0) tail = visit(i - cols, id, tail);
        if (row < rows - 1) tail = visit(i + cols, id, tail);
        return tail;
    }

    private int visit(int j, int id, int tail) {
        if (!conducts[j] || network[j] >= 0) return tail;
        network[j] = id;
        queue[tail++] = j;
        return tail;
    }

    /** The {@code size} tiles of network {@code id}, ordered by their distance from its nearest source. */
    private int[] breadthFromSources(int id, int size) {
        int[] members = Arrays.copyOf(queue, size);
        int[] order = new int[size];
        boolean[] seen = new boolean[cols * rows];
        int head = 0;
        int tail = 0;
        for (int i : members) {
            if (sources[i] > 0) {
                order[tail++] = i;
                seen[i] = true;
            }
        }
        while (head < tail) {
            int i = order[head++];
            int col = i % cols;
            int row = i / cols;
            int[] next = {col > 0 ? i - 1 : -1, col < cols - 1 ? i + 1 : -1, row > 0 ? i - cols : -1,
                    row < rows - 1 ? i + cols : -1};
            for (int j : next) {
                if (j < 0 || seen[j] || network[j] != id) continue;
                seen[j] = true;
                order[tail++] = j;
            }
        }
        return order;
    }

    /** True if the load on a tile is served; false for a tile without a load. */
    public boolean supplied(int col, int row) {
        return contains(col, row) && supplied[row * cols + col];
    }

    /** True if a tile conducts and its network has a source, whether or not that source suffices. */
    public boolean connected(int col, int row) {
        if (!contains(col, row)) return false;
        int id = network[row * cols + col];
        return id >= 0 && id < fed;
    }

    /** The network a tile belongs to, or -1 if it does not conduct. */
    public int network(int col, int row) {
        return contains(col, row) ? network[row * cols + col] : -1;
    }

    public boolean conducts(int col, int row) {
        return contains(col, row) && conducts[row * cols + col];
    }

    /** What all sources put in, all loads ask for and the loads served take, after {@link #solve()}. */
    public double supply() {
        return supply;
    }

    public double demand() {
        return demand;
    }

    public double served() {
        return served;
    }

    /** How many networks there are, with or without a source. */
    public int networks() {
        return networks;
    }

    public boolean contains(int col, int row) {
        return col >= 0 && row >= 0 && col < cols && row < rows;
    }
}
