package eu.amadeos.gameengine.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionTest {
    private static final double STEP = 1.0 / 120;

    /** Runs the queue for {@code seconds}; the item finished meanwhile, or null. */
    private static String run(Production<String> queue, Credits credits, double seconds, double speed) {
        String done = null;
        for (int i = 0; i < Math.round(seconds / STEP); i++) {
            String item = queue.update(STEP, credits, speed);
            if (item != null) done = item;
        }
        return done;
    }

    // --8<-- [start:test-credits]
    @Test
    void theStoreHoldsWhatItCanAndTheRestIsLeftOver() {
        Credits credits = new Credits(900, 1000);
        assertEquals(150, credits.earn(250), "only 100 fit");
        assertTrue(credits.full());
        assertFalse(credits.pay(1200));
        assertTrue(credits.pay(400));
        assertEquals(600, credits.amount());
        credits.setCapacity(500);                       // a silo burned
        assertEquals(600, credits.amount(), "what was there stays");
        assertEquals(50, credits.earn(50), "but cannot grow");
    }
    // --8<-- [end:test-credits]

    // --8<-- [start:test-pay]
    @Test
    void theWorkIsPaidWhileItIsDoneAndStopsWithoutMoney() {
        Credits credits = new Credits(1000, 2000);
        Production<String> factory = new Production<>();
        factory.order("tank", 300, 20);
        assertNull(run(factory, credits, 10, 1));
        assertEquals(0.5, factory.progress(), 1e-6, "half the time");
        assertEquals(150, factory.paid(), "half the price, no more");
        assertEquals(850, credits.amount());
        assertEquals("tank", run(factory, credits, 10, 1));
        assertEquals(700, credits.amount(), "the whole price at the end, not a credit more");

        factory.order("tank", 300, 20);
        Credits poor = new Credits(60, 2000);
        assertNull(run(factory, poor, 20, 1));
        assertEquals(0.2, factory.progress(), 1e-6, "stopped where the money ended");
        assertEquals(0, poor.amount());
        poor.earn(240);                                  // a harvester came home
        assertEquals("tank", run(factory, poor, 16, 1), "and went on from there");
    }
    // --8<-- [end:test-pay]

    // --8<-- [start:test-cancel]
    @Test
    void lowPowerSlowsTheWorkAndCancellingRefundsWhatWasPaid() {
        Credits credits = new Credits(1000, 2000);
        Production<String> yard = new Production<>();
        yard.order("refinery", 400, 10);
        run(yard, credits, 10, 0.5);
        assertEquals(0.5, yard.progress(), 1e-6, "half the power, half the speed");
        assertTrue(yard.cancel("refinery", credits));
        assertEquals(1000, credits.amount(), "all of it back");
        assertTrue(yard.isEmpty());
    }
    // --8<-- [end:test-cancel]

    @Test
    void aBuildingWaitsToBePlacedAndHoldsTheQueue() {
        Credits credits = new Credits(1000, 2000);
        Production<String> yard = new Production<>();
        yard.order("turbine", 300, 5);
        yard.order("silo", 100, 5);
        String done = run(yard, credits, 5, 1);
        assertEquals("turbine", done);
        yard.keep(done);
        assertNull(run(yard, credits, 5, 1));
        assertEquals(0, yard.progress(), "the silo waits for the turbine to be placed");
        assertEquals("turbine", yard.take());
        assertEquals("silo", run(yard, credits, 5, 1));

        yard.order("silo", 100, 5);
        yard.hold(true);
        run(yard, credits, 5, 1);
        assertEquals(0, yard.progress());
        yard.hold(false);
        assertEquals(1, yard.count("silo"));
    }
}
