package dev.rezu.dashboard;

public record LogEntry(
        String timestamp,
        String level,
        String thread,
        String logger,
        String message
) {}