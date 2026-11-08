package eu.amadeos.gameengine.drive;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Pictures for the background of a road, made in code: a sky that runs from one colour at the top to another
 * at the horizon, soft clouds, and ranges of mountains. A range is a line of {@linkplain Noise noise} for
 * its ridge, lit by the slope (sunny where it falls to the left), with snow on the peaks above a line and
 * haze that thickens towards the horizon, so a range further back looks paler. Flat-topped ranges are mesas.
 *
 * <p>The pictures wrap around: their left and right edges fit together, so a {@link
 * eu.amadeos.gameengine.camera.Parallax} layer can repeat them while the road turns.
 */
// --8<-- [start:panorama]
public final class Panorama {
    private Panorama() {
    }

    /** A sky {@code width} × {@code height}: {@code top} at the top, {@code middle} at 60 %, {@code horizon} at the bottom. */
    public static BufferedImage sky(int width, int height, Color top, Color middle, Color horizon) {
        BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        g.setPaint(new LinearGradientPaint(0, 0, 0, height, new float[]{0f, 0.6f, 1f}, new Color[]{top, middle, horizon}));
        g.fillRect(0, 0, width, height);
        g.dispose();
        return im;
    }

    /** A sun (or moon) with a glow around it, painted onto a sky. */
    public static void sun(BufferedImage sky, double x, double y, double radius, Color disc, Color glow) {
        Graphics2D g = graphics(sky);
        double r = radius * 9;
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float) r, new float[]{0f, 0.25f, 1f},
                new Color[]{glow, new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), glow.getAlpha() / 3),
                        new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 0)}));
        g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
        g.setColor(disc);
        g.fill(new Ellipse2D.Double(x - radius, y - radius, 2 * radius, 2 * radius));
        g.dispose();
    }

    /** Soft clouds, lit from above ({@code lit}) and darker below ({@code dark}), in the upper {@code reach} of the picture. */
    public static void clouds(BufferedImage sky, long seed, int count, double reach, Color lit, Color dark) {
        Graphics2D g = graphics(sky);
        Random r = new Random(seed);
        int w = sky.getWidth();
        for (int c = 0; c < count; c++) {
            double cx = r.nextDouble() * w, cy = sky.getHeight() * (0.05 + r.nextDouble() * reach), s = (0.5 + r.nextDouble() * 1.1) * sky.getHeight() / 340.0;
            for (int i = 0; i < 26; i++) {
                double ox = (r.nextDouble() - 0.5) * 220 * s, oy = (r.nextDouble() - 0.5) * 40 * s, rr = (30 + r.nextDouble() * 40) * s;
                Color col = RoadLook.mix(lit, dark, oy / (40 * s) + 0.5);
                for (int k = -1; k <= 1; k++) {   // drawn three times, so it wraps around
                    double x = cx + ox + k * w;
                    if (x + rr < 0 || x - rr > w) continue;
                    g.setPaint(new RadialGradientPaint(new Point2D.Double(x, cy + oy), (float) rr, new float[]{0f, 1f},
                            new Color[]{new Color(col.getRed(), col.getGreen(), col.getBlue(), 140), new Color(col.getRed(), col.getGreen(), col.getBlue(), 0)}));
                    g.fill(new Ellipse2D.Double(x - rr, cy + oy - rr, rr * 2, rr * 2));
                }
            }
        }
        g.dispose();
    }

    // --8<-- [start:range]
    /**
     * A range of mountains {@code width} × {@code height}, transparent above the ridge. The ridge rises up to
     * {@code height} from the bottom; {@code rock} is its colour in the sun, {@code haze} the air in front of
     * it, {@code hazeAmount} how much of that air there is (0 to 1). Peaks higher than {@code snowLine} (a part
     * of the height, 0 for no snow) carry snow; a {@code mesa} range has flat tops.
     */
    public static BufferedImage range(int width, int height, int seed, double roughness, Color rock, Color haze,
                                      double hazeAmount, double snowLine, boolean mesa) {
        BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        double[] hgt = new double[width];
        for (int x = 0; x < width; x++) {
            // around a circle, so the right edge meets the left
            double a = x * 2 * Math.PI / width;
            double v = Noise.fbm(Math.cos(a) * roughness + 50, Math.sin(a) * roughness + 50, seed, 6);
            v = Math.max(0, Math.min(1, (v - 0.25) / 0.6));
            if (mesa) v = v > 0.55 ? 0.62 + 0.04 * Noise.noise(x * 0.05, seed + 3) : v * 0.9;
            hgt[x] = v;
        }
        for (int x = 0; x < width; x++) {
            int top = (int) (height - hgt[x] * height);
            double slope = (hgt[(x + 6) % width] - hgt[(x - 6 + width) % width]) * height / 6;
            for (int y = Math.max(0, top); y < height; y++) {
                double depth = (y - top) / (double) Math.max(1, height - top);
                double gully = Noise.fbm(x * 0.03, y * 0.015, seed + 7, 4) - 0.5;
                double light = 0.86 + Math.max(-0.35, Math.min(0.35, slope * 0.35 + gully * 0.5)) - depth * 0.12;
                double tex = 0.95 + 0.1 * Noise.fbm(x * 0.08, y * 0.08, seed + 1, 3);
                Color c = RoadRenderer.scale(rock, light * tex);
                if (snowLine > 0 && hgt[x] > snowLine && (y - top) < (hgt[x] - snowLine) * height * (0.55 + 0.6 * Noise.fbm(x * 0.04, y * 0.05, seed + 5, 3))) {
                    c = RoadRenderer.scale(new Color(240, 242, 250), 0.82 + Math.max(-0.3, Math.min(0.25, slope * 0.35 + gully * 0.6)));
                }
                c = RoadLook.mix(c, haze, hazeAmount + (1 - hazeAmount) * Math.pow(y / (double) height, 3) * 0.5);
                im.setRGB(x, y, c.getRGB());
            }
        }
        return im;
    }
    // --8<-- [end:range]

    /** A line of tree tops along the bottom, as a forest on the horizon. */
    public static BufferedImage treeLine(int width, int height, long seed, Color colour) {
        BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(im);
        Random r = new Random(seed);
        java.awt.geom.Path2D.Double p = new java.awt.geom.Path2D.Double();
        p.moveTo(0, height);
        double x = 0;
        while (x < width) {
            double th = height * (0.4 + r.nextDouble() * 0.6), tw = 6 + r.nextDouble() * 10;
            double gy = height - height * 0.25 * Noise.fbm(x * 0.01, (int) seed, 3);
            p.lineTo(x, gy);
            p.lineTo(Math.min(width, x + tw / 2), gy - th);
            p.lineTo(Math.min(width, x + tw), gy);
            x += tw * 0.7;
        }
        p.lineTo(width, height);
        p.closePath();
        g.setColor(colour);
        g.fill(p);
        g.dispose();
        return im;
    }

    private static Graphics2D graphics(BufferedImage im) {
        Graphics2D g = im.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return g;
    }
}
// --8<-- [end:panorama]
