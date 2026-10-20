package eu.amadeos.gameengine.ai;

import eu.amadeos.gameengine.tile.Cell;

import java.util.Arrays;

/**
 * How strong a side is at every place of the map: an <b>influence map</b>, the computer opponent's picture
 * of the battle. Every unit and building spreads its strength around it, full on its own tile and less
 * further away, nothing beyond its reach. Added up, the map shows where a side is strong and where it is
 * weak, without looking at single units.
 *
 * <p>With two of them, one per side, the questions of an opponent become arithmetic: where is the enemy
 * strongest (the value at its largest)? Which of the enemy's buildings is defended worst (the building
 * with the least enemy strength around it)? Is this spice field safe for a harvester (no enemy strength
 * there)? Game AI has used such maps since the strategy games of the nineties; they are cheap, and what
 * they answer is easy to see when they are drawn.
 *
 * <pre>
 * enemy.clear();
 * for (Unit unit : player.units()) enemy.add(unit.col(), unit.row(), unit.strength(), 6);
 * Cell weakest = enemy.weakest(targets);   // where to attack
 * </pre>
 */
// --8<-- [start:influence]
public final class InfluenceMap {
    private final int width;
    private final int height;
    private final double[] values;

    public InfluenceMap(int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("empty map");
        this.width = width;
        this.height = height;
        this.values = new double[width * height];
    }

    public void clear() {
        Arrays.fill(values, 0);
    }

    /**
     * Spreads {@code strength} around a tile: all of it there, falling in a straight line to nothing at
     * {@code reach} tiles, as the crow flies.
     */
    public void add(int col, int row, double strength, int reach) {
        for (int r = Math.max(0, row - reach); r <= Math.min(height - 1, row + reach); r++) {
            for (int c = Math.max(0, col - reach); c <= Math.min(width - 1, col + reach); c++) {
                double distance = Math.hypot(c - col, r - row);
                if (distance >= reach + 1) continue;
                values[r * width + c] += strength * (1 - distance / (reach + 1));
            }
        }
    }

    /** The strength at a tile; 0 outside the map. */
    public double at(int col, int row) {
        return col >= 0 && row >= 0 && col < width && row < height ? values[row * width + col] : 0;
    }

    /** The tile of the greatest strength, the first of them from the top left; {@code null} if all are 0. */
    public Cell strongest() {
        int best = -1;
        for (int i = 0; i < values.length; i++) if (values[i] > 0 && (best < 0 || values[i] > values[best])) best = i;
        return best < 0 ? null : new Cell(best % width, best / width);
    }

    /** Of {@code candidates}, the one with the least strength on it; the first of them on a tie. */
    public Cell weakest(Iterable<Cell> candidates) {
        Cell best = null;
        double least = Double.MAX_VALUE;
        for (Cell cell : candidates) {
            double value = at(cell.col(), cell.row());
            if (value < least) {
                least = value;
                best = cell;
            }
        }
        return best;
    }
    // --8<-- [end:influence]

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
