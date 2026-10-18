package eu.amadeos.gameengine.scene;

import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SceneStackTest {
    private final List<String> log = new ArrayList<>();

    private Scene scene(String name) {
        return new Scene() {
            @Override
            public void update(double seconds) {
                log.add("update " + name);
            }

            @Override
            public void render(Graphics2D g, double alpha) {
                log.add("render " + name);
            }
        };
    }

    @Test
    void onlyTheTopMovesButAllAreDrawn() {
        SceneStack stack = new SceneStack();
        stack.push(scene("match"));
        stack.push(scene("pause"));
        stack.update(0.01);
        stack.render(null, 0);
        assertEquals(List.of("update pause", "render match", "render pause"), log);
        log.clear();
        stack.pop();
        stack.update(0.01);
        assertEquals(List.of("update match"), log);
        stack.replace(scene("title"));
        assertEquals(1, stack.size());
    }
}
