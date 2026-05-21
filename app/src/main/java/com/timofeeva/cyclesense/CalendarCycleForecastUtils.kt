package com.timofeeva.cyclesense

import com.timofeeva.cyclesense.data.local.entity.CycleEntity
import com.timofeeva.cyclesense.ml.CycleDayData
import com.timofeeva.cyclesense.ml.CycleParamsPredictor
import java.time.LocalDate
import kotlin.math.roundToInt

enum class FutureCycleMarkType {
    PREDICTED_MENSES,
    PREDICTED_OVULATION,
    DELAY
}

data class CycleTimingHint(
    val message: String,
    val delayDays: Int? = null
)

data class CycleTimingPrediction(
    val predictedCycleLength: Int,
    val predictedMensesLength: Int,
    val predictedOvulationDay: Int,
    val maxRecentCycleLength: Int?,
    val delayStartAfterDay: Int
)

fun buildCycleTimingPrediction(
    allCycles: List<List<CycleDayData>>,
    dbCycles: List<CycleEntity> = emptyList(),
    fallbackCycleLength: Int = 28,
    fallbackMensesLength: Int = 5
): CycleTimingPrediction {
    val safeFallbackCycleLength = fallbackCycleLength.coerceIn(21, 100)
    val safeFallbackMensesLength = fallbackMensesLength.coerceIn(1, 12)

    val completedCycleLengths = dbCycles
        .filter { it.isCompleted }
        .mapNotNull { it.cycleLength }
        .filter { it in 21..100 }
        .takeLast(6)

    val predictedCycleLength = when {
        completedCycleLengths.isNotEmpty() ->
            completedCycleLengths
                .takeLast(3)
                .average()
                .roundToInt()
                .coerceIn(21, 100)

        else ->
            safeFallbackCycleLength
    }

    val historicalMensesLengths = dbCycles
        .filter { it.isCompleted }
        .mapNotNull { it.predictedMensesLength }
        .filter { it in 1..12 }
        .takeLast(6)

    val predictedMensesLength = when {
        historicalMensesLengths.isNotEmpty() ->
            medianInt(historicalMensesLengths).coerceIn(1, 12)

        else ->
            safeFallbackMensesLength
    }

    val latestPossibleOvulationDay = (predictedCycleLength - 10).coerceAtLeast(8)

    val predictedOvulationDay = (predictedCycleLength - 14)
        .coerceIn(8, latestPossibleOvulationDay)

    val maxRecentCycleLength = completedCycleLengths.maxOrNull()

    val delayStartAfterDay = if (maxRecentCycleLength != null) {
        maxRecentCycleLength + 2
    } else {
        predictedCycleLength + 5
    }

    return CycleTimingPrediction(
        predictedCycleLength = predictedCycleLength,
        predictedMensesLength = predictedMensesLength,
        predictedOvulationDay = predictedOvulationDay,
        maxRecentCycleLength = maxRecentCycleLength,
        delayStartAfterDay = delayStartAfterDay
    )
}

fun buildCycleTimingHint(
    currentDayInCycle: Int,
    timingPrediction: CycleTimingPrediction
): CycleTimingHint? {
    val predictedStartDay = timingPrediction.predictedCycleLength
    val daysUntilPredictedStart = predictedStartDay - currentDayInCycle
    val delayStartAfterDay = timingPrediction.delayStartAfterDay

    return when {
        daysUntilPredictedStart in 1..2 -> {
            CycleTimingHint(
                message = "Месячные могут начаться в ближайшие дни"
            )
        }

        daysUntilPredictedStart == 0 -> {
            CycleTimingHint(
                message = "Месячные могут начаться сегодня"
            )
        }

        currentDayInCycle > predictedStartDay &&
                currentDayInCycle <= delayStartAfterDay -> {
            CycleTimingHint(
                message = "Месячные могут начаться сегодня"
            )
        }

        currentDayInCycle > delayStartAfterDay -> {
            val delayDays = currentDayInCycle - delayStartAfterDay
            CycleTimingHint(
                message = formatDelayMessage(delayDays),
                delayDays = delayDays
            )
        }

        else -> null
    }
}

