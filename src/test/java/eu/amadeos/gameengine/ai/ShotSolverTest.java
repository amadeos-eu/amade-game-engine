package eu.amadeos.gameengine.ai;

import eu.amadeos.gameengine.physics.Ballistics;
import eu.amadeos.gameengine.physics.Projectile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShotSolverTest {
    static final double STEP = 1 / 120.0;

    // --8<-- [start:test-solver]
    @Test
    void findsAShotOverAHillAgainstTheWind() {
        Ballistics air = new Ballistics(120, -20, 0, 0.5);
        Ballistics.Field field = new Ballistics.Field(0, 640, 400);
        Ballistics.Solid ground = (x, y) -> y >= 320 - 140 * Math.exp(-Math.pow((x - 320) / 50, 2));
        double targetX = 520;
        ShotSolver.Aim aim = ShotSolver.solve(10, 85, 5, 20, 100, 5, 5, (angle, power) -> {
            Ballistics.Flight flight = air.fly(Projectile.launch(100, 312, angle, power * 3.3), STEP, 8, field, ground);
            return flight.ending() == Ballistics.Ending.HIT ? Math.abs(flight.x() - targetX) : 1000;
        });
        assertTrue(aim.miss() < 2, "lands within two units: " + aim);
        assertTrue(aim.angle() > 45, "a high shot, over the hill: " + aim);
        assertTrue(aim.trials() < 600, "a few hundred trials: " + aim.trials());
    }
    // --8<-- [end:test-solver]

    @Test
    void staysInsideTheRanges() {
        ShotSolver.Aim aim = ShotSolver.solve(10, 20, 5, 50, 60, 5, 4, (a, p) -> Math.abs(a - 90) + Math.abs(p - 100));
        assertEquals(20, aim.angle(), 1e-9);
        assertEquals(60, aim.power(), 1e-9);
    }

    @Test
    void refinesBetweenTheGridPoints() {
        ShotSolver.Aim aim = ShotSolver.solve(0, 90, 10, 0, 100, 10, 6, (a, p) -> Math.hypot(a - 37.3, p - 61.7));
        assertEquals(37.3, aim.angle(), 0.3);
        assertEquals(61.7, aim.power(), 0.3);
    }

    @Test
    void anEmptySearchIsAMistake() {
        assertThrows(IllegalArgumentException.class, () -> ShotSolver.solve(10, 5, 1, 0, 1, 1, 1, (a, p) -> 0));
    }
}
