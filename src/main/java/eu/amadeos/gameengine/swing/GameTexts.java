package eu.amadeos.gameengine.swing;

import eu.amadeos.commons.i18n.Messages;

import java.util.Locale;

/** The texts of amade-game-engine itself: the standard menu entries and the high-score dialogs. */
public final class GameTexts {
    /** Base name of the bundle, for {@link eu.amadeos.commons.i18n.TranslationCheck}. */
    public static final String BUNDLE = "eu.amadeos.gameengine.gameengine";

    private GameTexts() {
    }

    public static Messages load(Locale locale) {
        return Messages.load(GameTexts.class, BUNDLE, locale);
    }

    /** In the default locale, which {@code AmadeApplication} set to the language of the desktop. */
    public static Messages load() {
        return load(Locale.getDefault());
    }
}
