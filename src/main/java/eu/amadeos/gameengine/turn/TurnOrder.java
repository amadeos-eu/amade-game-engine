package eu.amadeos.gameengine.turn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Whose turn it is in a game where players take turns: an artillery duel, a board game, a card game. The
 * players go round in a fixed order; those out of the game (a destroyed tank, a player who has folded)
 * are skipped. The round is decided when one player or none is left.
 *
 * <p>The order knows nothing about the players; whether one is still in play, it asks the game each time.
 * So a tank destroyed by somebody else's shot drops out without anyone telling the order.
 *
 * <pre>
 * TurnOrder&lt;Tank&gt; turns = new TurnOrder&lt;&gt;(tanks, Tank::alive, 0);
 * // after a shot has landed and everything has settled:
 * if (turns.decided()) endRound(turns.winner()); else turns.next();
 * </pre>
 *
 * @param <P> the players
 */
// --8<-- [start:turns]
public final class TurnOrder<P> {
    private final List<P> players;
    private final Predicate<P> inPlay;
    private int current;
    private int turns;

    /** The players in the order they take turns; {@code first} is the index of the one who begins. */
    public TurnOrder(List<P> players, Predicate<P> inPlay, int first) {
        if (players.isEmpty()) throw new IllegalArgumentException("nobody to take turns");
        if (first < 0 || first >= players.size()) throw new IllegalArgumentException("no player " + first);
        this.players = List.copyOf(players);
        this.inPlay = inPlay;
        this.current = first;
        if (!inPlay.test(this.players.get(first))) advance();
    }

    /** Whose turn it is. */
    public P current() {
        return players.get(current);
    }

    /** Passes the turn to the next player still in play, and returns them; the same one if nobody else is left. */
    public P next() {
        advance();
        turns++;
        return current();
    }

    private void advance() {
        for (int i = 1; i <= players.size(); i++) {
            int candidate = (current + i) % players.size();
            if (inPlay.test(players.get(candidate))) {
                current = candidate;
                return;
            }
        }
    }

    /** The players still in play, in their order. */
    public List<P> inPlay() {
        List<P> left = new ArrayList<>();
        for (P p : players) if (inPlay.test(p)) left.add(p);
        return left;
    }

    /** One player or none is left: the round is over. */
    public boolean decided() {
        return inPlay().size() <= 1;
    }

    /** The last player in play once {@link #decided()}, or {@code null} if nobody is left (or it is not decided). */
    public P winner() {
        List<P> left = inPlay();
        return left.size() == 1 ? left.getFirst() : null;
    }
    // --8<-- [end:turns]

    /** How many times the turn has passed. */
    public int turns() {
        return turns;
    }

    public List<P> players() {
        return players;
    }
}
