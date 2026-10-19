package eu.amadeos.gameengine.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileColliderTest {
    private static final double STEP = 1.0 / 120;

    /** A world of 16 × 16 tiles from rows of letters: X solid, = platform; outside, walls at the sides. */
    static TileCollider world(String... rows) {
        return new TileCollider((col, row) -> {
            if (col < 0 || col >= rows[0].length()) return Solidity.SOLID;
            if (row < 0 || row >= rows.length) return Solidity.EMPTY;
            return switch (rows[row].charAt(col)) {
                case 'X' -> Solidity.SOLID;
                case '=' -> Solidity.PLATFORM;
                default -> Solidity.EMPTY;
            };
        }, 16, 16);
    }

    // --8<-- [start:test-land]
    @Test
    void aFallingBodyLandsOnTheGroundExactly() {
        TileCollider world = world(
                "......",
                "......",
                "......",
                "XXXXXX");
        Body body = new Body(20, 0, 10, 14);
        for (int i = 0; i < 120 && !body.onGround; i++) {
            body.fall(1300, 420, STEP);
            world.move(body, STEP, false);
        }
        assertTrue(body.onGround);
        assertEquals(48 - 14, body.y, 1e-9, "the feet on top of row 3");
        assertEquals(0, body.vy, 1e-9);
    }

    @Test
    void evenAVeryFastBodyCannotFallThroughAFloor() {
        TileCollider world = world(
                "......",
                "......",
                "XXXXXX",
                "......",
                "......");
        Body body = new Body(20, 0, 10, 14);
        body.vy = 30_000;                       // 250 units, almost 16 tiles, in one step
        world.move(body, STEP, false);
        assertTrue(body.onGround);
        assertEquals(32 - 14, body.y, 1e-9);
    }
    // --8<-- [end:test-land]

    // --8<-- [start:test-axes]
    @Test
    void sidewaysFirstThenDownAWallStopsTheRunButNotTheFall() {
        TileCollider world = world(
                "...X..",
                "...X..",
                "...X..",
                "XXXXXX");
        Body body = new Body(30, 10, 10, 14);
        body.vx = 2400;                         // 20 units in this step, 12 of them free
        body.vy = 300;
        world.move(body, STEP, false);
        assertTrue(body.hitRight);
        assertEquals(48 - 10, body.x, 1e-9, "against the wall");
        assertEquals(0, body.vx, 1e-9);
        assertEquals(12.5, body.y, 1e-9, "and still falling along it");
        assertFalse(body.onGround);
    }
    // --8<-- [end:test-axes]

    @Test
    void aCeilingStopsTheRise() {
        TileCollider world = world(
                "XXXXXX",
                "......",
                "......",
                "XXXXXX");
        Body body = new Body(20, 30, 10, 14);
        body.vy = -2000;
        world.move(body, STEP, false);
        assertTrue(body.hitCeiling);
        assertEquals(16, body.y, 1e-9);
        assertEquals(0, body.vy, 1e-9);
    }

    // --8<-- [start:test-platform]
    @Test
    void aPlatformHoldsFromAboveAndLetsThroughFromBelow() {
        TileCollider world = world(
                "......",
                "......",
                "==....",
                "......",
                "XXXXXX");
        Body body = new Body(8, 50, 10, 14);    // under the platform, on the ground
        body.vy = -600;
        for (int i = 0; i < 8; i++) world.move(body, STEP, false);
        assertFalse(body.hitCeiling, "through the platform from below");
        assertEquals(10, body.y, 1e-9, "the feet above it now");
        body.vy = 300;                          // now down again: the feet come from above
        for (int i = 0; i < 6 && !body.onGround; i++) world.move(body, STEP, false);
        assertTrue(body.onGround);
        assertTrue(body.onPlatform);
        assertEquals(32 - 14, body.y, 1e-9);
    }

    @Test
    void holdingDownDropsThroughAPlatform() {
        TileCollider world = world(
                "......",
                "==....",
                "......",
                "XXXXXX");
        Body body = new Body(8, 2, 10, 14);     // standing on the platform
        body.vy = 0;
        world.move(body, STEP, false);
        assertTrue(body.onGround, "standing still on it");
        body.vy = 100;
        world.move(body, STEP, true);
        assertFalse(body.onGround);
        assertTrue(body.y > 2);
    }
    // --8<-- [end:test-platform]

    @Test
    void theCellsABodyOverlapsAreVisited() {
        TileCollider world = world("......", "......", "......");
        Body body = new Body(10, 10, 10, 14);   // from (10, 10) to (20, 24): columns 0 and 1, rows 0 and 1
        StringBuilder cells = new StringBuilder();
        world.forEachCell(body, (col, row) -> cells.append(col).append(row).append(' '));
        assertEquals("00 10 01 11 ", cells.toString());
    }
}
