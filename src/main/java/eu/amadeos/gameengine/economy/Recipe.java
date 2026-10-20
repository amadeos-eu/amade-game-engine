package eu.amadeos.gameengine.economy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * What a workshop needs and what it makes of it: a mill makes one sack of flour from one of grain in eight
 * seconds, a residence one settler from food and cloth. An input can be one of several goods, as food can
 * be bread or fish; the recipe then takes the first of them that is there.
 *
 * <pre>
 * Recipe&lt;Good&gt; mill = new Recipe&lt;&gt;(List.of(Set.of(Good.GRAIN)), Good.FLOUR, 8);
 * if (mill.ready(stock)) { mill.take(stock); worker.startWork(mill.seconds()); }
 * </pre>
 *
 * <p>The goods of each alternative are tried in the order of their enum, so the same stock always gives
 * the same choice.
 *
 * @param inputs  one set of alternatives per good needed; an empty list for a recipe from nothing, as a
 *                woodcutter's, whose input is a tree outside
 * @param makes   the good made, or {@code null} when the result is not a good, as a settler is not
 * @param seconds how long the work takes
 * @param <G>     the kinds of goods, an enum
 */
// --8<-- [start:recipe]
public record Recipe<G extends Enum<G>>(List<Set<G>> inputs, G makes, double seconds) {
    public Recipe {
        inputs = List.copyOf(inputs);
        for (Set<G> input : inputs) {
            if (input.isEmpty()) throw new IllegalArgumentException("an input needs one good at least");
        }
        if (seconds <= 0) throw new IllegalArgumentException("seconds " + seconds);
    }

    /** True if {@code stock} has a good for every input. */
    public boolean ready(Stock<G> stock) {
        return choose(stock) != null;
    }

    /** Takes one good for every input from {@code stock}; the goods taken, or {@code null} and nothing taken. */
    public List<G> take(Stock<G> stock) {
        List<G> chosen = choose(stock);
        if (chosen == null) return null;
        for (G good : chosen) stock.take(good, 1);
        return chosen;
    }

    private List<G> choose(Stock<G> stock) {
        List<G> chosen = new ArrayList<>(inputs.size());
        for (Set<G> input : inputs) {
            G found = null;
            for (G good : input.stream().sorted().toList()) {
                long already = chosen.stream().filter(good::equals).count();
                if (stock.count(good) > already) {
                    found = good;
                    break;
                }
            }
            if (found == null) return null;
            chosen.add(found);
        }
        return chosen;
    }
    // --8<-- [end:recipe]

    /** Every good any input can take, for the carriers that should bring them. */
    public Set<G> needs() {
        java.util.Set<G> all = new java.util.TreeSet<>();
        for (Set<G> input : inputs) all.addAll(input);
        return all;
    }
}
