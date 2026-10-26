package eu.amadeos.gameengine.adventure;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Speech the way classic adventures show it: no balloon, the words float over the speaker's head in the
 * speaker's own colour, with a dark rim so they can be read on any part of the picture. The text is broken
 * into lines no wider than {@code maxWidth}, centred over the head, and pushed inside the screen where the
 * speaker stands near an edge.
 */
public final class SpokenText {
    private SpokenText() {
    }

    /** The text broken into lines that fit {@code maxWidth}; a single long word keeps a line of its own. */
    public static List<String> wrap(String text, Font font, FontRenderContext frc, double maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.strip().split("\\s+")) {
            String tried = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && font.getStringBounds(tried, frc).getWidth() > maxWidth) {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(tried);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    // --8<-- [start:draw]
    /**
     * Draws {@code text} centred over {@code (x, top)}, the top of the speaker's head, with its last line
     * just above it, inside {@code screen}.
     */
    public static void draw(Graphics2D g, String text, Font font, Color color, double x, double top,
                            double maxWidth, Rectangle2D screen) {
        Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        FontRenderContext frc = g.getFontRenderContext();
        List<String> lines = wrap(text, font, frc, maxWidth);
        double lineHeight = font.getSize2D() * 1.2;
        double y = Math.max(screen.getMinY() + lineHeight, top - 6 - (lines.size() - 1) * lineHeight);
        BasicStroke rim = new BasicStroke(font.getSize2D() / 5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        for (String line : lines) {
            TextLayout layout = new TextLayout(line, font, frc);
            double width = layout.getAdvance();
            double left = Math.max(screen.getMinX() + 4, Math.min(screen.getMaxX() - 4 - width, x - width / 2));
            Shape outline = layout.getOutline(AffineTransform.getTranslateInstance(left, y));
            g.setColor(new Color(20, 12, 6, 230));
            g.setStroke(rim);
            g.draw(outline);
            g.setColor(color);
            g.fill(outline);
            y += lineHeight;
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
    }
    // --8<-- [end:draw]
}
