package eu.amadeos.gameengine.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnglesTest {
    private static final double EPSILON = 1e-9;

    private static double degrees(double d) {
        return Math.toRadians(d);
    }

    // --8<-- [start:test-angles]
    @Test
    void theShortWayRoundCrossesTheBackOfTheCircle() {
        assertEquals(degrees(20), Angles.difference(degrees(170), degrees(-170)), EPSILON);
        assertEquals(degrees(-20), Angles.difference(degrees(-170), degrees(170)), EPSILON);
        assertEquals(degrees(90), Angles.difference(degrees(720), degrees(90)), EPSILON);
    }

    @Test
    void turningTowardsATargetStopsOnIt() {
        assertEquals(degrees(10), Angles.turnTowards(0, degrees(90), degrees(10)), EPSILON);
        assertEquals(degrees(-10), Angles.turnTowards(0, degrees(-90), degrees(10)), EPSILON);
        assertEquals(degrees(5), Angles.turnTowards(0, degrees(5), degrees(10)), EPSILON, "no overshooting");
    }
    // --8<-- [end:test-angles]

    @Test
    void normalizeKeepsTheDirection() {
        assertEquals(Math.PI, Angles.normalize(-Math.PI), EPSILON);
        assertEquals(degrees(-90), Angles.normalize(degrees(270)), EPSILON);
        assertEquals(degrees(10), Angles.normalize(degrees(10 - 3 * 360)), EPSILON);
    }
}
