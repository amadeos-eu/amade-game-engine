package eu.amadeos.gameengine.path;

import eu.amadeos.gameengine.tile.Cell;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Flow fields towards many goals, each searched once and kept while the map stays as it is. In Towers all
 * enemies had one goal and one field. In a strategy game every order has a goal of its own: this group to
 * the ridge, that one to the crystal, the harvesters home. A group shares one field, and two groups sent to
 * the same place share it too. When the map changes, because a building went up or came down, every field
 * may be wrong: {@link #invalidate} forgets them all, and each is searched again when it is next asked for.
 *
 * <p>Only the {@value #DEFAULT_SIZE} fields used last are kept: a field of a map of 64 × 64 tiles is 20 KB,
 * and a player who clicks all over the map would otherwise fill the memory with fields nobody follows.
 *
 * <pre>
 * FieldCache fields = new FieldCache(map.width(), map.height(), this::open);
 * FlowField way = fields.toward(order.target());
 * // after building or destroying:
 * fields.invalidate();
 * </pre>
 */
// --8<-- [start:cache]
public final class FieldCache {
    public static final int DEFAULT_SIZE = 32;

    private final int width;
    private final int height;
    private final Passable open;
    private final Map<Cell, FlowField> fields;
    private int searches;

    public FieldCache(int width, int height, Passable open) {
        this(width, height, open, DEFAULT_SIZE);
    }

    public FieldCache(int width, int height, Passable open, int size) {
        this.width = width;
        this.height = height;
        this.open = open;
        // An access-ordered map that drops its eldest entry: the least recently used field.
        this.fields = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Cell, FlowField> eldest) {
                return size() > size;
            }
        };
    }

    /** The field towards {@code goal}: from the cache, or searched now. */
    public FlowField toward(Cell goal) {
        FlowField field = fields.get(goal);
        if (field == null) {
            field = FlowField.toward(width, height, open, goal);
            fields.put(goal, field);
            searches++;
        }
        return field;
    }

    /** The map changed: every field is searched again when it is next needed. */
    public void invalidate() {
        fields.clear();
    }
    // --8<-- [end:cache]

    /** How many searches the cache has made: what it saved is the asks it answered without one. */
    public int searches() {
        return searches;
    }

    public int size() {
        return fields.size();
    }
}
