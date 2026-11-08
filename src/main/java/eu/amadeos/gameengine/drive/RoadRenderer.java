package eu.amadeos.gameengine.drive;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws a {@link Road} as seen from a {@link RoadCamera}: ground, road, walls, scenery and cars, with fog in
 * the distance and, if the game wants, night.
 *
 * <p>Every segment's two ends are <i>projected</i>: a point {@code dz} ahead of the eye and {@code dx}
 * beside it lands {@code dx · depth / dz} from the middle of the view, and the same for height; so the two
 * edges of a segment become two horizontal lines, and the road between them a trapezoid. Curves come from
 * adding up the segments' {@link Segment#curve} from near to far: each segment is pushed sideways by the
 * sum so far, which bends the picture of the road the way a curve looks. The segments are drawn from far to
 * near, each with its ground, road, walls, scenery and cars, so what is near covers what is behind it, and a
 * hill hides the road beyond it without any further test (the painter's algorithm).
 *
 * <p>Use it on the game loop thread only. The background (sky, mountains) is the game's: draw it first.
 */
// --8<-- [start:renderer]
public final class RoadRenderer {
    /** The colour of the air inside a tunnel: everything far in it fades into this. */
    public static final Color TUNNEL_DARK = new Color(22, 20, 18);
    private static final Color LAMP = new Color(255, 228, 160);

    /** 1 by day, down to 0 in the dark: everything but lights and glowing things fades to {@link #shade}. */
    public double light = 1;
    public Color shade = new Color(8, 10, 28);
    /** Headlights of this car light the road ahead of it; {@code null} for none. */
    public Car headlights;
    // --8<-- [end:renderer]

    private final Path2D.Float path = new Path2D.Float();
    private final List<Proj> projections = new ArrayList<>();
    private final Map<Long, List<Car>> carsBySegment = new HashMap<>();
    private final List<Proj> pool = new ArrayList<>();
    private double horizonY;
    private double scale;
    private double viewWidth;
    private double viewHeight;
    private double camX;
    private double camY;

    /** One segment, projected: its near (N) and far (F) end. */
    static final class Proj {
        Segment seg;
        double zN, zF, sN, sF, cxN, cxF, yN, yF;
    }

    /** Where the horizon of a flat road lay in the last frame, from the top of the view. */
    public double horizonY() {
        return horizonY;
    }

    // --8<-- [start:render]
    /** Draws the view of {@code width} × {@code height} at the origin of {@code g}, with these cars on the road. */
    public void render(Graphics2D g, Road road, RoadCamera cam, double width, double height, List<Car> cars) {
        viewWidth = width;
        viewHeight = height;
        scale = Math.min(width / 2, height * 0.8);
        horizonY = height * cam.horizon;
        camX = cam.x * Road.HALF_WIDTH;
        camY = cam.height + road.heightAt(cam.z + cam.height * cam.depth * 1.9);
        project(road, cam);
        sortCars(cars);
        // Shapes exactly where they are: the default "normalize" moves their edges by a fraction of a pixel,
        // and then the thin segments near the horizon no longer meet.
        Object stroke = g.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        for (int i = projections.size() - 1; i >= 0; i--) {
            Proj p = projections.get(i);
            drawSegment(g, p);
            if (p.seg.left != null) wall(g, p, p.seg.left, -1);
            if (p.seg.right != null) wall(g, p, p.seg.right, 1);
            if (p.seg.tunnel && p.seg.left != null && p.seg.right != null) ceiling(g, p);
            scenery(g, p, cam);
            if (p.seg.portal != null) portal(g, p);
        }
        carsBySegment.clear();
        if (stroke != null) g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, stroke);
    }
    // --8<-- [end:render]

    // --8<-- [start:project]
    private void project(Road road, RoadCamera cam) {
        pool.addAll(projections);
        projections.clear();
        long base = (long) Math.floor(cam.z / Road.SEGMENT);
        double basePart = cam.z / Road.SEGMENT - base;
        double x = 0, dx = -road.segment(base).curve * basePart;
        for (int n = 0; n < cam.drawDistance; n++) {
            long i = base + n;
            if (!road.has(i)) break;
            Segment s = road.segment(i);
            Proj p = pool.isEmpty() ? new Proj() : pool.removeLast();
            p.seg = s;
            p.zN = i * Road.SEGMENT - cam.z;
            p.zF = p.zN + Road.SEGMENT;
            p.yN = s.startY;
            p.yF = s.y;
            // the curve: each segment is pushed sideways by the sum of the bends before it
            p.cxN = x;
            p.cxF = x + dx;
            x += dx;
            dx += s.curve;
            if (p.zF <= 1) {
                pool.add(p);
                continue;
            }
            p.sN = cam.depth / Math.max(p.zN, 1);
            p.sF = cam.depth / p.zF;
            projections.add(p);
        }
    }

    /** Screen x of a point {@code lateral} world units across, at projection scale {@code s}. */
    private double sx(double s, double lateral) {
        return viewWidth / 2 + s * (lateral - camX) * scale;
    }

    /** Screen y of a point {@code wy} world units high. */
    private double sy(double s, double wy) {
        return horizonY - s * (wy - camY) * scale;
    }
    // --8<-- [end:project]

    private void sortCars(List<Car> cars) {
        for (Car c : cars) carsBySegment.computeIfAbsent((long) Math.floor(c.z / Road.SEGMENT), k -> new ArrayList<>()).add(c);
    }

    private double fog(RoadLook look, double z) {
        return 1 - Math.exp(-z / look.fogDistance());
    }

    /** A colour lit by the time of day and faded by fog. */
    private Color tint(Color c, Color fog, double f) {
        Color lit = light >= 1 ? c : RoadLook.mix(shade, c, light);
        return RoadLook.mix(lit, fog, f);
    }

    private Color fogColour(RoadLook look) {
        return light >= 1 ? look.fog() : RoadLook.mix(shade, look.fog(), Math.max(light, 0.15));
    }

    // --8<-- [start:segment]
    private void drawSegment(Graphics2D g, Proj p) {
        Segment s = p.seg;
        RoadLook look = s.look;
        boolean near = p.zN < 1;
        double yN = near ? viewHeight + 50 : sy(p.sN, p.yN), yF = sy(p.sF, p.yF);
        // A segment that faces away (the far side of a hill) is hidden by the nearer ones. Each band reaches a
        // little below its near edge, under the next nearer segment, so no seam shows between them; never
        // above its far edge, or the many thin segments near the horizon would paint over the road beyond.
        if (yN < yF - 0.5) return;
        double f = fog(look, (p.zN + p.zF) / 2);
        Color fogC = s.tunnel ? TUNNEL_DARK : fogColour(look);
        boolean stripe = (s.index / 3) % 2 == 0;
        g.setColor(tint(stripe ? look.grass() : look.grassDark(), fogC, f));
        quad(g, 0, yN + 0.6, viewWidth, yN + 0.6, viewWidth, yF, 0, yF, g.getColor());
        if (s.waterLeft > 0) water(g, p, yN, yF, -1, s.waterLeft, stripe, fogC, f);
        if (s.waterRight > 0) water(g, p, yN, yF, 1, s.waterRight, stripe, fogC, f);
        band(g, p, yN, yF, -1.28, 1.28, tint(look.shoulder(), fogC, f));
        if (look.kerb() != null) band(g, p, yN, yF, -1.12, 1.12, tint(stripe ? look.kerb() : look.kerbDark(), fogC, f));
        band(g, p, yN, yF, -1.04, 1.04, tint(stripe ? look.asphalt() : look.asphaltDark(), fogC, f));
        Color edge = look.glowingLines() ? look.edgeLine() : tint(look.edgeLine(), fogC, f);
        band(g, p, yN, yF, -1.005, -0.935, edge);
        band(g, p, yN, yF, 0.935, 1.005, edge);
        if (s.index % 4 < 2) band(g, p, yN, yF, -0.028, 0.028, look.glowingLines() ? look.centreLine() : tint(look.centreLine(), fogC, f));
        if (look.glowingLines() && s.index % 4 == 0 && !near) {
            double r = Math.max(1.5, p.sN * 360 * scale);
            Color c = new Color(look.edgeLine().getRed(), look.edgeLine().getGreen(), look.edgeLine().getBlue(), 170);
            glow(g, sx(p.sN, p.cxN - Road.HALF_WIDTH), yN, r, c);
            glow(g, sx(p.sN, p.cxN + Road.HALF_WIDTH), yN, r, c);
        }
    }
    // --8<-- [end:segment]

    /** A band of the road between {@code a} and {@code b} half-widths from the centre. */
    private void band(Graphics2D g, Proj p, double yN, double yF, double a, double b, Color c) {
        double hw = Road.HALF_WIDTH;
        path.reset();
        path.moveTo(sx(p.sN, p.cxN + a * hw), yN + 0.6);
        path.lineTo(sx(p.sN, p.cxN + b * hw), yN + 0.6);
        path.lineTo(sx(p.sF, p.cxF + b * hw), yF);
        path.lineTo(sx(p.sF, p.cxF + a * hw), yF);
        path.closePath();
        g.setColor(c);
        g.fill(path);
    }

    private void water(Graphics2D g, Proj p, double yN, double yF, int side, double from, boolean stripe, Color fogC, double f) {
        RoadLook look = p.seg.look;
        double hw = Road.HALF_WIDTH;
        double aN = sx(p.sN, p.cxN + side * from * hw), aF = sx(p.sF, p.cxF + side * from * hw);
        double edge = side < 0 ? -10 : viewWidth + 10;
        path.reset();
        path.moveTo(edge, yN + 0.6);
        path.lineTo(aN, yN + 0.6);
        path.lineTo(aF, yF);
        path.lineTo(edge, yF);
        path.closePath();
        g.setColor(tint(stripe ? look.water() : look.waterDark(), fogC, f * 0.8));
        g.fill(path);
        double bN = sx(p.sN, p.cxN + side * (from - 0.25) * hw), bF = sx(p.sF, p.cxF + side * (from - 0.25) * hw);
        path.reset();
        path.moveTo(aN, yN + 0.6);
        path.lineTo(bN, yN + 0.6);
        path.lineTo(bF, yF);
        path.lineTo(aF, yF);
        path.closePath();
        g.setColor(tint(look.beach(), fogC, f));
        g.fill(path);
    }

    // --8<-- [start:wall]
    /** A wall's face towards the road, in bands of its colours from the bottom up. */
    private void wall(Graphics2D g, Proj p, Wall w, int side) {
        double hw = Road.HALF_WIDTH;
        double latN = p.cxN + side * w.offset() * hw, latF = p.cxF + side * w.offset() * hw;
        double hN = w.heightAt(p.seg.index), hF = w.heightAt(p.seg.index + 1);
        double xN = sx(p.sN, latN), xF = sx(p.sF, latF);
        boolean near = p.zN < 1;
        double f = fog(p.seg.look, (p.zN + p.zF) / 2);
        Color fogC = w.kind() == Wall.Kind.TUNNEL ? TUNNEL_DARK : fogColour(p.seg.look);
        // the light comes from the left: the right wall's face, turned to the left, is lit
        double sun = side > 0 ? 1.0 : 0.66;
        switch (w.kind()) {
            case CLIFF -> {
                Color[] strata = w.colours();
                double maxH = w.height() * 1.3;
                int bands = 14;
                for (int b = 0; b < bands; b++) {
                    double a0 = maxH * b / bands, a1 = maxH * (b + 1) / bands;
                    double n0 = Math.min(a0, hN), n1 = Math.min(a1, hN), f0 = Math.min(a0, hF), f1 = Math.min(a1, hF);
                    if (n0 >= n1 && f0 >= f1) continue;
                    Color c = strata[(b + (int) (Noise.hash(b, w.seed()) * 2)) % strata.length];
                    double streak = 0.9 + 0.2 * Noise.fbm(p.seg.index * 0.18 + b * 3.7, w.seed() + 5, 3);
                    double up = 0.78 + 0.3 * b / bands;
                    quad(g, xN, near ? viewHeight + 400 : sy(p.sN, p.yN + n0), xF, sy(p.sF, p.yF + f0),
                            xF, sy(p.sF, p.yF + f1), xN, near ? -400 : sy(p.sN, p.yN + n1),
                            tint(scale(c, sun * streak * up), fogC, f));
                }
                if (!near) {
                    double gN = sy(p.sN, p.yN), tN = sy(p.sN, p.yN + hN);
                    if (Noise.hash(p.seg.index, w.seed() + 9) < 0.18) {
                        g.setColor(tint(new Color(40, 15, 10), fogC, f));
                        g.setStroke(new BasicStroke((float) Math.max(0.5, p.sN * 14 * scale)));
                        g.draw(new Line2D.Double(xN, gN, xN, tN));
                    }
                    g.setColor(tint(scale(strata[strata.length - 1], 1.25), fogC, f));
                    g.setStroke(new BasicStroke((float) Math.max(0.5, p.sN * 50 * scale)));
                    g.draw(new Line2D.Double(xN, tN, xF, sy(p.sF, p.yF + hF)));
                }
            }
            case TUNNEL -> {
                double split = w.height() * 0.45;
                double gN = near ? viewHeight + 400 : sy(p.sN, p.yN), gF = sy(p.sF, p.yF);
                double mN = near ? viewHeight / 2 : sy(p.sN, p.yN + split), mF = sy(p.sF, p.yF + split);
                double tN = near ? -400 : sy(p.sN, p.yN + hN), tF = sy(p.sF, p.yF + hF);
                quad(g, xN, gN, xF, gF, xF, mF, xN, mN, tint(scale(w.colours()[0], sun), fogC, f));
                quad(g, xN, mN, xF, mF, xF, tF, xN, tN, tint(scale(w.colours()[1], sun), fogC, f));
                if (!near) {
                    double s1 = sy(p.sN, p.yN + 160), s2 = sy(p.sF, p.yF + 160);
                    quad(g, xN, s1, xF, s2, xF, s2 - Math.max(0.5, p.sF * 60 * scale), xN, s1 - Math.max(0.5, p.sN * 60 * scale),
                            RoadLook.mix(new Color(230, 170, 40), fogC, f * 0.7));
                    if (p.seg.index % 3 == 0) {
                        g.setColor(tint(new Color(20, 20, 18), fogC, f));
                        g.setStroke(new BasicStroke((float) Math.max(0.5, p.sN * 20 * scale)));
                        g.draw(new Line2D.Double(xN, gN, xN, tN));
                    }
                }
            }
            case HOUSES -> houses(g, p, w, side, xN, xF, hN, hF, sun, fogC, f, near);
        }
    }
    // --8<-- [end:wall]

    private void houses(Graphics2D g, Proj p, Wall w, int side, double xN, double xF, double hN, double hF, double sun,
                        Color fogC, double f, boolean near) {
        Color[] facades = w.colours();
        Color c = facades[(int) (Noise.hash(p.seg.index / 4, w.seed()) * facades.length)];
        double gN = near ? viewHeight + 400 : sy(p.sN, p.yN), gF = sy(p.sF, p.yF);
        double tN = near ? -400 : sy(p.sN, p.yN + hN), tF = sy(p.sF, p.yF + hF);
        quad(g, xN, gN, xF, gF, xF, tF, xN, tN, tint(scale(c, sun), fogC, f * 0.9));
        if (near) return;
        int floors = (int) (Math.min(hN, hF) / 320);
        double off = side * w.offset() * Road.HALF_WIDTH;
        for (int fl = 0; fl < floors; fl++) {
            double y0 = 200 + fl * 320, y1 = y0 + 170;
            for (int col = 0; col < 2; col++) {
                double t0 = 0.15 + col * 0.45, t1 = t0 + 0.3;
                boolean on = w.litWindows() && Noise.hash(p.seg.index * 31 + fl * 7L + col, w.seed()) < 0.6;
                double sa = 1 / (1 / p.sN * (1 - t0) + 1 / p.sF * t0), sb = 1 / (1 / p.sN * (1 - t1) + 1 / p.sF * t1);
                double ya = p.yN + (p.yF - p.yN) * t0, yb = p.yN + (p.yF - p.yN) * t1;
                double ax = sx(sa, p.cxN + (p.cxF - p.cxN) * t0 + off), bx = sx(sb, p.cxN + (p.cxF - p.cxN) * t1 + off);
                Color wc = on ? RoadLook.mix(new Color(255, 205, 120), fogC, f * 0.4) : tint(new Color(60, 70, 90), fogC, f);
                quad(g, ax, sy(sa, ya + y0), bx, sy(sb, yb + y0), bx, sy(sb, yb + y1), ax, sy(sa, ya + y1), wc);
            }
        }
        if (p.seg.index % 4 == 0) {
            g.setColor(tint(scale(c, 0.5), fogC, f));
            g.setStroke(new BasicStroke((float) Math.max(0.5, p.sN * 25 * scale)));
            g.draw(new Line2D.Double(xN, gN, xN, tN));
        }
    }

    private void ceiling(Graphics2D g, Proj p) {
        Segment s = p.seg;
        double hw = Road.HALF_WIDTH, h = s.left.height();
        double lN = sx(p.sN, p.cxN - s.left.offset() * hw), rN = sx(p.sN, p.cxN + s.right.offset() * hw);
        double lF = sx(p.sF, p.cxF - s.left.offset() * hw), rF = sx(p.sF, p.cxF + s.right.offset() * hw);
        double tN = p.zN < 1 ? -400 : sy(p.sN, p.yN + h), tF = sy(p.sF, p.yF + h);
        double f = fog(s.look, (p.zN + p.zF) / 2);
        quad(g, lN, tN, rN, tN, rF, tF, lF, tF, tint(new Color(70, 68, 62), TUNNEL_DARK, f));
        if (s.index % 2 == 0 && p.zN > 1) {
            double aN = sx(p.sN, p.cxN - 0.12 * hw), bN = sx(p.sN, p.cxN + 0.12 * hw);
            double aF = sx(p.sF, p.cxF - 0.12 * hw), bF = sx(p.sF, p.cxF + 0.12 * hw);
            double mid = (tN + tF) / 2;
            quad(g, aN, tN, bN, tN, bF, mid, aF, mid, LAMP);
            glow(g, (aN + bF) / 2, (tN + mid) / 2, Math.max(3, (bN - aN) * 1.6), new Color(255, 210, 120, 90));
        }
    }

    // --8<-- [start:portal]
    /** The mountain face around a tunnel mouth: a wide wall with a rough top and an arched hole. */
    private void portal(Graphics2D g, Proj p) {
        if (p.zN < 1) return;
        double s = p.sN, cy = p.yN, hw = Road.HALF_WIDTH;
        Segment seg = p.seg;
        double th = seg.left == null ? 1500 : seg.left.height(), half = seg.left == null ? 1.35 : seg.left.offset();
        double f = fog(seg.look, p.zN);
        Color fogC = fogColour(seg.look);
        Path2D face = new Path2D.Double();
        double from = -42, to = 42;
        face.moveTo(sx(s, p.cxN + from * hw), sy(s, cy - 200));
        for (int i = 0; i <= 60; i++) {
            double u = from + (to - from) * i / 60.0;
            double top = 5200 + 4200 * Noise.fbm(u * 0.08, 77 + (int) (seg.index % 1000), 4) - Math.abs(u) * 70;
            face.lineTo(sx(s, p.cxN + u * hw), sy(s, cy + top));
        }
        face.lineTo(sx(s, p.cxN + to * hw), sy(s, cy - 200));
        face.closePath();
        Area rock = new Area(face);
        double x0 = sx(s, p.cxN - half * hw), x1 = sx(s, p.cxN + half * hw);
        double y0 = sy(s, cy), y1 = sy(s, cy + th), r = (x1 - x0) / 2;
        Path2D arch = new Path2D.Double();
        arch.moveTo(x0, y0 + 2);
        arch.lineTo(x0, y1 + r * 0.35);
        arch.quadTo(x0, y1 - r * 0.15, (x0 + x1) / 2, y1 - r * 0.18);
        arch.quadTo(x1, y1 - r * 0.15, x1, y1 + r * 0.35);
        arch.lineTo(x1, y0 + 2);
        arch.closePath();
        rock.subtract(new Area(arch));
        Shape clip = g.getClip();
        g.clip(rock);
        Rectangle2D box = rock.getBounds2D();
        Color[] strata = seg.portal;
        double bandH = Math.max(1, s * 260 * scale);
        int count = (int) (box.getHeight() / bandH) + 2;
        for (int b = 0; b < count; b++) {
            double y = box.getMaxY() - b * bandH;
            g.setColor(tint(scale(strata[b % strata.length], 0.95 + 0.15 * Noise.hash(b, 3)), fogC, f));
            g.fill(new Rectangle2D.Double(box.getX(), y - bandH, box.getWidth(), bandH + 1));
        }
        g.setPaint(new GradientPaint((float) box.getX(), 0, new Color(255, 200, 150, 40), (float) box.getMaxX(), 0, new Color(40, 10, 20, 90)));
        g.fill(box);
        g.setClip(clip);
        g.setColor(tint(new Color(190, 184, 170), fogC, f));
        g.setStroke(new BasicStroke((float) Math.max(1, s * 220 * scale)));
        Path2D ring = new Path2D.Double();
        ring.moveTo(x0, y0);
        ring.lineTo(x0, y1 + r * 0.35);
        ring.quadTo(x0, y1 - r * 0.15, (x0 + x1) / 2, y1 - r * 0.18);
        ring.quadTo(x1, y1 - r * 0.15, x1, y1 + r * 0.35);
        ring.lineTo(x1, y0);
        g.draw(ring);
    }
    // --8<-- [end:portal]

    // --8<-- [start:scenery]
    /** The scenery and cars of a segment, from its far end to its near end. */
    private void scenery(Graphics2D g, Proj p, RoadCamera cam) {
        List<Car> cars = carsBySegment.get(p.seg.index);
        List<Scenery> things = p.seg.scenery;
        if (things.isEmpty() && cars == null) return;
        List<Object> order = new ArrayList<>(things.size() + (cars == null ? 0 : cars.size()));
        order.addAll(things);
        if (cars != null) order.addAll(cars);
        order.sort((a, b) -> Double.compare(along(b), along(a)));
        Color fogC = p.seg.tunnel ? TUNNEL_DARK : fogColour(p.seg.look);
        for (Object o : order) {
            if (o instanceof Scenery s) draw(g, p, s.sprite(), null, s.offset(), s.along(), 0, s.flipped(), fogC);
            else if (o instanceof Car c) {
                if (c == headlights) headlights(g, p, c, cam);
                draw(g, p, c.sprite, c.lights ? c.glowSprite : null, c.x, c.z / Road.SEGMENT - p.seg.index, c.lift, c.flipped, fogC);
            }
        }
    }

    private static double along(Object o) {
        return o instanceof Scenery s ? s.along() : ((Car) o).z / Road.SEGMENT - Math.floor(((Car) o).z / Road.SEGMENT);
    }

    /** A sprite standing {@code along} the segment, {@code offset} half-widths from the centre; scale is perspective-correct. */
    private void draw(Graphics2D g, Proj p, RoadSprite sprite, RoadSprite glowing, double offset, double along, double lift, boolean flip, Color fogC) {
        double inv = (1 / p.sN) * (1 - along) + (1 / p.sF) * along;
        double s = 1 / inv;
        double z = p.zN + (p.zF - p.zN) * along;
        if (z < 30) return;
        double gx = p.cxN + (p.cxF - p.cxN) * along + offset * Road.HALF_WIDTH;
        double gy = p.yN + (p.yF - p.yN) * along + lift;
        double x = sx(s, gx), y = sy(s, gy);
        double pw = s * sprite.worldWidth() * scale;
        double ph = pw * sprite.art().getHeight() / sprite.art().getWidth();
        if (pw < 0.4 || x + pw < 0 || x - pw > viewWidth || y - ph > viewHeight) return;
        double device = pw * Math.abs(g.getTransform().getScaleX());
        double f = fog(p.seg.look, z);
        paint(g, sprite.copy(device, f, fogC, light, shade), x, y, pw, ph, flip);
        if (glowing != null) paint(g, glowing.copy(device, 0, fogC, 1, shade), x, y, pw, ph, flip);
    }
    // --8<-- [end:scenery]

    private static void paint(Graphics2D g, BufferedImage im, double x, double y, double pw, double ph, boolean flip) {
        double left = x - pw / 2, top = y - ph;
        if (flip) g.drawImage(im, (int) Math.round(left + pw), (int) Math.round(top), (int) -Math.round(pw), (int) Math.round(ph), null);
        else g.drawImage(im, (int) Math.round(left), (int) Math.round(top), (int) Math.round(pw), (int) Math.round(ph), null);
    }

    /** Coordinates in logical units are rounded only where an image is placed; a shape keeps its fractions. */
    private void quad(Graphics2D g, double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4, Color c) {
        path.reset();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.lineTo(x4, y4);
        path.closePath();
        g.setColor(c);
        g.fill(path);
    }

    // --8<-- [start:headlights]
    /** A pool of light on the road ahead of the car, fading with distance. */
    private void headlights(Graphics2D g, Proj p, Car car, RoadCamera cam) {
        double z0 = car.z + car.length * 0.4 - cam.z, z1 = z0 + 7000;
        if (z0 < 1) return;
        double s0 = cam.depth / z0, s1 = cam.depth / z1, hw = Road.HALF_WIDTH;
        double gy = p.yN;
        double y0 = sy(s0, gy), y1 = sy(s1, gy);
        double cx = p.cxN + car.x * hw;
        Path2D pool = new Path2D.Double();
        pool.moveTo(sx(s0, cx - 0.45 * hw), y0);
        pool.lineTo(sx(s0, cx + 0.45 * hw), y0);
        pool.lineTo(sx(s1, cx + 1.3 * hw), y1);
        pool.lineTo(sx(s1, cx - 1.3 * hw), y1);
        pool.closePath();
        double strength = Math.max(0, 1 - light);
        g.setPaint(new LinearGradientPaint((float) 0, (float) y0, 0f, (float) Math.min(y0 - 1, y1), new float[]{0f, 0.25f, 1f},
                new Color[]{new Color(255, 244, 205, 0), new Color(255, 244, 205, (int) (90 * strength)), new Color(255, 244, 205, 0)}));
        g.fill(pool);
    }
    // --8<-- [end:headlights]

    static Color scale(Color c, double f) {
        return new Color(clamp(c.getRed() * f), clamp(c.getGreen() * f), clamp(c.getBlue() * f), c.getAlpha());
    }

    private static int clamp(double v) {
        return (int) Math.max(0, Math.min(255, Math.round(v)));
    }

    static void glow(Graphics2D g, double x, double y, double r, Color c) {
        if (r < 0.5) return;
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float) r, new float[]{0f, 0.35f, 1f},
                new Color[]{c, new Color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() / 3), new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)}));
        g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
    }
}
