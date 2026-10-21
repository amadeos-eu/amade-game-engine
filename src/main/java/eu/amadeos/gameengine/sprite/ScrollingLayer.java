package eu.amadeos.gameengine.sprite;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * A layer of the world that rarely changes, such as the ground of a big map, kept as one picture of the
 * view in screen pixels. While nothing changes and the view stands still, a frame copies that picture;
 * when the view moves, the picture is shifted by whole pixels and only the strips that came into view are
 * painted; when the world changes, the game passes another {@code key} and the whole view is painted again.
 *
 * <pre>
 * long key = groundVersion();            // anything that changes when the layer must be painted anew
 * ground.draw(g, camera.screenLeft(), camera.screenTop(), camera.x(alpha), camera.y(alpha), key,
 *         (lg, left, top, width, height) -> paintGround(lg, left, top, width, height));
 * </pre>
 *
 * <p>The painter must cover every pixel of the region it is given (fill a background first): the picture
 * is opaque, so copying it costs no blending. It paints in world coordinates, clipped to the region; what
 * it draws outside is cut off. The view's position is rounded to whole screen pixels, so the rest of the
 * world should be drawn at {@link #snap} positions to stay in step with the layer. Use a layer on one
 * thread only, the game loop's.
 */
// --8<-- [start:layer]
public final class ScrollingLayer {
    /** Paints the world from {@code left}, {@code top}, {@code width} × {@code height} world units. */
    public interface Painter {
        void paint(Graphics2D g, double left, double top, double width, double height);
    }

    private final double viewWidth;
    private final double viewHeight;
    private BufferedImage image;
    private double scaleX;
    private double scaleY;
    private long key;
    private long pixelLeft;    // the world position of the picture's first column, in screen pixels
    private long pixelTop;
    private boolean valid;
    private int painted;
    private RenderingHints hints;

    /** A layer for a view of {@code viewWidth} × {@code viewHeight} world units. */
    public ScrollingLayer(double viewWidth, double viewHeight) {
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
    }

    /** Forgets the picture: the next frame paints the whole view. */
    public void invalidate() {
        valid = false;
    }

    /**
     * Draws the view whose top left world point is {@code left}, {@code top} at {@code screenLeft},
     * {@code screenTop} of the graphics, painting first what is missing.
     */
    public void draw(Graphics2D g, double screenLeft, double screenTop, double left, double top, long key,
                     Painter painter) {
        AffineTransform t = g.getTransform();
        int rotated = AffineTransform.TYPE_MASK_ROTATION | AffineTransform.TYPE_GENERAL_TRANSFORM;
        if ((t.getType() & rotated) != 0) {
            // Rotated: no picture of screen pixels fits, paint directly.
            Graphics2D direct = (Graphics2D) g.create();
            direct.translate(screenLeft - left, screenTop - top);
            direct.clip(new Rectangle2D.Double(left, top, viewWidth, viewHeight));
            painter.paint(direct, left, top, viewWidth, viewHeight);
            direct.dispose();
            return;
        }
        double sx = t.getScaleX();
        double sy = t.getScaleY();
        hints = g.getRenderingHints();   // the painter draws as the frame does: smooth or in pixel mode
        int w = (int) Math.ceil(viewWidth * sx) + 1;
        int h = (int) Math.ceil(viewHeight * sy) + 1;
        long pl = Math.round(left * sx);
        long pt = Math.round(top * sy);
        if (image == null || image.getWidth() != w || image.getHeight() != h || sx != scaleX || sy != scaleY) {
            image = create(g.getDeviceConfiguration(), w, h);
            scaleX = sx;
            scaleY = sy;
            valid = false;
        }
        long dx = pl - pixelLeft;
        long dy = pt - pixelTop;
        if (!valid || key != this.key || Math.abs(dx) >= w || Math.abs(dy) >= h) {
            pixelLeft = pl;
            pixelTop = pt;
            this.key = key;
            paint(painter, 0, 0, w, h);
            valid = true;
        } else if (dx != 0 || dy != 0) {
            Graphics2D ig = image.createGraphics();
            ig.copyArea(0, 0, w, h, (int) -dx, (int) -dy);
            ig.dispose();
            pixelLeft = pl;
            pixelTop = pt;
            if (dx > 0) paint(painter, w - (int) dx, 0, (int) dx, h);
            if (dx < 0) paint(painter, 0, 0, (int) -dx, h);
            if (dy > 0) paint(painter, 0, h - (int) dy, w, (int) dy);
            if (dy < 0) paint(painter, 0, 0, w, (int) -dy);
        }
        // The picture's first column is world pixel pixelLeft; where that is on the screen, rounded.
        long x = Math.round(t.getTranslateX() + (screenLeft - left) * sx + pixelLeft);
        long y = Math.round(t.getTranslateY() + (screenTop - top) * sy + pixelTop);
        g.setTransform(AffineTransform.getTranslateInstance(x, y));
        g.drawImage(image, 0, 0, null);
        g.setTransform(t);
    }

    /** A world position rounded to the screen pixel the layer puts it on, at the given scale. */
    public static double snap(double world, double scale) {
        return Math.round(world * scale) / scale;
    }

    /** How many regions were painted so far: for tests and for measuring what the layer saves. */
    public int painted() {
        return painted;
    }

    private void paint(Painter painter, int x, int y, int w, int h) {
        Graphics2D ig = image.createGraphics();
        ig.setRenderingHints(hints);
        ig.clipRect(x, y, w, h);
        ig.translate(-pixelLeft, -pixelTop);
        ig.scale(scaleX, scaleY);
        painter.paint(ig, (pixelLeft + x) / scaleX, (pixelTop + y) / scaleY, w / scaleX, h / scaleY);
        ig.dispose();
        painted++;
    }

    private static BufferedImage create(GraphicsConfiguration gc, int w, int h) {
        return gc != null ? gc.createCompatibleImage(w, h, Transparency.OPAQUE)
                : new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    }
}
// --8<-- [end:layer]
