package eu.amadeos.gameengine.pinball;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * A steel ball, drawn as a mirror: each pixel of the sphere shows what its surface reflects towards the
 * eye. Reflections that go up show the bright room above the machine, those near the horizon the dark
 * cabinet walls, those going down the lit playfield in its colour, with the coloured dots of its lamps. Two
 * window lights make the highlights, and the rim reflects more than the middle (Fresnel), which gives the
 * ball its weight.
 *
 * <p>That is a few dozen operations per pixel, too slow for every frame, so the pictures are made once per
 * size (whole pixels) and kept. Draw a ball with {@link #draw}, in screen pixels: its size shrinks with the
 * perspective, which picks another of the kept pictures.
 */
public final class ChromeBall {
    private final Color floor;
    private final Color[] lamps;
    private final Map<Integer, BufferedImage> sizes = new HashMap<>();

    /** A ball over a playfield of colour {@code floor}, reflecting lamps of the given colours. */
    public ChromeBall(Color floor, Color... lamps) {
        this.floor = floor;
        this.lamps = lamps.length == 0 ? new Color[]{new Color(0xfff0d0)} : lamps.clone();
    }

    /** Draws the ball centred at {@code (cx, cy)} with radius {@code r}, all in the pixels of {@code g}'s device. */
    public void draw(Graphics2D g, double cx, double cy, double r) {
        int d = Math.max(3, (int) Math.round(2 * r));
        BufferedImage img = sizes.computeIfAbsent(d, this::render);
        AffineTransform t = g.getTransform();
        g.setTransform(new AffineTransform());
        g.drawImage(img, (int) Math.round(t.getTranslateX() + cx * t.getScaleX() - d / 2.0),
                (int) Math.round(t.getTranslateY() + cy * t.getScaleY() - d / 2.0), null);
        g.setTransform(t);
    }

    /** The picture of a ball {@code d} pixels across. */
    public synchronized BufferedImage picture(int d) {
        return sizes.computeIfAbsent(d, this::render);
    }

    // --8<-- [start:render]
    BufferedImage render(int d) {
        BufferedImage img = new BufferedImage(d, d, BufferedImage.TYPE_INT_ARGB);
        double r = d / 2.0;
        Random rnd = new Random(5);
        double[][] spots = new double[14][];
        for (int i = 0; i < spots.length; i++) {
            Color c = lamps[rnd.nextInt(lamps.length)];
            spots[i] = new double[]{rnd.nextDouble() * 1.6 - 0.8, 0.35 + rnd.nextDouble() * 0.55, c.getRed(), c.getGreen(), c.getBlue()};
        }
        for (int py = 0; py < d; py++) {
            for (int px = 0; px < d; px++) {
                double ar = 0, ag = 0, ab = 0;
                int n = 0;
                for (int s = 0; s < 9; s++) {
                    double nx = (px + (s % 3 + 0.5) / 3 - r) / r, ny = (py + (s / 3 + 0.5) / 3 - r) / r;
                    double rr = nx * nx + ny * ny;
                    if (rr > 1) continue;
                    double nz = Math.sqrt(1 - rr);
                    // the view ray (0, 0, -1) reflected about the normal
                    double rx = 2 * nz * nx, ry = 2 * nz * ny;
                    double up = -ry;
                    double tr = clamp((up - 0.02) / 0.9), tf = clamp((-up - 0.05) / 0.8);
                    double wRoom = smooth(0, 0.18, up), wFloor = smooth(0, 0.2, -up), wDark = Math.max(0, 1 - wRoom - wFloor);
                    double cr = (150 + 100 * tr) * wRoom + (0.5 * (floor.getRed() * 2.2 + 30) + 0.5 * (110 + 60 * tf)) * wFloor + 58 * wDark;
                    double cg = (152 + 98 * tr) * wRoom + (0.5 * (floor.getGreen() * 2.2 + 25) + 0.5 * (110 + 60 * tf)) * wFloor + 56 * wDark;
                    double cb = (165 + 88 * tr) * wRoom + (0.5 * (floor.getBlue() * 2.2 + 30) + 0.5 * (118 + 60 * tf)) * wFloor + 66 * wDark;
                    if (up < -0.1) {
                        for (double[] l : spots) {
                            double k = Math.exp(-((rx - l[0]) * (rx - l[0]) + (-up - l[1]) * (-up - l[1])) / 0.0015);
                            cr += (l[2] - cr) * k;
                            cg += (l[3] - cg) * k;
                            cb += (l[4] - cb) * k;
                        }
                    }
                    double fres = 0.55 + 0.45 * (1 - nz) * (1 - nz);
                    cr = cr * fres + 40 * (1 - fres);
                    cg = cg * fres + 40 * (1 - fres);
                    cb = cb * fres + 46 * (1 - fres);
                    double h1 = Math.exp(-((nx + 0.38) * (nx + 0.38) + (ny + 0.42) * (ny + 0.42)) / 0.012);
                    double h2 = Math.exp(-((nx - 0.42) * (nx - 0.42) + (ny + 0.2) * (ny + 0.2)) / 0.004);
                    double hl = Math.min(1, h1 * 1.2 + h2 * 0.8);
                    ar += Math.min(255, cr + (255 - cr) * hl);
                    ag += Math.min(255, cg + (255 - cg) * hl);
                    ab += Math.min(255, cb + (255 - cb) * hl);
                    n++;
                }
                if (n == 0) continue;
                int a = (int) Math.round(255.0 * n / 9);
                img.setRGB(px, py, (a << 24) | ((int) (ar / n) << 16) | ((int) (ag / n) << 8) | (int) (ab / n));
            }
        }
        return img;
    }
    // --8<-- [end:render]

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private static double smooth(double e0, double e1, double x) {
        double t = clamp((x - e0) / (e1 - e0));
        return t * t * (3 - 2 * t);
    }
}
