package eu.amadeos.gameengine.raycast.model;

import eu.amadeos.gameengine.raycast.Texture;
import eu.amadeos.gameengine.raycast.Views;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelRendererTest {
    /** A grey ball with a red glowing dot on its front. */
    static final Model BALL = new Model() {
        @Override
        protected void shape(double x, double y, double z) {
            part(Sdf.sphere(x, y, z, 0.5), 0);
            part(Sdf.sphere(x, y, z - 0.5, 0.12), 1);
        }

        @Override
        protected Material material(int part, double x, double y, double z) {
            return part == 1 ? Material.light(0xff2000) : Material.paint(0x909090);
        }
    };

    @Test
    void distancesAreNegativeInsideAndPositiveOutside() {
        assertTrue(BALL.distance(0, 0, 0) < 0);
        assertEquals(0.5, BALL.distance(1, 0, 0), 1e-9);
        assertEquals(0xff2000, BALL.materialAt(0, 0, 0.62).rgb());
    }

    @Test
    void aBillboardHasTheModelInTheMiddleAndHolesAround() {
        Texture t = new ModelRenderer().billboard(BALL, 32, 0);
        assertFalse(t.opaque(0, 0));
        assertTrue(t.opaque(10, 16));
        assertEquals(1, t.glow(16, 16), 0.01);        // the dot faces the camera and glows
        assertEquals(0, t.glow(10, 16), 0.01);
        int lit = t.argb(12, 12) & 0xff, shade = t.argb(20, 20) & 0xff;
        assertTrue(lit > shade, "the key light comes from the upper left");
    }

    @Test
    void turningShowsOtherSides() {
        Views v = new ModelRenderer().views(f -> BALL, 1, 4, 32, 0);
        assertEquals(4, v.directions());
        Texture front = v.pick(0, 0, 0), back = v.pick(0, 0, Math.PI);
        assertEquals(1, front.glow(16, 16), 0.01);
        assertEquals(0, back.glow(16, 16), 0.01);
    }

    @Test
    void aPictureInPerspectiveIsTransparentAroundTheModel() {
        BufferedImage image = new ModelRenderer().picture(BALL, 40, 40, 40,
                new ModelRenderer.Placement(0, 0, 3, 180, 0, 0), List.of(ModelRenderer.Lamp.from(0, 1, -1, 1, 1, 1)));
        assertEquals(0, image.getRGB(0, 0) >>> 24);
        assertEquals(255, image.getRGB(20, 20) >>> 24);
        // turned 180°, the front with the glowing dot looks at the camera
        assertEquals(0xff2000, image.getRGB(20, 20) & 0xffffff);
    }
}
