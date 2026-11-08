package eu.amadeos.gameengine.pinball;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The physics of a pinball table: balls rolling down a sloped field, bouncing off walls, posts, bumpers and
 * flippers, rolling over switches, riding ramps and falling into holes.
 *
 * <p>The table is flat, in table units, with x to the right and y down the table towards the player; its
 * slope is just {@code gravity} pulling along +y. Each {@link #step} is cut into {@code substeps} small
 * steps, so that even a fast ball moves less than its radius in one of them and can never pass through a
 * thin wall or a flipper.
 *
 * <p>What happens on the table is told to the game through {@link Events}: which wall, post or bumper was
 * hit and how hard, which switch the ball rolled over, which hole caught it, which ramp it completed. The
 * rules (scores, modes, lights) live in the game; the playfield knows nothing about them.
 *
 * <p>A table has floors: 0 is the playfield, higher numbers raised mini playfields with their own walls and
 * flippers. Every part belongs to one floor, and a ball only meets the parts of its own floor. Ramps lead
 * from one floor to another ({@link Track#exitLayer}).
 */
public final class Playfield {
    /** What the game hears from the table. All methods run on the loop thread, inside {@link #step}. */
    public interface Events {
        /** A wall, post, bumper, target or flipper with this id was hit at this speed (units per second). */
        default void hit(String id, Ball ball, double speed) {
        }

        /** The ball rolled into a switch or spinner zone. */
        default void entered(String zone, Ball ball) {
        }

        /** A hole caught the ball; it stays there until {@link #eject}. */
        default void caught(String hole, Ball ball) {
        }

        /** The ball went up a ramp or wire form. */
        default void climbed(String track, Ball ball) {
        }

        /** The ball reached the end of a ramp. */
        default void rode(String track, Ball ball) {
        }

        /** The ball was too slow and rolled back out of a ramp. */
        default void rolledBack(String track, Ball ball) {
        }

        /** The ball went down the drain below the flippers. */
        default void drained(Ball ball) {
        }
    }

    public static final double STANDARD_GRAVITY = 900;
    /**
     * How much harder a climb pulls than the table's slope: the slope is only about a ninth of real gravity,
     * but going up a ramp works against all of it.
     */
    public static final double CLIMB = 6;

    private final double width;
    private final double drainY;
    private final double gravity;
    private final int substeps;
    private final double maxSpeed;
    private final List<Segment> segments = new ArrayList<>();
    private final List<Disc> discs = new ArrayList<>();
    private final List<Flipper> flippers = new ArrayList<>();
    private final List<Zone> zones = new ArrayList<>();
    private final Map<String, Track> tracks = new HashMap<>();
    private final Map<Integer, Double> layerHeights = new HashMap<>();
    private final List<Ball> balls = new ArrayList<>();
    private final Map<Ball, List<String>> inside = new HashMap<>();
    private Events events = new Events() {
    };

    /** A table {@code width} wide; a ball below {@code drainY} is lost. */
    public Playfield(double width, double drainY, double gravity, int substeps, double maxSpeed) {
        this.width = width;
        this.drainY = drainY;
        this.gravity = gravity;
        this.substeps = substeps;
        this.maxSpeed = maxSpeed;
        layerHeights.put(0, 0.0);
    }

    public void listen(Events events) {
        this.events = events;
    }

    public Segment add(Segment s) {
        segments.add(s);
        return s;
    }

    public Disc add(Disc d) {
        discs.add(d);
        return d;
    }

    public Flipper add(Flipper f) {
        flippers.add(f);
        return f;
    }

    public Zone add(Zone z) {
        zones.add(z);
        return z;
    }

    public Track add(Track t) {
        tracks.put(t.id, t);
        return t;
    }

    public Ball add(Ball b) {
        balls.add(b);
        return b;
    }

    /** A raised floor and its height above the playfield, for drawing its balls. */
    public void layer(int layer, double height) {
        layerHeights.put(layer, height);
    }

    public double layerHeight(int layer) {
        return layerHeights.getOrDefault(layer, 0.0);
    }

    public List<Ball> balls() {
        return Collections.unmodifiableList(balls);
    }

    public List<Segment> segments() {
        return Collections.unmodifiableList(segments);
    }

    public List<Disc> discs() {
        return Collections.unmodifiableList(discs);
    }

    public List<Flipper> flippers() {
        return Collections.unmodifiableList(flippers);
    }

    public List<Zone> zones() {
        return Collections.unmodifiableList(zones);
    }

    public Track track(String id) {
        return tracks.get(id);
    }

    public java.util.Collection<Track> tracks() {
        return Collections.unmodifiableCollection(tracks.values());
    }

    public Zone zone(String id) {
        for (Zone z : zones) if (z.id.equals(id)) return z;
        return null;
    }

    /** Takes a lost ball off the table. */
    public void remove(Ball ball) {
        balls.remove(ball);
        inside.remove(ball);
    }

    /** Every ball still in play: not lost. */
    public int inPlay() {
        int n = 0;
        for (Ball b : balls) if (b.state != Ball.State.LOST && !b.captive) n++;
        return n;
    }

    /** Throws a held ball out of its hole, at this velocity. */
    public void eject(Ball ball, double vx, double vy) {
        if (ball.state != Ball.State.HELD) return;
        Zone hole = zone(ball.holder);
        ball.state = Ball.State.FREE;
        ball.holder = null;
        ball.vx = vx;
        ball.vy = vy;
        // out of the hole at once, so it does not fall straight back in
        double l = Math.max(1e-9, Math.hypot(vx, vy));
        double out = hole == null ? 0 : hole.radius + ball.radius;
        ball.x += vx / l * out;
        ball.y += vy / l * out;
        inside.computeIfAbsent(ball, b -> new ArrayList<>()).add(hole == null ? "" : hole.id);
    }

    /** Puts a ball onto a track at its entrance, as if it had rolled up into it at {@code speed}. */
    public void ride(Ball ball, String trackId, double speed) {
        Track t = tracks.get(trackId);
        ball.state = Ball.State.ON_TRACK;
        ball.track = t;
        ball.along = 0;
        ball.speed = speed;
        ball.x = t.x(0);
        ball.y = t.y(0);
    }

    /** Shakes the table: every free ball gets this push, in units per second. */
    public void nudge(double vx, double vy) {
        for (Ball b : balls) {
            if (b.state != Ball.State.FREE) continue;
            b.vx += vx;
            b.vy += vy;
        }
    }

    // --8<-- [start:step]
    /** Moves everything on the table on by {@code seconds}, in {@code substeps} small steps. */
    public void step(double seconds) {
        double dt = seconds / substeps;
        for (int s = 0; s < substeps; s++) {
            for (Flipper f : flippers) f.turn(dt);
            for (Ball b : new ArrayList<>(balls)) {
                switch (b.state) {
                    case FREE -> roll(b, dt);
                    case ON_TRACK -> ride(b, dt);
                    default -> {
                    }
                }
            }
            collideBalls();
        }
    }

    private void roll(Ball b, double dt) {
        b.vy += gravity * dt;
        pullByMagnets(b, dt);
        double v = Math.hypot(b.vx, b.vy);
        if (v > maxSpeed) {
            b.vx *= maxSpeed / v;
            b.vy *= maxSpeed / v;
        }
        b.x += b.vx * dt;
        b.y += b.vy * dt;
        for (Segment seg : segments) if (!seg.down && seg.layer == b.layer) bounce(b, seg);
        for (Disc d : discs) if (!d.down && d.layer == b.layer) bounce(b, d);
        for (Flipper f : flippers) if (f.layer == b.layer) bounce(b, f);
        if (b.fence != null) fence(b);
        if (b.captive) return;
        zones(b);
        if (b.y - b.radius > drainY && !b.captive) {
            b.state = Ball.State.LOST;
            events.drained(b);
        }
    }
    // --8<-- [end:step]

    // --8<-- [start:bounce]
    /**
     * Bounces the ball off a surface at {@code (cx, cy)}, the nearest point of a part, if the ball overlaps
     * it: pushes the ball out along the normal, reflects the speed towards the surface (relative to the
     * surface's own velocity {@code (sx, sy)}, which only flippers have), takes some of the sliding speed,
     * and adds the part's kick. Returns the speed of the hit, 0 if there was none.
     */
    private double contact(Ball b, double cx, double cy, double reach, Material m, double sx, double sy) {
        double dx = b.x - cx, dy = b.y - cy;
        double d2 = dx * dx + dy * dy;
        double min = b.radius + reach;
        if (d2 >= min * min) return 0;
        double d = Math.sqrt(d2);
        double nx = d == 0 ? 0 : dx / d, ny = d == 0 ? -1 : dy / d;
        b.x += nx * (min - d);
        b.y += ny * (min - d);
        double rvx = b.vx - sx, rvy = b.vy - sy;
        double vn = rvx * nx + rvy * ny;
        if (vn >= 0) return 0; // already moving away
        double tx = rvx - vn * nx, ty = rvy - vn * ny;
        rvx = -vn * m.bounce() * nx + tx * (1 - m.grip());
        rvy = -vn * m.bounce() * ny + ty * (1 - m.grip());
        if (m.kick() > 0 && -vn > m.kickAbove()) {
            rvx += nx * m.kick();
            rvy += ny * m.kick();
        }
        b.vx = rvx + sx;
        b.vy = rvy + sy;
        return -vn;
    }
    // --8<-- [end:bounce]

    private void bounce(Ball b, Segment seg) {
        if (seg.passes != null && b.vx * seg.passes[0] + b.vy * seg.passes[1] > 0) return;
        double[] p = seg.closest(b.x, b.y);
        double hit = contact(b, p[0], p[1], seg.thickness / 2, seg.material, 0, 0);
        if (hit > 0 && seg.id != null) events.hit(seg.id, b, hit);
    }

    private void bounce(Ball b, Disc d) {
        double hit = contact(b, d.x, d.y, d.radius, d.material, 0, 0);
        if (hit > 0 && d.id != null) events.hit(d.id, b, hit);
    }

    private void bounce(Ball b, Flipper f) {
        double[] p = f.closest(b.x, b.y);
        double[] sv = f.surfaceVelocity(p[0], p[1]);
        double hit = contact(b, p[0], p[1], p[2], Material.FLIPPER, sv[0], sv[1]);
        if (hit > 0) events.hit(f.id, b, hit);
    }

    /** Keeps a captive ball in its box: it bounces off the box's sides like off a wall. */
    private void fence(Ball b) {
        double[] f = b.fence;
        if (b.x < f[0] + b.radius) { b.x = f[0] + b.radius; b.vx = Math.abs(b.vx) * 0.5; }
        if (b.x > f[2] - b.radius) { b.x = f[2] - b.radius; b.vx = -Math.abs(b.vx) * 0.5; }
        if (b.y < f[1] + b.radius) { b.y = f[1] + b.radius; b.vy = Math.abs(b.vy) * 0.5; }
        if (b.y > f[3] - b.radius) { b.y = f[3] - b.radius; b.vy = -Math.abs(b.vy) * 0.2; }
    }

    private void pullByMagnets(Ball b, double dt) {
        for (Zone z : zones) {
            if (z.kind != Zone.Kind.MAGNET || !z.active || z.layer != b.layer || !z.contains(b.x, b.y)) continue;
            double dx = z.x - b.x, dy = z.y - b.y;
            b.vx += dx * z.param * dt;
            b.vy += dy * z.param * dt;
            b.vx *= 1 - 4 * dt;
            b.vy *= 1 - 4 * dt;
        }
    }

    private void zones(Ball b) {
        List<String> was = inside.computeIfAbsent(b, k -> new ArrayList<>());
        for (Zone z : zones) {
            if (z.layer != b.layer || z.kind == Zone.Kind.MAGNET) continue;
            boolean in = z.contains(b.x, b.y);
            boolean before = was.contains(z.id);
            if (in && !before) {
                was.add(z.id);
                enter(b, z);
                if (b.state != Ball.State.FREE) return;
            } else if (!in && before) {
                was.remove(z.id);
            }
        }
    }

    private void enter(Ball b, Zone z) {
        if (!z.active) return;
        switch (z.kind) {
            case SWITCH, SPINNER -> events.entered(z.id, b);
            case HOLE -> {
                if (b.captive || Math.hypot(b.vx, b.vy) > z.param) return;
                b.state = Ball.State.HELD;
                b.holder = z.id;
                b.x = z.x;
                b.y = z.y;
                b.vx = 0;
                b.vy = 0;
                events.caught(z.id, b);
            }
            case ENTRY -> {
                double along = b.vx * z.dx + b.vy * z.dy;
                if (along < z.param) return;
                Track t = tracks.get(z.target);
                if (t == null) return;
                b.state = Ball.State.ON_TRACK;
                b.track = t;
                b.along = 0;
                b.speed = along;
                events.climbed(t.id, b);
            }
            default -> {
            }
        }
    }

    // --8<-- [start:ride]
    /** A ball on a ramp: gravity along the table and against the climb, a little rolling drag. */
    private void ride(Ball b, double dt) {
        Track t = b.track;
        double[] dir = t.direction(b.along);
        double slope = dir[2];
        double accel = gravity * dir[1] - gravity * CLIMB * slope - 0.4 * b.speed;
        b.speed += accel * dt;
        b.along += b.speed * dt;
        b.x = t.xAt(b.along);
        b.y = t.yAt(b.along);
        if (b.along >= t.length()) {
            double[] end = t.direction(t.length());
            b.state = Ball.State.FREE;
            b.layer = t.exitLayer;
            b.x = t.xAt(t.length());
            b.y = t.yAt(t.length());
            b.vx = end[0] * b.speed;
            b.vy = end[1] * b.speed;
            b.track = null;
            events.rode(t.id, b);
        } else if (b.along <= 0 && b.speed <= 0) {
            double[] start = t.direction(0);
            b.state = Ball.State.FREE;
            b.x = t.x(0) - start[0] * (b.radius + 2);
            b.y = t.y(0) - start[1] * (b.radius + 2);
            b.vx = start[0] * b.speed;
            b.vy = start[1] * b.speed;
            b.track = null;
            events.rolledBack(t.id, b);
        }
    }
    // --8<-- [end:ride]

    /** Balls on the same floor bounce off each other like billiard balls of equal weight. */
    private void collideBalls() {
        for (int i = 0; i < balls.size(); i++) {
            Ball a = balls.get(i);
            if (a.state != Ball.State.FREE) continue;
            for (int j = i + 1; j < balls.size(); j++) {
                Ball c = balls.get(j);
                if (c.state != Ball.State.FREE || c.layer != a.layer && !c.captive && !a.captive) continue;
                double dx = c.x - a.x, dy = c.y - a.y;
                double min = a.radius + c.radius;
                double d2 = dx * dx + dy * dy;
                if (d2 >= min * min || d2 == 0) continue;
                double d = Math.sqrt(d2);
                double nx = dx / d, ny = dy / d;
                double push = (min - d) / 2;
                a.x -= nx * push;
                a.y -= ny * push;
                c.x += nx * push;
                c.y += ny * push;
                double vn = (c.vx - a.vx) * nx + (c.vy - a.vy) * ny;
                if (vn >= 0) continue;
                double j2 = -vn * 0.95;
                a.vx -= nx * j2;
                a.vy -= ny * j2;
                c.vx += nx * j2;
                c.vy += ny * j2;
                events.hit(c.captive ? "captive" : "ball", c, -vn);
            }
        }
    }

    public double width() {
        return width;
    }

    public double gravity() {
        return gravity;
    }
}
