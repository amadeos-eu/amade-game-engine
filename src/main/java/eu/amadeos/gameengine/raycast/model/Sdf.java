package eu.amadeos.gameengine.raycast.model;

/**
 * Signed distance functions: for a point, how far it is from the surface of a shape, negative inside. A
 * {@link Model} is built from them: the distance to several shapes is the smallest of their distances, a
 * hole is the largest of the shape and the negated hole. All shapes sit at the origin; move the point
 * instead of the shape ({@code sphere(x - 1, y, z, 0.5)} is a ball at x = 1).
 */
// --8<-- [start:sdf]
public final class Sdf {
    private Sdf() {
    }

    public static double length(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    public static double length(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public static double sphere(double x, double y, double z, double radius) {
        return length(x, y, z) - radius;
    }

    /** A box with half sizes {@code bx, by, bz} and edges rounded by {@code round}. */
    public static double box(double x, double y, double z, double bx, double by, double bz, double round) {
        double qx = Math.abs(x) - bx + round, qy = Math.abs(y) - by + round, qz = Math.abs(z) - bz + round;
        return length(Math.max(qx, 0), Math.max(qy, 0), Math.max(qz, 0)) + Math.min(Math.max(qx, Math.max(qy, qz)), 0) - round;
    }

    /** A rod with round ends from point a to point b. */
    public static double capsule(double x, double y, double z, double ax, double ay, double az,
                                 double bx, double by, double bz, double radius) {
        double pax = x - ax, pay = y - ay, paz = z - az, bax = bx - ax, bay = by - ay, baz = bz - az;
        double h = clamp((pax * bax + pay * bay + paz * baz) / (bax * bax + bay * bay + baz * baz), 0, 1);
        return length(pax - bax * h, pay - bay * h, paz - baz * h) - radius;
    }

    /**
     * A cylinder, given the point's distance from the axis ({@code radial}) and its position along it
     * ({@code axial}, 0 in the middle): {@code cylinder(length(x, z), y, r, h)} stands upright.
     */
    public static double cylinder(double radial, double axial, double radius, double halfHeight) {
        double dx = radial - radius, dy = Math.abs(axial) - halfHeight;
        return Math.min(Math.max(dx, dy), 0) + length(Math.max(dx, 0), Math.max(dy, 0));
    }

    /** A ring around an axis: {@code radial} and {@code axial} as for {@link #cylinder}. */
    public static double torus(double radial, double axial, double ringRadius, double tubeRadius) {
        return length(radial - ringRadius, axial) - tubeRadius;
    }

    /** A cone with radius {@code base} at axial 0 and its tip at axial {@code length}. An estimate, good for marching. */
    public static double cone(double radial, double axial, double base, double length) {
        double r = base * (1 - clamp(axial / length, 0, 1));
        return Math.max((radial - r) * 0.85, Math.max(-axial, axial - length));
    }

    /** Joins two shapes with a fillet of size {@code k} instead of a sharp crease. */
    public static double smoothUnion(double a, double b, double k) {
        double h = clamp(0.5 + 0.5 * (b - a) / k, 0, 1);
        return b + (a - b) * h - k * h * (1 - h);
    }
}
// --8<-- [end:sdf]
