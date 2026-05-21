package com.timofeeva.cyclesense.ml

import java.time.LocalDate

object CycleHistoryInputBuilder {

    fun buildNext3DaysInputForSelectedDate(
        featureOrder: List<String>,
        cycles: List<List<CycleDayData>>,
        selectedDate: LocalDate,
        dayDataByDate: Map<LocalDate, CycleDayData>,
        seqCycles: Int,
        maxCycleLen: Int
    ): Array<Array<FloatArray>> {
        val totalLen = (seqCycles + 1) * maxCycleLen
        val nFeatures = featureOrder.size
        val result = Array(1) { Array(totalLen) { FloatArray(nFeatures) } }

        val historyPairs = dayDataByDate
            .filterKeys { !it.isAfter(selectedDate) }
            .toList()
            .sortedBy { it.first }

        if (historyPairs.isEmpty()) {
            return result
        }

        val historyDays = historyPairs.map { it.second }
        val groupedCycles = splitIntoCycles(historyDays)

        if (groupedCycles.isEmpty()) {
            return result
        }

        val currentCycle = groupedCycles.last()
        val previousCycles = groupedCycles.dropLast(1).takeLast(seqCycles)

        val cycleBlocks = mutableListOf<List<CycleDayData>>()

        val missingPrevCycles = seqCycles - previousCycles.size
        repeat(missingPrevCycles.coerceAtLeast(0)) {
            cycleBlocks.add(emptyList())
        }

        cycleBlocks.addAll(previousCycles)
        cycleBlocks.add(currentCycle)

        for (blockIndex in cycleBlocks.indices) {
            val block = cycleBlocks[blockIndex]
            val isCurrentCycleBlock = blockIndex == cycleBlocks.lastIndex

            val preparedBlock = if (isCurrentCycleBlock) {
                prepareCurrentCycleBlock(
                    cycle = block,
                    maxCycleLen = maxCycleLen
                )
            } else {
                prepareFullCycleBlock(
                    cycle = block,
                    maxCycleLen = maxCycleLen
                )
            }

            val startOffset = blockIndex * maxCycleLen

            for (t in 0 until maxCycleLen) {
                val day = preparedBlock[t]
                val row = buildFeatureRow(featureOrder, day)
                result[0][startOffset + t] = row
            }
        }

        return result
    }

    private fun splitIntoCycles(days: List<CycleDayData>): List<List<CycleDayData>> {
        if (days.isEmpty()) return emptyList()

        val grouped = mutableListOf<MutableList<CycleDayData>>()
        var current = mutableListOf<CycleDayData>()
        var previousDayInCycle = -1

        for (day in days) {
            if (current.isNotEmpty() && day.dayInCycle <= previousDayInCycle) {
                grouped.add(current)
                current = mutableListOf()
            }

            current.add(day)
            previousDayInCycle = day.dayInCycle
        }

        if (current.isNotEmpty()) {
            grouped.add(current)
        }

        return grouped.map { cycle ->
            cycle.sortedBy { it.dayInCycle }
        }
    }

    private fun prepareFullCycleBlock(
        cycle: List<CycleDayData>,
        maxCycleLen: Int
    ): List<CycleDayData?> {
        val sorted = cycle.sortedBy { it.dayInCycle }
        val trimmed = if (sorted.size > maxCycleLen) {
            sorted.take(maxCycleLen)
        } else {
            sorted
        }

        val padded = trimmed.toMutableList<CycleDayData?>()

        while (padded.size < maxCycleLen) {
            padded.add(null)
        }

        return padded
    }

    private fun prepareCurrentCycleBlock(
        cycle: List<CycleDayData>,
        maxCycleLen: Int
    ): List<CycleDayData?> {
        val sorted = cycle.sortedBy { it.dayInCycle }
        val trimmed = if (sorted.size > maxCycleLen) {
            sorted.take(maxCycleLen)
        } else {
            sorted
        }

        val padded = trimmed.toMutableList<CycleDayData?>()

        while (padded.size < maxCycleLen) {
            padded.add(null)
        }

        return padded
    }

    private fun buildFeatureRow(
        featureOrder: List<String>,
        day: CycleDayData?
    ): FloatArray {
        val row = FloatArray(featureOrder.size)

        if (day == null) return row

        val rawMap = mutableMapOf<String, Float?>()

        val normDay = if (day.cycleLength > 0) {
            day.dayInCycle.toFloat() / day.cycleLength.toFloat()
        } else {
            0f
        }

        val phase = day.phase ?: inferPhase(day.dayInCycle, day.cycleLength)

        rawMap["steps"] = day.steps
        rawMap["heart_rate"] = day.heartRate
        rawMap["rmssd"] = day.rmssd
        rawMap["sleep_hours"] = day.sleepHours

        rawMap["stress_score_cycle"] = day.stressScore
        rawMap["energy_level"] = day.energyLevel
        rawMap["mood_score"] = day.moodScore
        rawMap["concentration_score"] = day.concentrationScore
        rawMap["pain_level"] = day.painLevel

        rawMap["day_in_cycle_norm"] = normDay
        rawMap["phase_menstrual"] = if (phase == "menstrual") 1f else 0f
        rawMap["phase_follicular"] = if (phase == "follicular") 1f else 0f
        rawMap["phase_ovulatory"] = if (phase == "ovulatory") 1f else 0f
        rawMap["phase_luteal"] = if (phase == "luteal") 1f else 0f

        for (i in featureOrder.indices) {
            row[i] = rawMap[featureOrder[i]] ?: 0f
        }

        return row
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
}