package com.timofeeva.cyclesense.ml

import android.content.Context
import java.time.LocalDate

data class AllNext3DaysResult(
    val stress: List<Float>,
    val energy: List<Float>,
    val mood: List<Float>,
    val concentration: List<Float>,
    val pain: List<Float>
)

class AllNext3DaysRunner(
    context: Context
) {
    private val stressRunner = Next3DaysRunner(context, "stress_score_cycle")
    private val energyRunner = Next3DaysRunner(context, "energy_level")
    private val moodRunner = Next3DaysRunner(context, "mood_score")
    private val concentrationRunner = Next3DaysRunner(context, "concentration_score")
    private val painRunner = Next3DaysRunner(context, "pain_level")

    fun predictFromSelectedDate(
        selectedDate: LocalDate,
        dayDataByDate: Map<LocalDate, CycleDayData>
    ): AllNext3DaysResult {
        return AllNext3DaysResult(
            stress = stressRunner.predictFromSelectedDate(selectedDate, dayDataByDate).values,
            energy = energyRunner.predictFromSelectedDate(selectedDate, dayDataByDate).values,
            mood = moodRunner.predictFromSelectedDate(selectedDate, dayDataByDate).values,
            concentration = concentrationRunner.predictFromSelectedDate(selectedDate, dayDataByDate).values,
            pain = painRunner.predictFromSelectedDate(selectedDate, dayDataByDate).values
        )
    }

    fun close() {
        stressRunner.close()
        energyRunner.close()
        moodRunner.close()
        concentrationRunner.close()
        painRunner.close()
    }
}