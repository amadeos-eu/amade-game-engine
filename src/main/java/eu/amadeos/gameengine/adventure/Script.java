package eu.amadeos.gameengine.adventure;

import eu.amadeos.gameengine.data.TextData;
import eu.amadeos.gameengine.tile.Direction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * What happens in an adventure, written as text: a list of <b>blocks</b>, each started by what the player
 * does and ended by {@code end}. The game never says in Java what Lena answers when one looks at the psalter;
 * the script does, so the story can be written, read and changed like a play.
 *
 * <pre>
 * on look psalter                  # right click on the psalter
 *   say lena psalter.look          # Lena says the text psalter.look of the game's texts
 * end
 *
 * on use bonefolder psalter        # the bone folder from the inventory, used on the psalter
 *   if not found-page
 *     anim lena reach
 *     say lena psalter.found
 *     gain page
 *     set found-page
 *   else
 *     say lena psalter.empty
 *   end
 * end
 *
 * on talk lefort
 *   dialog lefort                  # shows the options of the dialog lefort until one says exit
 * end
 *
 * option lefort coffee lefort.q.coffee once if has coffee
 *   say lena lefort.coffee.1
 *   say lefort lefort.coffee.2
 *   lose coffee
 * end
 *
 * option lefort bye dialog.bye
 *   exit
 * end
 * </pre>
 *
 * <p>Triggers are {@code on look <thing>}, {@code on use <thing>}, {@code on use <item> <thing>},
 * {@code on talk <someone>}, {@code on enter <room>} or whatever else the game starts; {@code *} stands for
 * any word, so {@code on look *} answers every look the script has no own block for.
 *
 * <p>Commands, one per line: {@code say} actor text, {@code walk} actor x y, {@code walk} actor {@code to}
 * place, {@code face} actor direction, {@code anim} actor animation, {@code gain} item, {@code lose} item,
 * {@code set} flag [value], {@code add} flag amount, {@code clear} flag, {@code show} hotspot,
 * {@code hide} hotspot, {@code goto} room spot, {@code title} text, {@code sound} name, {@code wait}
 * seconds, {@code dialog} name, {@code exit}, {@code stop}, and {@code if} condition … [{@code else} …]
 * {@code end}. A condition is a flag, {@code has} item, a flag compared with a number
 * ({@code asked >= 2}), each optionally after {@code not}.
 *
 * <p>Texts are keys, never sentences: the sentences are in the game's text files, in every language.
 * Errors in a script name the file and the line, when it is read, not when a player finally gets there.
 */
public final class Script {
    /** A condition of {@code if} and of dialog options. */
    public sealed interface Condition {
        boolean holds(Flags flags, Inventory inventory);
    }

    record FlagSet(String flag) implements Condition {
        public boolean holds(Flags flags, Inventory inventory) {
            return flags.is(flag);
        }
    }

    record Carries(String item) implements Condition {
        public boolean holds(Flags flags, Inventory inventory) {
            return inventory.has(item);
        }
    }

    record Compare(String flag, String op, int value) implements Condition {
        public boolean holds(Flags flags, Inventory inventory) {
            int v = flags.get(flag);
            return switch (op) {
                case "==" -> v == value;
                case "!=" -> v != value;
                case "<" -> v < value;
                case "<=" -> v <= value;
                case ">" -> v > value;
                default -> v >= value;
            };
        }
    }

    record Not(Condition condition) implements Condition {
        public boolean holds(Flags flags, Inventory inventory) {
            return !condition.holds(flags, inventory);
        }
    }

    /** Always true: an option without {@code if}. */
    record Always() implements Condition {
        public boolean holds(Flags flags, Inventory inventory) {
            return true;
        }
    }

    /** One step of a block. {@code line} says where it was written, for errors while it runs. */
    public sealed interface Op {
        TextData.Line line();
    }

    record Say(TextData.Line line, String actor, String text) implements Op {
    }

    record WalkTo(TextData.Line line, String actor, String place) implements Op {
    }

    record WalkPoint(TextData.Line line, String actor, double x, double y) implements Op {
    }

    record Face(TextData.Line line, String actor, Direction direction) implements Op {
    }

    record Anim(TextData.Line line, String actor, String animation) implements Op {
    }

    record Gain(TextData.Line line, String item) implements Op {
    }

    record Lose(TextData.Line line, String item) implements Op {
    }

    record SetFlag(TextData.Line line, String flag, int value) implements Op {
    }

    record AddFlag(TextData.Line line, String flag, int amount) implements Op {
    }

    record Show(TextData.Line line, String hotspot, boolean visible) implements Op {
    }

    record Goto(TextData.Line line, String room, String spot) implements Op {
    }

    record Title(TextData.Line line, String text) implements Op {
    }

