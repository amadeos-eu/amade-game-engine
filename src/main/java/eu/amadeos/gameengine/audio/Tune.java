package eu.amadeos.gameengine.audio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A melody written as text, in the <i>Music Macro Language</i> (MML) of the home computers of the 1980s:
 * BASIC's {@code PLAY "T120 O4 L8 CDEFG"} played a scale on an IBM PC, an MSX or a Commodore 128. One
 * voice per string; {@link Sound#mix} plays several together.
 *
 * <table>
 * <caption>The commands</caption>
 * <tr><td>{@code c d e f g a b}</td><td>a note; {@code +} or {@code #} after it raises it a semitone,
 *     {@code -} lowers it; a number after it is its length, a dot makes it half as long again</td></tr>
 * <tr><td>{@code r}</td><td>a rest, with a length like a note</td></tr>
 * <tr><td>{@code l4}</td><td>the length of notes without a number: 1 whole, 2 half, 4 quarter, 8, 16, 32</td></tr>
 * <tr><td>{@code o4}</td><td>the octave; in octave 4, {@code a} is 440 Hz. {@code >} goes up one, {@code <}
 *     down one</td></tr>
 * <tr><td>{@code t120}</td><td>the tempo, in quarter notes per minute</td></tr>
 * </table>
 *
 * <p>Spaces and bars ({@code |}) are only for the eye. Each note is a {@link Sound#tone} of the voice's wave,
 * a little shorter than its length, so repeated notes are heard as separate notes.
 */
public final class Tune {
    private Tune() {
    }

    // --8<-- [start:tune]
    /** One voice of a tune, played with {@code wave} at {@code volume} 0 to 1. */
    public static Sound voice(Sound.Wave wave, double volume, String mml) {
        String text = mml.toLowerCase(Locale.ROOT);
        double tempo = 120;
        int octave = 4;
        double defaultLength = 4;
        double beats = 0;                 // where in the tune we are, in quarter notes
        List<double[]> notes = new ArrayList<>();    // start beat, beats, frequency
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i++);
            switch (c) {
                case 't' -> {
                    int[] read = number(text, i);
                    tempo = read[0];
                    i = read[1];
                }
                case 'o' -> {
                    int[] read = number(text, i);
                    octave = read[0];
                    i = read[1];
                }
                case 'l' -> {
                    int[] read = number(text, i);
                    defaultLength = read[0];
                    i = read[1];
                }
                case '>' -> octave++;
                case '<' -> octave--;
                case 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'r' -> {
                    int semitone = c == 'r' ? 0 : "c d ef g a b".indexOf(c);
                    while (i < text.length() && "+#-".indexOf(text.charAt(i)) >= 0) {
                        semitone += text.charAt(i++) == '-' ? -1 : 1;
                    }
                    double length = defaultLength;
                    if (i < text.length() && Character.isDigit(text.charAt(i))) {
                        int[] read = number(text, i);
                        length = read[0];
                        i = read[1];
                    }
                    double noteBeats = 4 / length;
                    for (double dot = noteBeats / 2; i < text.length() && text.charAt(i) == '.'; dot /= 2, i++) {
                        noteBeats += dot;
                    }
                    if (c != 'r') notes.add(new double[]{beats, noteBeats, frequency(octave, semitone)});
                    beats += noteBeats;
                }
                case ' ', '|', '\n', '\t' -> {
                }
                default -> throw new IllegalArgumentException("unknown MML command '" + c + "' at " + (i - 1));
            }
        }
        double samplesPerBeat = Sound.RATE * 60 / tempo;
        short[] samples = new short[(int) Math.round(beats * samplesPerBeat)];
        for (double[] note : notes) {
            // Start and end from the beat count, not by adding up rounded note lengths: voices stay together.
            int start = (int) Math.round(note[0] * samplesPerBeat);
            int end = (int) Math.round((note[0] + note[1]) * samplesPerBeat);
            int sounding = (int) ((end - start) * 0.9);
            Sound tone = Sound.tone(wave, note[2], note[2], sounding / (double) Sound.RATE, volume);
            for (int s = 0; s < tone.length() && start + s < samples.length; s++) {
                samples[start + s] = tone.sample(s);
            }
        }
        return Sound.of(samples);
    }

    /** The frequency of a note: 440 Hz for A in octave 4, and twelve equal semitones to each octave. */
    static double frequency(int octave, int semitone) {
        int fromA4 = (octave - 4) * 12 + semitone - 9;
        return 440 * Math.pow(2, fromA4 / 12.0);
    }
    // --8<-- [end:tune]

    /** The number at {@code from} and the position after it. */
    private static int[] number(String text, int from) {
        int end = from;
        while (end < text.length() && Character.isDigit(text.charAt(end))) end++;
        if (end == from) throw new IllegalArgumentException("a number expected at " + from + " in \"" + text + '"');
        return new int[]{Integer.parseInt(text.substring(from, end)), end};
    }
}
