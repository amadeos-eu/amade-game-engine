package eu.amadeos.gameengine.anim;

/**
 * How an animation moves between start and end: maps the elapsed share of its time, 0 to 1, to the share of
 * the way covered, 0 to 1. Linear movement looks mechanical; things in the real world speed up and slow
 * down.
 */
// --8<-- [start:easing]
@FunctionalInterface
public interface Easing {
    double apply(double t);

    /** Constant speed. */
    Easing LINEAR = t -> t;

    /** Fast start, gentle stop: for things that are thrown and land, like a card on its pile. */
    Easing EASE_OUT = t -> 1 - Math.pow(1 - t, 3);

    /** Gentle start and stop: for things that are carried. */
    Easing EASE_IN_OUT = t -> t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
}
// --8<-- [end:easing]
