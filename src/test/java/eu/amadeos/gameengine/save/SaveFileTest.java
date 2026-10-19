package eu.amadeos.gameengine.save;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveFileTest {
    @TempDir
    Path folder;

    // --8<-- [start:test-save]
    @Test
    void whatWasSavedIsReadBack() {
        SaveFile save = new SaveFile(folder.resolve("progress.save"));
        assertTrue(save.load().isEmpty(), "nothing saved yet");
        save.store(1, Map.of("unlocked", "2", "level.1.best", "71.25", "name", "Ada = the first: #1"));
        SaveFile.Saved saved = save.load().orElseThrow();
        assertEquals(1, saved.version());
        assertEquals("71.25", saved.get("level.1.best", ""));
        assertEquals("Ada = the first: #1", saved.get("name", ""), "any text survives");
    }

    @Test
    void theFileIsPlainSortedTextWithAChecksum() {
        String text = SaveFile.format(1, Map.of("unlocked", "2", "level.1.best", "71.25"));
        assertEquals("""
                amade-save 1
                level.1.best=71.25
                unlocked=2
                crc32 %s
                """.formatted(text.lines().toList().getLast().substring(6)), text);
    }

    @Test
    void aDamagedSaveIsNotReadAsIfItWereWhole() {
        String text = SaveFile.format(1, Map.of("unlocked", "2"));
        assertThrows(IllegalArgumentException.class, () -> SaveFile.parse(text.replace("unlocked=2", "unlocked=9")));
        assertThrows(IllegalArgumentException.class, () -> SaveFile.parse(text.substring(0, text.length() / 2)));
    }

    @Test
    void aDamagedSaveFallsBackToTheStateBefore() throws IOException {
        SaveFile save = new SaveFile(folder.resolve("progress.save"));
        save.store(1, Map.of("unlocked", "2"));
        save.store(1, Map.of("unlocked", "3"));
        assertEquals("unlocked=2", Files.readAllLines(save.backup()).get(1), "the state before is the backup");
        // The save is cut short, as by a full disk: the backup is read instead.
        Files.writeString(save.file(), "amade-save 1\nunlocked=", StandardCharsets.UTF_8);
        assertEquals("2", save.load().orElseThrow().get("unlocked", ""));
    }
    // --8<-- [end:test-save]

    @Test
    void noTemporaryFilesAreLeftBehind() throws IOException {
        SaveFile save = new SaveFile(folder.resolve("games/jump/progress.save"));
        save.store(1, Map.of("a", "1"));
        save.store(1, Map.of("a", "2"));
        try (var files = Files.list(save.file().getParent())) {
            assertEquals(2, files.count(), "the save and its backup");
        }
        save.delete();
        assertFalse(Files.exists(save.file()));
        assertTrue(save.load().isEmpty());
    }
}
