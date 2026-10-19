package eu.amadeos.gameengine.tiled;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The custom properties of a map, a layer, an object or a tile, as they are set in Tiled's property panel:
 * names and values, such as {@code name = Meadow} or {@code speed = 40}. Tiled knows types (string, int,
 * float, bool, color, file); here every value is text, read in the type the game expects. A missing
 * property gives the fallback, so a map made before a property existed still works.
 */
public record TiledProperties(Map<String, String> values) {
    public static final TiledProperties NONE = new TiledProperties(Map.of());

    public TiledProperties {
        values = Map.copyOf(values);
    }

    public boolean has(String name) {
        return values.containsKey(name);
    }

    public String string(String name, String fallback) {
        return values.getOrDefault(name, fallback);
    }

    /** A number property; the fallback if it is missing. Not a number: an error naming the property. */
    public double number(String name, double fallback) {
        String value = values.get(name);
        if (value == null) return fallback;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("the property " + name + " is \"" + value + "\", not a number");
        }
    }

    public int integer(String name, int fallback) {
        double value = number(name, fallback);
        if (value != Math.rint(value)) {
            throw new IllegalArgumentException("the property " + name + " is " + value + ", not a whole number");
        }
        return (int) value;
    }

    /** {@code true}, {@code yes} or {@code 1} for yes, as Tiled writes and people type them. */
    public boolean bool(String name, boolean fallback) {
        String value = values.get(name);
        if (value == null) return fallback;
        return switch (value.strip().toLowerCase(Locale.ROOT)) {
            case "true", "yes", "1" -> true;
            case "false", "no", "0" -> false;
            default -> throw new IllegalArgumentException("the property " + name + " is \"" + value + "\", not true or false");
        };
    }

    public Set<String> names() {
        return values.keySet();
    }
}
