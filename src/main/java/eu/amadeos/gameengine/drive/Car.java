package eu.amadeos.gameengine.drive;

/**
 * A vehicle on the {@link Road}: the player's car, a truck to overtake, a car coming the other way, a
 * rival in a race. {@code z} is where its back is along the road, {@code x} where its middle is across it in
 * road half-widths (the right lane at +0.5, the left at -0.5), {@code speed} in world units per second.
 * Cars driving the other way have {@code direction} -1. {@link Traffic} moves the cars that drive themselves;
 * a car the player steers is moved by {@link Driving} and only taken into account by the others.
 */
// --8<-- [start:car]
public final class Car {
    public RoadSprite sprite;
    /** A picture of only its lights, drawn over the car without fog or night while {@link #lights} is on. */
    public RoadSprite glowSprite;
    public double z;
    public double x;
    public double speed;
    /** +1 along the road, -1 against it. */
    public int direction = 1;
    /** The speed it likes to drive at, when nothing is in its way. */
    public double cruise;
    /** Width in road half-widths and length in world units, for the gaps between cars and for bumps. */
    public double width = 0.46;
    public double length = 900;
    /** Overtakes slower cars when the other lane is clear. */
    public boolean overtakes;
    /** Steered by a player: {@link Traffic} does not move it. */
    public boolean steered;
    /** Its lights are on: tail lights glow at night. */
    public boolean lights;
    /** Where it is heading across the road, e.g. the other lane while it overtakes. */
    public double targetX;
    /** The lane it keeps to when not overtaking. */
    public double homeX;
    /** Height above the road in world units: a bird in flight, a balloon. 0 for anything on wheels or legs. */
    public double lift;
    /** Drawn mirrored: an animal walking to the left, a train going the other way. */
    public boolean flipped;
    /** Anything the game wants to remember about the car. */
    public Object tag;
    // --8<-- [end:car]

    public Car(RoadSprite sprite, double z, double x, double speed) {
        this.sprite = sprite;
        this.z = z;
        this.x = x;
        this.targetX = x;
        this.homeX = x;
        this.speed = speed;
        this.cruise = speed;
    }

    /** A car driving the other way in the left lane. */
    public Car oncoming() {
        direction = -1;
        return this;
    }

    public Car overtaking() {
        overtakes = true;
        return this;
    }

    /** Whether this car and {@code other} overlap: they touch. */
    public boolean touches(Car other) {
        return Math.abs(z - other.z) < (length + other.length) / 2 && Math.abs(x - other.x) < (width + other.width) / 2;
    }
}
