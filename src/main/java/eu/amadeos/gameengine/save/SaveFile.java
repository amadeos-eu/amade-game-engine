package eu.amadeos.gameengine.save;

import eu.amadeos.commons.config.XdgDirectories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.zip.CRC32;

/**
 * A player's progress on disk: which levels are open, the best times, what was collected. Settings may get
 * lost without harm; progress may not. A player who has worked through ten levels and finds the game back
 * at level one after a crash does not play again. So a save file is written with more care than settings:
 *
 * <ul>
 *   <li><b>Never half written.</b> The new state goes into a temporary file next to the save, is forced
 *   onto the disk, and only then takes the save's name, in one atomic rename. A crash or a power cut
 *   leaves either the old file or the new one, never a mixture.</li>
 *   <li><b>A backup.</b> The file before the last save is kept as {@code <name>.bak}. If the save itself
 *   is damaged, the backup is read instead: the player loses one step, not everything.</li>
 *   <li><b>A checksum.</b> The last line is a CRC-32 of everything above it. A file cut short, or changed
 *   by a disk error, is recognised and not read as if it were whole.</li>
 *   <li><b>A version.</b> The first line says which version of the game's format the file is in, so a
 *   newer game can read and convert what an older one wrote.</li>
 * </ul>
 *
 * <p>The content is plain text, keys and values as in a properties file, sorted, so it can be read and
 * compared:
 *
 * <pre>
 * amade-save 1
 * level.1.best=71.25
 * level.1.coins=34
 * unlocked=2
 * crc32 5e1f0c2a
 * </pre>
 *
 * <p>Not thread-safe: save from one thread, or one at a time.
 */
// --8<-- [start:savefile]
public final class SaveFile {
    /** What was read: the version of the format it was written in, and the keys and values. */
    public record Saved(int version, Map<String, String> values) {
        public Saved {
            values = Map.copyOf(values);
        }

        public String get(String key, String fallback) {
            return values.getOrDefault(key, fallback);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(SaveFile.class);
    private static final String HEADER = "amade-save ";
    private static final String CHECKSUM = "crc32 ";

    private final Path file;
    private final Path backup;

    public SaveFile(Path file) {
        this.file = file;
        this.backup = file.resolveSibling(file.getFileName() + ".bak");
    }

    /** The save {@code <name>.save} of a game in {@code $XDG_DATA_HOME/amadeos/games/<game>}. */
    public static SaveFile of(XdgDirectories xdg, String game, String name) {
        return new SaveFile(xdg.dataHome().resolve("amadeos").resolve("games").resolve(game).resolve(name + ".save"));
    }

    /**
     * The last state saved: the save itself, or the backup if the save is missing or damaged; empty if
     * neither can be read, as on the first start.
     */
    public Optional<Saved> load() {
        for (Path candidate : List.of(file, backup)) {
            try {
                return Optional.of(parse(Files.readString(candidate, StandardCharsets.UTF_8)));
            } catch (NoSuchFileException e) {
                // Nothing saved yet, or no backup yet.
            } catch (IOException | IllegalArgumentException e) {
                LOG.warn("Cannot read the save {}: {}", candidate, e.getMessage());
            }
        }
        return Optional.empty();
    }

    /** Writes a new state; the one before becomes the backup. */
    public void store(int version, Map<String, String> values) {
        String text = format(version, values);
        try {
            Files.createDirectories(file.getParent());
            Path temp = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
            try {
                try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                    channel.write(ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)));
                    channel.force(true);            // on the disk, not only in the system's cache
                }
                if (Files.exists(file)) move(file, backup);
                move(temp, file);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + file, e);
        }
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException | AccessDeniedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** The text of a save: the version, the sorted keys and values, the checksum of all that. */
    static String format(int version, Map<String, String> values) {
        StringBuilder text = new StringBuilder(HEADER).append(version).append('\n');
        for (Map.Entry<String, String> entry : new TreeMap<>(values).entrySet()) {
            text.append(escape(entry.getKey(), true)).append('=').append(escape(entry.getValue(), false)).append('\n');
        }
        String sum = crc(text);
        return text.append(CHECKSUM).append(sum).append('\n').toString();
    }

    /** Reads a save's text; an {@link IllegalArgumentException} if it is not whole. */
    static Saved parse(String text) {
        int last = text.lastIndexOf(CHECKSUM);
        if (!text.startsWith(HEADER) || last < 0 || !text.endsWith("\n")) {
            throw new IllegalArgumentException("not a whole save");
        }
        String body = text.substring(0, last);
        String sum = text.substring(last + CHECKSUM.length()).strip();
        if (!sum.equals(crc(body))) throw new IllegalArgumentException("wrong checksum, the file is damaged");
        int newline = body.indexOf('\n');
        int version;
        try {
            version = Integer.parseInt(body.substring(HEADER.length(), newline));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("no version");
        }
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(body.substring(newline + 1)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Map<String, String> values = new TreeMap<>();
        for (String key : properties.stringPropertyNames()) values.put(key, properties.getProperty(key));
        return new Saved(version, values);
    }
    // --8<-- [end:savefile]

    private static String crc(CharSequence text) {
        CRC32 crc = new CRC32();
        crc.update(text.toString().getBytes(StandardCharsets.UTF_8));
        return String.format(Locale.ROOT, "%08x", crc.getValue());
    }

    /** Escapes as a properties file does, so that any text survives: line breaks, '=', ':' and the like. */
    private static String escape(String text, boolean key) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '=', ':', '#', '!' -> out.append('\\').append(c);
                case ' ' -> out.append(key || i == 0 ? "\\ " : " ");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    public Path file() {
        return file;
    }

    public Path backup() {
        return backup;
    }

    /** Deletes the save and its backup: a fresh start. */
    public void delete() {
        try {
            Files.deleteIfExists(file);
            Files.deleteIfExists(backup);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot delete " + file, e);
        }
    }
}
