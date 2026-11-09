package eu.amadeos.gameengine.drive;

import java.util.ArrayList;

/**
 * A road as a row of {@link Segment}s, endless if the game wants it: new segments are added at the far end
 * while the car drives, and those far behind it are dropped, so a journey of hundreds of kilometres takes as
 * little memory as one of a few. Segments keep their number ({@link Segment#index()}) for as long as they
 * exist; a position on the road is a distance {@code z} from its start.
 *
 * <p>Usually a {@link RoadBuilder} adds the segments, section by section.
 */
// --8<-- [start:road]
public final class Road {
    /** The length of a segment, in world units. */
    public static final double SEGMENT = 200;
    /** Half the width of the road (two lanes), in world units: the unit of all sideways positions. */
    public static final double HALF_WIDTH = 1100;

    private final ArrayList<Segment> segments = new ArrayList<>();
    private long first;
    private long next;
    private double lastY;
    // --8<-- [end:road]

    /** Adds a segment at the far end; its near end lies where the last one ended, its far end at height {@code y}. */
    public Segment add(RoadLook look, double curve, double y) {
        Segment s = new Segment(next++, lastY, look);
        segments.add(s);
        s.curve = curve;
        s.y = y;
        lastY = y;
        return s;
    }

    /** The segment with this number; the last one if the road does not reach that far, the first if it is gone. */
    public Segment segment(long n) {
        if (segments.isEmpty()) throw new IllegalStateException("the road has no segments yet");
        long i = Math.max(first, Math.min(next - 1, n));
        return segments.get((int) (i - first));
    }

    /** The segment at distance {@code z} from the start. */
    public Segment at(double z) {
        return segment((long) Math.floor(z / SEGMENT));
    }

    /** True if the segment with this number exists (not yet dropped, already added). */
    public boolean has(long n) {
        return n >= first && n < next;
    }

    /** The number of the first segment still there. */
    public long first() {
        return first;
    }

    /** One more than the number of the last segment: how many segments have ever been added. */
    public long end() {
        return next;
    }

    /** Where the road ends now, in units from the start. */
    public double length() {
        return next * SEGMENT;
    }

    /** The height of the road at distance {@code z}, between the heights of the segment's two ends. */
    public double heightAt(double z) {
        Segment s = at(z);
        double t = Math.max(0, Math.min(1, z / SEGMENT - s.index));
        return s.startY + (s.y - s.startY) * t;
    }

    /** Drops the segments before number {@code n}; they are behind every camera. */
    public void dropBefore(long n) {
        int drop = (int) Math.max(0, Math.min(n - first, segments.size() - 1L));
        if (drop == 0) return;
        segments.subList(0, drop).clear();
        first += drop;
    }

    /** Number of segments held now. */
    public int size() {
        return segments.size();
    }
}
