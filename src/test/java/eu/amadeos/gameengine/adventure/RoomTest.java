package eu.amadeos.gameengine.adventure;

import eu.amadeos.gameengine.data.TextData;
import eu.amadeos.gameengine.math.Vector;
import eu.amadeos.gameengine.tile.Direction;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomTest {
    static final String WORKSHOP = """
            # Lena's workshop
            room workshop
            background workshop.png
            depth 262 0.85 400 1.15
            floor 30 268  610 268  640 400  0 400
            hole 214 292  434 292  434 330  214 330
            spot start 300 340 up
            hotspot bench 214 240 220 60 stand 300 340 up
            hotspot psalter 292 238 62 16 stand 300 340 up
            hotspot window poly 40 44 190 44 190 214 40 214 stand 120 280 up
            exit door 368 104 64 158 stand 400 275 up to corridor workshop-door
            layer press.png 20 272 baseline 400
            hidden psalter
            """;

    static Room parse(String text) {
        return Room.parse(TextData.parse("workshop.room", new StringReader(text)));
    }

    @Test
    void readsARoom() {
        Room room = parse(WORKSHOP);
        assertEquals("workshop", room.id());
        assertEquals("workshop.png", room.background());
        assertEquals(0.85, room.depth().at(262), 1e-9);
        assertEquals(new Room.Spot("start", new Vector(300, 340), Direction.UP), room.spot("start"));
        assertTrue(room.floor().contains(new Vector(100, 300)));
        assertEquals(List.of(new Room.Layer("press.png", 20, 272, 400)), room.layers());
        Hotspot door = room.hotspot("door");
        assertTrue(door.isExit());
        assertEquals("corridor", door.exitTo());
        assertEquals("workshop-door", door.exitSpot());
    }

    @Test
    void theLaterHotspotLiesOnTopUnlessItIsHidden() {
        Room room = parse(WORKSHOP);
        Vector onBook = new Vector(300, 245);
        assertEquals("bench", room.hotspotAt(onBook).id(), "the psalter is hidden at first");
        room.show("psalter");
        assertEquals("psalter", room.hotspotAt(onBook).id());
        assertEquals("window", room.hotspotAt(new Vector(100, 100)).id());
        assertNull(room.hotspotAt(new Vector(620, 20)));
    }

    @Test
    void errorsNameTheLine() {
        TextData.DataException e = assertThrows(TextData.DataException.class,
                () -> parse("room r\nfloor 0 0 10 0 10 10\nhotspot x 1 1 2 2 stand 1 1 sideways\n"));
        assertEquals("workshop.room:3: \"sideways\" is no direction (up, down, left, right)", e.getMessage());
        assertThrows(TextData.DataException.class, () -> parse("room r\n"), "no floor");
        assertThrows(IllegalArgumentException.class, () -> parse(WORKSHOP).hide("nothing"));
    }
}
