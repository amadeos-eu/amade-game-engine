package eu.amadeos.gameengine.sprite;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RotationsTest {
    // --8<-- [start:test-rotations]
    @Test
    void theNearestDirectionIsShown() {
        // A bar from the middle to the right edge: pointing at angle 0.
        Rotations pointer = Rotations.of(32, 20, (g, w, h) -> {
            g.setColor(Color.WHITE);
            g.fill(new Rectangle2D.Double(w / 2, h / 2 - 2, w / 2, 4));
        });
        assertEquals(0, pointer.index(0.04));
        assertEquals(1, pointer.index(Math.PI / 16 - 0.04), "11.25° per step");
        assertEquals(8, pointer.index(Math.PI / 2));
        assertEquals(24, pointer.index(-Math.PI / 2), "up");
        assertEquals(0, pointer.index(4 * Math.PI), "two whole turns");

        BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.scale(2, 2);
        pointer.draw(g, 10, 10, Math.PI / 2 + 0.05);     // pointing down
        g.dispose();
        assertEquals(255, image.getRGB(20, 34) >>> 24, "below the middle");
        assertEquals(0, image.getRGB(34, 20) >>> 24, "not to the right");
    }
    // --8<-- [end:test-rotations]
}
