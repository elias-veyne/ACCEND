package com.example.data.db

import androidx.room.TypeConverter

class AccendConverters {
    @TypeConverter
    fun fromMuscleGroup(value: MuscleGroup): String = value.name

    @TypeConverter
    fun toMuscleGroup(value: String): MuscleGroup =
        runCatching { MuscleGroup.valueOf(value) }.getOrDefault(MuscleGroup.RECOVERY)

    @TypeConverter
    fun fromTargetUnit(value: TargetUnit): String = value.name

    @TypeConverter
    fun toTargetUnit(value: String): TargetUnit =
        runCatching { TargetUnit.valueOf(value) }.getOrDefault(TargetUnit.REPS)
}
