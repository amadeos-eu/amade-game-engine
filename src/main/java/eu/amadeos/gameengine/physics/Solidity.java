package eu.amadeos.gameengine.physics;

/** What a tile is to a body that moves: nothing, a wall, or a platform to stand on. */
public enum Solidity {
    /** Air: the body passes. */
    EMPTY,
    /** Ground and walls: nothing passes, from any side. */
    SOLID,
    /**
     * A one-way platform: it holds a body that comes down onto it from above, and lets it pass in every
     * other direction, so it can be jumped through from below. A body can also drop through on purpose.
     */
    PLATFORM
}
