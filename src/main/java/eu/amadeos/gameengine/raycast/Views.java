package eu.amadeos.gameengine.raycast;

/**
 * The pictures of a thing for a {@link Billboard}: one per direction it can be seen from (1, 8 or 16) and
 * per animation frame. Direction 0 is the thing seen from the front; the next ones go round it the way map
 * angles grow, so a quarter turn on, the viewer sees the thing's right side.
 */
// --8<-- [start:views]
public final class Views {
    private final Texture[][] textures;   // [frame][direction]

    public Views(Texture[][] textures) {
        this.textures = textures;
    }

    /** A thing that looks the same from all sides and does not move, such as a pickup. */
    public static Views of(Texture texture) {
        return new Views(new Texture[][]{{texture}});
    }

    public int frames() {
        return textures.length;
    }

    public int directions() {
        return textures[0].length;
    }

    /**
     * The picture of frame {@code frame} for a thing facing {@code facing} seen by an eye at angle
     * {@code fromViewer} (the angle from the thing to the eye).
     */
    public Texture pick(int frame, double facing, double fromViewer) {
        Texture[] row = textures[Math.floorMod(frame, textures.length)];
        int n = row.length;
        if (n == 1) return row[0];
        double turn = (fromViewer - facing) / (2 * Math.PI);
        return row[Math.floorMod((int) Math.round(turn * n), n)];
    }
}
// --8<-- [end:views]
