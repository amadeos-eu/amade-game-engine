package eu.amadeos.gameengine.raycast.model;

/**
 * The look of a part of a {@link Model}: its colour, how much it shines (0 = rubber, 1 = polished metal)
 * and whether it glows by itself (lamps, eyes, flames), which no light or shadow changes.
 */
public record Material(int rgb, double shine, double glow) {
    /** A dull, painted surface. */
    public static Material paint(int rgb) {
        return new Material(rgb, 0.3, 0);
    }

    /** Bare metal, with bright highlights. */
    public static Material metal(int rgb) {
        return new Material(rgb, 0.8, 0);
    }

    /** Rubber, cloth, leather: no highlights. */
    public static Material matte(int rgb) {
        return new Material(rgb, 0.08, 0);
    }

    /** Something that shines by itself. */
    public static Material light(int rgb) {
        return new Material(rgb, 0, 1);
    }

    double red() {
        return ((rgb >> 16) & 255) / 255.0;
    }

    double green() {
        return ((rgb >> 8) & 255) / 255.0;
    }

    double blue() {
        return (rgb & 255) / 255.0;
    }
}
