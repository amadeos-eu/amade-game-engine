package eu.amadeos.gameengine.drive;

/**
 * How the player's car drives: it speeds up while the gas is held and slows down without it, brakes, steers
 * faster the faster it goes, and is pushed outwards in curves, more the faster it takes them; so a curve must
 * be steered through, and taken too fast it carries the car off the road. On grass the car slows down to a
 * crawl; against a wall it stops short.
 *
 * <p>Sideways everything is in road half-widths, along the road in world units per second. A car of the
 * game sets its own numbers: a camper van has a low top speed and soft steering, a sports car the opposite.
 */
// --8<-- [start:driving]
public final class Driving {
    public double topSpeed = 9000;
    public double acceleration = 2600;
    public double braking = 7000;
    /** Slowing down without gas. */
    public double coasting = 900;
    /** How far the wheel moves the car across, in half-widths per second at top speed. */
    public double steering = 2.2;
    /** How strongly a curve pushes outwards. */
    public double centrifugal = 0.32;
    /** Off the road the car does not go faster than this part of its top speed. */
    public double offRoad = 0.35;
    /** The car cannot leave the road further than this, in half-widths. */
    public double limit = 3;

    /** What happened in a step: the car went over the grass, or hit a wall. */
    public record Result(boolean offRoad, boolean hitWall) {
    }

    /**
     * Moves {@code car} by {@code seconds}: {@code steer} from -1 (left) to 1 (right), {@code gas} and
     * {@code brake} held or not. The segment under the car gives the curve and the walls.
     */
    public Result update(Car car, Road road, double seconds, double steer, boolean gas, boolean brake) {
        Segment s = road.at(car.z);
        if (gas) car.speed += acceleration * seconds * (1 - 0.6 * car.speed / topSpeed);
        else car.speed -= coasting * seconds;
        if (brake) car.speed -= braking * seconds;
        double ratio = Math.max(0, car.speed) / topSpeed;
        car.x += steer * steering * seconds * Math.min(1, 0.25 + ratio);
        car.x -= s.curve * centrifugal * ratio * ratio * seconds;
        boolean off = Math.abs(car.x) > 1.08;
        if (off && car.speed > topSpeed * offRoad) car.speed -= braking * 0.6 * seconds;
        boolean hit = false;
        double half = car.width / 2;
        if (s.left != null && car.x < -s.left.offset() + half) {
            car.x = -s.left.offset() + half;
            hit = true;
        }
        if (s.right != null && car.x > s.right.offset() - half) {
            car.x = s.right.offset() - half;
            hit = true;
        }
        car.x = Math.max(-limit, Math.min(limit, car.x));
        if (hit) car.speed *= Math.pow(0.3, seconds);
        car.speed = Math.max(0, Math.min(topSpeed, car.speed));
        car.z += car.speed * seconds;
        return new Result(off, hit);
    }
}
// --8<-- [end:driving]
