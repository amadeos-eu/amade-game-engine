package eu.amadeos.gameengine.select;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectionTest {
    // --8<-- [start:test-box]
    @Test
    void aSmallMoveIsAClickALargerOneABox() {
        BoxSelect box = new BoxSelect();
        box.press(100, 100);
        box.move(102, 97);                       // a shaking hand
        assertFalse(box.dragging());
        assertEquals(new BoxSelect.Click(100, 100), box.release(102, 97));

        box.press(100, 100);
        box.move(60, 140);
        assertTrue(box.dragging(), "dragged further than the slop");
        BoxSelect.Result result = box.release(50, 150);
        BoxSelect.Box b = assertInstanceOf(BoxSelect.Box.class, result);
        assertEquals(new BoxSelect.Box(50, 100, 100, 150), b, "the edges sorted, whichever way it was dragged");
        assertTrue(b.contains(75, 120));
        assertNull(box.release(50, 150), "no press, no result");
    }
    // --8<-- [end:test-box]

    // --8<-- [start:test-groups]
    @Test
    void shiftAddsAndTogglesGroupsAreKeptAndTheDeadLeave() {
        Selection<String> selection = new Selection<>();
        selection.pick(List.of("tank 1", "tank 2"), false);
        selection.click("trike", true);
        assertEquals(List.of("tank 1", "tank 2", "trike"), selection.all(), "in the order of picking");
        selection.click("tank 2", true);
        assertEquals(List.of("tank 1", "trike"), selection.all(), "Shift and a click on a picked one takes it out");

        selection.store(1);                      // Ctrl+1
        selection.click("harvester", false);
        assertEquals(List.of("harvester"), selection.all(), "a click without Shift replaces");
        assertTrue(selection.recall(1));         // 1
        assertEquals(List.of("tank 1", "trike"), selection.all());
        assertFalse(selection.recall(2), "an empty group picks nothing");

        selection.keepOnly(thing -> !thing.equals("trike"));      // the trike was destroyed
        assertEquals(List.of("tank 1"), selection.all());
        assertEquals(List.of("tank 1"), selection.group(1), "and it left the group as well");
    }
    // --8<-- [end:test-groups]

    @Test
    void theVersionChangesWithTheSelection() {
        Selection<Integer> selection = new Selection<>();
        int before = selection.version();
        selection.pick(List.of(1), false);
        assertTrue(selection.version() > before);
        int now = selection.version();
        selection.keepOnly(x -> true);
        assertEquals(now, selection.version(), "nothing left, nothing changed");
    }
}
