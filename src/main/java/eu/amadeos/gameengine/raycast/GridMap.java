package eu.amadeos.gameengine.raycast;

import eu.amadeos.gameengine.path.Passable;

/**
 * The world of a {@link Raycaster}: a grid of square cells, each one unit wide and one unit high. A cell is
 * either a wall, with a texture on all four sides, or open, with a floor and a ceiling texture; an open cell
 * can hold a {@link Door}. A ceiling of {@code null} is open sky.
 *
 * <p>The map also answers the questions every game on it asks: what does a ray hit ({@link #cast}), does a
 * body fit here ({@link #blocks}), can one point see another ({@link #sees}).
 */
// --8<-- [start:gridmap]
public final class GridMap {
    /** What a ray hit; filled by {@link #cast}, reused to spare the garbage collector. */
    public static final class Ray {
        /** The distance along the ray's direction, in lengths of the direction vector; infinite if nothing was hit. */
        public double distance;
        public int cellX;
        public int cellY;
        /** 0 if the ray hit a side facing west or east, 1 if one facing north or south. */
        public int side;
        /** Where on the face the ray hit, 0 to 1 from the face's left edge as seen by the ray. */
        public double u;
        /** The texture the ray hit; {@code null} at the edge of the map. */
        public Texture texture;
        /** The door the ray hit, or {@code null}. */
        public Door door;

        public boolean hit() {
            return distance != Double.POSITIVE_INFINITY;
        }
    }

    private final int width;
    private final int height;
    private final Texture[] walls;
    private final Texture[] floors;
    private final Texture[] ceilings;
    private final Door[] doors;

