package eu.amadeos.gameengine.data;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextDataTest {
    // --8<-- [start:test-textdata]
    @Test
    void linesBecomeWordsAndErrorsSayWhere() {
        List<TextData.Line> lines = TextData.parse("waves.txt", new StringReader("""
                # Stage 1
                stage 1

                group 0.5 swoop-left   bee 1 3   # two bees
                speed fast
                """));
        assertEquals(3, lines.size());
        TextData.Line group = lines.get(1);
        assertEquals("group", group.keyword());
        assertEquals(4, group.lineNumber());
        assertEquals(0.5, group.number(1));
        assertEquals(List.of("group", "0.5", "swoop-left", "bee", "1", "3"), group.words());
        TextData.DataException error = assertThrows(TextData.DataException.class, () -> lines.get(2).number(1));
        assertEquals("waves.txt:5: \"fast\" is not a number", error.getMessage());
    }
    // --8<-- [end:test-textdata]

    @Test
    void missingWordsAndWholeNumbers() {
        TextData.Line line = TextData.parse("a.txt", new StringReader("stage 2.5")).getFirst();
        assertEquals("a.txt:1: \"2.5\" is not a whole number",
                assertThrows(TextData.DataException.class, () -> line.integer(1)).getMessage());
        assertEquals("a.txt:1: expected 3 words, found 2",
                assertThrows(TextData.DataException.class, () -> line.word(2)).getMessage());
        assertEquals("a.txt:1: no such stage", line.error("no such stage").getMessage());
    }

    @Test
    void filesAreReadFromTheClassPath() {
        assertThrows(IllegalArgumentException.class, () -> TextData.load(TextDataTest.class, "/nothing.txt"));
    }
}
