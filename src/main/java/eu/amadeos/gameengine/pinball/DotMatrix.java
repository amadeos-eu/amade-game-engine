package eu.amadeos.gameengine.pinball;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/**
 * A dot-matrix display, the orange screen of the machines of the nineties: a grid of round dots, each on or
 * off, here 128 × 32. Write lines of text or draw anything into its one-bit picture; {@link #paint} shows
 * the dots, lit or dark, at any size.
 *
 * <p>Drawing thousands of dots is slow in Java2D, so the painted display is kept as a picture and made
 * again only when its content, size or colour changes.
 */
public final class DotMatrix {
    private final int columns, rows;
    private final BufferedImage bits;
    private BufferedImage painted;
    private int paintedW, paintedH;
    private Color paintedOn;
    private long version, paintedVersion = -1;

    public DotMatrix(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
        this.bits = new BufferedImage(columns, rows, BufferedImage.TYPE_BYTE_BINARY);
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }

    public void clear() {
        Graphics2D g = bits.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, columns, rows);
        g.dispose();
        version++;
    }

    /** One line of text, centred, its baseline at {@code baseline}, in a font {@code size} dots high. */
    public void line(String text, int baseline, int size, boolean bold) {
        Graphics2D g = bits.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.MONOSPACED, bold ? Font.BOLD : Font.PLAIN, size));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (columns - fm.stringWidth(text)) / 2, baseline);
        g.dispose();
        version++;
    }

    /** Two lines: a big one above, a small one below; the usual message. */
    public void show(String big, String small) {
        clear();
        if (small == null || small.isEmpty()) {
            line(big, rows / 2 + 6, 15, true);
        } else {
            line(big, 13, 12, true);
            line(small, 28, 10, false);
        }
    }

    /** Draws onto the dots: white is on. For animations; call {@link #changed()} afterwards. */
    public Graphics2D graphics() {
        return bits.createGraphics();
    }

    public void changed() {
        version++;
    }

    public boolean on(int x, int y) {
        return (bits.getRGB(x, y) & 0xffffff) != 0;
    }

    // --8<-- [start:paint]
    /** The display, {@code w} × {@code h} in {@code g}'s units, its dots in {@code on} and a dark shade of it. */
    public void paint(Graphics2D g, double x, double y, double w, double h, Color on) {
        AffineTransform t = g.getTransform();
        int pw = Math.max(1, (int) Math.round(w * t.getScaleX())), ph = Math.max(1, (int) Math.round(h * t.getScaleY()));
        if (painted == null || paintedVersion != version || pw != paintedW || ph != paintedH || !on.equals(paintedOn)) {
            painted = render(pw, ph, on);
            paintedW = pw;
            paintedH = ph;
            paintedOn = on;
            paintedVersion = version;
        }
        g.setTransform(new AffineTransform());
        g.drawImage(painted, (int) Math.round(t.getTranslateX() + x * t.getScaleX()),
                (int) Math.round(t.getTranslateY() + y * t.getScaleY()), null);
        g.setTransform(t);
    }
    // --8<-- [end:paint]

    private BufferedImage render(int w, int h, Color on) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(5, 3, 3));
        g.fillRoundRect(0, 0, w, h, Math.max(2, w / 40), Math.max(2, w / 40));
        double pad = Math.max(2, w * 0.02);
        double dx = (w - 2 * pad) / columns, dy = (h - 2 * pad) / rows;
        Color off = new Color(on.getRed() / 6, on.getGreen() / 6, on.getBlue() / 6);
        for (int j = 0; j < rows; j++) {
            for (int i = 0; i < columns; i++) {
                g.setColor(on(i, j) ? on : off);
                g.fill(new Ellipse2D.Double(pad + i * dx, pad + j * dy, dx * 0.85, dy * 0.85));
            }
        }
        g.dispose();
        return img;
    }
}
