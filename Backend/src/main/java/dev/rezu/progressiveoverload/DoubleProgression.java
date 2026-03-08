package dev.rezu.progressiveoverload;

import dev.rezu.workout.Exercise;

public class DoubleProgression implements ProgressionStrategy {

    private final int minReps;
    private final int maxReps;
    private final double incrementPercentage;
    private final double minimumWeightIncrement;

    // Standard Hypertrophy defaults: 8-12 reps, 2.5% load increase
    public DoubleProgression() {
        this(8, 12, 0.025, 1.25);
    }

    public DoubleProgression(int minReps, int maxReps, double incrementPercentage, double minimumWeightIncrement) {
        this.minReps = minReps;
        this.maxReps = maxReps;
        this.incrementPercentage = incrementPercentage;
        this.minimumWeightIncrement = minimumWeightIncrement;
    }

    @Override
    public Exercise calculateNext(Exercise current) {
        int currentReps = current.repetitions();
        double currentWeight = current.weight();

        if (currentReps < minReps) {
            return current.withRepsAndWeight(minReps, currentWeight);
        }

        if (currentReps >= maxReps) {
            double increase = Math.max(currentWeight * incrementPercentage, minimumWeightIncrement);
            double newWeight = Math.round((currentWeight + increase) * 4) / 4.0;

            return current.withRepsAndWeight(minReps, newWeight);
        } else {
            return current.withRepsAndWeight(currentReps + 1, currentWeight);
        }
    }

    @Override
    public Exercise calculateNext(Exercise actual, Exercise planned) {
        return null;
    }
}