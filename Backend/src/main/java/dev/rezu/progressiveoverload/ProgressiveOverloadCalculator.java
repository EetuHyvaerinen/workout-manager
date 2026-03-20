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
        this.strategy = new AdaptiveDoubleProgression();
    }

    // If more progressive overloading strategies are implemented in the future
    public ProgressiveOverloadCalculator(ProgressionStrategy strategy) {
        this.strategy = strategy;
    }


    public Exercise getProgressiveOverloadForExercise(Exercise exercise) {
        return strategy.calculateNext(exercise);
    }

    public Exercise getProgressiveOverloadForExercise(Exercise actual, Exercise planned) {
        return strategy.calculateNext(actual, planned);
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

    public PlannedWorkout getProgressiveOverloadForWorkout(Workout actualWorkout,
                                                           PlannedWorkout plannedWorkout) {

        List<Exercise> actualExercises  = actualWorkout.exercises();
        List<Exercise> plannedExercises = plannedWorkout.exercises();

        if (actualExercises.size() != plannedExercises.size()) {
            throw new IllegalArgumentException("Actual and planned workouts differ in exercise count");
        }

        List<Exercise> overloadedExercises =
                java.util.stream.IntStream.range(0, actualExercises.size())
                        .mapToObj(i -> getProgressiveOverloadForExercise(
                                actualExercises.get(i),
                                plannedExercises.get(i)
                        ))
                        .toList();

        Instant nextOccurrence = plannedWorkout.activateTime().plus(7, java.time.temporal.ChronoUnit.DAYS);

        return new PlannedWorkout(
                UUID.randomUUID().toString(),
                actualWorkout.userId(),
                actualWorkout.name(),
                nextOccurrence,
                WorkoutStatus.PLANNED,
                null,
                overloadedExercises
        );
    }
}