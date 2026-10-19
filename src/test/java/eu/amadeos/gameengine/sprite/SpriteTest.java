package eu.amadeos.gameengine.sprite;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SpriteTest {
    /** A 40 × 16 source: red left half, blue right half. */
    private static BufferedImage source() {
        BufferedImage image = new BufferedImage(40, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 20, 16);
        g.setColor(Color.BLUE);
        g.fillRect(20, 0, 20, 16);
        g.dispose();
        return image;
    }

    // --8<-- [start:test-transparency]
    @Test
    void opaqueAndHardEdgedPicturesAreCachedWithoutBlending() {
        BufferedImage screen = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = screen.createGraphics();
        g.scale(2.5, 2.5);
        Sprite sky = Sprite.of(source(), 40, 16);                       // every pixel opaque
        sky.draw(g, 0, 0);
        assertEquals(Transparency.OPAQUE, sky.cachedTransparency());

        BufferedImage holes = source();
        holes.setRGB(3, 3, 0);                                          // one pixel invisible
        Sprite hill = Sprite.of(holes, 40, 16).hardEdges();
        hill.draw(g, 0, 0);
        assertEquals(Transparency.BITMASK, hill.cachedTransparency());
        // Scaled smoothly, the hole's edge is half seen: blending is needed again.
        Sprite smooth = Sprite.of(holes, 40, 16);
        smooth.draw(g, 0, 0);
        assertEquals(Transparency.TRANSLUCENT, smooth.cachedTransparency());

        holes.setRGB(5, 5, 0x80FF0000);                                 // and one half seen
        assertEquals(Transparency.TRANSLUCENT, Sprite.transparencyOf(holes));
    }
    // --8<-- [end:test-transparency]

    // --8<-- [start:test-cache]
    @Test
    void theSpriteIsScaledOnceToItsSizeOnTheScreen() {
        Sprite brick = Sprite.of(source(), 20, 8);   // shown at half the source's pixels
        assertNull(brick.cachedSize());
        BufferedImage screen = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = screen.createGraphics();
        g.scale(3, 3);                                // three screen pixels per logical unit
        brick.draw(g, 10, 5);
        assertEquals(new Dimension(60, 24), brick.cachedSize());
        // At 3 × (10, 5): red on the left, blue on the right.
        assertEquals(Color.RED.getRGB(), screen.getRGB(31, 16));
        assertEquals(Color.BLUE.getRGB(), screen.getRGB(88, 37));
        assertEquals(0, screen.getRGB(29, 16));
        g.dispose();
    }
    // --8<-- [end:test-cache]

    @Test
    void aNewScaleOrPixelModeMakesANewCache() {
        Sprite brick = Sprite.of(source(), 20, 8);
        BufferedImage screen = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = screen.createGraphics();
        brick.draw(g, 0, 0);
        assertEquals(new Dimension(20, 8), brick.cachedSize());
        g.scale(2, 2);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        brick.draw(g, 0, 0);
        assertEquals(new Dimension(40, 16), brick.cachedSize());
        g.dispose();
    }

    @Test
    void drawnSpritesAreDrawnAtTheScreenSize() {
        Sprite dot = Sprite.drawn(4, 4, (g, w, h) -> {
            g.setColor(Color.GREEN);
            g.fill(new java.awt.geom.Rectangle2D.Double(0, 0, w, h));
        });
        BufferedImage screen = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = screen.createGraphics();
        g.scale(5, 5);
        dot.draw(g, 2, 2);
        g.dispose();
        assertEquals(new Dimension(20, 20), dot.cachedSize());
        assertEquals(Color.GREEN.getRGB(), screen.getRGB(10, 10));
        assertEquals(Color.GREEN.getRGB(), screen.getRGB(29, 29));
        assertEquals(0, screen.getRGB(30, 30));
    }

    // --8<-- [start:test-seamless]
    @Test
    void tilesDrawnSeamlesslyLeaveNoGapsAtAnyScale() {
        Sprite tile = Sprite.drawn(16, 16, (g, w, h) -> {
            g.setColor(Color.GREEN);
            g.fill(new java.awt.geom.Rectangle2D.Double(0, 0, w, h));
        });
        BufferedImage plain = row(tile, false);
        BufferedImage seamless = row(tile, true);
        int gaps = 0;
        for (int x = 0; x < 20 * 43; x++) {
            if (plain.getRGB(x, 20) == 0) gaps++;
            assertEquals(Color.GREEN.getRGB(), seamless.getRGB(x, 20), "pixel " + x);
        }
        assertEquals(4, gaps, "draw: a pixel lost at every fifth tile");
        assertEquals(2, tile.seamlessCount(), "43 and 44 pixels wide, 43 high");
    }

    /** Twenty tiles side by side at 2.7 pixels per unit, starting at a fraction of a unit. */
    private static BufferedImage row(Sprite tile, boolean seamless) {
        BufferedImage screen = new BufferedImage(900, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = screen.createGraphics();
        g.scale(2.7, 2.7);
        g.translate(-0.3, 0);
        for (int i = 0; i < 20; i++) {
            if (seamless) tile.drawSeamless(g, i * 16, 0);
            else tile.draw(g, i * 16, 0);
        }
        g.dispose();
        return screen;
    }
    // --8<-- [end:test-seamless]
}
