package eu.amadeos.gameengine.iso;

import eu.amadeos.gameengine.camera.Camera;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiamondMinimapTest {
    // --8<-- [start:test-minimap]
    @Test
    void theMinimapIsTheProjectionsPictureScaledDown() {
        IsoProjection iso = IsoProjection.forMap(40, 40, 16, 32, 16, 100);
        DiamondMinimap minimap = new DiamondMinimap(iso, 500, 300, 128);   // 1280 units wide: a tenth
        assertEquals(128, minimap.width(), 1e-9);
        assertEquals(64, minimap.height(), 1e-9);
        // The map's corners touch the box: top corner in the middle at the top, left corner on the left.
        assertEquals(564, minimap.screenX(0, 0), 1e-9);
        assertEquals(300, minimap.screenY(0, 0), 1e-9);
        assertEquals(500, minimap.screenX(0, 40 * 16), 1e-9);
        assertTrue(minimap.contains(564, 332));
        assertFalse(minimap.contains(502, 302), "the box's corner is not on the diamond");
        // A click is a point of the picture: where the camera goes.
        assertEquals(iso.pictureX(20 * 16, 20 * 16), minimap.pictureX(564), 1e-9);
        assertEquals(iso.pictureY(20 * 16, 20 * 16, 0), minimap.pictureY(332), 1e-9);
    }
    // --8<-- [end:test-minimap]

    @Test
    void tilesAreLittleDiamondsAndTheViewABox() {
        IsoProjection iso = IsoProjection.forMap(4, 4, 16, 32, 16, 0);
        DiamondMinimap minimap = new DiamondMinimap(iso, 0, 0, 80);   // 128 units wide: 0.625
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) minimap.paint(c, r, 0x203040);
        minimap.paint(3, 3, 0xFF0000);
        Camera camera = new Camera(32, 16, iso.width(), iso.height());
        camera.jumpTo(64, 32);
        BufferedImage image = new BufferedImage(80, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        minimap.render(g);
        g.dispose();
        // Tile (3, 3) is the bottom corner of the diamond.
        int x = (int) minimap.screenX(3.5 * 16, 3.5 * 16);
        int y = (int) minimap.screenY(3.5 * 16, 3.5 * 16);
        assertEquals(0xFF0000, image.getRGB(x, y) & 0xFFFFFF);
        assertEquals(0x203040, image.getRGB(40, 10) & 0xFFFFFF);
        assertEquals(0, image.getRGB(2, 2) & 0xFFFFFF, "outside the diamond");
        Graphics2D view = image.createGraphics();
        minimap.view(view, camera, 1, Color.WHITE);
        view.dispose();
        assertEquals(0xFF0000, minimap.colour(3, 3));
    }
}
