package dev.rezu.plannedworkout;

import dev.rezu.database.PooledConnection;
import dev.rezu.logger.AsyncLogger;
import dev.rezu.workout.Exercise;
import dev.rezu.workout.WorkoutStatus;

import java.sql.*;
import java.time.Instant;
import java.util.*;

public class PlannedWorkoutDAO {

    private static final AsyncLogger logger = AsyncLogger.getLogger(PlannedWorkoutDAO.class);

    public String createPlan(PooledConnection conn, PlannedWorkout plan) throws SQLException {
        String sql = "INSERT INTO planned_workouts (id, user_id, name, activate_time, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, plan.id());
            pstmt.setInt(2, plan.userId());
            pstmt.setString(3, plan.name());
            pstmt.setTimestamp(4, Timestamp.from(plan.activateTime()));
            pstmt.setString(5, plan.status().name());
            pstmt.executeUpdate();

            savePlannedExercises(conn, plan.id(), plan.exercises());
            return plan.id();
        }
    }

    private void savePlannedExercises(PooledConnection conn, String planId, List<Exercise> exercises) throws SQLException {
        String sql = "INSERT INTO planned_exercises (planned_workout_id, name, target_repetitions, target_weight) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Exercise ex : exercises) {
                pstmt.setString(1, planId);
                pstmt.setString(2, ex.name());
                pstmt.setInt(3, ex.repetitions());
                pstmt.setDouble(4, ex.weight());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    public List<PlannedWorkout> getActiveOrMissedPlans(PooledConnection conn, int userId) throws SQLException {
        // We only fetch PLANNED and MISSED. COMPLETED are handled by the historical workouts table.
        String sql = "SELECT * FROM planned_workouts WHERE user_id = ? AND status IN ('PLANNED', 'MISSED') ORDER BY activate_time ASC";
        List<PlannedWorkout> plans = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    plans.add(new PlannedWorkout(
                            rs.getString("id"),
                            userId,
                            rs.getString("name"),
                            rs.getTimestamp("activate_time").toInstant(),
                            WorkoutStatus.valueOf(rs.getString("status")),
                            rs.getString("completed_workout_id"),
                            new ArrayList<>()
                    ));
                }
            }
        }
        return plans;
    }

    public void markExpiredAsMissed(PooledConnection conn, int userId, Instant cutoff) throws SQLException {
        String sql = "UPDATE planned_workouts SET status = 'MISSED' WHERE user_id = ? AND status = 'PLANNED' AND activate_time < ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setTimestamp(2, Timestamp.from(cutoff));
            pstmt.executeUpdate();
        }
    }

    public void reschedule(PooledConnection conn, String planId, Instant newTime) throws SQLException {
        String sql = "UPDATE planned_workouts SET activate_time = ?, status = 'PLANNED' WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.from(newTime));
            pstmt.setString(2, planId);
            pstmt.executeUpdate();
        }
    }

    public void markCompleted(PooledConnection conn, String planId, String workoutId) throws SQLException {
        String sql = "UPDATE planned_workouts SET status = 'COMPLETED', completed_workout_id = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, workoutId);
            pstmt.setString(2, planId);
            pstmt.executeUpdate();
        }
    }

    public Map<String, List<Exercise>> getExercisesByPlanIds(PooledConnection conn, List<String> planIds) throws SQLException {
        if (planIds.isEmpty()) return Map.of();
        String inClause = String.join(", ", Collections.nCopies(planIds.size(), "?"));
        String sql = "SELECT * FROM planned_exercises WHERE planned_workout_id IN (" + inClause + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < planIds.size(); i++) ps.setString(i + 1, planIds.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, List<Exercise>> map = new HashMap<>();
                while (rs.next()) {
                    String pId = rs.getString("planned_workout_id");
                    map.computeIfAbsent(pId, _ -> new ArrayList<>()).add(new Exercise(
                            0, pId, rs.getString("name"), rs.getInt("target_repetitions"), rs.getDouble("target_weight")
                    ));
                }
                return map;
            }
        }
    }

    public boolean deletePlan(PooledConnection conn, String id, int userId) throws SQLException {
        String sql = "DELETE FROM planned_workouts WHERE id = ? AND user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() == 1;
        }
    }
}