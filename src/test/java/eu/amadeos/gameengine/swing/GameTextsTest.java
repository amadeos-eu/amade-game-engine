package eu.amadeos.gameengine.swing;

import eu.amadeos.commons.i18n.Language;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.i18n.TranslationCheck;
import org.junit.jupiter.api.Test;

import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameTextsTest {
    @Test
    void everyLanguageHasEveryText() {
        assertEquals(Map.of(), TranslationCheck.problems(GameTexts.class, GameTexts.BUNDLE));
    }

    /** The standard entries of the Game menu leave each other's letters alone in every language. */
    @Test
    void mnemonicsOfTheGameMenuAreUnique() {
        List<String> keys = List.of("menu.game.new", "menu.game.undo", "menu.game.redo", "menu.game.full-screen", "menu.game.scores",
                "menu.game.exit");
        Map<String, String> clashes = new HashMap<>();
        for (Language language : Language.values()) {
            Messages texts = GameTexts.load(language.locale());
            Map<Character, String> seen = new HashMap<>();
            for (String key : keys) {
                Mnemonics.Text t = Mnemonics.parse(texts.get(key));
                if (t.index() < 0) {
                    clashes.put(language.code() + " " + key, "no mnemonic");
                    continue;
                }
                String other = seen.put(Character.toLowerCase(t.text().charAt(t.index())), key);
                if (other != null) clashes.put(language.code() + " " + key, other);
            }
        }
        assertEquals(Map.of(), clashes);
    }

    @Test
    void menuEntriesHaveTheStandardKeys() {
        GameMenus menus = new GameMenus(GameTexts.load(Locale.GERMANY));
        int[] runs = {0};
        JMenuItem newGame = menus.newGame(() -> runs[0]++);
        assertEquals("Neues Spiel", newGame.getText());
        assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), newGame.getAccelerator());
        newGame.doClick();
        assertEquals(1, runs[0]);
        assertEquals("Über Minen", menus.about("Minen", () -> { }).getText());
        assertEquals('Q', menus.exit(() -> { }).getAccelerator().getKeyCode());
    }
}
