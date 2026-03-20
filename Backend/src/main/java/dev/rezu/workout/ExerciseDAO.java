package dev.rezu.workout;

import dev.rezu.database.PooledConnection;

import java.sql.*;
import java.util.*;

public class ExerciseDAO {

    public int createExercise(PooledConnection conn, Exercise exercise) throws SQLException {
        String sql = "INSERT INTO exercises (workout_id, name, repetitions, weight) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, exercise.workoutId());
            pstmt.setString(2, exercise.name());
            pstmt.setInt(3, exercise.repetitions());
            pstmt.setDouble(4, exercise.weight());
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
            throw new SQLException("Failed to get exercise ID");
        }
    }

    public void createExercises(PooledConnection conn, List<Exercise> exercises) throws SQLException {
        String sql = "INSERT INTO exercises (workout_id, name, repetitions, weight) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Exercise ex : exercises) {
                stmt.setString(1, ex.workoutId());
                stmt.setString(2, ex.name());
                stmt.setInt(3, ex.repetitions());
                stmt.setDouble(4, ex.weight());
                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

    public List<Exercise> getExercisesByWorkoutId(PooledConnection conn, String workoutId) throws SQLException {
        List<Exercise> exercises = new ArrayList<>();
        String sql = "SELECT * FROM exercises WHERE workout_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, workoutId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Exercise exercise = mapExercise(rs);
                    exercises.add(exercise);
                }
            }
        }
        return exercises;
    }

    public Map<String, List<Exercise>> getExercisesByWorkoutIds(PooledConnection conn, List<String> workoutIds) throws SQLException {
        if (workoutIds.isEmpty()) return Map.of();

        String inClause = String.join(", ", Collections.nCopies(workoutIds.size(), "?"));
        String sql = "SELECT * FROM exercises WHERE workout_id IN (" + inClause + ") ORDER BY id";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < workoutIds.size(); i++) {
                ps.setString(i + 1, workoutIds.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                Map<String, List<Exercise>> map = new HashMap<>();

                while (rs.next()) {
                    Exercise exercise = mapExercise(rs);
                    map.computeIfAbsent(exercise.workoutId(), _ -> new ArrayList<>()).add(exercise);
                }

                return map;
            }
        }
    }

    public boolean updateExercise(PooledConnection conn, Exercise exercise) throws SQLException {
        String sql = "UPDATE exercises SET name = ?, repetitions = ?, weight = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, exercise.name());
            pstmt.setInt(2, exercise.repetitions());
            pstmt.setDouble(3, exercise.weight());
            pstmt.setInt(4, exercise.id());
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean deleteExercise(PooledConnection conn, int exerciseId) throws SQLException {
        String sql = "DELETE FROM exercises WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, exerciseId);
            return pstmt.executeUpdate() > 0;
        }
    }

    private Exercise mapExercise(ResultSet rs) throws SQLException {
        return new Exercise(
                rs.getInt("id"),
                rs.getString("workout_id"),
                rs.getString("name"),
                rs.getInt("repetitions"),
                rs.getDouble("weight"),
                rs.getDouble("rpe"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }
}