package eu.amadeos.gameengine.swing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class GameCanvasTest {
    private JFrame frame;

    @AfterEach
    void close() throws Exception {
        if (frame != null) onEdt(frame::dispose);
    }

    // --8<-- [start:test-to-logical]
    @Test
    void mousePositionsAreMappedLikeThePicture() {
        // 1920 × 1080 drawn directly: scaled 2.7 times to 1728 × 1080, with black bars of 96 pixels left and right.
        Point2D.Double p = GameCanvas.toLogical(96, 0, 1920, 1080, 640, 400, RenderMode.DIRECT);
        assertEquals(0, p.x, 1e-9);
        assertEquals(0, p.y, 1e-9);
        p = GameCanvas.toLogical(960, 540, 1920, 1080, 640, 400, RenderMode.DIRECT);
        assertEquals(320, p.x, 1e-9);
        assertEquals(200, p.y, 1e-9);
        // In pixel mode the same screen shows 2 × 640 × 400 = 1280 × 800, starting at 320, 140.
        p = GameCanvas.toLogical(320, 140, 1920, 1080, 640, 400, RenderMode.PIXEL);
        assertEquals(0, p.x, 1e-9);
        assertEquals(0, p.y, 1e-9);
        p = GameCanvas.toLogical(1599, 939, 1920, 1080, 640, 400, RenderMode.PIXEL);
        assertEquals(639.5, p.x, 1e-9);
        assertEquals(399.5, p.y, 1e-9);
        // On a black bar: outside the field.
        assertTrue(GameCanvas.toLogical(10, 540, 1920, 1080, 640, 400, RenderMode.PIXEL).x < 0);
    }
    // --8<-- [end:test-to-logical]

    // --8<-- [start:test-whole-scale]
    @Test
    void pixelsAreEnlargedByTheLargestWholeFactorThatFits() {
        assertEquals(1, GameCanvas.wholeScale(640, 400, 640, 400));
        assertEquals(1, GameCanvas.wholeScale(1000, 700, 640, 400));
        assertEquals(2, GameCanvas.wholeScale(1920, 1080, 640, 400));   // 1280 × 800 with wide black bars
        assertEquals(4, GameCanvas.wholeScale(2560, 1600, 640, 400));   // exactly 4: no bars at all
        assertEquals(5, GameCanvas.wholeScale(3840, 2160, 640, 400));   // 3200 × 2000
        assertEquals(1, GameCanvas.wholeScale(300, 200, 640, 400));     // smaller than logical: still 1
    }
    // --8<-- [end:test-whole-scale]

    // --8<-- [start:test-width-for]
    @Test
    void aWideningCanvasFillsWideScreens() {
        // 16:9 at 1920 × 1080: 400 units high means 2.7 pixels a unit, and 1920 / 2.7 = 711 units wide.
        assertEquals(711, GameCanvas.widthFor(1920, 1080, 640, 960, 400, RenderMode.DIRECT));
        // 16:10 needs nothing more; a 4:3 screen keeps the narrowest width and gets bars above and below.
        assertEquals(640, GameCanvas.widthFor(1920, 1200, 640, 960, 400, RenderMode.DIRECT));
        assertEquals(640, GameCanvas.widthFor(1024, 768, 640, 960, 400, RenderMode.DIRECT));
        // 21:9 at 3440 × 1440 would be 955 units; a maximum of 900 leaves narrow bars.
        assertEquals(900, GameCanvas.widthFor(3440, 1440, 640, 900, 400, RenderMode.DIRECT));
        // Pixel mode: 1080 / 400 gives a whole 2, so 1920 / 2 = 960 units, and only bars above and below.
        assertEquals(960, GameCanvas.widthFor(1920, 1080, 640, 960, 400, RenderMode.PIXEL));
        // A canvas that may not widen stays as it is.
        assertEquals(640, GameCanvas.widthFor(1920, 1080, 640, 640, 400, RenderMode.DIRECT));
        // With the widened width the screen's left edge is logical 0 and its right edge the full width.
        Point2D.Double left = GameCanvas.toLogical(0, 540, 1920, 1080, 711, 400, RenderMode.DIRECT);
        Point2D.Double right = GameCanvas.toLogical(1919, 540, 1920, 1080, 711, 400, RenderMode.DIRECT);
        assertEquals(0, left.x, 0.5);
        assertEquals(711, right.x, 0.5);
    }
    // --8<-- [end:test-width-for]

    /** Every mode puts the world and the overlay in the same place on a real screen. */
    @Test
    void allModesShowWorldAndOverlay() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        AtomicReference<GameCanvas> ref = new AtomicReference<>();
        onEdt(() -> {
            GameCanvas canvas = new GameCanvas(64, 40,
                    (g, alpha) -> {
                        g.setColor(Color.RED);
                        g.fill(new Rectangle2D.Double(0, 0, 32, 40));
                    },
                    (g, alpha) -> {
                        g.setColor(Color.GREEN);
                        g.fill(new Rectangle2D.Double(32, 0, 32, 40));
                    });
            frame = new JFrame("canvas test");
            frame.setUndecorated(true);
            frame.getContentPane().add(canvas, BorderLayout.CENTER);
            frame.setBounds(20, 20, 64 * 3 + 20, 40 * 3);
            frame.setVisible(true);
            ref.set(canvas);
        });
        GameCanvas canvas = ref.get();
        Robot robot = new Robot();
        for (RenderMode mode : RenderMode.values()) {
            canvas.setRenderMode(mode);
            for (int i = 0; i < 5; i++) {
                canvas.render(0);
                robot.delay(30);
            }
            robot.waitForIdle();
            AtomicReference<Point> origin = new AtomicReference<>();
            onEdt(() -> origin.set(canvas.getLocationOnScreen()));
            BufferedImage shot = robot.createScreenCapture(new Rectangle(origin.get().x, origin.get().y, 212, 120));
            // Three times 64 × 40 fits; ten pixels of black bar on either side.
            assertEquals(Color.BLACK.getRGB(), shot.getRGB(4, 60), mode + " bar");
            assertEquals(Color.RED.getRGB(), shot.getRGB(40, 60), mode + " world");
            assertEquals(Color.GREEN.getRGB(), shot.getRGB(170, 60), mode + " overlay");
        }
        assertTrue(canvas.frameTimes().framesPerSecond() > 0);
        // Copied, not flipped: flipped buffers stayed black in full screen on the amadeOS desktop.
        assertFalse(canvas.getBufferStrategy().getCapabilities().isPageFlipping(), "buffers are copied");
    }

    private static void onEdt(Runnable code) throws Exception {
        try {
            SwingUtilities.invokeAndWait(code);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Error error) throw error;
            throw (Exception) e.getCause();
        }
    }
}
