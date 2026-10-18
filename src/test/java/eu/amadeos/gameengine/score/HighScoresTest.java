package eu.amadeos.gameengine.score;

import eu.amadeos.commons.config.XdgDirectories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HighScoresTest {
    private static final Instant T0 = Instant.parse("2026-10-18T12:00:00Z");

    @TempDir
    Path dir;

    private static Score score(String name, long value, int minute) {
        return new Score(name, value, T0.plusSeconds(60L * minute));
    }

    @Test
    void liesInTheXdgDataDirectory() {
        XdgDirectories xdg = new XdgDirectories(Map.of(), "/home/anna");
        assertEquals(Path.of("/home/anna/.local/share/amadeos/games/mines"),
                HighScores.of(xdg, "mines", Ranking.LOWER_WINS).directory());
        XdgDirectories custom = new XdgDirectories(Map.of("XDG_DATA_HOME", "/data"), "/home/anna");
        assertEquals(Path.of("/data/amadeos/games/mines"), HighScores.of(custom, "mines", Ranking.LOWER_WINS).directory());
    }

    @Test
    void emptyBeforeTheFirstEntry() {
        HighScores scores = new HighScores(dir, Ranking.LOWER_WINS);
        assertEquals(List.of(), scores.list("beginner"));
        assertTrue(scores.qualifies("beginner", 999));
    }

    @Test
    void lowerWinsSortsFastestFirstAndOlderFirstOnATie() {
        HighScores scores = new HighScores(dir, Ranking.LOWER_WINS);
        assertEquals(0, scores.add("beginner", score("Anna", 50, 0)));
        assertEquals(0, scores.add("beginner", score("Ben", 30, 1)));
        assertEquals(2, scores.add("beginner", score("Cleo", 50, 2)));
        assertEquals(List.of("Ben", "Anna", "Cleo"), scores.list("beginner").stream().map(Score::name).toList());
    }

    @Test
    void higherWinsSortsMostPointsFirst() {
        HighScores scores = new HighScores(dir, Ranking.HIGHER_WINS);
        scores.add("points", score("Anna", 100, 0));
        scores.add("points", score("Ben", 300, 1));
        assertEquals(List.of(300L, 100L), scores.list("points").stream().map(Score::value).toList());
    }

    @Test
    void keepsTheBestTen() {
        HighScores scores = new HighScores(dir, Ranking.LOWER_WINS);
        for (int i = 1; i <= HighScores.SIZE; i++) scores.add("expert", score("P" + i, i * 10, i));
        assertFalse(scores.qualifies("expert", 100));
        assertFalse(scores.qualifies("expert", 101));
        assertTrue(scores.qualifies("expert", 99));
        assertEquals(-1, scores.add("expert", score("Slow", 500, 20)));
        assertEquals(3, scores.add("expert", score("Fast", 35, 21)));
        List<Score> list = scores.list("expert");
        assertEquals(HighScores.SIZE, list.size());
        assertEquals("Fast", list.get(3).name());
        assertEquals(90, list.getLast().value());
    }

    @Test
    void tablesAreSeparateFiles() {
        HighScores scores = new HighScores(dir, Ranking.LOWER_WINS);
        scores.add("beginner", score("Anna", 10, 0));
        scores.add("expert", score("Ben", 200, 0));
        assertTrue(Files.exists(dir.resolve("beginner.scores")));
        assertTrue(Files.exists(dir.resolve("expert.scores")));
        scores.clear("beginner");
        assertEquals(List.of(), scores.list("beginner"));
        assertEquals(1, scores.list("expert").size());
    }

    @Test
    void survivesAnotherInstanceAndDamagedLines() throws Exception {
        new HighScores(dir, Ranking.LOWER_WINS).add("beginner", score("Anna", 42, 0));
        Files.writeString(dir.resolve("beginner.scores"), "garbage\nx\t2026\tBen\n", StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.APPEND);
        List<Score> list = new HighScores(dir, Ranking.LOWER_WINS).list("beginner");
        assertEquals(List.of(score("Anna", 42, 0)), list);
    }

    @Test
    void namesStayOnOneLine() {
        HighScores scores = new HighScores(dir, Ranking.LOWER_WINS);
        scores.add("beginner", score("  Anna\tvon\nHof  ", 42, 0));
        assertEquals("Anna von Hof", scores.list("beginner").getFirst().name());
        assertEquals(Score.MAX_NAME, new Score("x".repeat(100), 1, T0).name().length());
    }

    @Test
    void refusesNamesThatAreNoFileNames() {
        HighScores scores = new HighScores(dir, Ranking.LOWER_WINS);
        assertThrows(IllegalArgumentException.class, () -> scores.list("../etc"));
        assertThrows(IllegalArgumentException.class, () -> scores.list("Expert"));
    }

    @Test
    void remembersThePlayerName() {
        PlayerName name = new PlayerName(dir, "anna");
        assertEquals("anna", name.get());
        name.set(" Anna Huber ");
        assertEquals("Anna Huber", new PlayerName(dir, "anna").get());
    }
}
