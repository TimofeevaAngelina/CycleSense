package com.timofeeva.cyclesense.ml

import android.content.Context
import java.time.LocalDate

class PredictionService(
    context: Context
) {
    private val runner = AllNext3DaysRunner(context)

    fun predictFromSelectedDate(
        selectedDate: LocalDate,
        dayDataByDate: Map<LocalDate, CycleDayData>
    ): AllNext3DaysResult {
        if (dayDataByDate.isEmpty()) {
            throw IllegalStateException("Нет данных для прогноза")
        }

        return runner.predictFromSelectedDate(
            selectedDate = selectedDate,
            dayDataByDate = dayDataByDate
        )
    }

    fun close() {
        runner.close()
    }
}