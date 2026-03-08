package dev.rezu.workout;

import com.sun.net.httpserver.HttpExchange;
import dev.rezu.*;
import dev.rezu.json.JsonMapper;
import dev.rezu.json.JsonReader;
import dev.rezu.json.JsonNode;
import dev.rezu.logger.AsyncLogger;
import dev.rezu.plannedworkout.PlannedWorkout;
import dev.rezu.response.ResponseMessage;
import dev.rezu.response.ResponseMessageType;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class WorkoutHandler extends BaseHandler {

    private final WorkoutService workoutService;
    private static final AsyncLogger logger = AsyncLogger.getLogger(WorkoutHandler.class);

    public WorkoutHandler(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @Override
    protected void handleGet(HttpExchange exchange) throws IOException {
        int userId = auth().userId();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        logger.info("entered handleGet for: " + exchange.getRemoteAddress() + " with path: " + path);

        if (query != null && query.contains("date=")) {
            String dateString = extractParameter(query, "date");
            Instant instant = Instant.parse(dateString);
            List<Workout> workouts = workoutService.getWorkoutsForMonth(instant, userId);
            ResponseMessage.send(exchange, 200, ResponseMessageType.DATA, workouts);
        }
        else if (query != null && query.contains("workoutId=")) {
            String workoutId = extractParameter(query, "workoutId");
            Workout workout = workoutService.getWorkoutDetails(workoutId, userId);
            ResponseMessage.send(exchange, 200, ResponseMessageType.DATA, workout);
        } else if (path.endsWith("/plans")) {
            List<PlannedWorkout> plans = workoutService.getPlannedWorkouts(userId);
            ResponseMessage.send(exchange, 200, ResponseMessageType.DATA, plans);
        } else {
            List<Workout> workouts = workoutService.getWorkouts(userId);
            ResponseMessage.send(exchange, 200, ResponseMessageType.DATA, workouts);
        }
    }

    @Override
    protected void handlePost(HttpExchange exchange) throws IOException {
        int userId = auth().userId();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        logger.info("Processing workout creation request for user: " + userId + " on path: " + path);

        InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        JsonNode jsonNode = JsonReader.parse(reader);

        if (path.endsWith("/plans/generate")) {
            String sourceId = extractParameter(exchange.getRequestURI().getQuery(), "sourceWorkoutId");
            String planId = workoutService.planNextWorkout(sourceId, userId);
            ResponseMessage.send(exchange, 201, ResponseMessageType.MESSAGE, planId);
            return;
        }

        if (path.endsWith("/plans/complete")) {
            Workout workout = jsonNode.map(JsonMapper::readWorkout);
            String planId = extractParameter(exchange.getRequestURI().getQuery(), "planId");

            String workoutId = workoutService.completePlannedWorkout(planId, workout.name(), workout.exercises(), userId);
            ResponseMessage.send(exchange, 200, ResponseMessageType.DATA, Map.of("workoutId", workoutId));
            return;
        }

        if (path.endsWith("/plans/reschedule")) {
            String planId = extractParameter(query, "planId");
            String dateStr = extractParameter(query, "date");
            workoutService.rescheduleWorkout(planId, Instant.parse(dateStr));
            ResponseMessage.send(exchange, 200, ResponseMessageType.MESSAGE, "Rescheduled successfully");
            return;
        }

        if (path.contains("/plans")) {
            PlannedWorkout plan = jsonNode.map(JsonMapper::readPlannedWorkout);
            workoutService.savePlan(plan, userId);
            ResponseMessage.send(exchange, 201, "Plan saved");
        } else {
            Workout workout = jsonNode.map(JsonMapper::readWorkout);
            workoutService.saveWorkoutByDate(workout, userId);
            ResponseMessage.send(exchange, 201, "Workout saved");
        }
    }

    @Override
    protected void handleDelete(HttpExchange exchange) throws IOException {
        int userId = auth().userId();
        String query = exchange.getRequestURI().getQuery();

        if (query == null) {
            ResponseMessage.send(exchange, 400, ResponseMessageType.ERROR, "Missing parameters");
            return;
        }

        boolean success;
        String id;

        if (query.contains("planId=")) {
            id = extractParameter(query, "planId");
            logger.info("Deleting plan " + id + " for user " + userId);
            success = workoutService.deletePlan(id, userId);
        } else if (query.contains("workoutId=")) {
            id = extractParameter(query, "workoutId");
            logger.info("Deleting workout " + id + " for user " + userId);
            success = workoutService.deleteWorkout(id, userId);
        } else {
            ResponseMessage.send(exchange, 400, ResponseMessageType.ERROR, "Missing workoutId or planId");
            return;
        }

        if (success) {
            logger.info("Successfully deleted item " + id);
            ResponseMessage.send(exchange, 204, "");
        } else {
            logger.warn("Delete failed: Item " + id + " not found or unauthorized");
            ResponseMessage.send(exchange, 404, ResponseMessageType.ERROR, "Item not found");
        }
    }

    private String extractParameter(String query, String paramName) {
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("Query string is missing");
        }

        String prefix = paramName + "=";
        int startIndex = query.indexOf(prefix);
        if (startIndex == -1) {
            throw new IllegalArgumentException("Missing parameter: " + paramName);
        }

        startIndex += prefix.length();
        int endIndex = query.indexOf('&', startIndex);

        String rawValue = (endIndex == -1)
                ? query.substring(startIndex)
                : query.substring(startIndex, endIndex);

        String value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8).trim();

        if (value.isEmpty()) {
            throw new IllegalArgumentException("Parameter '" + paramName + "' cannot be empty");
        }

        return value;
    }
}