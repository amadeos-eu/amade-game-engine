package eu.amadeos.gameengine.raycast;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HitscanTest {
    record Bot(double x, double y, double radius) implements Hitscan.Target { }

    private final GridMap map = Maps.of(
            "########",
            "#......#",
            "#...#..#",
            "########");

    @Test
    void theNearestTargetInFrontOfTheWallIsHit() {
        Bot near = new Bot(3, 1.5, 0.3), far = new Bot(5, 1.5, 0.3);
        Hitscan.Hit<Bot> hit = Hitscan.shoot(map, 1.5, 1.5, 0, 20, List.of(far, near));
        assertSame(near, hit.target());
        assertEquals(1.2, hit.distance(), 1e-9);
    }

    @Test
    void aTargetBehindAWallIsSafe() {
        Bot hidden = new Bot(5.5, 2.5, 0.3);
        Hitscan.Hit<Bot> hit = Hitscan.shoot(map, 1.5, 2.5, 0, 20, List.of(hidden));
        assertTrue(hit.wall());
        assertEquals(2.5, hit.distance(), 1e-9);
        assertEquals(4, hit.x(), 1e-9);
    }

    @Test
    void aShotThatPassesBesideATargetMissesIt() {
        Bot aside = new Bot(4, 1.1, 0.2);
        assertTrue(Hitscan.shoot(map, 1.5, 1.6, 0, 20, List.of(aside)).wall());
    }
}
