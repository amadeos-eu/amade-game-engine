package eu.amadeos.gameengine.raycast;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Draws a {@link GridMap} as seen from a {@link Camera}, into a picture of its own of {@code width} ×
 * {@code height} pixels that the game then draws onto its canvas. It casts one ray per column of pixels: the
 * distance to the wall the ray hits gives the height of that wall's slice, as in the first 3D games. Floor
 * and ceiling are drawn row by row, because every pixel of a row is equally far away. Things on the map are
 * {@link Billboard}s, hidden behind walls by the distance each column remembers.
 *
 * <p>Everything is lit by a {@link LightMap} and fades into fog with distance; glowing texels keep their own
 * colour. The picture belongs to the game loop thread: draw it with {@code g.drawImage(raycaster.frame(), …)}.
 */
// --8<-- [start:raycaster]
public final class Raycaster {
    /** Pixels per unit at distance 1, as a part of the picture's height: 1.2 shows 640 × 400 at about 68°. */
    public static final double ZOOM = 1.2;

    private final int width;
    private final int height;
    private final BufferedImage frame;
    private final int[] pixels;
    private final double[] depth;
    private final GridMap.Ray ray = new GridMap.Ray();
    private final float[] light = new float[3];
    private final List<Billboard> sorted = new ArrayList<>();
    private int fog = 0x000000;
    private double fogDensity = 0.08;
    private int[] sky;
    private int skyWidth;
    private int skyHeight;
    private double scale;
    private double plane;

