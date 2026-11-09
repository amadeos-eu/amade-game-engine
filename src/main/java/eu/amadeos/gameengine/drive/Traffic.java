package eu.amadeos.gameengine.drive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The cars on the road and how they drive themselves. Each keeps to its lane at the speed it likes; when it
 * comes up behind a slower car it slows down to keep its distance, and if it {@link Car#overtakes} and the
 * other lane is clear far enough ahead, oncoming cars included, it pulls out, passes and pulls back in.
 * Cars coming the other way just drive. Cars steered by a player are not moved here, but the others see
 * them and keep their distance.
 *
 * <p>Collisions are the game's business: {@link #touching(Car)} finds the car a player's car runs into.
 */
// --8<-- [start:traffic]
public final class Traffic {
    /** Gaps kept to the car ahead: this many seconds of driving plus a car's length. */
    static final double FOLLOW_SECONDS = 1.0;
    /** How far ahead the other lane must be clear before a car pulls out, in world units. */
    static final double PASS_CLEAR = 9000;
    /** How fast a car moves across the road, in half-widths per second. */
    static final double LANE_CHANGE = 0.9;
    static final double ACCELERATION = 1800;
    static final double BRAKING = 5000;

    private final List<Car> cars = new ArrayList<>();
    // --8<-- [end:traffic]

    public void add(Car car) {
        cars.add(car);
    }

    public void remove(Car car) {
        cars.remove(car);
    }

    public void clear() {
        cars.clear();
    }

    /** All cars, in no particular order; the list itself may not be changed. */
    public List<Car> cars() {
        return Collections.unmodifiableList(cars);
    }

    /** Removes the cars that drive themselves and are further than {@code behind} back or {@code ahead} in front of {@code z}. */
    public int removeOutside(double z, double behind, double ahead) {
        int before = cars.size();
        cars.removeIf(c -> !c.steered && (c.z < z - behind || c.z > z + ahead));
        return before - cars.size();
    }

    // --8<-- [start:update]
    /** Moves every car that drives itself by {@code seconds}. */
    public void update(double seconds) {
        cars.sort(Comparator.comparingDouble(c -> c.z));
        for (Car c : cars) {
            if (c.steered) continue;
            double want = c.cruise;
            if (c.direction > 0) {
                Car ahead = ahead(c, c.x);
                if (ahead != null) {
                    double gap = ahead.z - c.z - (ahead.length + c.length) / 2;
                    double keep = c.speed * FOLLOW_SECONDS + c.length;
                    boolean slower = ahead.speed < c.cruise * 0.95;
                    if (c.overtakes && slower && gap < keep * 3 && Math.abs(c.targetX - c.homeX) < 0.01) {
                        double other = c.homeX > 0 ? c.homeX - 1 : c.homeX + 1;
                        if (clear(other, c.z - c.length * 1.5, c.z + PASS_CLEAR, c)) c.targetX = other;
                    }
                    if (gap < keep && Math.abs(c.targetX - ahead.x) < 0.5) want = Math.min(want, ahead.speed * Math.max(0, gap / keep));
                }
                // back into its lane once the lane there is clear again
                if (Math.abs(c.targetX - c.homeX) > 0.01 && clear(c.homeX, c.z - c.length * 2.5, c.z + c.length * 2, c)) {
                    c.targetX = c.homeX;
                }
            }
            double dv = want - c.speed;
            c.speed += Math.max(-BRAKING * seconds, Math.min(ACCELERATION * seconds, dv));
            c.speed = Math.max(0, c.speed);
            double dx = c.targetX - c.x;
            c.x += Math.max(-LANE_CHANGE * seconds, Math.min(LANE_CHANGE * seconds, dx));
            c.z += c.speed * c.direction * seconds;
        }
    }
    // --8<-- [end:update]

    /** The nearest car in front of {@code c} that drives its way and is near {@code laneX}; {@code null} if none. */
    public Car ahead(Car c, double laneX) {
        Car best = null;
        for (Car o : cars) {
            if (o == c || o.direction != c.direction || o.z <= c.z) continue;
            if (Math.abs(o.x - laneX) > (o.width + c.width) / 2 + 0.05) continue;
            if (best == null || o.z < best.z) best = o;
        }
        return best;
    }

    /**
     * True if no car but {@code except} is in the lane at {@code laneX} between {@code from} and {@code to};
     * a car coming the other way counts where it will be when we get there.
     */
    public boolean clear(double laneX, double from, double to, Car except) {
        for (Car o : cars) {
            if (o == except || Math.abs(o.x - laneX) > 0.6) continue;
            double reach = o.direction < 0 ? to + (to - from) * 0.6 : to;
            if (o.z + o.length / 2 > from && o.z - o.length / 2 < reach) return false;
        }
        return true;
    }

    /** The first car that {@code car} touches, or {@code null}. */
    public Car touching(Car car) {
        for (Car o : cars) if (o != car && o.touches(car)) return o;
        return null;
    }
}
