package eu.amadeos.gameengine.drive;

import java.awt.Color;

/**
 * The colours of the road and the ground in one stretch of landscape: grass, asphalt, gravel shoulder,
 * optional red and white kerbs, the lines, water and beach, and the fog the distance fades into. Grass and
 * asphalt come in two shades that change every three segments: the stripes that show the speed, as in the
 * old road games. Two looks can be {@linkplain #mix mixed}, so one landscape melts into the next.
 *
 * <p>{@code fogDistance} is how far one sees: after that distance two thirds of a colour are fog.
 */
// --8<-- [start:look]
public record RoadLook(Color grass, Color grassDark, Color asphalt, Color asphaltDark, Color shoulder,
                       Color kerb, Color kerbDark, Color edgeLine, Color centreLine, boolean glowingLines,
                       Color water, Color waterDark, Color beach, Color fog, double fogDistance) {

    /** A look with grey asphalt, white lines, a gravel shoulder, no kerbs, and water and beach colours. */
    public static RoadLook of(Color grass, Color grassDark, Color fog, double fogDistance) {
        return new RoadLook(grass, grassDark, new Color(104, 102, 104), new Color(100, 98, 100), new Color(150, 138, 110),
                null, null, new Color(236, 236, 228), new Color(236, 236, 228), false,
                new Color(96, 150, 190), new Color(92, 146, 186), new Color(226, 206, 160), fog, fogDistance);
    }
    // --8<-- [end:look]

    public RoadLook withAsphalt(Color light, Color dark) {
        return new RoadLook(grass, grassDark, light, dark, shoulder, kerb, kerbDark, edgeLine, centreLine, glowingLines,
                water, waterDark, beach, fog, fogDistance);
    }

    public RoadLook withShoulder(Color c) {
        return new RoadLook(grass, grassDark, asphalt, asphaltDark, c, kerb, kerbDark, edgeLine, centreLine, glowingLines,
                water, waterDark, beach, fog, fogDistance);
    }

    /** Red and white kerbs along the edges, as on a race track; {@code null} for none. */
    public RoadLook withKerbs(Color a, Color b) {
        return new RoadLook(grass, grassDark, asphalt, asphaltDark, shoulder, a, b, edgeLine, centreLine, glowingLines,
                water, waterDark, beach, fog, fogDistance);
    }

    /** Line colours; glowing lines keep their colour in fog and at night, with cat's eyes along the edges. */
    public RoadLook withLines(Color edge, Color centre, boolean glowing) {
        return new RoadLook(grass, grassDark, asphalt, asphaltDark, shoulder, kerb, kerbDark, edge, centre, glowing,
                water, waterDark, beach, fog, fogDistance);
    }

    public RoadLook withWater(Color light, Color dark, Color beach) {
        return new RoadLook(grass, grassDark, asphalt, asphaltDark, shoulder, kerb, kerbDark, edgeLine, centreLine,
                glowingLines, light, dark, beach, fog, fogDistance);
    }

    public RoadLook withFog(Color fog, double distance) {
        return new RoadLook(grass, grassDark, asphalt, asphaltDark, shoulder, kerb, kerbDark, edgeLine, centreLine,
                glowingLines, water, waterDark, beach, fog, distance);
    }

    // --8<-- [start:mix]
    /** Between this look ({@code t} = 0) and {@code other} ({@code t} = 1). */
    public RoadLook mix(RoadLook other, double t) {
        if (t <= 0) return this;
        if (t >= 1) return other;
        return new RoadLook(mix(grass, other.grass, t), mix(grassDark, other.grassDark, t), mix(asphalt, other.asphalt, t),
                mix(asphaltDark, other.asphaltDark, t), mix(shoulder, other.shoulder, t),
                t < 0.5 ? kerb : other.kerb, t < 0.5 ? kerbDark : other.kerbDark,
                mix(edgeLine, other.edgeLine, t), mix(centreLine, other.centreLine, t), t < 0.5 ? glowingLines : other.glowingLines,
                mix(water, other.water, t), mix(waterDark, other.waterDark, t), mix(beach, other.beach, t),
                mix(fog, other.fog, t), fogDistance + (other.fogDistance - fogDistance) * t);
    }
    // --8<-- [end:mix]

    /** Between two colours; {@code null} if either is. */
    public static Color mix(Color a, Color b, double t) {
        if (a == null || b == null) return t < 0.5 ? a : b;
        t = Math.max(0, Math.min(1, t));
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int) Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }
}
