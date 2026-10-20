package eu.amadeos.gameengine.data;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogTest {
    static List<Catalog.Entry> read(String text) {
        return Catalog.read(TextData.parse("units.txt", new StringReader(text)), "unit", "tower");
    }

    // --8<-- [start:test-read]
    @Test
    void blocksHaveNamesAndPropertiesAndCanBeLikeAnother() {
        List<Catalog.Entry> entries = read("""
                unit orc
                  health 60
                  speed 1.1     # tiles per second
                  bounty 4
                unit chief like orc
                  health 180
                tower archer
                  level 40 range 3
                  level 30 range 3.5
                """);
        assertEquals(3, entries.size());
        Catalog.Entry chief = entries.get(1);
        assertEquals("unit", chief.kind());
        assertEquals("chief", chief.name());
        assertEquals(180, chief.number("health"));
        assertEquals(1.1, chief.number("speed"), "taken over from the orc");
        assertEquals(4, chief.integer("bounty"));
        assertEquals(0.5, chief.number("armour", 0.5), "not given: the fallback");
        assertFalse(chief.has("armour"));
        chief.finish();
        assertEquals(2, entries.get(2).lines("level").size(), "a property given several times");
        assertEquals(3.5, entries.get(2).lines("level").get(1).number(3));
    }
    // --8<-- [end:test-read]

    // --8<-- [start:test-errors]
    @Test
    void mistakesNameTheFileAndTheLine() {
        assertEquals("units.txt:1: expected unit or tower before \"health\"",
                assertThrows(TextData.DataException.class, () -> read("health 60\nunit orc\n")).getMessage());
        assertEquals("units.txt:3: there is already a unit orc",
                assertThrows(TextData.DataException.class, () -> read("unit orc\nhealth 1\nunit orc\n")).getMessage());
        assertEquals("units.txt:2: no unit troll before this line",
                assertThrows(TextData.DataException.class, () -> read("unit orc\nunit wolf like troll\n")).getMessage());

        Catalog.Entry orc = read("""
                unit orc
                  health 60
                  sped 1.1
                  speed 1.0 1.2
                """).getFirst();
        assertEquals(60, orc.number("health"));
        assertEquals("units.txt:4: speed takes one number",
                assertThrows(TextData.DataException.class, () -> orc.number("speed")).getMessage());
        assertEquals("units.txt:1: unit orc: bounty is missing",
                assertThrows(TextData.DataException.class, () -> orc.integer("bounty")).getMessage());
        // The game asked for health, speed and bounty; "sped" is a typo nobody asked for.
        assertEquals("units.txt:3: \"sped\" is not a property of a unit",
                assertThrows(TextData.DataException.class, orc::finish).getMessage());
    }
    // --8<-- [end:test-errors]

    @Test
    void aPropertyGivenTwiceIsAnErrorWhereOnlyOneIsExpected() {
        Catalog.Entry orc = read("unit orc\nhealth 60\nhealth 70\n").getFirst();
        assertEquals("units.txt:3: health is given twice",
                assertThrows(TextData.DataException.class, () -> orc.number("health")).getMessage());
        Catalog.Entry wolf = read("unit orc\nhealth 60\nunit wolf like orc\nhealth 30\n").get(1);
        assertEquals(30, wolf.number("health"), "the block's own line replaces the one taken over");
        assertTrue(wolf.has("health"));
    }
}
