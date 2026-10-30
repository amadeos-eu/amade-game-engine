package eu.amadeos.gameengine.raycast;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightMapTest {
    private final GridMap map = Maps.of(
            "#######",
            "#..#..#",
            "#..#..#",
            "#.....#",
            "#######");
    private final float[] rgb = new float[3];

    // --8<-- [start:test-lightmap]
    @Test
    void aLampLightsWhatItSeesAndNotWhatIsBehindAWall() {
        LightMap light = new LightMap(map, 0.1, 0.1, 0.1, List.of(new LightMap.Light(1.5, 1.5, 1, 0.5, 0, 4)));
        light.sample(1.5, 1.5, rgb);
        assertEquals(1.1, rgb[0], 1e-5);
        assertEquals(0.6, rgb[1], 1e-5);
        assertEquals(0.1, rgb[2], 1e-5);
        light.sample(4.5, 1.5, rgb);                    // behind the wall at x = 3
        assertEquals(0.1, rgb[0], 1e-5);
    }
    // --8<-- [end:test-lightmap]

    @Test
    void lightFadesWithDistance() {
        LightMap light = new LightMap(map, 0, 0, 0, List.of(new LightMap.Light(1.5, 3.5, 1, 1, 1, 5)));
        float[] near = new float[3], far = new float[3];
        light.sample(2.5, 3.5, near);
        light.sample(4.5, 3.5, far);
        assertTrue(near[0] > far[0] && far[0] > 0);
    }

    @Test
    void aFlashLastsOneFrame() {
        LightMap light = new LightMap(map, 0.1, 0.1, 0.1, List.of());
        light.flash(new LightMap.Light(5.5, 3.5, 2, 2, 2, 2));
        light.sample(5.5, 3.5, rgb);
        assertTrue(rgb[0] > 1);
        light.beginFrame();
        light.sample(5.5, 3.5, rgb);
        assertEquals(0.1, rgb[0], 1e-5);
    }
}
