package eu.amadeos.gameengine.ai;

/**
 * Finds the angle and the power of a shot that hits: the aim of a computer player in an artillery duel, a
 * thrower, a golfer. It knows nothing about shells; it is given a <b>trial</b> that fires a shot in
 * thought (with {@link eu.amadeos.gameengine.physics.Ballistics#fly}, say) and answers how far from the
 * target it lands. It searches for the aim with the smallest miss.
 *
 * <p>The search tries a coarse grid first, every few degrees and every few points of power, because a
 * landscape makes the miss jump: a hill in the way, a shot that falls short of it and one that just
 * clears it are far apart. Then it looks closer around the best aim, again and again with half the steps.
 * A few hundred trials find a hit wherever one exists; each takes microseconds.
 *
 * <pre>
 * ShotSolver.Aim aim = ShotSolver.solve(95, 175, 4, 20, 100, 5, 4,
 *         (angle, power) -> distanceFromTarget(air.fly(launch(angle, power), STEP, 6, field, solid)));
 * </pre>
 */
// --8<-- [start:solver]
public final class ShotSolver {
    /** A shot in thought: how far from the target it lands, 0 for a hit in the middle. */
    @FunctionalInterface
    public interface Trial {
        double miss(double angle, double power);
    }

    /** The best aim found, its miss and how many trials the search took. */
    public record Aim(double angle, double power, double miss, int trials) {
    }

    private ShotSolver() {
    }

    /**
     * Searches angles from {@code angleFrom} to {@code angleTo} in steps of {@code angleStep} and powers from
     * {@code powerFrom} to {@code powerTo} in steps of {@code powerStep}, then refines the best {@code rounds}
     * times, each time around it with half the steps. Stays within the ranges.
     */
    public static Aim solve(double angleFrom, double angleTo, double angleStep, double powerFrom, double powerTo,
                            double powerStep, int rounds, Trial trial) {
        if (angleTo < angleFrom || powerTo < powerFrom || angleStep <= 0 || powerStep <= 0) {
            throw new IllegalArgumentException("empty search");
        }
        double bestAngle = angleFrom, bestPower = powerFrom, bestMiss = Double.POSITIVE_INFINITY;
        int trials = 0;
        for (double a = angleFrom; a <= angleTo + 1e-9; a += angleStep) {
            for (double p = powerFrom; p <= powerTo + 1e-9; p += powerStep) {
                double miss = trial.miss(a, p);
                trials++;
                if (miss < bestMiss) {
                    bestMiss = miss;
                    bestAngle = a;
                    bestPower = p;
                }
            }
        }
        double da = angleStep, dp = powerStep;
        for (int round = 0; round < rounds && bestMiss > 0; round++) {
            da /= 2;
            dp /= 2;
            double centreAngle = bestAngle, centrePower = bestPower;
            for (int i = -2; i <= 2; i++) {
                for (int j = -2; j <= 2; j++) {
                    if (i == 0 && j == 0) continue;
                    double a = clamp(centreAngle + i * da, angleFrom, angleTo);
                    double p = clamp(centrePower + j * dp, powerFrom, powerTo);
                    double miss = trial.miss(a, p);
                    trials++;
                    if (miss < bestMiss) {
                        bestMiss = miss;
                        bestAngle = a;
                        bestPower = p;
                    }
                }
            }
        }
        return new Aim(bestAngle, bestPower, bestMiss, trials);
    }
    // --8<-- [end:solver]

    private static double clamp(double value, double from, double to) {
        return Math.max(from, Math.min(to, value));
    }
}
