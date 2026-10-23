package eu.amadeos.gameengine.audio;

import java.util.ArrayList;
import java.util.List;

/**
 * A talking machine: makes a {@link Sound} of a phrase written as {@link Phoneme}s, {@code "hh ey1 , k ae1 b !"}
 * for "Hey, cab!", in a voice of its own. No recordings: like the speaking arcade machines and home
 * computers of the early 1980s, it is a <i>formant synthesizer</i>, a small model of the human voice.
 *
 * <p>A voice is a source and a filter. The source is the vocal folds, which let the air out in puffs, a
 * hundred times a second for a man and two hundred for a woman: that is the pitch. A narrow place in the
 * mouth adds hiss instead, for an <i>s</i> or an <i>f</i>. The filter is the mouth and the throat, a tube
 * that rings at a few frequencies, the formants, and moving the tongue and the lips moves them. A vowel is
 * nothing but where its formants are. So the synthesizer makes the puffs and the hiss, sends them through
 * five resonators tuned to the formants, and moves the formants smoothly from one phoneme to the next, as a
 * tongue does.
 *
 * <p>The phrase: phonemes by their ARPAbet names, separated by spaces. A {@code 1} after a vowel stresses
 * it: longer and higher. {@code /} only separates words for the eye, {@code ,} is a short pause and
 * {@code .} a long one. A phrase ending in {@code !} is called out, one ending in {@code ?} rises at the
 * end; without either it falls, as a statement does.
 */
public final class Speech {
    /**
     * Who speaks: the pitch of the voice in Hz, the size of the mouth as a factor on the formants (women's
     * and children's mouths are smaller, so their formants are higher), the pace as a factor on the speed,
     * and how much breath hisses in the voice, 0 to 1.
     */
    // --8<-- [start:voice]
    public record Voice(double pitch, double formants, double pace, double breath) {
        public static final Voice MAN = new Voice(112, 1.0, 1.0, 0.04);
        public static final Voice WOMAN = new Voice(205, 1.15, 1.0, 0.08);
        public static final Voice CHILD = new Voice(280, 1.28, 0.95, 0.06);
        public static final Voice OLD_MAN = new Voice(92, 0.97, 0.85, 0.16);
        public static final Voice ROBOT = new Voice(100, 1.05, 1.0, 0);

        public Voice {
            if (pitch < 40 || pitch > 600) throw new IllegalArgumentException("pitch out of range: " + pitch);
            if (formants < 0.7 || formants > 1.5) throw new IllegalArgumentException("formants out of range: " + formants);
            if (pace <= 0) throw new IllegalArgumentException("pace must be positive: " + pace);
        }

        /** The same voice at another pitch: to tell two passengers apart who are both women. */
        public Voice withPitch(double hz) {
            return new Voice(hz, formants, pace, breath);
        }
    }
    // --8<-- [end:voice]

    /** How the pitch moves over the phrase. */
    enum Tune { STATEMENT, CALL, QUESTION }

    /** A phoneme or a pause in a phrase, with its length in samples; a pause has no phoneme. */
    record Segment(Phoneme phoneme, boolean stressed, int samples) {
        boolean isPause() {
            return phoneme == null;
        }
    }

    /** Where the formants are at a moment of the phrase; between two such points they move in a straight line. */
    private record Anchor(double at, double f1, double f2, double f3) {
    }

    private static final double PAUSE_MILLIS = 130;
    private static final double LONG_PAUSE_MILLIS = 260;
    /** The resonances above F3 hardly move; they give a voice its colour. */
    private static final double F4 = 3300, F5 = 3850;
    private static final double B1 = 80, B2 = 100, B3 = 150, B4 = 250, B5 = 300;
    /** The coefficients of the resonators are worked out anew every so many samples, not at every one. */
    private static final int CONTROL = 16;

    private Speech() {
    }

    /** The phrase spoken by {@code voice} at a moderate volume. */
    public static Sound say(Voice voice, String phrase) {
        return say(voice, phrase, 0.5);
    }

