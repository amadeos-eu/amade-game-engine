package eu.amadeos.gameengine.raycast;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaycasterTest {
    private final GridMap map = Maps.of(
            "#####",
            "#...#",
            "#...#",
            "#...#",
            "#####");
    private final LightMap light = new LightMap(map, 1, 1, 1, List.of());

    private int wallPixels(Raycaster r, int column) {
        int n = 0;
        for (int y = 0; y < r.height(); y++) if ((r.frame().getRGB(column, y) & 0xffffff) == 0x808080) n++;
        return n;
    }

    // --8<-- [start:test-raycaster]
    @Test
    void aWallTwiceAsFarIsHalfAsHigh() {
        Raycaster r = new Raycaster(64, 100);
        r.setFog(0, 0);
        r.render(map, light, new Camera(1.5, 2.5, 0), List.of());          // the wall is 2.5 ahead
        assertEquals(2.5, r.depth(32), 0.01);
        int far = wallPixels(r, 32);
        assertEquals(100 * Raycaster.ZOOM / 2.5, far, 1.5);
        r.render(map, light, new Camera(2.75, 2.5, 0), List.of());         // 1.25 ahead
        assertEquals(2 * far, wallPixels(r, 32), 2);
    }
    // --8<-- [end:test-raycaster]

    @Test
    void theFloorIsBelowAndTheCeilingAbove() {
        Raycaster r = new Raycaster(64, 100);
        r.setFog(0, 0);
        r.render(map, light, new Camera(1.5, 2.5, 0), List.of());
        // floors are drawn a little darker than lit, ceilings darker still: light comes from above
        assertEquals(0x5b5b5b, r.frame().getRGB(32, 99) & 0xffffff);
        assertEquals(0x4c4c4c, r.frame().getRGB(32, 0) & 0xffffff);
    }

    @Test
    void fogDarkensWhatIsFar() {
        Raycaster r = new Raycaster(64, 100);
        r.setFog(0x000000, 0.5);
        r.render(map, light, new Camera(1.5, 2.5, 0), List.of());
        int wall = r.frame().getRGB(32, 50) & 0xff;
        assertTrue(wall < 0x80 * Math.exp(-0.5 * 2.5) + 2 && wall > 0x80 * Math.exp(-0.5 * 2.5) - 2);
    }

    @Test
    void aBillboardIsHiddenBehindAWallButNotInFront() {
        GridMap pillar = Maps.of(
                "#######",
                "#.....#",
                "#..#..#",
                "#.....#",
                "#######");
        LightMap l = new LightMap(pillar, 1, 1, 1, List.of());
        Raycaster r = new Raycaster(64, 100);
        r.setFog(0, 0);
        Billboard thing = new Billboard(Views.of(Maps.solid(0xffff0000)), 4.5, 2.5, 0.5);
        r.render(pillar, l, new Camera(1.5, 2.5, 0), List.of(thing));
        assertNotEquals(0xff0000, r.frame().getRGB(32, 70) & 0xffffff);
        thing.x = 2.5;
        r.render(pillar, l, new Camera(1.5, 2.5, 0), List.of(thing));
        assertEquals(0xff0000, r.frame().getRGB(32, 70) & 0xffffff);
    }

    @Test
    void aPointOnTheFloorStraightAheadProjectsBelowTheMiddle() {
        Raycaster r = new Raycaster(64, 100);
        double[] p = r.project(new Camera(1.5, 2.5, 0), 3.5, 2.5, 0);
        assertEquals(32, p[0], 1e-9);
        assertEquals(50 + 0.5 * 120 / 2, p[1], 1e-9);
        assertEquals(2, p[2], 1e-9);
    }
}
