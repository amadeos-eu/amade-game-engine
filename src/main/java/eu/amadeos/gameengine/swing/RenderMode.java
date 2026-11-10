package eu.amadeos.gameengine.swing;

import java.util.Locale;
import java.util.Optional;

/** How {@link GameCanvas} brings the game's logical picture to the screen. */
// --8<-- [start:modes]
public enum RenderMode {
    /**
     * Everything is drawn at the full size of the screen, through one scaling transform. Sharp, but every
     * shape and image costs as many pixels as it covers on the screen: on a 4K screen about 30 times as
     * many as at 640 × 400.
     */
    DIRECT,
    /**
     * The world is drawn at the logical size itself, then enlarged by a whole factor with hard pixel edges:
     * the look of old games. Drawing costs 640 × 400 pixels on any screen; black bars fill what is left.
     */
    PIXEL,
    /**
     * The world is drawn into a picture in main memory and copied onto the screen in one piece: at the full
     * size of the screen while the frames are in time, at half or a third of it, enlarged, when they are
     * not ({@link AutoResolution}). For a game that paints every pixel many times over, such as a road with
     * hundreds of trees, drawing in memory is much faster than drawing on the screen's own surface.
     */
    AUTO;
    // --8<-- [end:modes]

    /** The name in settings files, e.g. {@code pixel}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<RenderMode> ofId(String id) {
        for (RenderMode mode : values()) if (mode.id().equals(id)) return Optional.of(mode);
        return Optional.empty();
    }
}
