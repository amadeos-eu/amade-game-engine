package eu.amadeos.gameengine.pinball;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Draws a {@link Track} into a {@link Relief}: a ramp of tinted plastic with walls, a lip underneath and
 * metal supports down to the floor, or a wire form of four rails held together by ties. The heights come from
 * the track itself, so the picture rises and falls with the path the ball rides.
 */
public final class RampArt {
    private RampArt() {
    }

    // --8<-- [start:plastic]
    /** A plastic ramp {@code width} units wide along the track, in {@code color}, with walls {@code wall} high. */
    public static void plastic(Relief relief, Track track, double width, Color color, double wall) {
        Graphics2D g = relief.parts();
        Edges e = edges(track, width);
        supports(relief, g, e, 22);
        double thick = 2.4;
        Color lite = new Color(Math.min(255, color.getRed() + 70), Math.min(255, color.getGreen() + 70),
                Math.min(255, color.getBlue() + 70));
        for (int i = 0; i < e.n - 1; i++) {
            boolean leftNear = (e.l[i][1] + e.l[i + 1][1]) > (e.r[i][1] + e.r[i + 1][1]);
            double[][] near = leftNear ? e.l : e.r, far = leftNear ? e.r : e.l;
            double ex = near[i + 1][0] - near[i][0], ey = near[i + 1][1] - near[i][1];
            double lightF = 0.75 + 0.35 * Math.abs(ey / Math.max(1e-6, Math.hypot(ex, ey)));
            Color dark = Relief.shade(color, 0.55);
            g.setColor(new Color(dark.getRed(), dark.getGreen(), dark.getBlue(), 230));
            g.fill(strip(far, e.h, i, 0, wall));
            Path2D bed = new Path2D.Double();
            bed.moveTo(e.l[i][0], e.l[i][1] - e.h[i]);
            bed.lineTo(e.l[i + 1][0], e.l[i + 1][1] - e.h[i + 1]);
            bed.lineTo(e.r[i + 1][0], e.r[i + 1][1] - e.h[i + 1]);
            bed.lineTo(e.r[i][0], e.r[i][1] - e.h[i]);
            bed.closePath();
            Color bedC = Relief.shade(color, 0.8);
            g.setColor(new Color(bedC.getRed(), bedC.getGreen(), bedC.getBlue(), 200));
            g.fill(bed);
            g.setStroke(new BasicStroke(0.35f));
            g.draw(bed);
            g.setColor(Relief.shade(color, 0.35));
            g.fill(strip(near, e.h, i, -thick, 0));
            Color face = Relief.shade(color, lightF);
            g.setPaint(new GradientPaint(0f, (float) (near[i][1] - e.h[i] - wall), new Color(lite.getRed(), lite.getGreen(), lite.getBlue(), 235),
                    0f, (float) (near[i][1] - e.h[i]), new Color(face.getRed(), face.getGreen(), face.getBlue(), 235)));
            g.fill(strip(near, e.h, i, 0, wall));
        }
        for (int k = 0; k < 3; k++) {
            Path2D line = new Path2D.Double();
            for (int i = 0; i < e.n; i++) {
                double x, y;
                if (k == 0) { x = e.l[i][0]; y = e.l[i][1] - e.h[i] - wall; }
                else if (k == 1) { x = e.r[i][0]; y = e.r[i][1] - e.h[i] - wall; }
                else { x = e.l[i][0] * 0.65 + e.r[i][0] * 0.35; y = e.l[i][1] * 0.65 + e.r[i][1] * 0.35 - e.h[i]; }
                if (i == 0) line.moveTo(x, y); else line.lineTo(x, y);
            }
            if (k < 2) {
                g.setColor(Relief.shade(color, 0.5));
                g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(AffineTransform.getTranslateInstance(0.3, 0.6).createTransformedShape(line));
                g.setColor(new Color(Math.min(255, color.getRed() + 90), Math.min(255, color.getGreen() + 90),
                        Math.min(255, color.getBlue() + 90)));
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(line);
                g.setColor(new Color(255, 255, 255, 230));
                g.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(AffineTransform.getTranslateInstance(-0.3, -0.4).createTransformedShape(line));
            } else {
                g.setColor(new Color(255, 255, 255, 80));
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(line);
            }
        }
    }
    // --8<-- [end:plastic]

