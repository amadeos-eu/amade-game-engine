package eu.amadeos.gameengine.score;

import java.time.Instant;
import java.util.Objects;

/**
 * One entry of a high-score list.
 *
 * @param name  the player's name as typed, without tabs or line breaks
 * @param value points, seconds or moves; what it means and whether less is better is up to the game
 * @param date  when the game was finished
 */
public record Score(String name, long value, Instant date) {
    /** Longest name that is kept; longer names are cut. */
    public static final int MAX_NAME = 32;

    public Score {
        Objects.requireNonNull(date, "date");
        name = clean(name);
    }

    /** The name as it can be stored in one line of a scores file. */
    public static String clean(String name) {
        String cleaned = name == null ? "" : name.replaceAll("[\\t\\r\\n]+", " ").strip();
        return cleaned.length() > MAX_NAME ? cleaned.substring(0, MAX_NAME).strip() : cleaned;
    }
}
