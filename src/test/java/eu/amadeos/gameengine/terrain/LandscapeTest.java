package eu.amadeos.gameengine.terrain;

import eu.amadeos.gameengine.random.SeededRandom;
import org.junit.jupiter.api.Test;

import java.util.function.DoubleUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LandscapeTest {
    @Test
    void theSameSeedMakesTheSameLandscape() {
        double[] a = Landscape.generate(129, 300, 150, 0.55, new SeededRandom(7));
        double[] b = Landscape.generate(129, 300, 150, 0.55, new SeededRandom(7));
        double[] c = Landscape.generate(129, 300, 150, 0.55, new SeededRandom(8));
        assertArrayEquals(a, b);
        assertTrue(a[64] != c[64]);
    }

    @Test
    void rougherLinesHaveBiggerSmallSteps() {
        double smooth = 0, rough = 0;
        for (int seed = 0; seed < 20; seed++) {
            smooth += bumpiness(Landscape.generate(257, 300, 150, 0.4, new SeededRandom(seed)));
            rough += bumpiness(Landscape.generate(257, 300, 150, 0.75, new SeededRandom(seed)));
        }
        assertTrue(rough > smooth * 2, rough + " vs " + smooth);
    }

    private static double bumpiness(double[] line) {
        double sum = 0;
        for (int i = 1; i < line.length; i++) sum += Math.abs(line[i] - line[i - 1]);
        return sum;
    }

    @Test
    void clampFlattenAndSample() {
        double[] line = {100, 400, 250, 200, 50};
        Landscape.clamp(line, 120, 370);
        assertArrayEquals(new double[]{120, 370, 250, 200, 120}, line);
        Landscape.flatten(line, 2, 1);
        assertArrayEquals(new double[]{120, 250, 250, 250, 120}, line);
        DoubleUnaryOperator surface = Landscape.sample(line, 10);
        assertEquals(185, surface.applyAsDouble(5), 1e-9);
        assertEquals(120, surface.applyAsDouble(-3), 1e-9);
        assertEquals(120, surface.applyAsDouble(99), 1e-9);
    }

    @Test
    void aBumpRaisesTheMiddleMost() {
        double[] line = new double[11];
        Landscape.bump(line, 5, 2, -50);
        assertEquals(-50, line[5], 1e-9);
        assertTrue(line[3] > -50 && line[3] < 0);
        assertTrue(Math.abs(line[0]) < 0.1);
    }
}
