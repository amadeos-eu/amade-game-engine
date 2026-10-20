package eu.amadeos.gameengine.fog;

import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FogOfWarTest {
    private static void step(FogOfWar fog, int col, int row, int radius) {
        fog.begin();
        fog.look(col, row, radius);
        fog.end();
    }

    // --8<-- [start:test-fog]
    @Test
    void whatWasSeenStaysKnownButOnlyWhatIsSeenNowIsVisible() {
        FogOfWar fog = new FogOfWar(20, 10);
        assertEquals(FogOfWar.State.UNSEEN, fog.state(5, 5));
        step(fog, 5, 5, 3);
        assertEquals(FogOfWar.State.VISIBLE, fog.state(5, 5));
        assertEquals(FogOfWar.State.VISIBLE, fog.state(8, 5), "three tiles away");
        assertEquals(FogOfWar.State.UNSEEN, fog.state(8, 8), "outside the circle, though inside its square");

        step(fog, 15, 5, 3);                        // the unit drove on
        assertEquals(FogOfWar.State.SEEN, fog.state(5, 5), "known, but nobody looks now");
        assertEquals(FogOfWar.State.VISIBLE, fog.state(15, 5));
        assertFalse(fog.visible(-1, 5), "outside the map");
    }
    // --8<-- [end:test-fog]

    @Test
    void theVersionChangesOnlyWhenATileDoes() {
        FogOfWar fog = new FogOfWar(10, 10);
        step(fog, 5, 5, 2);
        int version = fog.version();
        step(fog, 5, 5, 2);
        assertEquals(version, fog.version(), "the same view: nothing to draw again");
        step(fog, 6, 5, 2);
        assertTrue(fog.version() > version);
    }

    // --8<-- [start:test-save]
    @Test
    void theSeenTilesSurviveASaveFile() {
        FogOfWar fog = new FogOfWar(64, 64);
        step(fog, 10, 10, 5);
        step(fog, 50, 40, 4);
        String text = fog.seenAsText();
        assertEquals(684, text.length(), "4,096 tiles in 512 bytes, as Base64");
        FogOfWar loaded = new FogOfWar(64, 64);
        loaded.restoreSeen(text);
        for (int row = 0; row < 64; row++) {
            for (int col = 0; col < 64; col++) assertEquals(fog.seen(col, row), loaded.seen(col, row));
        }
        assertFalse(loaded.visible(10, 10), "what is visible comes with the next step, not from the file");
        assertThrows(IllegalArgumentException.class, () -> new FogOfWar(32, 32).restoreSeen(text));
    }
    // --8<-- [end:test-save]

    // --8<-- [start:test-painter]
    @Test
    void cornersTakeTheDarkestTileAndTheShadeRunsAcross() {
        FogOfWar fog = new FogOfWar(4, 1);
        step(fog, 0, 0, 1);                          // tiles 0 and 1 visible, 2 and 3 unseen
        assertEquals(0, FogPainter.corner(fog, 1, 0), "between two visible tiles");
        assertEquals(2, FogPainter.corner(fog, 2, 0), "between a visible and an unseen tile: unseen");
        assertEquals(0, FogPainter.corner(fog, 0, 0), "the edge of the map adds no darkness");

        FogPainter painter = new FogPainter(16);
        BufferedImage image = new BufferedImage(64, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        painter.render(g, fog, 0, 0, 3, 0);
        g.dispose();
        assertEquals(0, image.getRGB(4, 8) >>> 24, "deep in the visible: clear");
        int left = image.getRGB(17, 8) >>> 24;
        int right = image.getRGB(30, 8) >>> 24;
        assertTrue(left < 60 && right > 200, "the second tile shades from clear to black: " + left + ", " + right);
        assertEquals(255, image.getRGB(56, 8) >>> 24, "deep in the unseen: black");
    }
    // --8<-- [end:test-painter]

    @Test
    void theNoiseMeetsItselfAtTheEdgesOfATile() {
        for (double v = 0; v < 1; v += 0.1) assertEquals(FogPainter.noise(0, v), FogPainter.noise(1, v), 1e-9);
    }
}
