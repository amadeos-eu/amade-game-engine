package eu.amadeos.gameengine.raycast.model;

/**
 * A 3D thing described by distances: {@link #shape} measures the point against every part with the
 * {@link Sdf} functions and reports each with {@link #part}; the nearest part decides the distance and the
 * material. The {@link ModelRenderer} marches rays through it. Model space: x to the right, y up, z towards
 * the viewer; for a billboard the thing fits into -1 … 1, its feet at y = -1, its front facing +z.
 *
 * <pre>
 * Model drone = new Model() {
 *     protected void shape(double x, double y, double z) {
 *         part(Sdf.sphere(x, y, z, 0.4), BODY);
 *         part(Sdf.sphere(x, y, z - 0.35, 0.12), EYE);
 *     }
 *     protected Material material(int part, double x, double y, double z) {
 *         return part == EYE ? Material.light(0xff3020) : Material.metal(0xa8b0b8);
 *     }
 * };
 * </pre>
 *
 * <p>A model is asked from many threads at once while it is rendered: {@link #distance} gives each thread
 * its own scratch state.
 */
// --8<-- [start:model]
public abstract class Model {
    private static final class Nearest {
        double distance;
        int part;
    }

    private final ThreadLocal<Nearest> nearest = ThreadLocal.withInitial(Nearest::new);

    /** Reports every part of the model at the point; called with the point in model space. */
    protected abstract void shape(double x, double y, double z);

    /** The material of part {@code part} at the point; may vary over the part, such as stripes. */
    protected abstract Material material(int part, double x, double y, double z);

    /** Called by {@link #shape} for each part: its distance from the point, and a number naming it. */
    protected final void part(double distance, int part) {
        Nearest n = nearest.get();
        if (distance < n.distance) {
            n.distance = distance;
            n.part = part;
        }
    }

    /** The distance from the point to the model's surface; negative inside. */
    public final double distance(double x, double y, double z) {
        Nearest n = nearest.get();
        n.distance = Double.POSITIVE_INFINITY;
        n.part = -1;
        shape(x, y, z);
        return n.distance;
    }

    /** The material of the surface at a point on (or very near) it. */
    public final Material materialAt(double x, double y, double z) {
        distance(x, y, z);
        return material(nearest.get().part, x, y, z);
    }

    /** The model turned about the upright axis by {@code degrees}: the way it is seen from another side. */
    public final Model turned(double degrees) {
        Model self = this;
        double a = Math.toRadians(degrees), c = Math.cos(a), s = Math.sin(a);
        return new Model() {
            @Override
            protected void shape(double x, double y, double z) {
                part(self.distance(x * c + z * s, y, -x * s + z * c), 0);
            }

            @Override
            protected Material material(int part, double x, double y, double z) {
                return self.materialAt(x * c + z * s, y, -x * s + z * c);
            }
        };
    }
}
// --8<-- [end:model]
