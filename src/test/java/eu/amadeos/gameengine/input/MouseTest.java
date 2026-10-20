package eu.amadeos.gameengine.input;

import org.junit.jupiter.api.Test;

import java.awt.event.MouseEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MouseTest {
    private final Mouse mouse = new Mouse();

    @Test
    void movesAndClicksCountUntilTheNextPoll() {
        assertTrue(Double.isNaN(mouse.x()));
        mouse.moved(100, 50);
        mouse.pressed(MouseEvent.BUTTON1);
        assertFalse(mouse.wasMoved());   // not before the poll
        mouse.poll();
        assertTrue(mouse.wasMoved());
        assertTrue(mouse.wasPressed(MouseEvent.BUTTON1));
        assertFalse(mouse.wasPressed(MouseEvent.BUTTON3));
        assertEquals(100, mouse.x());
        assertEquals(50, mouse.y());
        mouse.poll();
        assertFalse(mouse.wasMoved());
        assertFalse(mouse.wasPressed(MouseEvent.BUTTON1));
        assertEquals(100, mouse.x());   // the position stays
    }

    @Test
    void aHeldButtonIsDownUntilReleased() {
        mouse.pressed(MouseEvent.BUTTON3);
        mouse.poll();
        assertTrue(mouse.isDown(MouseEvent.BUTTON3));
        mouse.poll();
        assertTrue(mouse.isDown(MouseEvent.BUTTON3), "still held");
        assertFalse(mouse.wasPressed(MouseEvent.BUTTON3), "but pressed only once");
        mouse.released(MouseEvent.BUTTON3);
        mouse.poll();
        assertFalse(mouse.isDown(MouseEvent.BUTTON3));
    }

    @Test
    void aReleaseEndsADragAndLeavingTheCanvasIsNoticed() {
        mouse.moved(10, 10);
        mouse.pressed(MouseEvent.BUTTON3);
        mouse.poll();
        assertTrue(mouse.isInside());
        assertFalse(mouse.wasReleased(MouseEvent.BUTTON3));
        mouse.released(MouseEvent.BUTTON3);
        mouse.exited();
        mouse.poll();
        assertTrue(mouse.wasReleased(MouseEvent.BUTTON3));
        assertFalse(mouse.isInside(), "left the canvas");
        mouse.poll();
        assertFalse(mouse.wasReleased(MouseEvent.BUTTON3), "released once");
    }

    @Test
    void wheelNotchesAddUpUntilTheNextPoll() {
        mouse.wheeled(-1);
        mouse.wheeled(-2);
        assertEquals(0, mouse.wheel(), "not polled yet");
        mouse.poll();
        assertEquals(-3, mouse.wheel());
        mouse.poll();
        assertEquals(0, mouse.wheel(), "counted once");
    }
}
