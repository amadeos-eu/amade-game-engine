package eu.amadeos.gameengine.iso;

import java.awt.geom.AffineTransform;

/**
 * The isometric view of a tile map: square tiles seen from a corner, so that each tile becomes a diamond
 * twice as wide as high, the classic 2:1 view of city builders and strategy games.
 *
 * <p>The game keeps its world flat and square: a tile is {@code tile} units a side, {@code x} runs along the
 * columns and {@code y} along the rows, as in every other map of the engine. Only the drawing turns it. The
 * projection takes a point of the world, with a height above the ground, to a point of the <i>picture</i>,
 * the plane in which the whole map is drawn, and back:
 *
 * <pre>
 * picture x = originX + (x - y) · halfWidth / tile
 * picture y = originY + (x + y) · halfHeight / tile - height
 * </pre>
 *
 * <p>The way back works for points on the ground; that is how a mouse click finds its tile. Heights are
 * in the picture's units, so a wall of 20 is 20 units tall on the screen however the tiles are sized.
 *
 * <p>The projection is also three {@link AffineTransform}s. A game that draws a roof, a lawn or a car park
 * in world coordinates (a rectangle of 32 × 16 units) puts {@link #ground(double)} on the graphics and gets
 * the diamond for free; a game that draws a wall with its windows and its door puts {@link #frontWall}
 * or {@link #sideWall} on the graphics and draws the wall as if it faced the viewer. Java2D then shears
 * the windows into the wall.
 *
 * <pre>
 * IsoProjection iso = IsoProjection.forMap(64, 64, 16, 32, 16, 120);   // room above the map for towers
 * Camera camera = new Camera(512, 380, iso.width(), iso.height());      // the camera moves over the picture
 * int col = iso.col(mouseX + camera.x(), mouseY + camera.y());          // the tile under the mouse
 * g.transform(iso.ground(0));  g.fill(new Rectangle2D.Double(0, 0, 16, 16));   // tile (0, 0) as a diamond
 * </pre>
 *
 * <p>No AWT beyond {@code AffineTransform}: the arithmetic is tested on its own.
 */
// --8<-- [start:projection]
public final class IsoProjection {
    private final double tile;
    private final double halfWidth;
    private final double halfHeight;
    private final double originX;
    private final double originY;
    private final int columns;
    private final int rows;
    private final double above;

    private IsoProjection(double tile, double tileWidth, double tileHeight, double originX, double originY,
                          int columns, int rows, double above) {
        if (tile <= 0 || tileWidth <= 0 || tileHeight <= 0) throw new IllegalArgumentException("empty tile");
        this.tile = tile;
        this.halfWidth = tileWidth / 2;
        this.halfHeight = tileHeight / 2;
        this.originX = originX;
        this.originY = originY;
        this.columns = columns;
        this.rows = rows;
        this.above = above;
    }

    /**
     * The projection of a map of {@code columns} × {@code rows} tiles of {@code tile} world units, each
     * drawn as a diamond of {@code tileWidth} × {@code tileHeight}, placed so that the whole map lies in the
     * picture from (0, 0) to ({@link #width()}, {@link #height()}). The top corner of the map, tile (0, 0),
     * sits {@code above} units below the top, so that the tallest building on it still fits.
     */
    public static IsoProjection forMap(int columns, int rows, double tile, double tileWidth, double tileHeight,
                                       double above) {
        if (columns <= 0 || rows <= 0) throw new IllegalArgumentException("empty map");
        return new IsoProjection(tile, tileWidth, tileHeight, rows * tileWidth / 2, above, columns, rows, above);
    }

    /**
     * A projection with its world origin at {@code originX}, {@code originY} of the picture and no map
     * around it: for drawing one building into a picture of its own, with (0, 0) at its top corner.
     */
    public static IsoProjection at(double tile, double tileWidth, double tileHeight, double originX, double originY) {
        return new IsoProjection(tile, tileWidth, tileHeight, originX, originY, 0, 0, 0);
    }

    /** Where a point of the world, {@code height} above the ground, lies in the picture. */
    public double pictureX(double x, double y) {
        return originX + (x - y) * halfWidth / tile;
    }

