package eu.amadeos.gameengine.adventure;

import java.util.List;

/**
 * What a click means, in the two-button control of later adventures: no list of verbs to pick from, the
 * thing itself decides. The right button <b>looks</b> at it, item in hand or not. The left button <b>talks</b> to a person,
 * <b>leaves</b> through an exit and <b>uses</b> everything else; with an item from the inventory in hand,
 * it uses the item <i>on</i> the thing. The answer is the trigger for {@link ScriptRunner#start}.
 *
 * <pre>
 * List&lt;String&gt; trigger = Interaction.trigger(button, hotspot, held, people::contains);
 * if (!runner.start(trigger.toArray(String[]::new))) say(defaultAnswer(trigger));
 * </pre>
 */
public final class Interaction {
    /** The left mouse button, as {@link java.awt.event.MouseEvent#BUTTON1}. */
    public static final int LEFT = 1;
    /** The right mouse button, as {@link java.awt.event.MouseEvent#BUTTON3}. */
    public static final int RIGHT = 3;

    /** Whether a hotspot is a person one talks to. */
    @FunctionalInterface
    public interface People {
        boolean isPerson(String hotspot);
    }

    private Interaction() {
    }

    // --8<-- [start:trigger]
    /**
     * The trigger for a click with {@code button} on {@code hotspot}, while the player holds the item
     * {@code held} (null for none): {@code look psalter}, {@code use bonefolder psalter},
     * {@code talk lefort}, {@code exit door} or {@code use psalter}.
     */
    public static List<String> trigger(int button, Hotspot hotspot, String held, People people) {
        if (button == RIGHT) return List.of("look", hotspot.id());
        if (held != null) return List.of("use", held, hotspot.id());
        if (people.isPerson(hotspot.id())) return List.of("talk", hotspot.id());
        if (hotspot.isExit()) return List.of("exit", hotspot.id());
        return List.of("use", hotspot.id());
    }
    // --8<-- [end:trigger]
}
