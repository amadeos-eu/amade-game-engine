package eu.amadeos.gameengine.camera;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CameraTest {
    private static final double STEP = 1.0 / 120;

    // --8<-- [start:test-dead-zone]
    @Test
    void theCameraStaysWhileTheTargetMovesInsideTheDeadZone() {
        Camera camera = new Camera(640, 400, 3200, 400).deadZone(64, 80).catchUp(1);
        camera.jumpTo(1000, 200);
        assertEquals(680, camera.x(), 1e-9, "the target in the middle: 1000 - 320");
        camera.follow(1030, 200, STEP);         // 30 to the right: still inside the 32 of the half zone
        assertEquals(680, camera.x(), 1e-9);
        camera.follow(1050, 200, STEP);         // 50: 18 beyond the zone, and the camera follows by 18
        assertEquals(698, camera.x(), 1e-9);
    }

    @Test
    void theCameraStopsAtTheEdgesOfTheWorld() {
        Camera camera = new Camera(640, 400, 3200, 400).catchUp(1);
        camera.jumpTo(100, 200);
        assertEquals(0, camera.x(), 1e-9, "not left of the world, so the target is left of the middle");
        camera.jumpTo(3150, 200);
        assertEquals(2560, camera.x(), 1e-9, "the right edge of the view at the right edge of the world");
        assertEquals(0, camera.y(), 1e-9, "a world as high as the view never scrolls up or down");
    }
    // --8<-- [end:test-dead-zone]

    // --8<-- [start:test-resize]
    @Test
    void aWiderViewKeepsItsMiddleAndStopsAtTheEdges() {
        Camera camera = new Camera(512, 380, 2048, 1024).catchUp(1);
        camera.jumpTo(1000, 500);
        assertEquals(744, camera.x(), 1e-9);
        camera.resize(720, 380);                // a 16:9 screen: 208 units more map
        assertEquals(640, camera.x(), 1e-9, "the middle stays at 1000");
        assertEquals(720, camera.viewWidth(), 1e-9);
        assertTrue(camera.shows(700, 100), "the new part of the view is on the screen");
        camera.jumpTo(2000, 500);
        assertEquals(1328, camera.x(), 1e-9, "the right edge of the view at the right edge of the world");
        camera.resize(512, 380);
        assertEquals(1328 + 104, camera.x(), 1e-9, "narrower again, at the same middle");
    }
    // --8<-- [end:test-resize]

    // --8<-- [start:test-smoothing]
    @Test
    void smoothingClosesTheSamePartPerSecondAtAnyStepLength() {
        Camera fast = new Camera(640, 400, 10_000, 400).catchUp(0.9);
        Camera slow = new Camera(640, 400, 10_000, 400).catchUp(0.9);
        fast.jumpTo(320, 200);
        slow.jumpTo(320, 200);
        for (int i = 0; i < 120; i++) fast.follow(1320, 200, 1.0 / 120);     // one second in 120 steps
        for (int i = 0; i < 30; i++) slow.follow(1320, 200, 1.0 / 30);       // and in 30
        assertEquals(900, fast.x(), 1e-6, "90 % of the 1000 after one second");
        assertEquals(fast.x(), slow.x(), 1e-6);
    }
    // --8<-- [end:test-smoothing]

    @Test
    void betweenTwoStepsTheCameraIsBetweenItsPositions() {
        Camera camera = new Camera(640, 400, 3200, 800).catchUp(1);
        camera.jumpTo(320, 200);
        camera.follow(340, 600, STEP);
        assertEquals(10, camera.x(0.5), 1e-9, "from 0 to 20, half way");
        assertEquals(200, camera.y(0.5), 1e-9, "from 0 to 400, half way");
    }

    @Test
    void aWorldSmallerThanTheViewIsCentred() {
        Camera camera = new Camera(640, 400, 480, 400);
        camera.jumpTo(400, 200);
        assertEquals(-80, camera.x(), 1e-9);
    }

    @Test
    void onlyWhatTheViewTouchesIsSeen() {
        Camera camera = new Camera(640, 400, 3200, 400);
        camera.jumpTo(1000, 200);
        assertTrue(camera.sees(670, 10, 16, 16, 1));
        assertFalse(camera.sees(1400, 10, 16, 16, 1));
    }

    // --8<-- [start:test-world]
    @Test
    void theMousePointsIntoTheWorldThroughTheCamera() {
        // A map of 800 × 480 below a bar of 22: the view is 640 × 378, from y = 22 on the screen.
        Camera camera = new Camera(640, 378, 800, 480).placedAt(0, 22);
        camera.moveBy(100, 50);
        assertEquals(100, camera.x(), 1e-9);
        assertEquals(415, camera.worldX(315), 1e-9, "the mouse at 315 on the screen, 100 into the world");
        assertEquals(50 + 200 - 22, camera.worldY(200), 1e-9, "the bar is not part of the view");
        assertEquals(315, camera.screenX(415, 1), 1e-9, "and back");
        assertTrue(camera.shows(315, 200));
        assertFalse(camera.shows(315, 10), "on the bar");
    }

    @Test
    void theViewIsMovedByThePlayerAsFarAsTheEdges() {
        Camera camera = new Camera(640, 378, 800, 480);
        camera.moveBy(-30, 0);
        assertEquals(0, camera.x(), 1e-9, "not left of the world");
        camera.moveBy(500, 500);
        assertEquals(160, camera.x(), 1e-9, "800 - 640");
        assertEquals(102, camera.y(), 1e-9, "480 - 378");
        camera.moveBy(0, 0);
        assertEquals(160, camera.x(0), 1e-9, "a step without moving: no drift between the steps");
    }
    // --8<-- [end:test-world]
}
