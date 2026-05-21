package com.timofeeva.cyclesense.data.repository

import com.timofeeva.cyclesense.data.local.entity.CycleDayEntity
import com.timofeeva.cyclesense.data.local.entity.CycleEntity
import com.timofeeva.cyclesense.ml.CycleDayData
import java.time.LocalDate

object RoomCycleMapper {

    fun buildCycles(
        cycles: List<CycleEntity>,
        days: List<CycleDayEntity>
    ): List<List<CycleDayData>> {
        if (cycles.isEmpty() || days.isEmpty()) return emptyList()

        val daysByCycleId = days.groupBy { it.cycleId }

        return cycles
            .sortedBy { LocalDate.parse(it.startDate) }
            .mapNotNull { cycle ->
                val cycleDays = daysByCycleId[cycle.id].orEmpty()
                    .sortedBy { LocalDate.parse(it.date) }

                if (cycleDays.isEmpty()) {
                    null
                } else {
                    cycleDays.map { day ->
                        day.toCycleDayData(
                            fallbackCycleLength = cycle.cycleLength ?: cycle.predictedCycleLength ?: 28
                        )
                    }
                }
            }
    }

    fun buildDateMap(
        days: List<CycleDayEntity>,
        cycles: List<CycleEntity>
    ): Map<LocalDate, CycleDayData> {
        if (days.isEmpty()) return emptyMap()

        val cycleLengthById = cycles.associate { cycle ->
            cycle.id to (cycle.cycleLength ?: cycle.predictedCycleLength ?: 28)
        }

        return days.associate { day ->
            LocalDate.parse(day.date) to day.toCycleDayData(
                fallbackCycleLength = cycleLengthById[day.cycleId] ?: 28
            )
        }
    }

    private fun CycleDayEntity.toCycleDayData(
        fallbackCycleLength: Int
    ): CycleDayData {
        return CycleDayData(
            dayInCycle = dayInCycle,
            cycleLength = fallbackCycleLength,

            steps = steps,
            calories = calories,
            heartRate = heartRate,
            rmssd = rmssd,
            sleepHours = sleepHours,

            stressScore = stressScore,
            energyLevel = energyLevel,
            moodScore = moodScore,
            concentrationScore = concentrationScore,
            painLevel = painLevel,

            phase = phase.takeIf { it != "unknown" }
        )
    }
}