    public double pictureY(double x, double y, double height) {
        return originY + (x + y) * halfHeight / tile - height;
    }

    /** The point of the ground that lies at a point of the picture: the way back, for the mouse. */
    public double worldX(double pictureX, double pictureY) {
        double across = (pictureX - originX) / halfWidth * tile;   // x - y
        double down = (pictureY - originY) / halfHeight * tile;    // x + y
        return (down + across) / 2;
    }

    public double worldY(double pictureX, double pictureY) {
        double across = (pictureX - originX) / halfWidth * tile;
        double down = (pictureY - originY) / halfHeight * tile;
        return (down - across) / 2;
    }

    /** The column and row of the tile whose diamond holds a point of the picture; may lie off the map. */
    public int col(double pictureX, double pictureY) {
        return (int) Math.floor(worldX(pictureX, pictureY) / tile);
    }

    public int row(double pictureX, double pictureY) {
        return (int) Math.floor(worldY(pictureX, pictureY) / tile);
    }
    // --8<-- [end:projection]

    // --8<-- [start:transforms]
    /**
     * Draws the world flat at {@code height} above the ground: a rectangle in world units becomes a diamond.
     * Floors, lawns, roofs and the tops of boxes.
     */
    public AffineTransform ground(double height) {
        return new AffineTransform(halfWidth / tile, halfHeight / tile, -halfWidth / tile, halfHeight / tile,
                originX, originY - height);
    }

    /**
     * Draws the wall that runs along {@code x} and faces the viewer on the left, from the ground corner
     * ({@code x}, {@code y}) onwards. In the wall's own coordinates {@code u} is the distance along the wall
     * in world units and {@code v} the height in picture units, negative upwards: the wall is the rectangle
     * (0, -height) to (length, 0), drawn as if seen from the front.
     */
    public AffineTransform frontWall(double x, double y) {
        return new AffineTransform(halfWidth / tile, halfHeight / tile, 0, 1, pictureX(x, y), pictureY(x, y, 0));
    }

    /**
     * Draws the wall that runs along {@code y} and faces the viewer on the right, from its front ground
     * corner ({@code x}, {@code y}) back towards smaller {@code y}: {@code u} runs from the front corner
     * away from the viewer, {@code v} as for {@link #frontWall}.
     */
    public AffineTransform sideWall(double x, double y) {
        return new AffineTransform(halfWidth / tile, -halfHeight / tile, 0, 1, pictureX(x, y), pictureY(x, y, 0));
    }
    // --8<-- [end:transforms]

    // --8<-- [start:visible]
    /**
     * The tiles whose diamonds may show in a rectangle of the picture, as the smallest and largest column
     * and row, kept on the map: {@code {colMin, rowMin, colMax, rowMax}}. A frame draws only these. The box
     * reaches {@code margin} tiles further down, for things that stand on tiles below the view but are tall
     * enough to reach into it.
     */
    public int[] visible(double left, double top, double width, double height, int margin) {
        double right = left + width;
        double bottom = top + height + margin * 2 * halfHeight;
        int colMin = (int) Math.floor(worldX(left, top) / tile);
        int colMax = (int) Math.floor(worldX(right, bottom) / tile);
        int rowMin = (int) Math.floor(worldY(right, top) / tile);
        int rowMax = (int) Math.floor(worldY(left, bottom) / tile);
        return new int[]{Math.max(0, colMin - 1), Math.max(0, rowMin - 1),
                Math.min(columns - 1, colMax + 1), Math.min(rows - 1, rowMax + 1)};
    }
    // --8<-- [end:visible]

    /** The size of the picture of the whole map, with the room above it. */
    public double width() {
        return (columns + rows) * halfWidth;
    }

    public double height() {
        return (columns + rows) * halfHeight + above;
    }

    public double tile() {
        return tile;
    }

    public double tileWidth() {
        return halfWidth * 2;
    }

    public double tileHeight() {
        return halfHeight * 2;
    }

    public double originX() {
        return originX;
    }

    public double originY() {
        return originY;
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }
}
