package eu.amadeos.gameengine.minimap;

import eu.amadeos.gameengine.camera.Camera;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * The whole map, small, in a corner of the screen: one pixel per tile, the things on it as dots, and a box
 * that shows what the camera sees. A click on it moves the camera there; a click with the other button can
 * send units there.
 *
 * <p>The picture is an image of exactly one pixel per tile. The game paints a tile when it changes, which
 * is seldom (spice gone, a building up, fog lifted), and the image is drawn scaled into the minimap's box in
 * every frame. Scaling an image of 64 × 64 pixels into a box of 112 × 112 units is a small copy; drawing 4,096
 * rectangles would not be. The dots are drawn over it in every frame: they move.
 *
 * <pre>
 * Minimap minimap = new Minimap(520, 30, 112, 112, map.width(), map.height(), TILE);
 * minimap.paint(col, row, sand);                 // when a tile changes
 * minimap.render(g);                              // in the overlay
 * minimap.dot(g, unit.x(), unit.y(), BLUE);
 * minimap.view(g, camera);
 * if (minimap.contains(mouse.x(), mouse.y())) camera.moveBy(minimap.worldX(mouse.x()) - ..., ...);
 * </pre>
 */
// --8<-- [start:minimap]
public final class Minimap {
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final int columns;
    private final int rows;
    private final double tile;
    private final BufferedImage image;
    private final int[] pixels;
    private final Rectangle2D.Double rectangle = new Rectangle2D.Double();
    private final BasicStroke thin = new BasicStroke(0.8f);
    private boolean dirty = true;

    /**
     * A minimap that shows a map of {@code columns} × {@code rows} tiles of {@code tile} units inside the
     * box {@code x}, {@code y}, {@code width} × {@code height} of the screen. A map that is not square keeps
     * its proportions and is centred in the box.
     */
    public Minimap(double x, double y, double width, double height, int columns, int rows, double tile) {
        if (columns <= 0 || rows <= 0) throw new IllegalArgumentException("empty map");
        double scale = Math.min(width / columns, height / rows);
        this.width = columns * scale;
        this.height = rows * scale;
        this.x = x + (width - this.width) / 2;
        this.y = y + (height - this.height) / 2;
        this.columns = columns;
        this.rows = rows;
        this.tile = tile;
        this.image = new BufferedImage(columns, rows, BufferedImage.TYPE_INT_RGB);
        this.pixels = new int[columns * rows];
    }

    /** The colour of one tile, {@code 0xRRGGBB}; drawn with the next {@link #render}. */
    public void paint(int col, int row, int rgb) {
        int i = row * columns + col;
        if (pixels[i] == rgb) return;
        pixels[i] = rgb;
        dirty = true;
    }

    /** The map's picture, scaled into the box with hard edges: every tile a little square. */
    public void render(Graphics2D g) {
        if (dirty) {
            image.setRGB(0, 0, columns, rows, pixels, 0, columns);
            dirty = false;
        }
        Object before = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image, (int) Math.round(x), (int) Math.round(y), (int) Math.round(x + width) - (int) Math.round(x),
                (int) Math.round(y + height) - (int) Math.round(y), null);
        if (before != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, before);
    }

    /** A thing at {@code worldX}, {@code worldY}, as a square of {@code size} units at least a tile large. */
    public void dot(Graphics2D g, double worldX, double worldY, double size, Color colour) {
        double s = Math.max(size, width / columns);
        g.setColor(colour);
        rectangle.setRect(screenX(worldX) - s / 2, screenY(worldY) - s / 2, s, s);
        g.fill(rectangle);
    }

    /** A box of the world, such as a building, in its true size. */
    public void box(Graphics2D g, double worldX, double worldY, double worldWidth, double worldHeight, Color colour) {
        g.setColor(colour);
        rectangle.setRect(screenX(worldX), screenY(worldY), Math.max(1, worldWidth / (columns * tile) * width),
                Math.max(1, worldHeight / (rows * tile) * height));
        g.fill(rectangle);
    }

    /** The box of what the camera sees. */
    public void view(Graphics2D g, Camera camera, double alpha, Color colour) {
        double left = screenX(camera.x(alpha));
        double top = screenY(camera.y(alpha));
        double right = screenX(camera.x(alpha) + camera.viewWidth());
        double bottom = screenY(camera.y(alpha) + camera.viewHeight());
        g.setStroke(thin);
        g.setColor(colour);
        rectangle.setRect(left, top, right - left, bottom - top);
        g.draw(rectangle);
    }
    // --8<-- [end:minimap]

    // --8<-- [start:convert]
    /** True if a point of the screen lies on the minimap. */
    public boolean contains(double screenX, double screenY) {
        return screenX >= x && screenX < x + width && screenY >= y && screenY < y + height;
    }

    /** Where in the world a point of the minimap lies, in the world's units. */
    public double worldX(double screenX) {
        return (screenX - x) / width * columns * tile;
    }

    public double worldY(double screenY) {
        return (screenY - y) / height * rows * tile;
    }

    /** Where on the screen a point of the world is shown. */
    public double screenX(double worldX) {
        return x + worldX / (columns * tile) * width;
    }

    public double screenY(double worldY) {
        return y + worldY / (rows * tile) * height;
    }
    // --8<-- [end:convert]

    /** The box on the screen, after keeping the map's proportions. */
    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    /** The colour painted for a tile, for the tests. */
    public int colour(int col, int row) {
        return pixels[row * columns + col];
    }
}
