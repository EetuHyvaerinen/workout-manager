package dev.rezu.dashboard;

import java.time.Instant;
import java.util.List;

public record LogResponse(
        String logFile,
        List<String> availableLogFiles,
        int totalLines,
        List<String> logs,
        Instant fetchedAt
) {
    public static LogResponse error(String name, List<String> available, String msg) {
        return new LogResponse(name, List.copyOf(available), -1, List.of(msg), Instant.now());
    }
}