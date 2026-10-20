package eu.amadeos.gameengine.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Things of a game described in a data file, one block each: a line naming the thing, then its properties,
 * one per line. The units of a strategy game, its towers, its weapons; numbers a designer changes again and
 * again while the game is played, without a compiler.
 *
 * <pre>
 * unit orc
 *   health 60
 *   speed 1.1        # tiles per second
 *   bounty 4
 *
 * unit orc-chief like orc
 *   health 180       # everything else as the orc
 * </pre>
 *
 * <p>A block begins with one of the kinds the game names ({@code unit}) and the thing's name; it ends where
 * the next begins. With {@code like}, a block starts with all properties of an earlier one of its kind
 * and changes some. Indenting the properties is only for the eye.
 *
 * <p>The game asks each {@link Entry} for the properties it knows and then calls {@link Entry#finish()}.
 * Whatever the game did not ask for is an error, with the file and the line: a property typed
 * {@code rnage} is not silently ignored while the tower keeps its old range, the classic mistake of every
 * data file that is read without such a check.
 */
// --8<-- [start:catalog]
public final class Catalog {
    /** One thing: its kind, its name, and its properties by name, each with the lines that gave it. */
    public static final class Entry {
        private final String kind;
        private final String name;
        private final TextData.Line header;
        private final Map<String, List<TextData.Line>> properties = new LinkedHashMap<>();
        private final Set<String> asked = new HashSet<>();
        /** Properties taken over with like and not yet given by the block itself. */
        private final Set<String> inherited = new HashSet<>();

        private Entry(String kind, String name, TextData.Line header) {
            this.kind = kind;
            this.name = name;
            this.header = header;
        }

        public String kind() {
            return kind;
        }

        public String name() {
            return name;
        }

        public boolean has(String key) {
            asked.add(key);
            return properties.containsKey(key);
        }

        /** The line of a property given once, such as {@code range 2.5}; an error if it is missing or given twice. */
        public TextData.Line line(String key) {
            List<TextData.Line> lines = lines(key);
            if (lines.isEmpty()) throw error(key + " is missing");
            if (lines.size() > 1) throw lines.get(1).error(key + " is given twice");
            return lines.getFirst();
        }

        /** All lines of a property that may be given several times, such as the levels of a tower. */
        public List<TextData.Line> lines(String key) {
            asked.add(key);
            return properties.getOrDefault(key, List.of());
        }

        /** A property of one number: {@code health 60}. */
        public double number(String key) {
            TextData.Line line = line(key);
            if (line.size() != 2) throw line.error(key + " takes one number");
            return line.number(1);
        }

        public double number(String key, double fallback) {
            return has(key) ? number(key) : fallback;
        }

        public int integer(String key) {
            TextData.Line line = line(key);
            if (line.size() != 2) throw line.error(key + " takes one whole number");
            return line.integer(1);
        }

        public int integer(String key, int fallback) {
            return has(key) ? integer(key) : fallback;
        }

        /** A property of one word: {@code shot arrow}. */
        public String text(String key) {
            TextData.Line line = line(key);
            if (line.size() != 2) throw line.error(key + " takes one word");
            return line.word(1);
        }

        public String text(String key, String fallback) {
            return has(key) ? text(key) : fallback;
        }

        /** An error at the line that names this thing. */
        public TextData.DataException error(String message) {
            return header.error(kind + " " + name + ": " + message);
        }

        /** Throws for the first property the game did not ask for: a typo, or something no longer used. */
        public void finish() {
            for (Map.Entry<String, List<TextData.Line>> property : properties.entrySet()) {
                if (!asked.contains(property.getKey())) {
                    throw property.getValue().getFirst().error(
                            '"' + property.getKey() + "\" is not a property of a " + kind);
                }
            }
        }
    }
    // --8<-- [end:catalog]

    private Catalog() {
    }

    // --8<-- [start:read]
    /**
     * Reads the blocks of the {@code kinds} given, in the order of the file. Names are unique within a
     * kind; a line before the first block is an error.
     */
    public static List<Entry> read(List<TextData.Line> lines, String... kinds) {
        Set<String> known = Set.of(kinds);
        List<Entry> entries = new ArrayList<>();
        Map<String, Entry> byName = new LinkedHashMap<>();
        Entry current = null;
        for (TextData.Line line : lines) {
            String keyword = line.keyword();
            if (known.contains(keyword)) {
                current = header(line, byName);
                entries.add(current);
                continue;
            }
            if (current == null) {
                throw line.error("expected " + String.join(" or ", kinds) + " before \"" + keyword + '"');
            }
            if (current.properties.containsKey(keyword) && current.inherited.contains(keyword)) {
                // The block's own line replaces what it took over with like.
                current.properties.remove(keyword);
                current.inherited.remove(keyword);
            }
            current.properties.computeIfAbsent(keyword, key -> new ArrayList<>()).add(line);
        }
        return Collections.unmodifiableList(entries);
    }

    private static Entry header(TextData.Line line, Map<String, Entry> byName) {
        String kind = line.keyword();
        String name = line.word(1);
        if (line.size() != 2 && !(line.size() == 4 && line.word(2).equals("like"))) {
            throw line.error("expected \"" + kind + " <name>\" or \"" + kind + " <name> like <other>\"");
        }
        if (byName.containsKey(kind + " " + name)) throw line.error("there is already a " + kind + " " + name);
        Entry entry = new Entry(kind, name, line);
        if (line.size() == 4) {
            Entry model = byName.get(kind + " " + line.word(3));
            if (model == null) throw line.error("no " + kind + " " + line.word(3) + " before this line");
            model.properties.forEach((key, given) -> {
                entry.properties.put(key, new ArrayList<>(given));
                entry.inherited.add(key);
            });
        }
        byName.put(kind + " " + name, entry);
        return entry;
    }
    // --8<-- [end:read]
}
