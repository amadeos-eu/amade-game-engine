package eu.amadeos.gameengine.fog;

import eu.amadeos.gameengine.sprite.Sprite;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Draws a {@link FogOfWar} over the map, with soft edges. A tile drawn black or dim as a whole would show
 * the grid: a staircase at the edge of everything a unit sees. Instead each <b>corner</b> of a tile gets a
 * state, the darkest of the four tiles that meet there, and the tile shades smoothly from corner to corner.
 * Where four corners agree the tile is plain: clear, dim or black. At the edges they differ, and the shade
 * runs across the tile.
 *
 * <p>Three states at four corners make 3⁴ = 81 kinds of tile, and that is all there are. Each is drawn
 * once, as a {@link Sprite}, and only copied after that, whatever the map: the same trick as the 32 turned
 * copies of a turret. A little noise in the shade, the same at every tile's edge so the tiles meet
 * without a seam, frays the edges like a real fog.
 *
 * <p>Taking the darkest of the four tiles at a corner means that the fog reaches half a tile into what is
 * visible, never the other way: nothing that stands on a hidden tile ever shows through.
 */
// --8<-- [start:painter]
public final class FogPainter {
    /** How dark a seen tile is drawn that nobody sees now, 0 clear to 1 black. */
    public static final double DIM = 0.48;

    private final double tile;
    private final Color shroud;
    private final Sprite[] tiles = new Sprite[81];
    private final Rectangle2D.Double rectangle = new Rectangle2D.Double();

    /** A painter for tiles of {@code tile} logical units, in black. */
    public FogPainter(double tile) {
        this(tile, new Color(0x050302));
    }

    public FogPainter(double tile, Color shroud) {
        this.tile = tile;
        this.shroud = shroud;
        for (int kind = 0; kind < tiles.length; kind++) {
            int k = kind;
            tiles[kind] = Sprite.drawn(tile, tile, (g, w, h) -> draw(g, w, h, k));
        }
    }

    /** The darkness of a state: 0 visible, {@link #DIM} seen, 1 unseen. */
    static double darkness(int level) {
        return level == 0 ? 0 : level == 1 ? DIM : 1;
    }

    /**
     * The fog over the tiles from {@code fromCol}, {@code fromRow} to {@code toCol}, {@code toRow}, in the
     * world's coordinates: draw it after the world and before the marks that must stay readable.
     */
    public void render(Graphics2D g, FogOfWar fog, int fromCol, int fromRow, int toCol, int toRow) {
        g.setColor(shroud);
        for (int row = Math.max(0, fromRow); row <= Math.min(fog.height() - 1, toRow); row++) {
            int black = -1;                                   // where a run of black tiles began
            int last = Math.min(fog.width() - 1, toCol);
            for (int col = Math.max(0, fromCol); col <= last + 1; col++) {
                int kind = col > last ? -1 : kind(corner(fog, col, row), corner(fog, col + 1, row),
                        corner(fog, col, row + 1), corner(fog, col + 1, row + 1));
                if (kind == 80) {                             // all black: no picture needed
                    if (black < 0) black = col;
                    continue;
                }
                if (black >= 0) {
                    fill(g, black * tile, row * tile, (col - black) * tile, tile);
                    black = -1;
                }
                if (kind > 0) tiles[kind].drawSeamless(g, col * tile, row * tile);
            }
        }
    }

    /**
     * Black from {@code x}, {@code y}, {@code w} × {@code h}, its edges rounded to the screen's pixels just as
     * {@link Sprite#drawSeamless} rounds them: a shape filled in the world's coordinates may round the other
     * way, and leave a line of pixels uncovered between the black and a soft tile.
     */
    private void fill(Graphics2D g, double x, double y, double w, double h) {
        AffineTransform t = g.getTransform();
        if ((t.getType() & (AffineTransform.TYPE_MASK_ROTATION | AffineTransform.TYPE_GENERAL_TRANSFORM)) != 0) {
            rectangle.setRect(x, y, w, h);
            g.fill(rectangle);
            return;
        }
        long left = Math.round(t.getTranslateX() + x * t.getScaleX());
        long right = Math.round(t.getTranslateX() + (x + w) * t.getScaleX());
        long top = Math.round(t.getTranslateY() + y * t.getScaleY());
        long bottom = Math.round(t.getTranslateY() + (y + h) * t.getScaleY());
        g.setTransform(new AffineTransform());
        g.fillRect((int) left, (int) top, (int) (right - left), (int) (bottom - top));
        g.setTransform(t);
    }

