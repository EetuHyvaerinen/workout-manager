package dev.rezu.progressiveoverload;

import dev.rezu.workout.Exercise;
import dev.rezu.plannedworkout.PlannedWorkout;
import dev.rezu.workout.Workout;
import dev.rezu.workout.WorkoutStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

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

    public PlannedWorkout getProgressiveOverloadForWorkout(
            Workout actualWorkout,
            PlannedWorkout plannedWorkout) {

        List<Exercise> actualExercises  = actualWorkout.exercises();
        List<Exercise> plannedExercises = plannedWorkout.exercises();

        Map<Integer, Exercise> plannedById = new HashMap<>();
        for (Exercise planned : plannedExercises) {
            plannedById.put(planned.id(), planned);
        }

        List<Exercise> overloadedExercises = new ArrayList<>();

        for (Exercise actual : actualExercises) {
            Exercise planned = plannedById.get(actual.id());
            if (planned != null) {
                overloadedExercises.add(getProgressiveOverloadForExercise(actual, planned));
            } else {
                overloadedExercises.add(getProgressiveOverloadForExercise(actual));
            }
        }

        for (Exercise planned : plannedExercises) {
            if (actualExercises.stream().noneMatch(a -> a.id() == planned.id())) {
                overloadedExercises.add(planned);
            }
        }

        Instant nextOccurrence = plannedWorkout.activateTime().plus(7, ChronoUnit.DAYS);

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