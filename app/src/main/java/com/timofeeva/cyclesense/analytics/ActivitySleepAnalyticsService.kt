package com.timofeeva.cyclesense.analytics

import com.timofeeva.cyclesense.ml.CycleDayData

data class ActivitySleepPhasePoint(
    val phase: String,
    val phaseTitle: String,
    val avgSteps: Float?,
    val avgCalories: Float?,
    val avgSleepHours: Float?,
    val daysCount: Int
)

data class ActivitySleepDayPoint(
    val day: Int,
    val avgSteps: Float?,
    val avgCalories: Float?,
    val avgSleepHours: Float?,
    val observationsCount: Int
)

data class ActivitySleepAnalyticsResult(
    val phasePoints: List<ActivitySleepPhasePoint>,
    val dayPoints: List<ActivitySleepDayPoint>,

    // Завершённые циклы + текущий незавершённый цикл, если он есть
    val sourceCyclesUsed: Int,

    // Только завершённые циклы
    val completedCyclesUsed: Int,

    val normalizedLength: Int,
    val overallAvgSteps: Float?,
    val overallAvgCalories: Float?,
    val overallAvgSleepHours: Float?
)

class ActivitySleepAnalyticsService {

    fun buildActivitySleepAnalytics(
        allCycles: List<List<CycleDayData>>,
        maxCompletedCycles: Int = 3,
        normalizedLength: Int = 28
    ): ActivitySleepAnalyticsResult {
        val nonEmptyCycles = allCycles
            .filter { it.isNotEmpty() }

        val completedCycles = nonEmptyCycles
            .filter { cycle ->
                val maxDay = cycle.maxOfOrNull { it.dayInCycle } ?: 0
                val cycleLength = cycle.firstOrNull()?.cycleLength ?: 0
                cycleLength > 0 && maxDay >= cycleLength
            }
            .takeLast(maxCompletedCycles)

        val currentCycle = nonEmptyCycles
            .lastOrNull { cycle ->
                val maxDay = cycle.maxOfOrNull { it.dayInCycle } ?: 0
                val cycleLength = cycle.firstOrNull()?.cycleLength ?: 0
                cycleLength > 0 && maxDay < cycleLength
            }

        val sourceCycles = when {
            completedCycles.isNotEmpty() && currentCycle != null -> {
                completedCycles + listOf(currentCycle)
            }

            completedCycles.isNotEmpty() -> {
                completedCycles
            }

            currentCycle != null -> {
                listOf(currentCycle)
            }

            else -> {
                nonEmptyCycles.takeLast(1)
            }
        }

        if (sourceCycles.isEmpty()) {
            throw IllegalStateException("Нет данных для физиологической аналитики.")
        }

        val effectiveNormalizedLength = sourceCycles
            .mapNotNull { cycle -> cycle.firstOrNull()?.cycleLength }
            .maxOrNull()
            ?.coerceIn(21, 40)
            ?: normalizedLength

        // Для фазовой и общей статистики используем:
        // завершённые циклы + текущий цикл, если он есть.
        val sourceDays = sourceCycles.flatten()
        val phasePoints = buildPhasePoints(sourceDays)

        val completedResampledCycles = completedCycles.map { cycle ->
            resampleCycle(cycle, effectiveNormalizedLength)
        }

        val cycleForObservedDays = currentCycle
            ?: if (completedCycles.isEmpty()) {
                sourceCycles.lastOrNull()
            } else {
                null
            }

        val currentCycleResampled: List<ActivitySleepDayPointSource?> =
            if (cycleForObservedDays != null) {
                resampleCurrentCycleOnlyObservedDays(
                    cycle = cycleForObservedDays,
                    normalizedLength = effectiveNormalizedLength
                )
            } else {
                List(effectiveNormalizedLength) { null }
            }

        val dayPoints = (0 until effectiveNormalizedLength).map { index ->
            val stepsValues = completedResampledCycles.mapNotNull { cycle ->
                cycle.getOrNull(index)?.steps
            }.toMutableList()

            val caloriesValues = completedResampledCycles.mapNotNull { cycle ->
                cycle.getOrNull(index)?.calories
            }.toMutableList()

            val sleepValues = completedResampledCycles.mapNotNull { cycle ->
                cycle.getOrNull(index)?.sleepHours
            }.toMutableList()

            currentCycleResampled.getOrNull(index)?.let { currentPoint ->
                currentPoint.steps?.let { stepsValues.add(it) }
                currentPoint.calories?.let { caloriesValues.add(it) }
                currentPoint.sleepHours?.let { sleepValues.add(it) }
            }

            ActivitySleepDayPoint(
                day = index + 1,
                avgSteps = stepsValues.averageOrNull(),
                avgCalories = caloriesValues.averageOrNull(),
                avgSleepHours = sleepValues.averageOrNull(),
                observationsCount = maxOf(
                    stepsValues.size,
                    caloriesValues.size,
                    sleepValues.size
                )
            )
        }

        return ActivitySleepAnalyticsResult(
            phasePoints = phasePoints,
            dayPoints = dayPoints,
            sourceCyclesUsed = sourceCycles.size,
            completedCyclesUsed = completedCycles.size,
            normalizedLength = effectiveNormalizedLength,
            overallAvgSteps = sourceDays.mapNotNull { it.steps }.averageOrNull(),
            overallAvgCalories = sourceDays.mapNotNull { it.calories }.averageOrNull(),
            overallAvgSleepHours = sourceDays.mapNotNull { it.sleepHours }.averageOrNull()
        )
    }

