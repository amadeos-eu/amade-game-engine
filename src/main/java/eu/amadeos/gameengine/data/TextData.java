package eu.amadeos.gameengine.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A game's data as plain text, one thing per line: words separated by spaces, the first word saying what
 * the line is. Everything after {@code #} is a comment, empty lines are skipped. The game decides what the
 * words mean; this class reads them and, above all, says <i>where</i> something is wrong.
 *
 * <pre>
 * # Stage 1: two columns of bees swoop in from the top
 * path swoop-left  from 300 -20  curve 300 160 80 260 160 180
 * stage 1
 * group 0.0 swoop-left bee 1 3  bee 1 4  bee 2 3  bee 2 4
 * </pre>
 *
 * <p>Data in a file instead of in code means that a level designer, or a player, can change the game
 * without a compiler. It also means that errors arrive at run time, typed by a person: so every error names
 * the file, the line and the word, as {@code waves.txt:12: "swoop-lef" is not a path}.
 */
// --8<-- [start:textdata]
public final class TextData {
    /** A line with the words in it, and where it came from. */
    public record Line(String source, int lineNumber, List<String> words) {
        public Line {
            words = List.copyOf(words);
        }

        /** The first word: what the line is. */
        public String keyword() {
            return words.getFirst();
        }

        public int size() {
            return words.size();
        }

        /** Word {@code index}; an error naming the line if there is none. */
        public String word(int index) {
            if (index >= words.size()) throw error("expected " + (index + 1) + " words, found " + words.size());
            return words.get(index);
        }

        /** Word {@code index} as a number, such as {@code 12}, {@code -0.5} or {@code 1e3}. */
        public double number(int index) {
            String word = word(index);
            try {
                return Double.parseDouble(word);
            } catch (NumberFormatException e) {
                throw error('"' + word + "\" is not a number");
            }
        }

        /** Word {@code index} as a whole number. */
        public int integer(int index) {
            String word = word(index);
            try {
                return Integer.parseInt(word);
            } catch (NumberFormatException e) {
                throw error('"' + word + "\" is not a whole number");
            }
        }

        /** An error at this line, to throw: {@code waves.txt:12: message}. */
        public DataException error(String message) {
            return new DataException(source + ":" + lineNumber + ": " + message);
        }
    }

    /** Something in a data file the game cannot use. */
    public static final class DataException extends RuntimeException {
        public DataException(String message) {
            super(message);
        }
    }

    private TextData() {
    }

    /** Reads all lines that say something; {@code source} names the file in error messages. */
    public static List<Line> parse(String source, Reader reader) {
        List<Line> lines = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(reader)) {
            int number = 0;
            for (String text; (text = in.readLine()) != null; ) {
                number++;
                int comment = text.indexOf('#');
                if (comment >= 0) text = text.substring(0, comment);
                text = text.strip();
                if (!text.isEmpty()) lines.add(new Line(source, number, Arrays.asList(text.split("\\s+"))));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + source, e);
        }
        return Collections.unmodifiableList(lines);
    }
    // --8<-- [end:textdata]

    /** Reads a text file among the classes of {@code owner}, e.g. {@code "/data/waves.txt"}, in UTF-8. */
    public static List<Line> load(Class<?> owner, String resource) {
        InputStream in = owner.getResourceAsStream(resource);
        if (in == null) throw new IllegalArgumentException("no data file " + resource);
        String name = resource.substring(resource.lastIndexOf('/') + 1);
        return parse(name, new InputStreamReader(in, StandardCharsets.UTF_8));
    }
}
