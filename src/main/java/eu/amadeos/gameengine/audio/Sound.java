package eu.amadeos.gameengine.audio;

import java.util.Arrays;

/**
 * A short sound effect as raw samples: 44,100 numbers per second, each the position of the loudspeaker's
 * membrane between -32768 and 32767. That is all a sound card plays.
 *
 * <p>The effects of the early arcade games were not recordings but tones made by the machine, and so are
 * these: {@link #tone} makes a square wave (the hard "blip" of the arcade), a triangle wave (softer) or
 * noise (crashes and explosions), sliding from one pitch to another, with a short fade in and out so the
 * speaker does not click. {@link #then} joins sounds into a little melody.
 */
public final class Sound {
    /** Samples per second, CD quality. */
    public static final int RATE = 44_100;

    /** The shape of one vibration. */
    public enum Wave { SQUARE, TRIANGLE, NOISE }

    private final short[] samples;

    private Sound(short[] samples) {
        this.samples = samples;
    }

    /** A sound from samples at {@link #RATE}; the array is copied. */
    public static Sound of(short[] samples) {
        return new Sound(samples.clone());
    }

    // --8<-- [start:tone]
    /**
     * A tone sliding from {@code fromHz} to {@code toHz} within {@code seconds}, at {@code volume} 0 to 1.
     * Noise ignores the pitch.
     */
    public static Sound tone(Wave wave, double fromHz, double toHz, double seconds, double volume) {
        int n = (int) Math.round(seconds * RATE);
        short[] samples = new short[n];
        int fade = Math.min(n / 4, RATE / 200);  // 5 ms in and out against clicks
        double phase = 0;                         // where in one vibration we are, 0 to 1
        long noise = 0x2545F4914F6CDD1DL;         // a fixed seed: the same sound every time
        for (int i = 0; i < n; i++) {
            double hz = fromHz + (toHz - fromHz) * i / n;
            phase = (phase + hz / RATE) % 1.0;
            double value = switch (wave) {
                case SQUARE -> phase < 0.5 ? 1 : -1;
                case TRIANGLE -> 4 * Math.abs(phase - 0.5) - 1;
                case NOISE -> {
                    noise ^= noise << 13;
                    noise ^= noise >>> 7;
                    noise ^= noise << 17;
                    yield (noise >>> 11) / (double) (1L << 53) * 2 - 1;
                }
            };
            double envelope = Math.min(1, Math.min(i, n - 1 - i) / (double) Math.max(1, fade));
            samples[i] = (short) Math.round(value * envelope * volume * Short.MAX_VALUE);
        }
        return new Sound(samples);
    }
    // --8<-- [end:tone]

    /** This sound, then {@code next}. */
    public Sound then(Sound next) {
        short[] joined = Arrays.copyOf(samples, samples.length + next.samples.length);
        System.arraycopy(next.samples, 0, joined, samples.length, next.samples.length);
        return new Sound(joined);
    }

    // --8<-- [start:mix]
    /**
     * The sounds played at once, as one sound as long as the longest: the voices of a tune, melody and bass.
     * The samples are added up and clipped, as the {@link Mixer} does while playing.
     */
    public static Sound mix(Sound... sounds) {
        int n = 0;
        for (Sound sound : sounds) n = Math.max(n, sound.samples.length);
        short[] mixed = new short[n];
        for (int i = 0; i < n; i++) {
            int sum = 0;
            for (Sound sound : sounds) {
                if (i < sound.samples.length) sum += sound.samples[i];
            }
            mixed[i] = (short) Math.clamp(sum, Short.MIN_VALUE, Short.MAX_VALUE);
        }
        return new Sound(mixed);
    }
    // --8<-- [end:mix]

    public int length() {
        return samples.length;
    }

    public double seconds() {
        return samples.length / (double) RATE;
    }

    /** Sample {@code i}; for the mixer and for tests. */
    public short sample(int i) {
        return samples[i];
    }
}
