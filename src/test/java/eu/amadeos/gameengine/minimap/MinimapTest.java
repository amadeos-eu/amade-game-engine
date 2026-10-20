package eu.amadeos.gameengine.minimap;

import eu.amadeos.gameengine.camera.Camera;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinimapTest {
    // --8<-- [start:test-minimap]
    @Test
    void aPointOfTheMinimapIsAPlaceInTheWorldAndBack() {
        // A map of 64 × 32 tiles of 16 units, in a box of 128 × 128: two pixels per tile, centred.
        Minimap minimap = new Minimap(500, 20, 128, 128, 64, 32, 16);
        assertEquals(128, minimap.width());
        assertEquals(64, minimap.height());
        assertEquals(52, minimap.y(), "centred in the box");
        assertTrue(minimap.contains(564, 84));
        assertFalse(minimap.contains(564, 30), "above the map, in the empty part of the box");
        assertEquals(64 * 16 / 2.0, minimap.worldX(564), 1e-9, "the middle of the minimap");
        assertEquals(32 * 16 / 2.0, minimap.worldY(84), 1e-9);
        assertEquals(564, minimap.screenX(minimap.worldX(564)), 1e-9);
    }
    // --8<-- [end:test-minimap]

    @Test
    void tilesAreDrawnAsSquaresAndTheViewAsABox() {
        Minimap minimap = new Minimap(0, 0, 40, 40, 4, 4, 10);
        for (int row = 0; row < 4; row++) for (int col = 0; col < 4; col++) minimap.paint(col, row, 0x203040);
        minimap.paint(1, 2, 0xFF0000);
        Camera camera = new Camera(20, 20, 40, 40);
        camera.jumpTo(20, 20);                      // sees the middle: 10 to 30 in both directions
        BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        minimap.render(g);
        minimap.view(g, camera, 1, Color.WHITE);
        g.dispose();
        assertEquals(0xFF0000, image.getRGB(15, 25) & 0xFFFFFF, "tile (1, 2), ten pixels a side");
        assertEquals(0x203040, image.getRGB(35, 35) & 0xFFFFFF);
        boolean edge = false;
        for (int x = 8; x <= 12; x++) edge |= (image.getRGB(x, 20) & 0xFFFFFF) == 0xFFFFFF;
        assertTrue(edge, "the view's left edge at 10");
        assertEquals(0xFF0000, minimap.colour(1, 2));
    }
}
