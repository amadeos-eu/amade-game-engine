package eu.amadeos.gameengine.pinball;

import java.util.ArrayList;
import java.util.List;

/**
 * A ramp or a wire form: a path the ball rides above the playfield, from an entrance to an exit, with a
 * height at every point. Instead of colliding with the ramp's walls, the ball simply follows the path: the
 * {@link Playfield} keeps only how far along it is and how fast it goes. Gravity slows the ball while the
 * track climbs (and pulls it down the table where the track runs towards the player), so a weak shot rolls
 * back out of the entrance, as on a real machine; a strong one reaches the end and drops onto the floor
 * {@code exitLayer} there, moving the way the track ran.
 *
 * <p>Points are table units: x, y and the height h above the playfield at that point.
 */
public final class Track {
    public final String id;
    public final int exitLayer;
    private final double[] xs, ys, hs, at;

    public Track(String id, int exitLayer, double... xyh) {
        if (xyh.length < 6 || xyh.length % 3 != 0) throw new IllegalArgumentException("a track needs points x y h");
        this.id = id;
        this.exitLayer = exitLayer;
        int n = xyh.length / 3;
        xs = new double[n];
        ys = new double[n];
        hs = new double[n];
        at = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = xyh[3 * i];
            ys[i] = xyh[3 * i + 1];
            hs[i] = xyh[3 * i + 2];
            if (i > 0) at[i] = at[i - 1] + Math.hypot(xs[i] - xs[i - 1], ys[i] - ys[i - 1]);
        }
    }

    public double length() {
        return at[at.length - 1];
    }

    public int points() {
        return xs.length;
    }

    public double x(int i) {
        return xs[i];
    }

    public double y(int i) {
        return ys[i];
    }

    public double h(int i) {
        return hs[i];
    }

    private int segment(double s) {
        int i = 0;
        while (i < at.length - 2 && at[i + 1] < s) i++;
        return i;
    }

    private double part(int i, double s) {
        double len = at[i + 1] - at[i];
        return len == 0 ? 0 : Math.max(0, Math.min(1, (s - at[i]) / len));
    }

    public double xAt(double s) {
        int i = segment(s);
        return xs[i] + (xs[i + 1] - xs[i]) * part(i, s);
    }

    public double yAt(double s) {
        int i = segment(s);
        return ys[i] + (ys[i + 1] - ys[i]) * part(i, s);
    }

    public double heightAt(double s) {
        int i = segment(s);
        return hs[i] + (hs[i + 1] - hs[i]) * part(i, s);
    }

    /** The direction of the track at {@code s}, as a unit vector {dx, dy} on the table, and the climb dh per unit. */
    public double[] direction(double s) {
        int i = segment(s);
        double dx = xs[i + 1] - xs[i], dy = ys[i + 1] - ys[i];
        double len = Math.max(1e-9, Math.hypot(dx, dy));
        return new double[]{dx / len, dy / len, (hs[i + 1] - hs[i]) / len};
    }

    /** Points every {@code step} units along the track, for drawing: {x, y, h}. */
    public List<double[]> samples(double step) {
        List<double[]> out = new ArrayList<>();
        for (double s = 0; s < length(); s += step) out.add(new double[]{xAt(s), yAt(s), heightAt(s)});
        out.add(new double[]{xs[xs.length - 1], ys[ys.length - 1], hs[hs.length - 1]});
        return out;
    }
}
