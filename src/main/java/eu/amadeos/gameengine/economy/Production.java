package eu.amadeos.gameengine.economy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What a building is making: a queue of orders, the first one in the works. As in Dune II the price is not
 * paid at the start but <b>while it is made</b>: a tank of 300 credits that takes 20 seconds costs 15
 * credits a second. Without the money, the work stops and waits; with less power, it goes slower. The player
 * can start an order with nearly no money and watch it grow as the harvesters bring crystal, which keeps the
 * game moving, and can cancel an order and get back what was paid so far.
 *
 * <p>The credits are whole, the payment per step is not: 15 credits a second at 120 steps a second is an
 * eighth of a credit per step. So the queue pays a whole credit as soon as the work reaches it, and the
 * work only grows as far as it has been paid: no credit, no progress.
 *
 * <pre>
 * Production&lt;UnitType&gt; factory = new Production&lt;&gt;();
 * factory.order(tank, tank.cost(), tank.seconds());
 * // in each step, at full power 1, at low power 0.5:
 * UnitType done = factory.update(seconds, credits, power);
 * if (done != null) spawn(done);
 * </pre>
 *
 * @param <T> what is made, such as a kind of unit
 */
// --8<-- [start:production]
public final class Production<T> {
    /** One order: what, its price and how long it takes at full speed. */
    public record Order<T>(T item, int cost, double seconds) {
        public Order {
            if (cost < 0 || seconds <= 0) throw new IllegalArgumentException("cost " + cost + ", seconds " + seconds);
        }
    }

    private final List<Order<T>> queue = new ArrayList<>();
    /** Credits paid for the first order so far, and how far it is, 0 to 1. */
    private int paid;
    private double done;
    private boolean held;
    private T ready;

    /** Adds an order at the end of the queue. */
    public void order(T item, int cost, double seconds) {
        queue.add(new Order<>(item, cost, seconds));
    }

    /**
     * One step: the first order is worked on for {@code seconds} at {@code speed} (1 full, 0.5 half), as
     * far as the credits pay for it. Returns the item finished in this step, or {@code null}.
     */
    public T update(double seconds, Credits credits, double speed) {
        if (queue.isEmpty() || held || ready != null || speed <= 0) return null;
        Order<T> first = queue.getFirst();
        double wanted = done + seconds * speed / first.seconds();
        if (wanted > 1 - 1e-9) wanted = 1;             // what the sum of many small steps misses
        // The credits this much work costs, rounded up: a credit is paid before its work is done.
        int due = (int) Math.ceil(wanted * first.cost() - 1e-9) - paid;
        if (due > 0) {
            int paying = Math.min(due, credits.amount());
            credits.pay(paying);
            paid += paying;
        }
        // The work goes only as far as it is paid for: without money it stands still.
        done = first.cost() == 0 ? wanted : Math.min(wanted, (double) paid / first.cost());
        if (done < 1 || paid < first.cost()) return null;
        queue.removeFirst();
        paid = 0;
        done = 0;
        return first.item();
    }
    // --8<-- [end:production]

    /**
     * Keeps a finished item until it is {@link #take() taken}, instead of handing it out at once: a
     * building that waits to be placed. While one is kept, the queue does not go on.
     */
    public void keep(T item) {
        ready = item;
    }

    /** The finished item that waits, or {@code null}. */
    public T ready() {
        return ready;
    }

    /** Takes the waiting item away, when it has been placed; the queue goes on. */
    public T take() {
        T item = ready;
        ready = null;
        return item;
    }

    // --8<-- [start:cancel]
    /** Stops or goes on with the work, keeping what was paid. */
    public void hold(boolean on) {
        held = on;
    }

    public boolean held() {
        return held;
    }

    /**
     * Removes the last order of {@code item}; if it is the one in the works, what was paid for it so far
     * comes back. True if there was one.
     */
    public boolean cancel(T item, Credits credits) {
        for (int i = queue.size() - 1; i >= 0; i--) {
            if (!queue.get(i).item().equals(item)) continue;
            if (i == 0) {
                credits.refund(paid);
                paid = 0;
                done = 0;
                held = false;
            }
            queue.remove(i);
            return true;
        }
        return false;
    }
    // --8<-- [end:cancel]

    /** How far the first order is, 0 to 1. */
    public double progress() {
        return done;
    }

    /** Credits paid so far for the first order. */
    public int paid() {
        return paid;
    }

    /** The item in the works, or {@code null}. */
    public T current() {
        return queue.isEmpty() ? null : queue.getFirst().item();
    }

    public List<Order<T>> queue() {
        return Collections.unmodifiableList(queue);
    }

    /** How many of {@code item} are ordered, the one in the works included. */
    public int count(T item) {
        int n = 0;
        for (Order<T> order : queue) if (order.item().equals(item)) n++;
        return n;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Takes back the state of the first order, as a save file kept it: what was paid, the work done and
     * whether it is held. The queue itself is rebuilt with {@link #order}, the waiting item with {@link #keep}.
     */
    public void restore(int paid, double done, boolean held) {
        this.paid = paid;
        this.done = done;
        this.held = held;
    }
}
