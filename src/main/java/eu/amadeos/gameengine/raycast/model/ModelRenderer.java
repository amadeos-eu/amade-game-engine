package eu.amadeos.gameengine.raycast.model;

import eu.amadeos.gameengine.raycast.Texture;
import eu.amadeos.gameengine.raycast.Views;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

/**
 * Turns a {@link Model} into pictures by marching a ray through it for every pixel: step forward by the
 * distance the model reports, which can never overshoot the surface, until the ray touches it. At the
 * surface the renderer finds the normal, then lights it: lamps with soft shadows, a little occlusion in the
 * creases, highlights on metal and a cool rim along the outline. That is far too slow for every frame, so a
 * game renders its things once, at the start, from 8 or 16 sides ({@link #views}); the {@link
 * eu.amadeos.gameengine.raycast.Raycaster} then only copies the right picture.
 */
// --8<-- [start:renderer]
public final class ModelRenderer {
    /**
     * A light: a direction it comes from (towards the light, need not be unit length) or, with {@code point},
     * a position; and its colour, about 1 for a normal lamp.
     */
    public record Lamp(double x, double y, double z, double r, double g, double b, boolean point) {
        public static Lamp from(double x, double y, double z, double r, double g, double b) {
            double l = Sdf.length(x, y, z);
            return new Lamp(x / l, y / l, z / l, r, g, b, false);
        }

        public static Lamp at(double x, double y, double z, double r, double g, double b) {
            return new Lamp(x, y, z, r, g, b, true);
        }
    }

    /** Where a model stands in front of the camera for {@link #picture}: a position and three turns in degrees. */
    public record Placement(double x, double y, double z, double yaw, double pitch, double roll) { }

    /** A key light from the upper left front and a cool fill from behind on the right. */
    public static final List<Lamp> STUDIO = List.of(
            Lamp.from(-0.42, 0.78, 0.46, 1.05, 1.0, 0.95),
            Lamp.from(0.75, 0.15, -0.3, 0.25, 0.32, 0.45));

    private List<Lamp> lamps = STUDIO;
    private double ambient = 0.28;

    public ModelRenderer lamps(List<Lamp> lamps, double ambient) {
        this.lamps = List.copyOf(lamps);
        this.ambient = ambient;
        return this;
    }

    /**
     * The model from the front, a little from above ({@code pitch} degrees), as a billboard texture of
     * {@code size} pixels (a power of two). Model space -1 … 1 fills the picture; pixels the model does not
     * cover are holes.
     */
    public Texture billboard(Model model, int size, double pitch) {
        int[] argb = new int[size * size];
        float[] glow = new float[size * size];
        double p = Math.toRadians(pitch), fy = -Math.sin(p), fz = -Math.cos(p), uy = Math.cos(p), uz = -Math.sin(p);
        double[] out = new double[4];
        for (int j = 0; j < size; j++) {
            for (int i = 0; i < size; i++) {
                double u = (i + 0.5) / size * 2 - 1, v = 1 - (j + 0.5) / size * 2;
                if (!trace(model, u, v * uy - 4 * fy, v * uz - 4 * fz, 0, fy, fz, 8, lamps, out)) continue;
                argb[j * size + i] = 0xff000000 | rgb(out);
                glow[j * size + i] = (float) out[3];
            }
        }
        return Texture.of(size, argb, glow);
    }

    /**
     * All views of a thing for a {@link eu.amadeos.gameengine.raycast.Billboard}: {@code frames} animation
     * frames ({@code frame} gives the model of each) from {@code directions} sides, rendered in parallel.
     */
    public Views views(IntFunction<Model> frame, int frames, int directions, int size, double pitch) {
        Texture[][] textures = new Texture[frames][directions];
        Model[] models = new Model[frames];
        for (int f = 0; f < frames; f++) models[f] = frame.apply(f);
        IntStream.range(0, frames * directions).parallel().forEach(k -> {
            int f = k / directions, d = k % directions;
            textures[f][d] = billboard(models[f].turned(360.0 * d / directions), size, pitch);
        });
        return new Views(textures);
    }

