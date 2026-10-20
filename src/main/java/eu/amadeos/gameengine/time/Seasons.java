package eu.amadeos.gameengine.time;

/**
 * The year of a game, in seasons of given lengths: fields that grow in summer and stand bare in winter, a
 * mission to be done before the third winter. It counts game seconds, not the clock, so a paused or
 * hastened game has its seasons paused or hastened too.
 *
 * <pre>
 * Seasons year = new Seasons(120, 120, 120, 90);   // spring, summer, autumn, winter, in seconds
 * int passed = year.update(seconds);               // seasons that began in this step, mostly 0
 * if (year.season() == 3) freeze();
 * </pre>
 *
 * <p>Season 0 is the first given, year 0 the first year. {@link #progress()} says how far the current
 * season is, from 0 to 1, for a picture that turns slowly from green to brown.
 */
// --8<-- [start:seasons]
public final class Seasons {
    private final double[] lengths;
    private final double yearLength;
    private double time;

    public Seasons(double... lengths) {
        if (lengths.length == 0) throw new IllegalArgumentException("a year needs one season at least");
        double sum = 0;
        for (double length : lengths) {
            if (length <= 0) throw new IllegalArgumentException("season of " + length + " s");
            sum += length;
        }
        this.lengths = lengths.clone();
        this.yearLength = sum;
    }

    /** Lets {@code seconds} pass; the number of seasons that began meanwhile. */
    public int update(double seconds) {
        if (seconds < 0) throw new IllegalArgumentException("negative time " + seconds);
        long before = count(time);
        time += seconds;
        return (int) (count(time) - before);
    }

    /** The seasons begun since the start, the first not counted. */
    private long count(double at) {
        long years = (long) Math.floor(at / yearLength);
        double rest = at - years * yearLength;
        int season = 0;
        while (season < lengths.length - 1 && rest >= lengths[season]) {
            rest -= lengths[season];
            season++;
        }
        return years * lengths.length + season;
    }

    /** The current season, 0 to the number of seasons less one. */
    public int season() {
        return (int) (count(time) % lengths.length);
    }

    /** The current year, from 0. */
    public int year() {
        return (int) (count(time) / lengths.length);
    }
    // --8<-- [end:seasons]

    /** How far the current season is, 0 at its start to nearly 1 at its end. */
    public double progress() {
        double rest = time - year() * yearLength;
        int season = 0;
        while (season < lengths.length - 1 && rest >= lengths[season]) {
            rest -= lengths[season];
            season++;
        }
        return rest / lengths[season];
    }

    /** Seconds left in the current season. */
    public double left() {
        return (1 - progress()) * lengths[season()];
    }

    public int count() {
        return lengths.length;
    }

    public double length(int season) {
        return lengths[season];
    }

    /** The game seconds since the start: all a saved game needs to keep. */
    public double time() {
        return time;
    }

    public void restore(double time) {
        if (time < 0) throw new IllegalArgumentException("negative time " + time);
        this.time = time;
    }
}
