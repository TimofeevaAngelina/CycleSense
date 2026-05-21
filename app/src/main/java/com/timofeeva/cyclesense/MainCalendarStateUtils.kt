package com.timofeeva.cyclesense

import com.timofeeva.cyclesense.ml.CycleDayData
import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun inferNonMenstrualPhase(
    dayInCycle: Int,
    predictedOvulationDay: Int
): String {
    return when {
        dayInCycle < predictedOvulationDay - 1 -> "follicular"
        dayInCycle in (predictedOvulationDay - 1)..(predictedOvulationDay + 1) -> "ovulatory"
        else -> "luteal"
    }
}

fun inferPhaseAfterCycleRebuild(
    dayInCycle: Int,
    isMenstruationMarked: Boolean,
    hasKnownCycleStart: Boolean,
    predictedOvulationDay: Int
): String {
    if (!hasKnownCycleStart) return "unknown"

    if (isMenstruationMarked) return "menstrual"

    return when {
        dayInCycle < predictedOvulationDay - 1 -> "follicular"
        dayInCycle in (predictedOvulationDay - 1)..(predictedOvulationDay + 1) -> "ovulatory"
        else -> "luteal"
    }
}

fun buildDelayHintForDate(
    date: LocalDate,
    dayData: CycleDayData?,
    delayDates: Set<LocalDate>,
    allDelayDates: Set<LocalDate>
): CycleTimingHint? {
    if (dayData == null) return null
    if (!delayDates.contains(date)) return null

    val sortedDelayDates = allDelayDates.sorted()
    val firstDelayDate = sortedDelayDates.firstOrNull() ?: return null

    val delayDays = ChronoUnit.DAYS
        .between(firstDelayDate, date)
        .toInt() + 1

    val word = when {
        delayDays % 10 == 1 && delayDays % 100 != 11 -> "день"
        delayDays % 10 in 2..4 && delayDays % 100 !in 12..14 -> "дня"
        else -> "дней"
    }

    return CycleTimingHint(
        message = "Задержка $delayDays $word",
        delayDays = delayDays
    )
}

fun normalizeMenstruationDates(
    dates: Set<LocalDate>
): Set<LocalDate> {
    return dates
}

fun canAutoFillMenstruationFromDate(
    startDate: LocalDate,
    predictedMensesLength: Int,
    existingDates: Set<LocalDate>,
    today: LocalDate
): Boolean {
    if (existingDates.isEmpty()) return true

    val blockDates = (0 until predictedMensesLength)
        .map { offset -> startDate.plusDays(offset.toLong()) }
        .filter { !it.isAfter(today) }

    if (blockDates.isEmpty()) return false

    val blockStart = blockDates.minOrNull() ?: return false
    val blockEnd = blockDates.maxOrNull() ?: return false

    val previousExistingDate = existingDates
        .filter { it.isBefore(blockStart) }
        .maxOrNull()

    val nextExistingDate = existingDates
        .filter { it.isAfter(blockEnd) }
        .minOrNull()

    val enoughGapBefore = previousExistingDate == null ||
            ChronoUnit.DAYS.between(
                previousExistingDate,
                blockStart
            ) >= 7

    val enoughGapAfter = nextExistingDate == null ||
            ChronoUnit.DAYS.between(
                blockEnd,
                nextExistingDate
            ) >= 7

    return enoughGapBefore && enoughGapAfter
}