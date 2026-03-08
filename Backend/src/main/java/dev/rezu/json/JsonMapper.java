package dev.rezu.json;

import dev.rezu.auth.JwtHeader;
import dev.rezu.auth.JwtPayload;
import dev.rezu.dashboard.*;
import dev.rezu.plannedworkout.PlannedWorkout;
import dev.rezu.user.User;
import dev.rezu.workout.Exercise;
import dev.rezu.workout.Workout;
import dev.rezu.workout.WorkoutStatus;

import java.time.Instant;
import java.util.List;

public class JsonMapper {

    public static String toJson(Object o) {
        JsonWriter writer = new JsonWriter();
        if (!writeRecord(writer, o)) {
            writer.field("value", o);
        }
        return writer.toString();
    }

    public static boolean writeRecord(JsonWriter writer, Object value) {
        return switch (value) {
            case Exercise(int id, String wid, String name, int reps, double weight, Instant created, Instant updated) -> {
                writer.beginObject()
                        .field("id", id)
                        .field("workoutId", wid)
                        .field("name", name)
                        .field("repetitions", reps)
                        .field("weight", weight)
                        .field("createdAt", created)
                        .field("updatedAt", updated)
                        .endObject();
                yield true;
            }

            case Workout(String id, int uid, String name, Instant created, Instant updated, List<Exercise> exercises) -> {
                writer.beginObject()
                        .field("id", id)
                        .field("userId", uid)
                        .field("name", name)
                        .field("createdAt", created)
                        .field("updatedAt", updated)
                        .field("exercises", exercises)
                        .endObject();
                yield true;
            }

            case PlannedWorkout p -> {
                writer.beginObject()
                        .field("id", p.id())
                        .field("name", p.name())
                        .field("activateTime", p.activateTime())
                        .field("status", p.status().name())
                        .field("exercises", p.exercises()) // Recursively calls Exercise mapping
                        .endObject();
                yield true;
            }
            case User(int id, String email, String name, _, _, boolean isAdmin, Instant created) -> {
                writer.beginObject()
                        .field("id", id)
                        .field("email", email)
                        .field("name", name)
                        .field("isAdmin", isAdmin)
                        .field("createdAt", created)
                        .endObject();
                yield true;
            }
            case JwtHeader(String alg, String typ) -> {
                writer.beginObject()
                        .field("alg", alg)
                        .field("typ", typ)
                        .endObject();
                yield true;
            }
            case JwtPayload(String sub, boolean isAdmin, long iat, long exp) -> {
                writer.beginObject()
                        .field("sub", sub)
                        .field("isAdmin", isAdmin)
                        .field("iat", iat)
                        .field("exp", exp)
                        .endObject();
                yield true;
            }
            case LogResponse(String logFile,
                             List<String> availableLogFiles,
                             int totalLines,
                             List<String> logs,
                             Instant fetchedAt) -> {
                writer.beginObject()
                        .field("logFile", logFile)
                        .field("availableLogFiles", availableLogFiles)
                        .field("totalLines", totalLines)
                        .field("logs", logs)
                        .field("fetchedAt", fetchedAt)
                        .endObject();
                yield true;
            }
            case RecentActivity(List<RecentActivity.UserActivity> users,
                                List<RecentActivity.WorkoutActivity> workouts,
                                List<RecentActivity.ErrorActivity> errors) -> {
                writer.beginObject()
                        .field("recentUsers", users)
                        .field("recentWorkouts", workouts)
                        .field("recentErrors", errors)
                        .endObject();
                yield true;
            }
            case SystemStats(var totU, var totW, var totE, var act24, var wToday, var db, var srv, var at) -> {
                writer.beginObject()
                        .field("totalUsers", totU)
                        .field("totalWorkouts", totW)
                        .field("totalExercises", totE)
                        .field("activeUsers24h", act24)
                        .field("workoutsToday", wToday)
                        .field("databaseStats", db)
                        .field("serverStats", srv)
                        .field("collectedAt", at)
                        .endObject();
                yield true;
            }

            case DatabaseStats(int pSize, int act, int idle, long tot, long timeouts, long valFailed, long totalRecycled, double loadPercentage, boolean healthy) -> {
                writer.beginObject()
                        .field("poolSize", pSize)
                        .field("activeConnections", act)
                        .field("idleConnections", idle)
                        .field("totalConnectionsCreated", tot)
                        .field("connectionTimeouts", timeouts)
                        .field("valFailed", valFailed)
                        .field("totalRecycled", totalRecycled)
                        .field("loadPercentage", loadPercentage)
                        .field("healthy", healthy)
                        .endObject();
                yield true;
            }

            case ServerStats(long uptime, long totMem, long usedMem, long freeMem, int processors, long threads, int qSize, int cap,
                             long droppedCount
            ) -> {
                writer.beginObject()
                        .field("uptimeSeconds", uptime)
                        .field("totalMemoryMB", totMem)
                        .field("usedMemoryMB", usedMem)
                        .field("freeMemoryMB", freeMem)
                        .field("availableProcessors", processors)
                        .field("threadCount", threads)
                        .field("loggerQueueSize", qSize)
                        .field("loggerCapacity", cap)
                        .field("droppedLogEvents", droppedCount)
                        .endObject();
                yield true;
            }

            case RecentActivity.UserActivity(int uid, String email, String action, Instant ts) -> {
                writer.beginObject()
                        .field("userId", uid)
                        .field("email", email)
                        .field("action", action)
                        .field("timestamp", ts)
                        .endObject();
                yield true;
            }

            case RecentActivity.WorkoutActivity(String wid, int uid, String name, int count, Instant created) -> {
                writer.beginObject()
                        .field("workoutId", wid)
                        .field("userId", uid)
                        .field("workoutName", name)
                        .field("exerciseCount", count)
                        .field("createdAt", created)
                        .endObject();
                yield true;
            }

            case RecentActivity.ErrorActivity(String type, String msg, String endp, Instant ts) -> {
                writer.beginObject()
                        .field("errorType", type)
                        .field("message", msg)
                        .field("endpoint", endp)
                        .field("timestamp", ts)
                        .endObject();
                yield true;
            }
            case GrowthData(var uGrowth, var wTrends) -> {
                writer.beginObject()
                        .field("userGrowth", uGrowth)
                        .field("workoutTrends", wTrends)
                        .endObject();
                yield true;
            }
            case TimeSeriesData(String date, long val) -> {
                writer.beginObject()
                        .field("date", date)
                        .field("value", val)
                        .endObject();
                yield true;
            }
            case DashboardOverview(var stats, var logs, var msg) -> {
                writer.beginObject()
                        .field("stats", stats)
                        .field("availableLogFiles", logs)
                        .field("message", msg)
                        .endObject();
                yield true;
            }
            default -> false;
        };
    }


