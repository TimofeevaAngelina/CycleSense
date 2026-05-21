package com.timofeeva.cyclesense.analytics

import com.timofeeva.cyclesense.ml.CycleDayData

class RelationshipAnalyticsService {

    fun buildRelationshipAnalytics(
        allCycles: List<List<CycleDayData>>,
        selectedMetrics: List<RelationshipMetric>,
        maxCompletedCycles: Int = 3,
        normalizedLength: Int = 28
    ): RelationshipAnalyticsResult {
        val cleanSelectedMetrics = selectedMetrics
            .distinct()
            .take(3)

        if (cleanSelectedMetrics.size < 2) {
            throw IllegalStateException("Выберите минимум 2 показателя для анализа взаимосвязей")
        }

        val nonEmptyCycles = allCycles
            .filter { it.isNotEmpty() }

        val completedCycles = nonEmptyCycles
            .filter { cycle ->
                val maxDay = cycle.maxOfOrNull { it.dayInCycle } ?: 0
                val cycleLength = cycle.firstOrNull()?.cycleLength ?: 0
                cycleLength > 0 && maxDay >= cycleLength
            }
            .takeLast(maxCompletedCycles)

        if (completedCycles.isEmpty()) {
            throw IllegalStateException("Нет завершённых циклов для анализа взаимосвязей")
        }

        val currentCycle = nonEmptyCycles
            .lastOrNull { cycle ->
                val maxDay = cycle.maxOfOrNull { it.dayInCycle } ?: 0
                val cycleLength = cycle.firstOrNull()?.cycleLength ?: 0
                cycleLength > 0 && maxDay < cycleLength
            }

        val sourceCycles = if (currentCycle != null) {
            completedCycles + listOf(currentCycle)
        } else {
            completedCycles
        }

        val effectiveNormalizedLength = sourceCycles
            .mapNotNull { cycle -> cycle.firstOrNull()?.cycleLength }
            .maxOrNull()
            ?.coerceIn(21, 40)
            ?: normalizedLength

        val completedResampledCycles = completedCycles.map { cycle ->
            resampleCycle(cycle, effectiveNormalizedLength)
        }

        val currentCycleResampled: List<RelationshipDaySource?> =
            if (currentCycle != null) {
                resampleCurrentCycleOnlyObservedDays(
                    cycle = currentCycle,
                    normalizedLength = effectiveNormalizedLength
                )
            } else {
                List(effectiveNormalizedLength) { null }
            }

        val series = cleanSelectedMetrics.map { metric ->
            val rawPoints = (0 until effectiveNormalizedLength).map { index ->
                val values = completedResampledCycles.mapNotNull { cycle ->
                    cycle.getOrNull(index)?.valueOf(metric)
                }.toMutableList()

                currentCycleResampled.getOrNull(index)?.let { currentPoint ->
                    currentPoint.valueOf(metric)?.let { values.add(it) }
                }

                RelationshipDayPoint(
                    day = index + 1,
                    rawValue = values.averageOrNull(),
                    normalizedValue = null,
                    observationsCount = values.size
                )
            }

            buildNormalizedSeries(
                metric = metric,
                rawPoints = rawPoints
            )
        }

        return RelationshipAnalyticsResult(
            selectedMetrics = cleanSelectedMetrics,
            series = series,
            sourceCyclesUsed = sourceCycles.size,
            completedCyclesUsed = completedCycles.size,
            normalizedLength = effectiveNormalizedLength
        )
    }

