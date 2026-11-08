package eu.amadeos.gameengine.drive;

/**
 * Something that stands beside the road in one {@link Segment}: a tree, a rock, a house, a sign. {@code offset}
 * is how far from the centre of the road, in road half-widths (negative is left; the road's edge is at ±1),
 * {@code along} where in the segment, from 0 at its near end to 1 at its far end. A flipped picture is drawn
 * mirrored, so one tree can stand as two different ones.
 */
// --8<-- [start:scenery]
public record Scenery(RoadSprite sprite, double offset, double along, boolean flipped) {
    public Scenery(RoadSprite sprite, double offset, double along) {
        this(sprite, offset, along, false);
    }
}
// --8<-- [end:scenery]
