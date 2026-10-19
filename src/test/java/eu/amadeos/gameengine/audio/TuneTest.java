package eu.amadeos.gameengine.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TuneTest {
    /** How many times the wave crosses zero upwards between two samples: about the frequency, over a second. */
    private static int crossings(Sound sound, int from, int to) {
        int count = 0;
        for (int i = from + 1; i < to; i++) {
            if (sound.sample(i - 1) < 0 && sound.sample(i) >= 0) count++;
        }
        return count;
    }

    // --8<-- [start:test-tune]
    @Test
    void notesHaveTheirPitchAndLength() {
        assertEquals(440, Tune.frequency(4, 9), 1e-9);              // A in octave 4
        assertEquals(261.63, Tune.frequency(4, 0), 0.01);           // middle C
        assertEquals(880, Tune.frequency(5, 9), 1e-9);              // an octave up: twice the frequency
        // Tempo 60: a quarter note is a second. A whole A, then a half rest, then a dotted quarter C an octave up.
        Sound tune = Tune.voice(Sound.Wave.SQUARE, 0.5, "t60 o4 a1 r2 > c4.");
        assertEquals((4 + 2 + 1.5) * Sound.RATE, tune.length());
        assertEquals(440, crossings(tune, 0, Sound.RATE), 2, "440 Hz");
        assertEquals(0, tune.sample(5 * Sound.RATE), "the rest");
        assertTrue(crossings(tune, 6 * Sound.RATE, (int) (6.5 * Sound.RATE)) > 120, "C5, 523 Hz");
    }
    // --8<-- [end:test-tune]

    @Test
    void sharpsFlatsAndTheDefaultLength() {
        Sound sharp = Tune.voice(Sound.Wave.SQUARE, 0.5, "t60 l1 g#");
        Sound flat = Tune.voice(Sound.Wave.SQUARE, 0.5, "t60 l1 a-");
        assertEquals(sharp.length(), flat.length());
        assertEquals(4 * Sound.RATE, sharp.length());
        for (int i = 0; i < sharp.length(); i += 997) assertEquals(sharp.sample(i), flat.sample(i));
        assertThrows(IllegalArgumentException.class, () -> Tune.voice(Sound.Wave.SQUARE, 0.5, "c x"));
        assertThrows(IllegalArgumentException.class, () -> Tune.voice(Sound.Wave.SQUARE, 0.5, "t c"));
    }

    @Test
    void voicesAreMixedIntoOneSound() {
        Sound melody = Tune.voice(Sound.Wave.SQUARE, 0.3, "t120 c d e f");
        Sound bass = Tune.voice(Sound.Wave.TRIANGLE, 0.3, "t120 o2 c2 g2");
        Sound both = Sound.mix(melody, bass);
        assertEquals(melody.length(), both.length(), "both two seconds long");
        int i = Sound.RATE / 3;
        assertEquals(melody.sample(i) + bass.sample(i), both.sample(i));
    }
}
