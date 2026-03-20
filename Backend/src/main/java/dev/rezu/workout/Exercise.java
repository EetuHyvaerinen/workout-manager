package dev.rezu.workout;

import java.time.Instant;

public record Exercise(
        int id,
        String workoutId,
        String name,
        int repetitions,
        double weight,
        double rpe,
        Instant createdAt,
        Instant updatedAt
) {
    public Exercise(int id, String workoutId, String name, int repetitions, double weight, double rpe) {
        this(id, workoutId, name, repetitions, weight, rpe, null, null);
    }

    public Exercise withWorkoutId(String workoutId) {
        return new Exercise(this.id, workoutId, this.name, this.repetitions, this.weight, this.rpe, this.createdAt, this.updatedAt);
    }

    public Exercise withRepsAndWeight(int newReps, double newWeight) {
        return new Exercise(
                this.id,
                this.workoutId,
                this.name,
                newReps,
                newWeight,
                this.rpe,
                this.createdAt,
                this.updatedAt
        );
    }
}