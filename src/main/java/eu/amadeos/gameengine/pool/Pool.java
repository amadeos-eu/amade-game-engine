package eu.amadeos.gameengine.pool;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A fixed number of reusable objects, such as bullets: all of them are made at the start, and the game
 * takes one when it needs it and gives it back when it is done, instead of creating and forgetting objects
 * while it runs.
 *
 * <p>Creating an object is cheap in Java; a pool is not about that. It is about two other things. The
 * capacity is a <i>limit</i> the game can rely on, and often a rule: the ship of the old arcade game could
 * have four shots in the air, no more. And a game that creates nothing while it runs gives the garbage
 * collector nothing to do, so it never has to pause the game to clean up.
 *
 * <p>The objects in use are always the first {@link #size()} ones. Giving one back moves the last one in
 * use into its place, as {@link eu.amadeos.gameengine.fx.Particles} does with its rows, so the order
 * changes; walk backwards when giving back while walking.
 *
 * <pre>
 * Pool&lt;Bullet&gt; bullets = new Pool&lt;&gt;(4, Bullet::new);
 * Bullet b = bullets.obtain();                 // null when all four fly
 * if (b != null) b.launch(nose, velocity);
 * for (int i = 0; i &lt; bullets.size(); i++) bullets.get(i).move(seconds);
 * bullets.releaseIf(Bullet::expired);
 * </pre>
 *
 * <p>Use a pool on one thread only, the game loop's.
 *
 * @param <T> the objects, usually mutable, with a method that makes them like new
 */
// --8<-- [start:pool]
public final class Pool<T> {
    private final Object[] items;
    private int size;

    /** Makes all {@code capacity} objects at once with {@code factory}. */
    public Pool(int capacity, Supplier<? extends T> factory) {
        if (capacity < 1) throw new IllegalArgumentException("capacity " + capacity);
        items = new Object[capacity];
        for (int i = 0; i < capacity; i++) items[i] = factory.get();
    }

    /** Gives all objects back, e.g. for a new game. */
    public void clear() {
        size = 0;
    }

    /** The objects in use. */
    public int size() {
        return size;
    }

    public int capacity() {
        return items.length;
    }

    public boolean isFull() {
        return size == items.length;
    }

    /** A free object, now in use, as the last one; {@code null} if all are in use. */
    @SuppressWarnings("unchecked")
    public T obtain() {
        return size == items.length ? null : (T) items[size++];
    }

    /** The object in use at {@code index}, 0 to {@link #size()} - 1. */
    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
        return (T) items[index];
    }

    /** Gives the object at {@code index} back; the last object in use takes its place. */
    public void release(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
        size--;
        Object released = items[index];
        items[index] = items[size];
        items[size] = released;     // kept for the next obtain: the pool never forgets an object
    }

    /** Gives back every object in use that {@code done} says is done; returns how many. */
    public int releaseIf(Predicate<? super T> done) {
        int released = 0;
        for (int i = size - 1; i >= 0; i--) {
            if (done.test(get(i))) {
                release(i);
                released++;
            }
        }
        return released;
    }
}
// --8<-- [end:pool]
