package eu.amadeos.gameengine.adventure;

import eu.amadeos.gameengine.data.TextData;
import eu.amadeos.gameengine.math.Vector;
import eu.amadeos.gameengine.tile.Direction;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptRunnerTest {
    /** A stage that writes down what it is asked and lets each step take one update. */
    static final class Recorder implements ScriptRunner.Stage {
        final List<String> log = new ArrayList<>();
        boolean instant;

        BooleanSupplier oneFrame() {
            if (instant) return () -> true;
            boolean[] asked = {false};
            return () -> {
                boolean done = asked[0];
                asked[0] = true;
                return done;
            };
        }

        public BooleanSupplier say(String actor, String text) {
            log.add(actor + ": " + text);
            return oneFrame();
        }

        public BooleanSupplier walk(String actor, Vector to) {
            log.add(actor + " walks to " + (int) to.x() + " " + (int) to.y());
            return oneFrame();
        }

        public BooleanSupplier walkTo(String actor, String place) {
            log.add(actor + " walks to " + place);
            return oneFrame();
        }

        public void face(String actor, Direction direction) {
            log.add(actor + " faces " + direction);
        }

        public BooleanSupplier animate(String actor, String animation) {
            log.add(actor + " " + animation);
            return oneFrame();
        }

        public void show(String hotspot, boolean visible) {
            log.add((visible ? "show " : "hide ") + hotspot);
        }

        public BooleanSupplier enter(String room, String spot) {
            log.add("enter " + room + " at " + spot);
            return oneFrame();
        }

        public BooleanSupplier title(String text) {
            log.add("title " + text);
            return oneFrame();
        }

        public void sound(String name) {
            log.add("sound " + name);
        }
    }

    static final String SCRIPT = """
            on use bonefolder psalter
              if not found-page
                walk lena to psalter
                face lena up
                anim lena reach
                say lena psalter.found
                gain page
                show page
                set found-page
              else
                say lena psalter.empty
              end
            end

            on talk lefort
              say lena lefort.hello
              dialog lefort
              say lefort lefort.goodbye
            end

            option lefort coffee lefort.q.coffee once if has coffee
              say lefort lefort.coffee
              lose coffee
            end
            option lefort chirograph lefort.q.chirograph
              add asked 1
              if asked >= 2
                say lefort lefort.again
                stop
              end
              say lefort lefort.chirograph
            end
            option lefort bye dialog.bye
              exit
            end
            """;

    final Recorder stage = new Recorder();
    final Flags flags = new Flags();
    final Inventory inventory = new Inventory();
    final ScriptRunner runner = new ScriptRunner(
            Script.parse(TextData.parse("test.script", new StringReader(SCRIPT))), stage, flags, inventory);

    void play() {
        for (int i = 0; i < 100 && runner.isBusy() && !runner.isChoosing(); i++) runner.update(0.02);
    }

    // --8<-- [start:test-cutscene]
    @Test
    void aBlockPlaysLikeACutsceneOneStepAfterTheOther() {
        assertTrue(runner.start("use", "bonefolder", "psalter"));
        assertFalse(runner.start("look", "window"), "busy: nothing else starts meanwhile");
        runner.update(0.02);
        assertEquals(List.of("lena walks to psalter"), stage.log, "waits until she is there");
        play();
        assertEquals(List.of("lena walks to psalter", "lena faces UP", "lena reach", "lena: psalter.found",
                "show page"), stage.log);
        assertTrue(inventory.has("page"));
        assertTrue(flags.is("found-page"));
        stage.log.clear();
        runner.start("use", "bonefolder", "psalter");
        play();
        assertEquals(List.of("lena: psalter.empty"), stage.log, "the second time, the else part");
    }
    // --8<-- [end:test-cutscene]

    @Test
    void aDialogOffersItsOptionsUntilOneSaysExit() {
        runner.start("talk", "lefort");
        play();
        assertTrue(runner.isChoosing());
        assertEquals(List.of("chirograph", "bye"), ids(), "no coffee, no coffee question");
        runner.choose("chirograph");
        play();
        assertEquals(List.of("chirograph", "bye"), ids(), "back to the options");
        runner.choose("chirograph");
        play();
        assertEquals(List.of("lena: lefort.hello", "lefort: lefort.chirograph", "lefort: lefort.again"), stage.log);
        runner.choose("bye");
        play();
        assertFalse(runner.isBusy());
        assertEquals("lefort: lefort.goodbye", stage.log.getLast(), "the block that opened the dialog goes on");
    }

    @Test
    void aOnceOptionIsGoneAfterItWasChosenAlsoInASavedGame() {
        inventory.gain("coffee");
        runner.start("talk", "lefort");
        play();
        assertEquals(List.of("coffee", "chirograph", "bye"), ids());
        runner.choose("coffee");
        play();
        assertEquals(List.of("chirograph", "bye"), ids());
        assertTrue(flags.is("chosen.lefort.coffee"), "remembered as a flag");
        assertThrows(IllegalArgumentException.class, () -> runner.choose("coffee"));
    }

    @Test
    void stepsThatAreOverAtOnceDoNotCostAFrame() {
        stage.instant = true;
        runner.start("use", "bonefolder", "psalter");
        runner.update(0.02);
        assertFalse(runner.isBusy());
        assertEquals(5, stage.log.size());
    }

    @Test
    void anUnknownTriggerStartsNothing() {
        assertFalse(runner.start("look", "ceiling"));
        assertFalse(runner.isBusy());
    }

    List<String> ids() {
        return runner.choices().stream().map(ScriptRunner.Choice::id).toList();
    }
}
