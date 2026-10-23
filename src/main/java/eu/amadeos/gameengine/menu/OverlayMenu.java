package eu.amadeos.gameengine.menu;

import eu.amadeos.gameengine.sprite.Sprite;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Objects;

/**
 * A menu inside the game's picture: it opens beside the place it belongs to, such as the tile the player
 * clicked, and offers a few things to choose, such as the towers to build there and what they cost. Not a
 * Swing menu: the game's canvas covers Swing's lightweight popups, a heavyweight one would be a window of
 * its own over a game in full screen, and neither looks like the game.
 *
 * <p>The menu is arithmetic first: where it lies, which row is under the mouse, what a click or a key
 * chooses. The game hands it the mouse and the keys in its update step, so the menu needs no listeners and
 * the tests no screen. {@link #render} draws it with a {@link Style}, in the overlay.
 *
 * <pre>
 * menu.open("Build", items, cellLeft, cellTop, TILE, TILE);
 * // in each update step:
 * menu.hover(mouse.x(), mouse.y());
 * if (mouse.wasPressed(BUTTON1)) build(menu.click(mouse.x(), mouse.y()));   // null: nothing chosen
 * </pre>
 *
 * @param <T> what the rows stand for, e.g. a kind of tower
 */
// --8<-- [start:menu]
public final class OverlayMenu<T> {
    /**
     * A row: the value it stands for, a small picture, its text and, on the right, a detail such as a
     * price. A row that is not enabled shows but cannot be chosen, e.g. a tower the player cannot pay.
     * The icon is drawn at its own size, best {@code rowHeight - 4} square; it may be {@code null}, and so
     * may the detail.
     */
    public record Item<T>(T value, Sprite icon, String label, String detail, boolean enabled) {
        public Item {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(label, "label");
        }
    }

    /** The space between the place the menu belongs to and the menu. */
    public static final double GAP = 8;
    /** How long the menu takes to open, in seconds. */
    public static final double OPENING = 0.1;

    private final double width;
    private final double rowHeight;
    private final double titleHeight;
    private double boundsLeft;
    private double boundsTop;
    private double boundsRight;
    private double boundsBottom;
    private String title = "";
    private List<Item<T>> items = List.of();
    private boolean open;
    private double x;
    private double y;
    private int highlighted = -1;
    private double openness;

    /**
     * A menu of {@code width}, with rows of {@code rowHeight} below a title of {@code titleHeight}, always
     * kept inside the box from {@code left}, {@code top} to {@code right}, {@code bottom}: the part of the
     * screen it may cover.
     */
    public OverlayMenu(double width, double rowHeight, double titleHeight,
                       double left, double top, double right, double bottom) {
        this.width = width;
        this.rowHeight = rowHeight;
        this.titleHeight = titleHeight;
        this.boundsLeft = left;
        this.boundsTop = top;
        this.boundsRight = right;
        this.boundsBottom = bottom;
    }
    // --8<-- [end:menu]

    /**
     * The part of the screen the menu may cover from now on, e.g. wider when the canvas widens on a wide
     * screen; a menu that is open stays where it is until it opens again.
     */
    public void setBounds(double left, double top, double right, double bottom) {
        boundsLeft = left;
        boundsTop = top;
        boundsRight = right;
        boundsBottom = bottom;
    }

    // --8<-- [start:open]
    /**
     * Opens the menu beside the box {@code anchorX}, {@code anchorY}, {@code anchorWidth} ×
     * {@code anchorHeight}, the thing it is about: to its right, vertically centred on it; to its left when
     * there is no room on the right; and moved up or down as far as needed to stay inside the bounds. The
     * first row that can be chosen is highlighted, so Enter chooses it.
     */
    public void open(String title, List<Item<T>> items, double anchorX, double anchorY,
                     double anchorWidth, double anchorHeight) {
        if (items.isEmpty()) throw new IllegalArgumentException("a menu needs something to choose");
        this.title = title;
        this.items = List.copyOf(items);
        double height = height();
        x = anchorX + anchorWidth + GAP;
        if (x + width > boundsRight) x = anchorX - GAP - width;
        x = Math.clamp(x, boundsLeft, Math.max(boundsLeft, boundsRight - width));
        y = anchorY + anchorHeight / 2 - height / 2;
        y = Math.clamp(y, boundsTop, Math.max(boundsTop, boundsBottom - height));
        highlighted = -1;
        move(1);
        openness = 0;
        open = true;
    }
    // --8<-- [end:open]

    public boolean isOpen() {
        return open;
    }

    public void close() {
        open = false;
        highlighted = -1;
    }

