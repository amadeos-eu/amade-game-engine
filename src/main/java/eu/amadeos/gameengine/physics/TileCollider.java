package eu.amadeos.gameengine.physics;

/**
 * Moves {@link Body bodies} through a world of tiles and stops them where the tiles are solid: the ground
 * a player stands on, the walls they run into, the ceiling they bump their head on, and one-way platforms
 * that hold them only from above.
 *
 * <p>The trick of every platformer since the 1980s: move <b>one axis at a time</b>. First sideways, and
 * if a wall is in the way, stop at it; then up or down, and if the ground is in the way, stand on it.
 * Moved together, a body that falls diagonally onto the corner of a tile cannot tell whether it hit a
 * wall or landed; moved apart, each axis has one answer.
 *
 * <p>Each move looks at <i>every</i> column or row the edge of the body passes, not only at where it
 * ends up. A body falling 20 units in a step through tiles of 16 cannot fall through a floor however fast
 * it is. The world asks the game what a tile is, so the tiles can be anything: a {@code TileMap}, a level
 * from the Tiled editor, a function. What lies outside the map is the game's choice too: usually walls at
 * the sides and air above and below, so that a player can fall out of the world.
 *
 * <pre>
 * TileCollider world = new TileCollider((col, row) -> level.solidity(col, row), 16, 16);
 * // in each update step:
 * player.fall(GRAVITY, MAX_FALL, seconds);
 * world.move(player, seconds, dropping);
 * if (player.onGround) ...
 * </pre>
 */
// --8<-- [start:collider]
public final class TileCollider {
    /** What the tile at {@code col}, {@code row} is; asked also for tiles outside the map. */
    @FunctionalInterface
    public interface Tiles {
        Solidity at(int col, int row);
    }

    /** Visits one tile. */
    @FunctionalInterface
    public interface CellVisitor {
        void visit(int col, int row);
    }

    /** Edges that lie exactly on a tile border belong to the tile before it, not to the one after. */
    private static final double EPSILON = 1e-6;

    private final Tiles tiles;
    private final double tileWidth;
    private final double tileHeight;

    public TileCollider(Tiles tiles, double tileWidth, double tileHeight) {
        this.tiles = tiles;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
    }

    /**
     * Moves the body by its velocity for {@code seconds}: sideways, then up or down. Afterwards its contact
     * flags say what it touched, and the velocity towards what it touched is 0.
     *
     * @param dropThrough true to fall through one-way platforms, e.g. while the player holds down
     */
    public void move(Body body, double seconds, boolean dropThrough) {
        body.remember();
        body.onGround = body.hitCeiling = body.hitLeft = body.hitRight = body.onPlatform = false;
        moveX(body, body.vx * seconds);
        moveY(body, body.vy * seconds, dropThrough);
        if (!body.onGround && body.vy >= 0) standing(body, dropThrough);
    }

    private void moveX(Body body, double dx) {
        int top = row(body.y);
        int bottom = row(body.bottom() - EPSILON);
        if (dx > 0) {
            // Every column the right edge enters, nearest first.
            for (int col = col(body.right() - EPSILON) + 1; col <= col(body.right() + dx - EPSILON); col++) {
                if (solidInColumn(col, top, bottom)) {
                    body.x = col * tileWidth - body.width;
                    body.vx = 0;
                    body.hitRight = true;
                    return;
                }
            }
        } else if (dx < 0) {
            for (int col = col(body.x) - 1; col >= col(body.x + dx); col--) {
                if (solidInColumn(col, top, bottom)) {
                    body.x = (col + 1) * tileWidth;
                    body.vx = 0;
                    body.hitLeft = true;
                    return;
                }
            }
        }
        body.x += dx;
    }

    private void moveY(Body body, double dy, boolean dropThrough) {
        int left = col(body.x);
        int right = col(body.right() - EPSILON);
        if (dy > 0) {
            // Every row the feet enter. A platform holds only from above, and the feet come from above
            // whenever they enter its row from the row before.
            for (int row = row(body.bottom() - EPSILON) + 1; row <= row(body.bottom() + dy - EPSILON); row++) {
                boolean solid = solidInRow(row, left, right);
                boolean platform = !dropThrough && platformInRow(row, left, right);
                if (solid || platform) {
                    body.y = row * tileHeight - body.height;
                    body.vy = 0;
                    body.onGround = true;
                    body.onPlatform = !solid;
                    return;
                }
            }
        } else if (dy < 0) {
            for (int row = row(body.y) - 1; row >= row(body.y + dy); row--) {
                if (solidInRow(row, left, right)) {
                    body.y = (row + 1) * tileHeight;
                    body.vy = 0;
                    body.hitCeiling = true;
                    return;
                }
            }
        }
        body.y += dy;
    }
    // --8<-- [end:collider]

    /** A body that did not move down may still stand: its feet exactly on top of ground or a platform. */
    private void standing(Body body, boolean dropThrough) {
        double rows = body.bottom() / tileHeight;
        if (Math.abs(rows - Math.round(rows)) * tileHeight > EPSILON) return;
        int row = (int) Math.round(rows);
        int left = col(body.x);
        int right = col(body.right() - EPSILON);
        boolean solid = solidInRow(row, left, right);
        if (solid || !dropThrough && platformInRow(row, left, right)) {
            body.onGround = true;
            body.onPlatform = !solid;
        }
    }

    /** What lies at a point of the world, in the units of the tiles. */
    public Solidity at(double x, double y) {
        return tiles.at(col(x), row(y));
    }

    /** True if the body could stand where it is: ground or a platform right under its feet, at {@code x}. */
    public boolean groundAt(double x, Body body) {
        double feet = body.bottom() + EPSILON;
        return at(x, feet) != Solidity.EMPTY;
    }

    /** Visits every tile the body overlaps, row by row: for spikes, coins, water. */
    public void forEachCell(Body body, CellVisitor visitor) {
        for (int row = row(body.y); row <= row(body.bottom() - EPSILON); row++) {
            for (int col = col(body.x); col <= col(body.right() - EPSILON); col++) visitor.visit(col, row);
        }
    }

    private boolean solidInColumn(int col, int top, int bottom) {
        for (int row = top; row <= bottom; row++) {
            if (tiles.at(col, row) == Solidity.SOLID) return true;
        }
        return false;
    }

    private boolean solidInRow(int row, int left, int right) {
        for (int col = left; col <= right; col++) {
            if (tiles.at(col, row) == Solidity.SOLID) return true;
        }
        return false;
    }

    private boolean platformInRow(int row, int left, int right) {
        for (int col = left; col <= right; col++) {
            if (tiles.at(col, row) == Solidity.PLATFORM) return true;
        }
        return false;
    }

    private int col(double x) {
        return (int) Math.floor(x / tileWidth);
    }

    private int row(double y) {
        return (int) Math.floor(y / tileHeight);
    }

    public double tileWidth() {
        return tileWidth;
    }

    public double tileHeight() {
        return tileHeight;
    }
}
