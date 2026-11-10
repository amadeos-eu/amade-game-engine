package eu.amadeos.gameengine.drive;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.WritableRaster;
import java.util.LinkedHashMap;

/**
 * A picture that stands on the road or beside it: a tree, a house, a car. It is drawn at every size, from a
 * few pixels far away to half the screen close by, and it fades into the fog with distance and darkens at
 * night. Scaling and tinting a large picture for every tree in every frame would be far too slow, so the
 * sprite keeps finished copies: scaled down to a few fixed widths (each about a fifth wider than the one
 * before) and tinted to a few steps of fog and light. A frame takes the copy just wider than it needs and
 * draws it with a small, cheap scale. A tree seen at thirty distances needs about thirty copies. All sprites
 * share one budget of pixels for their copies; when it is spent, the copy used longest ago goes, whichever
 * sprite it belongs to.
 *
 * <p>Java 2D copies a picture whose pixels are either there or not to a new size about three times as fast
 * as one whose every pixel must be blended with what lies behind it, and five times as fast when the picture
 * is small. So the sprite takes its picture apart: the <i>solid</i> part (the tree, the house; whatever is
 * opaque, with its outline) gets copies with one bit of transparency, and the <i>soft</i> part (a shadow on
 * the ground, a glow) gets copies of its own, only as large as it is, with full transparency. A picture
 * that is soft nearly all over (a lamp in its light, a cloud) stays in one piece. A picture drawn mirrored
 * gets mirrored copies: mirroring while drawing is slow as well.
 *
 * <p>The picture's bottom centre stands on the ground; {@link #worldWidth()} is its width in world units (a
 * pine about 900, a car about 560, with the road 2200 wide). A <i>glowing</i> sprite (a lamp, a lit window,
 * a luminous mushroom) keeps its colours in fog and at night, by the part given.
 */
// --8<-- [start:sprite]
public final class RoadSprite {
    /** Steps of fog and of light the copies are tinted to. */
    static final int FOG_STEPS = 16;
    static final int LIGHT_STEPS = 8;
    /** Each width is this much wider than the one before. */
    static final double WIDTH_STEP = 1.19;
    /** The pixels all copies of all sprites may hold together, about 96 MB. */
    static final long BUDGET = 24_000_000;
    /** A pixel of a solid sprite's copy is there from this much alpha on. */
    static final int THERE = 100;
    /** Pixels with one bit of transparency: Java 2D has fast loops for scaling these onto a picture. */
    private static final DirectColorModel MASKED = new DirectColorModel(25, 0xff0000, 0xff00, 0xff, 0x1000000);

    private final BufferedImage art;
    private final double worldWidth;
    private final double glow;
    /** The opaque part of the picture, for copies with one bit of transparency; {@code null} if it stays in one piece. */
    private final BufferedImage solidArt;
    /** The soft part, cut to its bounds, or {@code null} if there is none; and where it lies, as parts of the picture. */
    private final BufferedImage softArt;
    private final double softX, softY, softWidth, softHeight;

    /** The copies of all sprites, the one used longest ago first. */
    private static final LinkedHashMap<Key, BufferedImage> COPIES = new LinkedHashMap<>(256, 0.75f, true);
    private static long pixels;

    /** A sprite is its own identity: the key holds the very sprite, not one that looks alike. */
    private record Key(RoadSprite sprite, boolean soft, int width, int fog, int light, int fogColour, int shade, boolean mirrored) {
    }
    // --8<-- [end:sprite]

    /**
     * @param art        the picture, larger than it will usually be drawn; transparent around the thing
     * @param worldWidth its width in world units
     * @param glow       0 for an ordinary thing, up to 1 for one that keeps its colours in fog and dark
     */
    public RoadSprite(BufferedImage art, double worldWidth, double glow) {
        this.art = art;
        this.worldWidth = worldWidth;
        this.glow = Math.max(0, Math.min(1, glow));
        Parts parts = Parts.of(art);
        this.solidArt = parts.solid();
        this.softArt = parts.soft();
        this.softX = parts.x() / (double) art.getWidth();
        this.softY = parts.y() / (double) art.getHeight();
        this.softWidth = parts.soft() == null ? 0 : parts.soft().getWidth() / (double) art.getWidth();
        this.softHeight = parts.soft() == null ? 0 : parts.soft().getHeight() / (double) art.getHeight();
    }

