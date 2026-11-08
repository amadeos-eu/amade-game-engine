package eu.amadeos.gameengine.pinball;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PartsTest {
    @Test
    void theThirdWarningTiltsTheFourthNudge() {
        Tilt tilt = new Tilt(2);
        assertEquals(Tilt.Result.NUDGE, tilt.nudge());
        assertEquals(Tilt.Result.WARNING, tilt.nudge());
        assertEquals(Tilt.Result.WARNING, tilt.nudge());
        assertEquals(Tilt.Result.TILT, tilt.nudge());
        assertTrue(tilt.tilted());
        tilt.reset();
        assertFalse(tilt.tilted());
    }

    @Test
    void nudgesFarApartAreNoWarning() {
        Tilt tilt = new Tilt(2);
        for (int i = 0; i < 10; i++) {
            assertEquals(Tilt.Result.NUDGE, tilt.nudge());
            tilt.settle(1.5);
        }
    }

    @Test
    void aLongerPullShootsHarder() {
        Plunger short_ = new Plunger(1, 300, 1600);
        short_.update(true, 0.2);
        double weak = short_.update(false, 0.01);
        Plunger long_ = new Plunger(1, 300, 1600);
        for (int i = 0; i < 20; i++) long_.update(true, 0.1);
        double strong = long_.update(false, 0.01);
        assertTrue(weak > 0 && strong > weak);
        assertEquals(1600, strong, 1e-6);
        assertEquals(0, long_.update(false, 0.01), "fires once");
    }

    @Test
    void lampsFadeInAndBlink() {
        Lamps lamps = new Lamps();
        lamps.set("a", Lamps.State.ON);
        lamps.update(0.1);
        assertEquals(1, lamps.level("a"), 1e-9);
        lamps.set("a", Lamps.State.OFF);
        lamps.update(0.05);
        assertTrue(lamps.level("a") > 0 && lamps.level("a") < 1, "fades out");
        lamps.set("b", Lamps.State.FAST);
        boolean seenOn = false, seenOff = false;
        for (int i = 0; i < 60; i++) {
            lamps.update(0.01);
            if (lamps.level("b") > 0.9) seenOn = true;
            if (lamps.level("b") < 0.1) seenOff = true;
        }
        assertTrue(seenOn && seenOff);
        lamps.chase(List.of("x", "y", "z"));
        lamps.update(0.2);
        assertTrue(lamps.level("x") + lamps.level("y") + lamps.level("z") > 0);
    }

    @Test
    void theDotMatrixShowsText() {
        DotMatrix dmd = new DotMatrix(128, 32);
        dmd.show("JACKPOT", "1,000,000");
        int lit = 0;
        for (int y = 0; y < 32; y++) for (int x = 0; x < 128; x++) if (dmd.on(x, y)) lit++;
        assertTrue(lit > 100, "dots lit: " + lit);
        dmd.clear();
        assertFalse(dmd.on(64, 16));
    }

    @Test
    void theChromeBallIsRoundAndShiny() {
        BufferedImage ball = new ChromeBall(new Color(0x2c1450)).picture(24);
        assertEquals(0, ball.getRGB(0, 0) >>> 24, "corner transparent");
        assertEquals(255, ball.getRGB(12, 12) >>> 24, "centre solid");
        int highlight = ball.getRGB(7, 7);
        assertTrue(((highlight >> 16) & 255) > 200, "a bright highlight at the top left");
    }

    @Test
    void aTrackRisesAndFallsWithItsPoints() {
        Track t = new Track("t", 0, 0, 100, 0, 0, 0, 30, 0, -100, 0);
        assertEquals(200, t.length(), 1e-9);
        assertEquals(15, t.heightAt(50), 1e-9);
        assertEquals(30, t.heightAt(100), 1e-9);
        assertEquals(-1, t.direction(10)[1], 1e-9);
    }
}
