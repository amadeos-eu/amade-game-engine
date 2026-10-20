package eu.amadeos.gameengine.save;

import eu.amadeos.gameengine.random.SeededRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotTest {
    @TempDir
    Path dir;

    enum Order { MOVE, HARVEST }

    // --8<-- [start:test-snapshot]
    @Test
    void listsOfThingsGoIntoASaveFileAndComeBackExactly() {
        Snapshot.Writer w = Snapshot.writer();
        w.put("credits", 1250).put("time", 0.1 + 0.2);
        Snapshot.Writer tank = w.add("unit");
        tank.put("kind", "tank").put("x", Math.PI * 100).put("order", Order.MOVE);
        Snapshot.Writer harvester = w.add("unit");
        harvester.put("kind", "harvester").put("x", 1e-300).put("order", Order.HARVEST).put("full", true);
        w.child("camera").put("x", 96.5);
        w.list("shot");
        assertEquals("2", w.values().get("unit.count"));
        assertEquals("tank", w.values().get("unit.0.kind"));

        SaveFile file = new SaveFile(dir.resolve("game.save"));
        file.store(1, w.values());
        Snapshot.Reader r = Snapshot.reader(file.load().orElseThrow().values());
        assertEquals(1250, r.integer("credits"));
        assertEquals(0.1 + 0.2, r.number("time"), "to the last bit, not 0.3");
        List<Snapshot.Reader> units = r.list("unit");
        assertEquals(2, units.size());
        assertEquals(Math.PI * 100, units.get(0).number("x"));
        assertEquals(Order.HARVEST, units.get(1).choice("order", Order.class));
        assertTrue(units.get(1).bool("full"));
        assertEquals(1e-300, units.get(1).number("x"));
        assertEquals(96.5, r.child("camera").number("x"));
        assertEquals(List.of(), r.list("shot"), "an empty list is a list");
    }
    // --8<-- [end:test-snapshot]

    @Test
    void aMissingOrWrongKeyIsNamed() {
        Snapshot.Reader r = Snapshot.reader(Map.of("unit.count", "1", "unit.0.x", "far"));
        Snapshot.Reader unit = r.list("unit").getFirst();
        Snapshot.SnapshotException wrong = assertThrows(Snapshot.SnapshotException.class, () -> unit.number("x"));
        assertEquals("unit.0.x is not a number: far", wrong.getMessage());
        Snapshot.SnapshotException missing = assertThrows(Snapshot.SnapshotException.class, () -> unit.integer("health"));
        assertEquals("missing unit.0.health", missing.getMessage());
        assertEquals("tank", unit.text("kind", "tank"));
    }

    // --8<-- [start:test-random]
    @Test
    void aRandomGeneratorGoesOnWhereItStood() {
        SeededRandom random = new SeededRandom(42);
        for (int i = 0; i < 1000; i++) random.nextDouble();
        SeededRandom resumed = SeededRandom.resume(random.seed(), random.state());
        for (int i = 0; i < 100; i++) assertEquals(random.nextLong(), resumed.nextLong());
        assertEquals(42, resumed.seed());
    }
    // --8<-- [end:test-random]
}
