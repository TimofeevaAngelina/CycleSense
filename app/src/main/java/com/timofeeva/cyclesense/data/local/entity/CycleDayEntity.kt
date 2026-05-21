package com.timofeeva.cyclesense.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cycle_days",
    foreignKeys = [
        ForeignKey(
            entity = CycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["cycleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["cycleId"]),
        Index(value = ["date"])
    ]
)
data class CycleDayEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val cycleId: Long,
    val date: String,

    val dayInCycle: Int,
    val phase: String? = null,

    val steps: Float? = null,
    val calories: Float? = null,
    val heartRate: Float? = null,
    val rmssd: Float? = null,
    val sleepHours: Float? = null,

    val stressScore: Float? = null,
    val energyLevel: Float? = null,
    val moodScore: Float? = null,
    val concentrationScore: Float? = null,
    val painLevel: Float? = null,

    val isMenstruationMarked: Boolean = false,
    val notes: String? = null
)