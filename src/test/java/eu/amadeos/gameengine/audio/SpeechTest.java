package eu.amadeos.gameengine.audio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeechTest {
    /** The energy of the sound around {@code hz} between two samples (a Goertzel filter, one bin of a Fourier transform). */
    private static double energy(Sound sound, int from, int to, double hz) {
        double k = 2 * Math.cos(2 * Math.PI * hz / Sound.RATE), s1 = 0, s2 = 0;
        for (int i = from; i < to; i++) {
            double s = sound.sample(i) + k * s1 - s2;
            s2 = s1;
            s1 = s;
        }
        return s1 * s1 + s2 * s2 - k * s1 * s2;
    }

    /** The pitch of a stretch of voice: the lag at which it looks most like itself. */
    private static double pitch(Sound sound, int from, int to) {
        double best = 0;
        int bestLag = 0;
        for (int lag = Sound.RATE / 500; lag < Sound.RATE / 60; lag++) {
            double sum = 0;
            for (int i = from; i < to - lag; i++) sum += sound.sample(i) * (double) sound.sample(i + lag);
            if (sum > best) {
                best = sum;
                bestLag = lag;
            }
        }
        return Sound.RATE / (double) bestLag;
    }

    // --8<-- [start:test-speech]
    @Test
    void vowelsAreToldApartByTheirFormants() {
        Sound ee = Speech.say(Speech.Voice.MAN, "iy");
        Sound ah = Speech.say(Speech.Voice.MAN, "aa");
        int a = ee.length() / 4, b = ee.length() * 3 / 4;
        // "ee" has its second formant high, near 2300 Hz; "ah" has it low, near 1100 Hz
        assertTrue(energy(ee, a, b, 2290) > 10 * energy(ee, a, b, 1100), "ee rings high");
        int c = ah.length() / 4, d = ah.length() * 3 / 4;
        assertTrue(energy(ah, c, d, 1090) > 10 * energy(ah, c, d, 2290), "ah rings low");
    }

    @Test
    void aWomansVoiceIsHigherThanAMans() {
        Sound man = Speech.say(Speech.Voice.MAN, "aa");
        Sound woman = Speech.say(Speech.Voice.WOMAN, "aa");
        double low = pitch(man, man.length() / 3, man.length() / 3 + 2000);
        double high = pitch(woman, woman.length() / 3, woman.length() / 3 + 2000);
        assertEquals(1.08 * 112, low, 15, "about the voice's pitch, a little raised at the start of a phrase");
        assertTrue(high > 1.6 * low, high + " Hz against " + low + " Hz");
    }
    // --8<-- [end:test-speech]

    @Test
    void stressAndTheEndOfAPhraseMakeVowelsLonger() {
        List<Speech.Segment> plain = Speech.segments("p ah d / w ah n", 1);
        List<Speech.Segment> stressed = Speech.segments("p ah1 d / w ah1 n", 1);
        assertTrue(stressed.get(1).samples() > plain.get(1).samples());
        assertTrue(plain.get(4).samples() > plain.get(1).samples() * 1.2, "the last vowel is drawn out");
        List<Speech.Segment> slow = Speech.segments("p ae d", 0.5);
        assertEquals(2 * plain.get(0).samples(), slow.get(0).samples(), 1);
    }

    @Test
    void pausesAndTunesAreReadFromThePunctuation() {
        List<Speech.Segment> segments = Speech.segments("hh ey1 , k ae1 b !", 1);
        assertEquals(6, segments.size());
        assertTrue(segments.get(2).isPause());
        assertEquals(Speech.Tune.CALL, Speech.tune("hh ey1 !"));
        assertEquals(Speech.Tune.QUESTION, Speech.tune("w ah1 t ?"));
        assertEquals(Speech.Tune.STATEMENT, Speech.tune("ow1 k ey1"));
        assertEquals(4, Speech.segments("ow1 k ey1.", 1).size(), "a full stop after a word is a pause");
    }

    @Test
    void unknownPhonemesAreAnError() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Speech.say(Speech.Voice.MAN, "hh xx1"));
        assertTrue(e.getMessage().contains("xx1"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Speech.say(Speech.Voice.MAN, " , "));
        assertThrows(IllegalArgumentException.class, () -> new Speech.Voice(10, 1, 1, 0));
    }

    @Test
    void theSamePhraseSoundsTheSameAndFitsTheVolume() {
        Sound a = Speech.say(Speech.Voice.CHILD, "th ae1 ng k s !", 0.5);
        Sound b = Speech.say(Speech.Voice.CHILD, "th ae1 ng k s !", 0.5);
        int peak = 0;
        for (int i = 0; i < a.length(); i++) {
            assertEquals(a.sample(i), b.sample(i));
            peak = Math.max(peak, Math.abs(a.sample(i)));
        }
        assertEquals(0.5 * Short.MAX_VALUE, peak, 2);
        assertEquals(0, a.sample(0));
    }
}
