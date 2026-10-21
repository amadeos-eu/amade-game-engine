package eu.amadeos.gameengine.iso;

import org.junit.jupiter.api.Test;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IsoProjectionTest {
    // --8<-- [start:test-projection]
    @Test
    void aPointOfTheGroundGoesToThePictureAndBack() {
        // 40 × 40 tiles of 16 units, diamonds of 32 × 16, 100 units of room above the map.
        IsoProjection iso = IsoProjection.forMap(40, 40, 16, 32, 16, 100);
        assertEquals(40 * 32, iso.width());
        assertEquals(40 * 16 + 100, iso.height());
        // The top corner of the map is in the middle at the top, below the room for towers.
        assertEquals(640, iso.pictureX(0, 0));
        assertEquals(100, iso.pictureY(0, 0, 0));
        // One tile along x: half a diamond to the right and down; along y: to the left and down.
        assertEquals(656, iso.pictureX(16, 0));
        assertEquals(108, iso.pictureY(16, 0, 0));
        assertEquals(624, iso.pictureX(0, 16));
        // A height lifts the point straight up.
        assertEquals(108 - 30, iso.pictureY(16, 0, 30));
        // And back: the middle of tile (5, 7) is found in the middle of its diamond.
        double x = iso.pictureX(5.5 * 16, 7.5 * 16);
        double y = iso.pictureY(5.5 * 16, 7.5 * 16, 0);
        assertEquals(5.5 * 16, iso.worldX(x, y), 1e-9);
        assertEquals(7.5 * 16, iso.worldY(x, y), 1e-9);
        assertEquals(5, iso.col(x, y));
        assertEquals(7, iso.row(x, y));
    }
    // --8<-- [end:test-projection]

    @Test
    void theCornersOfADiamondBelongToTheirTiles() {
        IsoProjection iso = IsoProjection.forMap(10, 10, 16, 32, 16, 0);
        // Just inside the left corner of tile (3, 3) and just inside its right corner.
        double leftX = iso.pictureX(3 * 16, 4 * 16) + 1;
        double leftY = iso.pictureY(3 * 16, 4 * 16, 0);
        assertEquals(3, iso.col(leftX, leftY));
        assertEquals(3, iso.row(leftX, leftY));
        double rightX = iso.pictureX(4 * 16, 3 * 16) - 1;
        assertEquals(3, iso.col(rightX, leftY));
        assertEquals(3, iso.row(rightX, leftY));
        // Just outside the left corner is tile (2, 3)... or rather (2, 4): left of the corner lies the next row.
        assertEquals(2, iso.col(leftX - 2, leftY));
        assertEquals(4, iso.row(leftX - 2, leftY));
    }

    @Test
    void theTransformsDrawTheGroundAndBothWalls() {
        IsoProjection iso = IsoProjection.forMap(20, 20, 16, 32, 16, 50);
        Point2D p = iso.ground(10).transform(new Point2D.Double(32, 48), null);
        assertEquals(iso.pictureX(32, 48), p.getX(), 1e-9);
        assertEquals(iso.pictureY(32, 48, 10), p.getY(), 1e-9);
        // The front wall from (16, 32): u along x, v straight up.
        AffineTransform front = iso.frontWall(16, 32);
        Point2D top = front.transform(new Point2D.Double(16, -20), null);
        assertEquals(iso.pictureX(32, 32), top.getX(), 1e-9);
        assertEquals(iso.pictureY(32, 32, 20), top.getY(), 1e-9);
        // The side wall from its front corner (32, 32) back along y.
        Point2D back = iso.sideWall(32, 32).transform(new Point2D.Double(16, 0), null);
        assertEquals(iso.pictureX(32, 16), back.getX(), 1e-9);
        assertEquals(iso.pictureY(32, 16, 0), back.getY(), 1e-9);
    }

    @Test
    void onlyTheTilesInTheViewAreVisible() {
        IsoProjection iso = IsoProjection.forMap(64, 64, 16, 32, 16, 0);
        // A view of 64 × 32 around the middle of tile (20, 20).
        double x = iso.pictureX(20.5 * 16, 20.5 * 16);
        double y = iso.pictureY(20.5 * 16, 20.5 * 16, 0);
        int[] range = iso.visible(x - 32, y - 16, 64, 32, 0);
        assertArrayEquals(new int[]{17, 17, 23, 23}, range);
        // At the edge of the map the range stops at the map.
        int[] corner = iso.visible(iso.pictureX(0, 0) - 32, -40, 64, 48, 2);
        assertEquals(0, corner[0]);
        assertEquals(0, corner[1]);
    }
}