    // --8<-- [start:parts]
    /** A picture taken apart: its opaque part, its soft part cut to its bounds, and where that lies. */
    record Parts(BufferedImage solid, BufferedImage soft, int x, int y) {
        /** A pixel is opaque from this much alpha on, counts as visible from this much, and can be seen at all from this much. */
        static final int OPAQUE = 224, VISIBLE = 32, FAINT = 8;

        static Parts of(BufferedImage art) {
            int w = art.getWidth(), h = art.getHeight();
            int[] px = art.getRGB(0, 0, w, h, null, 0, w);
            // Soft pixels next to opaque ones are the smoothed outline and belong to the solid part; soft
            // pixels on their own, farther than a few pixels from anything opaque, are a shadow or a glow,
            // if there are enough of them in one patch: the tip of a twig is not.
            int reach = Math.max(2, w / 100);
            boolean[] opaque = new boolean[px.length];
            for (int i = 0; i < px.length; i++) opaque[i] = (px[i] >>> 24) >= OPAQUE;
            boolean[] soft = near(opaque, w, h, reach);
            long visible = 0;
            for (int i = 0; i < px.length; i++) {
                int a = px[i] >>> 24;
                if (a >= VISIBLE) visible++;
                soft[i] = a >= FAINT && a < OPAQUE && !soft[i];
            }
            long softPixels = patches(soft, w, h, Math.max(40, w * h / 400));
            // Where a shadow meets the trunk, the pixels between them are soft too: they go with the shadow.
            boolean[] beside = near(soft, w, h, reach + 1);
            for (int i = 0; i < px.length; i++) {
                int a = px[i] >>> 24;
                if (!soft[i] && beside[i] && a > 0 && a < OPAQUE) soft[i] = true;
            }
            int x0 = w, y0 = h, x1 = -1, y1 = -1;
            for (int i = 0; i < px.length; i++) {
                if (!soft[i]) continue;
                int x = i % w, y = i / w;
                x0 = Math.min(x0, x);
                x1 = Math.max(x1, x);
                y0 = Math.min(y0, y);
                y1 = Math.max(y1, y);
            }
            // Hardly anything soft: all solid.
            if (softPixels * 100 < visible) return new Parts(art, null, 0, 0);
            long box = (long) (x1 - x0 + 1) * (y1 - y0 + 1);
            // Soft nearly all over: taking it apart would save nothing.
            if (box * 5 > (long) w * h * 3 || softPixels * 2 > visible) return new Parts(null, null, 0, 0);
            BufferedImage solidPart = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            BufferedImage softPart = new BufferedImage(x1 - x0 + 1, y1 - y0 + 1, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int p = px[y * w + x];
                    if (soft[y * w + x]) softPart.setRGB(x - x0, y - y0, p);
                    else solidPart.setRGB(x, y, p);
                }
            }
            return new Parts(solidPart, softPart, x0, y0);
        }

        /** Keeps the patches of connected pixels that have at least {@code fewest} of them; returns how many pixels stay. */
        private static long patches(boolean[] soft, int w, int h, int fewest) {
            boolean[] seen = new boolean[soft.length];
            int[] patch = new int[soft.length];
            long kept = 0;
            for (int start = 0; start < soft.length; start++) {
                if (!soft[start] || seen[start]) continue;
                int size = 0;
                patch[size++] = start;
                seen[start] = true;
                for (int next = 0; next < size; next++) {
                    int i = patch[next], x = i % w, y = i / w;
                    if (x > 0 && soft[i - 1] && !seen[i - 1]) seen[patch[size++] = i - 1] = true;
                    if (x < w - 1 && soft[i + 1] && !seen[i + 1]) seen[patch[size++] = i + 1] = true;
                    if (y > 0 && soft[i - w] && !seen[i - w]) seen[patch[size++] = i - w] = true;
                    if (y < h - 1 && soft[i + w] && !seen[i + w]) seen[patch[size++] = i + w] = true;
                }
                if (size >= fewest) kept += size;
                else for (int k = 0; k < size; k++) soft[patch[k]] = false;
            }
            return kept;
        }

