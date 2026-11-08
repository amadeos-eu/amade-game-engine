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
}
