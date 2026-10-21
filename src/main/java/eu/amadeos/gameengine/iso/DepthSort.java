package eu.amadeos.gameengine.iso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * The order in which the things of an isometric view are drawn, so that what stands in front covers what
 * stands behind: the painter's algorithm, for things of any size.
 *
 * <p>For things of one tile, sorting by {@code x + y} would do. A building of three tiles by two does not
 * fit that rule: a tree on the tile behind its right end has a larger {@code x + y} than the building's
 * middle and would be drawn over its wall. So each thing is a <i>footprint</i>, a rectangle of the ground,
 * and the rule is the one of the ground: A lies behind B if A ends before B begins along {@code x}, or along
 * {@code y}. Viewed from the corner of larger {@code x} and {@code y}, that is all it takes.
 *
 * <p>That rule only orders things that touch in the picture, so the sort compares only things whose boxes in
 * the picture overlap (a sweep along the picture's {@code x}), and draws them in the order of a topological
 * sort, the nearest {@code x + y} first among those that are free. Things that are behind each other both
 * ways, a cycle that cannot be drawn right, are drawn by {@code x + y}.
 *
 * <pre>
 * DepthSort&lt;Building&gt; sort = new DepthSort&lt;&gt;();
 * for (Building b : visible) sort.add(b.x0(), b.y0(), b.x1(), b.y1(), left, top, right, bottom, b);
 * for (Building b : sort.sorted()) draw(b);   // empties the sort for the next frame
 * </pre>
 *
 * <p>The footprints are in world units; the boxes in the picture's units. Use it on one thread.
 */
// --8<-- [start:sort]
public final class DepthSort<T> {
    private static final class Item<T> {
        double x0, y0, x1, y1;
        double left, top, right, bottom;
        double key;
        int index;
        T thing;
        int[] after = new int[4];   // the items that must be drawn after this one
        int afterCount;
        int before;                 // how many items must be drawn before this one
    }

    private final List<Item<T>> items = new ArrayList<>();
    private final List<Item<T>> spare = new ArrayList<>();
    private Item<T>[] byLeft;
    private final List<T> order = new ArrayList<>();
    private final PriorityQueue<Item<T>> free =
            new PriorityQueue<>(Comparator.<Item<T>>comparingDouble(i -> i.key).thenComparingInt(i -> i.index));

    /**
     * A thing with its footprint from ({@code x0}, {@code y0}) to ({@code x1}, {@code y1}) in world units,
     * and the box it covers in the picture, from {@code left}, {@code top} to {@code right}, {@code bottom}.
     */
    public void add(double x0, double y0, double x1, double y1, double left, double top, double right,
                    double bottom, T thing) {
        Item<T> item = spare.isEmpty() ? new Item<>() : spare.removeLast();
        item.x0 = x0;
        item.y0 = y0;
        item.x1 = x1;
        item.y1 = y1;
        item.left = left;
        item.top = top;
        item.right = right;
        item.bottom = bottom;
        item.key = x0 + x1 + y0 + y1;
        item.index = items.size();
        item.thing = thing;
        item.afterCount = 0;
        item.before = 0;
        items.add(item);
    }

    /** How many things wait to be sorted. */
    public int size() {
        return items.size();
    }

    /** True if footprint A must be drawn before footprint B: A ends before B begins along x or along y. */
    public static boolean behind(double ax0, double ay0, double ax1, double ay1,
                                 double bx0, double by0, double bx1, double by1) {
        boolean aFirst = ax1 <= bx0 || ay1 <= by0;
        boolean bFirst = bx1 <= ax0 || by1 <= ay0;
        return aFirst && !bFirst;
    }

    /**
     * The things in the order to draw them, back to front. The sort is empty afterwards; the list is reused
     * by the next call.
     */
    @SuppressWarnings("unchecked")
    public List<T> sorted() {
        order.clear();
        int n = items.size();
        if (byLeft == null || byLeft.length < n) byLeft = new Item[Math.max(16, n * 2)];
        for (int i = 0; i < n; i++) byLeft[i] = items.get(i);
        Arrays.sort(byLeft, 0, n, Comparator.comparingDouble(i -> i.left));
        // Only things whose boxes overlap in the picture can cover each other.
        for (int i = 0; i < n; i++) {
            Item<T> a = byLeft[i];
            for (int j = i + 1; j < n && byLeft[j].left < a.right; j++) {
                Item<T> b = byLeft[j];
                if (b.top >= a.bottom || a.top >= b.bottom) continue;
                if (behind(a.x0, a.y0, a.x1, a.y1, b.x0, b.y0, b.x1, b.y1)) link(a, b);
                else if (behind(b.x0, b.y0, b.x1, b.y1, a.x0, a.y0, a.x1, a.y1)) link(b, a);
            }
        }
        for (Item<T> item : items) if (item.before == 0) free.add(item);
        boolean[] done = new boolean[n];
        int drawn = 0;
        while (drawn < n) {
            Item<T> next = free.poll();
            if (next == null) {
                // A cycle: draw the nearest-to-the-back of what is left and go on.
                for (Item<T> item : items) {
                    if (!done[item.index] && (next == null || item.key < next.key)) next = item;
                }
            }
            if (done[next.index]) continue;
            done[next.index] = true;
            drawn++;
            order.add(next.thing);
            for (int k = 0; k < next.afterCount; k++) {
                Item<T> later = items.get(next.after[k]);
                if (--later.before == 0 && !done[later.index]) free.add(later);
            }
        }
        free.clear();
        for (Item<T> item : items) item.thing = null;
        spare.addAll(items);
        items.clear();
        return order;
    }
    // --8<-- [end:sort]

    private static <T> void link(Item<T> first, Item<T> then) {
        if (first.afterCount == first.after.length) first.after = Arrays.copyOf(first.after, first.after.length * 2);
        first.after[first.afterCount++] = then.index;
        then.before++;
    }
}
