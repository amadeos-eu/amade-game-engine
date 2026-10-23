package eu.amadeos.gameengine.audio;

import java.util.Locale;
import java.util.Optional;

/**
 * The sounds {@link Speech} can make, named as in the ARPAbet that American speech research has written
 * since the 1970s: {@code k ae b} is "cab", {@code p l iy z} is "please". Each one says how the voice makes
 * it: whether the vocal folds hum, whether air hisses through a narrow place, and where the mouth puts its
 * resonances, the <i>formants</i> F1, F2 and F3 (in Hz, for a man's voice; women's and children's are
 * higher). The vowels are told apart by their first two formants alone: F1 is high when the mouth is open
 * (<i>aa</i>), F2 is high when the tongue is in front (<i>iy</i>).
 *
 * <p>The numbers are textbook values, measured on many speakers since the 1950s. A consonant's formants
 * are its <i>locus</i>: where the formants of the vowel next to it seem to come from, so that the ear hears
 * a <i>b</i> where they rise from low and a <i>d</i> where F2 comes from about 1700 Hz.
 */
// --8<-- [start:phoneme]
public enum Phoneme {
    // vowels: formants, length in milliseconds
    IY(Kind.VOWEL, 110, 270, 2290, 3010),       // beet
    IH(Kind.VOWEL, 80, 390, 1990, 2550),        // bit
    EH(Kind.VOWEL, 90, 530, 1840, 2480),        // bet
    AE(Kind.VOWEL, 130, 660, 1720, 2410),       // bat
    AA(Kind.VOWEL, 130, 730, 1090, 2440),       // father
    AH(Kind.VOWEL, 90, 640, 1190, 2390),        // but
    AO(Kind.VOWEL, 130, 570, 840, 2410),        // law
    UH(Kind.VOWEL, 80, 440, 1020, 2240),        // book
    UW(Kind.VOWEL, 110, 300, 870, 2240),        // boot
    ER(Kind.VOWEL, 120, 490, 1350, 1690),       // bird
    AX(Kind.VOWEL, 60, 500, 1500, 2500),        // the a in "about"
    // diphthongs: they glide from the first vowel to the second
    EY(Kind.VOWEL, 150, 480, 1720, 2520, 330, 2200, 2600),   // bait
    AY(Kind.VOWEL, 170, 660, 1200, 2550, 400, 1900, 2550),   // bite
    OW(Kind.VOWEL, 150, 540, 1100, 2300, 450, 900, 2300),    // boat
    AW(Kind.VOWEL, 170, 640, 1230, 2550, 420, 940, 2350),    // bout
    OY(Kind.VOWEL, 170, 550, 960, 2400, 360, 1820, 2450),    // boy
    // glides and liquids: voiced, like short vowels
    W(Kind.GLIDE, 60, 290, 610, 2150),
    Y(Kind.GLIDE, 60, 260, 2070, 3020),
    L(Kind.GLIDE, 70, 310, 1050, 2880),
    R(Kind.GLIDE, 70, 310, 1060, 1380),
    // nasals: voiced, quieter, the sound goes out through the nose
    M(Kind.NASAL, 70, 480, 1270, 2130),
    N(Kind.NASAL, 65, 480, 1340, 2470),
    NG(Kind.NASAL, 70, 480, 2000, 2900),
    // fricatives: noise from a narrow place, around a centre frequency; the voiced ones hum as well
    F(Kind.FRICATIVE, 100, 340, 1100, 2080, 6000, 6000, 0.25, false),
    V(Kind.FRICATIVE, 70, 220, 1100, 2080, 6000, 6000, 0.18, true),
    TH(Kind.FRICATIVE, 100, 320, 1290, 2540, 5000, 5000, 0.22, false),
    DH(Kind.FRICATIVE, 50, 270, 1290, 2540, 5000, 5000, 0.15, true),
    S(Kind.FRICATIVE, 110, 320, 1390, 2530, 6200, 2400, 1.0, false),
    Z(Kind.FRICATIVE, 80, 240, 1390, 2530, 6200, 2400, 0.6, true),
    SH(Kind.FRICATIVE, 110, 300, 1840, 2750, 3000, 1600, 0.9, false),
    ZH(Kind.FRICATIVE, 80, 300, 1840, 2750, 3000, 1600, 0.55, true),
    /** Breath through an open mouth: it takes the formants of the vowel after it. */
    HH(Kind.ASPIRATE, 90, 0, 0, 0),
    // stops: the mouth closes, opens with a burst, and the unvoiced ones breathe out before the vowel
    P(Kind.STOP, 90, 200, 800, 2200, 1200, 2400, 0.35, false),
    B(Kind.STOP, 70, 200, 800, 2200, 1200, 2400, 0.25, true),
    T(Kind.STOP, 90, 200, 1700, 2600, 4200, 3000, 0.7, false),
    D(Kind.STOP, 70, 200, 1700, 2600, 4200, 3000, 0.45, true),
    K(Kind.STOP, 95, 200, 1900, 2100, 2300, 1400, 0.7, false),
    G(Kind.STOP, 75, 200, 1900, 2100, 2300, 1400, 0.45, true),
    // affricates: a stop that opens into a fricative
    CH(Kind.AFFRICATE, 130, 300, 1840, 2750, 3000, 1600, 0.9, false),
    JH(Kind.AFFRICATE, 110, 300, 1840, 2750, 3000, 1600, 0.55, true);

