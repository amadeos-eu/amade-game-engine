package eu.amadeos.gameengine.fx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/**
 * Many small, short-lived squares: splinters of a brick, sparks, dust. Each flies in a straight line,
 * falls with gravity and fades out at the end of its life.
 *
 * <p>Hundreds of them may live at the same time, so they are not objects. They are rows in a few arrays
 * (one array per property), and the system never creates an object while the game runs: no {@code new} in
 * {@link #update} or {@link #render}, not even for colours, which are made once for every shade of fading.
 * A dead particle's row is taken over by the last living one, so the living stay at the front.
 *
 * <p>Use it on the game loop thread only.
 */
public final class Particles {
    /** Shades of fading per colour. */
    private static final int SHADES = 16;

    private final int capacity;
    private final double[] x, y, previousX, previousY, vx, vy, life, lifetime, size;
    private final int[] colour;
    /** {@code shades[c][s]}: colour {@code c} at opacity {@code s / (SHADES - 1)}. */
    private final Color[][] shades;
    private final Rectangle2D.Double square = new Rectangle2D.Double();
    private int count;

    /**
     * @param capacity most particles alive at once; more are dropped
     * @param palette  the colours particles can have, referred to by their index
     */
    public Particles(int capacity, Color... palette) {
        if (capacity < 1 || palette.length == 0) throw new IllegalArgumentException();
        this.capacity = capacity;
        x = new double[capacity];
        y = new double[capacity];
        previousX = new double[capacity];
        previousY = new double[capacity];
        vx = new double[capacity];
        vy = new double[capacity];
        life = new double[capacity];
        lifetime = new double[capacity];
        size = new double[capacity];
        colour = new int[capacity];
        shades = new Color[palette.length][SHADES];
        for (int c = 0; c < palette.length; c++) {
            for (int s = 0; s < SHADES; s++) {
                Color p = palette[c];
                shades[c][s] = new Color(p.getRed(), p.getGreen(), p.getBlue(), p.getAlpha() * s / (SHADES - 1));
            }
        }
    }

    public int count() {
        return count;
    }

    public int capacity() {
        return capacity;
    }

    // --8<-- [start:spawn]
    /**
     * A new particle at {@code x}, {@code y}, flying at {@code vx}, {@code vy} units per second, living
     * {@code seconds}; false if all rows are taken.
     */
    public boolean spawn(double x, double y, double vx, double vy, double seconds, double size, int colour) {
        if (count == capacity) return false;
        int i = count++;
        this.x[i] = previousX[i] = x;
        this.y[i] = previousY[i] = y;
        this.vx[i] = vx;
        this.vy[i] = vy;
        life[i] = lifetime[i] = seconds;
        this.size[i] = size;
        this.colour[i] = colour;
        return true;
    }
    // --8<-- [end:spawn]

    // --8<-- [start:update]
    /** Moves all particles by one step; {@code gravity} pulls down in units per second². */
    public void update(double seconds, double gravity) {
        int i = 0;
        while (i < count) {
            life[i] -= seconds;
            if (life[i] <= 0) {
                remove(i);
                continue;  // row i now holds the former last particle: look at it again
            }
            previousX[i] = x[i];
            previousY[i] = y[i];
            vy[i] += gravity * seconds;
            x[i] += vx[i] * seconds;
            y[i] += vy[i] * seconds;
            i++;
        }
    }

    private void remove(int i) {
        int last = --count;
        x[i] = x[last];
        y[i] = y[last];
        previousX[i] = previousX[last];
        previousY[i] = previousY[last];
        vx[i] = vx[last];
        vy[i] = vy[last];
        life[i] = life[last];
        lifetime[i] = lifetime[last];
        size[i] = size[last];
        colour[i] = colour[last];
    }
    // --8<-- [end:update]

    /** Removes all particles, e.g. for a new level. */
    public void clear() {
        count = 0;
    }

    // --8<-- [start:render]
    /** Draws the particles between their last two positions; they fade during the last half of their life. */
    public void render(Graphics2D g, double alpha) {
        for (int i = 0; i < count; i++) {
            double fade = Math.min(1, 2 * life[i] / lifetime[i]);
            g.setColor(shades[colour[i]][(int) Math.round(fade * (SHADES - 1))]);
            double s = size[i];
            square.setRect(previousX[i] + (x[i] - previousX[i]) * alpha - s / 2,
                    previousY[i] + (y[i] - previousY[i]) * alpha - s / 2, s, s);
            g.fill(square);
        }
    }
    // --8<-- [end:render]

    /** For tests: where particle {@code i} is. */
    double x(int i) {
        return x[i];
    }

    double y(int i) {
        return y[i];
    }
}