    // --8<-- [start:picture]
    /**
     * The model in perspective, as the player sees a tool in their hands: the camera at the origin looks
     * along +z, y up, and {@code focal} pixels are one unit at distance one. The lamps are given as the
     * camera sees them. The picture is transparent where the model is not.
     */
    public BufferedImage picture(Model model, int width, int height, double focal, Placement at, List<Lamp> cameraLamps) {
        return picture(model, width, height, focal, width / 2.0, height / 2.0, at, cameraLamps);
    }

    /**
     * A window of the camera's view: the same as {@link #picture(Model, int, int, double, Placement, List)},
     * but the camera's axis passes through ({@code axisX}, {@code axisY}) of the picture, which may lie
     * outside it. A tool in the lower right of the screen is rendered without the empty rest of the screen.
     */
    public BufferedImage picture(Model model, int width, int height, double focal, double axisX, double axisY, Placement at,
                                 List<Lamp> cameraLamps) {
        double[][] r = rotation(at);   // columns: the model's x, y and z axes in camera space
        List<Lamp> local = cameraLamps.stream().map(l -> l.point()
                ? Lamp.at(dot(r, 0, l.x() - at.x(), l.y() - at.y(), l.z() - at.z()), dot(r, 1, l.x() - at.x(), l.y() - at.y(), l.z() - at.z()),
                dot(r, 2, l.x() - at.x(), l.y() - at.y(), l.z() - at.z()), l.r(), l.g(), l.b())
                : new Lamp(dot(r, 0, l.x(), l.y(), l.z()), dot(r, 1, l.x(), l.y(), l.z()), dot(r, 2, l.x(), l.y(), l.z()),
                l.r(), l.g(), l.b(), false)).toList();
        double ox = dot(r, 0, -at.x(), -at.y(), -at.z()), oy = dot(r, 1, -at.x(), -at.y(), -at.z()), oz = dot(r, 2, -at.x(), -at.y(), -at.z());
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] argb = new int[width * height];
        IntStream.range(0, height).parallel().forEach(j -> {
            double[] out = new double[4];
            for (int i = 0; i < width; i++) {
                double dx = (i + 0.5 - axisX) / focal, dy = -(j + 0.5 - axisY) / focal, l = Sdf.length(dx, dy, 1);
                dx /= l; dy /= l;
                double dz = 1 / l;
                if (trace(model, ox, oy, oz, dot(r, 0, dx, dy, dz), dot(r, 1, dx, dy, dz), dot(r, 2, dx, dy, dz), 6, local, out)) {
                    argb[j * width + i] = 0xff000000 | rgb(out);
                }
            }
        });
        image.setRGB(0, 0, width, height, argb, 0, width);
        return image;
    }
    // --8<-- [end:picture]

    /** Turns yaw (about y), then pitch (about x), then roll (about z); returns the model's axes in camera space. */
    private static double[][] rotation(Placement at) {
        double y = Math.toRadians(at.yaw()), p = Math.toRadians(at.pitch()), r = Math.toRadians(at.roll());
        double cy = Math.cos(y), sy = Math.sin(y), cp = Math.cos(p), sp = Math.sin(p), cr = Math.cos(r), sr = Math.sin(r);
        // R = Ry * Rx * Rz
        double[][] m = {
                {cy * cr + sy * sp * sr, -cy * sr + sy * sp * cr, sy * cp},
                {cp * sr, cp * cr, -sp},
                {-sy * cr + cy * sp * sr, sy * sr + cy * sp * cr, cy * cp}};
        return m;
    }

    /** Component {@code axis} of the camera-space vector in model space: the dot product with that model axis. */
    private static double dot(double[][] m, int axis, double x, double y, double z) {
        return m[0][axis] * x + m[1][axis] * y + m[2][axis] * z;
    }

    // --8<-- [start:trace]
    /** Marches one ray; on a hit, writes {r, g, b, glow} into {@code out} and returns true. */
    private boolean trace(Model m, double ox, double oy, double oz, double dx, double dy, double dz, double far,
                          List<Lamp> lights, double[] out) {
        double t = 0;
        for (int i = 0; i < 160 && t < far; i++) {
            double x = ox + dx * t, y = oy + dy * t, z = oz + dz * t, d = m.distance(x, y, z);
            if (d < 0.0008) {
                Material mat = m.materialAt(x, y, z);
                if (mat.glow() > 0) {
                    out[0] = mat.red(); out[1] = mat.green(); out[2] = mat.blue(); out[3] = mat.glow();
                    return true;
                }
                double[] n = normal(m, x, y, z);
                double ao = occlusion(m, x, y, z, n);
                double r = mat.red() * ambient * ao, g = mat.green() * ambient * ao, b = mat.blue() * ambient * ao * 1.1;
                for (Lamp l : lights) {
                    double lx = l.x(), ly = l.y(), lz = l.z(), fall = 1;
                    if (l.point()) {
                        lx -= x; ly -= y; lz -= z;
                        double dl = Sdf.length(lx, ly, lz);
                        fall = 1 / (1 + dl * dl * 30);
                        lx /= dl; ly /= dl; lz /= dl;
                    }
                    double diffuse = Math.max(0, n[0] * lx + n[1] * ly + n[2] * lz) * fall;
                    if (diffuse <= 0) continue;
                    if (!l.point()) diffuse *= shadow(m, x + n[0] * 0.004, y + n[1] * 0.004, z + n[2] * 0.004, lx, ly, lz);
                    double hx = lx - dx, hy = ly - dy, hz = lz - dz, hl = Sdf.length(hx, hy, hz);
                    double spec = Math.pow(Math.max(0, (n[0] * hx + n[1] * hy + n[2] * hz) / hl), 40) * mat.shine() * diffuse * 2.2;
                    r += (mat.red() * diffuse + spec) * l.r();
                    g += (mat.green() * diffuse + spec) * l.g();
                    b += (mat.blue() * diffuse + spec) * l.b();
                }
                double rim = Math.pow(1 - Math.max(0, -(n[0] * dx + n[1] * dy + n[2] * dz)), 3) * 0.35 * ao;
                out[0] = r + rim * 0.55;
                out[1] = g + rim * 0.7;
                out[2] = b + rim * 0.9;
                out[3] = 0;
                return true;
            }
            t += d * 0.9;
        }
        return false;
    }
    // --8<-- [end:trace]

    private static double[] normal(Model m, double x, double y, double z) {
        double e = 0.002;
        double nx = m.distance(x + e, y, z) - m.distance(x - e, y, z);
        double ny = m.distance(x, y + e, z) - m.distance(x, y - e, z);
        double nz = m.distance(x, y, z + e) - m.distance(x, y, z - e);
        double l = Sdf.length(nx, ny, nz);
        return new double[]{nx / l, ny / l, nz / l};
    }

    /** How open the surface is around the point: creases and corners get less of the ambient light. */
    private static double occlusion(Model m, double x, double y, double z, double[] n) {
        double occ = 0, w = 1;
        for (int i = 1; i <= 5; i++) {
            double h = 0.025 * i;
            occ += (h - m.distance(x + n[0] * h, y + n[1] * h, z + n[2] * h)) * w;
            w *= 0.7;
        }
        return Sdf.clamp(1 - occ * 4, 0, 1);
    }

    /** A soft shadow: the closer the ray towards the lamp passes the model, the darker. */
    private static double shadow(Model m, double x, double y, double z, double lx, double ly, double lz) {
        double res = 1, t = 0.02;
        for (int i = 0; i < 48 && t < 3; i++) {
            double d = m.distance(x + lx * t, y + ly * t, z + lz * t);
            if (d < 0.0005) return 0.15;
            res = Math.min(res, 10 * d / t);
            t += Math.max(d, 0.01);
        }
        return 0.15 + 0.85 * Sdf.clamp(res, 0, 1);
    }

    private static int rgb(double[] c) {
        return ((int) (Sdf.clamp(c[0], 0, 1) * 255) << 16) | ((int) (Sdf.clamp(c[1], 0, 1) * 255) << 8)
                | (int) (Sdf.clamp(c[2], 0, 1) * 255);
    }
}
// --8<-- [end:renderer]
