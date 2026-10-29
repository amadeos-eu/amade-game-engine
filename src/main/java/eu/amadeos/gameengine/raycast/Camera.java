package eu.amadeos.gameengine.raycast;

/**
 * Where the player's eye is: a point on the map, the angle it looks at (0 = east, growing towards south, the
 * way y grows down the map), and the height of the eye above the floor, 0.5 being halfway up a wall.
 * A little up-and-down of the eye while walking is the bob.
 */
// --8<-- [start:camera]
public final class Camera {
    public double x;
    public double y;
    public double angle;
    public double eye = 0.5;

    public Camera() {
    }

    public Camera(double x, double y, double angle) {
        this.x = x;
        this.y = y;
        this.angle = angle;
    }

    public double dirX() {
        return Math.cos(angle);
    }

    public double dirY() {
        return Math.sin(angle);
    }
}
// --8<-- [end:camera]
