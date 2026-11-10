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
import java.awt.geom.Line2D;
import java.awt.geom.AffineTransform;
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
    /** A kerb or a line thinner than this many pixels is not drawn. */
    private static final double THINNEST = 0.25;
    private static final AffineTransform IDENTITY = new AffineTransform();
    private static final Color LAMP = new Color(255, 228, 160);

    /** 1 by day, down to 0 in the dark: everything but lights and glowing things fades to {@link #shade}. */
    public double light = 1;
    public Color shade = new Color(8, 10, 28);
    /** Headlights of this car light the road ahead of it; {@code null} for none. */
    public Car headlights;
    /** How far one sees, as a part of each look's fog distance: below 1 in fog and rain. */
    public double sight = 1;
    /** The colour of mist or rain that the fog takes on, by {@link #mistAmount} (0 to 1). */
    public Color mist = new Color(190, 194, 200);
    public double mistAmount;
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
    /** The same surface in its own pixels, without smoothing: for the ground's rectangles. */
    private Graphics2D flat;
    /** The same surface without smoothing: for the faces of walls, whose many small shapes meet edge to edge. */
    private Graphics2D rough;
    public static final long[] T = new long[4];
    private double deviceX, deviceY, deviceScaleX, deviceScaleY;

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
        // The sprite copies are already smooth and at most a fifth too wide: the nearest pixel is enough, and
        // four times faster than a bilinear blit, with hundreds of trees in a frame.
        Object interpolation = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        AffineTransform t = g.getTransform();
        if (t.getShearX() == 0 && t.getShearY() == 0 && t.getScaleX() > 0 && t.getScaleY() > 0) {
            flat = (Graphics2D) g.create();
            flat.setTransform(IDENTITY);
            flat.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            deviceX = t.getTranslateX();
            deviceY = t.getTranslateY();
            deviceScaleX = t.getScaleX();
            deviceScaleY = t.getScaleY();
        }
        rough = (Graphics2D) g.create();
        rough.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        try {
            for (int i = projections.size() - 1; i >= 0; i--) {
                Proj p = projections.get(i);
                drawSegment(g, p);
                if (p.seg.left != null) wall(g, p, p.seg.left, -1);
                if (p.seg.right != null) wall(g, p, p.seg.right, 1);
                if (p.seg.tunnel && p.seg.left != null && p.seg.right != null) ceiling(g, p);
                scenery(g, p, cam);
                if (p.seg.portal != null) portal(g, p);
            }
        } finally {
            if (flat != null) flat.dispose();
            flat = null;
            rough.dispose();
            rough = null;
        }
        carsBySegment.clear();
        if (stroke != null) g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, stroke);
        if (interpolation != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
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
        return 1 - Math.exp(-z / (look.fogDistance() * Math.max(0.05, sight)));
    }

    /** A colour lit by the time of day and faded by fog. */
    private Color tint(Color c, Color fog, double f) {
        Color lit = light >= 1 ? c : RoadLook.mix(shade, c, light);
        return RoadLook.mix(lit, fog, f);
    }

    // --8<-- [start:fog-colour]
    /**
     * The colour the distance fades into for a look, in the light and the mist of now. Light and mist are
     * taken in a few steps, the same steps the sprites' copies are tinted in: a colour that changed a little
     * in every frame of a sunset would need new copies of every sprite in every frame.
     */
    public Color fogColour(RoadLook look) {
        double steps = RoadSprite.LIGHT_STEPS - 1;
        double mistStep = Math.round(Math.max(0, Math.min(1, mistAmount)) * steps) / steps;
        double lightStep = Math.round(Math.max(0, Math.min(1, light)) * steps) / steps;
        Color fog = mistStep > 0 ? RoadLook.mix(look.fog(), mist, mistStep) : look.fog();
        return lightStep >= 1 ? fog : RoadLook.mix(shade, fog, Math.max(lightStep, 0.15));
    }
    // --8<-- [end:fog-colour]

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
        ground(g, yN + 0.6, yF, tint(stripe ? look.grass() : look.grassDark(), fogC, f));
        if (s.waterLeft > 0) water(g, p, yN, yF, -1, s.waterLeft, stripe, fogC, f);
        if (s.waterRight > 0) water(g, p, yN, yF, 1, s.waterRight, stripe, fogC, f);
        // Side by side, not on top of each other: every pixel of the road is smoothed once, not three times.
        // Each strip reaches a little under its inner neighbour, so no grass shows through between them.
        // Far away, where a kerb or a line is thinner than a quarter of a pixel, it is left out.
        double pixels = p.sN * Road.HALF_WIDTH * scale * (flat == null ? 1 : deviceScaleX);
        boolean kerbs = look.kerb() != null && pixels * 0.08 >= THINNEST;
        boolean lines = pixels * 0.07 >= THINNEST;
        bands(g, p, yN, yF, kerbs ? 1.12 : 1.04, 1.28, 1, tint(look.shoulder(), fogC, f));
        if (kerbs) bands(g, p, yN, yF, 1.04, 1.12, 1, tint(stripe ? look.kerb() : look.kerbDark(), fogC, f));
        band(g, p, yN, yF, -1.04, 1.04, tint(stripe ? look.asphalt() : look.asphaltDark(), fogC, f));
        if (!lines && !look.glowingLines()) return;
        Color edge = look.glowingLines() ? look.edgeLine() : tint(look.edgeLine(), fogC, f);
        bands(g, p, yN, yF, 0.935, 1.005, 0, edge);
        if (s.index % 4 < 2) band(g, p, yN, yF, -0.028, 0.028, look.glowingLines() ? look.centreLine() : tint(look.centreLine(), fogC, f));
        if (look.glowingLines() && s.index % 4 == 0 && !near) {
            double r = Math.max(1.5, p.sN * 360 * scale);
            Color c = new Color(look.edgeLine().getRed(), look.edgeLine().getGreen(), look.edgeLine().getBlue(), 170);
            glow(g, sx(p.sN, p.cxN - Road.HALF_WIDTH), yN, r, c);
            glow(g, sx(p.sN, p.cxN + Road.HALF_WIDTH), yN, r, c);
        }
    }
    // --8<-- [end:segment]

    // --8<-- [start:ground]
    /**
     * The ground across the whole view between two heights: a plain rectangle of whole pixels, without
     * smoothing, which costs a fraction of a smoothed shape. It begins at the first row of pixels that lies
     * wholly below the far edge: the row the edge runs through keeps what the segment beyond painted there.
     */
    private void ground(Graphics2D g, double yN, double yF, Color c) {
        if (flat == null) {
            quad(g, 0, yN, viewWidth, yN, viewWidth, yF, 0, yF, c);
            return;
        }
        int top = (int) Math.ceil(deviceY + deviceScaleY * yF);
        int bottom = (int) Math.ceil(deviceY + deviceScaleY * yN);
        if (bottom <= top) return;
        int left = (int) Math.floor(deviceX);
        int right = (int) Math.ceil(deviceX + deviceScaleX * viewWidth);
        flat.setColor(c);
        flat.fillRect(left, top, right - left, bottom - top);
    }
    // --8<-- [end:ground]

    /**
     * Two bands, left and right of the centre, from {@code a} to {@code b} half-widths, in one shape. Each
     * reaches {@code under} logical units towards the centre, under the band that is drawn after it.
     */
    private void bands(Graphics2D g, Proj p, double yN, double yF, double a, double b, double under, Color c) {
        double hw = Road.HALF_WIDTH;
        path.reset();
        // Both run the same way round: where the road is a pixel wide they overlap, and must not cancel out.
        trapezoid(sx(p.sN, p.cxN - b * hw), sx(p.sN, p.cxN - a * hw) + under, sx(p.sF, p.cxF - b * hw), sx(p.sF, p.cxF - a * hw) + under, yN, yF);
        trapezoid(sx(p.sN, p.cxN + a * hw) - under, sx(p.sN, p.cxN + b * hw), sx(p.sF, p.cxF + a * hw) - under, sx(p.sF, p.cxF + b * hw), yN, yF);
        g.setColor(c);
        g.fill(path);
    }

    private void trapezoid(double leftN, double rightN, double leftF, double rightF, double yN, double yF) {
        path.moveTo(leftN, yN + 0.6);
        path.lineTo(rightN, yN + 0.6);
        path.lineTo(rightF, yF);
        path.lineTo(leftF, yF);
        path.closePath();
    }

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
    /**
     * A wall's face towards the road. Its pieces are filled without smoothing: a canyon has thousands of
     * them in a frame, they meet edge to edge, and a smoothed shape costs several times as much as a plain
     * one. Only the lines on top of them are smooth.
     */
    private void wall(Graphics2D g, Proj p, Wall w, int side) {
        double hw = Road.HALF_WIDTH;
        double latN = p.cxN + side * w.offset() * hw, latF = p.cxF + side * w.offset() * hw;
        boolean near = p.zN < 1;
        double f = fog(p.seg.look, (p.zN + p.zF) / 2);
        Color fogC = w.kind() == Wall.Kind.TUNNEL ? TUNNEL_DARK : fogColour(p.seg.look);
        if (w.kind() == Wall.Kind.CLIFF) {
            cliff(g, p, w, side, fogC, f, near);
            return;
        }
        double hN = w.heightAt(p.seg.index), hF = w.heightAt(p.seg.index + 1);
        double xN = sx(p.sN, latN), xF = sx(p.sF, latF);
        // the light comes from the left: the right wall's face, turned to the left, is lit
        double sun = side > 0 ? 1.0 : 0.66;
        switch (w.kind()) {
            case CLIFF -> throw new AssertionError();
            case TUNNEL -> {
                double split = w.height() * 0.45;
                double gN = near ? viewHeight + 400 : sy(p.sN, p.yN), gF = sy(p.sF, p.yF);
                double mN = near ? viewHeight / 2 : sy(p.sN, p.yN + split), mF = sy(p.sF, p.yF + split);
                double tN = near ? -400 : sy(p.sN, p.yN + hN), tF = sy(p.sF, p.yF + hF);
                quad(rough, xN, gN, xF, gF, xF, mF, xN, mN, tint(scale(w.colours()[0], sun), fogC, f));
                quad(rough, xN, mN, xF, mF, xF, tF, xN, tN, tint(scale(w.colours()[1], sun), fogC, f));
                if (!near) {
                    double s1 = sy(p.sN, p.yN + 160), s2 = sy(p.sF, p.yF + 160);
                    quad(rough, xN, s1, xF, s2, xF, s2 - Math.max(0.5, p.sF * 60 * scale), xN, s1 - Math.max(0.5, p.sN * 60 * scale),
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

    // --8<-- [start:cliff]
    /** The layers a cliff's face is banded in. */
    private static final int LAYERS = 16;
    /** How high the scree at the foot of a cliff lies against it, in world units, and how far it reaches out. */
    private static final double SCREE = 480, SCREE_OUT = 0.55;

    /**
     * A cliff beside one segment: a piece of its second step, further out and higher, then a piece of its
     * face, then the scree at its foot. The face runs from the wall's distance at the near end of the
     * segment to its distance at the far end. Where the rock steps towards the road within one segment,
     * that piece stands almost across the view, the end of a buttress, and takes other light than the
     * face along the road: this is what makes the cliff a body.
     *
     * <p>Far away a segment is a pixel wide or less, and its pieces would cost more to begin than to fill:
     * there one piece stands for two or four segments, the second step earlier than the face.
     */
    private void cliff(Graphics2D g, Proj p, Wall w, int side, Color fogC, double f, boolean near) {
        long i = p.seg.index;
        int n = (int) (i - projections.getFirst().seg.index);
        double top = w.height() * 2.35;
        int back = own(pieces(i, n, 16, 48), n, w, side);
        if (back > 0) {
            Proj q = end(n, back, w, side);
            long j = q.seg.index + 1;
            // the second step is hidden behind the first up to the lowest head of the first: plain below that
            rock(p, q, w, side, w.backOffsetAt(i), w.backOffsetAt(j), w.backHeightAt(i), w.backHeightAt(j), top,
                    w.height() * 0.5 * Math.min(w.risen(i), w.risen(j)), 0.84, fogC, f, near);
        }
        int front = own(pieces(i, n, 100, 170), n, w, side);
        if (front == 0) return;
        Proj q = end(n, front, w, side);
        long j = q.seg.index + 1;
        double hw = Road.HALF_WIDTH;
        double oN = w.offsetAt(i), oF = w.offsetAt(j), hN = w.heightAt(i), hF = w.heightAt(j);
        rock(p, q, w, side, oN, oF, hN, hF, top, 0, 1, fogC, f, near);
        if (near || hN <= 0 && hF <= 0) return;
        double xN = sx(p.sN, p.cxN + side * oN * hw), xF = sx(q.sF, q.cxF + side * oF * hw);
        Color[] strata = w.colours();
        if (oF <= oN + 0.3) {
            // a slope of fallen stones against the foot; a piece that turns away has it behind its corner
            Color scree = RoadLook.mix(strata[0], p.seg.look.grass(), 0.45);
            quad(rough, sx(p.sN, p.cxN + side * (oN - SCREE_OUT) * hw), sy(p.sN, p.yN) + 0.6,
                    sx(q.sF, q.cxF + side * (oF - SCREE_OUT) * hw), sy(q.sF, q.yF), xF, sy(q.sF, q.yF + SCREE * w.risen(j)),
                    xN, sy(p.sN, p.yN + SCREE * w.risen(i)), tint(scale(scree, side > 0 ? 1.06 : 0.84), fogC, f));
        }
        if (oN - oF > 0.3 && front == 1) {
            // the corner where a niche ends against the next buttress lies in the dark
            g.setColor(tint(scale(strata[0], 0.32), fogC, f));
            g.setStroke(new BasicStroke((float) Math.max(0.5, p.sN * 34 * scale)));
            g.draw(new Line2D.Double(xN, sy(p.sN, p.yN), xN, sy(p.sN, p.yN + hN)));
        }
    }

    /**
     * How many segments the piece of rock that begins at segment {@code i} stands for, {@code n} segments
     * from the first one drawn: 1 near the camera, 2 from {@code two} segments on, 4 from {@code four} on,
     * and, if a piece that began {@code k} segments before {@code i} covers it, {@code -k}. A piece of two
     * begins at an even segment, a piece of four at every fourth, whatever the camera does: so the pieces
     * do not jump as it moves.
     */
    static int pieces(long i, int n, int two, int four) {
        int in4 = (int) Math.floorMod(i, 4), in2 = (int) Math.floorMod(i, 2);
        if (n - in4 >= four) return in4 == 0 ? 4 : -in4;
        if (n - in2 >= two) return in2 == 0 ? 2 : -in2;
        return 1;
    }

    /** A segment covered by an earlier piece draws nothing, unless the wall only begins after that piece began. */
    private int own(int pieces, int n, Wall w, int side) {
        if (pieces > 0) return pieces;
        Proj start = projections.get(n + pieces);
        return (side < 0 ? start.seg.left : start.seg.right) == w ? 0 : 1;
    }

    /** The last segment of a piece of {@code count} segments that begins with drawn segment {@code n}, with the same wall. */
    private Proj end(int n, int count, Wall w, int side) {
        Proj q = projections.get(n);
        for (int k = 1; k < count && n + k < projections.size(); k++) {
            Proj next = projections.get(n + k);
            if ((side < 0 ? next.seg.left : next.seg.right) != w) break;
            q = next;
        }
        return q;
    }

    /**
     * One piece of rock, from the near end of {@code p} to the far end of {@code q}: {@code oN} half-widths
     * from the centre and {@code hN} high there, {@code oF} and {@code hF} here, in the layers of the wall.
     * Below {@code plain} it is one colour.
     */
    private void rock(Proj p, Proj q, Wall w, int side, double oN, double oF, double hN, double hF, double top,
                      double plain, double shade, Color fogC, double f, boolean near) {
        if (hN <= 0 && hF <= 0) return;
        double hw = Road.HALF_WIDTH;
        long i = p.seg.index, j = q.seg.index + 1;
        double xN = sx(p.sN, p.cxN + side * oN * hw), xF = sx(q.sF, q.cxF + side * oF * hw);
        // The light comes from the left and a little from ahead: the right cliff's face is lit and the ends
        // of its buttresses, which look back at the driver, are in the shade; on the left it is the other
        // way round. A piece that turns away from the driver lies in the shadow of its own corner.
        double turned = Math.max(-1, Math.min(1, (oN - oF) / 0.7));
        double sun = side > 0 ? 1.0 - 0.3 * Math.max(0, turned) : 0.6 + 0.34 * Math.max(0, turned);
        if (turned < 0) sun *= 1 + 0.3 * turned;
        sun *= shade * (0.91 + 0.18 * Noise.hash(Math.floorDiv(i, 6), w.seed() + 5));
        Color[] strata = w.colours();
        double shiftN = w.faultAt(i), shiftF = w.faultAt(j);
        if (plain > 0) {
            quad(rough, xN, edge(p, near, true, 0), xF, sy(q.sF, q.yF), xF, sy(q.sF, q.yF + Math.min(plain, hF)),
                    xN, edge(p, near, true, Math.min(plain, hN)), tint(scale(strata[0], sun * 0.8), fogC, f));
        }
        // far away two layers together are not a pixel high: they are filled as one
        int stride = p.sN * scale * top < 3.0 * LAYERS ? 2 : 1;
        for (int b = 0; b < LAYERS; b += stride) {
            double a0 = b == 0 ? 0 : top * w.layer(b, LAYERS), a1 = top * w.layer(b + stride, LAYERS);
            double n0 = b == 0 ? plain : within(a0 + shiftN, plain, hN), f0 = b == 0 ? plain : within(a0 + shiftF, plain, hF);
            double n1 = b + stride >= LAYERS ? hN : within(a1 + shiftN, plain, hN);
            double f1 = b + stride >= LAYERS ? hF : within(a1 + shiftF, plain, hF);
            if (n0 >= n1 && f0 >= f1) continue;
            Color c = strata[(b + (int) (Noise.hash(b, w.seed()) * 2)) % strata.length];
            double up = 0.8 + 0.28 * b / LAYERS;
            quad(rough, xN, edge(p, near, false, n0), xF, sy(q.sF, q.yF + f0), xF, sy(q.sF, q.yF + f1),
                    xN, edge(p, near, false, n1), tint(scale(c, sun * up), fogC, f));
        }
    }

    private static double within(double v, double low, double high) {
        return Math.max(low, Math.min(high, v));
    }

    /** The height on the screen of a point of a wall at the near end of a segment; beside the camera it lies off the screen. */
    private double edge(Proj p, boolean near, boolean ground, double h) {
        if (!near) return sy(p.sN, p.yN + h);
        return ground || p.yN + h < camY ? viewHeight + 400 : -400;
    }
    // --8<-- [end:cliff]

    private void houses(Graphics2D g, Proj p, Wall w, int side, double xN, double xF, double hN, double hF, double sun,
                        Color fogC, double f, boolean near) {
        Color[] facades = w.colours();
        Color c = facades[(int) (Noise.hash(p.seg.index / 4, w.seed()) * facades.length)];
        double gN = near ? viewHeight + 400 : sy(p.sN, p.yN), gF = sy(p.sF, p.yF);
        double tN = near ? -400 : sy(p.sN, p.yN + hN), tF = sy(p.sF, p.yF + hF);
        quad(rough, xN, gN, xF, gF, xF, tF, xN, tN, tint(scale(c, sun), fogC, f * 0.9));
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
                quad(rough, ax, sy(sa, ya + y0), bx, sy(sb, yb + y0), bx, sy(sb, yb + y1), ax, sy(sa, ya + y1), wc);
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
        quad(rough, lN, tN, rN, tN, rF, tF, lF, tF, tint(new Color(70, 68, 62), TUNNEL_DARK, f));
        if (s.index % 2 == 0 && p.zN > 1) {
            double aN = sx(p.sN, p.cxN - 0.12 * hw), bN = sx(p.sN, p.cxN + 0.12 * hw);
            double aF = sx(p.sF, p.cxF - 0.12 * hw), bF = sx(p.sF, p.cxF + 0.12 * hw);
            double mid = (tN + tF) / 2;
            quad(rough, aN, tN, bN, tN, bF, mid, aF, mid, LAMP);
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
        double f = fog(p.seg.look, z);
        sprite.paint(g, x - pw / 2, y - ph, pw, ph, f, fogC, light, shade, flip);
        if (glowing != null) glowing.paint(g, x - pw / 2, y - ph, pw, ph, 0, fogC, 1, shade, flip);
    }
    // --8<-- [end:scenery]

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

    /** Pictures of glows by colour and size, the one used longest ago forgotten first. */
    private static final Map<Long, BufferedImage> GLOWS = new java.util.LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, BufferedImage> eldest) {
            return size() > 200;
        }
    };
    /** The largest picture of a glow; a larger glow is this picture enlarged. */
    private static final int LARGEST_GLOW = 384;

    // --8<-- [start:glow]
    /**
     * A soft light of radius {@code r}. A radial gradient is among the slowest things Java 2D can fill, and
     * a road with glowing lines has more than a hundred lights in a frame: so each is painted once as a
     * picture, in fixed sizes a tenth apart, and then only copied, in the size of the picture: a light may
     * be a tenth too large, which nobody sees on something without an edge.
     */
    static void glow(Graphics2D g, double x, double y, double r, Color c) {
        if (r < 0.5) return;
        AffineTransform t = g.getTransform();
        double device = 2 * r * Math.abs(t.getScaleX());
        int size = 3;
        while (size < device && size < LARGEST_GLOW) size = (int) Math.ceil(size * 1.1);
        long key = ((long) c.getRGB() << 32) | size;
        BufferedImage im;
        synchronized (GLOWS) {
            im = GLOWS.get(key);
            if (im == null) {
                im = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                Graphics2D p = im.createGraphics();
                float half = size / 2f;
                p.setPaint(new RadialGradientPaint(new Point2D.Float(half, half), half, new float[]{0f, 0.35f, 1f},
                        new Color[]{c, new Color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() / 3), new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)}));
                p.fillRect(0, 0, size, size);
                p.dispose();
                GLOWS.put(key, im);
            }
        }
        if (t.getShearX() != 0 || t.getShearY() != 0 || device > size) {
            g.drawImage(im, (int) Math.round(x - r), (int) Math.round(y - r), (int) Math.round(2 * r), (int) Math.round(2 * r), null);
            return;
        }
        g.setTransform(IDENTITY);
        g.drawImage(im, (int) Math.round(t.getTranslateX() + t.getScaleX() * x - size / 2.0),
                (int) Math.round(t.getTranslateY() + t.getScaleY() * y - size / 2.0), null);
        g.setTransform(t);
    }
    // --8<-- [end:glow]
}
