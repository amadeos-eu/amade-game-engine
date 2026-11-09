package eu.amadeos.gameengine.drive;

import java.util.function.Consumer;

/**
 * Adds a {@link Road} section by section: so many segments that bend so much and climb so high. A section
 * eases in and out, so a curve grows from straight to its sharpest in its middle and straightens again, and
 * a hill starts and ends flat; sections can then follow one another in any order without a kink.
 *
 * <pre>
 * RoadBuilder b = new RoadBuilder(road, look);
 * b.straight(20);
 * b.section(60, 3.2, 1600, s -&gt; s.add(new Scenery(pine, -2.5, 0.3)));   // a right bend up a hill
 * </pre>
 */
// --8<-- [start:builder]
public final class RoadBuilder {
    private final Road road;
    private RoadLook look;

    public RoadBuilder(Road road, RoadLook look) {
        this.road = road;
        this.look = look;
    }

    /** The look of the segments added from now on. */
    public RoadBuilder look(RoadLook look) {
        this.look = look;
        return this;
    }

    public RoadLook look() {
        return look;
    }

    /**
     * Adds {@code count} segments. The curve rises to {@code curve} in the middle of the section and falls back
     * to 0 (a sine); the height changes by {@code rise} along an S (a cosine), so the section starts and ends
     * level. {@code each} may dress every new segment.
     */
    public RoadBuilder section(int count, double curve, double rise, Consumer<Segment> each) {
        double y0 = road.end() == 0 ? 0 : road.segment(road.end() - 1).y;
        for (int i = 0; i < count; i++) {
            double t = (i + 1.0) / count;
            Segment s = road.add(look, curve * Math.sin(Math.PI * (i + 0.5) / count),
                    y0 + rise * (0.5 - 0.5 * Math.cos(Math.PI * t)));
            if (each != null) each.accept(s);
        }
        return this;
    }

    public RoadBuilder section(int count, double curve, double rise) {
        return section(count, curve, rise, null);
    }

    public RoadBuilder straight(int count) {
        return section(count, 0, 0, null);
    }
    // --8<-- [end:builder]

    public Road road() {
        return road;
    }
}
