package eu.amadeos.gameengine.swing;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * The menu entries every game has, with the same texts, mnemonics and keys in all of them:
 *
 * <pre>
 * GameMenus menus = new GameMenus(GameTexts.load());
 * JMenu game = menus.game();
 * game.add(menus.newGame(this::newGame));   // F2
 * ... the game's own entries ...
 * game.add(menus.highScores(this::showScores));
 * game.addSeparator();
 * game.add(menus.exit(this::dispose));      // Ctrl+Q
 * </pre>
 */
public final class GameMenus {
    private final Messages texts;

    public GameMenus(Messages texts) {
        this.texts = texts;
    }

    /** The texts the entries use, also for the game's own dialogs. */
    public Messages texts() {
        return texts;
    }

    public JMenu game() {
        return menu("menu.game", "menu-game");
    }

    public JMenu help() {
        return menu("menu.help", "menu-help");
    }

    public JMenuItem newGame(Runnable action) {
        return item("menu.game.new", "game-new", KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), action);
    }

    public JMenuItem highScores(Runnable action) {
        return item("menu.game.scores", "game-scores", null, action);
    }

    public JMenuItem exit(Runnable action) {
        return item("menu.game.exit", "game-exit", KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK), action);
    }

    /** "About &lt;title&gt;". */
    public JMenuItem about(String title, Runnable action) {
        JMenuItem item = Mnemonics.apply(new JMenuItem(), texts.format("menu.help.about", title));
        item.setName("help-about");
        item.addActionListener(e -> action.run());
        return item;
    }

    private JMenu menu(String key, String name) {
        JMenu menu = Mnemonics.apply(new JMenu(), texts.get(key));
        menu.setName(name);
        return menu;
    }

    private JMenuItem item(String key, String name, KeyStroke accelerator, Runnable action) {
        JMenuItem item = Mnemonics.apply(new JMenuItem(), texts.get(key));
        item.setName(name);
        if (accelerator != null) item.setAccelerator(accelerator);
        item.addActionListener(e -> action.run());
        return item;
    }
}
