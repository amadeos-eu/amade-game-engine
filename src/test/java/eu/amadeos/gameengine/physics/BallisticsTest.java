package eu.amadeos.gameengine.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BallisticsTest {
    static final double STEP = 1 / 120.0;
    static final Ballistics.Field FIELD = new Ballistics.Field(0, 2000, 400);

    @Test
    void launchPointsUpForNinetyDegrees() {
        Projectile p = Projectile.launch(0, 0, 90, 100);
        assertEquals(0, p.vx, 1e-9);
        assertEquals(-100, p.vy, 1e-9);
        Projectile q = Projectile.launch(0, 0, 180, 100);
        assertEquals(-100, q.vx, 1e-9);
    }

    @Test
    void withoutAirTheRangeIsWhatTheTextbookSays() {
        Ballistics air = new Ballistics(100, 0, 0, 0.5);
        Ballistics.Flight flight = air.fly(Projectile.launch(0, 300, 45, 200), STEP, 10, FIELD, (x, y) -> y >= 300);
        assertEquals(Ballistics.Ending.HIT, flight.ending());
        assertEquals(200 * 200 / 100.0, flight.x(), 3, "v² sin 2a / g, give or take a step");
    }

    // --8<-- [start:test-ballistics]
    @Test
    void aTriedShotLandsExactlyWhereTheFiredOneLands() {
        Ballistics air = new Ballistics(120, -15, 0, 0.5);
        Ballistics.Solid hill = (x, y) -> y >= 300 - 80 * Math.exp(-Math.pow((x - 300) / 60, 2));
        Projectile shell = Projectile.launch(40, 290, 60, 250);
        Ballistics.Flight tried = air.fly(shell, STEP, 10, FIELD, hill);
        Ballistics.Hit hit = null;
        double seconds = 0;
        while (hit == null) {
            hit = air.step(shell, STEP, hill);
            seconds += STEP;
        }
        assertEquals(tried.x(), hit.x(), 1e-12);
        assertEquals(tried.y(), hit.y(), 1e-12);
        assertEquals(tried.seconds(), seconds, 1e-9);
    }
    // --8<-- [end:test-ballistics]

    @Test
    void aStepLooksAlongTheWayAndStopsAtAThinWall() {
        Ballistics air = new Ballistics(0, 0, 0, 0.5);
        Projectile fast = new Projectile(0, 0, 1200, 0);                 // ten units in a step
        Ballistics.Hit hit = air.step(fast, STEP, (x, y) -> x >= 4 && x < 4.6);
        assertNotNull(hit, "a wall thinner than a step is not jumped over");
        assertEquals(4.5, hit.x(), 0.51);
        assertEquals(hit.x(), fast.x, 1e-12);
        assertNull(air.step(new Projectile(0, 0, 10, 0), STEP, (x, y) -> false));
    }

    @Test
    void windAndAirChangeTheFlight() {
        Ballistics calm = new Ballistics(100, 0, 0, 0.5);
        Ballistics.Solid floor = (x, y) -> y >= 300;
        Projectile shot = Projectile.launch(100, 300, 60, 150);
        double still = calm.fly(shot, STEP, 10, FIELD, floor).x();
        assertTrue(calm.withWind(30).fly(shot, STEP, 10, FIELD, floor).x() > still + 20);
        assertTrue(calm.withWind(-30).fly(shot, STEP, 10, FIELD, floor).x() < still - 20);
        assertTrue(new Ballistics(100, 0, 0.3, 0.5).fly(shot, STEP, 10, FIELD, floor).x() < still - 20);
    }

    @Test
    void aFlightEndsWhenItLeavesTheFieldOrTheTimeIsUp() {
        Ballistics air = new Ballistics(100, 0, 0, 0.5);
        Ballistics.Flight gone = air.fly(Projectile.launch(1990, 300, 10, 200), STEP, 10, FIELD, (x, y) -> false);
        assertEquals(Ballistics.Ending.LEFT_THE_FIELD, gone.ending());
        Ballistics.Flight up = new Ballistics(0, 0, 0, 0.5).fly(Projectile.launch(500, 300, 90, 10), STEP, 1, FIELD, (x, y) -> false);
        assertEquals(Ballistics.Ending.TIME_UP, up.ending());
        assertTrue(new Ballistics.Field(0, 640, 400).contains(320, -500), "above the field it may come back");
    }

    @Test
    void drawingIsBetweenTheLastTwoSteps() {
        Projectile p = new Projectile(0, 0, 120, 0);
        new Ballistics(0, 0, 0, 0.5).step(p, STEP, (x, y) -> false);
        assertEquals(0.5, p.x(0.5), 1e-9);
        assertEquals(0, p.heading(), 1e-9);
    }
}
