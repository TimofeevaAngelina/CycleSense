package com.timofeeva.cyclesense

import com.timofeeva.cyclesense.data.local.entity.CycleDayEntity
import com.timofeeva.cyclesense.data.local.entity.CycleEntity
import com.timofeeva.cyclesense.data.repository.CycleRepositoryRoom
import java.time.LocalDate

data class MenstruationEditToggleResult(
    val dates: Set<LocalDate>,
    val statusText: String? = null
)

class MainMenstruationEditActions(
    private val roomRepository: CycleRepositoryRoom,
    private val calendarDataService: CalendarDataService
) {
    suspend fun loadExistingMenstruationDates(
        profileId: Long,
        today: LocalDate = LocalDate.now()
    ): Set<LocalDate> {
        calendarDataService.ensureCalendarGeneratedUpToToday(profileId)

        return roomRepository
            .getDaysForProfile(profileId)
            .filter { day ->
                day.isMenstruationMarked &&
                        !LocalDate.parse(day.date).isAfter(today)
            }
            .map { LocalDate.parse(it.date) }
            .toSet()
    }

    fun toggleMenstruationDate(
        date: LocalDate,
        editingMenstruationDates: Set<LocalDate>,
        predictedMensesLength: Int,
        today: LocalDate = LocalDate.now()
    ): MenstruationEditToggleResult {
        if (date.isAfter(today)) {
            return MenstruationEditToggleResult(
                dates = editingMenstruationDates,
                statusText = "Будущие дни нельзя отмечать как фактическую менструацию"
            )
        }

        if (editingMenstruationDates.contains(date)) {
            return MenstruationEditToggleResult(
                dates = editingMenstruationDates - date
            )
        }

        val shouldAutoFill = canAutoFillMenstruationFromDate(
            startDate = date,
            predictedMensesLength = predictedMensesLength,
            existingDates = editingMenstruationDates,
            today = today
        )

        val newDates = if (shouldAutoFill) {
            val autoDates = (0 until predictedMensesLength)
                .map { offset -> date.plusDays(offset.toLong()) }
                .filter { !it.isAfter(today) }
                .toSet()

            editingMenstruationDates + autoDates
        } else {
            editingMenstruationDates + date
        }

        return MenstruationEditToggleResult(
            dates = newDates
        )
    }

    suspend fun saveMenstruationDates(
        profileId: Long,
        editingMenstruationDates: Set<LocalDate>,
        today: LocalDate = LocalDate.now()
    ) {
        val normalizedEditingDates = normalizeMenstruationDates(editingMenstruationDates)
            .filter { !it.isAfter(today) }
            .toSet()

        val allExistingDays = roomRepository.getDaysForProfile(profileId)
        val allExistingDates = allExistingDays.map { LocalDate.parse(it.date) }

        val minDateForRebuild = listOfNotNull(
            normalizedEditingDates.minOrNull(),
            allExistingDates.minOrNull()
        ).minOrNull()

        if (minDateForRebuild != null) {
            calendarDataService.ensureTechnicalDaysBetween(
                profileId = profileId,
                fromDate = minDateForRebuild,
                toDate = today
            )
        }

        var cycles = roomRepository.getCyclesForProfile(profileId)
            .sortedBy { it.startDate }

        if (cycles.isEmpty()) {
            val firstDate = normalizedEditingDates.minOrNull() ?: today

            roomRepository.insertCycle(
                CycleEntity(
                    userProfileId = profileId,
                    startDate = firstDate.toString(),
                    endDate = null,
                    cycleLength = null,
                    predictedCycleLength = 28,
                    predictedMensesLength = 5,
                    predictedOvulationDay = 14,
                    isCompleted = false
                )
            )

            cycles = roomRepository.getCyclesForProfile(profileId)
                .sortedBy { it.startDate }
        }

        val fallbackCycleId = cycles.lastOrNull()?.id
            ?: error("Не удалось найти цикл для сохранения")

        val existingDaysBeforeInsert = roomRepository.getDaysForProfile(profileId)
        val existingDatesBeforeInsert = existingDaysBeforeInsert
            .map { LocalDate.parse(it.date) }
            .toSet()

        val missingMarkedDates = normalizedEditingDates
            .filter { date -> !existingDatesBeforeInsert.contains(date) }
            .sorted()

        missingMarkedDates.forEach { date ->
            roomRepository.insertDay(
                CycleDayEntity(
                    cycleId = fallbackCycleId,
                    date = date.toString(),
                    dayInCycle = 1,
                    phase = "menstrual",

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

                    isMenstruationMarked = true,
                    notes = null
                )
            )
        }

        val days = roomRepository.getDaysForProfile(profileId)

        days.forEach { day ->
            val date = LocalDate.parse(day.date)

            if (!date.isAfter(today)) {
                val shouldBeMarked = normalizedEditingDates.contains(date)

                if (day.isMenstruationMarked != shouldBeMarked) {
                    roomRepository.updateDay(
                        day.copy(
                            isMenstruationMarked = shouldBeMarked,
                            phase = if (shouldBeMarked) "menstrual" else day.phase
                        )
                    )
                }
            }
        }

        calendarDataService.rebuildCyclesAfterMenstruationChanges(profileId)
        calendarDataService.ensureCalendarGeneratedUpToToday(profileId)
    }
}