package eu.amadeos.gameengine.drive;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadSpriteTest {
    private static RoadSprite red(double glow) {
        BufferedImage im = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 200; y++) for (int x = 0; x < 400; x++) im.setRGB(x, y, 0xffff0000);
        return new RoadSprite(im, 500, glow);
    }

    // --8<-- [start:test-copies]
    @Test
    void copiesComeInFixedWidthsAndAreReused() {
        RoadSprite s = red(0);
        BufferedImage a = s.copy(50, 0, Color.WHITE, 1, Color.BLACK);
        assertTrue(a.getWidth() >= 50 && a.getWidth() < 50 * RoadSprite.WIDTH_STEP + 1, "just wider: " + a.getWidth());
        assertSame(a, s.copy(48, 0, Color.WHITE, 1, Color.BLACK), "the same width step gives the same copy");
        assertEquals(400, s.copy(5000, 0, Color.WHITE, 1, Color.BLACK).getWidth(), "never wider than the picture");
    }
    // --8<-- [end:test-copies]

    @Test
    void fogAndNightTintAllButGlowingSprites() {
        BufferedImage fogged = red(0).copy(40, 1, Color.WHITE, 1, Color.BLACK);
        int p = fogged.getRGB(20, 10);
        assertTrue(((p >> 8) & 255) > 200, "all fog is white: " + Integer.toHexString(p));
        BufferedImage night = red(0).copy(40, 0, Color.WHITE, 0, Color.BLACK);
        assertTrue(((night.getRGB(20, 10) >> 16) & 255) < 20, "black at night");
        BufferedImage lamp = red(1).copy(40, 1, Color.WHITE, 0, Color.BLACK);
        assertEquals(0xffff0000, lamp.getRGB(20, 10), "a glowing thing keeps its colour");
    }

    /** A green block on a brown trunk, with a faint shadow on the ground beside the trunk. */
    private static BufferedImage tree() {
        BufferedImage im = new BufferedImage(200, 300, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 200; y++) for (int x = 20; x < 180; x++) im.setRGB(x, y, 0xff008000);
        for (int y = 200; y < 290; y++) for (int x = 90; x < 110; x++) im.setRGB(x, y, 0xff804000);
        for (int y = 270; y < 300; y++) for (int x = 20; x < 90; x++) im.setRGB(x, y, 0x50000000);
        return im;
    }

    // --8<-- [start:test-parts]
    @Test
    void aTreeIsTakenApartIntoItsSolidPartAndItsShadow() {
        RoadSprite s = new RoadSprite(tree(), 500);
        assertTrue(s.solid() && s.hasSoftPart());
        BufferedImage solid = s.copy(100, 0, Color.WHITE, 1, Color.BLACK);
        assertEquals(java.awt.Transparency.BITMASK, solid.getColorModel().getTransparency(), "one bit of transparency");
        assertEquals(0xff008000, solid.getRGB(50, 50));
        assertEquals(0, solid.getRGB(25, 142) >>> 24, "the shadow is not in the solid part");

        BufferedImage screen = new BufferedImage(200, 300, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = screen.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 200, 300);
        s.paint(g, 0, 0, 200, 300, 0, Color.WHITE, 1, Color.BLACK, false);
        g.dispose();
        assertEquals(0xff008000, screen.getRGB(100, 100), "the tree");
        int shadow = screen.getRGB(50, 285) & 255;
        assertTrue(shadow > 140 && shadow < 200, "the shadow darkens the ground a little: " + shadow);
        assertEquals(0xffffffff, screen.getRGB(150, 285), "nothing on the other side");
    }

    @Test
    void aMirroredSpriteHasItsShadowOnTheOtherSide() {
        RoadSprite s = new RoadSprite(tree(), 500);
        BufferedImage screen = new BufferedImage(200, 300, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = screen.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 200, 300);
        s.paint(g, 0, 0, 200, 300, 0, Color.WHITE, 1, Color.BLACK, true);
        g.dispose();
        assertEquals(0xffffffff, screen.getRGB(50, 285));
        assertTrue((screen.getRGB(150, 285) & 255) < 200, "the shadow lies to the right");
    }
    // --8<-- [end:test-parts]

    @Test
    void aGlowStaysInOnePieceAndAPlainPictureIsAllSolid() {
        BufferedImage glow = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                double d = Math.hypot(x - 50, y - 50) / 50;
                if (d < 1) glow.setRGB(x, y, ((int) (200 * (1 - d)) << 24) | 0xffe080);
            }
        }
        RoadSprite soft = new RoadSprite(glow, 300, 1);
        assertTrue(!soft.solid() && !soft.hasSoftPart());
        assertEquals(java.awt.Transparency.TRANSLUCENT, soft.copy(50, 0, Color.WHITE, 1, Color.BLACK).getColorModel().getTransparency());
        RoadSprite plain = red(0);
        assertTrue(plain.solid() && !plain.hasSoftPart());
    }
}