    private fun buildPhasePoints(days: List<CycleDayData>): List<ActivitySleepPhasePoint> {
        val phaseOrder = listOf(
            "menstrual" to "Менструальная",
            "follicular" to "Фолликулярная",
            "ovulatory" to "Овуляторная",
            "luteal" to "Лютеиновая"
        )

        return phaseOrder.map { (phaseKey, phaseTitle) ->
            val phaseDays = days.filter { day ->
                val resolvedPhase = day.phase ?: inferPhase(
                    dayInCycle = day.dayInCycle,
                    cycleLength = day.cycleLength
                )
                resolvedPhase == phaseKey
            }

            ActivitySleepPhasePoint(
                phase = phaseKey,
                phaseTitle = phaseTitle,
                avgSteps = phaseDays.mapNotNull { it.steps }.averageOrNull(),
                avgCalories = phaseDays.mapNotNull { it.calories }.averageOrNull(),
                avgSleepHours = phaseDays.mapNotNull { it.sleepHours }.averageOrNull(),
                daysCount = phaseDays.size
            )
        }
    }

    private fun resampleCycle(
        cycle: List<CycleDayData>,
        normLen: Int
    ): List<ActivitySleepDayPointSource> {
        val sorted = cycle.sortedBy { it.dayInCycle }

        if (sorted.isEmpty()) return emptyList()

        if (sorted.size == 1) {
            val only = sorted.first()
            return List(normLen) {
                ActivitySleepDayPointSource(
                    steps = only.steps,
                    calories = only.calories,
                    sleepHours = only.sleepHours
                )
            }
        }

        val oldX = FloatArray(sorted.size) { i ->
            i.toFloat() / (sorted.size - 1).toFloat()
        }

        val newX = FloatArray(normLen) { i ->
            i.toFloat() / (normLen - 1).toFloat()
        }

        return newX.map { x ->
            ActivitySleepDayPointSource(
                steps = interpolateNullable(x, oldX, sorted.map { it.steps }),
                calories = interpolateNullable(x, oldX, sorted.map { it.calories }),
                sleepHours = interpolateNullable(x, oldX, sorted.map { it.sleepHours })
            )
        }
    }

    private fun resampleCurrentCycleOnlyObservedDays(
        cycle: List<CycleDayData>,
        normalizedLength: Int
    ): List<ActivitySleepDayPointSource?> {
        val sorted = cycle.sortedBy { it.dayInCycle }

        if (sorted.isEmpty()) {
            return List(normalizedLength) { null }
        }

        val cycleLength = sorted.firstOrNull()?.cycleLength ?: normalizedLength
        if (cycleLength <= 0) {
            return List(normalizedLength) { null }
        }

        val observedMaxDay = sorted.maxOfOrNull { it.dayInCycle } ?: 0

        return List(normalizedLength) { index ->
            val normalizedDay = index + 1

            val originalDay = kotlin.math.round(
                normalizedDay.toFloat() * cycleLength.toFloat() / normalizedLength.toFloat()
            ).toInt().coerceAtLeast(1)

            if (originalDay > observedMaxDay) {
                null
            } else {
                val nearestDay = sorted.minByOrNull { day ->
                    kotlin.math.abs(day.dayInCycle - originalDay)
                }

                nearestDay?.let {
                    ActivitySleepDayPointSource(
                        steps = it.steps,
                        calories = it.calories,
                        sleepHours = it.sleepHours
                    )
                }
            }
        }
    }

    private fun interpolateNullable(
        x: Float,
        oldX: FloatArray,
        values: List<Float?>
    ): Float? {
        val valid = values.mapIndexedNotNull { index, value ->
            if (value == null) {
                null
            } else {
                oldX[index] to value
            }
        }

        if (valid.isEmpty()) return null
        if (valid.size == 1) return valid.first().second

        val xs = valid.map { it.first }
        val ys = valid.map { it.second }

        if (x <= xs.first()) return ys.first()
        if (x >= xs.last()) return ys.last()

        for (i in 0 until xs.size - 1) {
            val x0 = xs[i]
            val x1 = xs[i + 1]

            if (x in x0..x1) {
                val y0 = ys[i]
                val y1 = ys[i + 1]
                val ratio = if (x1 - x0 == 0f) {
                    0f
                } else {
                    (x - x0) / (x1 - x0)
                }

                return y0 + ratio * (y1 - y0)
            }
        }

        return ys.last()
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

    private fun List<Float>.averageOrNull(): Float? {
        if (isEmpty()) return null
        return average().toFloat()
    }

    private data class ActivitySleepDayPointSource(
        val steps: Float?,
        val calories: Float?,
        val sleepHours: Float?
    )
}