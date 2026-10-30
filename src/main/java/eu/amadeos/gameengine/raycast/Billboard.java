package eu.amadeos.gameengine.raycast;

/**
 * A thing in the {@link Raycaster}'s world drawn as a flat picture that always faces the eye: a machine, a
 * pickup, a spark. Pre-rendered from several directions ({@link Views}), it still looks solid as one walks
 * round it. The picture's bottom stands {@code lift} above the floor; it is {@code size} high and wide.
 */
// --8<-- [start:billboard]
public final class Billboard {
    public double x;
    public double y;
    /** Height of the picture's bottom above the floor; negative lets a model's empty bottom sink below it. */
    public double lift;
    public double size;
    /** The angle the thing faces, for picking the view. */
    public double facing;
    public int frame;
    /** 0 = as lit by the room, 1 = all white: a hit flashes the thing for a moment. */
    public double flash;
    /** A shadow on the floor under the thing, as wide as this part of {@link #size}; 0 for none. */
    public double shadow;
    public boolean visible = true;
    public Views views;

    public Billboard(Views views, double x, double y, double size) {
        this.views = views;
        this.x = x;
        this.y = y;
        this.size = size;
    }
}
// --8<-- [end:billboard]
