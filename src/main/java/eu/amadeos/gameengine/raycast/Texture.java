package eu.amadeos.gameengine.raycast;

import java.awt.image.BufferedImage;

/**
 * A square picture for the {@link Raycaster}: a wall, a floor, a ceiling or a {@link Billboard}. The side is
 * a power of two (32, 64, 128 …), so a texture coordinate wraps with a bit mask instead of a division.
 *
 * <p>Each texel has a colour and a <i>glow</i> between 0 and 1. A glowing texel ignores the light of the
 * room and shines with its own colour: the screen of a console, a lamp in the ceiling, the eye of a machine.
 * Texels whose alpha is below half are holes (only billboards have them).
 */
// --8<-- [start:texture]
public final class Texture {
    final int size;
    final int mask;
    final int shift;
    /** ARGB, row by row. */
    final int[] argb;
    /** 0 = lit by the room, 255 = shines by itself. */
    final byte[] glow;

    private Texture(int size, int[] argb, byte[] glow) {
        if (size < 1 || Integer.bitCount(size) != 1) throw new IllegalArgumentException("size must be a power of two: " + size);
        if (argb.length != size * size || glow.length != size * size) throw new IllegalArgumentException("wrong number of texels");
        this.size = size;
        this.mask = size - 1;
        this.shift = Integer.numberOfTrailingZeros(size);
        this.argb = argb;
        this.glow = glow;
    }

    /** A texture from ARGB texels and a glow per texel between 0 and 1; {@code glow} may be {@code null}. */
    public static Texture of(int size, int[] argb, float[] glow) {
        byte[] g = new byte[size * size];
        if (glow != null) for (int i = 0; i < g.length; i++) g[i] = (byte) Math.round(Math.max(0, Math.min(1, glow[i])) * 255);
        return new Texture(size, argb.clone(), g);
    }

    /** A texture from a square picture, without glow. */
    public static Texture of(BufferedImage image) {
        return of(image, null);
    }

    /**
     * A texture from a square picture and a glow mask of the same size: the brighter a pixel of the mask, the
     * more the texel glows. Black is lit by the room, white shines by itself.
     */
    public static Texture of(BufferedImage image, BufferedImage glowMask) {
        int size = image.getWidth();
        if (image.getHeight() != size) throw new IllegalArgumentException("a texture must be square");
        int[] argb = image.getRGB(0, 0, size, size, null, 0, size);
        byte[] g = new byte[size * size];
        if (glowMask != null) {
            int[] m = glowMask.getRGB(0, 0, size, size, null, 0, size);
            for (int i = 0; i < m.length; i++) g[i] = (byte) ((m[i] >> 8) & 255);
        }
        return new Texture(size, argb, g);
    }

    public int size() {
        return size;
    }

    /** The ARGB colour at texel ({@code u}, {@code v}); coordinates wrap around. */
    public int argb(int u, int v) {
        return argb[((v & mask) << shift) | (u & mask)];
    }

    /** How much texel ({@code u}, {@code v}) shines by itself, 0 to 1. */
    public double glow(int u, int v) {
        return (glow[((v & mask) << shift) | (u & mask)] & 255) / 255.0;
    }

    /** False for a hole: a texel whose alpha is below half. */
    public boolean opaque(int u, int v) {
        return argb(u, v) >>> 24 >= 128;
    }
}
// --8<-- [end:texture]
