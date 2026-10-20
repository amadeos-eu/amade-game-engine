package eu.amadeos.gameengine.sprite;

import java.awt.Graphics2D;

/**
 * A picture that turns: a cannon that follows its target, an orc that walks wherever the way leads. Seen
 * from above, such things point in every direction, and a sprite cannot turn cheaply: {@link Sprite#draw}
 * under a rotated transform has no cached picture to copy and draws the whole picture anew, in every frame.
 *
 * <p>So the picture is drawn once for each of a number of directions, 32 for example, each a sprite with
 * its own cache, and the nearest direction is shown. At 32 directions the steps are 11.25°; the eye sees a
 * turret turn smoothly, and drawing it costs a copy, as for any sprite. The old arcade games did the same,
 * by hand, with 8 or 16.
 *
 * <pre>
 * Rotations cannon = Rotations.of(32, 20, (g, w, h) -&gt; drawCannon(g, w, h));    // pointing right
 * cannon.draw(g, tower.x(), tower.y(), tower.angle());
 * </pre>
 */
// --8<-- [start:rotations]
public final class Rotations {
    private final Sprite[] sprites;
    private final double size;

    private Rotations(Sprite[] sprites, double size) {
        this.sprites = sprites;
        this.size = size;
    }

    /**
     * {@code steps} sprites of {@code size} × {@code size}, each {@code drawing} turned around the middle by
     * one more step. The drawing points to the right, angle 0, as {@code Vector.ofAngle} does.
     */
    public static Rotations of(int steps, double size, Sprite.Drawing drawing) {
        if (steps < 1) throw new IllegalArgumentException("at least one direction");
        Sprite[] sprites = new Sprite[steps];
        for (int i = 0; i < steps; i++) {
            double angle = 2 * Math.PI * i / steps;
            sprites[i] = Sprite.drawn(size, size, (g, w, h) -> {
                g.rotate(angle, w / 2, h / 2);
                drawing.draw(g, w, h);
            });
        }
        return new Rotations(sprites, size);
    }

    public int steps() {
        return sprites.length;
    }

    /** The direction nearest to {@code angle}, any angle in radians, also negative or above 2π. */
    public int index(double angle) {
        double turns = angle / (2 * Math.PI) * sprites.length;
        return Math.floorMod(Math.round(turns), sprites.length);
    }

    public Sprite at(double angle) {
        return sprites[index(angle)];
    }

    /** Draws the picture nearest to {@code angle} with its middle at {@code x}, {@code y}. */
    public void draw(Graphics2D g, double x, double y, double angle) {
        at(angle).draw(g, x - size / 2, y - size / 2);
    }
}
// --8<-- [end:rotations]
