package eu.amadeos.gameengine.pinball;

import org.junit.jupiter.api.Test;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class PerspectiveTest {
    private final Perspective view = new Perspective(232, 392, Perspective.trapezoid(200, 30, 350, 240, 360));

    @Test
    void theCornersOfTheTableAreTheCornersOfTheQuad() {
        assertEquals(80, view.view(0, 0).x, 1e-6);
        assertEquals(30, view.view(0, 0).y, 1e-6);
        assertEquals(380, view.view(232, 392).x, 1e-6);
        assertEquals(350, view.view(232, 392).y, 1e-6);
    }

    @Test
    void tableAndViewAreInverse() {
        Point2D.Double s = view.view(57, 211);
        Point2D.Double t = view.table(s.x, s.y);
        assertEquals(57, t.x, 1e-6);
        assertEquals(211, t.y, 1e-6);
    }

    @Test
    void theFarEndLooksSmallerAndItsStepsShorter() {
        assertTrue(view.scale(116, 20) < view.scale(116, 370));
        double farStep = view.view(116, 40).y - view.view(116, 20).y;
        double nearStep = view.view(116, 380).y - view.view(116, 360).y;
        assertTrue(farStep < nearStep);
    }

    @Test
    void theLocalTransformAgreesNearItsPoint() {
        AffineTransform t = view.local(100, 200);
        Point2D near = t.transform(new Point2D.Double(103, 198), null);
        Point2D exact = view.view(103, 198);
        assertEquals(exact.getX(), near.getX(), 0.05);
        assertEquals(exact.getY(), near.getY(), 0.05);
    }

    @Test
    void warpingPaintsTheTrapezoidOnly() {
        BufferedImage flat = new BufferedImage(232, 392, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = flat.createGraphics();
        g.setColor(java.awt.Color.RED);
        g.fillRect(0, 0, 232, 392);
        g.dispose();
        BufferedImage dst = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        view.warp(flat, 0, 0, 232, 392, dst, new AffineTransform());
        assertEquals(0xffff0000, dst.getRGB(200, 200));
        assertEquals(0, dst.getRGB(60, 35) >>> 24, "beside the narrow far end stays empty");
        assertEquals(0, dst.getRGB(5, 200) >>> 24);
    }
}
