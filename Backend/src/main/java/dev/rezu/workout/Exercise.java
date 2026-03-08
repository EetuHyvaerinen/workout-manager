package dev.rezu.workout;

import java.time.Instant;

public record Exercise(
        int id,
        String workoutId,
        String name,
        int repetitions,
        double weight,
        Instant createdAt,
        Instant updatedAt
) {
    public Exercise(int id, String workoutId, String name, int repetitions, double weight) {
        this(id, workoutId, name, repetitions, weight, null, null);
    }

    public Exercise withWorkoutId(String workoutId) {
        return new Exercise(this.id, workoutId, this.name(), this.repetitions(), this.weight(), this.createdAt(), this.updatedAt());
    }

    public Exercise withRepsAndWeight(int newReps, double newWeight) {
        return new Exercise(
                this.id,
                this.workoutId,
                this.name,
                newReps,
                newWeight,
                this.createdAt,
                this.updatedAt
        );
    }
}