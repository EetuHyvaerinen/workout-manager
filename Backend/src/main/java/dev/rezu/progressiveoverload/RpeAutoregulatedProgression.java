//package dev.rezu.progressiveoverload;
//
//import dev.rezu.workout.Exercise;
//
//public class RpeAutoregulatedProgression implements ProgressionStrategy {
//
//    private final int minReps;
//    private final int maxReps;
//    private final double baseIncrementPct; // e.g., 0.025 (2.5%)
//    private final double targetRpe;        // e.g., 8.0 (2 reps in reserve)
//
//    public RpeAutoregulatedProgression() {
//        this(8, 12, 0.025, 8.0);
//    }
//
//    public RpeAutoregulatedProgression(int minReps, int maxReps, double baseIncrementPct, double targetRpe) {
//        this.minReps = minReps;
//        this.maxReps = maxReps;
//        this.baseIncrementPct = baseIncrementPct;
//        this.targetRpe = targetRpe;
//    }
//
//    @Override
//    public Exercise calculateNext(Exercise previousPerformance) {
//        return null;
//    }
//
//    @Override
//    public Exercise calculateNext(Exercise actual, Exercise planned) {
//        int actualReps = actual.repetitions();
//        int plannedReps = planned.repetitions();
//        double currentWeight = actual.weight();
//
//        // Safety: If user didn't log RPE, assume it was a standard "hard" set (RPE 8)
//        double actualRpe = (actual.rpe() != null) ? actual.rpe() : 8.0;
//
//        // --- SCENARIO 1: MISSED REPS (FAILURE) ---
//        // If they couldn't finish the planned reps, RPE is irrelevant (it was failure).
//        // Logic: Deload weight slightly to allow recovery.
//        if (actualReps < plannedReps) {
//            double deloadWeight = roundWeight(currentWeight * 0.90);
//            return actual.withRepsAndWeight(minReps, deloadWeight);
//        }
//
//        // --- SCENARIO 2: REPS MET, BUT IT WAS A GRIND (OVERSHOOT) ---
//        // User hit the reps, but RPE was 9.5 or 10.
//        // Logic: Keep weight/reps exactly the same. Let them "grow into" this weight.
//        if (actualRpe >= 9.5) {
//            return actual.withRepsAndWeight(plannedReps, currentWeight);
//        }
//
//        // --- SCENARIO 3: REPS MET, WAY TOO EASY (UNDERSHOOT) ---
//        // User hit reps, but RPE was <= 6 (4+ reps in reserve).
//        // Logic: Double Jump. Increase weight by 2x the normal amount.
//        if (actualRpe <= 6.0) {
//            double aggressiveIncrease = Math.max(currentWeight * (baseIncrementPct * 2), 2.5);
//            double newWeight = roundWeight(currentWeight + aggressiveIncrease);
//
//            // If we are increasing weight, we usually reset reps to bottom of range
//            return actual.withRepsAndWeight(minReps, newWeight);
//        }
//
//        // --- SCENARIO 4: STANDARD PROGRESSION (TARGET RPE HIT) ---
//        // RPE was 7-9. This is perfect.
//        // Logic: Standard Double Progression (Add Reps -> If Max, Add Weight).
//        if (actualReps >= maxReps) {
//            // Hit rep ceiling -> Increase Weight, Reset Reps
//            double increase = Math.max(currentWeight * baseIncrementPct, 1.25);
//            return actual.withRepsAndWeight(minReps, roundWeight(currentWeight + increase));
//        } else {
//            // Haven't hit rep ceiling -> Add 1 rep, Keep Weight
//            return actual.withRepsAndWeight(actualReps + 1, currentWeight);
//        }
//    }
//
//    // Helper for standard gym rounding (nearest 0.25 or 1.0 depending on your plates)
//    private double roundWeight(double weight) {
//        return Math.round(weight * 4) / 4.0;
//    }
//}