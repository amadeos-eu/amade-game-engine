package eu.amadeos.gameengine.adventure;

import eu.amadeos.gameengine.math.Vector;
import eu.amadeos.gameengine.tile.Direction;

import java.util.List;
import java.util.Objects;

/**
 * Something in a room the player can point at: the psalter on the bench, the window, the door. The mouse
 * over it shows its name, a click on it makes the hero walk to its {@code stand} point, turn to
 * {@code facing} and then do something with it, as the room's script says.
 *
 * <p>An exit is a hotspot that leads to {@code exitTo}, a room, arriving at its spot {@code exitSpot}; a
 * click on it walks there and leaves, unless the script says otherwise.
 *
 * @param id     the name in scripts, and the key of its name in the game's texts
 * @param area   the corners of the outline on the screen, at least three
 * @param stand  where the hero stands to use it
 * @param facing where the hero looks while using it
 */
public record Hotspot(String id, List<Vector> area, Vector stand, Direction facing, String exitTo, String exitSpot) {
    public Hotspot {
        Objects.requireNonNull(id, "id");
        if (area.size() < 3) throw new IllegalArgumentException(id + ": an area needs at least 3 corners");
        area = List.copyOf(area);
        Objects.requireNonNull(stand, "stand");
        Objects.requireNonNull(facing, "facing");
    }

    /** A hotspot that is no exit. */
    public Hotspot(String id, List<Vector> area, Vector stand, Direction facing) {
        this(id, area, stand, facing, null, null);
    }

    /** A rectangle as an area: left, top, width, height. */
    public static List<Vector> rectangle(double x, double y, double width, double height) {
        return List.of(new Vector(x, y), new Vector(x + width, y), new Vector(x + width, y + height),
                new Vector(x, y + height));
    }

    public boolean isExit() {
        return exitTo != null;
    }

    /** Whether the point is inside the area (even-odd rule; the edge belongs to it). */
    public boolean contains(Vector p) {
        boolean in = false;
        for (int i = 0, j = area.size() - 1; i < area.size(); j = i++) {
            Vector a = area.get(i), b = area.get(j);
            if (WalkArea.closest(a, b, p).distance(p) < 1e-9) return true;
            if ((a.y() > p.y()) != (b.y() > p.y())
                    && p.x() < (b.x() - a.x()) * (p.y() - a.y()) / (b.y() - a.y()) + a.x()) in = !in;
        }
        return in;
    }
}
