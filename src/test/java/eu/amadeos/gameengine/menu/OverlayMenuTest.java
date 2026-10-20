package eu.amadeos.gameengine.menu;

import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayMenuTest {
    /** A menu of 132 × (16 + 3 × 26) on a screen of 640 × 400 with a bar of 22 at the top. */
    private final OverlayMenu<String> menu = new OverlayMenu<>(132, 26, 16, 0, 22, 640, 400);
    private final List<OverlayMenu.Item<String>> towers = List.of(
            new OverlayMenu.Item<>("cannon", null, "Cannon", "60", true),
            new OverlayMenu.Item<>("archer", null, "Archer", "40", true),
            new OverlayMenu.Item<>("frost", null, "Frost", "75", false));

    // --8<-- [start:test-place]
    @Test
    void theMenuOpensBesideItsTileAndStaysOnTheScreen() {
        menu.open("Build", towers, 420, 222, 20, 20);
        assertEquals(420 + 20 + 8, menu.x(), "to the right of the tile");
        assertEquals(232 - 94 / 2.0, menu.y(), "centred on it: 94 high");
        menu.open("Build", towers, 600, 222, 20, 20);
        assertEquals(600 - 8 - 132, menu.x(), "no room on the right: to the left");
        menu.open("Build", towers, 300, 30, 20, 20);
        assertEquals(22, menu.y(), "not over the bar at the top");
        menu.open("Build", towers, 300, 380, 20, 20);
        assertEquals(400 - 94, menu.y(), "not below the screen");
    }
    // --8<-- [end:test-place]

    // --8<-- [start:test-choose]
    @Test
    void theMouseAndTheKeysChooseWhatCanBeChosen() {
        menu.open("Build", towers, 100, 200, 20, 20);
        double left = menu.x() + 20;
        double row = menu.y() + 16 + 13;            // the middle of the first row
        menu.hover(left, row + 26);
        assertEquals(1, menu.highlighted(), "the archer under the mouse");
        assertNull(menu.click(left, row + 52), "the frost tower is too dear");
        assertTrue(menu.isOpen());
        assertNull(menu.click(left, menu.y() + 5), "on the title: nothing");
        assertEquals("archer", menu.click(left, row + 26));
        assertFalse(menu.isOpen(), "a choice closes the menu");

        menu.open("Build", towers, 100, 200, 20, 20);
        assertNull(menu.key(3), "3 is the frost tower");
        assertEquals("cannon", menu.key(1));

        menu.open("Build", towers, 100, 200, 20, 20);
        assertEquals(0, menu.highlighted(), "the first that can be chosen");
        menu.move(-1);
        assertEquals(1, menu.highlighted(), "up from the first wraps round, past the frost tower");
        assertEquals("archer", menu.chooseHighlighted());

        menu.open("Build", towers, 100, 200, 20, 20);
        assertNull(menu.click(10, 390), "outside");
        assertFalse(menu.isOpen(), "a click outside closes it");
    }
    // --8<-- [end:test-choose]

    @Test
    void theMenuDrawsWhileItOpens() {
        menu.open("Build", towers, 100, 200, 20, 20);
        BufferedImage image = new BufferedImage(640, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        menu.render(g, OverlayMenu.Style.DEFAULT);
        menu.update(1);
        menu.render(g, OverlayMenu.Style.DEFAULT);
        g.dispose();
        int x = (int) (menu.x() + menu.width() / 2);
        int y = (int) (menu.y() + menu.height() - 3);
        assertEquals(255, image.getRGB(x, y) >>> 24, "the panel is drawn");
    }
}
