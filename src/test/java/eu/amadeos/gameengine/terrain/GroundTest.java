package eu.amadeos.gameengine.terrain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroundTest {
    static final byte SOIL = 1, GRASS = 2, SCORCHED = 3;

    private static Ground flat(double surface) {
        Ground ground = new Ground(100, 50, 0.5);
        ground.fill(x -> surface, (x, depth) -> depth < 1 ? GRASS : SOIL);
        return ground;
    }

    @Test
    void fillMakesEarthFromTheSurfaceDownInLayers() {
        Ground ground = flat(30);
        assertEquals(200, ground.columns());
        assertEquals(100, ground.rows());
        assertEquals(30, ground.surface(50), 1e-9);
        assertFalse(ground.solid(50, 29.9));
        assertEquals(GRASS, ground.material(50, 30.2));
        assertEquals(SOIL, ground.material(50, 40));
        assertTrue(ground.solid(50, 60), "below the ground is earth");
        assertFalse(ground.solid(-1, 40), "beside it is air");
    }

    // --8<-- [start:test-ground]
    @Test
    void earthOverACarvedHoleFallsUntilItRests() {
        Ground ground = flat(20);
        ground.carve(50, 30, 5);                              // a cave under the surface
        assertFalse(ground.solid(50, 30));
        assertEquals(20, ground.surface(50), 1e-9, "the roof still hangs");
        int steps = 0;
        while (ground.settle(2)) steps++;                     // a cell is half a unit: a unit a step
        assertEquals(30, ground.surface(50), 0.5, "the roof fell into the cave");
        assertTrue(steps >= 9 && steps <= 11, "ten units in about ten steps, not at once: " + steps);
        assertEquals(GRASS, ground.material(50, ground.surface(50) + 0.2), "the grass came down on top");
        assertFalse(ground.settling());
    }
    // --8<-- [end:test-ground]

    @Test
    void aHeapOfDirtInTheAirFallsOntoTheGround() {
        Ground ground = flat(40);
        int cells = ground.heap(50, 20, 4, SOIL);
        assertTrue(cells > 0);
        while (ground.settle(4)) {
            // falls
        }
        assertTrue(ground.surface(50) < 40 && ground.surface(50) > 31, "a mound on the ground: " + ground.surface(50));
        assertFalse(ground.solid(50, 20));
    }

    @Test
    void recolourChangesOnlyEarth() {
        Ground ground = flat(30);
        ground.recolour(50, 30, 3, SCORCHED);
        assertEquals(SCORCHED, ground.material(50, 31));
        assertFalse(ground.solid(50, 28), "air stays air");
    }

    @Test
    void theWidestSurfaceIsTheHighestUnderIt() {
        Ground ground = flat(30);
        ground.heap(10, 28, 3, SOIL);
        assertEquals(25, ground.surface(5, 15), 0.5);
        assertEquals(30, ground.surface(20, 30), 1e-9);
    }

    @Test
    void changesAreTheColumnsTouchedAndAreTakenOnce() {
        Ground ground = flat(30);
        assertEquals(new Ground.Changes(0, 199), ground.takeChanges());
        assertNull(ground.takeChanges());
        ground.carve(50, 30, 2);
        Ground.Changes changes = ground.takeChanges();
        assertTrue(changes.from() >= 95 && changes.to() <= 104, changes.toString());
    }

    @Test
    void aCopyIsIndependent() {
        Ground ground = flat(30);
        Ground copy = ground.copy();
        copy.carve(50, 30, 5);
        assertTrue(ground.solid(50, 31));
        assertNotEquals(ground.surface(50), copy.surface(50));
    }
}
