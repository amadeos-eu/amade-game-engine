package eu.amadeos.gameengine.replay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A whole game as a seed and the player's actions, each with the tick at which the rules received it. When
 * the rules are deterministic (all chance comes from a {@link eu.amadeos.gameengine.random.SeededRandom}
 * with this seed, and time only from ticks), feeding the same actions at the same ticks plays the same game
 * again, to the last point.
 *
 * <p>As text, one line per action after the seed, so a replay can be saved, read and written in a test:
 *
 * <pre>
 * seed 42
 * 3 UP
 * 11 LEFT
 * </pre>
 *
 * @param <A> the game's actions, an enum
 */
// --8<-- [start:replay]
public final class Replay<A extends Enum<A>> {
    /** An action given to the rules before tick number {@code tick} (counted from 0). */
    public record Entry<A>(long tick, A action) {
    }

    private final Class<A> actions;
    private final long seed;
    private final List<Entry<A>> entries = new ArrayList<>();

    public Replay(Class<A> actions, long seed) {
        this.actions = actions;
        this.seed = seed;
    }

    public long seed() {
        return seed;
    }

    public List<Entry<A>> entries() {
        return Collections.unmodifiableList(entries);
    }

    /** Records an action; ticks must not run backwards. */
    public void record(long tick, A action) {
        if (tick < 0 || (!entries.isEmpty() && tick < entries.getLast().tick())) {
            throw new IllegalArgumentException("tick " + tick + " before the last recorded one");
        }
        entries.add(new Entry<>(tick, action));
    }

    /** Plays the actions back in order: ask it before each tick. */
    public Playback playback() {
        return new Playback();
    }

    public final class Playback {
        private int next;

        private Playback() {
        }

        /** The actions recorded for tick {@code tick}, in their order; ticks must be asked in order. */
        public List<A> actionsAt(long tick) {
            List<A> due = new ArrayList<>(1);
            while (next < entries.size() && entries.get(next).tick() <= tick) {
                due.add(entries.get(next++).action());
            }
            return due;
        }

        /** True when every recorded action has been played. */
        public boolean finished() {
            return next >= entries.size();
        }
    }
    // --8<-- [end:replay]

    // --8<-- [start:text]
    public String toText() {
        StringBuilder text = new StringBuilder("seed ").append(seed).append('\n');
        for (Entry<A> entry : entries) text.append(entry.tick()).append(' ').append(entry.action().name()).append('\n');
        return text.toString();
    }

    /** Reads {@link #toText()}; empty lines and lines starting with {@code #} are ignored. */
    public static <A extends Enum<A>> Replay<A> parse(Class<A> actions, String text) {
        Replay<A> replay = null;
        int number = 0;
        for (String line : text.split("\\R")) {
            number++;
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("\\s+");
            try {
                if (replay == null) {
                    if (parts.length != 2 || !parts[0].equals("seed")) throw new IllegalArgumentException("no seed");
                    replay = new Replay<>(actions, Long.parseLong(parts[1]));
                } else {
                    if (parts.length != 2) throw new IllegalArgumentException("expected: tick action");
                    replay.record(Long.parseLong(parts[0]), Enum.valueOf(actions, parts[1]));
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("line " + number + ": " + e.getMessage(), e);
            }
        }
        if (replay == null) throw new IllegalArgumentException("no seed");
        return replay;
    }
    // --8<-- [end:text]

    @Override
    public boolean equals(Object o) {
        return o instanceof Replay<?> other && actions == other.actions && seed == other.seed
                && entries.equals(other.entries);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(seed) * 31 + entries.hashCode();
    }

    @Override
    public String toString() {
        return "Replay[seed " + seed + ", " + entries.size() + " actions]";
    }
}