    private fun buildNormalizedSeries(
        metric: RelationshipMetric,
        rawPoints: List<RelationshipDayPoint>
    ): RelationshipMetricSeries {
        val values = rawPoints.mapNotNull { it.rawValue }

        if (values.isEmpty()) {
            return RelationshipMetricSeries(
                metric = metric,
                points = rawPoints,
                minRawValue = null,
                maxRawValue = null
            )
        }

        val minValue = values.minOrNull()
        val maxValue = values.maxOrNull()

        val range = if (minValue != null && maxValue != null) {
            maxValue - minValue
        } else {
            0f
        }

        val normalizedPoints = rawPoints.map { point ->
            val raw = point.rawValue

            val normalized = when {
                raw == null -> null
                minValue == null || maxValue == null -> null
                range <= 0f -> 50f
                else -> ((raw - minValue) / range * 100f).coerceIn(0f, 100f)
            }

            point.copy(
                normalizedValue = normalized
            )
        }

        return RelationshipMetricSeries(
            metric = metric,
            points = normalizedPoints,
            minRawValue = minValue,
            maxRawValue = maxValue
        )
    }

    private fun resampleCycle(
        cycle: List<CycleDayData>,
        normLen: Int
    ): List<RelationshipDaySource> {
        val sorted = cycle.sortedBy { it.dayInCycle }

        if (sorted.isEmpty()) return emptyList()

        if (sorted.size == 1) {
            val only = sorted.first()
            return List(normLen) {
                RelationshipDaySource.fromCycleDayData(only)
            }
        }

        val oldX = FloatArray(sorted.size) { i ->
            i.toFloat() / (sorted.size - 1).toFloat()
        }

        val newX = FloatArray(normLen) { i ->
            i.toFloat() / (normLen - 1).toFloat()
        }

        return newX.map { x ->
            RelationshipDaySource(
                stress = interpolateNullable(x, oldX, sorted.map { it.stressScore }),
                energy = interpolateNullable(x, oldX, sorted.map { it.energyLevel }),
                mood = interpolateNullable(x, oldX, sorted.map { it.moodScore }),
                concentration = interpolateNullable(x, oldX, sorted.map { it.concentrationScore }),
                pain = interpolateNullable(x, oldX, sorted.map { it.painLevel }),
                steps = interpolateNullable(x, oldX, sorted.map { it.steps }),
                calories = interpolateNullable(x, oldX, sorted.map { it.calories }),
                sleep = interpolateNullable(x, oldX, sorted.map { it.sleepHours }),
                heartRate = interpolateNullable(x, oldX, sorted.map { it.heartRate })
            )
        }
    }

    private fun resampleCurrentCycleOnlyObservedDays(
        cycle: List<CycleDayData>,
        normalizedLength: Int
    ): List<RelationshipDaySource?> {
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
                    RelationshipDaySource.fromCycleDayData(it)
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

    private fun List<Float>.averageOrNull(): Float? {
        if (isEmpty()) return null
        return average().toFloat()
    }

    private data class RelationshipDaySource(
        val stress: Float?,
        val energy: Float?,
        val mood: Float?,
        val concentration: Float?,
        val pain: Float?,
        val steps: Float?,
        val calories: Float?,
        val sleep: Float?,
        val heartRate: Float?
    ) {
        fun valueOf(metric: RelationshipMetric): Float? {
            return when (metric) {
                RelationshipMetric.STRESS -> stress
                RelationshipMetric.ENERGY -> energy
                RelationshipMetric.MOOD -> mood
                RelationshipMetric.CONCENTRATION -> concentration
                RelationshipMetric.PAIN -> pain
                RelationshipMetric.STEPS -> steps
                RelationshipMetric.CALORIES -> calories
                RelationshipMetric.SLEEP -> sleep
                RelationshipMetric.HEART_RATE -> heartRate
            }
        }

        companion object {
            fun fromCycleDayData(day: CycleDayData): RelationshipDaySource {
                return RelationshipDaySource(
                    stress = day.stressScore,
                    energy = day.energyLevel,
                    mood = day.moodScore,
                    concentration = day.concentrationScore,
                    pain = day.painLevel,
                    steps = day.steps,
                    calories = day.calories,
                    sleep = day.sleepHours,
                    heartRate = day.heartRate
                )
            }
        }
    }
}