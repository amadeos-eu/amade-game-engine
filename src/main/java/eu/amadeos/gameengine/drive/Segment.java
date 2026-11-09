package eu.amadeos.gameengine.drive;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * One short piece of a {@link Road}, {@link Road#SEGMENT} units long: the unit the road is drawn in. A segment
 * knows how much the road bends here ({@link #curve}), how high its far end lies ({@link #y}), how it looks,
 * and what stands beside it: scenery, walls, water. A curve is not a turn of the road in the world; it is how
 * far the next segment is pushed sideways on the screen. Added up from near to far, these pushes bend the
 * picture of a straight road into a curve, the trick of the road games of the 1980s.
 */
// --8<-- [start:segment]
public final class Segment {
    final long index;
    final double startY;
    /** How much the road bends here: negative to the left, positive to the right; 0 straight, about 6 is sharp. */
    public double curve;
    /** The height of the far end, in world units; the near end has the height of the segment before. */
    public double y;
    public RoadLook look;
    /** What stands beside the road here: trees, houses, signs. */
    public final List<Scenery> scenery = new ArrayList<>();
    /** A wall beside the road, or {@code null}: a cliff, a row of houses, the wall of a tunnel. */
    public Wall left;
    public Wall right;
    /** A ceiling between the two walls: the segment lies in a tunnel. Needs both walls. */
    public boolean tunnel;
    /** The face of a mountain with the tunnel's mouth, drawn at the near end of this segment, in these colours. */
    public Color[] portal;
    /** Water from this many road half-widths left of the centre outwards; 0 for none. */
    public double waterLeft;
    public double waterRight;
    // --8<-- [end:segment]

    Segment(long index, double startY, RoadLook look) {
        this.index = index;
        this.startY = startY;
        this.y = startY;
        this.look = look;
    }

    /** The segment's number, counted from the start of the road. */
    public long index() {
        return index;
    }

    /** The height of the near end: that of the segment before. */
    public double startY() {
        return startY;
    }

    /** Where the segment begins, in units from the start of the road. */
    public double z() {
        return index * Road.SEGMENT;
    }

    public Segment add(Scenery s) {
        scenery.add(s);
        return this;
    }
}
