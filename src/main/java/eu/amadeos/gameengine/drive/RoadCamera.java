package eu.amadeos.gameengine.drive;

/**
 * Where the eye is: {@code z} along the road, {@code x} sideways in road half-widths (0 the centre, the right
 * lane at 0.5), {@code height} world units above the road. {@code depth} is the projection's zoom, 1 divided
 * by the tangent of half the field of view: 1.2 sees about 80° across. The horizon of a flat road lies at
 * {@code horizon} times the view's height from its top; a little above the middle shows more road.
 */
// --8<-- [start:camera]
public final class RoadCamera {
    public double z;
    public double x;
    public double height = 760;
    public double depth = 1.2;
    public double horizon = 0.42;
    /** How many segments are drawn ahead. */
    public int drawDistance = 260;

    /** The camera this far behind and above a car, looking along the road. */
    public void follow(Car car, double behind) {
        z = car.z - behind;
        x = car.x;
    }
}
// --8<-- [end:camera]
