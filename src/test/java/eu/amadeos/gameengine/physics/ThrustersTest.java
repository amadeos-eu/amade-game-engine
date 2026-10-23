package eu.amadeos.gameengine.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThrustersTest {
    static final double GRAVITY = 300;

    // --8<-- [start:test-thrusters]
    @Test
    void theMainJetBeatsGravityUntilTheTankIsEmpty() {
        Thrusters jets = new Thrusters(1.5 * GRAVITY, 0.5 * GRAVITY, 0.5 * GRAVITY, 2);
        Body cab = new Body(0, 0, 10, 10);
        double fuel = 1;
        for (int i = 0; i < 60; i++) {                   // half a second at 120 steps a second
            fuel -= jets.fire(cab, 0, -1, fuel, 1 / 120.0);
            cab.fall(GRAVITY, 1000, 1 / 120.0);
        }
        assertEquals(-0.5 * GRAVITY * 0.5, cab.vy, 1e-9, "rising at half a g after half a second");
        assertEquals(0, fuel, 1e-9, "two units a second: the tank is empty");
        assertEquals(0, jets.fire(cab, 0, -1, fuel, 1 / 120.0), "nothing left to burn");
    }
    // --8<-- [end:test-thrusters]

    @Test
    void sideJetsSteerAndBurnInProportion() {
        Thrusters jets = new Thrusters(400, 200, 100, 4);
        Body cab = new Body(0, 0, 10, 10);
        double burned = jets.fire(cab, 1, 0, 10, 0.5);
        assertEquals(100, cab.vx, 1e-9);
        assertEquals(1, burned, 1e-9, "half the push of the main jet, half its burn");
        burned = jets.fire(cab, 0, 1, 0.25, 1);
        assertEquals(0.25, burned, 1e-9, "only what is left");
        assertEquals(25, cab.vy, 1e-9, "a quarter of the step");
    }

    @Test
    void dragSlowsTheSameWhateverTheStep() {
        Body a = new Body(0, 0, 1, 1);
        Body b = new Body(0, 0, 1, 1);
        a.vx = b.vx = 100;
        a.drag(0.5, 1);
        for (int i = 0; i < 120; i++) b.drag(0.5, 1 / 120.0);
        assertEquals(50, a.vx, 1e-9);
        assertEquals(a.vx, b.vx, 1e-9);
    }

    @Test
    void aTouchdownIsJudgedByItsSpeed() {
        assertEquals(Touchdown.SOFT, Touchdown.of(0, 30, 40, 90));
        assertEquals(Touchdown.HARD, Touchdown.of(0, 60, 40, 90));
        assertEquals(Touchdown.HARD, Touchdown.of(25, 10, 40, 90), "sliding counts double");
        assertEquals(Touchdown.CRASH, Touchdown.of(0, 120, 40, 90));
        assertTrue(Touchdown.of(0, -50, 40, 90) == Touchdown.SOFT, "rising onto a pad is no fall");
    }
}
