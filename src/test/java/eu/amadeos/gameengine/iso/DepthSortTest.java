package eu.amadeos.gameengine.iso;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepthSortTest {
    private static final IsoProjection ISO = IsoProjection.forMap(40, 40, 16, 32, 16, 100);

    /** Adds a footprint in tiles with a box in the picture from its corners up to {@code height}. */
    private static void add(DepthSort<String> sort, double c0, double r0, double c1, double r1, double height,
                            String name) {
        double x0 = c0 * 16, y0 = r0 * 16, x1 = c1 * 16, y1 = r1 * 16;
        sort.add(x0, y0, x1, y1, ISO.pictureX(x0, y1), ISO.pictureY(x0, y0, height), ISO.pictureX(x1, y0),
                ISO.pictureY(x1, y1, 0), name);
    }

    // --8<-- [start:test-sort]
    @Test
    void aTreeBehindTheEndOfALongBuildingIsDrawnBeforeIt() {
        // A hall of six tiles by two; a tree on the tile behind its right end, at a larger x + y than the
        // hall's middle. Sorting by x + y alone would draw the tree over the hall's wall.
        DepthSort<String> sort = new DepthSort<>();
        add(sort, 10, 10, 16, 12, 30, "hall");
        add(sort, 15, 9, 16, 10, 20, "tree");
        assertTrue(15 + 16 + 9 + 10 > 10 + 16 + 10 + 12, "the tree's x + y is the larger");
        assertEquals(List.of("tree", "hall"), sort.sorted());
    }
    // --8<-- [end:test-sort]

    @Test
    void thingsInFrontComeLastWhicheverWayTheyAreAdded() {
        DepthSort<String> sort = new DepthSort<>();
        add(sort, 5, 5, 6, 6, 10, "front");
        add(sort, 4, 5, 5, 6, 10, "left");
        add(sort, 5, 4, 6, 5, 10, "right");
        add(sort, 4, 4, 5, 5, 10, "back");
        List<String> order = sort.sorted();
        assertEquals("back", order.getFirst());
        assertEquals("front", order.getLast());
        assertEquals(0, sort.size(), "empty for the next frame");
    }

    @Test
    void thingsThatDoNotTouchInThePictureKeepTheOrderOfTheirDepth() {
        DepthSort<String> sort = new DepthSort<>();
        add(sort, 30, 30, 31, 31, 5, "far");
        add(sort, 1, 1, 2, 2, 5, "near the top");
        assertEquals(List.of("near the top", "far"), sort.sorted());
    }

    @Test
    void footprintsBehindEachOtherBothWaysAreNotOrderedByTheRule() {
        assertTrue(DepthSort.behind(0, 0, 1, 1, 1, 0, 2, 1));
        assertFalse(DepthSort.behind(1, 0, 2, 1, 0, 0, 1, 1));
        // Diagonal neighbours: A ends before B along x, B ends before A along y. Neither covers the other.
        assertFalse(DepthSort.behind(0, 1, 1, 2, 1, 0, 2, 1));
        assertFalse(DepthSort.behind(1, 0, 2, 1, 0, 1, 1, 2));
    }

    @Test
    void aBigCityIsSortedQuickly() {
        DepthSort<Integer> sort = new DepthSort<>();
        long start = System.nanoTime();
        for (int frame = 0; frame < 20; frame++) {
            for (int r = 0; r < 30; r++) {
                for (int c = 0; c < 30; c++) {
                    double x0 = c * 16, y0 = r * 16;
                    sort.add(x0, y0, x0 + 16, y0 + 16, ISO.pictureX(x0, y0 + 16), ISO.pictureY(x0, y0, 40),
                            ISO.pictureX(x0 + 16, y0), ISO.pictureY(x0 + 16, y0 + 16, 0), r * 30 + c);
                }
            }
            List<Integer> order = sort.sorted();
            assertEquals(900, order.size());
            // Every tile comes after the tiles behind it.
            int[] at = new int[900];
            for (int i = 0; i < 900; i++) at[order.get(i)] = i;
            assertTrue(at[5 * 30 + 6] > at[4 * 30 + 6] && at[5 * 30 + 6] > at[5 * 30 + 5]);
        }
        assertTrue(System.nanoTime() - start < 2_000_000_000L, "900 things, 20 frames");
    }
}
