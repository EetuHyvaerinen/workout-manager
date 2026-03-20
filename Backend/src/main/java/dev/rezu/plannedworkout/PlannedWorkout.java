package dev.rezu.plannedworkout;

import dev.rezu.workout.Exercise;
import dev.rezu.workout.Workout;
import dev.rezu.workout.WorkoutStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlannedWorkout(
        String id,
        int userId,
        String name,
        Instant activateTime,
        WorkoutStatus status,
        String completedWorkoutId,
        List<Exercise> exercises
) {

    public PlannedWorkout {
        exercises = (exercises == null) ? List.of() : List.copyOf(exercises);
        if (status == null) status = WorkoutStatus.PLANNED;
    }

    public static PlannedWorkout draft(int userId, String name, Instant activateTime, List<Exercise> exercises) {
        return new PlannedWorkout(
                UUID.randomUUID().toString(),
                userId,
                name,
                activateTime,
                WorkoutStatus.PLANNED,
                null,
                exercises
        );
    }
    public static PlannedWorkout fromWorkout(Workout workout) {
        return new PlannedWorkout(
                UUID.randomUUID().toString(), // generate new ID for the plan
                workout.userId(),
                workout.name(),
                workout.createdAt(),          // activateTime is the same as workout's date
                WorkoutStatus.PLANNED,
                null,                         // not completed yet
                workout.exercises()           // copy exercises
        );
    }
}