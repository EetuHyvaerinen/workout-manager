package dev.rezu.progressiveoverload;

import dev.rezu.workout.Exercise;
import dev.rezu.plannedworkout.PlannedWorkout;
import dev.rezu.workout.Workout;
import dev.rezu.workout.WorkoutStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ProgressiveOverloadCalculator {

    private final ProgressionStrategy strategy;

    public ProgressiveOverloadCalculator() {
        this.strategy = new DoubleProgression();
    }

    public ProgressiveOverloadCalculator(ProgressionStrategy strategy) {
        this.strategy = strategy;
    }


    public Exercise getProgressiveOverloadForExercise(Exercise exercise) {
        return strategy.calculateNext(exercise);
    }

    public PlannedWorkout getProgressiveOverloadForWorkout(Workout workout) {
        List<Exercise> overloadedExercises = workout.exercises().stream()
                .map(this::getProgressiveOverloadForExercise)
                .toList();

        Instant nextOccurrence = workout.createdAt().plus(7, java.time.temporal.ChronoUnit.DAYS);
        return new PlannedWorkout(
                UUID.randomUUID().toString(),
                workout.userId(),
                workout.name(),
                nextOccurrence,
                WorkoutStatus.PLANNED,
                null,
                overloadedExercises
        );
    }

    public List<PlannedWorkout> applyProgressiveOverload(List<Workout> workouts) {
        return workouts.stream()
                .map(this::getProgressiveOverloadForWorkout)
                .toList();
    }
}