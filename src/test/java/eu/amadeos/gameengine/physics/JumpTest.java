package eu.amadeos.gameengine.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JumpTest {
    private static final double STEP = 1.0 / 120;
    private static final double GRAVITY = 1300;
    private static final double MAX_FALL = 420;

    /** Flat ground at row 10, a ledge: ground only left of x = 64. */
    private static final TileCollider LEDGE = new TileCollider((col, row) ->
            row == 10 && (col < 4 || col > 30) ? Solidity.SOLID : Solidity.EMPTY, 16, 16);
    private static final TileCollider FLAT = new TileCollider((col, row) ->
            row == 10 ? Solidity.SOLID : Solidity.EMPTY, 16, 16);

    /** One step of a player: the jump first, then gravity and the move. */
    private static boolean step(Jump jump, Body body, TileCollider world, boolean pressed, boolean held) {
        boolean jumped = jump.update(body, pressed, held, STEP);
        body.fall(GRAVITY, MAX_FALL, STEP);
        world.move(body, STEP, false);
        return jumped;
    }

    private static Body standing(double x) {
        Body body = new Body(x, 160 - 14, 10, 14);
        LEDGE.move(body, STEP, false);
        return body;
    }

    // --8<-- [start:test-height]
    @Test
    void theSpeedForAHeightReachesThatHeight() {
        double height = 4.5 * 16;
        Jump jump = new Jump(Jump.speedFor(height, GRAVITY), 0.08, 0.1, 0.4);
        Body body = standing(20);
        double ground = body.y;
        double highest = ground;
        step(jump, body, FLAT, true, true);
        for (int i = 0; i < 120; i++) {
            step(jump, body, FLAT, false, true);
            highest = Math.min(highest, body.y);
        }
        // The steps lose half a step's way, v · dt / 2: 1.8 units of 72.
        double lost = Jump.speedFor(height, GRAVITY) * STEP / 2;
        assertEquals(height - lost, ground - highest, 0.1);
        assertTrue(body.onGround, "and back on the ground");
    }

    @Test
    void lettingGoEarlyMakesAHop() {
        Jump jump = new Jump(Jump.speedFor(72, GRAVITY), 0.08, 0.1, 0.4);
        Body body = standing(20);
        double ground = body.y;
        double highest = ground;
        step(jump, body, FLAT, true, true);
        for (int i = 0; i < 120; i++) {
            step(jump, body, FLAT, false, i < 6);      // held for 1/20 second
            highest = Math.min(highest, body.y);
        }
        assertTrue(ground - highest < 72 / 2.0, "a hop: " + (ground - highest));
    }
    // --8<-- [end:test-height]

    // --8<-- [start:test-coyote]
    @Test
    void aJumpPressedJustAfterRunningOffALedgeStillJumps() {
        Jump jump = new Jump(400, 0.08, 0.1, 0.4);
        Body body = standing(50);                     // the right edge at 60, the ledge ends at 64
        body.vx = 120;
        int air = 0;
        while (air < 6) {                             // 6 steps, 0.05 s, in the air
            step(jump, body, LEDGE, false, false);
            if (!body.onGround) air++;
            body.vx = 120;
        }
        assertTrue(step(jump, body, LEDGE, true, true), "coyote time: the jump counts");
        assertTrue(body.vy < 0);
    }

    @Test
    void tooLateAfterTheLedgeIsTooLate() {
        Jump jump = new Jump(400, 0.08, 0.1, 0.4);
        Body body = standing(50);
        body.vx = 120;
        int air = 0;
        while (air < 12) {                            // 0.1 s in the air
            step(jump, body, LEDGE, false, false);
            if (!body.onGround) air++;
            body.vx = 120;
        }
        assertFalse(step(jump, body, LEDGE, true, true));
    }

    @Test
    void aPressJustBeforeLandingJumpsOnLanding() {
        Jump jump = new Jump(400, 0.08, 0.1, 0.4);
        Body body = new Body(20, 100, 10, 14);        // falling, 46 units above the ground
        boolean pressed = false;
        boolean jumped = false;
        for (int i = 0; i < 120 && !jumped; i++) {
            // Press once, when the ground is less than 10 units away: a few steps too early.
            boolean press = !pressed && 160 - body.bottom() < 10;
            pressed |= press;
            jumped = step(jump, body, FLAT, press, true);
        }
        assertTrue(pressed);
        assertTrue(jumped, "the buffered press jumps as soon as the feet are down");
    }
    // --8<-- [end:test-coyote]

    @Test
    void oneGroundGivesOneJump() {
        Jump jump = new Jump(400, 0.08, 0.1, 0.4);
        Body body = standing(20);
        assertTrue(step(jump, body, FLAT, true, true));
        assertFalse(step(jump, body, FLAT, true, true), "no second jump in the air, within coyote time");
    }

    @Test
    void aSpringIsNotCutByLettingGo() {
        Jump jump = new Jump(400, 0.08, 0.1, 0.4);
        Body body = standing(20);
        jump.launch(body, 600, false);
        step(jump, body, FLAT, false, false);
        assertEquals(-600 + GRAVITY * STEP, body.vy, 1e-9);
        jump.launch(body, 600, true);
        step(jump, body, FLAT, false, false);
        assertEquals(-600 * 0.4 + GRAVITY * STEP, body.vy, 1e-9, "a bounce is");
    }
}
