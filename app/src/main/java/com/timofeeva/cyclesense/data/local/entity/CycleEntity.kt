package com.timofeeva.cyclesense.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cycles")
data class CycleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userProfileId: Long,
    val startDate: String,
    val endDate: String? = null,
    val cycleLength: Int? = null,
    val predictedCycleLength: Int? = null,
    val predictedMensesLength: Int? = null,
    val predictedOvulationDay: Int? = null,
    val isCompleted: Boolean = false
)