package eu.amadeos.gameengine.adventure;

import eu.amadeos.gameengine.math.Vector;
import eu.amadeos.gameengine.tile.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActorTest {
    @Test
    void walksTheWayAtItsSpeedAndThenDoesWhatWasAsked() {
        Actor maria = new Actor("maria", new Vector(0, 0), Direction.DOWN, 50);
        AtomicInteger arrived = new AtomicInteger();
        maria.walk(List.of(new Vector(0, 0), new Vector(100, 0), new Vector(100, 100)), arrived::incrementAndGet);
        maria.update(1, DepthScale.FLAT);
        assertEquals(new Vector(50, 0), maria.position());
        assertEquals(Direction.RIGHT, maria.facing());
        maria.update(1.5, DepthScale.FLAT);
        assertEquals(new Vector(100, 25), maria.position(), "round the corner within one step");
        assertEquals(Direction.DOWN, maria.facing());
        assertTrue(maria.isWalking());
        maria.update(10, DepthScale.FLAT);
        assertFalse(maria.isWalking());
        assertEquals(1, arrived.get());
        assertEquals(200, maria.stride(), 1e-9);
    }

    // --8<-- [start:test-depth]
    @Test
    void farAwayTheSameStepsCoverFewerPixels() {
        DepthScale depth = new DepthScale(100, 0.5, 300, 1.0);
        assertEquals(0.75, depth.at(200), 1e-9);
        assertEquals(0.5, depth.at(0), 1e-9, "beyond the back stays as small as at the back");
        Actor far = new Actor("far", new Vector(0, 100), Direction.RIGHT, 40);
        Actor near = new Actor("near", new Vector(0, 300), Direction.RIGHT, 40);
        far.walk(List.of(new Vector(500, 100)), null);
        near.walk(List.of(new Vector(500, 300)), null);
        far.update(1, depth);
        near.update(1, depth);
        assertEquals(20, far.position().x(), 1e-9);
        assertEquals(40, near.position().x(), 1e-9);
        assertEquals(far.stride(), near.stride(), 1e-9, "the legs move alike");
    }
    // --8<-- [end:test-depth]

    @Test
    void aSlantedWayShowsTheSideUntilItRunsMostlyUpOrDown() {
        assertEquals(Direction.LEFT, Actor.facingFor(new Vector(-10, -15)));
        assertEquals(Direction.UP, Actor.facingFor(new Vector(-10, -25)));
        assertEquals(Direction.DOWN, Actor.facingFor(new Vector(3, 40)));
    }

    @Test
    void placingOrANewWalkCancelsWhatWasToHappen() {
        Actor maria = new Actor("maria", new Vector(0, 0), Direction.DOWN, 50);
        AtomicInteger arrived = new AtomicInteger();
        maria.walk(List.of(new Vector(100, 0)), arrived::incrementAndGet);
        maria.place(new Vector(5, 5), Direction.UP);
        maria.update(10, DepthScale.FLAT);
        assertEquals(0, arrived.get());
        maria.walk(List.of(), arrived::incrementAndGet);
        assertEquals(1, arrived.get(), "an empty way arrives at once");
    }
}
