package eu.amadeos.gameengine.adventure;

import eu.amadeos.gameengine.math.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Where people can walk in a room: the floor as a polygon, with holes for the table, the well or the sleeping
 * dog in the middle of it. A click anywhere asks for a way there; a click outside the floor walks to the
 * nearest point on it.
 *
 * <p>Rooms of an adventure are pictures, not tiles, so the way is not searched on a grid. The shortest way
 * round obstacles in the plane is a string pulled tight: it runs straight from the start to a corner of an
 * obstacle, from corner to corner, and straight on to the goal. Only two kinds of corners can be on it: the
 * corners of the floor that point <i>into</i> the room (reflex corners, where the wall bends inwards), and the
 * corners of the holes. So the search needs only those corners. Two of them are joined when one can see the
 * other, and {@link #path} looks for the shortest way through this <b>visibility graph</b> with A*, from the
 * start to the goal, guessed by the straight distance.
 *
 * <p>Points on the edge of the floor count as inside: the string runs along the edge where it touches it.
 * Corners are joined once, when the area is made; each search adds only the start and the goal.
 */
public final class WalkArea {
    private static final double EPSILON = 1e-6;

    private final List<Vector> outline;
    private final List<List<Vector>> holes;
    /** The corners a shortest way can bend at. */
    private final List<Vector> corners;
    /** Which corners see each other: {@code sees[i][j]}. */
    private final boolean[][] sees;

    /**
     * @param outline the floor, as corners in order round it, either way round, at least three
     * @param holes   places inside the floor where one cannot go, each given the same way
     */
    public WalkArea(List<Vector> outline, List<List<Vector>> holes) {
        if (outline.size() < 3) throw new IllegalArgumentException("a floor needs at least 3 corners");
        for (List<Vector> hole : holes)
            if (hole.size() < 3) throw new IllegalArgumentException("a hole needs at least 3 corners");
        this.outline = List.copyOf(outline);
        this.holes = holes.stream().map(List::copyOf).toList();
        List<Vector> bends = new ArrayList<>();
        double turn = Math.signum(area(this.outline));
        for (int i = 0; i < this.outline.size(); i++)
            if (cross(this.outline, i) * turn < -EPSILON) bends.add(this.outline.get(i));
        for (List<Vector> hole : this.holes) {
            double holeTurn = Math.signum(area(hole));
            for (int i = 0; i < hole.size(); i++)
                if (cross(hole, i) * holeTurn > EPSILON) bends.add(hole.get(i));
        }
        this.corners = List.copyOf(bends);
        int n = corners.size();
        sees = new boolean[n][n];
        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++)
                sees[i][j] = sees[j][i] = sees(corners.get(i), corners.get(j));
    }

    /** A floor without holes. */
    public WalkArea(List<Vector> outline) {
        this(outline, List.of());
    }

    public List<Vector> outline() {
        return outline;
    }

    public List<List<Vector>> holes() {
        return holes;
    }

    /** Whether {@code p} is on the floor: inside the outline or on it, and in no hole. */
    public boolean contains(Vector p) {
        if (onEdge(outline, p)) return true;
        if (!inside(outline, p)) return false;
        for (List<Vector> hole : holes)
            if (!onEdge(hole, p) && inside(hole, p)) return false;
        return true;
    }

    /** {@code p} itself if it is on the floor, otherwise the nearest point of the floor's edges. */
    public Vector nearest(Vector p) {
        if (contains(p)) return p;
        Vector best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (List<Vector> ring : rings()) {
            for (int i = 0; i < ring.size(); i++) {
                Vector q = closest(ring.get(i), ring.get((i + 1) % ring.size()), p);
                double d = q.distance(p);
                if (d < bestDistance && contains(q)) {
                    bestDistance = d;
                    best = q;
                }
            }
        }
        return best == null ? outline.getFirst() : best;
    }

    /** Whether one can walk straight from {@code a} to {@code b} without leaving the floor. */
    public boolean sees(Vector a, Vector b) {
        if (a.distance(b) < EPSILON) return contains(a);
        for (List<Vector> ring : rings())
            for (int i = 0; i < ring.size(); i++)
                if (crosses(a, b, ring.get(i), ring.get((i + 1) % ring.size()))) return false;
        // No edge is crossed, but the line could still run outside along a corner or across a hole that it
        // only touches at corners: points along it must lie on the floor.
        for (int k = 1; k < 8; k++)
            if (!contains(a.plus(b.minus(a).times(k / 8.0)))) return false;
        return true;
    }

    // --8<-- [start:path]
    /**
     * The shortest way on the floor from {@code from} to {@code to}: the points to walk to, one after the
     * other, the first being {@code from} and the last {@code to}. Points off the floor are first moved to
     * the nearest point on it. Without a way, for a goal in a part of the floor one cannot reach, the way
     * leads to the reachable corner nearest to the goal.
     */
    public List<Vector> path(Vector from, Vector to) {
        Vector start = nearest(from);
        Vector goal = nearest(to);
        if (sees(start, goal)) return List.of(start, goal);
        int n = corners.size();
        // Nodes 0..n-1 are the corners, n is the start, n + 1 the goal.
        boolean[] fromStart = new boolean[n];
        boolean[] toGoal = new boolean[n];
        for (int i = 0; i < n; i++) {
            fromStart[i] = sees(start, corners.get(i));
            toGoal[i] = sees(corners.get(i), goal);
        }
        double[] g = new double[n + 2];
        Arrays.fill(g, Double.POSITIVE_INFINITY);
        int[] cameFrom = new int[n + 2];
        boolean[] closed = new boolean[n + 2];
        record Open(int node, double f) {
        }
        PriorityQueue<Open> border = new PriorityQueue<>((a, b) -> Double.compare(a.f(), b.f()));
        g[n] = 0;
        border.add(new Open(n, start.distance(goal)));
        while (!border.isEmpty()) {
            int node = border.poll().node();
            if (closed[node]) continue;
            closed[node] = true;
            if (node == n + 1) return way(cameFrom, start, goal);
            Vector here = node == n ? start : corners.get(node);
            for (int next = 0; next < n + 2; next++) {
                if (next == n || closed[next] || !linked(node, next, fromStart, toGoal)) continue;
                Vector there = next == n + 1 ? goal : corners.get(next);
                double through = g[node] + here.distance(there);
                if (through >= g[next]) continue;
                g[next] = through;
                cameFrom[next] = node;
                border.add(new Open(next, through + there.distance(goal)));
            }
        }
        return towardsUnreachable(start, goal, g, cameFrom);
    }
    // --8<-- [end:path]

    /** Whether two nodes of the graph see each other; the start is node n, the goal node n + 1. */
    private boolean linked(int a, int b, boolean[] fromStart, boolean[] toGoal) {
        int n = corners.size();
        if (a == n) return b == n + 1 ? false : fromStart[b];
        if (b == n + 1) return toGoal[a];
        return sees[a][b];
    }

    private List<Vector> way(int[] cameFrom, Vector start, Vector goal) {
        int n = corners.size();
        List<Vector> points = new ArrayList<>();
        points.add(goal);
        for (int node = cameFrom[n + 1]; node != n; node = cameFrom[node]) points.add(corners.get(node));
        points.add(start);
        Collections.reverse(points);
        return List.copyOf(points);
    }

    /** No way to the goal: walk to the reached corner that is nearest to it, or stay. */
    private List<Vector> towardsUnreachable(Vector start, Vector goal, double[] g, int[] cameFrom) {
        int n = corners.size();
        int best = -1;
        for (int i = 0; i < n; i++)
            if (g[i] < Double.POSITIVE_INFINITY
                    && (best < 0 || corners.get(i).distance(goal) < corners.get(best).distance(goal))) best = i;
        if (best < 0) return List.of(start);
        List<Vector> points = new ArrayList<>();
        for (int node = best; node != n; node = cameFrom[node]) points.add(corners.get(node));
        points.add(start);
        Collections.reverse(points);
        return List.copyOf(points);
    }

    private List<List<Vector>> rings() {
        List<List<Vector>> rings = new ArrayList<>(holes.size() + 1);
        rings.add(outline);
        rings.addAll(holes);
        return rings;
    }

    /** Twice the signed area: positive when the corners run clockwise on the screen (y down). */
    static double area(List<Vector> ring) {
        double sum = 0;
        for (int i = 0; i < ring.size(); i++) {
            Vector a = ring.get(i), b = ring.get((i + 1) % ring.size());
            sum += a.x() * b.y() - b.x() * a.y();
        }
        return sum;
    }

    /** The turn at corner i: positive if the ring bends the same way as a clockwise ring. */
    private static double cross(List<Vector> ring, int i) {
        Vector prev = ring.get((i + ring.size() - 1) % ring.size());
        Vector here = ring.get(i);
        Vector next = ring.get((i + 1) % ring.size());
        Vector in = here.minus(prev), out = next.minus(here);
        return in.x() * out.y() - in.y() * out.x();
    }

    /** Even-odd rule: a ray to the right crosses the edges of a ring an odd number of times from inside. */
    private static boolean inside(List<Vector> ring, Vector p) {
        boolean in = false;
        for (int i = 0, j = ring.size() - 1; i < ring.size(); j = i++) {
            Vector a = ring.get(i), b = ring.get(j);
            if ((a.y() > p.y()) != (b.y() > p.y())
                    && p.x() < (b.x() - a.x()) * (p.y() - a.y()) / (b.y() - a.y()) + a.x()) in = !in;
        }
        return in;
    }

    private static boolean onEdge(List<Vector> ring, Vector p) {
        for (int i = 0; i < ring.size(); i++)
            if (closest(ring.get(i), ring.get((i + 1) % ring.size()), p).distance(p) < EPSILON) return true;
        return false;
    }

    /** The point of the segment a-b nearest to p. */
    static Vector closest(Vector a, Vector b, Vector p) {
        Vector ab = b.minus(a);
        double length = ab.lengthSquared();
        if (length == 0) return a;
        double t = Math.max(0, Math.min(1, p.minus(a).dot(ab) / length));
        return a.plus(ab.times(t));
    }

    /** Whether segments a-b and c-d cross each other properly: not just touching at an end or a corner. */
    private static boolean crosses(Vector a, Vector b, Vector c, Vector d) {
        double d1 = side(c, d, a), d2 = side(c, d, b), d3 = side(a, b, c), d4 = side(a, b, d);
        return d1 * d2 < -EPSILON && d3 * d4 < -EPSILON;
    }

    private static double side(Vector a, Vector b, Vector p) {
        return (b.x() - a.x()) * (p.y() - a.y()) - (b.y() - a.y()) * (p.x() - a.x());
    }
}