fun buildFutureCycleMarks(
    dayDataByDate: Map<LocalDate, CycleDayData>,
    allCycles: List<List<CycleDayData>>,
    dbCycles: List<CycleEntity> = emptyList(),
    maxDate: LocalDate,
    today: LocalDate = LocalDate.now(),
    fallbackCycleLength: Int = 28,
    fallbackMensesLength: Int = 5
): Map<LocalDate, FutureCycleMarkType> {
    if (dayDataByDate.isEmpty()) return emptyMap()

    val sortedDays = dayDataByDate
        .toList()
        .sortedBy { it.first }

    val groupedCycles = splitDatedDaysIntoCycles(sortedDays)
    val lastKnownCycle = groupedCycles.lastOrNull() ?: return emptyMap()
    val lastKnownDay = lastKnownCycle.lastOrNull() ?: return emptyMap()

    val timing = buildCycleTimingPrediction(
        allCycles = allCycles,
        dbCycles = dbCycles,
        fallbackCycleLength = fallbackCycleLength,
        fallbackMensesLength = fallbackMensesLength
    )

    val todayData = dayDataByDate[today]

    val isTodayDelay = todayData != null &&
            todayData.phase != "menstrual" &&
            todayData.dayInCycle > timing.delayStartAfterDay

    // Если уже задержка и начало нового цикла не отмечено,
    // будущий прогноз скрываем.
    if (isTodayDelay) {
        return emptyMap()
    }

    val result = linkedMapOf<LocalDate, FutureCycleMarkType>()

    val currentCycleStartDate = lastKnownDay.first.minusDays(
        (lastKnownDay.second.dayInCycle - 1).toLong()
    )

    val currentDayInCycle = todayData?.dayInCycle
    val todayIsMenstrual = todayData?.phase == "menstrual"

    // 1. Если текущий цикл уже начался месячными,
    // показываем оставшиеся прогнозные дни менструации текущего цикла.
    if (todayIsMenstrual && currentDayInCycle != null) {
        val remainingMensesStartDay = currentDayInCycle + 1

        for (dayInCycle in remainingMensesStartDay..timing.predictedMensesLength) {
            val date = currentCycleStartDate.plusDays((dayInCycle - 1).toLong())

            if (
                date.isAfter(today) &&
                !date.isAfter(maxDate) &&
                !dayDataByDate.containsKey(date)
            ) {
                result[date] = FutureCycleMarkType.PREDICTED_MENSES
            }
        }

        addCurrentCycleOvulationMarks(
            result = result,
            currentCycleStartDate = currentCycleStartDate,
            timing = timing,
            dayDataByDate = dayDataByDate,
            today = today,
            maxDate = maxDate
        )

        var nextCycleStart = currentCycleStartDate.plusDays(timing.predictedCycleLength.toLong())

        while (!nextCycleStart.isAfter(maxDate)) {
            addPredictedCycleMarks(
                result = result,
                cycleStart = nextCycleStart,
                timing = timing,
                dayDataByDate = dayDataByDate,
                today = today,
                maxDate = maxDate
            )

            nextCycleStart = nextCycleStart.plusDays(timing.predictedCycleLength.toLong())
        }

        return result
    }

    // 2. Если месячные ещё не отмечены, но ожидаемое окно уже близко/идёт,
    // показываем ближайший возможный прогноз месячных.
    val expectedMensesStart = currentCycleStartDate
        .plusDays(timing.predictedCycleLength.toLong())

    val isNearOrInsideExpectedMensesWindow =
        todayData != null &&
                todayData.phase != "menstrual" &&
                todayData.dayInCycle >= (timing.predictedCycleLength - 2) &&
                todayData.dayInCycle <= timing.delayStartAfterDay

    if (isNearOrInsideExpectedMensesWindow) {
        val firstPredictedMensesStart = if (expectedMensesStart.isAfter(today)) {
            expectedMensesStart
        } else {
            today.plusDays(1)
        }

        addPredictedCycleMarks(
            result = result,
            cycleStart = firstPredictedMensesStart,
            timing = timing,
            dayDataByDate = dayDataByDate,
            today = today,
            maxDate = maxDate
        )

        var nextCycleStart = firstPredictedMensesStart.plusDays(timing.predictedCycleLength.toLong())

        while (!nextCycleStart.isAfter(maxDate)) {
            addPredictedCycleMarks(
                result = result,
                cycleStart = nextCycleStart,
                timing = timing,
                dayDataByDate = dayDataByDate,
                today = today,
                maxDate = maxDate
            )

            nextCycleStart = nextCycleStart.plusDays(timing.predictedCycleLength.toLong())
        }

        return result
    }

    // 3. Обычная ситуация: месячные ещё не близко.
    // Показываем текущую будущую овуляцию, если она ещё впереди,
    // и затем следующие циклы.
    addCurrentCycleOvulationMarks(
        result = result,
        currentCycleStartDate = currentCycleStartDate,
        timing = timing,
        dayDataByDate = dayDataByDate,
        today = today,
        maxDate = maxDate
    )

    var nextCycleStart = currentCycleStartDate.plusDays(timing.predictedCycleLength.toLong())

    while (!nextCycleStart.isAfter(maxDate)) {
        addPredictedCycleMarks(
            result = result,
            cycleStart = nextCycleStart,
            timing = timing,
            dayDataByDate = dayDataByDate,
            today = today,
            maxDate = maxDate
        )

        nextCycleStart = nextCycleStart.plusDays(timing.predictedCycleLength.toLong())
    }

    return result
}

