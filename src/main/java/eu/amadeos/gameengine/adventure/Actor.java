package eu.amadeos.gameengine.adventure;

import eu.amadeos.gameengine.math.Vector;
import eu.amadeos.gameengine.tile.Direction;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Someone in a room: the hero, the archivist, the pigeon. An actor stands with its feet at a {@link #position()},
 * looks in a {@link #facing() direction} and walks along a way of points, as {@link WalkArea#path} finds it,
 * at a speed that shrinks with the {@link DepthScale}: far away, a step covers fewer pixels.
 *
 * <p>The actor has no pictures. It tells the game what to draw: the direction, whether it walks, and
 * {@link #stride()}, the distance walked in steps of full size, from which the game picks the frame of the
 * walk cycle; so the feet keep in time with the floor at every depth. It has no AWT either, so the rules of a
 * room can be tested without a screen.
 */
public final class Actor {
    private final String id;
    private Vector position;
    private Direction facing;
    /** Pixels per second at scale 1. */
    private final double speed;
    private final Deque<Vector> route = new ArrayDeque<>();
    private Runnable then;
    private double stride;

    public Actor(String id, Vector position, Direction facing, double speed) {
        this.id = Objects.requireNonNull(id, "id");
        this.position = Objects.requireNonNull(position, "position");
        this.facing = Objects.requireNonNull(facing, "facing");
        if (speed <= 0) throw new IllegalArgumentException("speed must be positive: " + speed);
        this.speed = speed;
    }

    public String id() {
        return id;
    }

    public Vector position() {
        return position;
    }

    public Direction facing() {
        return facing;
    }

    public void face(Direction direction) {
        facing = Objects.requireNonNull(direction, "direction");
    }

    /** Puts the actor somewhere at once, as when it enters a room; a walk in progress is dropped. */
    public void place(Vector at, Direction direction) {
        stop();
        position = at;
        facing = direction;
    }

    /** Walks along the area's shortest way to {@code target}, then runs {@code then} (may be null). */
    public void walkTo(WalkArea area, Vector target, Runnable then) {
        walk(area.path(position, target), then);
    }

    /**
     * Walks to the points one after the other, then runs {@code then} (may be null). A point equal to the
     * position is skipped, so a way that starts here, as {@link WalkArea#path} returns it, can be passed as
     * it is. A walk in progress is replaced; its {@code then} is not run.
     */
    public void walk(List<Vector> points, Runnable then) {
        route.clear();
        route.addAll(points);
        this.then = then;
        if (route.isEmpty()) arrive();
    }

    /** Stands still where it is; what was to happen on arrival does not happen. */
    public void stop() {
        route.clear();
        then = null;
    }

    public boolean isWalking() {
        return !route.isEmpty();
    }

    /** The distance walked so far, in pixels at scale 1: the game picks the walk frame from it. */
    public double stride() {
        return stride;
    }

    // --8<-- [start:update]
    /** Walks on for {@code seconds}, at the speed the depth gives where it is. */
    public void update(double seconds, DepthScale depth) {
        double left = seconds;
        while (left > 0 && !route.isEmpty()) {
            double scale = depth.at(position.y());
            Vector target = route.peekFirst();
            Vector to = target.minus(position);
            double distance = to.length();
            double reach = speed * scale * left;
            if (distance <= reach) {
                position = target;
                route.removeFirst();
                if (distance > 0) {
                    facing = facingFor(to);
                    stride += distance / scale;
                    left -= distance / (speed * scale);
                }
                if (route.isEmpty()) arrive();
            } else {
                position = position.plus(to.times(reach / distance));
                facing = facingFor(to);
                stride += reach / scale;
                left = 0;
            }
        }
    }
    // --8<-- [end:update]

    private void arrive() {
        Runnable done = then;
        then = null;
        if (done != null) done.run();
    }

    /**
     * The direction to show for walking along {@code d}. A floor seen at a slant looks flatter than it is:
     * walking towards the back wall covers fewer pixels than walking sideways. So the side views win until
     * the way runs more than twice as much up or down as across.
     */
    static Direction facingFor(Vector d) {
        if (Math.abs(d.x()) * 2 >= Math.abs(d.y())) return d.x() < 0 ? Direction.LEFT : Direction.RIGHT;
        return d.y() < 0 ? Direction.UP : Direction.DOWN;
    }
}
