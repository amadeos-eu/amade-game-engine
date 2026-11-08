package eu.amadeos.gameengine.pinball;

/**
 * The plunger: hold its key and the rod is pulled back against its spring, the longer the further, up to
 * {@code fullPull} seconds; let go and it springs forward and throws the ball lying on its tip up the shooter
 * lane, at a speed between {@code minSpeed} and {@code maxSpeed} by how far it was pulled.
 */
public final class Plunger {
    private final double fullPull;
    private final double minSpeed;
    private final double maxSpeed;
    private double pull;
    private boolean held;
    private double release;

    public Plunger(double fullPull, double minSpeed, double maxSpeed) {
        this.fullPull = fullPull;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
    }

    /**
     * One step with the key {@code down} or not. Returns the speed to give the ball when the plunger was let
     * go in this step, else 0.
     */
    // --8<-- [start:plunger]
    public double update(boolean down, double seconds) {
        double fired = 0;
        if (down) {
            held = true;
            pull = Math.min(1, pull + seconds / fullPull);
        } else if (held) {
            held = false;
            fired = minSpeed + (maxSpeed - minSpeed) * pull * pull;
            release = pull;
        } else {
            // the rod springs back to rest
            pull = Math.max(0, pull - seconds * 12);
        }
        return fired;
    }
    // --8<-- [end:plunger]

    /** How far the rod is pulled back, 0 to 1: for drawing the rod and its spring. */
    public double pull() {
        return pull;
    }

    /** How far the rod was pulled at the last shot. */
    public double lastRelease() {
        return release;
    }
}
