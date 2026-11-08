package eu.amadeos.gameengine.pinball;

/**
 * A steel ball on the table, in table units (x to the right, y down the table towards the player) and
 * units per second. A ball is free and rolls, held in a hole or lock, riding a {@link Track} (a ramp or a wire
 * form, where it is lifted off the field), or lost down the drain.
 *
 * <p>The fields are public on purpose, like those of {@link eu.amadeos.gameengine.physics.Body}: the
 * {@link Playfield} moves them many times a frame, and the game reads them to draw the ball.
 */
public final class Ball {
    public enum State { FREE, HELD, ON_TRACK, LOST }

    public double x;
    public double y;
    public double vx;
    public double vy;
    public final double radius;
    /** The floor the ball rolls on: 0 the playfield, higher numbers raised mini playfields. */
    public int layer;
    public State state = State.FREE;
    /** A captive ball stays in its own little lane: it never drains and is never held or launched. */
    public final boolean captive;
    /** For a captive ball: the box it may move in, {left, top, right, bottom}; null for a free ball. */
    public double[] fence;

    // riding a track
    Track track;
    double along;
    double speed;
    /** The hole holding the ball, while {@link State#HELD}. */
    String holder;

    public Ball(double x, double y, double radius) {
        this(x, y, radius, false);
    }

    public Ball(double x, double y, double radius, boolean captive) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.captive = captive;
    }

    public double speed() {
        return state == State.ON_TRACK ? Math.abs(speed) : Math.hypot(vx, vy);
    }

    /** The track the ball rides, or null. */
    public Track track() {
        return track;
    }

    /** How far along its track the ball is, in units from the entrance. */
    public double along() {
        return along;
    }

    /** The hole that holds the ball, or null. */
    public String holder() {
        return holder;
    }

    /** How high above the playfield the ball's lowest point is: on a ramp, its height there; else its floor's. */
    public double height(Playfield field) {
        return state == State.ON_TRACK ? track.heightAt(along) : field.layerHeight(layer);
    }
}