fun buildDelayDatesForKnownDays(
    dayDataByDate: Map<LocalDate, CycleDayData>,
    allCycles: List<List<CycleDayData>>,
    dbCycles: List<com.timofeeva.cyclesense.data.local.entity.CycleEntity>,
    today: LocalDate = LocalDate.now(),
    fallbackCycleLength: Int = 28,
    fallbackMensesLength: Int = 5
): Set<LocalDate> {
    if (dayDataByDate.isEmpty()) return emptySet()

    val sortedDays = dayDataByDate
        .toList()
        .filter { (date, _) -> !date.isAfter(today) }
        .sortedBy { it.first }

    if (sortedDays.isEmpty()) return emptySet()

    val groupedCycles = splitDatedDaysIntoCycles(sortedDays)
    val currentCycle = groupedCycles.lastOrNull() ?: return emptySet()

    // Если в текущем цикле нет фактического начала месячных,
    // задержку не считаем: сначала нужно отметить начало последнего цикла.
    val hasMenstrualStartInCurrentCycle = currentCycle.any { (_, day) ->
        day.phase == "menstrual"
    }

    if (!hasMenstrualStartInCurrentCycle) return emptySet()

    val timing = buildCycleTimingPrediction(
        allCycles = allCycles,
        dbCycles = dbCycles,
        fallbackCycleLength = fallbackCycleLength,
        fallbackMensesLength = fallbackMensesLength
    )

    return currentCycle
        .filter { (_, day) ->
            day.phase != "menstrual" &&
                    day.dayInCycle > timing.delayStartAfterDay
        }
        .map { it.first }
        .toSet()
}

fun inferAutoGeneratedPhase(
    dayInCycle: Int,
    predictedCycleLength: Int,
    predictedMensesLength: Int,
    predictedOvulationDay: Int
): String {
    return when {
        dayInCycle <= predictedMensesLength -> "menstrual"
        dayInCycle < predictedOvulationDay - 1 -> "follicular"
        dayInCycle in (predictedOvulationDay - 1)..(predictedOvulationDay + 1) -> "ovulatory"
        else -> "luteal"
    }
}

