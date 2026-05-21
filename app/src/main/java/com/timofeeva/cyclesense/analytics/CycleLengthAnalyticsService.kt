package com.timofeeva.cyclesense.analytics

import com.timofeeva.cyclesense.data.local.entity.CycleEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

data class CycleLengthAnalyticsResult(
    val completedCyclesUsed: Int,
    val avgCycleLength: Int?,
    val minCycleLength: Int?,
    val maxCycleLength: Int?,
    val avgMensesLength: Int?,
    val minMensesLength: Int?,
    val maxMensesLength: Int?,
    val previousCycleLength: Int?,
    val previousMensesLength: Int?,
    val currentCycleDay: Int?,
    val currentPredictedCycleLength: Int?,
    val currentPredictedMensesLength: Int?,
    val expectedNextMensesDate: LocalDate?,
    val regularityText: String
)

class CycleLengthAnalyticsService {

    fun buildCycleLengthAnalytics(
        cycles: List<CycleEntity>,
        maxCompletedCycles: Int = 6,
        today: LocalDate = LocalDate.now()
    ): CycleLengthAnalyticsResult {
        val sortedCycles = cycles.sortedBy { it.startDate }

        val completedCycles = sortedCycles
            .filter { it.isCompleted }
            .filter { it.cycleLength != null }
            .takeLast(maxCompletedCycles)

        val cycleLengths = completedCycles
            .mapNotNull { it.cycleLength }
            .filter { it in 21..100 }

        val mensesLengths = completedCycles
            .mapNotNull { it.predictedMensesLength }
            .filter { it in 1..12 }

        val previousCompletedCycle = completedCycles.lastOrNull()

        val previousCycleLength = previousCompletedCycle
            ?.cycleLength
            ?.takeIf { it in 21..100 }

        val previousMensesLength = previousCompletedCycle
            ?.predictedMensesLength
            ?.takeIf { it in 1..12 }

        val currentCycle = sortedCycles.lastOrNull { !it.isCompleted }

        val currentCycleStart = currentCycle?.startDate
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        val currentCycleDay = currentCycleStart?.let { start ->
            ChronoUnit.DAYS.between(start, today).toInt() + 1
        }?.takeIf { it > 0 }

        val currentPredictedCycleLength = currentCycle
            ?.predictedCycleLength
            ?.coerceIn(21, 100)

        val currentPredictedMensesLength = currentCycle
            ?.predictedMensesLength
            ?.coerceIn(1, 12)

        val expectedNextMensesDate = if (
            currentCycleStart != null &&
            currentPredictedCycleLength != null
        ) {
            currentCycleStart.plusDays((currentPredictedCycleLength - 1).toLong())
        } else {
            null
        }

        return CycleLengthAnalyticsResult(
            completedCyclesUsed = cycleLengths.size,

            avgCycleLength = cycleLengths.averageRoundedOrNull(),
            minCycleLength = cycleLengths.minOrNull(),
            maxCycleLength = cycleLengths.maxOrNull(),

            avgMensesLength = mensesLengths.averageRoundedOrNull(),
            minMensesLength = mensesLengths.minOrNull(),
            maxMensesLength = mensesLengths.maxOrNull(),

            previousCycleLength = previousCycleLength,
            previousMensesLength = previousMensesLength,

            currentCycleDay = currentCycleDay,
            currentPredictedCycleLength = currentPredictedCycleLength,
            currentPredictedMensesLength = currentPredictedMensesLength,
            expectedNextMensesDate = expectedNextMensesDate,

            regularityText = buildRegularityText(cycleLengths)
        )
    }

    private fun buildRegularityText(cycleLengths: List<Int>): String {
        if (cycleLengths.size < 2) {
            return "Пока недостаточно завершённых циклов для оценки регулярности."
        }

        val min = cycleLengths.minOrNull() ?: return "Недостаточно данных."
        val max = cycleLengths.maxOrNull() ?: return "Недостаточно данных."
        val range = max - min

        return when {
            range <= 3 -> "Цикл выглядит достаточно стабильным."
            range <= 7 -> "Есть небольшие колебания длины цикла."
            range <= 14 -> "Цикл заметно меняется от месяца к месяцу."
            else -> "Цикл сильно варьируется, прогноз может быть менее точным."
        }
    }

    private fun List<Int>.averageRoundedOrNull(): Int? {
        if (isEmpty()) return null
        return average().roundToInt()
    }
}