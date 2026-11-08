package eu.amadeos.gameengine.drive;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrivingTest {
    // --8<-- [start:test-driving]
    @Test
    void aCurveTakenFastPushesTheCarOutwards() {
        Road road = new Road();
        new RoadBuilder(road, RoadTest.LOOK).section(400, 5, 0);
        Driving d = new Driving();
        Car slow = new Car(TrafficTest.CAR, 30000, 0.5, 2000);
        Car fast = new Car(TrafficTest.CAR, 30000, 0.5, 8800);
        for (int i = 0; i < 60; i++) {
            d.update(slow, road, 1 / 60.0, 0, slow.speed < 2000, false);
            d.update(fast, road, 1 / 60.0, 0, true, false);
        }
        assertTrue(0.5 - fast.x > 4 * (0.5 - slow.x), "fast " + fast.x + ", slow " + slow.x);
        assertTrue(fast.x < 0.5, "a right bend pushes to the left");
    }
    // --8<-- [end:test-driving]

    @Test
    void grassSlowsTheCarAndAWallStopsIt() {
        Road road = new Road();
        new RoadBuilder(road, RoadTest.LOOK).straight(400);
        Driving d = new Driving();
        Car car = new Car(TrafficTest.CAR, 0, 1.6, d.topSpeed);
        Driving.Result r = null;
        for (int i = 0; i < 120; i++) r = d.update(car, road, 1 / 60.0, 0, true, false);
        assertTrue(r.offRoad());
        assertTrue(car.speed < d.topSpeed * d.offRoad * 1.2, "crawls on the grass: " + car.speed);
        for (long i = 0; i < road.end(); i++) road.segment(i).right = Wall.cliff(2.2, 4000, 1, java.awt.Color.RED);
        car.x = 2.0;
        car.speed = 5000;
        r = d.update(car, road, 1 / 60.0, 1, true, false);
        assertTrue(r.hitWall());
        assertEquals(2.2 - car.width / 2, car.x, 1e-9);
    }
}
