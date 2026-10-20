package eu.amadeos.gameengine.save;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A whole game as keys and values, for a {@link SaveFile}. Progress was a handful of numbers; a running
 * battle is a few hundred things, each with a dozen values, in lists: the units, the buildings, the
 * shots in the air, the orders of each side. A snapshot gives them names a save file can hold and a
 * person can read:
 * <pre>
 * credits=1250
 * unit.count=2
 * unit.0.kind=tank
 * unit.0.x=212.5
 * unit.1.kind=harvester
 * </pre>
 * A {@link Writer} writes such names, nested as deep as needed; a {@link Reader} reads them back, and says
 * which key is missing or malformed when a file does not fit. Numbers are written so that they are read back
 * exactly, to the last bit: a battle that goes on after loading must go on exactly as it would have.
 *
 * <pre>
 * Snapshot.Writer w = Snapshot.writer();
 * w.put("credits", credits);
 * for (Unit unit : units) { Snapshot.Writer u = w.add("unit"); u.put("x", unit.x()); }
 * saveFile.store(VERSION, w.values());
 *
 * Snapshot.Reader r = Snapshot.reader(saved.values());
 * int credits = r.integer("credits");
 * for (Snapshot.Reader u : r.list("unit")) units.add(new Unit(u.number("x")));
 * </pre>
 */
// --8<-- [start:snapshot]
public final class Snapshot {
    private Snapshot() {
    }

    public static Writer writer() {
        return new Writer(new TreeMap<>(), "");
    }

    public static Reader reader(Map<String, String> values) {
        return new Reader(values, "");
    }

    /** A key that is missing or does not hold what was asked for. */
    public static final class SnapshotException extends IllegalArgumentException {
        SnapshotException(String message) {
            super(message);
        }
    }

    /** Writes values under a prefix; {@link #add} opens the next element of a list. */
    public static final class Writer {
        private final Map<String, String> values;
        private final String prefix;

        private Writer(Map<String, String> values, String prefix) {
            this.values = values;
            this.prefix = prefix;
        }

        public Writer put(String key, String value) {
            values.put(prefix + key, value);
            return this;
        }

        public Writer put(String key, int value) {
            return put(key, Integer.toString(value));
        }

        public Writer put(String key, long value) {
            return put(key, Long.toString(value));
        }

        /** {@link Double#toString} gives the shortest text that reads back as exactly the same double. */
        public Writer put(String key, double value) {
            return put(key, Double.toString(value));
        }

        public Writer put(String key, boolean value) {
            return put(key, Boolean.toString(value));
        }

        public Writer put(String key, Enum<?> value) {
            return put(key, value.name());
        }

        /** The values of one thing under {@code key}, such as {@code base.x}, without a list around it. */
        public Writer child(String key) {
            return new Writer(values, prefix + key + ".");
        }

        /** The next element of the list {@code key}: {@code key.0.}, {@code key.1.} and so on, and a count. */
        public Writer add(String key) {
            String count = prefix + key + ".count";
            int index = Integer.parseInt(values.getOrDefault(count, "0"));
            values.put(count, Integer.toString(index + 1));
            return new Writer(values, prefix + key + "." + index + ".");
        }

        /** An empty list, so that reading it gives no elements rather than a missing key. */
        public Writer list(String key) {
            values.putIfAbsent(prefix + key + ".count", "0");
            return this;
        }

        /** Everything written, sorted by key. */
        public Map<String, String> values() {
            return Collections.unmodifiableMap(values);
        }
    }

    /** Reads what a {@link Writer} wrote, under a prefix. */
    public static final class Reader {
        private final Map<String, String> values;
        private final String prefix;

        private Reader(Map<String, String> values, String prefix) {
            this.values = values;
            this.prefix = prefix;
        }

        public boolean has(String key) {
            return values.containsKey(prefix + key);
        }

        public String text(String key) {
            String value = values.get(prefix + key);
            if (value == null) throw new SnapshotException("missing " + prefix + key);
            return value;
        }

        public String text(String key, String fallback) {
            return values.getOrDefault(prefix + key, fallback);
        }

        public int integer(String key) {
            try {
                return Integer.parseInt(text(key));
            } catch (NumberFormatException e) {
                throw new SnapshotException(prefix + key + " is not a whole number: " + values.get(prefix + key));
            }
        }

        public long whole(String key) {
            try {
                return Long.parseLong(text(key));
            } catch (NumberFormatException e) {
                throw new SnapshotException(prefix + key + " is not a whole number: " + values.get(prefix + key));
            }
        }

        public double number(String key) {
            try {
                return Double.parseDouble(text(key));
            } catch (NumberFormatException e) {
                throw new SnapshotException(prefix + key + " is not a number: " + values.get(prefix + key));
            }
        }

        public boolean bool(String key) {
            String value = text(key);
            if (!value.equals("true") && !value.equals("false")) {
                throw new SnapshotException(prefix + key + " is neither true nor false: " + value);
            }
            return value.equals("true");
        }

        public <E extends Enum<E>> E choice(String key, Class<E> type) {
            String value = text(key);
            try {
                return Enum.valueOf(type, value);
            } catch (IllegalArgumentException e) {
                throw new SnapshotException(prefix + key + " is no " + type.getSimpleName() + ": " + value);
            }
        }

        public Reader child(String key) {
            return new Reader(values, prefix + key + ".");
        }

        /** The elements of the list {@code key}, in the order they were added. */
        public List<Reader> list(String key) {
            int count = integer(key + ".count");
            List<Reader> list = new ArrayList<>(count);
            for (int i = 0; i < count; i++) list.add(new Reader(values, prefix + key + "." + i + "."));
            return list;
        }
    }
}
// --8<-- [end:snapshot]
