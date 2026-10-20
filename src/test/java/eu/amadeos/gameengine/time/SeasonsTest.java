package eu.amadeos.gameengine.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeasonsTest {
    // --8<-- [start:test-seasons]
    @Test
    void theYearGoesRoundInSeasonsOfTheirOwnLength() {
        Seasons year = new Seasons(10, 20, 10, 5);
        assertEquals(0, year.update(9.5));
        assertEquals(0, year.season());
        assertEquals(1, year.update(1));
        assertEquals(1, year.season());
        assertEquals(0.025, year.progress(), 1e-9);
        assertEquals(2, year.update(30), "autumn and winter began");
        assertEquals(3, year.season());
        assertEquals(0, year.year());
        assertEquals(0, year.update(4));
        assertEquals(1, year.update(1));
        assertEquals(0, year.season());
        assertEquals(1, year.year(), "spring of the second year");
    }
    // --8<-- [end:test-seasons]

    @Test
    void manySmallStepsCountEverySeasonOnce() {
        Seasons year = new Seasons(1, 1, 1, 1);
        int began = 0;
        for (int i = 0; i < 120 * 10; i++) began += year.update(1.0 / 120);
        assertEquals(10, began, 1, "ten seconds, ten seasons");
        Seasons copy = new Seasons(1, 1, 1, 1);
        copy.restore(year.time());
        assertEquals(year.season(), copy.season());
        assertEquals(year.year(), copy.year());
        assertEquals(year.left(), copy.left(), 1e-12);
    }
}
