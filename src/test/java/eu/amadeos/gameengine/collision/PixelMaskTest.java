package eu.amadeos.gameengine.collision;

import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.geom.Ellipse2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PixelMaskTest {
    // --8<-- [start:test-mask]
    @Test
    void aRoundSignIsHitOnlyWhereItIsRound() {
        // a disc 40 pixels across in a 200 × 100 scene, and a 4 × 4 block flown around it
        PixelMask scene = PixelMask.painted(200, 100, g -> g.fill(new Ellipse2D.Double(80, 30, 40, 40)));
        PixelMask block = new PixelMask(4, 4);
        for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) block.set(x, y);
        assertTrue(scene.overlaps(block, 98, 48), "the middle of the disc");
        assertFalse(scene.overlaps(block, 80, 30), "the corner of its box is empty");
        assertFalse(scene.overlaps(block, 120, 48), "beside it");
        assertTrue(scene.overlaps(block, 117, 48), "its right edge");
    }
    // --8<-- [end:test-mask]

    @Test
    void rowsLongerThan64PixelsAreShiftedAcrossTheirLongs() {
        PixelMask scene = new PixelMask(300, 3);
        scene.set(130, 1);
        PixelMask line = new PixelMask(100, 1);
        for (int x = 0; x < 100; x++) line.set(x, 0);
        for (int dx = -120; dx < 320; dx++) {
            boolean expected = dx <= 130 && 130 < dx + 100;
            assertEquals(expected, scene.overlaps(line, dx, 1), "at " + dx);
        }
        assertEquals(new Point(130, 1), scene.firstOverlap(line, 100, 1));
        assertNull(scene.firstOverlap(line, 100, 0), "another row");
        assertNull(scene.firstOverlap(line, 100, -5), "above the scene");
    }

    @Test
    void masksAreMadeFromPicturesAndAdded() {
        PixelMask dot = PixelMask.painted(3, 3, g -> g.fillRect(1, 1, 1, 1));
        assertEquals(1, dot.count());
        assertTrue(dot.solid(1, 1));
        assertFalse(dot.solid(-1, 1), "outside nothing is solid");
        PixelMask scene = new PixelMask(10, 10);
        scene.add(dot, 5, 5);
        assertTrue(scene.solid(6, 6));
        assertTrue(scene.anyIn(4, 4, 3, 3));
        assertFalse(scene.anyIn(0, 0, 6, 6));
        scene.clear(6, 6);
        assertEquals(0, scene.count());
    }
}
