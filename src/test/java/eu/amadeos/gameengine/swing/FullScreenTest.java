package eu.amadeos.gameengine.swing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** A real window switched to full screen and back. */
class FullScreenTest {
    private static final KeyStroke F2 = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0);

    private JFrame frame;
    private FullScreen fullScreen;
    private int newGames;
    private int closes;
    private int deactivations;

    @BeforeEach
    void open() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        onEdt(() -> {
            frame = new JFrame("full screen test");
            GameCanvas canvas = new GameCanvas(64, 40, (g, alpha) -> { });
            frame.getContentPane().add(canvas, BorderLayout.CENTER);
            JMenu game = new JMenu("Game");
            JMenuItem newGame = new JMenuItem("New");
            newGame.setAccelerator(F2);
            newGame.addActionListener(e -> newGames++);
            game.add(newGame);
            game.add(new JMenuItem("No shortcut"));
            JMenuBar bar = new JMenuBar();
            bar.add(game);
            frame.setJMenuBar(bar);
            fullScreen = new FullScreen(frame, canvas);
            fullScreen.whenClosed(() -> closes++);
            fullScreen.whenDeactivated(() -> deactivations++);
            frame.setBounds(40, 30, 320, 200);
            frame.setVisible(true);
        });
    }

    @AfterEach
    void close() throws Exception {
        if (frame != null) onEdt(() -> frame.dispose());
    }

    // --8<-- [start:test-deactivated]
    @Test
    void losingTheFocusWhileSwitchingDoesNotCount() throws Exception {
        onEdt(() -> {
            deactivations = 0;
            loseTheFocus();
            assertEquals(1, deactivations, "another program took the focus: the game pauses");
            fullScreen.enter();
            loseTheFocus();
            assertEquals(1, deactivations, "the switch itself took it for a moment: nothing yet");
        });
    }
    // --8<-- [end:test-deactivated]

    @Test
    void entersAndLeavesWithoutClosingTheGame() throws Exception {
        onEdt(() -> {
            fullScreen.enter();
            assertTrue(fullScreen.active());
            assertTrue(frame.isUndecorated());
            assertFalse(frame.getJMenuBar().isVisible());
            Rectangle screen = frame.getGraphicsConfiguration().getBounds();
            assertEquals(screen.getSize(), frame.getSize());
        });
        onEdt(() -> {
            fullScreen.leave();
            assertFalse(fullScreen.active());
            assertFalse(frame.isUndecorated());
            assertTrue(frame.getJMenuBar().isVisible());
        });
        settle();
        assertEquals(0, closes, "switching is not closing");
        onEdt(() -> frame.dispose());
        settle();
        assertEquals(1, closes);
    }

    @Test
    void menuShortcutsWorkWhileTheMenuBarIsHidden() throws Exception {
        onEdt(() -> {
            fullScreen.enter();
            Object name = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(F2);
            assertNotNull(name);
            frame.getRootPane().getActionMap().get(name).actionPerformed(null);
            assertEquals(1, newGames);
            fullScreen.leave();
            assertEquals(null, frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(F2));
        });
    }

    @Test
    void findsTheItemsWithShortcuts() throws Exception {
        onEdt(() -> {
            List<JMenuItem> items = FullScreen.itemsWithShortcuts(frame.getJMenuBar());
            assertEquals(1, items.size());
            assertEquals("New", items.get(0).getText());
        });
    }

    /**
     * Tells the window's listeners that it lost the focus. frame.dispatchEvent would not do: the focus
     * manager drops a WINDOW_DEACTIVATED for a window that is not the active one, and on a desktop where
     * the terminal keeps the focus the test window never becomes active.
     */
    private void loseTheFocus() {
        WindowEvent lost = new WindowEvent(frame, WindowEvent.WINDOW_DEACTIVATED);
        for (WindowListener listener : frame.getWindowListeners()) listener.windowDeactivated(lost);
    }

    /** Lets the events the switch posted (window closed, focus) be handled. */
    private static void settle() throws Exception {
        Toolkit.getDefaultToolkit().sync();
        Thread.sleep(200);
        onEdt(() -> { });
    }

    private static void onEdt(Runnable r) throws InterruptedException, InvocationTargetException {
        try {
            SwingUtilities.invokeAndWait(r);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof AssertionError a) throw a;
            throw e;
        }
    }
}
