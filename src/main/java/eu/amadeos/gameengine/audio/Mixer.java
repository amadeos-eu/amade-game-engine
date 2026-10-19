package eu.amadeos.gameengine.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Plays sound effects, several at once. A brick breaks while the ball hits the wall: both sounds must be
 * heard together, and neither may wait for the other.
 *
 * <p>The mixer opens <i>one</i> line to the sound card and feeds it from a thread of its own, block by
 * block. For every block it adds up the samples of all sounds that are playing (that is all mixing is) and
 * clips the sum to the range a sample has. {@link #play} only puts the sound in a queue; the game loop never
 * waits for the sound card.
 *
 * <p>Besides the effects, the mixer plays one piece of <i>music</i>, once or again and again, on a voice of
 * its own that no effect can push out. Muting turns the music down, it does not stop it: switched on again,
 * it goes on where it would be.
 *
 * <p>Without a sound card (a server, a test machine) the mixer stays silent and the game plays on.
 */
public final class Mixer implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(Mixer.class);

    /** 16-bit mono at 44.1 kHz, the format of all {@link Sound}s. */
    public static final AudioFormat FORMAT = new AudioFormat(Sound.RATE, 16, 1, true, false);
    /** Samples per block: 512 are 11.6 ms, the longest a new sound waits. */
    static final int BLOCK = 512;
    /** Most sounds at once; a new one replaces the oldest. */
    static final int VOICES = 16;

    /** A sound being played and how far it has got. */
    private static final class Voice {
        final Sound sound;
        int position;

        Voice(Sound sound) {
            this.sound = sound;
        }
    }

    /** A change of the music, from the game's thread to the mixer's; {@code sound} null stops it. */
    private record MusicRequest(Sound sound, boolean loop) {
    }

    private final Queue<Sound> requests = new ConcurrentLinkedQueue<>();
    private final List<Voice> voices = new ArrayList<>();
    private final AtomicReference<MusicRequest> musicRequest = new AtomicReference<>();
    /** The music playing, on the mixer thread only; {@code null} for none. */
    private Voice music;
    private boolean musicLoops;
    private volatile boolean running;
    private volatile boolean muted;
    private SourceDataLine line;
    private Thread thread;
    /** The state of the hiss's random numbers; only the mixer thread touches it. */
    private long noise = 0x2545F4914F6CDD1DL;

    /** Opens the sound card; if there is none, the mixer stays silent. */
    public synchronized void start() {
        if (running) return;
        try {
            line = AudioSystem.getSourceDataLine(FORMAT);
            // Room for four blocks: enough against hiccups, short enough for a quick reaction.
            line.open(FORMAT, 4 * BLOCK * 2);
            line.start();
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException e) {
            LOG.info("No sound: {}", e.getMessage());
            line = null;
            return;
        }
        running = true;
        thread = new Thread(this::run, "audio-mixer");
        thread.setDaemon(true);
        thread.start();
    }

    /** True while sounds reach the sound card. */
    public boolean running() {
        return running;
    }

    // --8<-- [start:play]
    /** Plays a sound as soon as possible; safe to call from any thread, never waits. */
    public void play(Sound sound) {
        if (running && !muted) requests.add(sound);
    }
    // --8<-- [end:play]

    // --8<-- [start:music]
    /**
     * Plays {@code music} instead of the music playing now, from its start; with {@code loop}, again and
     * again until {@link #stopMusic()}. Safe to call from any thread, never waits.
     */
    public void playMusic(Sound music, boolean loop) {
        musicRequest.set(new MusicRequest(music, loop));
    }

    /** Stops the music at once. */
    public void stopMusic() {
        musicRequest.set(new MusicRequest(null, false));
    }
    // --8<-- [end:music]

    public boolean muted() {
        return muted;
    }

    /** Silences new sounds; those playing play to their end. */
    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    // --8<-- [start:run]
    private void run() {
        short[] block = new short[BLOCK];
        byte[] bytes = new byte[BLOCK * 2];
        while (running) {
            mix(block);
            hiss(block);
            for (int i = 0; i < BLOCK; i++) {
                bytes[2 * i] = (byte) block[i];             // little endian: low byte first
                bytes[2 * i + 1] = (byte) (block[i] >> 8);
            }
            // Waits while the line is full: this is what paces the thread.
            line.write(bytes, 0, bytes.length);
        }
    }
    // --8<-- [end:run]

    // --8<-- [start:mix]
    /** Fills {@code block} with the sum of all playing sounds; on the mixer thread, or in tests. */
    void mix(short[] block) {
        for (Sound s; (s = requests.poll()) != null; ) {
            if (voices.size() == VOICES) voices.removeFirst();
            voices.add(new Voice(s));
        }
        MusicRequest change = musicRequest.getAndSet(null);
        if (change != null) {
            music = change.sound() == null ? null : new Voice(change.sound());
            musicLoops = change.loop();
        }
        for (int i = 0; i < block.length; i++) {
            int sum = 0;
            for (Voice v : voices) {
                if (v.position < v.sound.length()) sum += v.sound.sample(v.position++);
            }
            sum += music();
            // Two loud sounds together exceed what 16 bits can hold: clip instead of wrapping around.
            block[i] = (short) Math.clamp(sum, Short.MIN_VALUE, Short.MAX_VALUE);
        }
        voices.removeIf(v -> v.position >= v.sound.length());
    }
    // --8<-- [end:mix]

    /** The music's next sample, silent while muted; at its end it starts again or stops. */
    private int music() {
        if (music == null) return 0;
        int sample = music.sound.length() == 0 ? 0 : music.sound.sample(music.position);
        if (++music.position >= music.sound.length()) {
            if (musicLoops) music.position = 0;
            else music = null;
        }
        return muted ? 0 : sample;
    }

    // --8<-- [start:hiss]
    /**
     * Adds a hiss of one or two steps out of 32768, far below hearing, so the line never carries pure
     * silence. Some wireless headsets and their dongles switch off after a few seconds of zeros and take
     * many more seconds to wake up again: the first sounds of a game would be lost.
     */
    void hiss(short[] block) {
        for (int i = 0; i < block.length; i++) {
            noise ^= noise << 13;
            noise ^= noise >>> 7;
            noise ^= noise << 17;
            block[i] = (short) Math.clamp(block[i] + (noise & 3) - 1, Short.MIN_VALUE, Short.MAX_VALUE);
        }
    }
    // --8<-- [end:hiss]

    /** For tests: true while music plays, on the mixer thread. */
    boolean playingMusic() {
        return music != null;
    }

    /** For tests: sounds playing or waiting. */
    int voices() {
        return voices.size();
    }

    /** For tests and for games that run without the thread: queue a sound even when not started. */
    void queue(Sound sound) {
        requests.add(sound);
    }

    @Override
    public synchronized void close() {
        if (!running) return;
        running = false;
        try {
            thread.join(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        line.stop();
        line.close();
    }
}
