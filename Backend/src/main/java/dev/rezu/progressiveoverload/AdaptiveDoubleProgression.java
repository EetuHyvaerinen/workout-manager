package dev.rezu.progressiveoverload;

import dev.rezu.workout.Exercise;

public class AdaptiveDoubleProgression implements ProgressionStrategy {

    private final int minReps;
    private final int maxReps;
    private final double incrementPercentage;
    private final double minimumWeightIncrement;

    public AdaptiveDoubleProgression() {
        this(8, 12, 0.025, 1.25);
    }

    public AdaptiveDoubleProgression(int minReps, int maxReps, double incrementPercentage, double minimumWeightIncrement) {
        this.minReps = minReps;
        this.maxReps = maxReps;
        this.incrementPercentage = incrementPercentage;
        this.minimumWeightIncrement = minimumWeightIncrement;
    }

    @Override
    public Exercise calculateNext(Exercise previousPerformance) {
        return null;
    }

    @Override
    public Exercise calculateNext(Exercise actual, Exercise planned) {
        int actualReps = actual.repetitions();
        double actualWeight = actual.weight();

        if (actualReps < planned.repetitions()) {
            double completionRate = (double) actualReps / planned.repetitions();

            if (completionRate < 0.80) {
                double deloadWeight = Math.round((actualWeight * 0.90) * 4) / 4.0;
                return actual.withRepsAndWeight(minReps, deloadWeight);
            }

            return actual.withRepsAndWeight(minReps, actualWeight);
        }

        if (actualReps >= maxReps) {
            double increase = Math.max(actualWeight * incrementPercentage, minimumWeightIncrement);
            double newWeight = Math.round((actualWeight + increase) * 4) / 4.0;
            return actual.withRepsAndWeight(minReps, newWeight);
        }

        return actual.withRepsAndWeight(actualReps + 1, actualWeight);
    }
}