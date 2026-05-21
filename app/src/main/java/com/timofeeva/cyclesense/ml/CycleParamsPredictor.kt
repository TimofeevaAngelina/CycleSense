package com.timofeeva.cyclesense.ml

import kotlin.math.roundToInt

data class PredictedCycleParams(
    val predictedCycleLength: Int,
    val predictedMensesLength: Int,
    val predictedOvulationDay: Int
)

object CycleParamsPredictor {

    fun predictFromCycles(
        cycles: List<List<CycleDayData>>,
        fallbackCycleLength: Int = 28,
        fallbackMensesLength: Int = 5
    ): PredictedCycleParams {
        val completedCycles = cycles.filter { it.isNotEmpty() }

        val cycleLengths = completedCycles
            .mapNotNull { cycle -> cycle.firstOrNull()?.cycleLength }
            .takeLast(3)

        val predictedCycleLength = predictCycleLength(
            cycleLengths = cycleLengths,
            fallbackLen = fallbackCycleLength
        )

        val mensesLengths = completedCycles
            .map { cycle -> estimateMensesLength(cycle) }
            .filterNotNull()
            .takeLast(3)

        val predictedMensesLength = predictMensesLength(
            mensesLengths = mensesLengths,
            fallbackLen = fallbackMensesLength
        )

        val ovulationDays = completedCycles
            .map { cycle -> estimateOvulationDay(cycle) }
            .filterNotNull()
            .takeLast(3)

        val predictedOvulationDay = predictOvulationDay(
            ovulationDays = ovulationDays,
            predictedCycleLength = predictedCycleLength
        )

        return PredictedCycleParams(
            predictedCycleLength = predictedCycleLength,
            predictedMensesLength = predictedMensesLength,
            predictedOvulationDay = predictedOvulationDay
        )
    }

    private fun predictCycleLength(
        cycleLengths: List<Int>,
        fallbackLen: Int
    ): Int {
        if (cycleLengths.isEmpty()) return fallbackLen
        return medianInt(cycleLengths).coerceIn(21, 40)
    }

    private fun predictMensesLength(
        mensesLengths: List<Int>,
        fallbackLen: Int
    ): Int {
        if (mensesLengths.isEmpty()) return fallbackLen
        return medianInt(mensesLengths).coerceIn(2, 10)
    }

    private fun predictOvulationDay(
        ovulationDays: List<Int>,
        predictedCycleLength: Int
    ): Int {
        val raw = if (ovulationDays.isNotEmpty()) {
            medianInt(ovulationDays)
        } else {
            predictedCycleLength - 14
        }

        val maxDay = (predictedCycleLength - 10).coerceAtLeast(8)
        return raw.coerceIn(8, maxDay)
    }

    private fun estimateMensesLength(cycle: List<CycleDayData>): Int? {
        if (cycle.isEmpty()) return null

        var count = 0
        for (day in cycle.sortedBy { it.dayInCycle }) {
            val phase = day.phase ?: inferPhase(day.dayInCycle, day.cycleLength)
            if (phase == "menstrual") {
                count++
            } else {
                break
            }
        }

        return if (count > 0) count else null
    }

    private fun estimateOvulationDay(cycle: List<CycleDayData>): Int? {
        if (cycle.isEmpty()) return null

        val ovulatoryDays = cycle
            .filter {
                val phase = it.phase ?: inferPhase(it.dayInCycle, it.cycleLength)
                phase == "ovulatory"
            }
            .map { it.dayInCycle }

        if (ovulatoryDays.isNotEmpty()) {
            return ovulatoryDays.average().roundToInt()
        }

        val cycleLength = cycle.firstOrNull()?.cycleLength ?: return null
        return (cycleLength - 14).coerceAtLeast(8)
    }

    private fun inferPhase(dayInCycle: Int, cycleLength: Int): String {
        if (cycleLength <= 0) return "follicular"

        val ovulationDay = (cycleLength - 14).coerceAtLeast(10)

        return when {
            dayInCycle <= 5 -> "menstrual"
            dayInCycle < ovulationDay - 1 -> "follicular"
            dayInCycle in (ovulationDay - 1)..(ovulationDay + 1) -> "ovulatory"
            else -> "luteal"
        }
    }

    private fun medianInt(values: List<Int>): Int {
        val sorted = values.sorted()
        val n = sorted.size
        return if (n % 2 == 1) {
            sorted[n / 2]
        } else {
            ((sorted[n / 2 - 1] + sorted[n / 2]) / 2.0).roundToInt()
        }
    }
}