package com.timofeeva.cyclesense.analytics

import com.timofeeva.cyclesense.ml.CycleDayData

data class PhaseMetricAnalytics(
    val stress: Float,
    val energy: Float,
    val mood: Float,
    val concentration: Float,
    val pain: Float
)

data class PhaseAnalyticsResult(
    val menstrual: PhaseMetricAnalytics,
    val follicular: PhaseMetricAnalytics,
    val ovulatory: PhaseMetricAnalytics,
    val luteal: PhaseMetricAnalytics,
    val cyclesUsed: Int
)

class PhaseAnalyticsService {

    fun buildPhaseAnalytics(
        allCycles: List<List<CycleDayData>>,
        maxCompletedCycles: Int = 3
    ): PhaseAnalyticsResult {
        val completedCycles = allCycles
            .filter { it.isNotEmpty() }
            .filter { cycle ->
                val maxDay = cycle.maxOfOrNull { it.dayInCycle } ?: 0
                val cycleLength = cycle.firstOrNull()?.cycleLength ?: 0
                maxDay >= cycleLength
            }
            .takeLast(maxCompletedCycles)

        if (completedCycles.isEmpty()) {
            throw IllegalStateException("Нет завершённых циклов для фазовой аналитики")
        }

        val weightedCycles = buildWeightedCycles(completedCycles)

        return PhaseAnalyticsResult(
            menstrual = buildPhaseMetric(weightedCycles, "menstrual"),
            follicular = buildPhaseMetric(weightedCycles, "follicular"),
            ovulatory = buildPhaseMetric(weightedCycles, "ovulatory"),
            luteal = buildPhaseMetric(weightedCycles, "luteal"),
            cyclesUsed = completedCycles.size
        )
    }

    private fun buildWeightedCycles(
        cycles: List<List<CycleDayData>>
    ): List<WeightedCycle> {
        val weights = when (cycles.size) {
            1 -> listOf(1.0f)
            2 -> listOf(0.4f, 0.6f)
            else -> {
                val lastThree = cycles.takeLast(3)
                return listOf(
                    WeightedCycle(lastThree[0], 0.2f),
                    WeightedCycle(lastThree[1], 0.3f),
                    WeightedCycle(lastThree[2], 0.5f)
                )
            }
        }

        return cycles.mapIndexed { index, cycle ->
            WeightedCycle(cycle, weights[index])
        }
    }

    private fun buildPhaseMetric(
        weightedCycles: List<WeightedCycle>,
        phase: String
    ): PhaseMetricAnalytics {
        val stress = weightedMean(weightedCycles, phase) { it.stressScore }
        val energy = weightedMean(weightedCycles, phase) { it.energyLevel }
        val mood = weightedMean(weightedCycles, phase) { it.moodScore }
        val concentration = weightedMean(weightedCycles, phase) { it.concentrationScore }
        val pain = weightedMean(weightedCycles, phase) { it.painLevel }

        return PhaseMetricAnalytics(
            stress = stress,
            energy = energy,
            mood = mood,
            concentration = concentration,
            pain = pain
        )
    }

    private fun weightedMean(
        weightedCycles: List<WeightedCycle>,
        phase: String,
        selector: (CycleDayData) -> Float?
    ): Float {
        var weightedSum = 0f
        var weightsSum = 0f

        for ((cycle, cycleWeight) in weightedCycles) {
            val phaseDays = cycle.filter { day ->
                val resolvedPhase = day.phase ?: inferPhase(day.dayInCycle, day.cycleLength)
                resolvedPhase == phase
            }

            val values = phaseDays.mapNotNull(selector)
            if (values.isEmpty()) continue

            val cycleMean = values.average().toFloat()
            weightedSum += cycleMean * cycleWeight
            weightsSum += cycleWeight
        }

        if (weightsSum == 0f) return 0f
        return (weightedSum / weightsSum).coerceIn(1f, 10f)
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

    private data class WeightedCycle(
        val cycle: List<CycleDayData>,
        val weight: Float
    )
}