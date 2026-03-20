package dev.rezu.progressiveoverload;

public record ProgressionConfig(
        int minReps,
        int maxReps,
        double weightIncrementKg,
        double minimumIncrementKg,
        double deloadThresholdPct,
        double deloadFactor
) {
    public static ProgressionConfig hypertrophy() {
        return new ProgressionConfig(8, 12, 2.5, 1.25, 0.80, 0.90);
    }

    public static ProgressionConfig strength() {
        return new ProgressionConfig(3, 6, 2.5, 1.25, 0.80, 0.90);
    }

    public static ProgressionConfig endurance() {
        return new ProgressionConfig(12, 15, 1.0, 0.5, 0.80, 0.92);
    }

    public double roundToIncrement(double weight) {
        // Round to nearest minimum weight increment
        return Math.round(weight / minimumIncrementKg) * minimumIncrementKg;
    }
}
