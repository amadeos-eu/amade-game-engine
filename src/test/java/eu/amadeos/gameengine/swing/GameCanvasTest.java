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
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class GameCanvasTest {
    private JFrame frame;

    @AfterEach
    void close() throws Exception {
        if (frame != null) onEdt(frame::dispose);
    }

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
