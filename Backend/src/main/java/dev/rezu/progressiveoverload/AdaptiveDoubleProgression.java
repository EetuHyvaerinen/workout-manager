package dev.rezu.progressiveoverload;


import dev.rezu.workout.Exercise;

public class AdaptiveDoubleProgression implements ProgressionStrategy {

    private static final double OVERSHOOT_RPE  = 9.5;
    private static final double UNDERSHOOT_RPE = 6.0;

    private final ProgressionConfig config;

    public AdaptiveDoubleProgression() {
        this(ProgressionConfig.strength());
    }

    public AdaptiveDoubleProgression(ProgressionConfig config) {
        this.config = config;
    }

    @Override
    public Exercise calculateNext(Exercise actual) {
        if (actual.rpe() > 0.0) {
            if (actual.rpe() >= OVERSHOOT_RPE) {
                return actual.withRepsAndWeight(actual.repetitions(), actual.weight());
            }
            if (actual.rpe() <= UNDERSHOOT_RPE) {
                return doubleJump(actual);
            }
        }
        return actual;
    }

    @Override
    public Exercise calculateNext(Exercise actual, Exercise planned) {
        int    actualReps  = actual.repetitions();
        int    plannedReps = planned.repetitions();
        double weight      = actual.weight();

        if (actualReps < plannedReps) {
            double completionRate = (double) actualReps / plannedReps;

            if (completionRate < config.deloadThresholdPct()) {
                double deloadWeight = config.roundToIncrement(weight * config.deloadFactor());
                return actual.withRepsAndWeight(config.minReps(), deloadWeight);
            }

            return actual.withRepsAndWeight(plannedReps, weight);
        }

        if (actual.rpe() > 0.0 && actual.rpe() >= OVERSHOOT_RPE) {
            return actual.withRepsAndWeight(plannedReps, weight);
        }

        if (actual.rpe() > 0.0 && actual.rpe() <= UNDERSHOOT_RPE) {
            return doubleJump(actual);
        }

        if (actualReps >= config.maxReps() + 2) {
            return doubleJump(actual);
        }

        return actual;
    }

    public ProgressionConfig getConfig() {
        return config;
    }

    private Exercise doubleJump(Exercise actual) {
        double weight    = actual.weight();
        double increment = Math.max(weight * 0.05, config.weightIncrementKg() * 2);
        double newWeight = config.roundToIncrement(weight + increment);
        return actual.withRepsAndWeight(config.minReps(), newWeight);
    }
}
