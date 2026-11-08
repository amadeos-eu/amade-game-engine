package eu.amadeos.gameengine.pinball;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Arrays;

/**
 * Draws a table seen from above with heights: a flat picture in which every part stands up from the floor
 * and throws a soft shadow. {@link Perspective#warp} then tilts the picture into the player's view.
 *
 * <p>Three layers make the picture. The <b>floor</b> takes the printed artwork of the playfield. The
 * <b>shadows</b> collect the footprints of the parts, moved away from the light by their height, and are
 * blurred and laid over the floor. The <b>parts</b> take the parts themselves, each lifted by
 * {@link #extrude}: its outline drawn again and again, a quarter unit higher each time, darker at the foot
 * and lit from the left, with its top on the last slice. Height goes up the picture (−y), which is where the
 * tilted view shows it.
 *
 * <p>Coordinates are table units; {@code pixelsPerUnit} sets how sharp the picture is. Draw the moving
 * parts the same way, but per frame, onto the screen through {@link Perspective#local}.
 */
public final class Relief {
    /** How far a shadow falls per unit of height: the light comes from above the player's left shoulder. */
    public static final double SHADOW_X = 0.35;
    public static final double SHADOW_Y = 0.6;

    private final double left, top, right, bottom, scale;
    private final BufferedImage floor, shadows, parts;
    private final Graphics2D floorG, shadowG, partsG;
    private final Relief below;

    /** A picture of the table rectangle {@code left, top} to {@code right, bottom}, at {@code pixelsPerUnit}. */
    public Relief(double left, double top, double right, double bottom, double pixelsPerUnit) {
        this(left, top, right, bottom, pixelsPerUnit, null);
    }

    private Relief(double left, double top, double right, double bottom, double pixelsPerUnit, Relief below) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.scale = pixelsPerUnit;
        this.below = below;
        int w = (int) Math.ceil((right - left) * pixelsPerUnit), h = (int) Math.ceil((bottom - top) * pixelsPerUnit);
        floor = below == null ? image(w, h) : null;
        shadows = below == null ? image(w, h) : null;
        parts = image(w, h);
        floorG = floor == null ? null : graphics(floor);
        shadowG = shadows == null ? null : graphics(shadows);
        partsG = graphics(parts);
    }

    /**
     * A second layer of parts above this one, for everything high up: ramps, wire forms, plastics. Its
     * shadows fall onto this relief's floor; moving parts and balls on the playfield are drawn between the
     * two layers, balls on a ramp after the upper one.
     */
    public Relief above() {
        return new Relief(left, top, right, bottom, scale, this);
    }

    private static BufferedImage image(int w, int h) {
        return new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
    }

    private Graphics2D graphics(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.scale(scale, scale);
        g.translate(-left, -top);
        return g;
    }

    /** The floor, for the printed art; null on a relief made by {@link #above()}. */
    public Graphics2D floor() {
        return floorG;
    }

    /** The parts, in table units. */
    public Graphics2D parts() {
        return partsG;
    }

    public double left() {
        return left;
    }

    public double top() {
        return top;
    }

    public double right() {
        return right;
    }

    public double bottom() {
        return bottom;
    }

    // --8<-- [start:shadow]
    /** The shadow of a footprint {@code h} units high: the footprint, moved away from the light. */
    public void shadow(Shape footprint, double h) {
        if (below != null) {
            below.shadow(footprint, h);
            return;
        }
        AffineTransform at = shadowG.getTransform();
        shadowG.translate(h * SHADOW_X, h * SHADOW_Y);
        shadowG.setColor(new Color(0, 0, 0, 170));
        shadowG.fill(footprint);
        shadowG.setTransform(at);
    }
    // --8<-- [end:shadow]

    /** {@link #extrude(Graphics2D, Shape, double, double, Color, Paint)} onto the parts layer. */
    public void extrude(Shape footprint, double from, double to, Color side, Paint top) {
        extrude(partsG, footprint, from, to, side, top);
    }

    // --8<-- [start:extrude]
    /**
     * Lifts a footprint from height {@code from} to {@code to}: its side as slices a quarter unit apart, dark
     * at the foot, light at the top and lit from the left; then its {@code top} (null for none) on the last.
     */
    public static void extrude(Graphics2D g, Shape footprint, double from, double to, Color side, Paint top) {
        Rectangle2D b = footprint.getBounds2D();
        AffineTransform at = g.getTransform();
        float x0 = (float) b.getMinX() - 0.01f, x1 = (float) b.getMaxX() + 0.01f;
        for (double h = from; h < to; h += 0.25) {
            double f = 0.55 + 0.35 * (h - from) / Math.max(0.01, to - from);
            g.setTransform(at);
            g.translate(0, -h);
            g.setPaint(new LinearGradientPaint(x0, 0f, x1, 0f, new float[]{0f, 0.3f, 1f},
                    new Color[]{shade(side, f * 1.15), shade(side, f * 1.3), shade(side, f * 0.7)}));
            g.fill(footprint);
        }
        g.setTransform(at);
        if (top != null) {
            g.translate(0, -to);
            g.setPaint(top);
            g.fill(footprint);
            g.setTransform(at);
        }
    }
    // --8<-- [end:extrude]

    /** The shape moved up by {@code h}: where its top is drawn. */
    public static Shape lift(Shape s, double h) {
        return AffineTransform.getTranslateInstance(0, -h).createTransformedShape(s);
    }

    /** A colour made lighter (factor above 1) or darker, alpha kept. */
    public static Color shade(Color c, double factor) {
        return new Color((int) Math.min(255, c.getRed() * factor), (int) Math.min(255, c.getGreen() * factor),
                (int) Math.min(255, c.getBlue() * factor), c.getAlpha());
    }

    // --8<-- [start:bake]
    /** The finished picture: floor, the blurred shadows over it, the parts on top. */
    public BufferedImage bake() {
        if (below != null) return parts;
        float[] k = new float[25];
        Arrays.fill(k, 1f / 25);
        ConvolveOp blur = new ConvolveOp(new Kernel(5, 5, k), ConvolveOp.EDGE_NO_OP, null);
        int passes = Math.max(1, (int) Math.round(scale / 2));
        BufferedImage soft = shadows;
        for (int i = 0; i < passes; i++) soft = blur.filter(soft, null);
        BufferedImage out = image(floor.getWidth(), floor.getHeight());
        Graphics2D g = out.createGraphics();
        g.drawImage(floor, 0, 0, null);
        g.setComposite(AlphaComposite.SrcAtop.derive(0.75f));
        g.drawImage(soft, 0, 0, null);
        g.setComposite(AlphaComposite.SrcOver);
        g.drawImage(parts, 0, 0, null);
        g.dispose();
        return out;
    }
    // --8<-- [end:bake]

    public void dispose() {
        if (floorG != null) floorG.dispose();
        if (shadowG != null) shadowG.dispose();
        partsG.dispose();
    }
}
