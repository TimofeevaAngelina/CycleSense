package com.timofeeva.cyclesense.ml

import java.time.LocalDate

object NormalizedCycleInputBuilder {

    fun buildInputForSelectedDate(
        featureOrder: List<String>,
        dayDataByDate: Map<LocalDate, CycleDayData>,
        selectedDate: LocalDate,
        seqCycles: Int,
        normCycleLen: Int,
        neutralRawValues: Map<String, Float?> = emptyMap()
    ): Array<Array<FloatArray>> {
        val totalLen = seqCycles * normCycleLen
        val nFeatures = featureOrder.size
        val result = Array(1) { Array(totalLen) { FloatArray(nFeatures) } }

        val historyDays = dayDataByDate
            .filterKeys { !it.isAfter(selectedDate) }
            .toList()
            .sortedBy { it.first }
            .map { it.second }

        if (historyDays.isEmpty()) return result

        val groupedCycles = splitIntoCycles(historyDays)
        if (groupedCycles.isEmpty()) return result

        val selectedCycle = groupedCycles.last()
        val prevCycles = groupedCycles.dropLast(1)

        val cycleBlocks = mutableListOf<List<CycleDayData>>()

        val neededPrevCount = seqCycles - 1
        val prevTake = prevCycles.takeLast(neededPrevCount)
        val missingPrev = neededPrevCount - prevTake.size

        repeat(missingPrev.coerceAtLeast(0)) {
            cycleBlocks.add(emptyList())
        }

        cycleBlocks.addAll(prevTake)
        cycleBlocks.add(selectedCycle)

        for (blockIndex in cycleBlocks.indices) {
            val cycle = cycleBlocks[blockIndex]
            val normalized = resampleCycleToFixedLength(
                featureOrder = featureOrder,
                cycle = cycle,
                normLen = normCycleLen,
                neutralRawValues = neutralRawValues
            )

            val offset = blockIndex * normCycleLen
            for (i in 0 until normCycleLen) {
                result[0][offset + i] = normalized[i]
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

        if (current.isNotEmpty()) grouped.add(current)

        return grouped.map { it.sortedBy { d -> d.dayInCycle } }
    }

    private fun resampleCycleToFixedLength(
        featureOrder: List<String>,
        cycle: List<CycleDayData>,
        normLen: Int,
        neutralRawValues: Map<String, Float?> = emptyMap()
    ): Array<FloatArray> {
        val result = Array(normLen) { FloatArray(featureOrder.size) }

        if (cycle.isEmpty()) {
            val neutralRow = buildNeutralRow(featureOrder, neutralRawValues)
            for (i in 0 until normLen) {
                result[i] = neutralRow.copyOf()
            }
            return result
        }

        val sorted = cycle.sortedBy { it.dayInCycle }
        val oldX = if (sorted.size == 1) {
            floatArrayOf(0f)
        } else {
            FloatArray(sorted.size) { i -> i.toFloat() / (sorted.size - 1).toFloat() }
        }
        val newX = if (normLen == 1) {
            floatArrayOf(0f)
        } else {
            FloatArray(normLen) { i -> i.toFloat() / (normLen - 1).toFloat() }
        }

        for (featureIndex in featureOrder.indices) {
            val featureName = featureOrder[featureIndex]
            val rawValues = sorted.map { extractFeatureValue(it, featureName) ?: fallbackValue(featureName, neutralRawValues) }

            if (rawValues.size == 1) {
                for (i in 0 until normLen) {
                    result[i][featureIndex] = rawValues[0]
                }
            } else {
                for (i in 0 until normLen) {
                    result[i][featureIndex] = interpolate(
                        x = newX[i],
                        oldX = oldX,
                        oldY = rawValues
                    )
                }
            }
        }

        return result
    }

    private fun buildNeutralRow(
        featureOrder: List<String>,
        neutralRawValues: Map<String, Float?>
    ): FloatArray {
        val row = FloatArray(featureOrder.size)
        for (i in featureOrder.indices) {
            row[i] = fallbackValue(featureOrder[i], neutralRawValues)
        }
        return row
    }

    private fun fallbackValue(
        featureName: String,
        neutralRawValues: Map<String, Float?>
    ): Float {
        return when (featureName) {
            "day_in_cycle_norm" -> 0.5f
            "phase_menstrual" -> 0f
            "phase_follicular" -> 1f
            "phase_ovulatory" -> 0f
            "phase_luteal" -> 0f
            else -> neutralRawValues[featureName] ?: 0f
        }
    }

    private fun interpolate(
        x: Float,
        oldX: FloatArray,
        oldY: List<Float>
    ): Float {
        if (oldX.isEmpty() || oldY.isEmpty()) return 0f
        if (oldX.size == 1) return oldY[0]

        if (x <= oldX.first()) return oldY.first()
        if (x >= oldX.last()) return oldY.last()

        for (i in 0 until oldX.size - 1) {
            val x0 = oldX[i]
            val x1 = oldX[i + 1]
            if (x in x0..x1) {
                val y0 = oldY[i]
                val y1 = oldY[i + 1]
                val ratio = if (x1 - x0 == 0f) 0f else (x - x0) / (x1 - x0)
                return y0 + ratio * (y1 - y0)
            }
        }

        return oldY.last()
    }

    private fun extractFeatureValue(day: CycleDayData, featureName: String): Float? {
        val phase = day.phase ?: inferPhase(day.dayInCycle, day.cycleLength)
        val normDay = if (day.cycleLength > 0) {
            day.dayInCycle.toFloat() / day.cycleLength.toFloat()
        } else {
            0f
        }

        return when (featureName) {
            "steps" -> day.steps
            "heart_rate" -> day.heartRate
            "rmssd" -> day.rmssd
            "sleep_hours" -> day.sleepHours
            "stress_score_cycle" -> day.stressScore
            "energy_level" -> day.energyLevel
            "mood_score" -> day.moodScore
            "concentration_score" -> day.concentrationScore
            "pain_level" -> day.painLevel
            "day_in_cycle_norm" -> normDay
            "phase_menstrual" -> if (phase == "menstrual") 1f else 0f
            "phase_follicular" -> if (phase == "follicular") 1f else 0f
            "phase_ovulatory" -> if (phase == "ovulatory") 1f else 0f
            "phase_luteal" -> if (phase == "luteal") 1f else 0f
            else -> 0f
        }
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