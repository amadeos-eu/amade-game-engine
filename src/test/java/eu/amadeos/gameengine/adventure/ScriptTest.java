package eu.amadeos.gameengine.adventure;

import eu.amadeos.gameengine.data.TextData;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScriptTest {
    static Script parse(String text) {
        return Script.parse(TextData.parse("test.script", new StringReader(text)));
    }

    @Test
    void findsTheBlockForATriggerExactWordsFirst() {
        Script script = parse("""
                on look *
                  say lena nothing-special
                end
                on look psalter
                  say lena psalter.look
                end
                on use * psalter
                  say lena psalter.no
                end
                """);
        assertEquals(List.of("look", "psalter"), script.find("look", "psalter").trigger());
        assertEquals(List.of("look", "*"), script.find("look", "window").trigger());
        assertEquals(List.of("use", "*", "psalter"), script.find("use", "coffee", "psalter").trigger());
        assertNull(script.find("talk", "lefort"));
    }

    @Test
    void ifAndElseBecomeJumps() {
        Script script = parse("""
                on use psalter
                  if not found-page
                    gain page
                    if has bonefolder
                      set clean-cut
                    end
                  else
                    say lena psalter.empty
                  end
                  sound page
                end
                """);
        List<Script.Op> ops = script.find("use", "psalter").ops();
        assertEquals(7, ops.size());
        Script.JumpUnless outer = (Script.JumpUnless) ops.get(0);
        assertEquals(5, outer.target(), "to the else part");
        assertEquals(4, ((Script.JumpUnless) ops.get(2)).target(), "past the inner if");
        assertEquals(6, ((Script.Jump) ops.get(4)).target(), "from the end of the if part past the else part");
    }

    @Test
    void dialogOptionsKeepTheirOrderAndConditions() {
        Script script = parse("""
                option lefort coffee lefort.q.coffee once if has coffee
                  lose coffee
                end
                option lefort count lefort.q.count if asked >= 2
                end
                option lefort bye dialog.bye
                  exit
                end
                """);
        List<Script.Option> options = script.options("lefort");
        assertEquals(List.of("coffee", "count", "bye"), options.stream().map(Script.Option::id).toList());
        assertEquals(true, options.getFirst().once());
        Flags flags = new Flags();
        Inventory inventory = new Inventory();
        assertEquals(false, options.get(1).condition().holds(flags, inventory));
        flags.set("asked", 2);
        assertEquals(true, options.get(1).condition().holds(flags, inventory));
        assertNotNull(options.get(2).block());
    }

    @Test
    void mistakesAreFoundWhenTheScriptIsRead() {
        assertEquals("test.script:2: unknown command sya",
                assertThrows(TextData.DataException.class, () -> parse("on look x\n  sya lena x\nend\n"))
                        .getMessage());
        assertEquals("test.script:1: block without end",
                assertThrows(TextData.DataException.class, () -> parse("on look x\n  say lena x\n")).getMessage());
        assertEquals("test.script:2: else without if",
                assertThrows(TextData.DataException.class, () -> parse("on look x\n else\nend\n")).getMessage());
        assertEquals("test.script:2: say takes 2 words, found 1",
                assertThrows(TextData.DataException.class, () -> parse("on look x\n say lena\nend\n")).getMessage());
        assertEquals("test.script:3: missing end of the block at line 1",
                assertThrows(TextData.DataException.class, () -> parse("on look x\n say lena x\non use x\nend\n"))
                        .getMessage());
    }
}
