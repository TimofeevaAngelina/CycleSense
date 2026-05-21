package com.timofeeva.cyclesense.ml

import android.content.Context
import java.time.LocalDate

class PhasePredictionService(
    context: Context
) {
    private val runner = PhaseRunner(context)



    fun predictNextCyclePhases(
        dayDataByDate: Map<LocalDate, CycleDayData>
    ): NextCyclePhasePrediction {
        if (dayDataByDate.isEmpty()) {
            throw IllegalStateException("Нет данных для фазового прогноза")
        }

        val featureOrder = runner.getInputFeatureOrder()
        val seqCycles = runner.getExpectedSeqCycles()

        val inputArray = PhaseInputBuilder.buildInputFromCycles(
            featureOrder = featureOrder,
            dayDataByDate = dayDataByDate,
            seqCycles = seqCycles
        )

        return runner.predictFromInput(inputArray)
    }

    fun close() {
        runner.close()
    }
}