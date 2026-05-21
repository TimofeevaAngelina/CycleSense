package com.timofeeva.cyclesense.ml

import java.time.LocalDate

data class ForecastDayLabel(
    val step: Int,
    val date: LocalDate,
    val cycleOffset: Int,
    val dayInCycle: Int,
    val phase: String
)

object ForecastDayLabeler {

    fun buildNext3DayLabels(
        selectedDate: LocalDate,
        currentDay: Int,
        predictedCycleLength: Int,
        predictedMensesLength: Int,
        predictedOvulationDay: Int,
        forecastDays: Int = 3
    ): List<ForecastDayLabel> {
        val safeCurrentDay = currentDay.coerceAtLeast(1)
        val safeCycleLength = predictedCycleLength.coerceAtLeast(21)
        val safeMensesLength = predictedMensesLength.coerceIn(1, safeCycleLength)
        val safeOvulationDay = predictedOvulationDay.coerceIn(8, safeCycleLength)

        return (1..forecastDays).map { step ->
            val absoluteFutureDay = safeCurrentDay + step
            val cycleOffset = (absoluteFutureDay - 1) / safeCycleLength
            val dayInCycle = ((absoluteFutureDay - 1) % safeCycleLength) + 1
            val date = selectedDate.plusDays(step.toLong())

            ForecastDayLabel(
                step = step,
                date = date,
                cycleOffset = cycleOffset,
                dayInCycle = dayInCycle,
                phase = inferPhase(
                    dayInCycle = dayInCycle,
                    mensesLength = safeMensesLength,
                    ovulationDay = safeOvulationDay,
                    cycleLength = safeCycleLength
                )
            )
        }
    }

    private fun inferPhase(
        dayInCycle: Int,
        mensesLength: Int,
        ovulationDay: Int,
        cycleLength: Int
    ): String {
        val ovulationStart = (ovulationDay - 1).coerceAtLeast(1)
        val ovulationEnd = (ovulationDay + 1).coerceAtMost(cycleLength)

        return when {
            dayInCycle <= mensesLength -> "menstrual"
            dayInCycle in ovulationStart..ovulationEnd -> "ovulatory"
            dayInCycle < ovulationStart -> "follicular"
            else -> "luteal"
        }
    }
}