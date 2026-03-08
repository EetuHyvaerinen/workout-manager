package dev.rezu.dashboard;

import java.util.List;

public record DashboardOverview(SystemStats stats, List<String> availableLogFiles, String message) {
}
