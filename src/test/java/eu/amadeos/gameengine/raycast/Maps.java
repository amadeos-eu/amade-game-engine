package eu.amadeos.gameengine.raycast;

/** Small maps for the tests, drawn as text: {@code #} wall, {@code |} and {@code -} doors, anything else open. */
final class Maps {
    static final Texture WALL = solid(0xff808080);
    static final Texture DOOR = solid(0xff806040);
    static final Texture FRAME = solid(0xff404040);
    static final Texture FLOOR = solid(0xff606060);

    private Maps() {
    }

    static Texture solid(int argb) {
        int[] texels = new int[16 * 16];
        java.util.Arrays.fill(texels, argb);
        return Texture.of(16, texels, null);
    }

    static GridMap of(String... rows) {
        GridMap map = new GridMap(rows[0].length(), rows.length);
        for (int y = 0; y < rows.length; y++) for (int x = 0; x < rows[y].length(); x++) {
            char c = rows[y].charAt(x);
            if (c == '#') map.setWall(x, y, WALL);
            else {
                map.setFloor(x, y, FLOOR);
                map.setCeiling(x, y, FLOOR);
                if (c == '-' || c == '|') map.addDoor(new Door(x, y, c == '-', DOOR, FRAME));
            }
        }
        return map;
    }
}
