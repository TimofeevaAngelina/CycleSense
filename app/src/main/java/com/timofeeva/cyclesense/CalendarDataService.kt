package com.timofeeva.cyclesense

import com.timofeeva.cyclesense.data.local.entity.CycleDayEntity
import com.timofeeva.cyclesense.data.local.entity.CycleEntity
import com.timofeeva.cyclesense.data.repository.CycleRepositoryRoom
import com.timofeeva.cyclesense.data.repository.RoomCycleMapper
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CalendarDataService(
    private val roomRepository: CycleRepositoryRoom
) {
    suspend fun ensureCalendarGeneratedUpToToday(profileId: Long) {
        val today = LocalDate.now()

        val dbCycles = roomRepository.getCyclesForProfile(profileId)
            .sortedBy { it.startDate }

        val dbDays = roomRepository.getDaysForProfile(profileId)
            .sortedBy { it.date }

        if (dbCycles.isEmpty() || dbDays.isEmpty()) {
            val newCycleId = roomRepository.insertCycle(
                CycleEntity(
                    userProfileId = profileId,
                    startDate = today.toString(),
                    endDate = null,
                    cycleLength = null,
                    predictedCycleLength = 28,
                    predictedMensesLength = 5,
                    predictedOvulationDay = 14,
                    isCompleted = false
                )
            )

            roomRepository.insertDay(
                CycleDayEntity(
                    cycleId = newCycleId,
                    date = today.toString(),
                    dayInCycle = 1,
                    phase = "unknown",

                    steps = null,
                    calories = null,
                    heartRate = null,
                    rmssd = null,
                    sleepHours = null,

                    stressScore = null,
                    energyLevel = null,
                    moodScore = null,
                    concentrationScore = null,
                    painLevel = null,

                    isMenstruationMarked = false,
                    notes = null
                )
            )

            return
        }

        val lastDay = dbDays.maxByOrNull { it.date } ?: return
        val lastDate = LocalDate.parse(lastDay.date)

        if (!lastDate.isBefore(today)) return

        val allCyclesForPrediction = RoomCycleMapper.buildCycles(
            cycles = dbCycles,
            days = dbDays
        )

        val timing = buildCycleTimingPrediction(
            allCycles = allCyclesForPrediction,
            dbCycles = dbCycles
        )

        val targetCycleId = lastDay.cycleId

        var date = lastDate.plusDays(1)
        var dayInCycle = lastDay.dayInCycle + 1

        val daysToInsert = mutableListOf<CycleDayEntity>()

        while (!date.isAfter(today)) {
            val phase = inferNonMenstrualPhase(
                dayInCycle = dayInCycle,
                predictedOvulationDay = timing.predictedOvulationDay
            )

            daysToInsert.add(
                CycleDayEntity(
                    cycleId = targetCycleId,
                    date = date.toString(),
                    dayInCycle = dayInCycle,
                    phase = phase,

                    steps = null,
                    calories = null,
                    heartRate = null,
                    rmssd = null,
                    sleepHours = null,

                    stressScore = null,
                    energyLevel = null,
                    moodScore = null,
                    concentrationScore = null,
                    painLevel = null,

                    isMenstruationMarked = false,
                    notes = null
                )
            )

            date = date.plusDays(1)
            dayInCycle += 1
        }

        if (daysToInsert.isNotEmpty()) {
            roomRepository.insertDays(daysToInsert)
        }
    }

    suspend fun ensureTechnicalDaysBetween(
        profileId: Long,
        fromDate: LocalDate,
        toDate: LocalDate
    ) {
        if (fromDate.isAfter(toDate)) return

        val cycles = roomRepository.getCyclesForProfile(profileId)
            .sortedBy { it.startDate }

        var fallbackCycleId = cycles.lastOrNull()?.id

        if (fallbackCycleId == null) {
            fallbackCycleId = roomRepository.insertCycle(
                CycleEntity(
                    userProfileId = profileId,
                    startDate = fromDate.toString(),
                    endDate = null,
                    cycleLength = null,
                    predictedCycleLength = 28,
                    predictedMensesLength = 5,
                    predictedOvulationDay = 14,
                    isCompleted = false
                )
            )
        }

        val existingDates = roomRepository.getDaysForProfile(profileId)
            .map { LocalDate.parse(it.date) }
            .toSet()

        val daysToInsert = mutableListOf<CycleDayEntity>()

        var date = fromDate
        while (!date.isAfter(toDate)) {
            if (!existingDates.contains(date)) {
                daysToInsert.add(
                    CycleDayEntity(
                        cycleId = fallbackCycleId,
                        date = date.toString(),
                        dayInCycle = 1,
                        phase = "unknown",

                        steps = null,
                        calories = null,
                        heartRate = null,
                        rmssd = null,
                        sleepHours = null,

                        stressScore = null,
                        energyLevel = null,
                        moodScore = null,
                        concentrationScore = null,
                        painLevel = null,

                        isMenstruationMarked = false,
                        notes = null
                    )
                )
            }

            date = date.plusDays(1)
        }

        if (daysToInsert.isNotEmpty()) {
            roomRepository.insertDays(daysToInsert)
        }
    }

    suspend fun rebuildCyclesAfterMenstruationChanges(profileId: Long) {
        val oldCycles = roomRepository.getCyclesForProfile(profileId)
            .sortedBy { it.startDate }

        val oldDays = roomRepository.getDaysForProfile(profileId)
            .sortedBy { it.date }

        if (oldDays.isEmpty()) return

        val previousAllCycles = RoomCycleMapper.buildCycles(
            cycles = oldCycles,
            days = oldDays
        )

        val timing = buildCycleTimingPrediction(
            allCycles = previousAllCycles,
            dbCycles = oldCycles
        )

        val sortedDays = oldDays.sortedBy { it.date }

        val cycleStartIndexes = mutableListOf<Int>()

        var lastMenstruationMarkedDate: LocalDate? = null

        sortedDays.forEachIndexed { index, day ->
            val isFirstDay = index == 0

            if (isFirstDay) {
                cycleStartIndexes.add(index)
            }

            if (day.isMenstruationMarked) {
                val currentDate = LocalDate.parse(day.date)
                val previousMarkedDate = lastMenstruationMarkedDate

                val isNewMenstruationStart = previousMarkedDate == null ||
                        ChronoUnit.DAYS.between(
                            previousMarkedDate,
                            currentDate
                        ) >= 7

                if (!isFirstDay && isNewMenstruationStart) {
                    cycleStartIndexes.add(index)
                }

                lastMenstruationMarkedDate = currentDate
            }
        }

        if (cycleStartIndexes.isEmpty()) {
            cycleStartIndexes.add(0)
        }

        val cycleSegments = cycleStartIndexes.mapIndexed { segmentIndex, startIndex ->
            val endIndexExclusive = cycleStartIndexes.getOrNull(segmentIndex + 1)
                ?: sortedDays.size

            sortedDays.subList(startIndex, endIndexExclusive)
        }.filter { it.isNotEmpty() }

        roomRepository.clearProfileData(profileId)

        cycleSegments.forEachIndexed { index, segment ->
            val isLastCycle = index == cycleSegments.lastIndex

            val firstMenstruationDayInSegment = segment
                .sortedBy { it.date }
                .firstOrNull { it.isMenstruationMarked }

            val hasKnownCycleStart = firstMenstruationDayInSegment != null

            val startDate = if (hasKnownCycleStart) {
                LocalDate.parse(firstMenstruationDayInSegment!!.date)
            } else {
                LocalDate.parse(segment.first().date)
            }

            val lastDate = LocalDate.parse(segment.last().date)

            val actualCycleLength = ChronoUnit.DAYS
                .between(startDate, lastDate)
                .toInt() + 1

            val actualLeadingMensesLength = segment
                .sortedBy { it.date }
                .takeWhile { it.isMenstruationMarked }
                .size
                .takeIf { it > 0 }

            val predictedMensesLength = if (isLastCycle) {
                timing.predictedMensesLength
            } else {
                (actualLeadingMensesLength ?: timing.predictedMensesLength).coerceIn(1, 12)
            }

            val predictedCycleLength = if (isLastCycle) {
                timing.predictedCycleLength
            } else {
                actualCycleLength.coerceIn(21, 100)
            }

            val latestPossibleOvulationDay = (predictedCycleLength - 10).coerceAtLeast(8)

            val predictedOvulationDay = (predictedCycleLength - 14)
                .coerceIn(8, latestPossibleOvulationDay)

            val newCycleId = roomRepository.insertCycle(
                CycleEntity(
                    userProfileId = profileId,
                    startDate = startDate.toString(),
                    endDate = if (isLastCycle) null else lastDate.toString(),
                    cycleLength = if (isLastCycle) null else actualCycleLength,
                    predictedCycleLength = predictedCycleLength,
                    predictedMensesLength = predictedMensesLength,
                    predictedOvulationDay = predictedOvulationDay,
                    isCompleted = !isLastCycle
                )
            )

            val segmentHasKnownCycleStart = hasKnownCycleStart

            val rebuiltDays = segment.map { oldDay ->
                val date = LocalDate.parse(oldDay.date)

                val newDayInCycle = ChronoUnit.DAYS
                    .between(startDate, date)
                    .toInt() + 1

                val newPhase = if (newDayInCycle <= 0) {
                    "unknown"
                } else {
                    inferPhaseAfterCycleRebuild(
                        dayInCycle = newDayInCycle,
                        isMenstruationMarked = oldDay.isMenstruationMarked,
                        hasKnownCycleStart = segmentHasKnownCycleStart,
                        predictedOvulationDay = predictedOvulationDay
                    )
                }

                oldDay.copy(
                    id = 0,
                    cycleId = newCycleId,
                    dayInCycle = newDayInCycle,
                    phase = newPhase
                )
            }

            roomRepository.insertDays(rebuiltDays)
        }
    }
}