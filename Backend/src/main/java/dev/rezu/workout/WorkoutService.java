package dev.rezu.workout;

import dev.rezu.logger.AsyncLogger;
import dev.rezu.database.DatabaseConnectionManager;
import dev.rezu.database.PooledConnection;
import dev.rezu.plannedworkout.PlannedWorkout;
import dev.rezu.plannedworkout.PlannedWorkoutDAO;
import dev.rezu.progressiveoverload.ProgressiveOverloadCalculator;

import java.sql.SQLException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WorkoutService {
    private static final AsyncLogger logger = AsyncLogger.getLogger(WorkoutService.class);

    private final DatabaseConnectionManager connectionManager;
    private final WorkoutDAO workoutDAO = new WorkoutDAO();
    private final ExerciseDAO exerciseDAO = new ExerciseDAO();
    private final PlannedWorkoutDAO plannedWorkoutDAO = new PlannedWorkoutDAO();
    private final ProgressiveOverloadCalculator progressiveOverloadCalculator = new ProgressiveOverloadCalculator();

    public WorkoutService(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public String createWorkoutWithExercises(List<Exercise> exercises, int userId, String name, Instant time) {
        try (PooledConnection conn = connectionManager.getConnection()) {
            conn.setAutoCommit(false);
                String workoutId = workoutDAO.createWorkout(conn, userId, name, time);

                List<Exercise> exercisesWithWorkoutId = exercises.stream()
                        .map(ex -> ex.withWorkoutId(workoutId))
                        .toList();
                exerciseDAO.createExercises(conn, exercisesWithWorkoutId);
                Workout completedWorkout = new Workout(
                        workoutId,
                        userId,
                        name,
                        time,
                        null,
                        exercisesWithWorkoutId
                );
                PlannedWorkout nextPlan = progressiveOverloadCalculator.getProgressiveOverloadForWorkout(completedWorkout);
                plannedWorkoutDAO.createPlan(conn, nextPlan);
                conn.commit();
                return workoutId;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Workout getWorkoutDetails(String workoutId, int userId) {
        try (PooledConnection conn = connectionManager.getConnection()) {
            Workout workout = workoutDAO.getWorkout(conn, workoutId, userId);
            if (workout != null) {
                List<Exercise> exercises = exerciseDAO.getExercisesByWorkoutId(conn, workoutId);
                return workout.withExercises(exercises);
            }
        } catch (SQLException e) {
            logger.error("Error fetching workout details for ID: " + workoutId, e);
        }
        return null;
    }

    public List<Workout> getWorkoutsForMonth(Instant instant, int userId) {
        ZonedDateTime startOfMonth = instant.atZone(ZoneOffset.UTC)
                .withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS);
        Instant start = startOfMonth.toInstant();
        Instant end = startOfMonth.plusMonths(1).toInstant();

        try (PooledConnection conn = connectionManager.getConnection()) {
            List<Workout> workouts = workoutDAO.getWorkouts(conn, start, end, userId);
            List<String> workoutIds = new ArrayList<>(workouts.size());
            for (Workout workout : workouts) {
                workoutIds.add(workout.id());
            }
            Map<String, List<Exercise>> exercisesByWorkoutId =
                    exerciseDAO.getExercisesByWorkoutIds(conn, workoutIds);
            List<Workout> returnData = new ArrayList<>(workouts.size());
            for (Workout workout : workouts) {
                List<Exercise> exercises = exercisesByWorkoutId.getOrDefault(workout.id(), List.of());
                returnData.add(workout.withExercises(exercises));
            }
            return returnData;

        } catch (SQLException e) {
            logger.error("Failed to fetch workouts for month: " + startOfMonth.getMonth() + " for user " + userId, e);
            throw new RuntimeException("Failed to fetch workouts for month", e);
        }
    }

    public List<Workout> getWorkouts(int userId) {
        try (PooledConnection conn = connectionManager.getConnection()) {
            List<Workout> workouts = workoutDAO.getWorkouts(conn, userId);
            if (workouts.isEmpty()) {
                logger.debug("No workouts found for user: " + userId);
                return List.of();
            }
            List<String> workoutIds = workouts.stream()
                    .map(Workout::id)
                    .toList();

            Map<String, List<Exercise>> exercisesByWorkoutId =
                    exerciseDAO.getExercisesByWorkoutIds(conn, workoutIds);

            return workouts.stream()
                    .map(w -> w.withExercises(
                            exercisesByWorkoutId.getOrDefault(w.id(), List.of())
                    ))
                    .toList();
        } catch (SQLException e) {
            logger.error("Error fetching workouts for user: " + userId, e);
            throw new RuntimeException("Error fetching workouts", e);        }
    }

    public String planNextWorkout(String sourceWorkoutId, int userId) {
        Workout source = getWorkoutDetails(sourceWorkoutId, userId);
        if (source == null) {
            logger.warn("Source workout " + sourceWorkoutId + " not found for user " + userId);
            throw new RuntimeException("Source workout not found");
        }

        PlannedWorkout nextPlan = progressiveOverloadCalculator.getProgressiveOverloadForWorkout(source);

        try (PooledConnection conn = connectionManager.getConnection()) {
            String dbId = plannedWorkoutDAO.createPlan(conn, nextPlan);
            logger.info("Saved progressive overload plan: " + dbId + " derived from workout: " + sourceWorkoutId);
            return dbId;
        } catch (SQLException e) {
            logger.error("Failed to save progressive overload plan for user " + userId, e);
            throw new RuntimeException("Database error while saving plan", e);
        }
    }

    public String saveWorkoutByDate(Workout workout, int userId) {
        try (PooledConnection conn = connectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String workoutId = workoutDAO.createWorkout(conn, userId, workout.name(), workout.createdAt());

                List<Exercise> exercisesWithId = workout.exercises().stream()
                        .map(ex -> ex.withWorkoutId(workoutId))
                        .toList();

                exerciseDAO.createExercises(conn, exercisesWithId);
                conn.commit();
                return workoutId;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save workout", e);
        }
    }


    public String savePlan(PlannedWorkout plan, int userId) {
        PlannedWorkout sanitized = new PlannedWorkout(
                plan.id(),
                userId,
                plan.name(),
                plan.activateTime(),
                plan.status(),
                plan.completedWorkoutId(),
                plan.exercises()
        );
        try (PooledConnection conn = connectionManager.getConnection()) {
            return plannedWorkoutDAO.createPlan(conn, sanitized);
        } catch (SQLException e) {
            logger.error("Failed to save planned workout for user " + userId, e);
            throw new RuntimeException(e);
        }
    }

    public String saveWorkoutByDate(String name, List<Exercise> exercises, Instant targetDate, int userId) {
        LocalDate target = targetDate.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        if (target.isAfter(today)) {
            PlannedWorkout newPlan = new PlannedWorkout(
                    UUID.randomUUID().toString(),
                    userId,
                    name,
                    targetDate,
                    WorkoutStatus.PLANNED,
                    null,
                    exercises
            );
            try (PooledConnection conn = connectionManager.getConnection()) {
                return plannedWorkoutDAO.createPlan(conn, newPlan);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to save future plan", e);
            }
        } else {
            return createWorkoutWithExercises(exercises, userId, name, targetDate);
        }
    }

    public List<PlannedWorkout> getPlannedWorkouts(int userId) {
        try (PooledConnection conn = connectionManager.getConnection()) {
            Instant startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
            plannedWorkoutDAO.markExpiredAsMissed(conn, userId, startOfToday);

            List<PlannedWorkout> plans = plannedWorkoutDAO.getActiveOrMissedPlans(conn, userId);
            List<String> ids = plans.stream().map(PlannedWorkout::id).toList();
            Map<String, List<Exercise>> exercises = plannedWorkoutDAO.getExercisesByPlanIds(conn, ids);

            return plans.stream().map(p -> new PlannedWorkout(
                    p.id(), p.userId(), p.name(), p.activateTime(),
                    p.status(), p.completedWorkoutId(), exercises.getOrDefault(p.id(), List.of())
            )).toList();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void rescheduleWorkout(String planId, Instant newDate) {
        try (PooledConnection conn = connectionManager.getConnection()) {
            plannedWorkoutDAO.reschedule(conn, planId, newDate);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String completePlannedWorkout(String planId, String name, List<Exercise> actuals, int userId) {
        try (PooledConnection conn = connectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String workoutId = workoutDAO.createWorkout(conn, userId, name);
                List<Exercise> exercisesWithId = actuals.stream()
                        .map(ex -> ex.withWorkoutId(workoutId)).toList();
                exerciseDAO.createExercises(conn, exercisesWithId);
                PlannedWorkout originalPlan = plannedWorkoutDAO.getPlanById(conn, planId, userId);
                plannedWorkoutDAO.markCompleted(conn, planId, workoutId);

                Workout finished = new Workout(workoutId, userId, name, Instant.now(), null, exercisesWithId);
                PlannedWorkout next = progressiveOverloadCalculator.getProgressiveOverloadForWorkout(finished, originalPlan);
                plannedWorkoutDAO.createPlan(conn, next);

                conn.commit();
                return workoutId;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean deleteWorkout(String workoutId, int userId) {
        if (workoutId == null || workoutId.isEmpty()) {
            throw new IllegalArgumentException("Workout ID cannot be null or empty");
        }
        try (PooledConnection conn = connectionManager.getConnection()) {
            boolean isDeleted = workoutDAO.deleteWorkout(conn, workoutId, userId);
            if (isDeleted) {
                logger.info("Deleted workout: " + workoutId + " for user: " + userId);
                return true;
            }
            logger.warn("Delete failed: Workout " + workoutId + " not found for user " + userId);
            return false;
        } catch (SQLException e) {
            logger.error("Error during workout deletion for ID: " + workoutId, e);
            return false;
        }
    }

    public boolean deletePlan(String id, int userId) {
        try (PooledConnection conn = connectionManager.getConnection()) {
            return plannedWorkoutDAO.deletePlan(conn, id, userId);
        } catch (SQLException e) {
            logger.error("Database error while deleting plan " + id + " for user " + userId, e);
            return false;
        }
    }
}
