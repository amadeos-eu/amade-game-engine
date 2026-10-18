package eu.amadeos.gameengine.scene;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The scenes of a game as a stack: only the top scene is updated, but all are drawn from the bottom up. A
 * pause screen pushed onto the match therefore freezes it and still shows it underneath. Use it on the game
 * loop thread only.
 */
// --8<-- [start:stack]
public final class SceneStack implements Scene {
    private final List<Scene> scenes = new ArrayList<>();

    /** Puts a scene on top; the one below stops moving. */
    public void push(Scene scene) {
        scenes.add(Objects.requireNonNull(scene, "scene"));
    }

    /** Removes the top scene; the one below moves again. */
    public void pop() {
        if (!scenes.isEmpty()) scenes.removeLast();
    }

    /** Replaces all scenes by one, e.g. from the title to the match. */
    public void replace(Scene scene) {
        scenes.clear();
        push(scene);
    }

    public Scene top() {
        return scenes.isEmpty() ? null : scenes.getLast();
    }

    public int size() {
        return scenes.size();
    }

    @Override
    public void update(double seconds) {
        if (!scenes.isEmpty()) scenes.getLast().update(seconds);
    }

    @Override
    public void render(Graphics2D g, double alpha) {
        // A copy: a scene may push or pop while the frame is drawn.
        for (Scene scene : List.copyOf(scenes)) scene.render(g, alpha);
    }
}
// --8<-- [end:stack]
