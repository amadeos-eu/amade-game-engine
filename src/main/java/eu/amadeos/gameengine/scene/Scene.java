package eu.amadeos.gameengine.scene;

import java.awt.Graphics2D;

/**
 * One screen of a real-time game: a title, the match, a pause screen, the end. A scene advances in fixed
 * steps and draws itself in the game's logical coordinates. Both run on the game loop thread.
 */
// --8<-- [start:scene]
public interface Scene {
    /** Advances by one fixed step of {@code seconds}. */
    void update(double seconds);

    /**
     * Draws the scene.
     *
     * @param alpha how far the time is between the last step and the next, 0 to 1, for smooth movement
     */
    void render(Graphics2D g, double alpha);
}
// --8<-- [end:scene]
