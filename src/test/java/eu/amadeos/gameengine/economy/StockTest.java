package eu.amadeos.gameengine.economy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockTest {
    enum Good { GRAIN, FLOUR, BREAD, FISH, CLOTH }

    // --8<-- [start:test-stock]
    @Test
    void aStockKeepsGoodsUpToItsLimits() {
        Stock<Good> mill = new Stock<>(Good.class);
        mill.limit(Good.GRAIN, 4);
        assertEquals(2, mill.add(Good.GRAIN, 6), "two did not fit");
        assertEquals(0, mill.room(Good.GRAIN));
        assertTrue(mill.take(Good.GRAIN, 3));
        assertFalse(mill.take(Good.GRAIN, 3));
        assertEquals(1, mill.count(Good.GRAIN));
        assertEquals(Integer.MAX_VALUE, mill.limit(Good.FLOUR), "no limit unless set");
        assertFalse(mill.pay(Map.of(Good.GRAIN, 1, Good.FLOUR, 1)));
        assertEquals(1, mill.count(Good.GRAIN), "all or nothing");
    }
    // --8<-- [end:test-stock]

    // --8<-- [start:test-recipe]
    @Test
    void aRecipeTakesOneOfEachInputAndPrefersTheFirstAlternative() {
        Recipe<Good> settler = new Recipe<>(List.of(Set.of(Good.FISH, Good.BREAD), Set.of(Good.CLOTH)), null, 30);
        Stock<Good> home = new Stock<>(Good.class);
        home.add(Good.FISH, 1);
        assertFalse(settler.ready(home), "no cloth");
        home.add(Good.CLOTH, 1);
        home.add(Good.BREAD, 1);
        assertEquals(List.of(Good.BREAD, Good.CLOTH), settler.take(home), "bread comes first in the enum");
        assertEquals(1, home.count(Good.FISH));
        assertNull(settler.take(home));
        assertEquals(Set.of(Good.BREAD, Good.FISH, Good.CLOTH), settler.needs());
    }
    // --8<-- [end:test-recipe]

    @Test
    void twoInputsOfTheSameGoodNeedTwo() {
        Recipe<Good> bakery = new Recipe<>(List.of(Set.of(Good.FLOUR), Set.of(Good.FLOUR)), Good.BREAD, 10);
        Stock<Good> stock = new Stock<>(Good.class);
        stock.add(Good.FLOUR, 1);
        assertFalse(bakery.ready(stock));
        stock.add(Good.FLOUR, 1);
        assertEquals(List.of(Good.FLOUR, Good.FLOUR), bakery.take(stock));
        assertEquals(0, stock.total());
    }
}
