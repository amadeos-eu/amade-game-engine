package eu.amadeos.gameengine.terrain;

import eu.amadeos.gameengine.random.SeededRandom;

import java.util.function.DoubleUnaryOperator;

/**
 * Hills and valleys from a seed: the line of a landscape seen from the side, made by <b>midpoint
 * displacement</b>. Start with the two ends; put the middle between them and move it up or down by chance;
 * do the same for the two halves with half the chance, and so on. Big moves make the mountains, the small
 * ones late the rough edges. The same seed gives the same landscape, on every computer.
 *
 * <p>Heights are y values in game units, so a larger number is lower on the screen.
 *
 * <pre>
 * double[] line = Landscape.generate(129, 290, 140, 0.55, new SeededRandom(seed));
 * Landscape.clamp(line, 120, 370);
 * Landscape.flatten(line, 20, 3);        // a shelf for a tank at sample 20
 * ground.fill(Landscape.sample(line, 640.0 / 128), layering);
 * </pre>
 */
public final class Landscape {
    private Landscape() {
    }

    // --8<-- [start:generate]
    /**
     * A landscape line of {@code samples} heights (at least 2) around {@code base}: the first move is up to
     * {@code amplitude} up or down, and each halving of the distance keeps {@code roughness} of the move
     * before, 0.5 for gentle hills, 0.7 for jagged mountains.
     */
    public static double[] generate(int samples, double base, double amplitude, double roughness, SeededRandom random) {
        if (samples < 2) throw new IllegalArgumentException("a line needs two ends");
        int size = 1;
        while (size < samples - 1) size *= 2;
        double[] line = new double[size + 1];
        line[0] = base + (random.nextDouble() - 0.5) * amplitude;
        line[size] = base + (random.nextDouble() - 0.5) * amplitude;
        double move = amplitude;
        for (int step = size; step > 1; step /= 2) {
            for (int i = step / 2; i < size; i += step) {
                line[i] = (line[i - step / 2] + line[i + step / 2]) / 2 + (random.nextDouble() - 0.5) * move;
            }
            move *= roughness;
        }
        double[] result = new double[samples];
        for (int i = 0; i < samples; i++) result[i] = line[(int) Math.round((double) i * size / (samples - 1))];
        return result;
    }
    // --8<-- [end:generate]

    /** Keeps every height between {@code top} and {@code bottom}. */
    public static void clamp(double[] line, double top, double bottom) {
        for (int i = 0; i < line.length; i++) line[i] = Math.max(top, Math.min(bottom, line[i]));
    }

    /** Levels the line {@code half} samples to each side of {@code centre} to the height there: a shelf. */
    public static void flatten(double[] line, int centre, int half) {
        double y = line[centre];
        for (int i = Math.max(0, centre - half); i <= Math.min(line.length - 1, centre + half); i++) line[i] = y;
    }

    /** Adds a bump of {@code height} units (negative raises, as y grows downwards) shaped like a bell around {@code centre}. */
    public static void bump(double[] line, double centre, double width, double height) {
        for (int i = 0; i < line.length; i++) {
            double d = (i - centre) / width;
            line[i] += height * Math.exp(-d * d);
        }
    }

    /** The line as a function of x, with the samples {@code spacing} units apart and straight between them. */
    public static DoubleUnaryOperator sample(double[] line, double spacing) {
        double[] copy = line.clone();
        return x -> {
            double i = x / spacing;
            if (i <= 0) return copy[0];
            if (i >= copy.length - 1) return copy[copy.length - 1];
            int a = (int) i;
            double t = i - a;
            return copy[a] * (1 - t) + copy[a + 1] * t;
        };
    }
}