    /** A wire form along the track: four chrome rails with ties every few units, on supports. */
    public static void wire(Relief relief, Track track, double width) {
        Graphics2D g = relief.parts();
        Edges e = edges(track, width);
        supports(relief, g, e, 22);
        double tie = 0;
        for (int i = 0; i < e.n; i++) {
            if (e.s[i] < tie) continue;
            tie += 5;
            g.setColor(new Color(0x9a9aa8));
            g.setStroke(new BasicStroke(0.5f));
            Path2D t = new Path2D.Double();
            t.moveTo(e.l[i][0], e.l[i][1] - e.h[i] - 3);
            t.lineTo(e.l[i][0] * 0.7 + e.r[i][0] * 0.3, e.l[i][1] * 0.7 + e.r[i][1] * 0.3 - e.h[i]);
            t.lineTo(e.l[i][0] * 0.3 + e.r[i][0] * 0.7, e.l[i][1] * 0.3 + e.r[i][1] * 0.7 - e.h[i]);
            t.lineTo(e.r[i][0], e.r[i][1] - e.h[i] - 3);
            g.draw(t);
        }
        for (int k = 0; k < 4; k++) {
            Path2D rail = new Path2D.Double();
            for (int i = 0; i < e.n; i++) {
                double rx, ry;
                if (k < 2) {
                    double[] p = k == 0 ? e.l[i] : e.r[i];
                    rx = p[0];
                    ry = p[1] - e.h[i] - 3;
                } else {
                    double f = k == 2 ? 0.3 : 0.7;
                    rx = e.l[i][0] * (1 - f) + e.r[i][0] * f;
                    ry = e.l[i][1] * (1 - f) + e.r[i][1] * f - e.h[i];
                }
                if (i == 0) rail.moveTo(rx, ry); else rail.lineTo(rx, ry);
            }
            g.setColor(new Color(0x5a5a68));
            g.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(rail);
            g.setColor(new Color(0xf4f4ff));
            g.setStroke(new BasicStroke(0.45f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(rail);
        }
    }

    private record Edges(int n, double[][] l, double[][] r, double[] h, double[] s) {
    }

    private static Edges edges(Track track, double width) {
        List<double[]> pts = track.samples(0.6);
        int n = pts.size();
        double[][] l = new double[n][2], r = new double[n][2];
        double[] h = new double[n], s = new double[n];
        for (int i = 0; i < n; i++) {
            double[] a = pts.get(Math.max(0, i - 1)), b = pts.get(Math.min(n - 1, i + 1));
            double dx = b[0] - a[0], dy = b[1] - a[1], d = Math.max(1e-6, Math.hypot(dx, dy));
            double nx = -dy / d * width / 2, ny = dx / d * width / 2;
            double[] p = pts.get(i);
            h[i] = p[2];
            s[i] = i == 0 ? 0 : s[i - 1] + Math.hypot(p[0] - pts.get(i - 1)[0], p[1] - pts.get(i - 1)[1]);
            l[i][0] = p[0] + nx;
            l[i][1] = p[1] + ny;
            r[i][0] = p[0] - nx;
            r[i][1] = p[1] - ny;
        }
        return new Edges(n, l, r, h, s);
    }

    private static void supports(Relief relief, Graphics2D g, Edges e, double every) {
        for (int i = 0; i < e.n - 1; i++) {
            Path2D q = new Path2D.Double();
            q.moveTo(e.l[i][0], e.l[i][1]);
            q.lineTo(e.l[i + 1][0], e.l[i + 1][1]);
            q.lineTo(e.r[i + 1][0], e.r[i + 1][1]);
            q.lineTo(e.r[i][0], e.r[i][1]);
            q.closePath();
            relief.shadow(q, e.h[i]);
        }
        double next = 18;
        for (int i = 0; i < e.n; i++) {
            if (e.s[i] < next || e.h[i] < 3) continue;
            next += every;
            for (double[] p : new double[][]{e.l[i], e.r[i]}) {
                Shape leg = new Rectangle2D.Double(p[0] - 1.0, p[1] - 1.0, 2.0, 2.0);
                relief.shadow(leg, e.h[i]);
                g.setPaint(new GradientPaint((float) p[0] - 1f, 0, new Color(0xf0f0f8), (float) p[0] + 1f, 0, new Color(0x50505c)));
                g.fill(new Rectangle2D.Double(p[0] - 1.0, p[1] - e.h[i], 2.0, e.h[i]));
                g.setColor(new Color(0x8a8a98));
                g.fill(new Ellipse2D.Double(p[0] - 1.8, p[1] - 1.0, 3.6, 2.0));
            }
        }
    }

    private static Shape strip(double[][] edge, double[] h, int i, double lo, double hi) {
        Path2D q = new Path2D.Double();
        q.moveTo(edge[i][0], edge[i][1] - h[i] - lo);
        q.lineTo(edge[i + 1][0], edge[i + 1][1] - h[i + 1] - lo);
        q.lineTo(edge[i + 1][0], edge[i + 1][1] - h[i + 1] - hi);
        q.lineTo(edge[i][0], edge[i][1] - h[i] - hi);
        q.closePath();
        return q;
    }
}
