package eu.amadeos.gameengine.pinball;

/**
 * A round place on a floor of the table that the ball rolls over rather than bounces off: a rollover switch
 * in a lane, a spinner, a hole that catches a slow ball (a saucer, a scoop, a lock), a magnet, the entrance
 * of a ramp, the drain. The {@link Playfield} tells the game when a ball enters one, by its {@code id}.
 *
 * <ul>
 *   <li>{@link Kind#SWITCH}: reports each entry (rollovers, lane switches, an outlane for the kickback).</li>
 *   <li>{@link Kind#SPINNER}: reports each entry with the ball's speed, from which the game counts the turns
 *       of the spinner's plate.</li>
 *   <li>{@link Kind#HOLE}: catches a ball slower than {@code param} units per second and holds it at the
 *       zone's centre until the game ejects it.</li>
 *   <li>{@link Kind#MAGNET}: while {@link #active}, pulls balls inside towards the centre with an
 *       acceleration of {@code param}.</li>
 *   <li>{@link Kind#ENTRY}: the mouth of a {@link Track}; a ball moving into it along {@code (dx, dy)} faster
 *       than {@code param} goes up the track named {@code target}.</li>
 * </ul>
 */
public final class Zone {
    public enum Kind { SWITCH, SPINNER, HOLE, MAGNET, ENTRY }

    public final String id;
    public final Kind kind;
    public final double x, y, radius;
    public final int layer;
    public final double param;
    public final double dx, dy;
    public final String target;
    /** A magnet pulls only while active; an entry or hole can be switched off by the game (a closed diverter). */
    public boolean active = true;

    public Zone(String id, Kind kind, double x, double y, double radius, int layer, double param) {
        this(id, kind, x, y, radius, layer, param, 0, 0, null);
    }

    public Zone(String id, Kind kind, double x, double y, double radius, int layer, double param, double dx, double dy,
                String target) {
        this.id = id;
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.layer = layer;
        this.param = param;
        double l = Math.hypot(dx, dy);
        this.dx = l == 0 ? 0 : dx / l;
        this.dy = l == 0 ? 0 : dy / l;
        this.target = target;
        if (kind == Kind.MAGNET) active = false;
    }

    public boolean contains(double px, double py) {
        double ddx = px - x, ddy = py - y;
        return ddx * ddx + ddy * ddy <= radius * radius;
    }
}
