package eu.amadeos.gameengine.motion;

import eu.amadeos.gameengine.math.Vector;

import java.util.Arrays;

/**
 * A curve to fly along: the swoop of an insect into its formation, the loop of a dive. It is made of
 * cubic Bézier curves, each from one point to the next, bent by two control points, and of straight
 * lines, which are Bézier curves with their control points on the line.
 *
 * <pre>
 * FlightPath swoop = FlightPath.from(320, -20)
 *         .curveTo(320, 150, 100, 260, 160, 180)      // control point, control point, end point
 *         .lineTo(160, 120)
 *         .build();
 * Vector at = swoop.at(distanceFlown);                 // flown at an even speed
 * double heading = swoop.angleAt(distanceFlown);       // the way the nose points
 * </pre>
 *
 * <p>A Bézier curve is easy to compute for a parameter <i>t</i> from 0 to 1, but equal steps of <i>t</i>
 * are not equal steps along the curve: they crowd where the curve bends sharply and spread where it is
 * straight. Flown by <i>t</i>, an insect would rush and crawl. So the path measures itself once, at many
 * points, and {@link #at} finds the <i>t</i> for a distance in that table: the speed stays even.
 */
public final class FlightPath {
    /** Points measured per curve; enough for curves the size of a screen. */
    static final int SAMPLES = 48;

    /** x, y of the start, then three points per curve: control, control, end. */
    private final double[] points;
    private final int curves;
    /** The distance along the path at each measured point, from 0 to the length. */
    private final double[] distances;

    private FlightPath(double[] points) {
        this.points = points;
        this.curves = (points.length / 2 - 1) / 3;
        if (curves < 1) throw new IllegalArgumentException("a path needs at least one curve or line");
        this.distances = measure();
    }

    /** Starts a path at {@code x}, {@code y}. */
    public static Builder from(double x, double y) {
        return new Builder(x, y);
    }

    /** Collects the curves of a path; {@link #build()} makes it. */
    public static final class Builder {
        private double[] xy = new double[32];
        private int size;

        private Builder(double x, double y) {
            add(x, y);
        }

        /** A cubic Bézier curve from the last point to {@code x}, {@code y}, bent towards two control points. */
        public Builder curveTo(double c1x, double c1y, double c2x, double c2y, double x, double y) {
            add(c1x, c1y);
            add(c2x, c2y);
            add(x, y);
            return this;
        }

        /** A straight line from the last point: a curve whose control points lie on the line, a third apart. */
        public Builder lineTo(double x, double y) {
            double fromX = xy[size - 2];
            double fromY = xy[size - 1];
            return curveTo(fromX + (x - fromX) / 3, fromY + (y - fromY) / 3,
                    fromX + 2 * (x - fromX) / 3, fromY + 2 * (y - fromY) / 3, x, y);
        }

        public FlightPath build() {
            return new FlightPath(Arrays.copyOf(xy, size));
        }

        private void add(double x, double y) {
            if (size + 2 > xy.length) xy = Arrays.copyOf(xy, 2 * xy.length);
            xy[size++] = x;
            xy[size++] = y;
        }
    }

    // --8<-- [start:measure]
    /** Walks the path in small straight steps and notes the distance flown at each. */
    private double[] measure() {
        double[] table = new double[curves * SAMPLES + 1];
        double lastX = points[0];
        double lastY = points[1];
        for (int i = 1; i < table.length; i++) {
            double u = i / (double) SAMPLES;    // curve number plus t
            double x = coordinate(u, 0);
            double y = coordinate(u, 1);
            table[i] = table[i - 1] + Math.hypot(x - lastX, y - lastY);
            lastX = x;
            lastY = y;
        }
        return table;
    }

    /** The parameter at {@code distance}: the curve number plus <i>t</i> within it. */
    private double parameterAt(double distance) {
        double d = Math.clamp(distance, 0, length());
        int i = Arrays.binarySearch(distances, d);
        if (i >= 0) return i / (double) SAMPLES;
        int after = -i - 1;                 // the first measured point beyond d
        int before = after - 1;
        double part = (d - distances[before]) / (distances[after] - distances[before]);
        return (before + part) / SAMPLES;
    }
    // --8<-- [end:measure]

    // --8<-- [start:bezier]
    /**
     * x ({@code axis} 0) or y (1) at parameter {@code u}, the curve number plus <i>t</i>: the four points
     * of the curve, weighted by the Bernstein polynomials of <i>t</i>.
     */
    private double coordinate(double u, int axis) {
        int curve = Math.min((int) u, curves - 1);
        double t = u - curve;
        double s = 1 - t;
        int p = 6 * curve + axis;
        return s * s * s * points[p] + 3 * s * s * t * points[p + 2] + 3 * s * t * t * points[p + 4]
                + t * t * t * points[p + 6];
    }

    /** The derivative: which way, and how fast in <i>t</i>, the curve goes at {@code u}. */
    private double slope(double u, int axis) {
        int curve = Math.min((int) u, curves - 1);
        double t = u - curve;
        double s = 1 - t;
        int p = 6 * curve + axis;
        return 3 * s * s * (points[p + 2] - points[p]) + 6 * s * t * (points[p + 4] - points[p + 2])
                + 3 * t * t * (points[p + 6] - points[p + 4]);
    }
    // --8<-- [end:bezier]

    // --8<-- [start:at]
    /** The length of the whole path, in logical units. */
    public double length() {
        return distances[distances.length - 1];
    }

    /** Where one is after flying {@code distance} along the path; before the start the start, after the end the end. */
    public Vector at(double distance) {
        double u = parameterAt(distance);
        return new Vector(coordinate(u, 0), coordinate(u, 1));
    }

    /** The direction of flight after {@code distance}, as an angle like {@link Vector#angle()}. */
    public double angleAt(double distance) {
        double u = parameterAt(distance);
        double dx = slope(u, 0);
        double dy = slope(u, 1);
        if (dx == 0 && dy == 0) {
            // A control point on an end point: the curve stands still there for a moment. Look around it.
            return at(distance + 0.5).minus(at(distance - 0.5)).angle();
        }
        return Math.atan2(dy, dx);
    }
    // --8<-- [end:at]

    public Vector start() {
        return new Vector(points[0], points[1]);
    }

    public Vector end() {
        return new Vector(points[points.length - 2], points[points.length - 1]);
    }

    /** The same path, mirrored at the vertical line in the middle of a field {@code width} wide. */
    public FlightPath mirrored(double width) {
        double[] mirrored = points.clone();
        for (int i = 0; i < mirrored.length; i += 2) mirrored[i] = width - mirrored[i];
        return new FlightPath(mirrored);
    }

    /** The same path, moved by {@code dx}, {@code dy}. */
    public FlightPath translated(double dx, double dy) {
        double[] moved = points.clone();
        for (int i = 0; i < moved.length; i += 2) {
            moved[i] += dx;
            moved[i + 1] += dy;
        }
        return new FlightPath(moved);
    }
}
