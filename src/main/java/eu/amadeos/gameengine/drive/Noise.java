package eu.amadeos.gameengine.drive;

/**
 * Value noise: smooth randomness that is the same every time for the same input and seed. One octave is a
 * row of random heights joined by smooth steps; {@link #fbm} adds octaves of twice the detail and half the
 * height, which gives the rough-but-natural line of a cliff top or a mountain ridge.
 */
public final class Noise {
    private Noise() {
    }

    /** A number from 0 to 1 for {@code i} and {@code seed}, always the same. */
    public static double hash(long i, int seed) {
        long x = i * 374761393L + seed * 668265263L + 0x9E3779B9L;
        x = (x ^ (x >>> 13)) * 1274126177L;
        x ^= x >>> 16;
        return (x & 0xffffff) / (double) 0xffffff;
    }

    /** Smooth noise along a line, 0 to 1. */
    public static double noise(double x, int seed) {
        long i = (long) Math.floor(x);
        double f = x - i, u = f * f * (3 - 2 * f);
        return hash(i, seed) * (1 - u) + hash(i + 1, seed) * u;
    }

    /** Smooth noise on a plane, 0 to 1. */
    public static double noise(double x, double y, int seed) {
        long ix = (long) Math.floor(x), iy = (long) Math.floor(y);
        double fx = x - ix, fy = y - iy, ux = fx * fx * (3 - 2 * fx), uy = fy * fy * (3 - 2 * fy);
        double a = hash(ix * 7919 + iy * 104729, seed), b = hash((ix + 1) * 7919 + iy * 104729, seed);
        double c = hash(ix * 7919 + (iy + 1) * 104729, seed), d = hash((ix + 1) * 7919 + (iy + 1) * 104729, seed);
        return (a * (1 - ux) + b * ux) * (1 - uy) + (c * (1 - ux) + d * ux) * uy;
    }

    /** {@code octaves} layers of {@link #noise(double, int)}, each twice as fine and half as strong; 0 to 1. */
    public static double fbm(double x, int seed, int octaves) {
        double a = 0, amp = 1, sum = 0;
        for (int o = 0; o < octaves; o++) {
            a += noise(x, seed + o * 31) * amp;
            sum += amp;
            amp *= 0.5;
            x *= 2.03;
        }
        return a / sum;
    }

    public static double fbm(double x, double y, int seed, int octaves) {
        double a = 0, amp = 1, sum = 0;
        for (int o = 0; o < octaves; o++) {
            a += noise(x, y, seed + o * 17) * amp;
            sum += amp;
            amp *= 0.5;
            x *= 2.01;
            y *= 2.01;
        }
        return a / sum;
    }
}
