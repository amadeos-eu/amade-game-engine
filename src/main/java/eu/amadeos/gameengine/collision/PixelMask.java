package eu.amadeos.gameengine.collision;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Which pixels of a thing are solid, one bit each: for collisions as exact as the picture. Boxes and
 * circles are quick and good enough for most games, but a cab that brushes a round neon sign, a rock with a
 * jagged edge or the slanted strut under a balcony would hit the empty corners of a box. Two masks overlap
 * when one of their solid pixels is in the same place, and that is what {@link #overlaps} finds out.
 *
 * <p>A row is kept as {@code long}s of 64 pixels each, the leftmost pixel in the lowest bit. Comparing two
 * rows then takes one AND for 64 pixels at once: a mask of the cab, 32 pixels wide, against the scenery
 * costs one or two ANDs per row, a few dozen for the whole cab. Masks are made once, when a level or a sprite
 * is loaded, and only read while playing.
 */
// --8<-- [start:mask]
public final class PixelMask {
    private final int width;
    private final int height;
    private final int words;           // longs per row
    private final long[] bits;

    /** An empty mask: no pixel is solid. */
    public PixelMask(int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("empty mask: " + width + "x" + height);
        this.width = width;
        this.height = height;
        this.words = (width + 63) / 64;
        this.bits = new long[words * height];
    }

    /** The pixels of an image that are mostly opaque, alpha 128 or more, are solid. */
    public static PixelMask of(BufferedImage image) {
        PixelMask mask = new PixelMask(image.getWidth(), image.getHeight());
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) >= 128) mask.set(x, y);
            }
        }
        return mask;
    }

    /**
     * A mask of what {@code painter} fills: shapes drawn in any colour on a transparent image, without
     * antialiasing, so each pixel is either in or out. The same shapes that are drawn can be the mask.
     */
    public static PixelMask painted(int width, int height, Consumer<Graphics2D> painter) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        painter.accept(g);
        g.dispose();
        return of(image);
    }
    // --8<-- [end:mask]

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** Whether a pixel is solid; outside the mask nothing is. */
    public boolean solid(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return false;
        return (bits[y * words + (x >> 6)] & (1L << (x & 63))) != 0;
    }

    /** Makes a pixel solid; outside the mask it does nothing. */
    public void set(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return;
        bits[y * words + (x >> 6)] |= 1L << (x & 63);
    }

    /** Makes a pixel empty again; outside the mask it does nothing. */
    public void clear(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return;
        bits[y * words + (x >> 6)] &= ~(1L << (x & 63));
    }

    /** Adds the solid pixels of {@code other}, its top left corner at {@code dx}, {@code dy} of this mask. */
    public void add(PixelMask other, int dx, int dy) {
        for (int y = 0; y < other.height; y++) {
            for (int x = 0; x < other.width; x++) {
                if (other.solid(x, y)) set(dx + x, dy + y);
            }
        }
    }

    /** How many pixels are solid. */
    public int count() {
        int count = 0;
        for (long word : bits) count += Long.bitCount(word);
        return count;
    }

    // --8<-- [start:overlaps]
    /**
     * Whether {@code other}, its top left corner at {@code dx}, {@code dy} of this mask, has a solid pixel
     * where this one has one. Row by row, each 64 pixels of the other mask are shifted to where they lie in
     * this one: they fall into one long of this row, or into two neighbouring ones.
     */
    public boolean overlaps(PixelMask other, int dx, int dy) {
        return firstOverlap(other, dx, dy) != null;
    }

    /** The first pixel, in this mask's coordinates, where both masks are solid, top row first; null if none. */
    public Point firstOverlap(PixelMask other, int dx, int dy) {
        int top = Math.max(0, dy);
        int bottom = Math.min(height, dy + other.height);
        if (top >= bottom || dx >= width || dx + other.width <= 0) return null;
        int q = Math.floorDiv(dx, 64);
        int r = Math.floorMod(dx, 64);
        for (int y = top; y < bottom; y++) {
            int mine = y * words;
            int theirs = (y - dy) * other.words;
            for (int j = 0; j < other.words; j++) {
                long word = other.bits[theirs + j];
                if (word == 0) continue;
                int k = q + j;                               // the long of this row the word starts in
                if (k >= 0 && k < words) {
                    long hit = bits[mine + k] & (word << r);
                    if (hit != 0) return new Point(k * 64 + Long.numberOfTrailingZeros(hit), y);
                }
                if (r != 0 && k + 1 >= 0 && k + 1 < words) {
                    long hit = bits[mine + k + 1] & (word >>> (64 - r));
                    if (hit != 0) return new Point((k + 1) * 64 + Long.numberOfTrailingZeros(hit), y);
                }
            }
        }
        return null;
    }
    // --8<-- [end:overlaps]

    /** Whether any pixel of the rectangle is solid: is there room for a box here? */
    public boolean anyIn(int x, int y, int w, int h) {
        for (int row = Math.max(0, y); row < Math.min(height, y + h); row++) {
            for (int col = Math.max(0, x); col < Math.min(width, x + w); col++) {
                if (solid(col, row)) return true;
            }
        }
        return false;
    }
}
