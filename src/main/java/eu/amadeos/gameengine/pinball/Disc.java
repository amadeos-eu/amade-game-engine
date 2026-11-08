package eu.amadeos.gameengine.pinball;

/**
 * A round part the ball bounces off: a post with its rubber ring, a pop bumper, the round end of a guide.
 * A bumper is a disc of {@link Material#BUMPER}, which kicks.
 */
public final class Disc {
    public final String id;
    public final double x, y, radius;
    public final Material material;
    public final int layer;
    public boolean down;

    public Disc(String id, double x, double y, double radius, Material material, int layer) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.material = material;
        this.layer = layer;
    }
}
