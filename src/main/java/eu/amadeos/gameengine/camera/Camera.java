package eu.amadeos.gameengine.camera;

/**
 * The part of a world larger than the screen that is shown: a view of the screen's logical size, e.g.
 * 640 × 400, whose top left corner lies somewhere in a world of, say, 2400 × 400. The game draws the world
 * shifted by the camera's position; everything on the screen is then {@code worldX - camera.x()}.
 *
 * <p>The camera follows a target, usually the player, with two rules that make scrolling calm:
 *
 * <ul>
 *   <li>A <b>dead zone</b>: a box in the middle of the view in which the target may move without the
 *   camera moving at all. A player who runs back and forth a little, or jumps on the spot, does not shake
 *   the whole world.</li>
 *   <li><b>Smoothing</b>: when the target leaves the dead zone, the camera does not jump after it but
 *   closes a part of the distance every second, the same part however long the step.</li>
 * </ul>
 *
 * <p>The camera never shows anything outside the world: at the edges of the world it stops, and the target
 * moves towards the edge of the screen. A world smaller than the view is centred.
 *
 * <pre>
 * Camera camera = new Camera(640, 400, map.width() * 16, map.height() * 16).deadZone(48, 64);
 * camera.jumpTo(player.centerX(), player.centerY());    // at the start: no scrolling in from elsewhere
 * // in each update step:
 * camera.follow(player.centerX(), player.centerY(), seconds);
 * // when drawing:
 * g.translate(-camera.x(alpha), -camera.y(alpha));
 * </pre>
 *
 * <p>No AWT: the camera is arithmetic, and the tests feed it any target.
 */
// --8<-- [start:camera]
public final class Camera {
    private final double viewWidth;
    private final double viewHeight;
    private final double worldWidth;
    private final double worldHeight;
    private double deadWidth;
    private double deadHeight;
    /** The part of the remaining distance the camera closes per second, 0 … 1; 1 follows at once. */
    private double catchUp = 0.99;
    private double x;
    private double y;
    private double previousX;
    private double previousY;
    /** Where the view's top left corner lies on the screen: (0, 0) unless a status bar takes the top. */
    private double screenLeft;
    private double screenTop;

    public Camera(double viewWidth, double viewHeight, double worldWidth, double worldHeight) {
        if (viewWidth <= 0 || viewHeight <= 0 || worldWidth <= 0 || worldHeight <= 0) {
            throw new IllegalArgumentException("empty view or world");
        }
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
    }

    /** The box in the middle of the view in which the target moves without the camera following. */
    public Camera deadZone(double width, double height) {
        deadWidth = Math.clamp(width, 0, viewWidth);
        deadHeight = Math.clamp(height, 0, viewHeight);
        return this;
    }

    /**
     * How fast the camera catches up once the target has left the dead zone: the part of the distance it
     * closes in one second. 0.99 leaves 1 % after a second, 0.5 is a slow drift; 1 sticks to the target.
     */
    public Camera catchUp(double partPerSecond) {
        catchUp = Math.clamp(partPerSecond, 0, 1);
        return this;
    }

    /** Puts the target in the middle of the view at once, without scrolling: at the start, after a jump. */
    public void jumpTo(double targetX, double targetY) {
        x = clampX(targetX - viewWidth / 2);
        y = clampY(targetY - viewHeight / 2);
        previousX = x;
        previousY = y;
    }

    /** One update step: moves towards the target if it has left the dead zone. */
    public void follow(double targetX, double targetY, double seconds) {
        previousX = x;
        previousY = y;
        double wantedX = x + outside(targetX - (x + viewWidth / 2), deadWidth / 2);
        double wantedY = y + outside(targetY - (y + viewHeight / 2), deadHeight / 2);
        // The part left after this step is (1 - catchUp) per second, so (1 - catchUp)^seconds of it:
        // the same speed at 60 or at 120 steps a second.
        double part = catchUp >= 1 ? 1 : 1 - Math.pow(1 - catchUp, seconds);
        x = clampX(x + (wantedX - x) * part);
        y = clampY(y + (wantedY - y) * part);
    }

    /** How far {@code offset} lies beyond {@code half} on either side of the middle; 0 inside. */
    private static double outside(double offset, double half) {
        if (offset > half) return offset - half;
        if (offset < -half) return offset + half;
        return 0;
    }

    private double clampX(double left) {
        if (worldWidth <= viewWidth) return (worldWidth - viewWidth) / 2;
        return Math.clamp(left, 0, worldWidth - viewWidth);
    }

    private double clampY(double top) {
        if (worldHeight <= viewHeight) return (worldHeight - viewHeight) / 2;
        return Math.clamp(top, 0, worldHeight - viewHeight);
    }
    // --8<-- [end:camera]

    // --8<-- [start:move]
    /**
     * One update step of a camera that the player moves, by keys, the mouse at the edge of the screen or
     * dragging the map: by {@code dx}, {@code dy} in the world, as far as the edges allow. Call it in every
     * step, with 0, 0 when nothing moves, so that drawing between two steps does not drift.
     */
    public void moveBy(double dx, double dy) {
        previousX = x;
        previousY = y;
        x = clampX(x + dx);
        y = clampY(y + dy);
    }
    // --8<-- [end:move]

    /**
     * Places the view on the screen with its top left corner at {@code left}, {@code top} in logical
     * coordinates, e.g. below a bar of 22 units: a view of 640 × 378 at (0, 22).
     */
    public Camera placedAt(double left, double top) {
        screenLeft = left;
        screenTop = top;
        return this;
    }

    public double screenLeft() {
        return screenLeft;
    }

    public double screenTop() {
        return screenTop;
    }

    // --8<-- [start:to-world]
    /**
     * Where a point of the screen, such as the mouse, lies in the world: the camera's position plus the
     * point's place in the view. Screen coordinates are those of the canvas, logical, so the window's size,
     * the black bars and the render mode are already taken care of ({@code Mouse}). The answer is for the
     * camera after the last step, where the rules live; the picture may lag a fraction of a step behind.
     */
    public double worldX(double screenX) {
        return x + screenX - screenLeft;
    }

    public double worldY(double screenY) {
        return y + screenY - screenTop;
    }

    /** Where a point of the world is drawn on the screen, between the last two steps. */
    public double screenX(double worldX, double alpha) {
        return worldX - x(alpha) + screenLeft;
    }

    public double screenY(double worldY, double alpha) {
        return worldY - y(alpha) + screenTop;
    }

    /** True if a point of the screen lies in the view, not on a bar beside it. */
    public boolean shows(double screenX, double screenY) {
        return screenX >= screenLeft && screenX < screenLeft + viewWidth
                && screenY >= screenTop && screenY < screenTop + viewHeight;
    }
    // --8<-- [end:to-world]

    /** The left edge of the view in the world, after the last step. */
    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    /** The left edge between the last two steps, for drawing: {@code alpha} 0 is the step before, 1 the last. */
    public double x(double alpha) {
        return previousX + (x - previousX) * alpha;
    }

    public double y(double alpha) {
        return previousY + (y - previousY) * alpha;
    }

    public double viewWidth() {
        return viewWidth;
    }

    public double viewHeight() {
        return viewHeight;
    }

    public double worldWidth() {
        return worldWidth;
    }

    public double worldHeight() {
        return worldHeight;
    }

    /** True if a box in the world can be seen, at least in part: what lies outside need not be drawn. */
    public boolean sees(double left, double top, double width, double height, double alpha) {
        double cx = x(alpha);
        double cy = y(alpha);
        return left + width > cx && left < cx + viewWidth && top + height > cy && top < cy + viewHeight;
    }
}
