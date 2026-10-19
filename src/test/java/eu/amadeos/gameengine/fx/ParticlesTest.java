package eu.amadeos.gameengine.fx;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticlesTest {
    @Test
    void particlesFlyFallAndDie() {
        Particles particles = new Particles(10, Color.RED);
        particles.spawn(100, 100, 50, 0, 1.0, 3, 0);
        particles.spawn(0, 0, 0, 0, 0.5, 3, 0);
        for (int i = 0; i < 60; i++) particles.update(1.0 / 120, 400);
        // After half a second: the short-lived one is gone, the other flew right and fell.
        assertEquals(1, particles.count());
        assertEquals(125, particles.x(0), 1e-6);
        assertTrue(particles.y(0) > 140, "fell: " + particles.y(0));
        for (int i = 0; i < 61; i++) particles.update(1.0 / 120, 400);
        assertEquals(0, particles.count());
    }

    @Test
    void aFullSystemDropsNewParticles() {
        Particles particles = new Particles(2, Color.RED);
        assertTrue(particles.spawn(0, 0, 0, 0, 1, 1, 0));
        assertTrue(particles.spawn(0, 0, 0, 0, 1, 1, 0));
        assertFalse(particles.spawn(0, 0, 0, 0, 1, 1, 0));
        assertEquals(2, particles.count());
    }

    @Test
    void particlesAreDrawnInTheirColour() {
        Particles particles = new Particles(4, Color.BLACK, Color.GREEN);
        particles.spawn(5, 5, 0, 0, 1, 4, 1);
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        particles.render(g, 0);
        g.dispose();
        assertEquals(Color.GREEN.getRGB(), image.getRGB(5, 5));
        assertEquals(0, image.getRGB(0, 0));
    }
}
