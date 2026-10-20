package eu.amadeos.gameengine.sprite;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScrollingLayerTest {
    /** A world of 4 × 4 unit squares in colours worked out from their position. */
    private static final ScrollingLayer.Painter CHECKS = (g, left, top, width, height) -> {
        for (int y = (int) Math.floor(top / 4); y * 4 < top + height; y++) {
            for (int x = (int) Math.floor(left / 4); x * 4 < left + width; x++) {
                g.setColor(new Color((x * 40) & 0xFF, (y * 70) & 0xFF, ((x + y) * 25) & 0xFF));
                g.fill(new Rectangle2D.Double(x * 4, y * 4, 4, 4));
            }
        }
    };

    private static BufferedImage frame(ScrollingLayer layer, double left, double top, long key) {
        BufferedImage screen = new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = screen.createGraphics();
        g.scale(2, 2);
        layer.draw(g, 0, 0, left, top, key, CHECKS);
        g.dispose();
        return screen;
    }

    private static void assertSame(BufferedImage expected, BufferedImage actual) {
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                assertEquals(expected.getRGB(x, y), actual.getRGB(x, y), "pixel " + x + ", " + y);
            }
        }
    }

    @Test
    void aStillViewIsPaintedOnce() {
        ScrollingLayer layer = new ScrollingLayer(60, 40);
        frame(layer, 10, 10, 1);
        frame(layer, 10, 10, 1);
        assertEquals(1, layer.painted());
    }

    // --8<-- [start:test-scroll]
    @Test
    void aMovedViewPaintsOnlyTheNewStripsAndLooksAsIfPaintedWhole() {
        ScrollingLayer layer = new ScrollingLayer(60, 40);
        frame(layer, 10, 10, 1);
        BufferedImage scrolled = frame(layer, 13.5, 8, 1);
        assertEquals(3, layer.painted(), "the whole view, then a strip at the right and one at the top");
        assertSame(frame(new ScrollingLayer(60, 40), 13.5, 8, 1), scrolled);
    }
    // --8<-- [end:test-scroll]

    @Test
    void anotherKeyOrAJumpPaintsTheWholeView() {
        ScrollingLayer layer = new ScrollingLayer(60, 40);
        frame(layer, 10, 10, 1);
        frame(layer, 10, 10, 2);
        assertEquals(2, layer.painted());
        frame(layer, 500, 10, 2);
        assertEquals(3, layer.painted());
    }
}
