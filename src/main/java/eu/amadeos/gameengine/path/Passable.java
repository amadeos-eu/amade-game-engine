package eu.amadeos.gameengine.path;

/** Whether a tile can be entered, e.g. not a wall; for a ghost flying home, also the door of its house. */
@FunctionalInterface
public interface Passable {
    boolean test(int col, int row);
}
