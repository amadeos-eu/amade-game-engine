package eu.amadeos.gameengine.sprite;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * A picture with a size in the game's logical coordinates, e.g. a brick of 40 × 16. Its source is an image
 * file, usually drawn larger than the logical size so it stays sharp on large screens, or a drawing in
 * code, a vector picture.
 *
 * <p>Scaling an image is expensive, and doing it for every sprite in every frame is the classic way to make
 * a Java2D game slow. So a sprite scales itself <i>once</i> to the size it has on the screen, into a
 * <i>compatible</i> image (the pixel format of the screen, which Java2D can keep in video memory), and from
 * then on only copies that image. When the window changes size, the next frame scales once more.
 *
 * <p>The graphics' interpolation hint decides how the image is scaled:
 * {@link RenderingHints#VALUE_INTERPOLATION_NEAREST_NEIGHBOR} keeps hard pixels (pixel mode), anything else
 * scales smoothly, unless the sprite has {@link #hardEdges()}. Use a sprite on one thread only, the game loop's.
 */
public final class Sprite {
    /**
     * {@code -Damade.sprites.uncached=true} scales every sprite in every frame, as if there were no cache:
     * only to measure what the cache is worth.
     */
    private static final boolean UNCACHED = Boolean.getBoolean("amade.sprites.uncached");

    /** Draws a picture in code, filling {@code width} × {@code height} in logical coordinates. */
    @FunctionalInterface
    public interface Drawing {
        void draw(Graphics2D g, double width, double height);
    }

    private final double width;
    private final double height;
    private final BufferedImage source;
    private final Drawing drawing;
    /** Always scaled with hard edges, whatever the render mode: pixel art. */
    private final boolean hard;
    /** The picture at its size on the screen; made by the first frame that needs it. */
    private BufferedImage cached;
    private boolean cachedSmooth;

    private Sprite(double width, double height, BufferedImage source, Drawing drawing, boolean hard) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("empty sprite");
        this.width = width;
        this.height = height;
        this.source = source;
        this.drawing = drawing;
        this.hard = hard;
    }

    /** A sprite from an image, shown at {@code width} × {@code height} logical units, whatever its pixels. */
    public static Sprite of(BufferedImage image, double width, double height) {
        return new Sprite(width, height, image, null, false);
    }

    /** A sprite drawn in code. */
    public static Sprite drawn(double width, double height, Drawing drawing) {
        return new Sprite(width, height, null, drawing, false);
    }

    /** A sprite from an image file among the classes of {@code owner}, e.g. {@code "/sprites/ball.png"}. */
    public static Sprite load(Class<?> owner, String resource, double width, double height) {
        return of(readImage(owner, resource), width, height);
    }

    public static BufferedImage readImage(Class<?> owner, String resource) {
        try (InputStream in = owner.getResourceAsStream(resource)) {
            if (in == null) throw new IllegalArgumentException("no image " + resource);
            BufferedImage image = ImageIO.read(in);
            if (image == null) throw new IllegalArgumentException("not an image: " + resource);
            return image;
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + resource, e);
        }
    }

    /**
     * The same picture, always scaled with hard edges, as pixel mode scales: for pixel art, whose pixels
     * must stay square blocks instead of blurring into each other.
     */
    public Sprite hardEdges() {
        return new Sprite(width, height, source, drawing, true);
    }

    public boolean hasHardEdges() {
        return hard;
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    // --8<-- [start:draw]
    /** Draws the sprite with its top left corner at {@code x}, {@code y} in logical coordinates. */
    public void draw(Graphics2D g, double x, double y) {
        AffineTransform t = g.getTransform();
        int rotated = AffineTransform.TYPE_MASK_ROTATION | AffineTransform.TYPE_GENERAL_TRANSFORM;
        if (UNCACHED || (t.getType() & rotated) != 0) {
            // Rotated: no cache can help, scale on the fly.
            drawScaled(g, x, y);
            return;
        }
        int w = Math.max(1, (int) Math.round(width * t.getScaleX()));
        int h = Math.max(1, (int) Math.round(height * t.getScaleY()));
        boolean smooth = !hard && g.getRenderingHint(RenderingHints.KEY_INTERPOLATION)
                != RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
        if (cached == null || cached.getWidth() != w || cached.getHeight() != h || cachedSmooth != smooth) {
            cached = render(g.getDeviceConfiguration(), w, h, smooth);
            cachedSmooth = smooth;
        }
        // Copy the cached picture 1:1 to whole screen pixels: no scaling, no blurring by half pixels.
        double screenX = Math.round(t.getTranslateX() + x * t.getScaleX());
        double screenY = Math.round(t.getTranslateY() + y * t.getScaleY());
        g.setTransform(AffineTransform.getTranslateInstance(screenX, screenY));
        g.drawImage(cached, 0, 0, null);
        g.setTransform(t);
    }
    // --8<-- [end:draw]

    private void drawScaled(Graphics2D g, double x, double y) {
        Graphics2D copy = (Graphics2D) g.create();
        try {
            copy.translate(x, y);
            if (drawing != null) {
                drawing.draw(copy, width, height);
            } else {
                copy.scale(width / source.getWidth(), height / source.getHeight());
                copy.drawImage(source, 0, 0, null);
            }
        } finally {
            copy.dispose();
        }
    }

    // --8<-- [start:render]
    /** The picture scaled once to {@code w} × {@code h} screen pixels, in the screen's own pixel format. */
    private BufferedImage render(GraphicsConfiguration gc, int w, int h, boolean smooth) {
        BufferedImage image = gc != null
                ? gc.createCompatibleImage(w, h, Transparency.TRANSLUCENT)
                : new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = image.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            if (drawing != null) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, smooth
                        ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
                g.scale(w / width, h / height);
                g.setComposite(AlphaComposite.SrcOver);
                drawing.draw(g, width, height);
            } else if (smooth) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(halvedTowards(source, w, h), 0, 0, w, h, null);
            } else {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(source, 0, 0, w, h, null);
            }
        } finally {
            g.dispose();
        }
        return image;
    }
    // --8<-- [end:render]

    /**
     * Bicubic scaling looks only at the nearest source pixels; shrinking to less than half in one go skips
     * pixels and looks rough. So shrink in halves first, until one more step is less than half.
     */
    private static BufferedImage halvedTowards(BufferedImage image, int w, int h) {
        BufferedImage current = image;
        while (current.getWidth() / 2 >= w && current.getHeight() / 2 >= h) {
            int nw = current.getWidth() / 2;
            int nh = current.getHeight() / 2;
            BufferedImage half = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = half.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(current, 0, 0, nw, nh, null);
            g.dispose();
            current = half;
        }
        return current;
    }

    /** For tests: the size of the cached picture, or {@code null} before the first frame. */
    java.awt.Dimension cachedSize() {
        return cached == null ? null : new java.awt.Dimension(cached.getWidth(), cached.getHeight());
    }
}
