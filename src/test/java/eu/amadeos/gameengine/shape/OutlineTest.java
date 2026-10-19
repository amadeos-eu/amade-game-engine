package eu.amadeos.gameengine.shape;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutlineTest {
    // --8<-- [start:test-outline]
    @Test
    void aFigureIsTurnedMovedAndEnlarged() {
        Outline arrow = Outline.closed(10, 0, -5, -5, -5, 5);    // nose to the right
        assertEquals(10, arrow.radius(), 1e-9);
        // A quarter turn clockwise, twice the size, at 100, 50: the nose points down.
        Path2D.Double path = arrow.path(100, 50, Math.PI / 2, 2);
        double[] nose = first(path);
        assertEquals(100, nose[0], 1e-9);
        assertEquals(70, nose[1], 1e-9);
        Rectangle2D bounds = path.getBounds2D();
        assertEquals(new Rectangle2D.Double(90, 40, 20, 30), bounds);
    }
    // --8<-- [end:test-outline]

    @Test
    void thePathIsReused() {
        Outline line = Outline.open(0, 0, 10, 0);
        assertSame(line.path(0, 0, 0, 1), line.path(5, 5, 1, 1));
        assertEquals(2, line.corners());
    }

    @Test
    void drawsWithTheCurrentColour() {
        BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        Outline.closed(10, 0, -10, -10, -10, 10).fill(g, 20, 20, 0, 1);
        Outline.open(-15, -15, 15, -15).draw(g, 20, 20, 0, 1);
        g.dispose();
        assertEquals(0xFFFFFFFF, image.getRGB(18, 20));
        assertEquals(0xFFFFFFFF, image.getRGB(20, 5));
        assertEquals(0, image.getRGB(35, 35));
    }

    @Test
    void needsAtLeastTwoPoints() {
        assertThrows(IllegalArgumentException.class, () -> Outline.closed(1, 2));
        assertThrows(IllegalArgumentException.class, () -> Outline.closed(1, 2, 3));
    }

    private static double[] first(Path2D path) {
        double[] point = new double[6];
        PathIterator it = path.getPathIterator(null);
        it.currentSegment(point);
        return point;
    }
}