    record Sound(TextData.Line line, String name) implements Op {
    }

    record Wait(TextData.Line line, double seconds) implements Op {
    }

    record OpenDialog(TextData.Line line, String dialog) implements Op {
    }

    record ExitDialog(TextData.Line line) implements Op {
    }

    record Stop(TextData.Line line) implements Op {
    }

    /** Goes on at {@code target} unless the condition holds: the {@code if}. */
    record JumpUnless(TextData.Line line, Condition condition, int target) implements Op {
    }

    /** Goes on at {@code target}: the end of the {@code if} part, where {@code else} begins. */
    record Jump(TextData.Line line, int target) implements Op {
    }

    /** A block: what it is started by (or the option it belongs to), and its steps. */
    public record Block(List<String> trigger, List<Op> ops) {
    }

    /** One answer the player can pick in a dialog. */
    public record Option(String dialog, String id, String text, boolean once, Condition condition, Block block) {
    }

    private final List<Block> blocks;
    private final Map<String, List<Option>> dialogs;

    private Script(List<Block> blocks, Map<String, List<Option>> dialogs) {
        this.blocks = List.copyOf(blocks);
        this.dialogs = dialogs;
    }

    /** The block started by the trigger: one written for exactly these words, else the first matching with *. */
    public Block find(String... trigger) {
        List<String> words = List.of(trigger);
        for (Block block : blocks) if (block.trigger().equals(words)) return block;
        for (Block block : blocks) if (matches(block.trigger(), words)) return block;
        return null;
    }

    private static boolean matches(List<String> pattern, List<String> words) {
        if (pattern.size() != words.size()) return false;
        for (int i = 0; i < words.size(); i++)
            if (!pattern.get(i).equals("*") && !pattern.get(i).equals(words.get(i))) return false;
        return true;
    }

    /** The options of a dialog, in the order written; an error if there is no such dialog. */
    public List<Option> options(String dialog) {
        List<Option> options = dialogs.get(dialog);
        if (options == null) throw new IllegalArgumentException("no dialog " + dialog);
        return options;
    }

    public List<Block> blocks() {
        return blocks;
    }

    /** Reads a script from among the classes of {@code owner}, e.g. {@code "/rooms/workshop.script"}. */
    public static Script load(Class<?> owner, String resource) {
        return parse(TextData.load(owner, resource));
    }

    // --8<-- [start:parse]
    /** Reads the blocks and options of a script; errors name the file and the line. */
    public static Script parse(List<TextData.Line> lines) {
        List<Block> blocks = new ArrayList<>();
        Map<String, List<Option>> dialogs = new LinkedHashMap<>();
        int i = 0;
        while (i < lines.size()) {
            TextData.Line head = lines.get(i);
            List<Op> ops = new ArrayList<>();
            int next = body(lines, i + 1, ops, head);
            switch (head.keyword()) {
                case "on" -> {
                    if (head.size() < 2) throw head.error("on what?");
                    blocks.add(new Block(head.words().subList(1, head.size()), ops));
                }
                case "option" -> {
                    Option option = option(head, ops);
                    dialogs.computeIfAbsent(option.dialog(), d -> new ArrayList<>()).add(option);
                }
                default -> throw head.error("expected on or option, found " + head.keyword());
            }
            i = next;
        }
        dialogs.replaceAll((d, options) -> List.copyOf(options));
        return new Script(blocks, dialogs);
    }
    // --8<-- [end:parse]

    /**
     * Reads the steps of a block up to its {@code end} into {@code ops}, with the jumps of {@code if} and
     * {@code else}; returns the index of the line after the {@code end}.
     */
    private static int body(List<TextData.Line> lines, int from, List<Op> ops, TextData.Line head) {
        // Open ifs: the index of their JumpUnless, and of the Jump at their else (-1 before one).
        List<int[]> open = new ArrayList<>();
        for (int i = from; i < lines.size(); i++) {
            TextData.Line line = lines.get(i);
            switch (line.keyword()) {
                case "end" -> {
                    if (open.isEmpty()) return i + 1;
                    int[] branch = open.removeLast();
                    if (branch[1] < 0) {
                        JumpUnless test = (JumpUnless) ops.get(branch[0]);
                        ops.set(branch[0], new JumpUnless(test.line(), test.condition(), ops.size()));
                    } else {
                        Jump jump = (Jump) ops.get(branch[1]);
                        ops.set(branch[1], new Jump(jump.line(), ops.size()));
                    }
                }
                case "else" -> {
                    if (open.isEmpty() || open.getLast()[1] >= 0) throw line.error("else without if");
                    int[] branch = open.getLast();
                    branch[1] = ops.size();
                    ops.add(new Jump(line, -1));
                    JumpUnless test = (JumpUnless) ops.get(branch[0]);
                    ops.set(branch[0], new JumpUnless(test.line(), test.condition(), ops.size()));
                }
                case "if" -> {
                    open.add(new int[]{ops.size(), -1});
                    ops.add(new JumpUnless(line, condition(line, line.words().subList(1, line.size())), -1));
                }
                case "on", "option" -> throw line.error("missing end of the block at line " + head.lineNumber());
                default -> ops.add(op(line));
            }
        }
        throw head.error("block without end");
    }

