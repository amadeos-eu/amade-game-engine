package eu.amadeos.gameengine.adventure;

import eu.amadeos.gameengine.math.Vector;
import eu.amadeos.gameengine.tile.Direction;
import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionTest {
    static final Hotspot PSALTER = new Hotspot("psalter", Hotspot.rectangle(0, 0, 10, 10), Vector.ZERO, Direction.UP);
    static final Hotspot LEFORT = new Hotspot("lefort", Hotspot.rectangle(0, 0, 10, 10), Vector.ZERO, Direction.UP);
    static final Hotspot DOOR = new Hotspot("door", Hotspot.rectangle(0, 0, 10, 10), Vector.ZERO, Direction.UP,
            "corridor", "door");
    static final Interaction.People PEOPLE = Set.of("lefort")::contains;

    @Test
    void theThingDecidesWhatTheLeftButtonDoes() {
        assertEquals(List.of("use", "psalter"), Interaction.trigger(Interaction.LEFT, PSALTER, null, PEOPLE));
        assertEquals(List.of("talk", "lefort"), Interaction.trigger(Interaction.LEFT, LEFORT, null, PEOPLE));
        assertEquals(List.of("exit", "door"), Interaction.trigger(Interaction.LEFT, DOOR, null, PEOPLE));
        assertEquals(List.of("use", "coffee", "lefort"), Interaction.trigger(Interaction.LEFT, LEFORT, "coffee", PEOPLE));
        assertEquals(List.of("look", "door"), Interaction.trigger(Interaction.RIGHT, DOOR, "coffee", PEOPLE));
    }

    @Test
    void readingTimeGrowsWithTheLineWithinLimits() {
        assertEquals(1.5, ReadingTime.of("Non."), 1e-9);
        assertEquals(1 + 45 / 15.0, ReadingTime.of("A chirograph is the oldest form of distrust."), 0.1);
        assertEquals(8, ReadingTime.of("x".repeat(500)), 1e-9);
    }

    @Test
    void spokenTextBreaksIntoLinesThatFit() {
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 12);
        FontRenderContext frc = new FontRenderContext(null, true, true);
        List<String> lines = SpokenText.wrap("Someone opened this binding before me. Centuries ago.", font, frc, 150);
        assertTrue(lines.size() >= 2, lines.toString());
        for (String line : lines)
            assertTrue(font.getStringBounds(line, frc).getWidth() <= 150 || !line.contains(" "), line);
        assertEquals("Someone opened this binding before me. Centuries ago.", String.join(" ", lines));
    }

    @Test
    void inventoryAndFlagsSurviveASavedGame() {
        Inventory inventory = new Inventory();
        inventory.gain("phone");
        inventory.gain("page");
        inventory.gain("phone");
        Inventory loaded = new Inventory();
        loaded.restore(inventory.save());
        assertEquals(List.of("phone", "page"), loaded.items());
        Flags flags = new Flags();
        flags.set("found-page");
        flags.add("asked", 2);
        flags.set("gone", 0);
        Flags restored = new Flags();
        restored.restore(flags.all());
        assertEquals(2, restored.get("asked"));
        assertTrue(restored.is("found-page"));
        assertEquals(2, restored.all().size());
    }
}
