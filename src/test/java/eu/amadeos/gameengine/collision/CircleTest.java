package eu.amadeos.gameengine.collision;

import eu.amadeos.gameengine.math.Wrap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircleTest {
    // --8<-- [start:test-circle]
    @Test
    void circlesOverlapWhenTheirCentresAreCloserThanBothRadii() {
        Circle rock = new Circle(100, 100, 30);
        assertTrue(rock.overlaps(new Circle(120, 120, 5)));     // 28.3 apart, 35 needed
        assertFalse(rock.overlaps(new Circle(130, 130, 5)));    // 42.4 apart
        assertFalse(rock.overlaps(new Circle(140, 100, 10)), "touching is not overlapping");
        assertTrue(rock.contains(100, 129));
    }

    @Test
    void onAFieldWithoutEdgesTheyMeetAcrossTheEdge() {
        Wrap field = new Wrap(640, 400);
        Circle right = new Circle(630, 200, 10);
        Circle left = new Circle(5, 200, 10);
        assertFalse(right.overlaps(left), "625 apart on a flat field");
        assertTrue(right.overlaps(left, field), "15 apart across the edge");
    }
    // --8<-- [end:test-circle]

    @Test
    void noNegativeRadius() {
        assertThrows(IllegalArgumentException.class, () -> new Circle(0, 0, -1));
    }
}
