package com.timofeeva.cyclesense.analytics

import com.timofeeva.cyclesense.ml.CycleDayData

data class CycleTrendPoint(
    val day: Int,
    val stress: Float,
    val energy: Float,
    val mood: Float,
    val concentration: Float,
    val pain: Float,
    val observationsCount: Int = 0
)

data class CycleTrendAnalyticsResult(
    val points: List<CycleTrendPoint>,

    // Для совместимости со старым кодом: количество завершённых циклов
    val cyclesUsed: Int,

    val normalizedLength: Int,

    // Новый честный источник: завершённые циклы + текущий цикл, если он есть
    val sourceCyclesUsed: Int = cyclesUsed,
    val completedCyclesUsed: Int = cyclesUsed
)

class CycleTrendAnalyticsService {

    fun buildTypicalCycleTrend(
        allCycles: List<List<CycleDayData>>,
        maxCompletedCycles: Int = 3,
        normalizedLength: Int = 28
    ): CycleTrendAnalyticsResult {
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
            throw IllegalStateException("Нет завершённых циклов для аналитики по дням")
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

        val currentCycleResampled: List<CycleTrendPointSource?> =
            if (currentCycle != null) {
                resampleCurrentCycleOnlyObservedDays(
                    cycle = currentCycle,
                    normalizedLength = effectiveNormalizedLength
                )
            } else {
                List(effectiveNormalizedLength) { null }
            }

        val points = (0 until effectiveNormalizedLength).map { index ->
            val stressValues = completedResampledCycles.mapNotNull { it.getOrNull(index)?.stressScore }.toMutableList()
            val energyValues = completedResampledCycles.mapNotNull { it.getOrNull(index)?.energyLevel }.toMutableList()
            val moodValues = completedResampledCycles.mapNotNull { it.getOrNull(index)?.moodScore }.toMutableList()
            val concentrationValues = completedResampledCycles.mapNotNull { it.getOrNull(index)?.concentrationScore }.toMutableList()
            val painValues = completedResampledCycles.mapNotNull { it.getOrNull(index)?.painLevel }.toMutableList()

            currentCycleResampled.getOrNull(index)?.let { currentPoint ->
                currentPoint.stress?.let { stressValues.add(it) }
                currentPoint.energy?.let { energyValues.add(it) }
                currentPoint.mood?.let { moodValues.add(it) }
                currentPoint.concentration?.let { concentrationValues.add(it) }
                currentPoint.pain?.let { painValues.add(it) }
            }

            val observationsCount = listOf(
                stressValues.size,
                energyValues.size,
                moodValues.size,
                concentrationValues.size,
                painValues.size
            ).maxOrNull() ?: 0

            CycleTrendPoint(
                day = index + 1,
                stress = stressValues.averageOrDefault(0f).coerceIn(1f, 10f),
                energy = energyValues.averageOrDefault(0f).coerceIn(1f, 10f),
                mood = moodValues.averageOrDefault(0f).coerceIn(1f, 10f),
                concentration = concentrationValues.averageOrDefault(0f).coerceIn(1f, 10f),
                pain = painValues.averageOrDefault(0f).coerceIn(1f, 10f),
                observationsCount = observationsCount
            )
        }

        return CycleTrendAnalyticsResult(
            points = points,
            cyclesUsed = completedCycles.size,
            normalizedLength = effectiveNormalizedLength,
            sourceCyclesUsed = sourceCycles.size,
            completedCyclesUsed = completedCycles.size
        )
    }

    private fun resampleCycle(
        cycle: List<CycleDayData>,
        normLen: Int
    ): List<CycleDayData> {
        val sorted = cycle.sortedBy { it.dayInCycle }
        if (sorted.isEmpty()) return emptyList()

        if (sorted.size == 1) {
            return List(normLen) { sorted.first() }
        }

        val oldX = FloatArray(sorted.size) { i ->
            i.toFloat() / (sorted.size - 1).toFloat()
        }

        val newX = FloatArray(normLen) { i ->
            i.toFloat() / (normLen - 1).toFloat()
        }

        return newX.mapIndexed { idx, x ->
            CycleDayData(
                dayInCycle = idx + 1,
                cycleLength = normLen,
                steps = interpolateNullable(x, oldX, sorted.map { it.steps }),
                heartRate = interpolateNullable(x, oldX, sorted.map { it.heartRate }),
                rmssd = interpolateNullable(x, oldX, sorted.map { it.rmssd }),
                sleepHours = interpolateNullable(x, oldX, sorted.map { it.sleepHours }),
                stressScore = interpolateNullable(x, oldX, sorted.map { it.stressScore }),
                energyLevel = interpolateNullable(x, oldX, sorted.map { it.energyLevel }),
                moodScore = interpolateNullable(x, oldX, sorted.map { it.moodScore }),
                concentrationScore = interpolateNullable(x, oldX, sorted.map { it.concentrationScore }),
                painLevel = interpolateNullable(x, oldX, sorted.map { it.painLevel }),
                phase = inferPhase(idx + 1, normLen)
            )
        }
    }

    private fun interpolateNullable(
        x: Float,
        oldX: FloatArray,
        values: List<Float?>
    ): Float? {
        val valid = values.mapIndexedNotNull { index, value ->
            if (value == null) null else oldX[index] to value
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
                val ratio = if (x1 - x0 == 0f) 0f else (x - x0) / (x1 - x0)
                return y0 + ratio * (y1 - y0)
            }
        }

        return ys.last()
    }

    private fun inferPhase(dayInCycle: Int, cycleLength: Int): String {
        val ovulationDay = (cycleLength - 14).coerceAtLeast(10)
        return when {
            dayInCycle <= 5 -> "menstrual"
            dayInCycle < ovulationDay - 1 -> "follicular"
            dayInCycle in (ovulationDay - 1)..(ovulationDay + 1) -> "ovulatory"
            else -> "luteal"
        }
    }

    private fun resampleCurrentCycleOnlyObservedDays(
        cycle: List<CycleDayData>,
        normalizedLength: Int
    ): List<CycleTrendPointSource?> {
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
                    CycleTrendPointSource(
                        stress = it.stressScore,
                        energy = it.energyLevel,
                        mood = it.moodScore,
                        concentration = it.concentrationScore,
                        pain = it.painLevel
                    )
                }
            }
        }
    }

    private fun List<Float>.averageOrDefault(defaultValue: Float): Float {
        if (isEmpty()) return defaultValue
        return average().toFloat()
    }

    private data class CycleTrendPointSource(
        val stress: Float?,
        val energy: Float?,
        val mood: Float?,
        val concentration: Float?,
        val pain: Float?
    )

}