    // --8<-- [start:say]
    /** The phrase spoken by {@code voice}, its loudest moment at {@code volume} 0 to 1. */
    public static Sound say(Voice voice, String phrase, double volume) {
        Tune tune = tune(phrase);
        List<Segment> segments = segments(phrase, voice.pace());
        int[] starts = new int[segments.size() + 1];
        for (int i = 0; i < segments.size(); i++) starts[i + 1] = starts[i] + segments.get(i).samples();
        int tail = Sound.RATE / 30;                 // the resonators ring out
        int length = starts[segments.size()] + tail;
        List<Anchor> anchors = anchors(segments, starts);

        Resonator[] tract = {new Resonator(), new Resonator(), new Resonator(), new Resonator(), new Resonator()};
        BandPass hiss = new BandPass();
        double[] out = new double[length];
        long noise = 0x9E3779B97F4A7C15L;           // a fixed seed: the same phrase sounds the same each time
        double phase = 0, tilt = 0, voiceLevel = 0, hissLevel = 0, breathLevel = 0, nasal = 0;
        double jitter = 0;
        int segment = 0, anchor = 0;
        for (int i = 0; i < length; i++) {
            while (segment < segments.size() && i >= starts[segment + 1]) segment++;
            Segment s = segment < segments.size() ? segments.get(segment) : null;
            double within = s == null ? 1 : (i - starts[segment]) / (double) s.samples();
            Source source = source(s, within, voice);

            // the sources: the amplitudes follow their targets within a few milliseconds, against clicks
            voiceLevel += (source.voice - voiceLevel) * 0.006;
            hissLevel += (source.hiss - hissLevel) * (source.hiss > hissLevel ? 0.05 : 0.01);
            breathLevel += (source.breath - breathLevel) * 0.01;
            nasal += ((s != null && s.phoneme() != null && s.phoneme().kind == Phoneme.Kind.NASAL ? 1 : 0) - nasal) * 0.004;

            if (i % CONTROL == 0) {
                while (anchor + 1 < anchors.size() && anchors.get(anchor + 1).at() <= i) anchor++;
                double[] f = formants(anchors, anchor, i);
                double scale = voice.formants();
                double widen = 1 + 2.5 * nasal;
                tract[0].tune(f[0] * scale, B1 * (1 + nasal));
                tract[1].tune(f[1] * scale, B2 * widen);
                tract[2].tune(f[2] * scale, B3 * widen);
                tract[3].tune(F4 * scale, B4);
                tract[4].tune(F5 * scale, B5);
                if (s != null && s.phoneme() != null && s.phoneme().noise > 0) {
                    hiss.tune(s.phoneme().noise, s.phoneme().noiseWidth);
                }
                jitter = jitter * 0.9 + (random(noise ^ i) - 0.5) * 0.004;
            }

            // the vocal folds: one puff of air per period, its slope is what the ear hears
            double hz = pitch(voice.pitch(), tune, segments, starts, segment, i, length) * (1 + jitter);
            phase += hz / Sound.RATE;
            if (phase >= 1) phase -= 1;
            double open = 0.6;
            double puff = phase < open ? 2 * (phase / open) - 3 * (phase / open) * (phase / open) : 0;
            tilt += (puff - tilt) * 0.35;                    // a soft voice, not a buzzer

            noise ^= noise << 13;
            noise ^= noise >>> 7;
            noise ^= noise << 17;
            double white = (noise >>> 11) / (double) (1L << 53) * 2 - 1;

            double excitation = tilt * voiceLevel * (1 - nasal * 0.35)
                    + white * (breathLevel + voice.breath() * voiceLevel * 0.08) * 0.35;
            double y = excitation;
            for (Resonator r : tract) y = r.next(y);
            out[i] = y + hiss.next(white) * hissLevel * 0.9;
        }
        return normalized(out, volume);
    }
    // --8<-- [end:say]

