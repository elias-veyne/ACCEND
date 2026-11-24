package com.accend.app.data.seed

import com.accend.app.data.db.MuscleGroup
import com.accend.app.data.db.ProgressionLevel
import com.accend.app.data.db.TargetUnit

data class SeedExerciseSpec(
    val dayOfWeek: Int, // 1=Sun .. 7=Sat
    val orderInDay: Int,
    val name: String,
    val muscleGroup: MuscleGroup,
    val sets: Int,
    val targetReps: Int,
    val unit: TargetUnit,
    val compoundLadder: List<Pair<String, Int>>? = null
)

object SeedData {
    val weeklyProgram: List<SeedExerciseSpec> = listOf(
        // Day 1 (Sun) — Upper
        SeedExerciseSpec(
            dayOfWeek = 1, orderInDay = 1, name = "Knee Pushups",
            muscleGroup = MuscleGroup.CHEST_PUSH, sets = 1, targetReps = 20, unit = TargetUnit.REPS,
            compoundLadder = listOf(
                "Knee Pushups" to 20,
                "Incline Pushups" to 15,
                "Full Pushups" to 10,
                "Decline Pushups" to 8,
                "Diamond Pushups" to 8,
                "Archer Pushups" to 6,
                "One-Arm Prep" to 5
            )
        ),
        SeedExerciseSpec(1, 2, "Neck Tilts", MuscleGroup.NECK, 1, 50, TargetUnit.REPS),
        SeedExerciseSpec(1, 3, "Bicep Curls", MuscleGroup.BICEPS, 1, 20, TargetUnit.REPS),
        SeedExerciseSpec(
            dayOfWeek = 1, orderInDay = 4, name = "Bench Dips",
            muscleGroup = MuscleGroup.TRICEPS, sets = 1, targetReps = 30, unit = TargetUnit.REPS,
            compoundLadder = listOf(
                "Bench Dips (bent knees)" to 30,
                "Bench Dips (legs straight)" to 20,
                "Feet-Elevated Bench Dips" to 15,
                "Single-Leg Bench Dips" to 8
            )
        ),
        SeedExerciseSpec(
            dayOfWeek = 1, orderInDay = 5, name = "Australian Pushups",
            muscleGroup = MuscleGroup.BACK_PULL, sets = 1, targetReps = 10, unit = TargetUnit.REPS,
            compoundLadder = listOf(
                "Australian (high bar)" to 10,
                "Australian (mid bar)" to 10,
                "Australian (low bar)" to 8,
                "Feet-Elevated Australian" to 8,
                "Archer Australian" to 6
            )
        ),
        SeedExerciseSpec(1, 6, "Shoulder Taps", MuscleGroup.SHOULDERS, 1, 10, TargetUnit.REPS),
        SeedExerciseSpec(1, 7, "Superman Hold", MuscleGroup.BACK_PULL, 3, 20, TargetUnit.SECONDS),

        // Day 2 (Mon) — Core
        SeedExerciseSpec(2, 1, "Crunches", MuscleGroup.CORE, 1, 30, TargetUnit.REPS),
        SeedExerciseSpec(2, 2, "Leg Raises", MuscleGroup.CORE, 1, 20, TargetUnit.REPS),
        SeedExerciseSpec(2, 3, "Plank", MuscleGroup.CORE, 3, 20, TargetUnit.SECONDS),
        SeedExerciseSpec(2, 4, "Russian Twists", MuscleGroup.CORE, 1, 30, TargetUnit.REPS),
        SeedExerciseSpec(2, 5, "Wrist Curls", MuscleGroup.FOREARMS, 1, 20, TargetUnit.REPS),
        SeedExerciseSpec(2, 6, "Forearm Plank", MuscleGroup.CORE, 1, 20, TargetUnit.SECONDS),

        // Day 3 (Tue) — Legs
        SeedExerciseSpec(
            dayOfWeek = 3, orderInDay = 1, name = "Squats",
            muscleGroup = MuscleGroup.LEGS, sets = 1, targetReps = 30, unit = TargetUnit.REPS,
            compoundLadder = listOf(
                "Squats" to 30,
                "Tempo Squats" to 20,
                "Pause Squats" to 20,
                "Shrimp Squat Prep" to 8,
                "Pistol Prep" to 6
            )
        ),
        SeedExerciseSpec(
            dayOfWeek = 3, orderInDay = 2, name = "Lunges",
            muscleGroup = MuscleGroup.LEGS, sets = 1, targetReps = 20, unit = TargetUnit.REPS,
            compoundLadder = listOf(
                "Lunges" to 20,
                "Reverse Lunges (pause)" to 15,
                "Deficit Lunges" to 15,
                "Jumping Lunges" to 10
            )
        ),
        SeedExerciseSpec(
            dayOfWeek = 3, orderInDay = 3, name = "Calf Raises",
            muscleGroup = MuscleGroup.LEGS, sets = 1, targetReps = 40, unit = TargetUnit.REPS,
            compoundLadder = listOf(
                "Calf Raises" to 40,
                "Single-Leg Calf Raises" to 20,
                "Deficit Single-Leg Calf Raises" to 15
            )
        ),
        SeedExerciseSpec(
            dayOfWeek = 3, orderInDay = 4, name = "Glute Bridges",
            muscleGroup = MuscleGroup.LEGS, sets = 1, targetReps = 20, unit = TargetUnit.REPS,
            compoundLadder = listOf(
                "Glute Bridges" to 20,
                "Single-Leg Glute Bridge" to 12,
                "Shoulder-Elevated Hip Thrust" to 12,
                "Single-Leg Hip Thrust" to 10
            )
        ),
        SeedExerciseSpec(3, 5, "Wall Sit", MuscleGroup.LEGS, 3, 20, TargetUnit.SECONDS),
        SeedExerciseSpec(3, 6, "Broad Jumps", MuscleGroup.LEGS, 1, 5, TargetUnit.REPS),

        // Day 4 (Wed) — Upper
        SeedExerciseSpec(
            dayOfWeek = 4, orderInDay = 1, name = "Incline Pushups",
            muscleGroup = MuscleGroup.CHEST_PUSH, sets = 1, targetReps = 20, unit = TargetUnit.REPS,
            compoundLadder = listOf(
                "Incline Pushups" to 20,
                "Full Pushups" to 10,
                "Decline Pushups" to 8,
                "Archer Pushups" to 6,
                "One-Arm Prep" to 5
            )
        ),
        SeedExerciseSpec(
            dayOfWeek = 4, orderInDay = 2, name = "Diamond Knee Pushups",
            muscleGroup = MuscleGroup.TRICEPS, sets = 1, targetReps = 10, unit = TargetUnit.REPS,
            compoundLadder = listOf(
                "Diamond Knee Pushups" to 10,
                "Diamond Incline Pushups" to 10,
                "Diamond Pushups" to 8,
                "Decline Diamond Pushups" to 6
            )
        ),
        SeedExerciseSpec(
            dayOfWeek = 4, orderInDay = 3, name = "Towel Rows",
            muscleGroup = MuscleGroup.BACK_PULL, sets = 1, targetReps = 10, unit = TargetUnit.REPS,
            compoundLadder = listOf(
                "Towel Rows" to 10,
                "Towel Rows (feet forward)" to 10,
                "Towel Rows (slow 3 s)" to 8,
                "Single-Arm Towel Row" to 6
            )
        ),
        SeedExerciseSpec(4, 4, "Lateral Raises", MuscleGroup.SHOULDERS, 1, 20, TargetUnit.REPS),
        SeedExerciseSpec(4, 5, "Neck Extensions", MuscleGroup.NECK, 1, 30, TargetUnit.REPS),
        SeedExerciseSpec(4, 6, "Reverse Snow Angels", MuscleGroup.BACK_PULL, 1, 20, TargetUnit.REPS),

        // Day 5 (Thu) — Core
        SeedExerciseSpec(5, 1, "Bicycle Crunches", MuscleGroup.CORE, 1, 30, TargetUnit.REPS),
        SeedExerciseSpec(5, 2, "Lying Knee Raises", MuscleGroup.CORE, 1, 20, TargetUnit.REPS),
        SeedExerciseSpec(5, 3, "Side Plank", MuscleGroup.CORE, 2, 20, TargetUnit.SECONDS),
        SeedExerciseSpec(5, 4, "Flutter Kicks", MuscleGroup.CORE, 1, 30, TargetUnit.REPS),
        SeedExerciseSpec(5, 5, "Hollow Hold", MuscleGroup.CORE, 3, 15, TargetUnit.SECONDS),
        SeedExerciseSpec(5, 6, "Forearm Curls", MuscleGroup.FOREARMS, 1, 20, TargetUnit.REPS),

        // Day 6 (Fri) — Legs
        SeedExerciseSpec(
            dayOfWeek = 6, orderInDay = 1, name = "Bulgarian Split Squats",
            muscleGroup = MuscleGroup.LEGS, sets = 1, targetReps = 20, unit = TargetUnit.REPS,
            compoundLadder = listOf(
                "Bulgarian Split Squats" to 20,
                "Slow Bulgarian Split Squats" to 15,
                "Deep Bulgarian Split Squats" to 12,
                "Shrimp Squat" to 8
            )
        ),
        SeedExerciseSpec(6, 2, "Jump Squats", MuscleGroup.LEGS, 1, 15, TargetUnit.REPS),
        SeedExerciseSpec(
            dayOfWeek = 6, orderInDay = 3, name = "Single-Leg Glute Bridge",
            muscleGroup = MuscleGroup.LEGS, sets = 1, targetReps = 20, unit = TargetUnit.REPS,
            compoundLadder = listOf(
                "Single-Leg Glute Bridge" to 20,
                "Shoulder-Elevated Single-Leg Hip Thrust" to 12
            )
        ),
        SeedExerciseSpec(
            dayOfWeek = 6, orderInDay = 4, name = "Single-Leg Calf Raises",
            muscleGroup = MuscleGroup.LEGS, sets = 1, targetReps = 30, unit = TargetUnit.REPS,
            compoundLadder = listOf(
                "Single-Leg Calf Raises" to 30,
                "Deficit Single-Leg Calf Raises" to 20
            )
        ),
        SeedExerciseSpec(6, 5, "Walking Lunges", MuscleGroup.LEGS, 1, 20, TargetUnit.REPS),
        SeedExerciseSpec(6, 6, "Sprint 100m", MuscleGroup.LEGS, 2, 100, TargetUnit.METERS),

        // Day 7 (Sat) — Rest / Active Recovery (Sleep 9h is mirrored from NutritionLog)
        SeedExerciseSpec(7, 1, "Walk", MuscleGroup.RECOVERY, 1, 20, TargetUnit.MINUTES),
        SeedExerciseSpec(7, 2, "Full-body Stretch", MuscleGroup.RECOVERY, 1, 10, TargetUnit.MINUTES),
        SeedExerciseSpec(7, 3, "Neck Mobility", MuscleGroup.RECOVERY, 1, 0, TargetUnit.NONE)
    )

    fun buildLevelsFor(spec: SeedExerciseSpec): List<ProgressionLevel> {
        return if (spec.compoundLadder != null) {
            spec.compoundLadder.map { (lvlName, target) ->
                ProgressionLevel(name = lvlName, sets = spec.sets, target = target)
            }
        } else {
            listOf(ProgressionLevel(name = spec.name, sets = spec.sets, target = spec.targetReps))
        }
    }
}
