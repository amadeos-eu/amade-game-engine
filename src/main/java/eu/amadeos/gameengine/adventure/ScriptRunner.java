package eu.amadeos.gameengine.adventure;

import eu.amadeos.gameengine.math.Vector;
import eu.amadeos.gameengine.tile.Direction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Plays the blocks of a {@link Script}, one step after the other, in the game loop. Most steps take time:
 * Lena speaks until the player has read the line, walks until she is there, a title stays until it fades.
 * The runner hands such a step to the {@link Stage}, the game's side, and waits until the stage says it is
 * done; only then does it go on. A block is thus a <b>cutscene</b>, written as a list, played as a story.
 *
 * <p>While a block plays, the player only watches: {@link #isBusy()} is true, clicks go to skipping text.
 * A {@code dialog} step shows the dialog's options ({@link #choices()}) and waits for {@link #choose}; the
 * chosen option's block plays, then the options are offered again, until a block says {@code exit}.
 * An option marked {@code once} is gone after it was chosen; the runner remembers that in the
 * {@link Flags}, so a saved game remembers it too.
 *
 * <pre>
 * // in the scene's update, every step:
 * runner.update(seconds);
 * if (!runner.isBusy() &amp;&amp; mouse.wasPressed(1)) {
 *     Hotspot h = room.hotspotAt(pointer);
 *     if (h != null) lena.walkTo(room.floor(), h.stand(), () -&gt; runner.start("use", h.id()));
 * }
 * </pre>
 */
public final class ScriptRunner {
    // --8<-- [start:stage]
    /**
     * The game's side of a script: it knows the actors, the pictures and the texts. Each step that takes
     * time returns a test the runner asks every frame, {@code true} when the step is over.
     */
    public interface Stage {
        /** Shows the text over the actor; done when it was read or clicked away. */
        BooleanSupplier say(String actor, String text);

        /** Walks the actor to the point, over the floor of the room. */
        BooleanSupplier walk(String actor, Vector to);

        /** Walks the actor to where one stands to use a hotspot, or to a spot of the room. */
        BooleanSupplier walkTo(String actor, String place);

        void face(String actor, Direction direction);

        /** Plays an animation of the actor, such as reaching or picking up. */
        BooleanSupplier animate(String actor, String animation);

        /** Shows a hotspot or hides it. */
        void show(String hotspot, boolean visible);

        /** Leaves for another room, arriving at its spot; done when the new room is there. */
        BooleanSupplier enter(String room, String spot);

        /** A text across the screen, such as a place and a date. */
        BooleanSupplier title(String text);

        void sound(String name);
    }
    // --8<-- [end:stage]

    /** An option the player can choose now: its id and the key of its text. */
    public record Choice(String id, String text) {
    }

    private record Frame(Script.Block block, int[] next) {
        Frame(Script.Block block) {
            this(block, new int[]{0});
        }
    }

    /** An open dialog, and how many frames were on the stack below its options. */
    private record OpenDialog(String id, int depth) {
    }

    private final Script script;
    private final Stage stage;
    private final Flags flags;
    private final Inventory inventory;
    private final Deque<Frame> frames = new ArrayDeque<>();
    private final Deque<OpenDialog> dialogs = new ArrayDeque<>();
    private BooleanSupplier waiting;
    private double waitSeconds;

    public ScriptRunner(Script script, Stage stage, Flags flags, Inventory inventory) {
        this.script = Objects.requireNonNull(script, "script");
        this.stage = Objects.requireNonNull(stage, "stage");
        this.flags = Objects.requireNonNull(flags, "flags");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    /**
     * Starts the block the trigger names, such as {@code start("use", "bonefolder", "psalter")}; it plays
     * from the next {@link #update}. Returns false, and starts nothing, if the script has no block for it or
     * a block is playing already, so the game can answer with its own default ("That does not work.").
     */
    public boolean start(String... trigger) {
        if (isBusy()) return false;
        Script.Block block = script.find(trigger);
        if (block == null) return false;
        frames.push(new Frame(block));
        return true;
    }

    /** Whether a block plays or a dialog waits for a choice: the player cannot walk about meanwhile. */
    public boolean isBusy() {
        return !frames.isEmpty() || !dialogs.isEmpty();
    }

    /** Whether a dialog waits for the player to choose one of the {@link #choices()}. */
    public boolean isChoosing() {
        return !dialogs.isEmpty() && waiting == null && frames.size() == dialogs.peek().depth();
    }

    /** The options of the open dialog the player can choose now, in the order written. */
    public List<Choice> choices() {
        if (!isChoosing()) return List.of();
        List<Choice> choices = new ArrayList<>();
        for (Script.Option option : script.options(dialogs.peek().id()))
            if (available(option)) choices.add(new Choice(option.id(), option.text()));
        return choices;
    }

    /** The player picks an option; its block plays from the next {@link #update}. */
    public void choose(String id) {
        if (!isChoosing()) throw new IllegalStateException("no dialog waits for a choice");
        for (Script.Option option : script.options(dialogs.peek().id())) {
            if (option.id().equals(id) && available(option)) {
                if (option.once()) flags.set(usedFlag(option));
                frames.push(new Frame(option.block()));
                return;
            }
        }
        throw new IllegalArgumentException("no option " + id + " to choose now");
    }

    private boolean available(Script.Option option) {
        return !(option.once() && flags.is(usedFlag(option))) && option.condition().holds(flags, inventory);
    }

    private static String usedFlag(Script.Option option) {
        return "chosen." + option.dialog() + "." + option.id();
    }

    // --8<-- [start:update]
    /** Plays on: waits for the step in progress, then runs steps until one takes time or all are done. */
    public void update(double seconds) {
        if (waitSeconds > 0) {
            waitSeconds -= seconds;
            if (waitSeconds > 0) return;
            waitSeconds = 0;
        }
        if (waiting != null) {
            if (!waiting.getAsBoolean()) return;
            waiting = null;
        }
        while (!frames.isEmpty() && waiting == null && waitSeconds == 0) {
            if (!dialogs.isEmpty() && frames.size() == dialogs.peek().depth()) return;   // choosing
            Frame frame = frames.peek();
            int pc = frame.next()[0];
            if (pc >= frame.block().ops().size()) {
                frames.pop();
                continue;
            }
            frame.next()[0] = pc + 1;
            run(frame, frame.block().ops().get(pc));
        }
    }
    // --8<-- [end:update]

    private void run(Frame frame, Script.Op op) {
        switch (op) {
            case Script.Say say -> waiting = stage.say(say.actor(), say.text());
            case Script.WalkTo walk -> waiting = stage.walkTo(walk.actor(), walk.place());
            case Script.WalkPoint walk -> waiting = stage.walk(walk.actor(), new Vector(walk.x(), walk.y()));
            case Script.Face face -> stage.face(face.actor(), face.direction());
            case Script.Anim anim -> waiting = stage.animate(anim.actor(), anim.animation());
            case Script.Gain gain -> inventory.gain(gain.item());
            case Script.Lose lose -> inventory.lose(lose.item());
            case Script.SetFlag set -> flags.set(set.flag(), set.value());
            case Script.AddFlag add -> flags.add(add.flag(), add.amount());
            case Script.Show show -> stage.show(show.hotspot(), show.visible());
            case Script.Goto go -> waiting = stage.enter(go.room(), go.spot());
            case Script.Title title -> waiting = stage.title(title.text());
            case Script.Sound sound -> stage.sound(sound.name());
            case Script.Wait wait -> waitSeconds = wait.seconds();
            case Script.OpenDialog open -> {
                script.options(open.dialog());          // fails here, at the line, for a dialog not written
                dialogs.push(new OpenDialog(open.dialog(), frames.size()));
            }
            case Script.ExitDialog exit -> {
                if (dialogs.isEmpty()) throw exit.line().error("exit outside a dialog");
                OpenDialog dialog = dialogs.pop();
                while (frames.size() > dialog.depth()) frames.pop();
            }
            case Script.Stop stop -> frames.pop();
            case Script.Jump jump -> frame.next()[0] = jump.target();
            case Script.JumpUnless test -> {
                if (!test.condition().holds(flags, inventory)) frame.next()[0] = test.target();
            }
        }
        if (waiting != null && waiting.getAsBoolean()) waiting = null;     // a step that was over at once
    }
}