    // ---- reading the phrase ------------------------------------------------------------------------

    static Tune tune(String phrase) {
        String end = phrase.strip();
        if (end.endsWith("!")) return Tune.CALL;
        if (end.endsWith("?")) return Tune.QUESTION;
        return Tune.STATEMENT;
    }

    /** The phonemes and pauses of a phrase with their lengths; unknown names are an error. */
    static List<Segment> segments(String phrase, double pace) {
        List<Segment> segments = new ArrayList<>();
        for (String token : phrase.strip().split("\\s+")) {
            if (token.isEmpty() || token.equals("/") || token.equals("!") || token.equals("?")) continue;
            if (token.equals(",")) {
                segments.add(new Segment(null, false, samples(PAUSE_MILLIS / pace)));
                continue;
            }
            if (token.equals(".")) {
                segments.add(new Segment(null, false, samples(LONG_PAUSE_MILLIS / pace)));
                continue;
            }
            String name = token.replaceAll("[!?.,]+$", "");
            boolean stressed = name.endsWith("1") || name.endsWith("2");
            String bare = name.replaceAll("[012]$", "");
            Phoneme phoneme = Phoneme.named(bare).orElseThrow(() ->
                    new IllegalArgumentException("unknown phoneme '" + token + "' in \"" + phrase + "\""));
            segments.add(new Segment(phoneme, stressed && phoneme.isVowel(), 0));
            if (name.length() < token.length()) {
                String mark = token.substring(name.length());
                if (mark.contains(",")) segments.add(new Segment(null, false, samples(PAUSE_MILLIS / pace)));
                if (mark.contains(".")) segments.add(new Segment(null, false, samples(LONG_PAUSE_MILLIS / pace)));
            }
        }
        if (segments.stream().allMatch(Segment::isPause)) throw new IllegalArgumentException("nothing to say: " + phrase);
        // lengths: stressed vowels are longer, and so is the last vowel before a pause or the end
        for (int i = 0; i < segments.size(); i++) {
            Segment s = segments.get(i);
            if (s.isPause()) continue;
            double millis = s.phoneme().millis;
            if (s.phoneme().isVowel()) {
                millis *= s.stressed() ? 1.35 : 0.85;
                if (lastVowelBeforePause(segments, i)) millis *= 1.35;
            }
            segments.set(i, new Segment(s.phoneme(), s.stressed(), samples(millis / pace)));
        }
        return segments;
    }

    private static boolean lastVowelBeforePause(List<Segment> segments, int index) {
        for (int i = index + 1; i < segments.size(); i++) {
            Segment s = segments.get(i);
            if (s.isPause()) return true;
            if (s.phoneme().isVowel()) return false;
        }
        return true;
    }

    private static int samples(double millis) {
        return (int) Math.round(millis * Sound.RATE / 1000);
    }

    // ---- the filter: where the formants are -----------------------------------------------------------

