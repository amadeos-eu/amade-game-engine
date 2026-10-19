package eu.amadeos.gameengine.sprite;

import java.awt.image.BufferedImage;

/**
 * Many small pictures in one image, in a grid of equal cells: the frames of a flapping insect, the four
 * looks of a ship, the stages of an explosion. One file instead of fifty, drawn and edited side by side, so
 * the frames of an animation line up. The cells are numbered row by row, from the top left.
 *
 * <pre>
 * SpriteSheet sheet = SpriteSheet.load(Painter.class, "/sprites/swarm.png", 8, 4, 16, 16).hardEdges();
 * sheet.frame(row * sheet.columns() + column).draw(g, x - 8, y - 8);
 * </pre>
 *
 * <p>Each frame is a {@link Sprite} of its own, with its own cache: a frame that is drawn is scaled once
 * per screen size and copied from then on, like every sprite.
 */
// --8<-- [start:sheet]
public final class SpriteSheet {
    private final Sprite[] frames;
    private final int columns;

    private SpriteSheet(Sprite[] frames, int columns) {
        this.frames = frames;
        this.columns = columns;
    }

    /**
     * Cuts {@code image} into {@code columns} × {@code rows} cells of equal size; each is shown at
     * {@code frameWidth} × {@code frameHeight} logical units.
     */
    public static SpriteSheet of(BufferedImage image, int columns, int rows, double frameWidth, double frameHeight) {
        if (columns < 1 || rows < 1 || image.getWidth() % columns != 0 || image.getHeight() % rows != 0) {
            throw new IllegalArgumentException(image.getWidth() + " × " + image.getHeight()
                    + " pixels do not make " + columns + " × " + rows + " equal cells");
        }
        int w = image.getWidth() / columns;
        int h = image.getHeight() / rows;
        Sprite[] frames = new Sprite[columns * rows];
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                // A view into the sheet, not a copy: the pixels stay in the one image.
                BufferedImage cell = image.getSubimage(column * w, row * h, w, h);
                frames[row * columns + column] = Sprite.of(cell, frameWidth, frameHeight);
            }
        }
        return new SpriteSheet(frames, columns);
    }
    // --8<-- [end:sheet]

    /** A sheet from an image file among the classes of {@code owner}, e.g. {@code "/sprites/swarm.png"}. */
    public static SpriteSheet load(Class<?> owner, String resource, int columns, int rows, double frameWidth,
                                   double frameHeight) {
        return of(Sprite.readImage(owner, resource), columns, rows, frameWidth, frameHeight);
    }

    /** The same sheet with every frame scaled with hard edges: for pixel art. */
    public SpriteSheet hardEdges() {
        Sprite[] hard = new Sprite[frames.length];
        for (int i = 0; i < frames.length; i++) hard[i] = frames[i].hardEdges();
        return new SpriteSheet(hard, columns);
    }

    /** Frame {@code index}, counted row by row from the top left. */
    public Sprite frame(int index) {
        return frames[index];
    }

    public Sprite frame(int column, int row) {
        return frames[row * columns + column];
    }

    public int frames() {
        return frames.length;
    }

    public int columns() {
        return columns;
    }
}
