package eu.amadeos.gameengine.raycast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridMapTest {
    private final GridMap.Ray ray = new GridMap.Ray();

    // --8<-- [start:test-cast]
    @Test
    void aRayStopsAtTheFirstWall() {
        GridMap map = Maps.of(
                "######",
                "#....#",
                "######");
        map.cast(1.5, 1.5, 1, 0, 100, ray);
        assertEquals(3.5, ray.distance, 1e-9);
        assertEquals(5, ray.cellX);
        assertEquals(0, ray.side);
        assertSame(Maps.WALL, ray.texture);

        map.cast(1.5, 1.5, 0, -1, 100, ray);
        assertEquals(0.5, ray.distance, 1e-9);
        assertEquals(1, ray.side);
    }
    // --8<-- [end:test-cast]

    @Test
    void theDistanceIsCountedInLengthsOfTheDirection() {
        GridMap map = Maps.of("######", "#....#", "######");
        map.cast(1.5, 1.5, 2, 0, 100, ray);      // the raycaster's rays are longer than 1
        assertEquals(1.75, ray.distance, 1e-9);
    }

    @Test
    void wherePointsOnTheFaceGoFromLeftToRight() {
        GridMap map = Maps.of("######", "#....#", "######");
        map.cast(1.5, 1.5, 1, 0.1, 100, ray);   // a little to the right of straight ahead
        assertTrue(ray.u > 0.8, "u = " + ray.u);
        map.cast(1.5, 1.5, 1, -0.1, 100, ray);
        assertTrue(ray.u < 0.2, "u = " + ray.u);
    }

    @Test
    void aClosedDoorStandsInTheMiddleOfItsCell() {
        GridMap map = Maps.of(
                "#######",
                "#..|..#",
                "#######");
        map.cast(1.5, 1.5, 1, 0, 100, ray);
        assertEquals(2.0, ray.distance, 1e-9);
        assertSame(Maps.DOOR, ray.texture);
        assertTrue(map.blocks(3, 1));
        assertFalse(map.sees(1.5, 1.5, 5.5, 1.5));
    }

    @Test
    void anOpenDoorLetsRaysAndBodiesThrough() {
        GridMap map = Maps.of(
                "#######",
                "#..|..#",
                "#######");
        Door door = map.door(3, 1);
        door.open();
        for (int i = 0; i < 60; i++) door.update(0.02, false);
        assertEquals(Door.State.OPEN, door.state());
        map.cast(1.5, 1.5, 1, 0, 100, ray);
        assertEquals(4.5, ray.distance, 1e-9);
        assertFalse(map.blocks(3, 1));
        assertTrue(map.sees(1.5, 1.5, 5.5, 1.5));
    }

    @Test
    void aHalfOpenDoorShowsTheFrameInItsOpening() {
        GridMap map = Maps.of(
                "#####",
                "#...#",
                "##-##",
                "#...#",
                "#####");
        Door door = map.door(2, 2);
        door.open();
        door.update(0.3, false);                 // half open: slid towards x = 2
        assertEquals(0.5, door.openness(), 1e-9);
        map.cast(2.25, 1.5, 0, 1, 100, ray);     // through the open half
        assertTrue(ray.distance > 1);
        map.cast(2.75, 1.5, 0, 1, 100, ray);     // the closed half
        assertEquals(1.0, ray.distance, 1e-9);
        assertSame(door, ray.door);
        // looking sideways from inside the door's cell, the wall next to it is the frame
        map.cast(2.25, 2.3, -1, 0.2, 100, ray);
        assertSame(Maps.FRAME, ray.texture);
    }

    @Test
    void aDoorWaitsWhileSomethingStandsInIt() {
        Door door = new Door(0, 0, true, Maps.DOOR, null).timing(0.5, 1);
        door.open();
        door.update(0.5, false);
        door.update(1.0, true);
        assertEquals(Door.State.OPEN, door.state());
        door.update(0.1, false);
        assertEquals(Door.State.CLOSING, door.state());
        door.update(0.1, true);                    // stepped back in: open again
        assertEquals(Door.State.OPENING, door.state());
    }

    @Test
    void outsideTheMapEverythingBlocks() {
        GridMap map = Maps.of("..", "..");
        assertTrue(map.blocks(-1, 0));
        map.cast(0.5, 0.5, 1, 0, 100, ray);
        assertEquals(1.5, ray.distance, 1e-9);
        assertNull(ray.texture);
    }
}
