package eu.amadeos.gameengine.economy;

/**
 * The money of one side, and how much of it can be kept. In Dune II spice brought in by the harvesters
 * fills the refineries and silos; what they cannot hold is lost. A limit to the store makes the player build
 * where the money goes, and spend it, instead of saving up for ever.
 *
 * <p>Whole credits only: a game shows its money as a number, and a number that grows in fractions is a
 * puzzle. {@link Production} pays in fractions over time, and keeps the fractions itself.
 */
// --8<-- [start:credits]
public final class Credits {
    private int amount;
    private int capacity;

    /** {@code amount} to start with; {@code capacity} what can be kept, {@link Integer#MAX_VALUE} for no limit. */
    public Credits(int amount, int capacity) {
        if (amount < 0 || capacity < 0) throw new IllegalArgumentException("negative credits");
        this.capacity = capacity;
        this.amount = amount;
    }

    public int amount() {
        return amount;
    }

    public int capacity() {
        return capacity;
    }

    /**
     * A new limit, when a silo is built or destroyed. Credits above it stay: a silo that burns does not
     * burn the money already spent from it. They only cannot grow until they are below it again.
     */
    public void setCapacity(int capacity) {
        if (capacity < 0) throw new IllegalArgumentException("negative capacity");
        this.capacity = capacity;
    }

    /** Takes {@code price} if there is that much; true if it was paid. */
    public boolean pay(int price) {
        if (price < 0) throw new IllegalArgumentException("negative price " + price);
        if (price > amount) return false;
        amount -= price;
        return true;
    }

    /**
     * Brings in up to {@code income}, as far as the store holds it, and returns what did not fit: the
     * harvester keeps it in its hold, or it is lost.
     */
    public int earn(int income) {
        if (income < 0) throw new IllegalArgumentException("negative income " + income);
        int room = Math.max(0, capacity - amount);
        int taken = Math.min(room, income);
        amount += taken;
        return income - taken;
    }

    /** Gives back what was paid, as for a cancelled order: past the limit, like a refund always is. */
    public void refund(int credits) {
        if (credits < 0) throw new IllegalArgumentException("negative refund " + credits);
        amount += credits;
    }

    /** True if the store is full. */
    public boolean full() {
        return amount >= capacity;
    }
}
// --8<-- [end:credits]
