package eu.amadeos.gameengine.loop;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameLoopTest {
    @Test
    void updatesRendersAndRunsCommandsOnItsOwnThread() throws Exception {
        AtomicInteger updates = new AtomicInteger();
        AtomicInteger frames = new AtomicInteger();
        AtomicReference<String> commandThread = new AtomicReference<>();
        CountDownLatch enough = new CountDownLatch(10);
        GameLoop loop = new GameLoop(120, 60, dt -> updates.incrementAndGet(), alpha -> {
            frames.incrementAndGet();
            enough.countDown();
        });
        loop.start();
        loop.post(() -> commandThread.set(Thread.currentThread().getName()));
        assertTrue(enough.await(5, TimeUnit.SECONDS));
        loop.stop();
        assertFalse(loop.running());
        assertEquals("game-loop", commandThread.get());
        assertTrue(updates.get() >= frames.get(), "about two updates per frame");
    }

    @Test
    void anExceptionDoesNotStopTheLoop() throws Exception {
        CountDownLatch after = new CountDownLatch(3);
        AtomicInteger calls = new AtomicInteger();
        GameLoop loop = new GameLoop(120, 60, dt -> {
            if (calls.incrementAndGet() == 1) throw new IllegalStateException("test");
        }, alpha -> after.countDown());
        loop.start();
        assertTrue(after.await(5, TimeUnit.SECONDS));
        loop.stop();
    }
}
