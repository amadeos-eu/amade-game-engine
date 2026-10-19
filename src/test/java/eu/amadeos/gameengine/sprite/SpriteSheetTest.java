package eu.amadeos.gameengine.sprite;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpriteSheetTest {
    /**
     * A sheet of 3 × 2 cells of 4 × 4 pixels: each cell one colour, except the last, a checkerboard of
     * black and white pixels.
     */
    private static BufferedImage sheet() {
        Color[] colours = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN};
        BufferedImage image = new BufferedImage(12, 8, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < 6; i++) {
            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 4; x++) {
                    int rgb = i < 5 ? colours[i].getRGB() : ((x + y) % 2 == 0 ? Color.BLACK : Color.WHITE).getRGB();
                    image.setRGB((i % 3) * 4 + x, (i / 3) * 4 + y, rgb);
                }
            }
        }
        return image;
    }

    // --8<-- [start:test-sheet]
    @Test
    void theCellsAreNumberedRowByRow() {
        SpriteSheet sheet = SpriteSheet.of(sheet(), 3, 2, 8, 8);
        assertEquals(6, sheet.frames());
        BufferedImage screen = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = screen.createGraphics();
        sheet.frame(4).draw(g, 0, 0);       // second row, second column
        assertEquals(Color.CYAN.getRGB(), screen.getRGB(4, 4));
        sheet.frame(2, 0).draw(g, 0, 0);
        assertEquals(Color.BLUE.getRGB(), screen.getRGB(4, 4));
        g.dispose();
    }

    @Test
    void pixelArtKeepsHardEdgesInEveryRenderMode() {
        SpriteSheet sheet = SpriteSheet.of(sheet(), 3, 2, 4, 4).hardEdges();
        BufferedImage screen = new BufferedImage(12, 12, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = screen.createGraphics();
        g.scale(3, 3);       // smooth scaling would blur black and white into greys
        sheet.frame(5).draw(g, 0, 0);
        g.dispose();
        Set<Integer> colours = new HashSet<>();
        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 12; x++) colours.add(screen.getRGB(x, y));
        }
        assertEquals(Set.of(Color.BLACK.getRGB(), Color.WHITE.getRGB()), colours);
        assertEquals(Color.BLACK.getRGB(), screen.getRGB(2, 2), "each pixel a block of 3 × 3");
        assertEquals(Color.WHITE.getRGB(), screen.getRGB(3, 2));
    }
    // --8<-- [end:test-sheet]

    @Test
    void cellsMustDivideTheImage() {
        assertThrows(IllegalArgumentException.class, () -> SpriteSheet.of(sheet(), 5, 2, 8, 8));
        assertTrue(SpriteSheet.of(sheet(), 1, 1, 8, 8).frames() == 1);
    }
}
