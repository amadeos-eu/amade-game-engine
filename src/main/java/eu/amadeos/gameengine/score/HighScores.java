package eu.amadeos.gameengine.score;

import eu.amadeos.commons.config.XdgDirectories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The best {@value #SIZE} results of a game, one list ("table") per difficulty or variant. Each table is a
 * UTF-8 text file {@code <table>.scores} in {@code $XDG_DATA_HOME/amadeos/games/<game>/}, one entry per
 * line: {@code value<TAB>date<TAB>name}. Lines that cannot be read are skipped, so a damaged file loses
 * entries but never stops the game.
 *
 * <pre>
 * HighScores scores = HighScores.of(XdgDirectories.system(), "mines", Ranking.LOWER_WINS);
 * if (scores.qualifies("beginner", seconds)) scores.add("beginner", new Score(name, seconds, Instant.now()));
 * </pre>
 */
public final class HighScores {
    /** Entries kept per table. */
    public static final int SIZE = 10;

    private static final Logger LOG = LoggerFactory.getLogger(HighScores.class);
    private static final String SUFFIX = ".scores";

    private final Path directory;
    private final Ranking ranking;

    public HighScores(Path directory, Ranking ranking) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.ranking = Objects.requireNonNull(ranking, "ranking");
    }

    /** The scores of a game in {@code $XDG_DATA_HOME/amadeos/games/<game>}, e.g. {@code ~/.local/share/amadeos/games/mines}. */
    public static HighScores of(XdgDirectories xdg, String game, Ranking ranking) {
        return new HighScores(xdg.dataHome().resolve("amadeos").resolve("games").resolve(checkName(game)), ranking);
    }

    public Path directory() {
        return directory;
    }

    public Ranking ranking() {
        return ranking;
    }

    /** The entries of a table, best first; empty if the game was never won. */
    public List<Score> list(String table) {
        Path file = file(table);
        if (!Files.exists(file)) return List.of();
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Cannot read the high scores {}", file, e);
            return List.of();
        }
        List<Score> scores = new ArrayList<>();
        for (String line : lines) {
            String[] fields = line.split("\t", 3);
            if (fields.length < 3) continue;
            try {
                scores.add(new Score(fields[2], Long.parseLong(fields[0]), Instant.parse(fields[1])));
            } catch (NumberFormatException | DateTimeParseException e) {
                LOG.warn("Skipping a damaged line in {}: {}", file, line);
            }
        }
        scores.sort(ranking.order());
        return scores.size() > SIZE ? List.copyOf(scores.subList(0, SIZE)) : List.copyOf(scores);
    }

    /** True if the value would enter the table. */
    public boolean qualifies(String table, long value) {
        List<Score> scores = list(table);
        return scores.size() < SIZE || ranking.better(value, scores.getLast().value());
    }

    /**
     * Adds an entry and writes the table.
     *
     * @return the place of the new entry, 0 for the best, or -1 if it did not make it into the table
     */
    public int add(String table, Score score) {
        List<Score> scores = new ArrayList<>(list(table));
        scores.add(score);
        scores.sort(ranking.order());
        int place = scores.indexOf(score);
        if (place >= SIZE) return -1;
        write(table, scores.size() > SIZE ? scores.subList(0, SIZE) : scores);
        return place;
    }

    /** Deletes the entries of a table. */
    public void clear(String table) {
        try {
            Files.deleteIfExists(file(table));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete " + file(table), e);
        }
    }

    private void write(String table, List<Score> scores) {
        Path file = file(table);
        StringBuilder text = new StringBuilder();
        for (Score s : scores) {
            text.append(s.value()).append('\t').append(s.date()).append('\t').append(s.name()).append('\n');
        }
        try {
            Files.createDirectories(directory);
            Path temp = Files.createTempFile(directory, table, ".tmp");
            try {
                Files.writeString(temp, text, StandardCharsets.UTF_8);
                try {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException | AccessDeniedException e) {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + file, e);
        }
    }

    private Path file(String table) {
        return directory.resolve(checkName(table) + SUFFIX);
    }

    private static String checkName(String name) {
        if (name == null || !name.matches("[a-z0-9][a-z0-9_-]*")) {
            throw new IllegalArgumentException("Invalid name: " + name);
        }
        return name;
    }
}