    // --8<-- [start:anchors]
    /**
     * The points the formants pass through. A vowel holds its formants through its middle, a diphthong
     * glides from its first to its second vowel, a stop sits at its locus from closing to opening, so the
     * vowel after it starts from there: the ear hears the stop in that movement more than in the burst.
     */
    private static List<Anchor> anchors(List<Segment> segments, int[] starts) {
        List<Anchor> anchors = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            Segment s = segments.get(i);
            if (s.isPause()) continue;
            Phoneme p = s.phoneme();
            double start = starts[i], length = s.samples();
            switch (p.kind) {
                case VOWEL -> {
                    if (p.isDiphthong()) {
                        anchors.add(new Anchor(start + 0.2 * length, p.f1, p.f2, p.f3));
                        anchors.add(new Anchor(start + 0.9 * length, p.g1, p.g2, p.g3));
                    } else {
                        anchors.add(new Anchor(start + 0.25 * length, p.f1, p.f2, p.f3));
                        anchors.add(new Anchor(start + 0.75 * length, p.f1, p.f2, p.f3));
                    }
                }
                case GLIDE, NASAL -> {
                    anchors.add(new Anchor(start + 0.3 * length, p.f1, p.f2, p.f3));
                    anchors.add(new Anchor(start + 0.7 * length, p.f1, p.f2, p.f3));
                }
                case FRICATIVE -> anchors.add(new Anchor(start + 0.5 * length, p.f1, p.f2, p.f3));
                case STOP, AFFRICATE -> {
                    anchors.add(new Anchor(start, p.f1, p.f2, p.f3));
                    anchors.add(new Anchor(start + length, p.f1, p.f2, p.f3));
                }
                case ASPIRATE -> {
                    // breath takes the shape of the mouth that comes next
                    Phoneme next = i + 1 < segments.size() && !segments.get(i + 1).isPause()
                            ? segments.get(i + 1).phoneme() : Phoneme.AX;
                    if (next.kind == Phoneme.Kind.ASPIRATE) next = Phoneme.AX;
                    anchors.add(new Anchor(start, next.f1, next.f2, next.f3));
                    anchors.add(new Anchor(start + length, next.f1, next.f2, next.f3));
                }
            }
        }
        if (anchors.isEmpty()) anchors.add(new Anchor(0, Phoneme.AX.f1, Phoneme.AX.f2, Phoneme.AX.f3));
        return anchors;
    }
    // --8<-- [end:anchors]

    private static double[] formants(List<Anchor> anchors, int index, int sample) {
        Anchor a = anchors.get(index);
        if (sample <= a.at() || index + 1 >= anchors.size()) return new double[]{a.f1(), a.f2(), a.f3()};
        Anchor b = anchors.get(index + 1);
        double t = (sample - a.at()) / Math.max(1, b.at() - a.at());
        return new double[]{a.f1() + (b.f1() - a.f1()) * t, a.f2() + (b.f2() - a.f2()) * t, a.f3() + (b.f3() - a.f3()) * t};
    }

    // ---- the sources --------------------------------------------------------------------------------

    /** How loud the voice, the hiss of a narrow place and the breath through an open mouth are wanted. */
    private record Source(double voice, double hiss, double breath) {
        static final Source SILENT = new Source(0, 0, 0);
    }

    // --8<-- [start:source]
    private static Source source(Segment s, double within, Voice voice) {
        if (s == null || s.isPause()) return Source.SILENT;
        Phoneme p = s.phoneme();
        double hum = p.voiced ? 1 : 0;
        return switch (p.kind) {
            case VOWEL -> new Source(s.stressed() ? 1 : 0.8, 0, 0);
            case GLIDE -> new Source(0.75, 0, 0);
            case NASAL -> new Source(0.6, 0, 0);
            case FRICATIVE -> new Source(0.45 * hum, p.noiseLevel, 0);
            case ASPIRATE -> new Source(0, 0, 1.6);
            case STOP -> {
                // closed, then a burst of noise, then breath (unvoiced) or the voice starting (voiced)
                double closed = p.voiced ? 0.7 : 0.55;
                double burst = closed + 0.12;
                if (within < closed) yield new Source(0.08 * hum, 0, 0);
                if (within < burst) yield new Source(0.3 * hum, p.noiseLevel, 0);
                yield p.voiced ? new Source(0.7, 0, 0) : new Source(0, 0, 0.6);
            }
            case AFFRICATE -> within < 0.4 ? new Source(0.08 * hum, 0, 0) : new Source(0.45 * hum, p.noiseLevel, 0);
        };
    }
    // --8<-- [end:source]

    // --8<-- [start:pitch]
    /**
     * The melody of the phrase: the voice starts a little high and sinks as the breath runs out; each stressed
     * vowel rises in a small hump; a call starts higher with bigger humps and falls at the end, a question
     * rises at the end.
     */
    private static double pitch(double base, Tune tune, List<Segment> segments, int[] starts, int segment,
                                int sample, int length) {
        double along = sample / (double) length;
        double high = tune == Tune.CALL ? 1.18 : 1.08;
        double hz = base * (high - 0.22 * along);
        if (segment < segments.size()) {
            Segment s = segments.get(segment);
            if (s.stressed()) {
                double within = (sample - starts[segment]) / (double) s.samples();
                hz += base * (tune == Tune.CALL ? 0.3 : 0.16) * Math.sin(Math.PI * within);
            }
        }
        if (tune == Tune.QUESTION && along > 0.7) hz += base * 0.6 * (along - 0.7) / 0.3;
        hz *= 1 + 0.006 * Math.sin(2 * Math.PI * 4.5 * sample / Sound.RATE);   // a little vibrato, as alive
        return hz;
    }
    // --8<-- [end:pitch]

    // ---- the parts ----------------------------------------------------------------------------------

    // --8<-- [start:resonator]
    /**
     * One resonance of the mouth: a filter that rings at a frequency with a bandwidth, the two-pole
     * resonator of Dennis Klatt's synthesizer of 1980. Each output is the input plus the last two outputs,
     * weighted so that a tone at the frequency builds up and others die away.
     */
    static final class Resonator {
        private double a = 1, b, c;
        private double y1, y2;

        void tune(double hz, double bandwidth) {
            double t = 1.0 / Sound.RATE;
            c = -Math.exp(-2 * Math.PI * bandwidth * t);
            b = 2 * Math.exp(-Math.PI * bandwidth * t) * Math.cos(2 * Math.PI * hz * t);
            a = 1 - b - c;
        }

        double next(double x) {
            double y = a * x + b * y1 + c * y2;
            y2 = y1;
            y1 = y;
            return y;
        }
    }
    // --8<-- [end:resonator]

    /** A band of noise around a centre frequency at the same loudness whatever its width: the hiss of an s or an sh. */
    static final class BandPass {
        private double b0, b2, a1, a2;
        private double x1, x2, y1, y2;
        private double tunedTo = -1, tunedWidth = -1;

        void tune(double hz, double bandwidth) {
            if (hz == tunedTo && bandwidth == tunedWidth) return;
            tunedTo = hz;
            tunedWidth = bandwidth;
            double w = 2 * Math.PI * Math.min(hz, Sound.RATE * 0.45) / Sound.RATE;
            double q = hz / bandwidth;
            double alpha = Math.sin(w) / (2 * q);
            double a0 = 1 + alpha;
            b0 = alpha / a0;
            b2 = -alpha / a0;
            a1 = -2 * Math.cos(w) / a0;
            a2 = (1 - alpha) / a0;
        }

        double next(double x) {
            double y = b0 * x + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1;
            x1 = x;
            y2 = y1;
            y1 = y;
            return y;
        }
    }

    private static double random(long seed) {
        seed ^= seed << 13;
        seed ^= seed >>> 7;
        seed ^= seed << 17;
        return (seed >>> 11) / (double) (1L << 53);
    }

    /** Scaled so the loudest moment is at {@code volume}, without the slow drift of a resonator, faded in and out. */
    private static Sound normalized(double[] out, double volume) {
        double last = 0, kept = 0;
        for (int i = 0; i < out.length; i++) {       // a gentle high-pass against a drifting zero line
            double x = out[i];
            kept = 0.995 * (kept + x - last);
            last = x;
            out[i] = kept;
        }
        double peak = 1e-9;
        for (double v : out) peak = Math.max(peak, Math.abs(v));
        int fade = Sound.RATE / 200;
        short[] samples = new short[out.length];
        for (int i = 0; i < out.length; i++) {
            double envelope = Math.min(1, Math.min(i, out.length - 1 - i) / (double) fade);
            samples[i] = (short) Math.round(out[i] / peak * volume * envelope * Short.MAX_VALUE);
        }
        return Sound.of(samples);
    }
}