    private static Op op(TextData.Line line) {
        return switch (line.keyword()) {
            case "say" -> {
                count(line, 3);
                yield new Say(line, line.word(1), line.word(2));
            }
            case "walk" -> {
                count(line, 4);
                yield line.word(2).equals("to") ? new WalkTo(line, line.word(1), line.word(3))
                        : new WalkPoint(line, line.word(1), line.number(2), line.number(3));
            }
            case "face" -> {
                count(line, 3);
                yield new Face(line, line.word(1), direction(line, 2));
            }
            case "anim" -> {
                count(line, 3);
                yield new Anim(line, line.word(1), line.word(2));
            }
            case "gain" -> new Gain(line, one(line));
            case "lose" -> new Lose(line, one(line));
            case "set" -> {
                if (line.size() != 2 && line.size() != 3) throw line.error("set flag [value]");
                yield new SetFlag(line, line.word(1), line.size() == 3 ? line.integer(2) : 1);
            }
            case "clear" -> new SetFlag(line, one(line), 0);
            case "add" -> {
                count(line, 3);
                yield new AddFlag(line, line.word(1), line.integer(2));
            }
            case "show" -> new Show(line, one(line), true);
            case "hide" -> new Show(line, one(line), false);
            case "goto" -> {
                count(line, 3);
                yield new Goto(line, line.word(1), line.word(2));
            }
            case "title" -> new Title(line, one(line));
            case "sound" -> new Sound(line, one(line));
            case "wait" -> {
                count(line, 2);
                double seconds = line.number(1);
                if (seconds < 0) throw line.error("cannot wait less than nothing");
                yield new Wait(line, seconds);
            }
            case "dialog" -> new OpenDialog(line, one(line));
            case "exit" -> {
                count(line, 1);
                yield new ExitDialog(line);
            }
            case "stop" -> {
                count(line, 1);
                yield new Stop(line);
            }
            default -> throw line.error("unknown command " + line.keyword());
        };
    }

    /** {@code option <dialog> <id> <text> [once] [if <condition>]}, and its block. */
    private static Option option(TextData.Line head, List<Op> ops) {
        if (head.size() < 4) throw head.error("option dialog id text [once] [if condition]");
        boolean once = false;
        Condition condition = new Always();
        int i = 4;
        if (i < head.size() && head.word(i).equals("once")) {
            once = true;
            i++;
        }
        if (i < head.size()) {
            if (!head.word(i).equals("if")) throw head.error("expected once or if, found " + head.word(i));
            condition = condition(head, head.words().subList(i + 1, head.size()));
        }
        return new Option(head.word(1), head.word(2), head.word(3), once, condition,
                new Block(List.of("option", head.word(1), head.word(2)), ops));
    }

    static Condition condition(TextData.Line line, List<String> words) {
        if (words.isEmpty()) throw line.error("if what?");
        if (words.getFirst().equals("not")) return new Not(condition(line, words.subList(1, words.size())));
        if (words.getFirst().equals("has")) {
            if (words.size() != 2) throw line.error("has needs one item");
            return new Carries(words.get(1));
        }
        if (words.size() == 1) return new FlagSet(words.getFirst());
        if (words.size() == 3 && List.of("==", "!=", "<", "<=", ">", ">=").contains(words.get(1))) {
            try {
                return new Compare(words.get(0), words.get(1), Integer.parseInt(words.get(2)));
            } catch (NumberFormatException e) {
                throw line.error('"' + words.get(2) + "\" is not a whole number");
            }
        }
        throw line.error("cannot read the condition " + String.join(" ", words));
    }

    private static void count(TextData.Line line, int words) {
        if (line.size() != words)
            throw line.error(line.keyword() + " takes " + (words - 1) + " words, found " + (line.size() - 1));
    }

    private static String one(TextData.Line line) {
        count(line, 2);
        return line.word(1);
    }

    private static Direction direction(TextData.Line line, int index) {
        String word = line.word(index);
        try {
            return Direction.valueOf(word.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw line.error('"' + word + "\" is no direction (up, down, left, right)");
        }
    }

    @Override
    public String toString() {
        return "Script[" + blocks.size() + " blocks, dialogs " + Objects.toString(dialogs.keySet()) + "]";
    }
}
