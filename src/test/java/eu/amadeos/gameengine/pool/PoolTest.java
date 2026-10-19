package eu.amadeos.gameengine.pool;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoolTest {
    /** A bullet as a game would have it: mutable, made like new when launched. */
    static final class Shot {
        double life;
    }

    // --8<-- [start:test-pool]
    @Test
    void theCapacityIsALimitAndObjectsAreReused() {
        int[] made = {0};
        Pool<Shot> shots = new Pool<>(4, () -> {
            made[0]++;
            return new Shot();
        });
        Set<Shot> seen = new HashSet<>();
        for (int i = 0; i < 4; i++) seen.add(shots.obtain());
        assertNull(shots.obtain(), "four in the air, no fifth");
        shots.release(1);
        Shot again = shots.obtain();
        assertTrue(seen.contains(again), "an old object, not a new one");
        assertEquals(4, made[0], "all made at the start, none later");
    }

    @Test
    void releasingWhileWalkingKeepsTheRest() {
        Pool<Shot> shots = new Pool<>(5, Shot::new);
        for (int i = 0; i < 5; i++) shots.obtain().life = i % 2;    // 0, 1, 0, 1, 0
        assertEquals(3, shots.releaseIf(shot -> shot.life == 0));
        assertEquals(2, shots.size());
        for (int i = 0; i < shots.size(); i++) assertEquals(1, shots.get(i).life);
    }
    // --8<-- [end:test-pool]

    @Test
    void theLastInUseMovesIntoTheGap() {
        Pool<Shot> shots = new Pool<>(3, Shot::new);
        Shot a = shots.obtain();
        shots.obtain();
        Shot c = shots.obtain();
        shots.release(0);
        assertSame(c, shots.get(0));
        assertEquals(2, shots.size());
        assertSame(a, shots.obtain(), "the released one is next");
        assertThrows(IndexOutOfBoundsException.class, () -> shots.get(3));
        shots.clear();
        assertEquals(0, shots.size());
        assertEquals(3, shots.capacity());
    }
}
