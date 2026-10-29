package eu.amadeos.gameengine.raycast;

import java.util.List;

/**
 * The light of a {@link GridMap}: a colour per cell, baked once from the map's lamps, and looked up between
 * cell centres so it fades smoothly across the floor. A lamp lights a cell only if it sees its centre, so
 * walls cast shadows. Light is red, green and blue between 0 and about 2; 1 shows a texture as painted.
 *
 * <p>Short flashes (a muzzle, an explosion) come on top for one frame: {@link #beginFrame()} clears them,
 * {@link #flash} adds one. They are cheap enough for every frame because they only touch the cells around them.
 */
// --8<-- [start:lightmap]
public final class LightMap {
    /** A lamp: position, colour and how far it reaches. */
    public record Light(double x, double y, double r, double g, double b, double radius) { }

    private final GridMap map;
    private final int width;
    private final int height;
    private final float[] baked;     // r, g, b per cell
    private final float[] lit;       // baked plus this frame's flashes
    private final boolean[] touched;
    private boolean flashing;

    /** Bakes the light of {@code lights} over a base of {@code ambient} (r, g, b). */
    public LightMap(GridMap map, double ambientR, double ambientG, double ambientB, List<Light> lights) {
        this.map = map;
        this.width = map.width();
        this.height = map.height();
        baked = new float[width * height * 3];
        lit = new float[width * height * 3];
        touched = new boolean[width * height];
        for (int i = 0; i < width * height; i++) {
            baked[i * 3] = (float) ambientR;
            baked[i * 3 + 1] = (float) ambientG;
            baked[i * 3 + 2] = (float) ambientB;
        }
        for (Light light : lights) add(baked, light, null);
        // A wall cell takes the mean of its open neighbours, so the light does not drop into black at its feet.
        float[] copy = baked.clone();
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            if (map.wall(x, y) == null) continue;
            float r = 0, g = 0, b = 0;
            int n = 0;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = x + d[0], ny = y + d[1];
                if (!map.contains(nx, ny) || map.wall(nx, ny) != null) continue;
                int j = (ny * width + nx) * 3;
                r += copy[j]; g += copy[j + 1]; b += copy[j + 2];
                n++;
            }
            if (n > 0) {
                int j = (y * width + x) * 3;
                baked[j] = r / n; baked[j + 1] = g / n; baked[j + 2] = b / n;
            }
        }
        System.arraycopy(baked, 0, lit, 0, baked.length);
    }

    private void add(float[] target, Light light, boolean[] mark) {
        int x0 = Math.max(0, (int) Math.floor(light.x() - light.radius())), x1 = Math.min(width - 1, (int) (light.x() + light.radius()));
        int y0 = Math.max(0, (int) Math.floor(light.y() - light.radius())), y1 = Math.min(height - 1, (int) (light.y() + light.radius()));
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
            double cx = x + 0.5, cy = y + 0.5, d = Math.hypot(cx - light.x(), cy - light.y());
            if (d >= light.radius()) continue;
            if (map.wall(x, y) != null || !sees(light.x(), light.y(), cx, cy)) continue;
            double f = 1 - d / light.radius();
            f *= f;
            int i = (y * width + x) * 3;
            target[i] += (float) (light.r() * f);
            target[i + 1] += (float) (light.g() * f);
            target[i + 2] += (float) (light.b() * f);
            if (mark != null) mark[y * width + x] = true;
        }
    }

    /** Line of sight for baking: only walls stop light, doors do not, so opening one needs no new bake. */
    private boolean sees(double x0, double y0, double x1, double y1) {
        double dx = x1 - x0, dy = y1 - y0;
        int steps = (int) Math.ceil(Math.hypot(dx, dy) * 4);
        for (int s = 1; s < steps; s++) {
            double t = (double) s / steps;
            if (map.wall((int) Math.floor(x0 + dx * t), (int) Math.floor(y0 + dy * t)) != null) return false;
        }
        return true;
    }

    /** Removes the last frame's flashes. */
    public void beginFrame() {
        if (!flashing) return;
        for (int i = 0; i < touched.length; i++) {
            if (!touched[i]) continue;
            lit[i * 3] = baked[i * 3];
            lit[i * 3 + 1] = baked[i * 3 + 1];
            lit[i * 3 + 2] = baked[i * 3 + 2];
            touched[i] = false;
        }
        flashing = false;
    }

    /** Adds a light for this frame only, such as a muzzle flash. */
    public void flash(Light light) {
        add(lit, light, touched);
        flashing = true;
    }

    /** The light at a point, between the cell centres around it; written into {@code out} (r, g, b). */
    public void sample(double x, double y, float[] out) {
        double fx = x - 0.5, fy = y - 0.5;
        int x0 = (int) Math.floor(fx), y0 = (int) Math.floor(fy);
        float tx = (float) (fx - x0), ty = (float) (fy - y0);
        int ax = clamp(x0, width), bx = clamp(x0 + 1, width), ay = clamp(y0, height), by = clamp(y0 + 1, height);
        int i00 = (ay * width + ax) * 3, i10 = (ay * width + bx) * 3, i01 = (by * width + ax) * 3, i11 = (by * width + bx) * 3;
        for (int c = 0; c < 3; c++) {
            float top = lit[i00 + c] + (lit[i10 + c] - lit[i00 + c]) * tx;
            float bottom = lit[i01 + c] + (lit[i11 + c] - lit[i01 + c]) * tx;
            out[c] = top + (bottom - top) * ty;
        }
    }

    private static int clamp(int v, int size) {
        return v < 0 ? 0 : v >= size ? size - 1 : v;
    }
}
// --8<-- [end:lightmap]