private fun estimateLeadingMensesLength(cycle: List<CycleDayData>): Int? {
    if (cycle.isEmpty()) return null

    val count = cycle
        .sortedBy { it.dayInCycle }
        .takeWhile { day ->
            day.phase == "menstrual"
        }
        .size

    return if (count > 0) count else null
}

private fun splitDatedDaysIntoCycles(
    sortedDays: List<Pair<LocalDate, CycleDayData>>
): List<List<Pair<LocalDate, CycleDayData>>> {
    if (sortedDays.isEmpty()) return emptyList()

    val result = mutableListOf<MutableList<Pair<LocalDate, CycleDayData>>>()
    var current = mutableListOf<Pair<LocalDate, CycleDayData>>()
    var previousDayInCycle = -1

    for (item in sortedDays) {
        val dayInCycle = item.second.dayInCycle

        if (current.isNotEmpty() && dayInCycle <= previousDayInCycle) {
            result.add(current)
            current = mutableListOf()
        }

        current.add(item)
        previousDayInCycle = dayInCycle
    }

    if (current.isNotEmpty()) {
        result.add(current)
    }

    return result
}

private fun medianInt(values: List<Int>): Int {
    if (values.isEmpty()) return 0

    val sorted = values.sorted()
    val n = sorted.size

    return if (n % 2 == 1) {
        sorted[n / 2]
    } else {
        ((sorted[n / 2 - 1] + sorted[n / 2]) / 2.0).roundToInt()
    }
}

private fun formatDelayMessage(delayDays: Int): String {
    val word = when {
        delayDays % 10 == 1 && delayDays % 100 != 11 -> "день"
        delayDays % 10 in 2..4 && delayDays % 100 !in 12..14 -> "дня"
        else -> "дней"
    }

    return "Задержка $delayDays $word"
}

private fun addPredictedCycleMarks(
    result: MutableMap<LocalDate, FutureCycleMarkType>,
    cycleStart: LocalDate,
    timing: CycleTimingPrediction,
    dayDataByDate: Map<LocalDate, CycleDayData>,
    today: LocalDate,
    maxDate: LocalDate
) {
    for (dayOffset in 0 until timing.predictedMensesLength) {
        val date = cycleStart.plusDays(dayOffset.toLong())

        if (
            date.isAfter(today) &&
            !date.isAfter(maxDate) &&
            !dayDataByDate.containsKey(date)
        ) {
            result[date] = FutureCycleMarkType.PREDICTED_MENSES
        }
    }

    for (dayInCycle in (timing.predictedOvulationDay - 1)..(timing.predictedOvulationDay + 1)) {
        if (dayInCycle < 1) continue

        val date = cycleStart.plusDays((dayInCycle - 1).toLong())

        if (
            date.isAfter(today) &&
            !date.isAfter(maxDate) &&
            !dayDataByDate.containsKey(date)
        ) {
            result.putIfAbsent(date, FutureCycleMarkType.PREDICTED_OVULATION)
        }
    }
}

private fun addCurrentCycleOvulationMarks(
    result: MutableMap<LocalDate, FutureCycleMarkType>,
    currentCycleStartDate: LocalDate,
    timing: CycleTimingPrediction,
    dayDataByDate: Map<LocalDate, CycleDayData>,
    today: LocalDate,
    maxDate: LocalDate
) {
    for (dayInCycle in (timing.predictedOvulationDay - 1)..(timing.predictedOvulationDay + 1)) {
        if (dayInCycle < 1) continue

        val date = currentCycleStartDate.plusDays((dayInCycle - 1).toLong())

        if (!date.isAfter(maxDate)) {
            result.putIfAbsent(date, FutureCycleMarkType.PREDICTED_OVULATION)
        }
    }
}