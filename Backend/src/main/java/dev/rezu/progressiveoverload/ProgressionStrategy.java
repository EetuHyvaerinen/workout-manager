package dev.rezu.progressiveoverload;

import dev.rezu.workout.Exercise;

public interface ProgressionStrategy {
    Exercise calculateNext(Exercise previousPerformance);
    Exercise calculateNext(Exercise actual, Exercise planned);
}