package eu.amadeos.gameengine.event;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventQueueTest {
    sealed interface Event {
        record Explode(String bomb) implements Event {
        }

        record Hit(String player) implements Event {
        }
    }

    // --8<-- [start:test-queue]
    @Test
    void aChainReactionRunsInWaves() {
        // Bomb a reaches b and c; b reaches d and a player.
        EventQueue<Event> queue = new EventQueue<>();
        List<String> order = new ArrayList<>();
        queue.post(new Event.Explode("a"));
        queue.process(event -> {
            switch (event) {
                case Event.Explode(String bomb) -> {
                    order.add(bomb);
                    if (bomb.equals("a")) {
                        queue.post(new Event.Explode("b"));
                        queue.post(new Event.Explode("c"));
                    }
                    if (bomb.equals("b")) {
                        queue.post(new Event.Explode("d"));
                        queue.post(new Event.Hit("white"));
                    }
                }
                case Event.Hit(String player) -> order.add(player + " hit");
            }
        });
        assertEquals(List.of("a", "b", "c", "d", "white hit"), order, "c before d: first the whole first wave");
        assertEquals(5, queue.happened().size());
        assertEquals(new Event.Explode("a"), queue.happened().getFirst());
    }
    // --8<-- [end:test-queue]

    @Test
    void aChainWithoutEndIsStopped() {
        EventQueue<Event> queue = new EventQueue<>(100);
        queue.post(new Event.Explode("a"));
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> queue.process(event -> queue.post(event)));
        assertTrue(e.getMessage().contains("Explode[bomb=a]"), e.getMessage());
        assertEquals(0, queue.waiting(), "the rest is dropped");
    }

    @Test
    void clearForgetsTheLastStep() {
        EventQueue<Event> queue = new EventQueue<>();
        queue.post(new Event.Hit("red"));
        queue.process(event -> { });
        queue.post(new Event.Hit("blue"));
        queue.clear();
        assertEquals(List.of(), queue.happened());
        assertEquals(0, queue.waiting());
    }
}
