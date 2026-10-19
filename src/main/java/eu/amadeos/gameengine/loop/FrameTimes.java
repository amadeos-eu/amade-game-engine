package eu.amadeos.gameengine.loop;

/**
 * How long the frames take: the numbers behind the frame-time display. Before you make a game faster,
 * measure where the time goes; this is the measuring tape.
 *
 * <p>It keeps the frames of the last second: the time each one took to draw (from the first drawing call
 * to the picture being on the screen), the average and the worst of them, and how many frames there were.
 * At 60 frames per second a frame has 16.7 ms; a frame that takes longer is late and the game stutters.
 *
 * <p>No clock of its own: the caller passes the times in nanoseconds, so tests can feed any sequence.
 * The methods are {@code synchronized}: the loop thread records, the display may read from elsewhere.
 */
// --8<-- [start:frame-times]
public final class FrameTimes {
    private static final long WINDOW = 1_000_000_000L;
    private static final int MAX_FRAMES = 1000;

    private final long[] starts = new long[MAX_FRAMES];
    private final long[] durations = new long[MAX_FRAMES];
    private int first;
    private int size;

    /** One frame: drawing began at {@code start} and ended at {@code end}. */
    public synchronized void record(long start, long end) {
        // Forget frames older than one second, and the oldest if the ring is full.
        while (size > 0 && (start - starts[first] > WINDOW || size == MAX_FRAMES)) {
            first = (first + 1) % MAX_FRAMES;
            size--;
        }
        int i = (first + size) % MAX_FRAMES;
        starts[i] = start;
        durations[i] = end - start;
        size++;
    }

    /** Frames drawn within the last second. */
    public synchronized int framesPerSecond() {
        return size;
    }

    /** Average drawing time of the last second's frames, in milliseconds. */
    public synchronized double averageMillis() {
        if (size == 0) return 0;
        long sum = 0;
        for (int k = 0; k < size; k++) sum += durations[(first + k) % MAX_FRAMES];
        return sum / (double) size / 1e6;
    }

    /** The slowest frame of the last second, in milliseconds. */
    public synchronized double maxMillis() {
        long max = 0;
        for (int k = 0; k < size; k++) max = Math.max(max, durations[(first + k) % MAX_FRAMES]);
        return max / 1e6;
    }
}
// --8<-- [end:frame-times]
