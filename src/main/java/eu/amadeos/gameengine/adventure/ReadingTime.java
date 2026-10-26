package eu.amadeos.gameengine.adventure;

/**
 * How long a spoken line stays on the screen when nobody clicks it away: long enough to read it twice,
 * short enough that a fast reader does not wait. People read some fifteen characters a second on screen;
 * a line gets a second to be noticed plus the time to read it, at least 1.5 seconds and at most 8.
 */
public final class ReadingTime {
    private ReadingTime() {
    }

    public static double of(String text) {
        double seconds = 1.0 + text.strip().length() / 15.0;
        return Math.max(1.5, Math.min(8, seconds));
    }
}