        /** For each pixel, whether a marked one lies within {@code reach} pixels of it, along the rows and columns. */
        private static boolean[] near(boolean[] marked, int w, int h, int reach) {
            int[] rows = new int[w * h];
            for (int y = 0; y < h; y++) {
                int last = -1_000_000;
                for (int x = 0; x < w; x++) {
                    if (marked[y * w + x]) last = x;
                    rows[y * w + x] = x - last;
                }
                last = 1_000_000;
                for (int x = w - 1; x >= 0; x--) {
                    if (marked[y * w + x]) last = x;
                    rows[y * w + x] = Math.min(rows[y * w + x], last - x);
                }
            }
            boolean[] near = new boolean[w * h];
            for (int x = 0; x < w; x++) {
                int last = -1_000_000;
                for (int y = 0; y < h; y++) {
                    if (rows[y * w + x] <= reach) last = y;
                    near[y * w + x] = y - last <= reach;
                }
                last = 1_000_000;
                for (int y = h - 1; y >= 0; y--) {
                    if (rows[y * w + x] <= reach) last = y;
                    near[y * w + x] |= last - y <= reach;
                }
            }
            return near;
        }
    }
    // --8<-- [end:parts]

    /** Whether the picture's copies have one bit of transparency (with a soft part of its own, if it has one). */
    public boolean solid() {
        return solidArt != null;
    }

    /** Whether a soft part (a shadow, a glow) is drawn under the solid one. */
    public boolean hasSoftPart() {
        return softArt != null;
    }

    public RoadSprite(BufferedImage art, double worldWidth) {
        this(art, worldWidth, 0);
    }

    public BufferedImage art() {
        return art;
    }

    public double worldWidth() {
        return worldWidth;
    }

    /** Height in world units, from the picture's proportions. */
    public double worldHeight() {
        return worldWidth * art.getHeight() / art.getWidth();
    }

    public double glow() {
        return glow;
    }

    // --8<-- [start:copy]
    /**
     * The copy to draw {@code pixels} wide on the screen, faded by {@code fog} (0 clear, 1 all fog) into
     * {@code fogColour}, and lit by {@code light} (1 day, 0 black night) towards {@code shade}. Of a picture
     * taken apart, this is the solid part; {@link #paint} draws both.
     */
    public BufferedImage copy(double pixels, double fog, Color fogColour, double light, Color shade) {
        return copy(false, pixels, fog, fogColour, light, shade, false);
    }

    private BufferedImage copy(boolean soft, double pixels, double fog, Color fogColour, double light, Color shade, boolean mirrored) {
        int width = bucket(pixels, art.getWidth());
        double keep = 1 - glow;
        int f = (int) Math.round(Math.max(0, Math.min(1, fog)) * keep * (FOG_STEPS - 1));
        int l = (int) Math.round((1 - (1 - Math.max(0, Math.min(1, light))) * keep) * (LIGHT_STEPS - 1));
        Key key = new Key(this, soft, width, f, l, fogColour.getRGB(), shade.getRGB(), mirrored);
        BufferedImage copy;
        synchronized (COPIES) {
            copy = COPIES.get(key);
        }
        if (copy == null) {
            BufferedImage from = soft ? softArt : solidArt != null ? solidArt : art;
            // The soft part is as much smaller than its picture as the whole copy is.
            int w = soft ? Math.max(1, (int) Math.round(width * softWidth)) : width;
            copy = make(from, w, f / (double) (FOG_STEPS - 1), fogColour, l / (double) (LIGHT_STEPS - 1), shade, mirrored,
                    !soft && solidArt != null);
            remember(key, copy);
        }
        return copy;
    }
    // --8<-- [end:copy]

    // --8<-- [start:paint]
    /**
     * Draws the sprite {@code width} × {@code height} logical units large with its top left corner at
     * {@code left}, {@code top}: the soft part first, if it has one, then the solid part over it.
     */
    public void paint(Graphics2D g, double left, double top, double width, double height, double fog, Color fogColour,
                      double light, Color shade, boolean mirrored) {
        double pixels = width * Math.abs(g.getTransform().getScaleX());
        if (softArt != null) {
            double x = left + (mirrored ? 1 - softX - softWidth : softX) * width, y = top + softY * height;
            int w = (int) Math.round(softWidth * width), h = (int) Math.round(softHeight * height);
            if (w > 0 && h > 0) {
                g.drawImage(copy(true, pixels, fog, fogColour, light, shade, mirrored), (int) Math.round(x), (int) Math.round(y), w, h, null);
            }
        }
        g.drawImage(copy(false, pixels, fog, fogColour, light, shade, mirrored), (int) Math.round(left), (int) Math.round(top),
                (int) Math.round(width), (int) Math.round(height), null);
    }
    // --8<-- [end:paint]

    private static void remember(Key key, BufferedImage copy) {
        synchronized (COPIES) {
            BufferedImage before = COPIES.put(key, copy);
            if (before != null) pixels -= size(before);
            pixels += size(copy);
            var oldest = COPIES.values().iterator();
            while (pixels > BUDGET && oldest.hasNext()) {
                BufferedImage gone = oldest.next();
                if (gone == copy) break;
                pixels -= size(gone);
                oldest.remove();
            }
        }
    }

    private static long size(BufferedImage im) {
        return (long) im.getWidth() * im.getHeight();
    }

    /** The smallest of the fixed widths that is at least {@code pixels}; never wider than the picture. */
    static int bucket(double pixels, int full) {
        double w = 4;
        while (w < pixels && w < full) w *= WIDTH_STEP;
        return (int) Math.max(1, Math.min(full, Math.ceil(w)));
    }

    private static BufferedImage make(BufferedImage art, int width, double fog, Color fogColour, double light, Color shade,
                                      boolean mirrored, boolean oneBit) {
        int height = Math.max(1, (int) Math.round(width * (double) art.getHeight() / art.getWidth()));
        BufferedImage scaled = scale(art, width, height);
        int[] px = ((DataBufferInt) scaled.getRaster().getDataBuffer()).getData();
        if (fog > 0 || light < 1) {
            int fr = fogColour.getRed(), fg = fogColour.getGreen(), fb = fogColour.getBlue();
            int sr = shade.getRed(), sg = shade.getGreen(), sb = shade.getBlue();
            for (int i = 0; i < px.length; i++) {
                int p = px[i], a = p >>> 24;
                if (a == 0) continue;
                double r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
                r = sr + (r - sr) * light;
                g = sg + (g - sg) * light;
                b = sb + (b - sb) * light;
                r += (fr - r) * fog;
                g += (fg - g) * fog;
                b += (fb - b) * fog;
                px[i] = (a << 24) | ((int) r << 16) | ((int) g << 8) | (int) b;
            }
        }
        if (mirrored) {
            int w = scaled.getWidth();
            for (int y = 0; y < scaled.getHeight(); y++) {
                for (int a = y * w, b = a + w - 1; a < b; a++, b--) {
                    int p = px[a];
                    px[a] = px[b];
                    px[b] = p;
                }
            }
        }
        return oneBit ? masked(scaled, px) : scaled;
    }

    // --8<-- [start:masked]
    /** The picture with one bit of transparency: each pixel all there or not at all. */
    private static BufferedImage masked(BufferedImage im, int[] px) {
        WritableRaster raster = MASKED.createCompatibleWritableRaster(im.getWidth(), im.getHeight());
        int[] out = ((DataBufferInt) raster.getDataBuffer()).getData();
        for (int i = 0; i < px.length; i++) out[i] = (px[i] >>> 24) >= THERE ? 0x1000000 | (px[i] & 0xffffff) : 0;
        return new BufferedImage(MASKED, raster, false, null);
    }
    // --8<-- [end:masked]

    /** Halves the picture step by step, then one last smooth step: no shimmering leaves on small trees. */
    static BufferedImage scale(BufferedImage src, int width, int height) {
        BufferedImage cur = src;
        int w = src.getWidth(), h = src.getHeight();
        while (w / 2 >= width && h / 2 >= height) {
            w /= 2;
            h /= 2;
            cur = draw(cur, w, h);
        }
        return draw(cur, width, height);
    }

    private static BufferedImage draw(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, out.getWidth(), out.getHeight(), null);
        g.dispose();
        return out;
    }
}
