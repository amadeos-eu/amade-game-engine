package eu.amadeos.gameengine.physics;

/**
 * The flight of a {@link Projectile}: gravity pulls it down, the wind pushes it sideways, the air slows it.
 * One {@link #step} moves it for one step of the game and looks along the way for something it hits;
 * {@link #fly} does the same for a whole flight at once, which is how a computer player tries a shot
 * before it fires.
 *
 * <p>Both use exactly the same arithmetic. So a shot tried with {@code fly} lands exactly where the same
 * shot fired with {@code step} will land, in the same steps: the computer's aim can be as good as its
 * search, and an error it makes is one the game gave it on purpose.
 *
 * <p>A shell moves several units in a step, more than the earth it might hit is thick. So a step does not
 * only look at where the shell arrives; it looks at points along the way, {@code sample} units apart, and
 * stops at the first that is solid.
 *
 * <pre>
 * Ballistics air = new Ballistics(GRAVITY, wind, 0, 0.5);
 * Ballistics.Hit hit = air.step(shell, seconds, ground::solid);
 * if (hit != null) explode(hit.x(), hit.y());
 * </pre>
 */
// --8<-- [start:ballistics]
public final class Ballistics {
    /** What a projectile can hit: earth, a tank, a wall. */
    @FunctionalInterface
    public interface Solid {
        boolean at(double x, double y);
    }

    /** Where a projectile hit something. */
    public record Hit(double x, double y) {
    }

    /** How a whole flight ended. */
    public enum Ending { HIT, LEFT_THE_FIELD, TIME_UP }

    /** A flight from {@link #fly}: how it ended, where, and after how many seconds. */
    public record Flight(Ending ending, double x, double y, double seconds) {
    }

    /** A flight leaves the field beside {@code left} or {@code right}, or below {@code bottom}; above it, it may come back. */
    public record Field(double left, double right, double bottom) {
        public boolean contains(double x, double y) {
            return x >= left && x <= right && y <= bottom;
        }
    }

    private final double gravity;
    private final double wind;
    private final double drag;
    private final double sample;

    /**
     * {@code gravity} and {@code wind} as accelerations in units per second per second (the wind positive to
     * the right), {@code drag} as the part of its speed a projectile loses in a second (0 for none), and
     * {@code sample} the distance in units between the points a step looks at.
     */
    public Ballistics(double gravity, double wind, double drag, double sample) {
        if (sample <= 0) throw new IllegalArgumentException("sample must be positive");
        if (drag < 0 || drag >= 1) throw new IllegalArgumentException("drag is 0 to below 1");
        this.gravity = gravity;
        this.wind = wind;
        this.drag = drag;
        this.sample = sample;
    }

    /** The same air with another wind. */
    public Ballistics withWind(double wind) {
        return new Ballistics(gravity, wind, drag, sample);
    }

    /**
     * Moves the projectile for {@code seconds}: first the speed changes, then it moves with the new speed.
     * Returns the first solid point on the way, with the projectile placed there, or {@code null}.
     */
    public Hit step(Projectile p, double seconds, Solid solid) {
        p.remember();
        p.vx += wind * seconds;
        p.vy += gravity * seconds;
        if (drag > 0) {
            double keep = Math.pow(1 - drag, seconds);
            p.vx *= keep;
            p.vy *= keep;
        }
        double fromX = p.x, fromY = p.y;
        double toX = p.x + p.vx * seconds, toY = p.y + p.vy * seconds;
        int points = Math.max(1, (int) Math.ceil(Math.hypot(toX - fromX, toY - fromY) / sample));
        for (int i = 1; i <= points; i++) {
            double t = (double) i / points;
            double x = fromX + (toX - fromX) * t, y = fromY + (toY - fromY) * t;
            if (solid.at(x, y)) {
                p.x = x;
                p.y = y;
                return new Hit(x, y);
            }
        }
        p.x = toX;
        p.y = toY;
        return null;
    }
    // --8<-- [end:ballistics]

    // --8<-- [start:fly]
    /**
     * A whole flight on a copy of {@code start}, in steps of {@code seconds}: until it hits something, leaves
     * the {@code field}, or {@code maxSeconds} have passed. The projectile given is not moved.
     */
    public Flight fly(Projectile start, double seconds, double maxSeconds, Field field, Solid solid) {
        Projectile p = start.copy();
        double time = 0;
        while (time < maxSeconds) {
            Hit hit = step(p, seconds, solid);
            time += seconds;
            if (hit != null) return new Flight(Ending.HIT, hit.x(), hit.y(), time);
            if (!field.contains(p.x, p.y)) return new Flight(Ending.LEFT_THE_FIELD, p.x, p.y, time);
        }
        return new Flight(Ending.TIME_UP, p.x, p.y, time);
    }
    // --8<-- [end:fly]

    public double gravity() {
        return gravity;
    }

    public double wind() {
        return wind;
    }

    public double drag() {
        return drag;
    }
}
