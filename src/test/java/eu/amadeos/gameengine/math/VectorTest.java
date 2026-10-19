package eu.amadeos.gameengine.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class VectorTest {
    private static final double EPSILON = 1e-9;

    private static void assertNear(Vector expected, Vector actual) {
        assertEquals(expected.x(), actual.x(), EPSILON, "x of " + actual);
        assertEquals(expected.y(), actual.y(), EPSILON, "y of " + actual);
    }

    // --8<-- [start:test-vector]
    @Test
    void angleZeroPointsRightAndMinusHalfPiPointsUp() {
        assertNear(new Vector(2, 0), Vector.ofAngle(0, 2));
        assertNear(new Vector(0, -1), Vector.ofAngle(-Math.PI / 2, 1));    // y grows downwards
        assertEquals(Math.PI / 2, new Vector(0, 5).angle(), EPSILON);       // down
    }

    @Test
    void turningAQuarterClockwiseMakesRightIntoDown() {
        assertNear(new Vector(0, 3), new Vector(3, 0).rotated(Math.PI / 2));
        assertNear(new Vector(-3, 0), new Vector(3, 0).rotated(Math.PI));
        assertEquals(5, new Vector(3, 4).rotated(1.234).length(), EPSILON, "turning keeps the length");
    }
    // --8<-- [end:test-vector]

    @Test
    void arithmetic() {
        Vector a = new Vector(3, 4);
        assertEquals(new Vector(4, 6), a.plus(new Vector(1, 2)));
        assertEquals(new Vector(2, 2), a.minus(new Vector(1, 2)));
        assertEquals(new Vector(6, 8), a.times(2));
        assertEquals(5, a.length());
        assertEquals(25, a.lengthSquared());
        assertEquals(0, new Vector(1, 0).dot(new Vector(0, 7)));
        assertEquals(5, a.distance(Vector.ZERO));
        assertNear(new Vector(0.6, 0.8), a.normalized());
        assertSame(Vector.ZERO, Vector.ZERO.normalized());
    }

    @Test
    void limitedKeepsTheDirection() {
        Vector a = new Vector(30, 40);
        assertNear(new Vector(6, 8), a.limited(10));
        assertSame(a, a.limited(100));
    }
}
