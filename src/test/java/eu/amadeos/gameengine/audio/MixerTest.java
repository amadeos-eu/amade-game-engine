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

    // --8<-- [start:test-music]
    @Test
    void musicPlaysBesideTheEffectsAndLoops() {
        Mixer mixer = new Mixer();
        mixer.playMusic(constant(100, 300), true);
        for (int i = 0; i < Mixer.VOICES + 2; i++) mixer.queue(constant(1, 10_000));
        short[] block = new short[Mixer.BLOCK];
        mixer.mix(block);
        assertEquals(100 + Mixer.VOICES, block[0], "the music and every effect voice");
        assertEquals(100 + Mixer.VOICES, block[300], "the music again from its start");
        mixer.setMuted(true);
        mixer.mix(block);
        assertEquals(Mixer.VOICES, block[0], "muted: the effects play to their end, the music is silent");
        assertTrue(mixer.playingMusic(), "but goes on");
        mixer.stopMusic();
        mixer.mix(block);
        assertTrue(!mixer.playingMusic());
    }

    @Test
    void musicOnceEnds() {
        Mixer mixer = new Mixer();
        mixer.playMusic(constant(100, 300), false);
        short[] block = new short[Mixer.BLOCK];
        mixer.mix(block);
        assertEquals(100, block[299]);
        assertEquals(0, block[300]);
        assertTrue(!mixer.playingMusic());
    }
    // --8<-- [end:test-music]

    // --8<-- [start:test-drone]
    @Test
    void theDroneGlidesToItsVolumeAndPlaysFasterAtAHigherRate() {
        Mixer mixer = new Mixer();
        short[] ramp = new short[1000];
        for (int i = 0; i < ramp.length; i++) ramp[i] = (short) (i * 10);
        mixer.setDrone(Sound.of(ramp));
        short[] block = new short[Mixer.BLOCK];
        mixer.mix(block);
        assertEquals(0, block[100], "silent until a volume is set");
        mixer.setDroneVolume(1);
        for (int n = 0; n < 20; n++) mixer.mix(block);   // the volume has glided up
        mixer.setDroneRate(2);
        mixer.mix(block);
        int steps = 0;
        for (int i = 1; i < 200; i++) if (block[i] > block[i - 1]) steps += block[i] - block[i - 1];
        assertTrue(steps / 199.0 > 15, "two samples of the ramp per sample: about 20 a step, was " + steps / 199.0);
        mixer.setMuted(true);
        for (int n = 0; n < 20; n++) mixer.mix(block);
        assertTrue(Math.abs(block[Mixer.BLOCK - 1]) < 50, "muted, the drone fades out");
    }
    // --8<-- [end:test-drone]

    // --8<-- [start:test-stream]
    @Test
    void aStreamPlaysWhatItsWriterSentAndThenEnds() throws InterruptedException {
        Mixer mixer = new Mixer();
        SampleStream stream = new SampleStream(0.1);
        short[] samples = new short[300];
        java.util.Arrays.fill(samples, (short) 200);
        assertTrue(stream.write(samples, samples.length));
        stream.end();
        mixer.setMusicVolume(0.5);
        mixer.playMusic(stream);
        short[] block = new short[Mixer.BLOCK];
        mixer.mix(block);
        assertEquals(100, block[0], "at half the music volume");
        assertEquals(0, block[300], "nothing more was written");
        assertTrue(!mixer.playingMusic());
        stream.close();
        assertTrue(!stream.write(samples, 1), "a closed stream tells its writer to stop");
    }

    @Test
    void bedsPlayTogether() {
        Mixer mixer = new Mixer();
        Mixer.Bed wind = mixer.bed(constant(1000, 50));
        Mixer.Bed birds = mixer.bed(constant(500, 70));
        wind.setVolume(1);
        birds.setVolume(1);
        short[] block = new short[Mixer.BLOCK];
        for (int n = 0; n < 20; n++) mixer.mix(block);
        assertEquals(1500, block[100], 2);
        birds.setSound(null);
        for (int n = 0; n < 2; n++) mixer.mix(block);
        assertEquals(1000, block[100], 2);
    }
    // --8<-- [end:test-stream]
}