    public GridMap(int width, int height) {
        this.width = width;
        this.height = height;
        walls = new Texture[width * height];
        floors = new Texture[width * height];
        ceilings = new Texture[width * height];
        doors = new Door[width * height];
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean contains(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    /** Makes the cell a wall; {@code null} opens it. */
    public void setWall(int x, int y, Texture texture) {
        walls[y * width + x] = texture;
    }

    public void setFloor(int x, int y, Texture texture) {
        floors[y * width + x] = texture;
    }

    /** {@code null} shows the sky above the cell. */
    public void setCeiling(int x, int y, Texture texture) {
        ceilings[y * width + x] = texture;
    }

    /** Puts a door into an open cell. */
    public Door addDoor(Door door) {
        doors[door.y() * width + door.x()] = door;
        return door;
    }

    /** The wall texture of the cell; {@code null} if it is open. */
    public Texture wall(int x, int y) {
        return contains(x, y) ? walls[y * width + x] : null;
    }

    public Texture floor(int x, int y) {
        return contains(x, y) ? floors[y * width + x] : null;
    }

    public Texture ceiling(int x, int y) {
        return contains(x, y) ? ceilings[y * width + x] : null;
    }

    public Door door(int x, int y) {
        return contains(x, y) ? doors[y * width + x] : null;
    }

    /** True for a wall, a door that is not open, and everything outside the map: no body fits there. */
    public boolean blocks(int x, int y) {
        if (!contains(x, y)) return true;
        int i = y * width + x;
        return walls[i] != null || doors[i] != null && !doors[i].passable();
    }

    /** True for a wall, a closed door and outside the map. */
    public boolean blocksSight(int x, int y) {
        if (!contains(x, y)) return true;
        int i = y * width + x;
        return walls[i] != null || doors[i] != null && doors[i].openness() < 0.5;
    }

    /** The cells a body can walk through, for {@link eu.amadeos.gameengine.path.DistanceMap} and friends. */
    public Passable passable() {
        return (x, y) -> !blocks(x, y);
    }

    /** Moves all doors on; {@code occupied} says whether something stands in a cell. */
    public void updateDoors(double dt, Passable occupied) {
        for (Door door : doors) if (door != null) door.update(dt, occupied.test(door.x(), door.y()));
    }

    /** True if nothing blocks the line from one point to the other. */
    public boolean sees(double x0, double y0, double x1, double y1) {
        double dx = x1 - x0, dy = y1 - y0, length = Math.hypot(dx, dy);
        if (length < 1e-9) return true;
        Ray ray = new Ray();
        cast(x0, y0, dx / length, dy / length, length, ray);
        return !ray.hit() || ray.distance >= length - 1e-6;
    }

    // --8<-- [start:cast]
    /**
     * Follows a ray from ({@code x}, {@code y}) in direction ({@code dx}, {@code dy}) from cell border to cell
     * border (the DDA of Amanatides and Woo) until it hits a wall or a closed part of a door, or has gone
     * farther than {@code max}. The distance is measured in lengths of the direction: for a unit vector it
     * is the real distance, for the {@link Raycaster}'s rays it is the distance straight ahead of the camera.
     */
    public void cast(double x, double y, double dx, double dy, double max, Ray out) {
        out.distance = Double.POSITIVE_INFINITY;
        out.texture = null;
        out.door = null;
        int mx = (int) Math.floor(x), my = (int) Math.floor(y);
        double ddx = dx == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dx);
        double ddy = dy == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dy);
        int stepX = dx < 0 ? -1 : 1, stepY = dy < 0 ? -1 : 1;
        double sideX = dx < 0 ? (x - mx) * ddx : (mx + 1 - x) * ddx;
        double sideY = dy < 0 ? (y - my) * ddy : (my + 1 - y) * ddy;
        Door previous = contains(mx, my) ? doors[my * width + mx] : null;
        if (previous != null && hitsDoor(previous, x, y, dx, dy, out)) return;
        while (true) {
            int side;
            double distance;
            if (sideX < sideY) { distance = sideX; sideX += ddx; mx += stepX; side = 0; }
            else { distance = sideY; sideY += ddy; my += stepY; side = 1; }
            if (distance > max) return;
            if (!contains(mx, my)) {
                out.distance = distance;
                out.cellX = mx;
                out.cellY = my;
                out.side = side;
                out.u = 0;
                return;
            }
            int i = my * width + mx;
            Door door = doors[i];
            if (door != null) {
                if (hitsDoor(door, x, y, dx, dy, out)) return;
                previous = door;
                continue;
            }
            Texture wall = walls[i];
            if (wall != null) {
                double along = side == 0 ? y + distance * dy : x + distance * dx;
                double u = along - Math.floor(along);
                if (side == 0 && dx < 0 || side == 1 && dy > 0) u = 1 - u;
                // In the opening of a door, the sides of the walls next to it show the door frame.
                boolean jamb = previous != null && previous.frame() != null && (previous.alongX() ? side == 0 : side == 1);
                out.distance = distance;
                out.cellX = mx;
                out.cellY = my;
                out.side = side;
                out.u = Math.min(u, 0.999999);
                out.texture = jamb ? previous.frame() : wall;
                return;
            }
            previous = null;
        }
    }

    /** Does the ray hit the closed part of the door, on the plane through the middle of its cell? */
    private static boolean hitsDoor(Door door, double x, double y, double dx, double dy, Ray out) {
        double t, along;
        int cell;
        if (door.alongX()) {
            if (dy == 0) return false;
            t = (door.y() + 0.5 - y) / dy;
            along = x + t * dx;
            cell = door.x();
        } else {
            if (dx == 0) return false;
            t = (door.x() + 0.5 - x) / dx;
            along = y + t * dy;
            cell = door.y();
        }
        if (t <= 0 || along < cell || along >= cell + 1) return false;
        double u = along - cell;
        if (u < door.openness()) return false;     // the door has slid away there: the ray passes
        out.distance = t;
        out.cellX = door.x();
        out.cellY = door.y();
        out.side = door.alongX() ? 1 : 0;
        out.u = Math.min(u - door.openness(), 0.999999);
        out.texture = door.face();
        out.door = door;
        return true;
    }
    // --8<-- [end:cast]
}
// --8<-- [end:gridmap]
