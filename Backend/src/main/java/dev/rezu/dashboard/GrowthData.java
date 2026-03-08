package dev.rezu.dashboard;

import java.util.List;

public record GrowthData(
        List<TimeSeriesData> userGrowth,
        List<TimeSeriesData> workoutTrends
) {}