package eu.amadeos.gameengine.tiled;

/**
 * A thing placed on an object layer in Tiled: where the player starts, an enemy, a door, the goal. Its
 * {@code type} is what Tiled calls the object's class (or type, in older versions): the word the game
 * decides by. Positions and sizes are in the map's pixels, the top left corner of the box; for an object
 * that shows a tile ({@code gid} not 0), Tiled stores the bottom left corner, and this record has already
 * turned it into the top left, so all objects are alike.
 *
 * @param gid the tile the object shows, without flip bits; 0 for a plain rectangle or point
 */
public record TiledObject(int id, String name, String type, double x, double y, double width, double height,
                          int gid, TiledProperties properties) {
    public double centerX() {
        return x + width / 2;
    }

    public double centerY() {
        return y + height / 2;
    }

    public double right() {
        return x + width;
    }

    public double bottom() {
        return y + height;
    }
}
