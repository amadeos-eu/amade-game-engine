package eu.amadeos.gameengine.score;

import eu.amadeos.commons.config.PropertiesFile;

import java.nio.file.Path;
import java.util.Properties;

/**
 * The name a player last typed for a high score, offered again next time. Kept in
 * {@code player.properties} in the game's configuration directory; the first time it is the login name.
 */
public final class PlayerName {
    private static final String KEY = "name";

    private final Path file;
    private final String fallback;

    /**
     * @param configDirectory e.g. {@code ~/.config/amadeos/mines}
     * @param fallback        the name before anything was typed, usually {@code System.getProperty("user.name")}
     */
    public PlayerName(Path configDirectory, String fallback) {
        this.file = configDirectory.resolve("player.properties");
        this.fallback = fallback == null ? "" : fallback;
    }

    public String get() {
        String name = PropertiesFile.load(file).getProperty(KEY);
        return name == null || name.isBlank() ? fallback : name;
    }

    public void set(String name) {
        Properties properties = PropertiesFile.load(file);
        properties.setProperty(KEY, Score.clean(name));
        PropertiesFile.store(file, properties, "Name for high scores");
    }
}
