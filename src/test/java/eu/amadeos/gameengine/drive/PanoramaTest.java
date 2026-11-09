package eu.amadeos.gameengine.drive;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanoramaTest {
    @Test
    void aRangeIsTransparentAboveItsRidgeAndWrapsAround() {
        BufferedImage r = Panorama.range(600, 120, 3, 2.5, new Color(120, 120, 150), new Color(200, 200, 220), 0.3, 0.6, false);
        assertEquals(0, r.getRGB(300, 0) >>> 24, "sky above the peaks");
        assertEquals(255, r.getRGB(300, 119) >>> 24, "mountain at the bottom");
        int left = top(r, 0), right = top(r, 599);
        assertTrue(Math.abs(left - right) <= 3, "the edges meet: " + left + " / " + right);
    }

    private static int top(BufferedImage im, int x) {
        for (int y = 0; y < im.getHeight(); y++) if ((im.getRGB(x, y) >>> 24) != 0) return y;
        return im.getHeight();
    }
}
