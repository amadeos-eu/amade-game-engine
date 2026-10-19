package eu.amadeos.gameengine.swing;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import java.awt.Component;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Switches a game window between a normal window and full screen, e.g. with F11 from
 * {@link GameMenus#fullScreen(Runnable)}. In full screen the window has no frame and no menu bar; the menu
 * shortcuts keep working.
 *
 * <p>Full screen uses {@link GraphicsDevice#setFullScreenWindow} on the monitor the window is on, but never
 * changes the screen's resolution: the canvas scales the picture itself. Where the platform has no full
 * screen mode, the window simply covers the monitor.
 *
 * <p>A frame can only lose its frame while it is not displayable, so the switch disposes the window and
 * shows it again. That gives the canvas a new native window and sends {@code windowClosed}: end the game
 * with {@link #whenClosed(Runnable)} instead of a window listener of your own.
 */
public final class FullScreen {
    private static final String MIRROR = "amade-fullscreen-shortcut-";

    private final JFrame frame;
    private final GameCanvas canvas;
    private final List<Runnable> closeActions = new ArrayList<>();
    private final List<KeyStroke> mirrored = new ArrayList<>();
    /** The windowClosed events of our own dispose() calls still to come. */
    private int ownCloses;
    private boolean active;
    private Rectangle windowBounds;
    private int windowState;

    public FullScreen(JFrame frame, GameCanvas canvas) {
        this.frame = frame;
        this.canvas = canvas;
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (ownCloses > 0) {
                    ownCloses--;
                    return;
                }
                closeActions.forEach(Runnable::run);
            }
        });
    }

    public boolean active() {
        return active;
    }

    /** Runs the action when the window is really closed, not when it is only switched. */
    public void whenClosed(Runnable action) {
        closeActions.add(action);
    }

    public void toggle() {
        if (active) leave();
        else enter();
    }

    // --8<-- [start:enter]
    public void enter() {
        if (active) return;
        GraphicsDevice device = frame.getGraphicsConfiguration().getDevice();
        windowBounds = frame.getBounds();
        windowState = frame.getExtendedState();
        canvas.withoutFrames(() -> {
            reopen(true);
            if (device.isFullScreenSupported()) {
                device.setFullScreenWindow(frame);
            } else {
                frame.setBounds(device.getDefaultConfiguration().getBounds());
                frame.setVisible(true);
            }
        });
        active = true;
        mirrorShortcuts();
        canvas.requestFocusInWindow();
    }
    // --8<-- [end:enter]

    public void leave() {
        if (!active) return;
        GraphicsDevice device = frame.getGraphicsConfiguration().getDevice();
        canvas.withoutFrames(() -> {
            if (device.getFullScreenWindow() == frame) device.setFullScreenWindow(null);
            reopen(false);
            frame.setBounds(windowBounds);
            frame.setExtendedState(windowState);
            frame.setVisible(true);
        });
        active = false;
        unmirrorShortcuts();
        canvas.requestFocusInWindow();
    }

    /** Takes the window off the screen and gives it or takes away its frame and menu bar. */
    private void reopen(boolean full) {
        ownCloses++;
        frame.dispose();
        frame.setUndecorated(full);
        JMenuBar bar = frame.getJMenuBar();
        if (bar != null) bar.setVisible(!full);
    }

    // --8<-- [start:mirror]
    /**
     * A hidden menu bar ignores its shortcuts. While it is hidden, the window's root pane answers them and
     * clicks the menu item.
     */
    private void mirrorShortcuts() {
        JMenuBar bar = frame.getJMenuBar();
        if (bar == null) return;
        InputMap keys = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        for (JMenuItem item : itemsWithShortcuts(bar)) {
            KeyStroke key = item.getAccelerator();
            String name = MIRROR + key;
            keys.put(key, name);
            frame.getRootPane().getActionMap().put(name, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (item.isEnabled()) item.doClick(0);
                }
            });
            mirrored.add(key);
        }
    }
    // --8<-- [end:mirror]

    private void unmirrorShortcuts() {
        InputMap keys = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        for (KeyStroke key : mirrored) {
            keys.remove(key);
            frame.getRootPane().getActionMap().remove(MIRROR + key);
        }
        mirrored.clear();
    }

    static List<JMenuItem> itemsWithShortcuts(JMenuBar bar) {
        List<JMenuItem> items = new ArrayList<>();
        for (int i = 0; i < bar.getMenuCount(); i++) {
            JMenu menu = bar.getMenu(i);
            if (menu != null) collect(menu, items);
        }
        return items;
    }

    private static void collect(JMenu menu, List<JMenuItem> items) {
        for (Component c : menu.getMenuComponents()) {
            if (c instanceof JMenu sub) collect(sub, items);
            else if (c instanceof JMenuItem item && item.getAccelerator() != null) items.add(item);
        }
    }
}
