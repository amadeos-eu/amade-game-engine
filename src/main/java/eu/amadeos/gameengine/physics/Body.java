package eu.amadeos.gameengine.physics;

import eu.amadeos.gameengine.collision.Box;

/**
 * Something that moves through a tile world and stands on things: a player, an enemy that walks. A box,
 * its top left corner at {@code x}, {@code y}, with a velocity in units per second, and what it touched in
 * its last move: the ground under it, a ceiling, a wall. {@link TileCollider} moves it and fills those in.
 *
 * <p>The fields are public, as in a {@link Box} that can change: the rules of a platformer read and set
 * them all the time, and nothing has to be kept consistent between them. The position before the last
 * step is kept for drawing between two steps.
 */
// --8<-- [start:body]
public final class Body {
    public double x;
    public double y;
    public final double width;
    public final double height;
    /** Velocity in units per second; y grows downwards, as on the screen. */
    public double vx;
    public double vy;
    /** What the last move touched. */
    public boolean onGround;
    public boolean hitCeiling;
    public boolean hitLeft;
    public boolean hitRight;
    /** Standing on a platform that can be jumped through from below and dropped through from above. */
    public boolean onPlatform;
    private double previousX;
    private double previousY;

    public Body(double x, double y, double width, double height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("empty body");
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        previousX = x;
        previousY = y;
    }

    /**
     * Gravity: the vertical speed grows by {@code gravity} units per second each second, up to
     * {@code maxFall}. Real things fall faster and faster; a game's things stop at a speed the eye can follow
     * and the tiles can catch.
     */
    public void fall(double gravity, double maxFall, double seconds) {
        vy = Math.min(vy + gravity * seconds, maxFall);
    }
    // --8<-- [end:body]

    /** Puts the body somewhere at once, at rest, also for drawing: a new start, no gliding there. */
    public void place(double x, double y) {
        this.x = x;
        this.y = y;
        vx = 0;
        vy = 0;
        previousX = x;
        previousY = y;
        onGround = hitCeiling = hitLeft = hitRight = onPlatform = false;
    }

    /** Remembers the position as the one before the coming step; {@link TileCollider#move} calls it. */
    public void remember() {
        previousX = x;
        previousY = y;
    }

    public double previousX() {
        return previousX;
    }

    public double previousY() {
        return previousY;
    }

    /** The left edge between the last two steps, for drawing. */
    public double x(double alpha) {
        return previousX + (x - previousX) * alpha;
    }

    public double y(double alpha) {
        return previousY + (y - previousY) * alpha;
    }

    public double right() {
        return x + width;
    }

    public double bottom() {
        return y + height;
    }

    public double centerX() {
        return x + width / 2;
    }

    public double centerY() {
        return y + height / 2;
    }

    public Box box() {
        return new Box(x, y, width, height);
    }

    public boolean overlaps(Body other) {
        return x < other.right() && other.x < right() && y < other.bottom() && other.y < bottom();
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "Body[%.2f, %.2f, %.0f × %.0f, v %.1f, %.1f%s]",
                x, y, width, height, vx, vy, onGround ? ", on the ground" : "");
    }
}