    public static Exercise readExercise(JsonNode n) {
        return new Exercise(
                n.integer("id"),
                n.str("workoutId"),
                n.str("name"),
                n.integer("repetitions"),
                n.dbl("weight"),
                n.instant("createdAt"),
                n.instant("updatedAt")
        );
    }

    public static Workout readWorkout(JsonNode n) {
        return new Workout(
                n.str("id"),
                n.integer("userId"),
                n.str("name"),
                n.instant("createdAt"),
                n.instant("updatedAt"),
                n.array("exercises", JsonMapper::readExercise)
        );
    }

    public static PlannedWorkout readPlannedWorkout(JsonNode n) {
        return new PlannedWorkout(
                n.str("id"),
                n.integer("userId"),
                n.str("name"),
                n.instant("activateTime"),
                WorkoutStatus.valueOf(n.str("status").toUpperCase()),
                n.str("completedWorkoutId"),
                n.array("exercises", JsonMapper::readExercise)
        );
    }

    public static User readUser(JsonNode n) {
        return new User(
                n.integer("id"),
                n.str("email"),
                n.str("name"),
                null, // Salt not usually sent in JSON
                null, // Hash not usually sent in JSON
                n.bool("isAdmin"),
                n.instant("createdAt")
        );
    }

    public static JwtPayload readJwtPayload(JsonNode n) {
        return new JwtPayload(
                n.str("sub"),
                n.bool("isAdmin"),
                n.lng("iat"),
                n.lng("exp")
        );
    }
    public static JwtHeader readJwtHeader(JsonNode n) {
        return new JwtHeader(n.str("alg"), n.str("typ"));
    }
}