    /** The kind of tile with these corner states, top left, top right, bottom left, bottom right. */
    static int kind(int topLeft, int topRight, int bottomLeft, int bottomRight) {
        return topLeft + 3 * topRight + 9 * bottomLeft + 27 * bottomRight;
    }

    /**
     * The state of the corner at the top left of tile {@code col}, {@code row}: the darkest of the four
     * tiles around it. Beyond the edge of the map the tiles inside count, so the edge gets no dark rim.
     */
    static int corner(FogOfWar fog, int col, int row) {
        int darkest = 0;
        for (int dr = -1; dr <= 0; dr++) {
            for (int dc = -1; dc <= 0; dc++) {
                int c = Math.clamp(col + dc, 0, fog.width() - 1);
                int r = Math.clamp(row + dr, 0, fog.height() - 1);
                darkest = Math.max(darkest, level(fog.state(c, r)));
            }
        }
        return darkest;
    }

    private static int level(FogOfWar.State state) {
        return switch (state) {
            case VISIBLE -> 0;
            case SEEN -> 1;
            case UNSEEN -> 2;
        };
    }
    // --8<-- [end:painter]

    // --8<-- [start:shade]
    /**
     * One kind of tile, pixel by pixel at its size on the screen: the darkness of the four corners mixed by
     * how near each pixel is to each, along a smooth curve, plus a little noise near the edge of the fog.
     */
    private void draw(Graphics2D g, double w, double h, int kind) {
        AffineTransform at = g.getTransform();
        int pw = Math.max(1, (int) Math.ceil(w * Math.abs(at.getScaleX())));
        int ph = Math.max(1, (int) Math.ceil(h * Math.abs(at.getScaleY())));
        double[] corner = {darkness(kind % 3), darkness(kind / 3 % 3), darkness(kind / 9 % 3), darkness(kind / 27)};
        BufferedImage image = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_ARGB);
        int rgb = shroud.getRGB() & 0xFFFFFF;
        for (int py = 0; py < ph; py++) {
            double v = smooth((py + 0.5) / ph);
            for (int px = 0; px < pw; px++) {
                double u = smooth((px + 0.5) / pw);
                double dark = (corner[0] * (1 - u) + corner[1] * u) * (1 - v) + (corner[2] * (1 - u) + corner[3] * u) * v;
                // Fray the edge: noise only where the shade is between clear and black.
                double edge = dark * (1 - dark) * 4;
                dark += (noise((px + 0.5) / pw, (py + 0.5) / ph) - 0.5) * 0.5 * edge;
                int alpha = (int) Math.round(255 * Math.clamp(dark, 0, 1));
                image.setRGB(px, py, alpha << 24 | rgb);
            }
        }
        g.drawImage(image, 0, 0, (int) Math.ceil(w), (int) Math.ceil(h), 0, 0, pw, ph, null);
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    /**
     * Value noise with a period of one tile in both directions, so that the noise at the right edge of one
     * tile is the noise at the left edge of the next: a lattice of 4 × 4 values per tile, wrapped.
     */
    static double noise(double u, double v) {
        double x = u * 4;
        double y = v * 4;
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        double fx = smooth(x - xi);
        double fy = smooth(y - yi);
        double a = lattice(xi, yi);
        double b = lattice(xi + 1, yi);
        double c = lattice(xi, yi + 1);
        double d = lattice(xi + 1, yi + 1);
        return (a + (b - a) * fx) * (1 - fy) + (c + (d - c) * fx) * fy;
    }

    private static double lattice(int x, int y) {
        long h = Math.floorMod(x, 4) * 374761393L + Math.floorMod(y, 4) * 668265263L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 65535.0;
    }
    // --8<-- [end:shade]
}
