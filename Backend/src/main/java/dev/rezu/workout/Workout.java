package dev.rezu.workout;

import java.time.Instant;
import java.util.List;

public record Workout(
        String id,
        int userId,
        String name,
        Instant createdAt,
        Instant updatedAt,
        List<Exercise> exercises
) {
    public Workout {
        exercises = (exercises == null) ? List.of() : List.copyOf(exercises);
        createdAt = (createdAt == null) ? Instant.now() : createdAt;
    }

    public Workout(String id, int userId, String name, Instant createdAt) {
        this(id, userId, name, createdAt, null, List.of());
    }

    public Workout withExercises(List<Exercise> newExercises) {
        return new Workout(this.id, this.userId, this.name, this.createdAt, this.updatedAt, List.copyOf(newExercises));
    }
}