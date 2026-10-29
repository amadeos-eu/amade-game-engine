package eu.amadeos.gameengine.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MouseLookTest {
    @Test
    void countsMovementOnlyWhileCaptured() {
        MouseLook look = new MouseLook(null);
        look.moved(10, 10);
        look.moved(30, 10);
        look.poll();
        assertEquals(0, look.dx());

        look.capture(true);
        look.moved(100, 100);          // the first position after capturing only sets the start
        look.moved(112, 95);
        look.moved(120, 90);
        look.poll();
        assertEquals(20, look.dx());
        assertEquals(-10, look.dy());
        look.poll();
        assertEquals(0, look.dx());     // nothing moved since
    }
}
