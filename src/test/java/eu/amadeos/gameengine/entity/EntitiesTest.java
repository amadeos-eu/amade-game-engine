package eu.amadeos.gameengine.entity;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntitiesTest {
    /** Components as a game would have them: small, mutable, nothing but data. */
    static final class Body {
        double x;
        double y;

        Body(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    static final class Velocity {
        final double dx;
        final double dy;

        Velocity(double dx, double dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }

    static final class Lives {
        int left = 2;
    }

    // --8<-- [start:test-entities]
    @Test
    void aSystemSeesOnlyTheEntitiesWithItsComponents() {
        Entities world = new Entities();
        Entity bee = world.create().add(new Body(10, 10)).add(new Velocity(0, 5));
        Entity rock = world.create().add(new Body(50, 50));     // has a body, but does not move
        world.flush();
        // The movement system: everything with a body and a velocity.
        world.forEach(Body.class, Velocity.class, (entity, body, velocity) -> {
            body.x += velocity.dx;
            body.y += velocity.dy;
        });
        assertEquals(15, bee.get(Body.class).y);
        assertEquals(50, rock.get(Body.class).y);
        // The bee lands: without a velocity it is left alone, like the rock.
        bee.remove(Velocity.class);
        world.forEach(Body.class, Velocity.class, (entity, body, velocity) -> body.y += velocity.dy);
        assertEquals(15, bee.get(Body.class).y);
        assertEquals(2, world.count(Body.class));
    }

    @Test
    void entitiesMadeOrDestroyedDuringAWalkChangeTheListOnlyAtTheFlush() {
        Entities world = new Entities();
        List<Entity> three = new ArrayList<>();
        for (int i = 0; i < 3; i++) three.add(world.create().add(new Lives()));
        world.flush();
        List<Long> visited = new ArrayList<>();
        world.forEach(Lives.class, (entity, lives) -> {
            visited.add(entity.id());
            if (entity.id() == 1) three.get(1).destroy();     // a shot hits the next one
            world.create().add(new Lives());                   // and each one leaves a copy behind
        });
        assertEquals(List.of(1L, 3L), visited, "the destroyed one is skipped, the new ones wait");
        assertEquals(3, world.all().size(), "still in the list until the flush");
        world.flush();
        assertEquals(List.of(1L, 3L, 4L, 5L), world.all().stream().map(Entity::id).toList());
    }
    // --8<-- [end:test-entities]

    @Test
    void componentsAreLookedUpByClass() {
        Entities world = new Entities();
        Entity e = world.create();
        Lives lives = new Lives();
        e.add(lives);
        assertSame(lives, e.get(Lives.class));
        assertTrue(e.has(Lives.class));
        assertNull(e.get(Body.class));
        assertSame(lives, e.remove(Lives.class));
        assertFalse(e.has(Lives.class));
        e.destroy();
        world.flush();
        assertEquals(0, world.all().size(), "destroyed before it was ever flushed in");
        world.create();
        world.clear();
        world.flush();
        assertEquals(0, world.all().size());
    }
}
