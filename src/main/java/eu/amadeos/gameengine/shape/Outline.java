package eu.amadeos.gameengine.shape;

import java.awt.Graphics2D;
import java.awt.geom.Path2D;

/**
 * A figure of straight lines, given by its corners around its own centre, which can be drawn anywhere,
 * turned by any angle and at any size: a ship, a rock. That is how the vector screens of the late 1970s drew
 * everything, and it suits a game where things turn, because turning a figure of lines costs a handful of
 * multiplications per corner, while turning a picture means working through all its pixels.
 *
 * <p>The outline keeps one {@link Path2D} and fills it anew for each drawing, so drawing creates no objects.
 * Use an outline on one thread only, the game loop's.
 *
 * <pre>
 * Outline ship = Outline.closed(12, 0,  -8, -7,  -5, 0,  -8, 7);   // nose to the right
 * ship.draw(g, x, y, angle, 1);
 * </pre>
 */
public final class Outline {
    private final double[] xs;
    private final double[] ys;
    private final boolean closed;
    private final double radius;
    private final Path2D.Double path = new Path2D.Double();

    private Outline(double[] xy, boolean closed) {
        if (xy.length < 4 || xy.length % 2 != 0) throw new IllegalArgumentException("needs pairs of x and y, at least two");
        xs = new double[xy.length / 2];
        ys = new double[xy.length / 2];
        double r = 0;
        for (int i = 0; i < xs.length; i++) {
            xs[i] = xy[2 * i];
            ys[i] = xy[2 * i + 1];
            r = Math.max(r, Math.hypot(xs[i], ys[i]));
        }
        this.closed = closed;
        this.radius = r;
    }

    /** A closed figure: the last corner is joined to the first. Pairs of x and y, angle 0 pointing right. */
    public static Outline closed(double... xy) {
        return new Outline(xy, true);
    }

    /** An open line through the points, e.g. a flame behind a ship. */
    public static Outline open(double... xy) {
        return new Outline(xy, false);
    }

    /** The farthest corner from the centre: a circle of this radius holds the whole figure, at any angle. */
    public double radius() {
        return radius;
    }

    public int corners() {
        return xs.length;
    }

    // --8<-- [start:path]
    /**
     * The figure turned by {@code angle}, enlarged by {@code scale} and moved to {@code x}, {@code y}. Every
     * corner is turned with the same formula as {@link eu.amadeos.gameengine.math.Vector#rotated}; sine and
     * cosine are worked out once for all corners. The path is reused: it is valid until the next call.
     */
    public Path2D.Double path(double x, double y, double angle, double scale) {
        double cos = Math.cos(angle) * scale;
        double sin = Math.sin(angle) * scale;
        path.reset();
        for (int i = 0; i < xs.length; i++) {
            double px = x + xs[i] * cos - ys[i] * sin;
            double py = y + xs[i] * sin + ys[i] * cos;
            if (i == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        if (closed) path.closePath();
        return path;
    }
    // --8<-- [end:path]

    /** Draws the lines with the graphics' current colour and stroke. */
    public void draw(Graphics2D g, double x, double y, double angle, double scale) {
        g.draw(path(x, y, angle, scale));
    }

    /** Fills the figure with the graphics' current paint. */
    public void fill(Graphics2D g, double x, double y, double angle, double scale) {
        g.fill(path(x, y, angle, scale));
    }
}