    public Raycaster(int width, int height) {
        this.width = width;
        this.height = height;
        frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) frame.getRaster().getDataBuffer()).getData();
        depth = new double[width];
        scale = height * ZOOM;
        plane = width / 2.0 / scale;
    }

    public BufferedImage frame() {
        return frame;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** Things far away fade into {@code rgb}; {@code density} 0.08 halves the colour after about 9 units. */
    public void setFog(int rgb, double density) {
        fog = rgb & 0xffffff;
        fogDensity = density;
    }

    /** A panorama for the cells without ceiling, wrapped once around the horizon; {@code null} shows the fog colour. */
    public void setSky(BufferedImage panorama) {
        if (panorama == null) {
            sky = null;
            return;
        }
        skyWidth = panorama.getWidth();
        skyHeight = panorama.getHeight();
        sky = panorama.getRGB(0, 0, skyWidth, skyHeight, null, 0, skyWidth);
    }

    /** The distance straight ahead of the camera to the wall seen in column {@code x}, after the last frame. */
    public double depth(int x) {
        return depth[x];
    }

    /**
     * Where a point of the world lands on the picture: {x, y, distance}, or {@code null} behind the camera.
     * {@code z} is the height above the floor.
     */
    public double[] project(Camera camera, double x, double y, double z) {
        double dx = camera.dirX(), dy = camera.dirY();
        double sx = x - camera.x, sy = y - camera.y;
        double forward = sx * dx + sy * dy, right = -sx * dy + sy * dx;
        if (forward <= 0.05) return null;
        return new double[]{width / 2.0 + right / forward * scale, height / 2.0 + (camera.eye - z) * scale / forward, forward};
    }

    /** Draws the world: floor and ceiling, then walls, then the billboards from far to near. */
    public void render(GridMap map, LightMap lights, Camera camera, List<Billboard> billboards) {
        double dx = camera.dirX(), dy = camera.dirY();
        double px = -dy * plane, py = dx * plane;
        floorAndCeiling(map, lights, camera, dx, dy, px, py);
        walls(map, lights, camera, dx, dy, px, py);
        billboards(lights, camera, dx, dy, billboards);
    }

    // --8<-- [start:floor]
    private void floorAndCeiling(GridMap map, LightMap lights, Camera camera, double dx, double dy, double px, double py) {
        double horizon = height / 2.0;
        double r0x = dx - px, r0y = dy - py, r1x = dx + px, r1y = dy + py;
        for (int y = 0; y < height; y++) {
            boolean floor = y + 0.5 > horizon;
            double rows = floor ? y + 0.5 - horizon : horizon - y - 0.5;
            double distance = (floor ? camera.eye : 1 - camera.eye) * scale / rows;
            double stepX = distance * (r1x - r0x) / width, stepY = distance * (r1y - r0y) / width;
            double wx = camera.x + distance * r0x + stepX * 0.5, wy = camera.y + distance * r0y + stepY * 0.5;
            double fogKeep = Math.exp(-distance * fogDensity);
            int row = y * width;
            for (int x = 0; x < width; x++, wx += stepX, wy += stepY) {
                int cx = (int) Math.floor(wx), cy = (int) Math.floor(wy);
                Texture t = floor ? map.floor(cx, cy) : map.ceiling(cx, cy);
                if (t == null) {
                    pixels[row + x] = floor ? fog : skyAt(camera, x, y);
                    continue;
                }
                int u = (int) ((wx - cx) * t.size) & t.mask, v = (int) ((wy - cy) * t.size) & t.mask;
                int i = (v << t.shift) | u;
                lights.sample(wx, wy, light);
                pixels[row + x] = shade(t.argb[i], t.glow[i] & 255, light, floor ? 0.95f : 0.8f, fogKeep);
            }
        }
    }
    // --8<-- [end:floor]

    private int skyAt(Camera camera, int x, int y) {
        if (sky == null) return fog;
        double angle = camera.angle + Math.atan((x + 0.5 - width / 2.0) / scale);
        int sx = (int) Math.floor(angle / (2 * Math.PI) * skyWidth);
        int sy = Math.min(skyHeight - 1, (int) ((y + 0.5) / (height / 2.0) * skyHeight));
        return sky[sy * skyWidth + Math.floorMod(sx, skyWidth)] & 0xffffff;
    }

    // --8<-- [start:walls]
    private void walls(GridMap map, LightMap lights, Camera camera, double dx, double dy, double px, double py) {
        double horizon = height / 2.0;
        for (int x = 0; x < width; x++) {
            double c = 2.0 * (x + 0.5) / width - 1;
            double rx = dx + px * c, ry = dy + py * c;
            map.cast(camera.x, camera.y, rx, ry, 1e6, ray);
            if (!ray.hit() || ray.texture == null) {
                depth[x] = ray.hit() ? ray.distance : Double.POSITIVE_INFINITY;
                continue;
            }
            double distance = Math.max(ray.distance, 1e-4);
            depth[x] = distance;
            Texture t = ray.texture;
            int u = (int) (ray.u * t.size);
            // The light just in front of the wall, on the ray's side of it.
            lights.sample(camera.x + rx * distance * 0.995, camera.y + ry * distance * 0.995, light);
            float sideShade = ray.side == 1 ? 0.8f : 1f;
            double fogKeep = Math.exp(-distance * fogDensity);
            double slice = scale / distance;
            double top = horizon - (1 - camera.eye) * slice;
            int y0 = Math.max(0, (int) Math.ceil(top - 0.5)), y1 = Math.min(height - 1, (int) Math.ceil(top + slice - 0.5) - 1);
            double texelsPerPixel = t.size / slice;
            double v = (y0 + 0.5 - top) * texelsPerPixel;
            for (int y = y0; y <= y1; y++, v += texelsPerPixel) {
                int i = (Math.min(t.mask, (int) v) << t.shift) | u;
                pixels[y * width + x] = shade(t.argb[i], t.glow[i] & 255, light, sideShade, fogKeep);
            }
        }
    }
    // --8<-- [end:walls]

    // --8<-- [start:billboards]
    private void billboards(LightMap lights, Camera camera, double dx, double dy, List<Billboard> billboards) {
        sorted.clear();
        for (Billboard b : billboards) if (b.visible && b.views != null) sorted.add(b);
        sorted.sort(Comparator.comparingDouble(b -> -((b.x - camera.x) * dx + (b.y - camera.y) * dy)));
        double horizon = height / 2.0;
        for (Billboard b : sorted) {
            double sx = b.x - camera.x, sy = b.y - camera.y;
            double forward = sx * dx + sy * dy, right = -sx * dy + sy * dx;
            if (forward < 0.1) continue;
            double unit = scale / forward;
            double size = b.size * unit, centre = width / 2.0 + right * unit;
            double bottom = horizon + (camera.eye - b.lift) * unit, top = bottom - size, left = centre - size / 2;
            int x0 = Math.max(0, (int) Math.ceil(left - 0.5)), x1 = Math.min(width - 1, (int) Math.ceil(left + size - 0.5) - 1);
            if (x0 > x1 || bottom < 0 || top >= height) continue;
            lights.sample(b.x, b.y, light);
            double fogKeep = Math.exp(-forward * fogDensity);
            if (b.shadow > 0) shadow(b, camera, unit, centre, forward);
            Texture t = b.views.pick(b.frame, b.facing, Math.atan2(-sy, -sx));
            int flash = (int) Math.round(Math.max(0, Math.min(1, b.flash)) * 255);
            int y0 = Math.max(0, (int) Math.ceil(top - 0.5)), y1 = Math.min(height - 1, (int) Math.ceil(bottom - 0.5) - 1);
            double texels = t.size / size;
            for (int x = x0; x <= x1; x++) {
                if (forward >= depth[x]) continue;
                int u = Math.min(t.mask, (int) ((x + 0.5 - left) * texels));
                double v = (y0 + 0.5 - top) * texels;
                for (int y = y0; y <= y1; y++, v += texels) {
                    int i = (Math.min(t.mask, (int) v) << t.shift) | u;
                    int argb = t.argb[i];
                    if (argb >>> 24 < 128) continue;
                    int c = shade(argb, t.glow[i] & 255, light, 1f, fogKeep);
                    if (flash > 0) c = mix(c, 0xffffff, flash);
                    pixels[y * width + x] = c;
                }
            }
        }
    }

    /** A soft dark ellipse on the floor under a billboard. */
    private void shadow(Billboard b, Camera camera, double unit, double centre, double forward) {
        double floorY = height / 2.0 + camera.eye * unit;
        double rx = b.size * b.shadow * unit / 2, ry = Math.max(1.5, rx * 0.18);
        int x0 = Math.max(0, (int) (centre - rx)), x1 = Math.min(width - 1, (int) (centre + rx));
        int y0 = Math.max((int) (height / 2.0) + 1, (int) (floorY - ry)), y1 = Math.min(height - 1, (int) (floorY + ry));
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
            if (forward >= depth[x]) continue;
            double ex = (x + 0.5 - centre) / rx, ey = (y + 0.5 - floorY) / ry, e = ex * ex + ey * ey;
            if (e >= 1) continue;
            int keep = (int) (255 * (1 - 0.55 * (1 - e)));
            int p = pixels[y * width + x];
            pixels[y * width + x] = ((((p >> 16) & 255) * keep >> 8) << 16) | ((((p >> 8) & 255) * keep >> 8) << 8) | ((p & 255) * keep >> 8);
        }
    }
    // --8<-- [end:billboards]

    // --8<-- [start:shade]
    /** Lights a texel, lets it glow, and fades it into the fog. */
    private int shade(int argb, int glow, float[] light, float side, double fogKeep) {
        float g = glow / 255f, lit = 1 - g;
        float keep = (float) fogKeep, fogPart = 1 - keep;
        int r = channel((argb >> 16) & 255, light[0] * side * lit + g * 1.1f, keep, (fog >> 16) & 255, fogPart);
        int gr = channel((argb >> 8) & 255, light[1] * side * lit + g * 1.1f, keep, (fog >> 8) & 255, fogPart);
        int b = channel(argb & 255, light[2] * side * lit + g * 1.1f, keep, fog & 255, fogPart);
        return (r << 16) | (gr << 8) | b;
    }

    private static int channel(int colour, float light, float keep, int fog, float fogPart) {
        int v = (int) (colour * light * keep + fog * fogPart);
        return v > 255 ? 255 : v;
    }
    // --8<-- [end:shade]

    private static int mix(int rgb, int with, int amount) {
        int keep = 255 - amount;
        int r = (((rgb >> 16) & 255) * keep + ((with >> 16) & 255) * amount) >> 8;
        int g = (((rgb >> 8) & 255) * keep + ((with >> 8) & 255) * amount) >> 8;
        int b = ((rgb & 255) * keep + (with & 255) * amount) >> 8;
        return (r << 16) | (g << 8) | b;
    }
}
// --8<-- [end:raycaster]
