package eu.amadeos.gameengine.audio;

/**
 * Samples on their way from a decoder to the {@link Mixer}: a ring buffer of a second or two that one thread
 * fills with {@link #write} (waiting while it is full) and the mixer empties with {@link #next()} (never
 * waiting: when the decoder falls behind, the mixer hears silence). The format is that of {@link Sound}:
 * 16-bit mono at {@link Sound#RATE}.
 */
// --8<-- [start:stream]
public final class SampleStream {
    private final short[] ring;
    /** Written and read counts; their difference is what waits in the ring. */
    private volatile long written, read;
    private volatile boolean ended, closed;

    /** A stream that holds up to {@code seconds} of sound ahead of the mixer. */
    public SampleStream(double seconds) {
        ring = new short[(int) Math.max(1024, seconds * Sound.RATE)];
    }

    /**
     * Adds samples, waiting while the ring is full; returns false once the stream is {@linkplain #close()
     * closed}, so the decoder knows to stop.
     */
    public boolean write(short[] samples, int count) throws InterruptedException {
        for (int i = 0; i < count; ) {
            if (closed) return false;
            long room = ring.length - (written - read);
            if (room == 0) {
                Thread.sleep(5);
                continue;
            }
            int n = (int) Math.min(room, count - i);
            for (int k = 0; k < n; k++) ring[(int) ((written + k) % ring.length)] = samples[i + k];
            written += n;
            i += n;
        }
        return !closed;
    }

    /** The decoder has written everything: when the ring is empty, the stream is finished. */
    public void end() {
        ended = true;
    }

    /** The listener wants no more: the decoder's next write returns false, and the mixer drops the stream. */
    public void close() {
        closed = true;
    }

    /** The next sample, or 0 if none has arrived yet; on the mixer thread. */
    int next() {
        if (read >= written) return 0;
        short s = ring[(int) (read % ring.length)];
        read++;
        return s;
    }

    /** All samples played, or closed. */
    public boolean finished() {
        return closed || (ended && read >= written);
    }
}
// --8<-- [end:stream]
