package eu.amadeos.gameengine.pinball;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * The view of a table from the player's end: the flat table, {@code width} × {@code height} units, seen
 * tilted away, so that its far end is narrower than its near end. On the screen the table fills a
 * trapezoid, the {@code quad} {top left, top right, bottom right, bottom left}.
 *
 * <p>This is a real perspective projection (a <i>homography</i>), not a squeeze: lines on the table stay
 * straight, but equal steps up the table get shorter towards the far end, as they do for the eye. It maps
 * a table point to the screen with {@link #view} and back with {@link #table}, both as three-by-three
 * matrices in homogeneous coordinates.
 *
 * <p>Heights are drawn the way the flat pictures of the parts draw them: a point {@code h} units above the
 * table at {@code (x, y)} appears where the table point {@code (x, y - h)} does. Looking down at a slant,
 * height and depth both move a point up the screen, so this simple rule is close enough and keeps the
 * pictures and the projection in step.
 */
public final class Perspective {
    private final double width;
    private final double height;
    private final double[] quad;
    private final double[][] m;
    private final double[][] inv;

    /** The table {@code width} × {@code height} units, seen in {@code quad}: x0 y0 (top left) … x3 y3 (bottom left). */
    public Perspective(double width, double height, double[] quad) {
        if (quad.length != 8) throw new IllegalArgumentException("a quad has four corners");
        this.width = width;
        this.height = height;
        this.quad = quad.clone();
        this.m = squareToQuad(quad);
        this.inv = invert(m);
    }

    /** A symmetric trapezoid: centred at {@code cx}, {@code top} to {@code bottom}, {@code topWidth} and {@code bottomWidth} wide. */
    public static double[] trapezoid(double cx, double top, double bottom, double topWidth, double bottomWidth) {
        return new double[]{cx - topWidth / 2, top, cx + topWidth / 2, top, cx + bottomWidth / 2, bottom,
                cx - bottomWidth / 2, bottom};
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    public double[] quad() {
        return quad.clone();
    }

    // --8<-- [start:view]
    /** Where the table point {@code (x, y)} appears on the screen. */
    public Point2D.Double view(double x, double y) {
        double u = x / width, v = y / height;
        double w = m[2][0] * u + m[2][1] * v + m[2][2];
        return new Point2D.Double((m[0][0] * u + m[0][1] * v + m[0][2]) / w, (m[1][0] * u + m[1][1] * v + m[1][2]) / w);
    }
    // --8<-- [end:view]

    /** Where a point {@code h} units above the table point {@code (x, y)} appears. */
    public Point2D.Double view(double x, double y, double h) {
        return view(x, y - h);
    }

    /** The table point seen at the screen point {@code (sx, sy)}. */
    public Point2D.Double table(double sx, double sy) {
        double w = inv[2][0] * sx + inv[2][1] * sy + inv[2][2];
        double u = (inv[0][0] * sx + inv[0][1] * sy + inv[0][2]) / w;
        double v = (inv[1][0] * sx + inv[1][1] * sy + inv[1][2]) / w;
        return new Point2D.Double(u * width, v * height);
    }

    /** How many screen units one table unit across is at {@code (x, y)}: things far up the table are smaller. */
    public double scale(double x, double y) {
        Point2D.Double a = view(x - 0.5, y), b = view(x + 0.5, y);
        return a.distance(b);
    }

    // --8<-- [start:local]
    /**
     * The perspective near {@code (x, y)}, straightened into an affine transform from table units to the
     * screen. Small moving parts (a flipper, a drop target) are drawn through it in their flat table
     * coordinates, just like the parts baked into the table picture.
     */
    public AffineTransform local(double x, double y) {
        Point2D.Double o = view(x, y), ex = view(x + 1, y), ey = view(x, y + 1);
        return new AffineTransform(ex.x - o.x, ex.y - o.y, ey.x - o.x, ey.y - o.y, o.x - x * (ex.x - o.x) - y * (ey.x - o.x),
                o.y - x * (ex.y - o.y) - y * (ey.y - o.y));
    }
    // --8<-- [end:local]

    // --8<-- [start:warp]
    /**
     * Projects a flat picture of the table onto {@code dst}. The picture covers the table rectangle
     * {@code left, top} to {@code right, bottom} (in table units; it may reach past the table's edges, where
     * raised parts stick out); {@code toPixels} turns screen units into pixels of {@code dst}. Each pixel of
     * the trapezoid is looked up backwards in the picture, four samples each, blended by their alpha over
     * what {@code dst} has there.
     */
    public void warp(BufferedImage flat, double left, double top, double right, double bottom, BufferedImage dst,
                     AffineTransform toPixels) {
        int sw = flat.getWidth(), sh = flat.getHeight();
        int[] src = flat.getRGB(0, 0, sw, sh, null, 0, sw);
        int dw = dst.getWidth(), dh = dst.getHeight();
        int[] out = dst.getRGB(0, 0, dw, dh, null, 0, dw);
        AffineTransform fromPixels;
        try {
            fromPixels = toPixels.createInverse();
        } catch (java.awt.geom.NoninvertibleTransformException e) {
            throw new IllegalArgumentException(e);
        }
        // the corners of the picture's rectangle, to bound the work
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (double[] c : new double[][]{{left, top}, {right, top}, {right, bottom}, {left, bottom}}) {
            Point2D p = toPixels.transform(view(c[0], c[1]), null);
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
        }
        double sx = sw / (right - left), sy = sh / (bottom - top);
        double[] pt = new double[2];
        for (int py = Math.max(0, (int) minY); py < Math.min(dh, (int) Math.ceil(maxY)); py++) {
            for (int px = Math.max(0, (int) minX); px < Math.min(dw, (int) Math.ceil(maxX)); px++) {
                double r = 0, g = 0, b = 0, a = 0;
                for (int k = 0; k < 4; k++) {
                    pt[0] = px + 0.25 + (k & 1) * 0.5;
                    pt[1] = py + 0.25 + (k >> 1) * 0.5;
                    fromPixels.transform(pt, 0, pt, 0, 1);
                    Point2D.Double t = table(pt[0], pt[1]);
                    double fx = (t.x - left) * sx - 0.5, fy = (t.y - top) * sy - 0.5;
                    if (fx < 0 || fy < 0 || fx >= sw - 1 || fy >= sh - 1) continue;
                    int ix = (int) fx, iy = (int) fy;
                    double ax = fx - ix, ay = fy - iy;
                    int i = iy * sw + ix;
                    int c00 = src[i], c10 = src[i + 1], c01 = src[i + sw], c11 = src[i + sw + 1];
                    double w00 = (1 - ax) * (1 - ay), w10 = ax * (1 - ay), w01 = (1 - ax) * ay, w11 = ax * ay;
                    double a00 = (c00 >>> 24) * w00, a10 = (c10 >>> 24) * w10, a01 = (c01 >>> 24) * w01, a11 = (c11 >>> 24) * w11;
                    a += a00 + a10 + a01 + a11;
                    r += ((c00 >> 16) & 255) * a00 + ((c10 >> 16) & 255) * a10 + ((c01 >> 16) & 255) * a01 + ((c11 >> 16) & 255) * a11;
                    g += ((c00 >> 8) & 255) * a00 + ((c10 >> 8) & 255) * a10 + ((c01 >> 8) & 255) * a01 + ((c11 >> 8) & 255) * a11;
                    b += (c00 & 255) * a00 + (c10 & 255) * a10 + (c01 & 255) * a01 + (c11 & 255) * a11;
                }
                if (a <= 0) continue;
                double cover = a / (4 * 255.0);
                int d = out[py * dw + px];
                double da = (d >>> 24) / 255.0;
                double oa = cover + da * (1 - cover);
                int nr = (int) ((r / a) * cover + ((d >> 16) & 255) * da * (1 - cover)) ;
                int ng = (int) ((g / a) * cover + ((d >> 8) & 255) * da * (1 - cover));
                int nb = (int) ((b / a) * cover + (d & 255) * da * (1 - cover));
                if (oa > 0) {
                    nr = (int) Math.min(255, nr / oa);
                    ng = (int) Math.min(255, ng / oa);
                    nb = (int) Math.min(255, nb / oa);
                }
                out[py * dw + px] = ((int) Math.round(oa * 255) << 24) | (nr << 16) | (ng << 8) | nb;
            }
        }
        dst.setRGB(0, 0, dw, dh, out, 0, dw);
    }
    // --8<-- [end:warp]

    private static double[][] squareToQuad(double[] q) {
        double x0 = q[0], y0 = q[1], x1 = q[2], y1 = q[3], x2 = q[4], y2 = q[5], x3 = q[6], y3 = q[7];
        double dx1 = x1 - x2, dx2 = x3 - x2, dx3 = x0 - x1 + x2 - x3;
        double dy1 = y1 - y2, dy2 = y3 - y2, dy3 = y0 - y1 + y2 - y3;
        double den = dx1 * dy2 - dx2 * dy1;
        double g = (dx3 * dy2 - dx2 * dy3) / den, h = (dx1 * dy3 - dx3 * dy1) / den;
        return new double[][]{
                {x1 - x0 + g * x1, x3 - x0 + h * x3, x0},
                {y1 - y0 + g * y1, y3 - y0 + h * y3, y0},
                {g, h, 1}};
    }

    private static double[][] invert(double[][] m) {
        double a = m[0][0], b = m[0][1], c = m[0][2], d = m[1][0], e = m[1][1], f = m[1][2], g = m[2][0], h = m[2][1], i = m[2][2];
        double ca = e * i - f * h, cb = -(d * i - f * g), cc = d * h - e * g;
        double det = a * ca + b * cb + c * cc;
        return new double[][]{
                {ca / det, -(b * i - c * h) / det, (b * f - c * e) / det},
                {cb / det, (a * i - c * g) / det, -(a * f - c * d) / det},
                {cc / det, -(a * h - b * g) / det, (a * e - b * d) / det}};
    }
}