    /** How the voice makes a phoneme. */
    public enum Kind { VOWEL, GLIDE, NASAL, FRICATIVE, ASPIRATE, STOP, AFFRICATE }

    public final Kind kind;
    /** Length in milliseconds at the normal pace, unstressed. */
    public final int millis;
    /** The formants, or the locus of a consonant; for a diphthong where it starts. */
    public final int f1, f2, f3;
    /** Where a diphthong ends; the same as the start for all others. */
    public final int g1, g2, g3;
    /** The centre and the width of the noise band of a fricative, a stop's burst and an affricate, in Hz. */
    public final int noise, noiseWidth;
    /** How loud the noise is against the voice, 0 to 1. */
    public final double noiseLevel;
    /** Whether the vocal folds hum while it is made. */
    public final boolean voiced;

    Phoneme(Kind kind, int millis, int f1, int f2, int f3) {
        this(kind, millis, f1, f2, f3, f1, f2, f3, 0, 0, 0, kind != Kind.ASPIRATE);
    }

    Phoneme(Kind kind, int millis, int f1, int f2, int f3, int g1, int g2, int g3) {
        this(kind, millis, f1, f2, f3, g1, g2, g3, 0, 0, 0, true);
    }

    Phoneme(Kind kind, int millis, int f1, int f2, int f3, int noise, int noiseWidth, double noiseLevel,
            boolean voiced) {
        this(kind, millis, f1, f2, f3, f1, f2, f3, noise, noiseWidth, noiseLevel, voiced);
    }

    Phoneme(Kind kind, int millis, int f1, int f2, int f3, int g1, int g2, int g3, int noise, int noiseWidth,
            double noiseLevel, boolean voiced) {
        this.kind = kind;
        this.millis = millis;
        this.f1 = f1;
        this.f2 = f2;
        this.f3 = f3;
        this.g1 = g1;
        this.g2 = g2;
        this.g3 = g3;
        this.noise = noise;
        this.noiseWidth = noiseWidth;
        this.noiseLevel = noiseLevel;
        this.voiced = voiced;
    }
    // --8<-- [end:phoneme]

    public boolean isVowel() {
        return kind == Kind.VOWEL;
    }

    /** True for the vowels that glide from one sound to another. */
    public boolean isDiphthong() {
        return f1 != g1 || f2 != g2 || f3 != g3;
    }

    /** The phoneme of an ARPAbet name in any case, "ae" or "AE"; empty for a name that is none. */
    public static Optional<Phoneme> named(String name) {
        try {
            return Optional.of(valueOf(name.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
