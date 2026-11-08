package eu.amadeos.gameengine.drive;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.HashMap;
import java.util.Map;

/**
 * A picture that stands on the road or beside it: a tree, a house, a car. It is drawn at every size, from a
 * few pixels far away to half the screen close by, and it fades into the fog with distance and darkens at
 * night. Scaling and tinting a large picture for every tree in every frame would be far too slow, so the
 * sprite keeps finished copies: scaled down to a few fixed widths (each about a fifth wider than the one
 * before) and tinted to a few steps of fog and light. A frame takes the copy just wider than it needs and
 * draws it with a small, cheap scale.
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
    /** More copies than this and the sprite forgets them all; they are made again as needed. */
    static final int MAX_COPIES = 96;

    private final BufferedImage art;
    private final double worldWidth;
    private final double glow;
    private final Map<Key, BufferedImage> copies = new HashMap<>();

    private record Key(int width, int fog, int light, int fogColour, int shade) {
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
     * {@code fogColour}, and lit by {@code light} (1 day, 0 black night) towards {@code shade}.
     */
    public BufferedImage copy(double pixels, double fog, Color fogColour, double light, Color shade) {
        int width = bucket(pixels, art.getWidth());
        double keep = 1 - glow;
        int f = (int) Math.round(Math.max(0, Math.min(1, fog)) * keep * (FOG_STEPS - 1));
        int l = (int) Math.round((1 - (1 - Math.max(0, Math.min(1, light))) * keep) * (LIGHT_STEPS - 1));
        Key key = new Key(width, f, l, fogColour.getRGB(), shade.getRGB());
        BufferedImage copy = copies.get(key);
        if (copy == null) {
            if (copies.size() >= MAX_COPIES) copies.clear();
            copy = make(width, f / (double) (FOG_STEPS - 1), fogColour, l / (double) (LIGHT_STEPS - 1), shade);
            copies.put(key, copy);
        }
        return copy;
    }
    // --8<-- [end:copy]

    /** The smallest of the fixed widths that is at least {@code pixels}; never wider than the picture. */
    static int bucket(double pixels, int full) {
        double w = 4;
        while (w < pixels && w < full) w *= WIDTH_STEP;
        return (int) Math.max(1, Math.min(full, Math.ceil(w)));
    }

    private BufferedImage make(int width, double fog, Color fogColour, double light, Color shade) {
        int height = Math.max(1, (int) Math.round(width * (double) art.getHeight() / art.getWidth()));
        BufferedImage scaled = scale(art, width, height);
        if (fog <= 0 && light >= 1) return scaled;
        int[] px = ((DataBufferInt) scaled.getRaster().getDataBuffer()).getData();
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
        return scaled;
    }

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
