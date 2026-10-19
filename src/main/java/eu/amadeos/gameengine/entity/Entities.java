package eu.amadeos.gameengine.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * All entities of a game, and the questions the rules ask about them: every entity with a body, every one
 * with a body and a flight. Each rule (a <i>system</i>, in the usual words) walks over the entities that
 * have the components it needs and ignores the others: the flight system moves whatever flies, be it an
 * insect, a shot or a ship, and never asks which it is.
 *
 * <p>Entities made or destroyed while a system walks over them would change the list under its feet. So
 * {@link #create()} and {@link Entity#destroy()} only take effect at {@link #flush()}, which the game calls
 * once per step, after all systems ran. Until then, a new entity is not in the list, and a destroyed one is
 * still there but skipped by every walk.
 *
 * <p>The entities stay in the order they were made, so the systems visit them in the same order in every
 * run: with a fixed seed, the game repeats exactly. Use it on one thread only, the game loop's.
 */
// --8<-- [start:entities]
public final class Entities {
    /** What a system does with an entity that has both components. */
    @FunctionalInterface
    public interface Each2<A, B> {
        void accept(Entity entity, A a, B b);
    }

    private final List<Entity> entities = new ArrayList<>();
    private final List<Entity> born = new ArrayList<>();
    private long nextId = 1;

    /** A new entity without components; it joins the others at the next {@link #flush()}. */
    public Entity create() {
        Entity entity = new Entity(nextId++);
        born.add(entity);
        return entity;
    }

    /** Takes in the entities made since the last flush and drops the destroyed ones. */
    public void flush() {
        entities.removeIf(entity -> !entity.alive());
        for (Entity entity : born) {
            if (entity.alive()) entities.add(entity);
        }
        born.clear();
    }

    /** Every living entity with a component of {@code type}, in the order they were made. */
    public <A> void forEach(Class<A> type, BiConsumer<Entity, A> system) {
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            if (!entity.alive()) continue;
            A a = entity.get(type);
            if (a != null) system.accept(entity, a);
        }
    }

    /** Every living entity with components of both types. */
    public <A, B> void forEach(Class<A> typeA, Class<B> typeB, Each2<A, B> system) {
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            if (!entity.alive()) continue;
            A a = entity.get(typeA);
            if (a == null) continue;
            B b = entity.get(typeB);
            if (b != null) system.accept(entity, a, b);
        }
    }
    // --8<-- [end:entities]

    /** How many living entities have a component of {@code type}. */
    public int count(Class<?> type) {
        int count = 0;
        for (Entity entity : entities) {
            if (entity.alive() && entity.has(type)) count++;
        }
        return count;
    }

    /** The living entities with a component of {@code type}, as a new list. */
    public List<Entity> with(Class<?> type) {
        List<Entity> found = new ArrayList<>();
        forEach(type, (entity, component) -> found.add(entity));
        return found;
    }

    /** All entities since the last flush, the destroyed ones among them; read only. */
    public List<Entity> all() {
        return Collections.unmodifiableList(entities);
    }

    /** Removes all entities at once, also those not yet flushed. */
    public void clear() {
        entities.clear();
        born.clear();
    }
}
