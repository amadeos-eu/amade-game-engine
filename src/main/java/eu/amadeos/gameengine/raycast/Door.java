package eu.amadeos.gameengine.raycast;

/**
 * A sliding door in a cell of a {@link GridMap}. The door is a thin wall through the middle of its cell,
 * either along x (it separates north from south) or along y. Opening slides it sideways into the wall
 * next to it; after a while it closes again by itself, but not while something stands in its cell.
 *
 * <p>A secret door is an ordinary door whose face is a wall texture: it looks like a wall until it moves.
 */
// --8<-- [start:door]
public final class Door {
    public enum State { CLOSED, OPENING, OPEN, CLOSING }

    private final int x;
    private final int y;
    private final boolean alongX;
    private final Texture face;
    private final Texture frame;
    private double seconds = 0.6;
    private double stayOpen = 3;
    private State state = State.CLOSED;
    private double open;
    private double openFor;

    /**
     * @param alongX true if the door runs along the x axis, through the middle of its cell from west to east
     * @param face   the door itself
     * @param frame  the sides of the walls next to the door, seen in the opening; {@code null} for their own texture
     */
    public Door(int x, int y, boolean alongX, Texture face, Texture frame) {
        this.x = x;
        this.y = y;
        this.alongX = alongX;
        this.face = face;
        this.frame = frame;
    }

    /** How long the door takes to open or close, and how long it stays open; {@code stayOpen} infinite never closes. */
    public Door timing(double seconds, double stayOpen) {
        this.seconds = seconds;
        this.stayOpen = stayOpen;
        return this;
    }

    /** Starts opening; does nothing if the door is open or opening. */
    public void open() {
        if (state == State.CLOSED || state == State.CLOSING) state = State.OPENING;
        openFor = 0;
    }

    /**
     * Moves the door on by one step.
     *
     * @param occupied true if something stands in the door's cell: an open door then waits
     */
    public void update(double dt, boolean occupied) {
        switch (state) {
            case OPENING -> {
                open = Math.min(1, open + dt / seconds);
                if (open >= 1) { state = State.OPEN; openFor = 0; }
            }
            case OPEN -> {
                openFor += dt;
                if (openFor >= stayOpen && !occupied) state = State.CLOSING;
            }
            case CLOSING -> {
                if (occupied) { state = State.OPENING; break; }
                open = Math.max(0, open - dt / seconds);
                if (open <= 0) state = State.CLOSED;
            }
            case CLOSED -> { }
        }
    }

    /** How far the door has slid, from 0 (closed) to 1 (open). */
    public double openness() {
        return open;
    }

    public State state() {
        return state;
    }

    /** True if a body fits through: the door is (almost) fully open. */
    public boolean passable() {
        return open >= 0.9;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public boolean alongX() {
        return alongX;
    }

    public Texture face() {
        return face;
    }

    public Texture frame() {
        return frame;
    }
}
// --8<-- [end:door]
