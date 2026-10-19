package eu.amadeos.gameengine.camera;

import eu.amadeos.gameengine.sprite.Sprite;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Backgrounds that scroll slower than the world, the further away they are: distant mountains, nearer
 * hills, clouds. Each layer is one picture, repeated side by side as far as the view needs, and moved by a
 * part of the camera's way, its <i>factor</i>: 0 stays still like the sky, 0.2 creeps along like mountains
 * on the horizon, 1 moves with the world. The eye reads the different speeds as depth.
 *
 * <pre>
 * Parallax back = new Parallax(640, 400)
 *         .layer(mountains, 0.15, 0.05, 140)
 *         .layer(hills, 0.4, 0.2, 230);
 * // when drawing, before the world:
 * back.draw(g, camera.x(alpha), camera.y(alpha));
 * </pre>
 *
 * <p>The copies meet without seams, as tiles do: they are drawn with {@link Sprite#drawSeamless}.
 */
// --8<-- [start:parallax]
public final class Parallax {
    /**
     * One picture of the background.
     *
     * @param factor  the part of the camera's horizontal way the layer moves; 0 still, 1 with the world
     * @param factorY the same for the vertical way, usually smaller, so the horizon stays where it is
     * @param y       the layer's top on the screen while the camera is at the top of the world
     */
    public record Layer(Sprite picture, double factor, double factorY, double y) {
    }

    private final double viewWidth;
    private final double viewHeight;
    private final List<Layer> layers = new ArrayList<>();

    /** Backgrounds for a view of {@code viewWidth} × {@code viewHeight} logical units. */
    public Parallax(double viewWidth, double viewHeight) {
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
    }

    /** Adds a layer in front of those added before. */
    public Parallax layer(Sprite picture, double factor, double factorY, double y) {
        layers.add(new Layer(picture, factor, factorY, y));
        return this;
    }

    /** Draws all layers, the furthest first, for a camera whose top left corner is at {@code cameraX}, {@code cameraY}. */
    public void draw(Graphics2D g, double cameraX, double cameraY) {
        for (Layer layer : layers) {
            double width = layer.picture().width();
            double top = layer.y() - cameraY * layer.factorY();
            if (top >= viewHeight || top + layer.picture().height() <= 0) continue;
            // Where the first copy starts: the layer's own position, wrapped into one picture width left of 0.
            double shift = (cameraX * layer.factor()) % width;
            double left = shift < 0 ? -shift - width : -shift;
            for (int i = 0; left + i * width < viewWidth; i++) {
                layer.picture().drawSeamless(g, left + i * width, top);
            }
        }
    }
    // --8<-- [end:parallax]

    public List<Layer> layers() {
        return List.copyOf(layers);
    }
}
