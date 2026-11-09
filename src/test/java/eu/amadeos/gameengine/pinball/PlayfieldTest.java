package eu.amadeos.gameengine.pinball;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayfieldTest {
    private static final double STEP = 1.0 / 120;

    private final List<String> heard = new ArrayList<>();

    private Playfield field() {
        Playfield f = new Playfield(232, 392, Playfield.STANDARD_GRAVITY, 8, 2400);
        f.listen(new Playfield.Events() {
            @Override
            public void hit(String id, Ball ball, double speed) {
                heard.add("hit " + id);
            }

            @Override
            public void entered(String zone, Ball ball) {
                heard.add("enter " + zone);
            }

            @Override
            public void caught(String hole, Ball ball) {
                heard.add("caught " + hole);
            }

            @Override
            public void rode(String track, Ball ball) {
                heard.add("rode " + track);
            }

            @Override
            public void rolledBack(String track, Ball ball) {
                heard.add("back " + track);
            }

            @Override
            public void drained(Ball ball) {
                heard.add("drained");
            }
        });
        return f;
    }

    private static void run(Playfield f, double seconds) {
        for (double t = 0; t < seconds; t += STEP) f.step(STEP);
    }

    @Test
    void aBallRollsDownTheTableAndDrains() {
        Playfield f = field();
        Ball b = f.add(new Ball(100, 100, 5.5));
        run(f, 1.5);
        assertEquals(Ball.State.LOST, b.state);
        assertTrue(heard.contains("drained"));
    }

    @Test
    void aWallStopsTheBallAndBouncesItBack() {
        Playfield f = field();
        f.add(new Segment("floor", 0, 300, 232, 300, 2, Material.WALL, 0));
        Ball b = f.add(new Ball(100, 100, 5.5));
        run(f, 3);
        assertEquals(Ball.State.FREE, b.state);
        assertEquals(300 - 1 - 5.5, b.y, 0.5, "resting on the wall");
        assertTrue(heard.contains("hit floor"));
    }

    @Test
    void aBallLeaningOnASlopingGuideRollsDownItFasterAndFaster() {
        Playfield f = field();
        // a guide falling 1 in 2, like an inlane guide; the ball is set down on it
        f.add(new Segment("guide", 20, 100, 220, 200, 2, Material.WALL, 0));
        Ball b = f.add(new Ball(40, 110 - 6.6 * Math.sqrt(1.25), 5.5));
        run(f, 0.3);
        double early = Math.hypot(b.vx, b.vy);
        run(f, 0.3);
        double later = Math.hypot(b.vx, b.vy);
        assertTrue(early > 60, "rolling, not creeping: " + early);
        assertTrue(later > early * 1.5, "faster and faster: " + early + " then " + later);
    }

    @Test
    void aFastBallDoesNotPassThroughAThinWall() {
        Playfield f = field();
        f.add(new Segment("wall", 150, 0, 150, 392, 1, Material.WALL, 0));
        Ball b = f.add(new Ball(100, 200, 5.5));
        b.vx = 2400;
        run(f, 0.05);
        assertTrue(b.x < 150, "still left of the wall: " + b.x);
        assertTrue(b.vx < 0, "bounced back");
    }

    @Test
    void aBumperKicksTheBallAway() {
        Playfield f = field();
        f.add(new Disc("pop", 100, 200, 10, Material.BUMPER, 0));
        Ball b = f.add(new Ball(100, 170, 5.5));
        b.vy = 200;
        run(f, 0.15);
        assertTrue(b.vy < -300, "kicked up, faster than it came: " + b.vy);
        assertTrue(heard.contains("hit pop"));
    }

    // --8<-- [start:test-flipper]
    @Test
    void theTipOfARaisedFlipperThrowsHarderThanItsBase() {
        assertTrue(throwSpeed(26) > throwSpeed(8) * 1.5);
    }

    /** Lets a ball fall onto a resting left flipper at this distance from the pivot, flips, returns the speed up the table. */
    private double throwSpeed(double along) {
        Playfield f = field();
        Flipper flipper = f.add(Flipper.of("left", true, 72, 349, 31, Math.toRadians(28), Math.toRadians(56), 0));
        double a = flipper.angle();
        double x = 72 + Math.cos(a) * along, y = 349 + Math.sin(a) * along - 12;
        Ball b = f.add(new Ball(x, y, 5.5));
        run(f, 0.05);
        flipper.hold(true);
        run(f, 0.08);
        return -b.vy;
    }
    // --8<-- [end:test-flipper]

    @Test
    void aFlippedBallReachesTheTopOfTheTable() {
        Playfield f = field();
        Flipper flipper = f.add(Flipper.of("left", true, 72, 349, 31, Math.toRadians(28), Math.toRadians(56), 0));
        Ball b = f.add(new Ball(98, 345, 5.5));
        run(f, 0.03);
        flipper.hold(true);
        double top = b.y;
        for (int i = 0; i < 120; i++) {
            f.step(STEP);
            top = Math.min(top, b.y);
        }
        assertTrue(top < 60, "highest point " + top);
    }

    @Test
    void aHoleCatchesASlowBallButNotAFastOne() {
        Playfield f = field();
        f.add(new Zone("scoop", Zone.Kind.HOLE, 100, 200, 6, 0, 300));
        Ball slow = f.add(new Ball(100, 180, 5.5));
        slow.vy = 100;
        run(f, 0.1);
        assertEquals(Ball.State.HELD, slow.state);
        assertEquals("scoop", slow.holder());
        f.eject(slow, 0, 400);
        assertEquals(Ball.State.FREE, slow.state);
        run(f, 0.1);
        assertNotEquals(Ball.State.HELD, slow.state, "an ejected ball does not fall straight back in");

        Playfield g = field();
        g.add(new Zone("scoop", Zone.Kind.HOLE, 100, 200, 6, 0, 300));
        Ball fast = g.add(new Ball(100, 150, 5.5));
        fast.vy = 900;
        run(g, 0.1);
        assertEquals(Ball.State.FREE, fast.state);
    }

    // --8<-- [start:test-ramp]
    @Test
    void aStrongShotRidesTheRampAndAWeakOneRollsBack() {
        assertEquals("rode ramp", rideRamp(1100));
        assertEquals("back ramp", rideRamp(450));
    }

    private String rideRamp(double speed) {
        heard.clear();
        Playfield f = field();
        f.add(new Track("ramp", 0, 100, 200, 0, 100, 100, 25, 60, 60, 25, 30, 300, 2));
        f.add(new Zone("ramp-entry", Zone.Kind.ENTRY, 100, 205, 6, 0, 100, 0, -1, "ramp"));
        Ball b = f.add(new Ball(100, 230, 5.5));
        b.vy = -speed;
        run(f, 3);
        return heard.stream().filter(s -> s.startsWith("rode") || s.startsWith("back")).findFirst().orElse("none");
    }
    // --8<-- [end:test-ramp]

    @Test
    void aGateLetsTheBallThroughOneWayOnly() {
        Playfield f = field();
        f.add(new Segment("gate", 50, 100, 150, 100, 1, Material.WALL, 0, new double[]{0, -1}));
        Ball up = f.add(new Ball(100, 130, 5.5));
        up.vy = -900;
        run(f, 0.1);
        assertTrue(up.y < 100, "passed going up");
        up.vy = 0;
        run(f, 1);
        assertTrue(up.y < 100, "stopped by the gate coming down: " + up.y);
    }

    @Test
    void aDroppedTargetIsNoLongerThere() {
        Playfield f = field();
        Segment target = f.add(new Segment("drop", 50, 300, 150, 300, 2, Material.TARGET, 0));
        target.down = true;
        Ball b = f.add(new Ball(100, 200, 5.5));
        run(f, 1);
        assertEquals(Ball.State.LOST, b.state);
    }

    @Test
    void ballsBounceOffEachOther() {
        Playfield f = new Playfield(232, 392, 0, 8, 2400);
        Ball a = f.add(new Ball(100, 200, 5.5));
        Ball c = f.add(new Ball(140, 200, 5.5));
        a.vx = 400;
        run(f, 0.2);
        assertTrue(c.vx > 300, "the struck ball took the speed: " + c.vx);
        assertTrue(Math.abs(a.vx) < 60, "the striking one nearly stopped: " + a.vx);
    }

    @Test
    void aBallOnARaisedFloorMeetsOnlyThatFloorsWalls() {
        Playfield f = field();
        f.add(new Segment("upper", 0, 300, 232, 300, 2, Material.WALL, 1));
        Ball b = f.add(new Ball(100, 200, 5.5));
        run(f, 1);
        assertEquals(Ball.State.LOST, b.state, "the floor-1 wall is not there for a ball on floor 0");
    }
}
