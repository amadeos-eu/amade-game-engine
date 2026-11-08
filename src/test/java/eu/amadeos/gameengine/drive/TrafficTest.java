package eu.amadeos.gameengine.drive;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrafficTest {
    static final RoadSprite CAR = new RoadSprite(new BufferedImage(8, 4, BufferedImage.TYPE_INT_ARGB), 560);

    private static void run(Traffic t, double seconds) {
        for (int i = 0; i < seconds * 60; i++) t.update(1 / 60.0);
    }

    // --8<-- [start:test-follow]
    @Test
    void aCarThatDoesNotOvertakeFollowsTheSlowerOneAhead() {
        Traffic t = new Traffic();
        Car truck = new Car(CAR, 5000, 0.5, 3000);
        Car car = new Car(CAR, 0, 0.5, 6000);
        t.add(truck);
        t.add(car);
        run(t, 20);
        assertTrue(car.z < truck.z - car.length, "never runs into it");
        assertEquals(truck.speed, car.speed, 300);
        assertEquals(0.5, car.x, 1e-9);
    }
    // --8<-- [end:test-follow]

    // --8<-- [start:test-overtake]
    @Test
    void anOvertakingCarPassesWhenTheOtherLaneIsClearAndWaitsWhenItIsNot() {
        Traffic t = new Traffic();
        Car truck = new Car(CAR, 4000, 0.5, 3000);
        Car car = new Car(CAR, 0, 0.5, 6000).overtaking();
        Car oncoming = new Car(CAR, 14000, -0.5, 5000).oncoming();
        t.add(truck);
        t.add(car);
        t.add(oncoming);
        run(t, 1.5);
        assertEquals(0.5, car.x, 0.01, "the oncoming car is in the way: stays in its lane");
        run(t, 20);
        assertTrue(car.z > truck.z + car.length, "has passed once the oncoming car went by");
        assertEquals(0.5, car.x, 0.05, "and is back in its lane");
    }
    // --8<-- [end:test-overtake]

    @Test
    void steeredCarsStayWhereThePlayerPutsThemAndTouchesAreFound() {
        Traffic t = new Traffic();
        Car player = new Car(CAR, 9000, 0.5, 0);
        player.steered = true;
        Car other = new Car(CAR, 0, 0.5, 5000);
        t.add(player);
        t.add(other);
        run(t, 3);
        assertEquals(9000, player.z, 1e-9);
        assertTrue(other.z < player.z, "keeps its distance behind the player");
        assertNull(t.touching(player));
        other.z = player.z + 100;
        assertSame(other, t.touching(player));
        assertEquals(1, t.removeOutside(50000, 1000, 1000), "the far car goes, the player stays");
    }
}