    /** Lets the opening run; call in each update step. */
    public void update(double seconds) {
        if (open) openness = Math.min(1, openness + seconds / OPENING);
    }

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
        return titleHeight + items.size() * rowHeight;
    }

    public List<Item<T>> items() {
        return items;
    }

    /** The highlighted row, -1 for none. */
    public int highlighted() {
        return highlighted;
    }

    /** True if the point lies on the open menu: a click there belongs to the menu, not to the map below. */
    public boolean contains(double px, double py) {
        return open && px >= x && px < x + width && py >= y && py < y + height();
    }

    // --8<-- [start:input]
    /** The row at a point, or -1. */
    public int rowAt(double px, double py) {
        if (!contains(px, py) || py < y + titleHeight) return -1;
        return (int) ((py - y - titleHeight) / rowHeight);
    }

    /** The mouse moved: the row under it is highlighted. Off the menu, the highlight stays where it was. */
    public void hover(double px, double py) {
        int row = rowAt(px, py);
        if (row >= 0) highlighted = row;
    }

    /**
     * A click: on a row that can be chosen, its value, and the menu closes. Elsewhere on the menu nothing
     * happens. Outside the menu it closes without a choice, as a menu does.
     */
    public T click(double px, double py) {
        if (!open) return null;
        if (!contains(px, py)) {
            close();
            return null;
        }
        int row = rowAt(px, py);
        return row >= 0 ? choose(row) : null;
    }

    /** A shortcut, 1 for the first row: the keys 1, 2, 3 of a build menu. */
    public T key(int number) {
        return open && number >= 1 && number <= items.size() ? choose(number - 1) : null;
    }

    /** The highlight to the next row that can be chosen, {@code +1} down, {@code -1} up; it wraps round. */
    public void move(int direction) {
        int n = items.size();
        int from = highlighted >= 0 ? highlighted : direction > 0 ? -1 : n;
        for (int i = 1; i <= n; i++) {
            int row = Math.floorMod(from + direction * i, n);
            if (items.get(row).enabled()) {
                highlighted = row;
                return;
            }
        }
    }

    /** Enter: the highlighted row. */
    public T chooseHighlighted() {
        return open && highlighted >= 0 ? choose(highlighted) : null;
    }

    private T choose(int row) {
        Item<T> item = items.get(row);
        if (!item.enabled()) return null;
        close();
        return item.value();
    }
    // --8<-- [end:input]

    // --8<-- [start:style]
    /** How the menu looks: the panel, the texts, and an optional picture before each detail, e.g. a coin. */
    public record Style(Color top, Color bottom, Color border, Color title, Color text, Color detail,
                        Color disabled, Color disabledDetail, Color highlight, Color shortcut,
                        Font titleFont, Font labelFont, Font detailFont, Font shortcutFont, Sprite detailIcon) {
        /** Dark wood and brass, readable over any map. */
        public static final Style DEFAULT = new Style(new Color(0x2A2420), new Color(0x1A1612), new Color(0x8A6A3A),
                new Color(0xC8A870), new Color(0xF2E8D4), new Color(0xFFE08A), new Color(0x8A7E6C),
                new Color(0xE36B5B), new Color(0x4A3C28), new Color(0x8A7A60),
                new Font(Font.SANS_SERIF, Font.BOLD, 8).deriveFont(8.5f), new Font(Font.SANS_SERIF, Font.BOLD, 10),
                new Font(Font.SANS_SERIF, Font.BOLD, 10), new Font(Font.SANS_SERIF, Font.PLAIN, 7).deriveFont(7.5f),
                null);

        public Style withDetailIcon(Sprite icon) {
            return new Style(top, bottom, border, title, text, detail, disabled, disabledDetail, highlight, shortcut,
                    titleFont, labelFont, detailFont, shortcutFont, icon);
        }
    }
    // --8<-- [end:style]

    // --8<-- [start:render]
    /** Draws the open menu; while it opens it grows from its anchor side and fades in. */
    public void render(Graphics2D g, Style style) {
        if (!open) return;
        Graphics2D m = (Graphics2D) g.create();
        try {
            double grow = 0.85 + 0.15 * openness;
            m.translate(x + width / 2, y + height() / 2);
            m.scale(grow, grow);
            m.translate(-x - width / 2, -y - height() / 2);
            Composite fade = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.max(0.2, openness));
            m.setComposite(fade);
            double h = height();
            m.setColor(new Color(0, 0, 0, 90));
            m.fill(new RoundRectangle2D.Double(x + 2, y + 3, width, h, 8, 8));
            m.setPaint(new GradientPaint((float) x, (float) y, style.top(), (float) x, (float) (y + h), style.bottom()));
            m.fill(new RoundRectangle2D.Double(x, y, width, h, 8, 8));
            m.setColor(style.border());
            m.setStroke(new BasicStroke(1f));
            m.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, width - 1, h - 1, 8, 8));
            m.setFont(style.titleFont());
            m.setColor(style.title());
            m.drawString(title, (float) (x + 9), (float) (y + titleHeight * 0.68));
            for (int i = 0; i < items.size(); i++) row(m, style, i);
        } finally {
            m.dispose();
        }
    }

    private void row(Graphics2D g, Style style, int i) {
        Item<T> item = items.get(i);
        double top = y + titleHeight + i * rowHeight;
        if (i == highlighted) {
            g.setColor(style.highlight());
            g.fill(new RoundRectangle2D.Double(x + 4, top, width - 8, rowHeight - 1, 5, 5));
        }
        double iconSize = rowHeight - 4;
        if (item.icon() != null) item.icon().draw(g, x + 7, top + 1.5);
        double middle = top + rowHeight / 2;
        g.setFont(style.labelFont());
        g.setColor(item.enabled() ? style.text() : style.disabled());
        g.drawString(item.label(), (float) (x + 11 + iconSize), (float) (middle + 3.5));
        if (item.detail() != null) {
            g.setFont(style.detailFont());
            float detailX = (float) (x + width - 14 - g.getFontMetrics().stringWidth(item.detail()));
            g.setColor(item.enabled() ? style.detail() : style.disabledDetail());
            g.drawString(item.detail(), detailX, (float) (middle + 3.5));
            if (style.detailIcon() != null) {
                Sprite icon = style.detailIcon();
                icon.draw(g, detailX - icon.width() - 2, middle - icon.height() / 2);
            }
        }
        g.setFont(style.shortcutFont());
        g.setColor(style.shortcut());
        g.drawString(Integer.toString(i + 1), (float) (x + width - 9), (float) (top + 7));
    }
    // --8<-- [end:render]
}
