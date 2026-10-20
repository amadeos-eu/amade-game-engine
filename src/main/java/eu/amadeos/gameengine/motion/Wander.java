package eu.amadeos.gameengine.motion;

import eu.amadeos.gameengine.random.SeededRandom;

/**
 * An animal that grazes: it stands a while, walks a few steps to a spot within its pasture, stands again.
 * The pasture is a circle around a home point, so a sheep never strays from its fence and a deer stays in
 * its wood. Each pause and each new spot is drawn from the {@link SeededRandom} given to {@link #update},
 * so a game that repeats its seed repeats its sheep.
 *
 * <pre>
 * Wander sheep = new Wander(homeX, homeY, 20, 8);  // within 20 units of home, 8 units a second
 * sheep.update(seconds, looks);                     // a fork of the game's random, or the game's own
 * draw(sheep.x(), sheep.y(), sheep.facingLeft());
 * </pre>
 *
 * <p>{@link #flee} sends it off at double speed away from a point, the wolf or the dog, to a spot at the
 * far side of its pasture.
 */
// --8<-- [start:wander]
public final class Wander {
    private double homeX;
    private double homeY;
    private final double radius;
    private final double speed;
    private double x;
    private double y;
    private double targetX;
    private double targetY;
    private double pause;
    private double pace = 1;
    private boolean left;

    public Wander(double homeX, double homeY, double radius, double speed) {
        if (radius < 0 || speed <= 0) throw new IllegalArgumentException("radius " + radius + ", speed " + speed);
        this.homeX = homeX;
        this.homeY = homeY;
        this.radius = radius;
        this.speed = speed;
        this.x = homeX;
        this.y = homeY;
        this.targetX = homeX;
        this.targetY = homeY;
    }

    /** One step: stands or walks; true while it walks. */
    public boolean update(double seconds, SeededRandom random) {
        if (pause > 0) {
            pause -= seconds;
            if (pause > 0) return false;
            pickSpot(random);
        }
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.hypot(dx, dy);
        double step = speed * pace * seconds;
        if (Math.abs(dx) > 1e-9) left = dx < 0;
        if (distance <= step) {
            x = targetX;
            y = targetY;
            pace = 1;
            pause = 1 + random.nextDouble() * 3;     // one to four seconds of grazing
            return false;
        }
        x += dx / distance * step;
        y += dy / distance * step;
        return true;
    }

    /** A spot drawn evenly from the circle: the square root keeps the middle from crowding. */
    private void pickSpot(SeededRandom random) {
        double angle = random.nextDouble() * 2 * Math.PI;
        double r = radius * Math.sqrt(random.nextDouble());
        targetX = homeX + Math.cos(angle) * r;
        targetY = homeY + Math.sin(angle) * r;
    }
    // --8<-- [end:wander]

    /** Runs away from {@code fromX}, {@code fromY} to the far side of the pasture, at twice the speed. */
    public void flee(double fromX, double fromY) {
        double dx = homeX - fromX;
        double dy = homeY - fromY;
        double d = Math.hypot(dx, dy);
        if (d < 1e-9) {
            dx = 1;
            d = 1;
        }
        targetX = homeX + dx / d * radius * 0.8;
        targetY = homeY + dy / d * radius * 0.8;
        pause = 0;
        pace = 2;
    }

    /** Moves the pasture, as when a farm is rebuilt; the animal walks there on its next spot. */
    public void home(double homeX, double homeY) {
        this.homeX = homeX;
        this.homeY = homeY;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public boolean walking() {
        return pause <= 0 && (x != targetX || y != targetY);
    }

    public boolean facingLeft() {
        return left;
    }

    public double homeX() {
        return homeX;
    }

    public double homeY() {
        return homeY;
    }

    /** Where it is, where it goes and how long it still stands, for a saved game. */
    public double[] state() {
        return new double[]{x, y, targetX, targetY, pause, pace, left ? 1 : 0};
    }

    public void restore(double[] state) {
        x = state[0];
        y = state[1];
        targetX = state[2];
        targetY = state[3];
        pause = state[4];
        pace = state[5];
        left = state[6] != 0;
    }
}
