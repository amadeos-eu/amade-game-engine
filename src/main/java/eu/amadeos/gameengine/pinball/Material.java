package eu.amadeos.gameengine.pinball;

/**
 * What a part of the table is made of, as far as the ball is concerned: how much of its speed the ball keeps
 * when it bounces off ({@code bounce}, 0 to 1), how much of its sliding speed along the surface it loses
 * ({@code grip}, 0 to 1), and how hard the part kicks back by itself ({@code kick}, in units per second added
 * along the normal). A pop bumper and a slingshot kick; a wall does not.
 *
 * <p>A kick fires only when the ball hits harder than {@code kickAbove} units per second: a ball resting
 * against a slingshot does not make it fire again and again.
 */
// --8<-- [start:material]
public record Material(double bounce, double grip, double kick, double kickAbove) {
    /** Painted wood or metal walls: a dull bounce. */
    public static final Material WALL = new Material(0.45, 0.02, 0, 0);
    /** Rubber rings round posts and on the guides: lively. */
    public static final Material RUBBER = new Material(0.72, 0.06, 0, 0);
    /** Plastic and metal targets. */
    public static final Material TARGET = new Material(0.35, 0.03, 0, 0);
    /** The rubber of a flipper. */
    public static final Material FLIPPER = new Material(0.55, 0.05, 0, 0);
    /** A pop bumper: the ring pulls the ball down and throws it away. */
    public static final Material BUMPER = new Material(0.6, 0.02, 520, 25);
    /** The kicking side of a slingshot. */
    public static final Material SLING = new Material(0.6, 0.02, 430, 60);
    // --8<-- [end:material]

    public Material {
        if (bounce < 0 || bounce > 1 || grip < 0 || grip > 1 || kick < 0 || kickAbove < 0) {
            throw new IllegalArgumentException("bad material " + bounce + " " + grip + " " + kick);
        }
    }
}
