package eu.amadeos.gameengine.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The mixing itself, without a sound card. */
class MixerTest {
    private static Sound constant(int value, int length) {
        short[] samples = new short[length];
        java.util.Arrays.fill(samples, (short) value);
        return Sound.of(samples);
    }

    // --8<-- [start:test-mix]
    @Test
    void soundsAreAddedUpAndClipped() {
        Mixer mixer = new Mixer();
        mixer.queue(constant(1000, 600));
        mixer.queue(constant(500, 100));
        short[] block = new short[Mixer.BLOCK];
        mixer.mix(block);
        assertEquals(1500, block[0]);     // both
        assertEquals(1000, block[100]);   // the short one has ended
        mixer.queue(constant(32000, 100));
        mixer.mix(block);
        assertEquals(Short.MAX_VALUE, block[0]);  // 33000 does not fit: clipped, not wrapped around
        assertEquals(0, block[100]);              // the long one ended at 600 samples
        assertEquals(0, mixer.voices());
    }
    // --8<-- [end:test-mix]

    // --8<-- [start:test-hiss]
    @Test
    void silenceIsNeverPureButFarBelowHearing() {
        Mixer mixer = new Mixer();
        short[] block = new short[Mixer.BLOCK];
        mixer.mix(block);
        mixer.hiss(block);
        int loudest = 0;
        int zeros = 0;
        for (short s : block) {
            loudest = Math.max(loudest, Math.abs(s));
            if (s == 0) zeros++;
        }
        assertTrue(loudest <= 2, "inaudible: " + loudest);
        assertTrue(zeros < Mixer.BLOCK / 2, "mostly not zero: " + zeros);
        block[0] = Short.MAX_VALUE;
        mixer.hiss(block);
        assertTrue(block[0] >= Short.MAX_VALUE - 1, "clipped, not wrapped around");
    }
    // --8<-- [end:test-hiss]

    @Test
    void tooManySoundsReplaceTheOldest() {
        Mixer mixer = new Mixer();
        for (int i = 0; i < Mixer.VOICES + 4; i++) mixer.queue(constant(1, 10_000));
        mixer.mix(new short[Mixer.BLOCK]);
        assertEquals(Mixer.VOICES, mixer.voices());
    }

    @Test
    void aMixerWithoutSoundCardIsSilent() {
        try (Mixer mixer = new Mixer()) {
            mixer.start();
            // Wherever the tests run, play must neither fail nor wait.
            mixer.play(Sound.tone(Sound.Wave.SQUARE, 440, 440, 0.05, 0.1));
            assertTrue(true);
        }
    }

    @Test
    void tonesHaveTheirLengthAndFadeInAndOut() {
        Sound tone = Sound.tone(Sound.Wave.SQUARE, 440, 880, 0.1, 0.5);
        assertEquals(4410, tone.length());
        assertEquals(0, tone.sample(0));
        assertEquals(0, tone.sample(tone.length() - 1));
        int loudest = 0;
        for (int i = 0; i < tone.length(); i++) loudest = Math.max(loudest, Math.abs(tone.sample(i)));
        assertEquals(Short.MAX_VALUE / 2, loudest, 1);
        assertEquals(8820, tone.then(tone).length());
    }
}
