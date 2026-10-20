package eu.amadeos.gameengine.iso;

import eu.amadeos.gameengine.camera.Camera;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * The minimap of an isometric map: the whole map as a small diamond, turned like the view, so that north in
 * the view is north on the minimap and the camera's view is a plain box on it.
 *
 * <p>It is the picture of the {@link IsoProjection}, scaled down: a point of the picture is the same point of
 * the minimap, only smaller. So the camera, which moves over the picture, is a box of the minimap, and a
 * click on the minimap is a point of the picture to move the camera to. The tiles are an image of one pixel
 * per tile, as in the flat {@link eu.amadeos.gameengine.minimap.Minimap}, drawn through a transform that
 * turns each pixel into a little diamond.
 *
 * <pre>
 * DiamondMinimap minimap = new DiamondMinimap(iso, 516, 290, 120);
 * minimap.paint(col, row, ZONE_GREEN);              // when a tile changes
 * minimap.render(g);                                // in the overlay
 * minimap.view(g, camera, alpha, Color.WHITE);
 * if (minimap.contains(x, y)) camera.jumpTo(minimap.pictureX(x), minimap.pictureY(y));
 * </pre>
 */
// --8<-- [start:minimap]
public final class DiamondMinimap {
    private final IsoProjection iso;
    private final double x;
    private final double y;
    private final double scale;
    private final int columns;
    private final int rows;
    private final BufferedImage image;
    private final int[] pixels;
    private final AffineTransform tiles;
    private final Path2D.Double outline = new Path2D.Double();
    private final Rectangle2D.Double rectangle = new Rectangle2D.Double();
    private final BasicStroke thin = new BasicStroke(0.8f);
    private boolean dirty = true;

    /**
     * The minimap of the map of {@code iso}, {@code width} wide with its top left corner at {@code x},
     * {@code y} of the screen; its height follows from the map. The room above the map is left out.
     */
    public DiamondMinimap(IsoProjection iso, double x, double y, double width) {
        if (iso.columns() <= 0) throw new IllegalArgumentException("the projection has no map");
        this.iso = iso;
        this.columns = iso.columns();
        this.rows = iso.rows();
        this.scale = width / iso.width();
        this.x = x;
        this.y = y - iso.originY() * scale;
        this.image = new BufferedImage(columns, rows, BufferedImage.TYPE_INT_ARGB);
        this.pixels = new int[columns * rows];
        // Pixel (col, row) of the image is tile (col, row): the projection's ground, scaled into the box.
        double t = iso.tile();
        AffineTransform place = new AffineTransform(scale, 0, 0, scale, this.x, this.y);
        place.concatenate(iso.ground(0));
        place.concatenate(AffineTransform.getScaleInstance(t, t));
        this.tiles = place;
        outline.moveTo(screenX(0, 0), screenY(0, 0));
        outline.lineTo(screenX(columns * t, 0), screenY(columns * t, 0));
        outline.lineTo(screenX(columns * t, rows * t), screenY(columns * t, rows * t));
        outline.lineTo(screenX(0, rows * t), screenY(0, rows * t));
        outline.closePath();
    }

    /** The colour of one tile, {@code 0xRRGGBB}; drawn with the next {@link #render}. */
    public void paint(int col, int row, int rgb) {
        int i = row * columns + col;
        int argb = 0xFF000000 | rgb;
        if (pixels[i] == argb) return;
        pixels[i] = argb;
        dirty = true;
    }

    /** The map, a diamond of little diamonds with hard edges. */
    public void render(Graphics2D g) {
        if (dirty) {
            image.setRGB(0, 0, columns, rows, pixels, 0, columns);
            dirty = false;
        }
        Object before = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image, tiles, null);
        if (before != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, before);
    }

    /** The diamond's edge, for a frame around it. */
    public Path2D.Double outline() {
        return outline;
    }

    /** A thing at a point of the world, as a square of {@code size} screen units. */
    public void dot(Graphics2D g, double worldX, double worldY, double size, Color colour) {
        g.setColor(colour);
        rectangle.setRect(screenX(worldX, worldY) - size / 2, screenY(worldX, worldY) - size / 2, size, size);
        g.fill(rectangle);
    }

    /** The box of what the camera sees; the camera moves over the projection's picture. */
    public void view(Graphics2D g, Camera camera, double alpha, Color colour) {
        double left = x + camera.x(alpha) * scale;
        double top = y + camera.y(alpha) * scale;
        g.setStroke(thin);
        g.setColor(colour);
        rectangle.setRect(left, top, camera.viewWidth() * scale, camera.viewHeight() * scale);
        g.draw(rectangle);
    }
    // --8<-- [end:minimap]

    // --8<-- [start:convert]
    /** True if a point of the screen lies on the map's diamond. */
    public boolean contains(double screenX, double screenY) {
        return outline.contains(screenX, screenY);
    }

    /** The point of the projection's picture that a point of the minimap shows: where to move the camera. */
    public double pictureX(double screenX) {
        return (screenX - x) / scale;
    }

    public double pictureY(double screenY) {
        return (screenY - y) / scale;
    }

    /** Where on the screen a point of the world's ground is shown. */
    public double screenX(double worldX, double worldY) {
        return x + iso.pictureX(worldX, worldY) * scale;
    }

    public double screenY(double worldX, double worldY) {
        return y + iso.pictureY(worldX, worldY, 0) * scale;
    }
    // --8<-- [end:convert]

    /** The box on the screen: the diamond's corners touch its sides. */
    public double x() {
        return x;
    }

    public double y() {
        return y + iso.originY() * scale;
    }

    public double width() {
        return iso.width() * scale;
    }

    public double height() {
        return (iso.height() - iso.originY()) * scale;
    }

    /** The colour painted for a tile, for the tests. */
    public int colour(int col, int row) {
        return pixels[row * columns + col] & 0xFFFFFF;
    }
}
