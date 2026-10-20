package eu.amadeos.gameengine.fog;

import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * What one side knows of the map. Every tile is in one of three states:
 * <ul>
 *   <li><b>unseen</b>: nobody of this side has ever looked there. It is black: the <i>shroud</i>.</li>
 *   <li><b>seen</b>: someone looked once, but nobody looks now. The ground is known, what moves on it is not:
 *   it is dimmed, and enemies there are not shown. That is the <i>fog of war</i> proper.</li>
 *   <li><b>visible</b>: a unit or a building of this side sees it now.</li>
 * </ul>
 * Each step, the game calls {@link #begin()}, then {@link #look} for everything of the side that sees, then
 * {@link #end()}. Looking marks the tiles within the radius as visible and as seen for ever. The circles
 * are worked out once per radius and kept: a hundred units looking cost a hundred loops over a few dozen
 * tiles each, no square roots.
 *
 * <p>{@link #version()} changes when any tile changes its state, so the picture of the fog and the minimap
 * are drawn again only then, and not in every step.
 *
 * <pre>
 * fog.begin();
 * for (Unit unit : side.units()) fog.look(unit.col(), unit.row(), unit.type().sight());
 * fog.end();
 * if (fog.visible(enemy.col(), enemy.row())) draw(enemy);
 * </pre>
 */
// --8<-- [start:fog]
public final class FogOfWar {
    public enum State { UNSEEN, SEEN, VISIBLE }

    private final int width;
    private final int height;
    private final boolean[] seen;
    /** The step in which a tile was last looked at; it is visible if that is the current step. */
    private final int[] lookedAt;
    /** What {@link #lookedAt} said at the end of the step before, to find the tiles that changed. */
    private final boolean[] wasVisible;
    private final Map<Integer, int[]> circles = new HashMap<>();
    private int step = 1;
    private int version;
    private boolean everything;

    public FogOfWar(int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("empty map");
        this.width = width;
        this.height = height;
        this.seen = new boolean[width * height];
        this.lookedAt = new int[width * height];
        this.wasVisible = new boolean[width * height];
    }

    /** A new step: what was visible is visible only if someone looks again. */
    public void begin() {
        step++;
    }

    /** Everything within {@code radius} tiles of the middle of {@code col}, {@code row} is visible now. */
    public void look(int col, int row, int radius) {
        for (int offset : circle(radius)) {
            // An offset packs the column and the row into one int: 16 bits each, with a sign.
            int c = col + (short) (offset >> 16);
            int r = row + (short) offset;
            if (c < 0 || r < 0 || c >= width || r >= height) continue;
            int i = r * width + c;
            lookedAt[i] = step;
            if (!seen[i]) {
                seen[i] = true;
                version++;
            }
        }
    }

    /** The end of a step: tiles that came into sight or went out of it change the version. */
    public void end() {
        for (int i = 0; i < lookedAt.length; i++) {
            boolean now = lookedAt[i] == step;
            if (now != wasVisible[i]) {
                wasVisible[i] = now;
                version++;
            }
        }
    }

    /** The tiles of a circle, as offsets from its middle tile: those whose middles lie within the radius. */
    private int[] circle(int radius) {
        return circles.computeIfAbsent(radius, r -> {
            int[] offsets = new int[(2 * r + 1) * (2 * r + 1)];
            int n = 0;
            for (int dr = -r; dr <= r; dr++) {
                for (int dc = -r; dc <= r; dc++) {
                    // A little more than r, so the circle of 1 is the tile and its four sides and corners.
                    if (dc * dc + dr * dr <= r * r + r) offsets[n++] = (dc << 16) | (dr & 0xFFFF);
                }
            }
            return Arrays.copyOf(offsets, n);
        });
    }
    // --8<-- [end:fog]

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean contains(int col, int row) {
        return col >= 0 && row >= 0 && col < width && row < height;
    }

    /** True if the side sees the tile now; outside the map, never. */
    public boolean visible(int col, int row) {
        return contains(col, row) && (everything || wasVisible[row * width + col]);
    }

    /** True if the side has ever seen the tile. */
    public boolean seen(int col, int row) {
        return contains(col, row) && (everything || seen[row * width + col]);
    }

    public State state(int col, int row) {
        return visible(col, row) ? State.VISIBLE : seen(col, row) ? State.SEEN : State.UNSEEN;
    }

    /** Changes whenever a tile changes its state. */
    public int version() {
        return version;
    }

    /** Shows the whole map as visible, as a cheat or for a game that has ended; {@code false} undoes it. */
    public void revealAll(boolean on) {
        if (everything != on) version++;
        everything = on;
    }

    public boolean revealsAll() {
        return everything;
    }

    // --8<-- [start:save]
    /**
     * The tiles seen so far, for a save file: one bit per tile, as Base64 text, 700 characters for a map
     * of 64 × 64. What is visible needs no saving: the next step looks again.
     */
    public String seenAsText() {
        byte[] bits = new byte[(seen.length + 7) / 8];
        for (int i = 0; i < seen.length; i++) if (seen[i]) bits[i / 8] |= (byte) (1 << (i % 8));
        return Base64.getEncoder().encodeToString(bits);
    }

    /** Takes back what {@link #seenAsText()} wrote; an {@link IllegalArgumentException} if it does not fit. */
    public void restoreSeen(String text) {
        byte[] bits = Base64.getDecoder().decode(text);
        if (bits.length != (seen.length + 7) / 8) {
            throw new IllegalArgumentException("seen tiles of another map size: " + bits.length + " bytes");
        }
        for (int i = 0; i < seen.length; i++) seen[i] = (bits[i / 8] & (1 << (i % 8))) != 0;
        version++;
    }
    // --8<-- [end:save]
}
