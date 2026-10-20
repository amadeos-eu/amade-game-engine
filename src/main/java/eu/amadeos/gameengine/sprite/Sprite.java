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
    /** A drawing that covers every pixel of its box, so its cache needs no transparency. */
    private final boolean opaque;
    /** The picture at its size on the screen; made by the first frame that needs it. */
    private BufferedImage cached;
    private boolean cachedSmooth;
    /** The pictures of {@link #drawSeamless}: a tile on a screen of 2.7 pixels per unit is 43 or 44 wide. */
    private final BufferedImage[] seamless = new BufferedImage[4];
    private final boolean[] seamlessSmooth = new boolean[4];
    private int nextSeamless;
    /** What the source's pixels need of transparency; 0 until the first picture is cached. */
    private int sourceTransparency;

    private Sprite(double width, double height, BufferedImage source, Drawing drawing, boolean hard, boolean opaque) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("empty sprite");
        this.width = width;
        this.height = height;
        this.source = source;
        this.drawing = drawing;
        this.hard = hard;
        this.opaque = opaque;
    }

    /** A sprite from an image, shown at {@code width} × {@code height} logical units, whatever its pixels. */
    public static Sprite of(BufferedImage image, double width, double height) {
        return new Sprite(width, height, image, null, false, false);
    }

    /** A sprite drawn in code. */
    public static Sprite drawn(double width, double height, Drawing drawing) {
        return new Sprite(width, height, null, drawing, false, false);
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
        return new Sprite(width, height, source, drawing, true, opaque);
    }

    // --8<-- [start:opaque]
    /**
     * The same drawing, promised to cover every pixel of its box, such as a whole map's ground: its cache
     * is then opaque and copied without blending, which a picture drawn in code otherwise never is. What
     * the drawing leaves out shows black.
     */
    public Sprite opaque() {
        if (drawing == null) throw new IllegalStateException("an image knows its own transparency");
        return new Sprite(width, height, null, drawing, hard, true);
    }
    // --8<-- [end:opaque]

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

    // --8<-- [start:seamless]
    /**
     * Draws the sprite so that it covers exactly the screen pixels from {@code x} to {@code x + width} and
     * from {@code y} to {@code y + height}, each edge rounded to the nearest pixel. Tiles drawn side by side
     * this way meet without a gap and without overlapping, whatever the scale.
     *
     * <p>{@link #draw} copies one cached picture of the rounded size, and that is not enough for tiles: at
     * 2.7 screen pixels per unit, a tile of 16 units is 43.2 pixels wide. Copied 43 pixels wide at rounded
     * positions, every fifth tile leaves a gap of one pixel, a line that flickers through the scrolling
     * ground. So here the width is worked out from both rounded edges, 43 or 44 pixels, and both pictures
     * are cached.
     */
    public void drawSeamless(Graphics2D g, double x, double y) {
        AffineTransform t = g.getTransform();
        int rotated = AffineTransform.TYPE_MASK_ROTATION | AffineTransform.TYPE_GENERAL_TRANSFORM;
        if (UNCACHED || (t.getType() & rotated) != 0) {
            drawScaled(g, x, y);
            return;
        }
        long left = Math.round(t.getTranslateX() + x * t.getScaleX());
        long right = Math.round(t.getTranslateX() + (x + width) * t.getScaleX());
        long top = Math.round(t.getTranslateY() + y * t.getScaleY());
        long bottom = Math.round(t.getTranslateY() + (y + height) * t.getScaleY());
        int w = (int) Math.max(1, right - left);
        int h = (int) Math.max(1, bottom - top);
        boolean smooth = !hard && g.getRenderingHint(RenderingHints.KEY_INTERPOLATION)
                != RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
        BufferedImage image = null;
        for (int i = 0; i < seamless.length; i++) {
            BufferedImage candidate = seamless[i];
            if (candidate != null && candidate.getWidth() == w && candidate.getHeight() == h
                    && seamlessSmooth[i] == smooth) {
                image = candidate;
                break;
            }
        }
        if (image == null) {
            image = render(g.getDeviceConfiguration(), w, h, smooth);
            seamless[nextSeamless] = image;
            seamlessSmooth[nextSeamless] = smooth;
            nextSeamless = (nextSeamless + 1) % seamless.length;
        }
        g.setTransform(AffineTransform.getTranslateInstance(left, top));
        g.drawImage(image, 0, 0, null);
        g.setTransform(t);
    }
    // --8<-- [end:seamless]

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
        int transparency = transparency(smooth);
        BufferedImage image = gc != null
                ? gc.createCompatibleImage(w, h, transparency)
                : new BufferedImage(w, h, transparency == Transparency.OPAQUE
                ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB_PRE);
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

    // --8<-- [start:transparency]
    /**
     * The least transparency the cached picture needs, because copying it costs accordingly: an opaque
     * picture is copied as it is, one whose pixels are either seen or not (a {@code BITMASK}) only skips
     * the unseen ones, and only a translucent one is blended with what lies below, pixel by pixel. A sky
     * the size of the screen costs a quarter as much when it is known to be opaque.
     *
     * <p>Pictures drawn in code have soft edges and stay translucent, unless they are {@link #opaque()}. An
     * image is looked at once: opaque if
     * every pixel is, a bitmask if every pixel is either opaque or invisible. Smooth scaling mixes the
     * edges of a bitmask with the invisible pixels around them, so then it is translucent too.
     */
    private int transparency(boolean smooth) {
        if (source == null) return opaque ? Transparency.OPAQUE : Transparency.TRANSLUCENT;
        if (sourceTransparency == 0) sourceTransparency = transparencyOf(source);
        if (sourceTransparency == Transparency.BITMASK && smooth) return Transparency.TRANSLUCENT;
        return sourceTransparency;
    }

    static int transparencyOf(BufferedImage image) {
        if (!image.getColorModel().hasAlpha()) return Transparency.OPAQUE;
        boolean seeThrough = false;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                if (alpha != 0 && alpha != 255) return Transparency.TRANSLUCENT;
                seeThrough |= alpha == 0;
            }
        }
        return seeThrough ? Transparency.BITMASK : Transparency.OPAQUE;
    }
    // --8<-- [end:transparency]

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

    /** For tests: how many pictures {@link #drawSeamless} keeps. */
    int seamlessCount() {
        int count = 0;
        for (BufferedImage image : seamless) if (image != null) count++;
        return count;
    }

    /** For tests: the size of the cached picture, or {@code null} before the first frame. */
    java.awt.Dimension cachedSize() {
        return cached == null ? null : new java.awt.Dimension(cached.getWidth(), cached.getHeight());
    }

    /** For tests: the transparency of the cached picture, a {@link Transparency} constant. */
    int cachedTransparency() {
        return cached.getTransparency();
    }
}
