package com.timofeeva.cyclesense.ml

import java.time.LocalDate
import kotlin.math.pow
import kotlin.math.sqrt

object PhaseInputBuilder {

    fun buildInputFromCycles(
        featureOrder: List<String>,
        dayDataByDate: Map<LocalDate, CycleDayData>,
        seqCycles: Int
    ): Array<Array<FloatArray>> {
        val groupedCycles = splitIntoCycles(
            dayDataByDate = dayDataByDate
        )

        val lastCompletedCycles = groupedCycles.takeLast(seqCycles)

        val result = Array(1) { Array(seqCycles) { FloatArray(featureOrder.size) } }

        val missingCycles = seqCycles - lastCompletedCycles.size

        for (cycleIndex in 0 until seqCycles) {
            val actualCycle = if (cycleIndex < missingCycles) {
                emptyList()
            } else {
                lastCompletedCycles[cycleIndex - missingCycles]
            }

            val featureMap = buildCycleFeatureMap(
                cycle = actualCycle,
                allCycles = groupedCycles
            )

            for (featureIdx in featureOrder.indices) {
                result[0][cycleIndex][featureIdx] = featureMap[featureOrder[featureIdx]] ?: 0f
            }
        }

        return result
    }

    private fun splitIntoCycles(
        dayDataByDate: Map<LocalDate, CycleDayData>
    ): List<List<CycleDayData>> {
        val sortedDays = dayDataByDate
            .toList()
            .sortedBy { it.first }
            .map { it.second }

        if (sortedDays.isEmpty()) return emptyList()

        val grouped = mutableListOf<MutableList<CycleDayData>>()
        var current = mutableListOf<CycleDayData>()
        var previousDayInCycle = -1

        for (day in sortedDays) {
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

    private fun buildCycleFeatureMap(
        cycle: List<CycleDayData>,
        allCycles: List<List<CycleDayData>>
    ): Map<String, Float> {
        if (cycle.isEmpty()) return emptyMap()

        val currentCycleLength = cycle.firstOrNull()?.cycleLength?.toFloat() ?: 28f
        val predictedParams = CycleParamsPredictor.predictFromCycles(allCycles)

        val cycleLengths = allCycles
            .filter { it.isNotEmpty() }
            .mapNotNull { it.firstOrNull()?.cycleLength?.toFloat() }

        val avgCycleLengthUser = cycleLengths.average().toFloat().takeIf { !it.isNaN() } ?: currentCycleLength
        val avgMensesLengthUser = estimateAllMensesLengths(allCycles).averageOrNull()?.toFloat()
            ?: predictedParams.predictedMensesLength.toFloat()

        val avgLutealLengthUser = cycleLengths
            .map { (it - predictedParams.predictedOvulationDay).coerceAtLeast(7f) }
            .average()
            .toFloat()

        val cycleLen = currentCycleLength
        val ovulationDay = predictedParams.predictedOvulationDay.toFloat()
        val lutealLen = (cycleLen - ovulationDay).coerceAtLeast(7f)
        val mensesLen = predictedParams.predictedMensesLength.toFloat()

        val valuesByPhase = mapOf(
            "menstrual" to cycle.filter { inferPhase(it.dayInCycle, it.cycleLength) == "menstrual" },
            "follicular" to cycle.filter { inferPhase(it.dayInCycle, it.cycleLength) == "follicular" },
            "ovulatory" to cycle.filter { inferPhase(it.dayInCycle, it.cycleLength) == "ovulatory" },
            "luteal" to cycle.filter { inferPhase(it.dayInCycle, it.cycleLength) == "luteal" }
        )

        val result = mutableMapOf<String, Float>()

        // cycle-level признаки
        result["cycle_len"] = cycleLen
        result["ovulation_day"] = ovulationDay
        result["luteal_len"] = lutealLen
        result["menses_len"] = mensesLen
        result["avg_cycle_length_user"] = avgCycleLengthUser
        result["avg_menses_length_user"] = avgMensesLengthUser
        result["avg_luteal_length_user"] = avgLutealLengthUser
        result["predicted_ovulation_day"] = predictedParams.predictedOvulationDay.toFloat()

        // отсутствующие synthetic-only признаки — нейтральными значениями
        result["age"] = 25f
        result["weight_kg"] = 60f
        result["height_cm"] = 165f
        result["bmi"] = 22f
        result["bmr_estimate"] = 1350f
        result["cycle_latent_state"] = 0f
        result["cycle_shock_flag"] = 0f
        result["cycle_profile_strength"] = 1f
        result["cycle_recovery_factor"] = 1f
        result["profile_type"] = 0f

        val metricExtractors = mapOf<String, (CycleDayData) -> Float?>(
            "steps" to { it.steps },
            "calories" to { null },
            "heart_rate" to { it.heartRate },
            "rmssd" to { it.rmssd },
            "sleep_hours" to { it.sleepHours },
            "stress_score_cycle" to { it.stressScore },
            "energy_level" to { it.energyLevel },
            "mood_score" to { it.moodScore },
            "concentration_score" to { it.concentrationScore },
            "pain_level" to { it.painLevel },
            "flare_magnitude" to { null },
            "episode_burden" to { null }
        )

        for ((phaseName, phaseDays) in valuesByPhase) {
            for ((metricName, extractor) in metricExtractors) {
                val values = phaseDays.mapNotNull(extractor)

                result["${metricName}_${phaseName}_mean"] = mean(values)
                result["${metricName}_${phaseName}_std"] = std(values)
                result["${metricName}_${phaseName}_min"] = min(values)
                result["${metricName}_${phaseName}_max"] = max(values)
            }

            val painValues = phaseDays.mapNotNull { it.painLevel }
            val stressValues = phaseDays.mapNotNull { it.stressScore }
            val energyValues = phaseDays.mapNotNull { it.energyLevel }

            result["bad_day_count_${phaseName}"] = 0f
            result["bad_day_ratio_${phaseName}"] = 0f
            result["flare_mean_${phaseName}"] = 0f
            result["flare_max_${phaseName}"] = 0f
            result["high_pain_ratio_${phaseName}"] = ratio(painValues) { it >= 7f }
            result["high_stress_ratio_${phaseName}"] = ratio(stressValues) { it >= 7f }
            result["low_energy_ratio_${phaseName}"] = ratio(energyValues) { it <= 3f }
            result["phase_day_count_${phaseName}"] = phaseDays.size.toFloat()
        }

        return result
    }

    private fun estimateAllMensesLengths(allCycles: List<List<CycleDayData>>): List<Int> {
        return allCycles.mapNotNull { cycle ->
            if (cycle.isEmpty()) return@mapNotNull null
            var count = 0
            for (day in cycle.sortedBy { it.dayInCycle }) {
                val phase = inferPhase(day.dayInCycle, day.cycleLength)
                if (phase == "menstrual") count++ else break
            }
            if (count > 0) count else null
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

    private fun mean(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        return values.average().toFloat()
    }

    private fun std(values: List<Float>): Float {
        if (values.size < 2) return 0f
        val m = values.average().toFloat()
        val variance = values.map { (it - m).pow(2) }.average().toFloat()
        return sqrt(variance)
    }

    private fun min(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        return values.minOrNull() ?: 0f
    }

    private fun max(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        return values.maxOrNull() ?: 0f
    }

    private fun ratio(values: List<Float>, predicate: (Float) -> Boolean): Float {
        if (values.isEmpty()) return 0f
        val count = values.count(predicate)
        return count.toFloat() / values.size.toFloat()
    }

    private fun Iterable<Int>.averageOrNull(): Double? {
        val list = this.toList()
        if (list.isEmpty()) return null
        return list.average()
    }
}