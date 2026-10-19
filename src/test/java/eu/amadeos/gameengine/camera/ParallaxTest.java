package eu.amadeos.gameengine.camera;

import eu.amadeos.gameengine.sprite.Sprite;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParallaxTest {
    /** A picture 100 wide: red on its first 10 units, blue on the rest. */
    private static final Sprite STRIPE = Sprite.drawn(100, 20, (g, w, h) -> {
        g.setColor(Color.BLUE);
        g.fill(new Rectangle2D.Double(0, 0, w, h));
        g.setColor(Color.RED);
        g.fill(new Rectangle2D.Double(0, 0, 10, h));
    });

    // --8<-- [start:test-parallax]
    @Test
    void aLayerMovesByItsFactorOfTheCamerasWayAndRepeats() {
        Parallax back = new Parallax(300, 100).layer(STRIPE, 0.5, 0, 40);
        BufferedImage screen = draw(back, 0);
        assertEquals(Color.RED.getRGB(), screen.getRGB(5, 50));
        assertEquals(Color.RED.getRGB(), screen.getRGB(205, 50), "the third copy");
        screen = draw(back, 40);                 // the camera 40 to the right: the layer 20 to the left
        assertEquals(Color.BLUE.getRGB(), screen.getRGB(5, 50));
        assertEquals(Color.RED.getRGB(), screen.getRGB(85, 50), "the second copy now starts at 80");
        assertEquals(Color.RED.getRGB(), screen.getRGB(285, 50), "and a fourth comes in on the right");
    }
    // --8<-- [end:test-parallax]

    @Test
    void theVerticalFactorMovesTheLayerUpAsTheCameraGoesDown() {
        Parallax back = new Parallax(300, 100).layer(STRIPE, 0, 0.25, 40);
        BufferedImage screen = draw(back, 0, 80);   // 80 down: the layer 20 up, now from 20 to 40
        assertEquals(Color.RED.getRGB(), screen.getRGB(5, 25));
        assertEquals(0, screen.getRGB(5, 45));
    }

    private static BufferedImage draw(Parallax back, double cameraX) {
        return draw(back, cameraX, 0);
    }

    private static BufferedImage draw(Parallax back, double cameraX, double cameraY) {
        BufferedImage screen = new BufferedImage(300, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = screen.createGraphics();
        back.draw(g, cameraX, cameraY);
        g.dispose();
        return screen;
    }
}
