package eu.amadeos.gameengine.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * A thing in the game, made of <i>components</i>: a body that is somewhere, a flight along a path, a place
 * in a formation, hit points. The entity itself is little more than a number and a bag; what it is and what
 * it does follows from the components it has right now. An insect that leaves the formation to dive gets a
 * flight, and loses it again when it is back. Its class never changes, its components do.
 *
 * <p>A component is any object, usually a small mutable class of the game, and is looked up by its class:
 * an entity holds at most one component of each class.
 */
// --8<-- [start:entity]
public final class Entity {
    private final long id;
    private final Map<Class<?>, Object> components = new HashMap<>();
    private boolean alive = true;

    Entity(long id) {
        this.id = id;
    }

    /** Adds or replaces the component of the component's class. */
    public <T> Entity add(T component) {
        components.put(component.getClass(), component);
        return this;
    }

    /** The component of this class, or {@code null}. */
    public <T> T get(Class<T> type) {
        return type.cast(components.get(type));
    }

    public boolean has(Class<?> type) {
        return components.containsKey(type);
    }

    /** Takes the component of this class away; returns it, or {@code null} if there was none. */
    public <T> T remove(Class<T> type) {
        return type.cast(components.remove(type));
    }

    /** Marks the entity for removal; it leaves its {@link Entities} at their next {@link Entities#flush()}. */
    public void destroy() {
        alive = false;
    }
    // --8<-- [end:entity]

    public boolean alive() {
        return alive;
    }

    /** Numbered in the order the entities were made, from 1. */
    public long id() {
        return id;
    }

    @Override
    public String toString() {
        return "Entity " + id + " " + components.keySet().stream().map(Class::getSimpleName).sorted().toList();
    }
}
