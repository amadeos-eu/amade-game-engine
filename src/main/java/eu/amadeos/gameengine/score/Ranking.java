package eu.amadeos.gameengine.score;

import java.util.Comparator;

/** Whether a smaller or a larger value is the better score; on a tie the older entry stays in front. */
public enum Ranking {
    /** Times and moves: the fastest game wins. */
    LOWER_WINS,
    /** Points. */
    HIGHER_WINS;

    Comparator<Score> order() {
        Comparator<Score> byValue = Comparator.comparingLong(Score::value);
        if (this == HIGHER_WINS) byValue = byValue.reversed();
        return byValue.thenComparing(Score::date);
    }

    /** True if {@code a} is a better value than {@code b}. */
    boolean better(long a, long b) {
        return this == LOWER_WINS ? a < b : a > b;
    }
}
