package eu.amadeos.gameengine.raycast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class ViewsTest {
    private final Texture[] sides = {Maps.solid(0xff000001), Maps.solid(0xff000002), Maps.solid(0xff000003), Maps.solid(0xff000004)};
    private final Views views = new Views(new Texture[][]{sides});

    @Test
    void theViewerInFrontSeesTheFront() {
        // facing east, the viewer stands east of the thing
        assertSame(sides[0], views.pick(0, 0, 0));
        // a quarter turn on the way map angles grow (south of a thing facing east): its right side
        assertSame(sides[1], views.pick(0, 0, Math.PI / 2));
        assertSame(sides[2], views.pick(0, 0, Math.PI));
        assertSame(sides[3], views.pick(0, 0, -Math.PI / 2));
        // the nearest view
        assertSame(sides[0], views.pick(0, Math.PI / 2, Math.PI / 2 + 0.6));
    }
}
