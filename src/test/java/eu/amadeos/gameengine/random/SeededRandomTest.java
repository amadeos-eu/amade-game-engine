package eu.amadeos.gameengine.random;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeededRandomTest {
    // --8<-- [start:test-seeded-random]
    /** The published first numbers of SplitMix64 for seed 0: if these change, every replay breaks. */
    @Test
    void theNumbersNeverChange() {
        SeededRandom random = new SeededRandom(0);
        assertEquals(0xE220A8397B1DCDAFL, random.nextLong());
        assertEquals(0x6E789E6AA1B965F4L, random.nextLong());
        assertEquals(0x06C45D188009454FL, random.nextLong());
    }

    @Test
    void theSameSeedGivesTheSameNumbers() {
        SeededRandom a = new SeededRandom(2026);
        SeededRandom b = new SeededRandom(2026);
        int[] fromA = IntStream.range(0, 100).map(i -> a.nextInt(1000)).toArray();
        int[] fromB = IntStream.range(0, 100).map(i -> b.nextInt(1000)).toArray();
        assertArrayEquals(fromA, fromB);
    }
    // --8<-- [end:test-seeded-random]

    // --8<-- [start:test-fork]
    @Test
    void aForkDoesNotDisturbItsParent() {
        SeededRandom quiet = new SeededRandom(7);
        quiet.fork();
        int expected = quiet.nextInt(100);

        SeededRandom busy = new SeededRandom(7);
        SeededRandom looks = busy.fork();
        for (int i = 0; i < 1000; i++) looks.nextDouble();   // a thousand splinters
        assertEquals(expected, busy.nextInt(100), "the effects did not change the rules' next number");
    }
    // --8<-- [end:test-fork]

    @Test
    void aCopyDrawsTheSameFromHere() {
        SeededRandom random = new SeededRandom(99);
        random.nextLong();
        SeededRandom copy = random.copy();
        assertEquals(random.nextLong(), copy.nextLong());
        assertEquals(99, copy.seed());
    }

    @Test
    void boundedNumbersStayInRangeAndCoverIt() {
        SeededRandom random = new SeededRandom(1);
        boolean[] seen = new boolean[6];
        for (int i = 0; i < 1000; i++) {
            int die = random.nextInt(6);
            assertTrue(die >= 0 && die < 6);
            seen[die] = true;
            double d = random.nextDouble();
            assertTrue(d >= 0 && d < 1);
            int r = random.nextInt(-3, 3);
            assertTrue(r >= -3 && r < 3);
        }
        for (boolean s : seen) assertTrue(s);
    }

    @Test
    void differentSeedsDiffer() {
        assertNotEquals(new SeededRandom(1).nextLong(), new SeededRandom(2).nextLong());
        assertFalse(new SeededRandom(3).seed() == new SeededRandom(4).seed());
    }

    @Test
    void theBoundMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new SeededRandom(0).nextInt(0));
        assertThrows(IllegalArgumentException.class, () -> new SeededRandom(0).nextInt(5, 5));
    }
}
