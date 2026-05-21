package com.timofeeva.cyclesense.analytics

import com.timofeeva.cyclesense.data.local.entity.CycleDayEntity
import com.timofeeva.cyclesense.data.local.entity.CycleEntity
import com.timofeeva.cyclesense.data.repository.HeartRateDaySummary
import java.time.LocalDate

class HeartRateAnalyticsService {

    fun buildTypicalHeartRateTrend(
        cycles: List<CycleEntity>,
        days: List<CycleDayEntity>,
        heartRateSummaryByDate: Map<LocalDate, HeartRateDaySummary>,
        maxCompletedCycles: Int = 3
    ): HeartRateAnalyticsResult {
        val sortedCycles = cycles.sortedBy { it.startDate }

        val completedCycles = sortedCycles
            .filter { it.isCompleted }
            .takeLast(maxCompletedCycles)

        val currentCycle = sortedCycles
            .lastOrNull { !it.isCompleted }

        val sourceCycles = when {
            completedCycles.isNotEmpty() && currentCycle != null -> {
                completedCycles + currentCycle
            }

            completedCycles.isNotEmpty() -> {
                completedCycles
            }

            currentCycle != null -> {
                listOf(currentCycle)
            }

            else -> {
                sortedCycles.takeLast(1)
            }
        }

        val sourceCycleIds = sourceCycles
            .map { it.id }
            .toSet()

        if (sourceCycleIds.isEmpty()) {
            return HeartRateAnalyticsResult(
                cyclesUsed = 0,
                points = emptyList(),
                phasePoints = emptyList()
            )
        }

        val avgValuesByDay = mutableMapOf<Int, MutableList<Float>>()
        val minValuesByDay = mutableMapOf<Int, MutableList<Float>>()
        val maxValuesByDay = mutableMapOf<Int, MutableList<Float>>()

        val avgValuesByPhase = mutableMapOf<String, MutableList<Float>>()
        val minValuesByPhase = mutableMapOf<String, MutableList<Float>>()
        val maxValuesByPhase = mutableMapOf<String, MutableList<Float>>()

        days.filter { it.cycleId in sourceCycleIds }
            .forEach { day ->
                val date = LocalDate.parse(day.date)
                val summary = heartRateSummaryByDate[date]

                val avg = summary?.avgBpm?.toFloat() ?: day.heartRate
                val min = summary?.minBpm?.toFloat()
                val max = summary?.maxBpm?.toFloat()

                val phaseTitle = phaseTitleFromCode(day.phase)

                avg?.let {
                    avgValuesByDay.getOrPut(day.dayInCycle) { mutableListOf() }.add(it)
                }

                min?.let {
                    minValuesByDay.getOrPut(day.dayInCycle) { mutableListOf() }.add(it)
                }

                max?.let {
                    maxValuesByDay.getOrPut(day.dayInCycle) { mutableListOf() }.add(it)
                }

                if (phaseTitle != null) {
                    avg?.let {
                        avgValuesByPhase.getOrPut(phaseTitle) { mutableListOf() }.add(it)
                    }

                    min?.let {
                        minValuesByPhase.getOrPut(phaseTitle) { mutableListOf() }.add(it)
                    }

                    max?.let {
                        maxValuesByPhase.getOrPut(phaseTitle) { mutableListOf() }.add(it)
                    }
                }
            }

        val allDays = (
                avgValuesByDay.keys +
                        minValuesByDay.keys +
                        maxValuesByDay.keys
                ).toSortedSet()

        val points = allDays.mapNotNull { dayInCycle ->
            val avgValues = avgValuesByDay[dayInCycle].orEmpty()
            val minValues = minValuesByDay[dayInCycle].orEmpty()
            val maxValues = maxValuesByDay[dayInCycle].orEmpty()

            if (avgValues.isEmpty() && minValues.isEmpty() && maxValues.isEmpty()) {
                null
            } else {
                val fallbackMin = minValues.averageOrNull()
                val fallbackMax = maxValues.averageOrNull()

                val avgHeartRate = when {
                    avgValues.isNotEmpty() -> {
                        avgValues.average().toFloat()
                    }

                    fallbackMin != null && fallbackMax != null -> {
                        (fallbackMin + fallbackMax) / 2f
                    }

                    else -> {
                        null
                    }
                }

                if (avgHeartRate == null) {
                    null
                } else {
                    HeartRateTrendPoint(
                        dayInCycle = dayInCycle,
                        avgHeartRate = avgHeartRate,
                        minHeartRate = fallbackMin,
                        maxHeartRate = fallbackMax,
                        samplesCount = maxOf(
                            avgValues.size,
                            minValues.size,
                            maxValues.size
                        )
                    )
                }
            }
        }

        return HeartRateAnalyticsResult(
            cyclesUsed = sourceCycles.size,
            points = points,
            phasePoints = buildHeartRatePhasePoints(
                avgValuesByPhase = avgValuesByPhase,
                minValuesByPhase = minValuesByPhase,
                maxValuesByPhase = maxValuesByPhase
            )
        )
    }

    private fun List<Float>.averageOrNull(): Float? {
        if (isEmpty()) return null
        return average().toFloat()
    }

    private fun buildHeartRatePhasePoints(
        avgValuesByPhase: Map<String, List<Float>>,
        minValuesByPhase: Map<String, List<Float>>,
        maxValuesByPhase: Map<String, List<Float>>
    ): List<HeartRatePhasePoint> {
        val phaseOrder = listOf(
            "Менструальная",
            "Фолликулярная",
            "Овуляторная",
            "Лютеиновая"
        )

        return phaseOrder.map { phaseTitle ->
            val avgValues = avgValuesByPhase[phaseTitle].orEmpty()
            val minValues = minValuesByPhase[phaseTitle].orEmpty()
            val maxValues = maxValuesByPhase[phaseTitle].orEmpty()

            val avgMin = when {
                minValues.isNotEmpty() -> minValues.averageOrNull()
                avgValues.isNotEmpty() -> avgValues.averageOrNull()
                else -> null
            }

            val avgMax = when {
                maxValues.isNotEmpty() -> maxValues.averageOrNull()
                avgValues.isNotEmpty() -> avgValues.averageOrNull()
                else -> null
            }

            HeartRatePhasePoint(
                phaseTitle = phaseTitle,
                avgMinHeartRate = avgMin,
                avgMaxHeartRate = avgMax,
                observationsCount = maxOf(
                    avgValues.size,
                    minValues.size,
                    maxValues.size
                )
            )
        }
    }

    private fun phaseTitleFromCode(phase: String?): String? {
        return when (phase) {
            "menstrual" -> "Менструальная"
            "follicular" -> "Фолликулярная"
            "ovulatory" -> "Овуляторная"
            "luteal" -> "Лютеиновая"
            else -> null
        }
    }
}