package com.timofeeva.cyclesense

import com.timofeeva.cyclesense.ml.AllNext3DaysResult
import com.timofeeva.cyclesense.ml.CycleDayData
import com.timofeeva.cyclesense.ml.CycleParamsPredictor
import com.timofeeva.cyclesense.ml.ForecastDayLabel
import com.timofeeva.cyclesense.ml.ForecastDayLabeler
import com.timofeeva.cyclesense.ml.PredictedCycleParams
import com.timofeeva.cyclesense.ml.PredictionService
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val MIN_CURRENT_CYCLE_COMPLETE_SUBJECTIVE_DAYS = 3
private const val MIN_PREVIOUS_CYCLE_COMPLETE_SUBJECTIVE_DAYS = 3
private const val MAX_DAYS_SINCE_LAST_COMPLETE_SUBJECTIVE_ENTRY = 3
private const val SIMILAR_PREVIOUS_CYCLE_DAY_WINDOW = 3

data class MainPredictionBuildResult(
    val cycleParams: PredictedCycleParams,
    val prediction: AllNext3DaysResult,
    val forecastLabels: List<ForecastDayLabel>,
    val warningMessage: String? = null
)

private data class DatedCycleDay(
    val date: LocalDate,
    val data: CycleDayData
)
fun buildMainNext3DaysPrediction(
    predictionService: PredictionService,
    selectedDate: LocalDate,
    allCycles: List<List<CycleDayData>>,
    dayDataByDate: Map<LocalDate, CycleDayData>,
    timingPrediction: CycleTimingPrediction? = null
): MainPredictionBuildResult {
    if (allCycles.isEmpty() || dayDataByDate.isEmpty()) {
        error("Нет данных для прогноза.")
    }

    val selectedDayData = dayDataByDate[selectedDate]
        ?: error(
            "Для выбранной даты нет данных. Сначала добавьте запись дня или выберите дату с данными."
        )

    val warningMessage = validatePsychophysiologyDataForPrediction(
        selectedDate = selectedDate,
        dayDataByDate = dayDataByDate
    )

    val cycleParams = timingPrediction?.let { timing ->
        PredictedCycleParams(
            predictedCycleLength = timing.predictedCycleLength,
            predictedMensesLength = timing.predictedMensesLength,
            predictedOvulationDay = timing.predictedOvulationDay
        )
    } ?: CycleParamsPredictor.predictFromCycles(allCycles)

    val prediction = predictionService.predictFromSelectedDate(
        selectedDate = selectedDate,
        dayDataByDate = dayDataByDate
    )

    val labelBaseCurrentDay = selectedDayData.dayInCycle
        .coerceAtMost(cycleParams.predictedCycleLength)

    val labels = ForecastDayLabeler.buildNext3DayLabels(
        selectedDate = selectedDate,
        currentDay = labelBaseCurrentDay,
        predictedCycleLength = cycleParams.predictedCycleLength,
        predictedMensesLength = cycleParams.predictedMensesLength,
        predictedOvulationDay = cycleParams.predictedOvulationDay,
        forecastDays = 3
    )

    return MainPredictionBuildResult(
        cycleParams = cycleParams,
        prediction = prediction,
        forecastLabels = labels,
        warningMessage = warningMessage
    )
}

private fun validatePsychophysiologyDataForPrediction(
    selectedDate: LocalDate,
    dayDataByDate: Map<LocalDate, CycleDayData>
): String? {
    val selectedDayData = dayDataByDate[selectedDate]
        ?: error(
            "Для выбранной даты нет данных. Сначала добавьте запись дня или выберите дату с данными."
        )

    val datedCycles = splitDatedDaysIntoCycles(dayDataByDate)

    val currentCycleIndex = datedCycles.indexOfFirst { cycle ->
        cycle.any { it.date == selectedDate }
    }

    if (currentCycleIndex < 0) {
        error("Не удалось определить текущий цикл для выбранной даты.")
    }

    val currentCycle = datedCycles[currentCycleIndex]
    val previousCycle = datedCycles.getOrNull(currentCycleIndex - 1)

    val currentCycleDaysUpToSelectedDate = currentCycle
        .filter { !it.date.isAfter(selectedDate) }

    val currentCycleCompleteSubjectiveDays = currentCycleDaysUpToSelectedDate
        .filter { it.data.hasCompleteSubjectiveData() }

    val requiredCurrentCycleDays = when {
        selectedDayData.dayInCycle <= 1 -> 1
        selectedDayData.dayInCycle == 2 -> 1
        else -> MIN_CURRENT_CYCLE_COMPLETE_SUBJECTIVE_DAYS
    }

    if (selectedDayData.dayInCycle <= 1 && !selectedDayData.hasCompleteSubjectiveData()) {
        error(
            "Для прогноза нужно полностью заполнить самочувствие за выбранный день: " +
                    "напряжение, энергию, настроение, концентрацию и дискомфорт."
        )
    }

    if (currentCycleCompleteSubjectiveDays.size < requiredCurrentCycleDays) {
        val message = when (selectedDayData.dayInCycle) {
            2 -> "Для прогноза нужен хотя бы 1 полностью заполненный день самочувствия в текущем цикле."
            else -> "Недостаточно полностью заполненных дней самочувствия в текущем цикле. " +
                    "Сейчас заполнено дней до выбранной даты: ${currentCycleCompleteSubjectiveDays.size}. " +
                    "Минимум для прогноза: $requiredCurrentCycleDays."
        }

        error(message)
    }

    val lastCompleteSubjectiveDate = currentCycleCompleteSubjectiveDays
        .maxOfOrNull { it.date }

    if (lastCompleteSubjectiveDate == null) {
        error(
            "В текущем цикле нет полностью заполненных дней самочувствия. " +
                    "Добавьте оценки напряжения, энергии, настроения, концентрации и дискомфорта."
        )
    }

    val daysSinceLastCompleteSubjectiveEntry = ChronoUnit.DAYS.between(
        lastCompleteSubjectiveDate,
        selectedDate
    )

    if (daysSinceLastCompleteSubjectiveEntry > MAX_DAYS_SINCE_LAST_COMPLETE_SUBJECTIVE_ENTRY) {
        error(
            "Последняя полностью заполненная запись самочувствия была $lastCompleteSubjectiveDate. " +
                    "Для прогноза нужны более свежие данные: не старше " +
                    "$MAX_DAYS_SINCE_LAST_COMPLETE_SUBJECTIVE_ENTRY дней от выбранной даты."
        )
    }

    if (previousCycle == null) {
        error(
            "Для прогноза нужен хотя бы один предыдущий цикл с полностью заполненными данными самочувствия."
        )
    }

    val previousCycleCompleteSubjectiveDays = previousCycle
        .filter { it.data.hasCompleteSubjectiveData() }

    if (previousCycleCompleteSubjectiveDays.size < MIN_PREVIOUS_CYCLE_COMPLETE_SUBJECTIVE_DAYS) {
        error(
            "Недостаточно полностью заполненных дней самочувствия в предыдущем цикле. " +
                    "Сейчас заполнено дней: ${previousCycleCompleteSubjectiveDays.size}. " +
                    "Минимум для прогноза: $MIN_PREVIOUS_CYCLE_COMPLETE_SUBJECTIVE_DAYS."
        )
    }

    val hasSimilarPreviousCycleArea = previousCycleCompleteSubjectiveDays.any { previousDay ->
        kotlin.math.abs(previousDay.data.dayInCycle - selectedDayData.dayInCycle) <=
                SIMILAR_PREVIOUS_CYCLE_DAY_WINDOW
    }

    return if (hasSimilarPreviousCycleArea) {
        null
    } else {
        "В предыдущем цикле нет полностью заполненных данных в похожей части цикла " +
                "(±$SIMILAR_PREVIOUS_CYCLE_DAY_WINDOW дня от выбранного дня). " +
                "Прогноз построен, но его точность может быть ниже."
    }
}

private fun splitDatedDaysIntoCycles(
    dayDataByDate: Map<LocalDate, CycleDayData>
): List<List<DatedCycleDay>> {
    val sortedDays = dayDataByDate
        .map { (date, data) ->
            DatedCycleDay(
                date = date,
                data = data
            )
        }
        .sortedBy { it.date }

    if (sortedDays.isEmpty()) return emptyList()

    val result = mutableListOf<MutableList<DatedCycleDay>>()
    var currentCycle = mutableListOf<DatedCycleDay>()
    var previousDayInCycle: Int? = null

    for (item in sortedDays) {
        val currentDayInCycle = item.data.dayInCycle

        if (
            currentCycle.isNotEmpty() &&
            previousDayInCycle != null &&
            currentDayInCycle <= previousDayInCycle
        ) {
            result.add(currentCycle)
            currentCycle = mutableListOf()
        }

        currentCycle.add(item)
        previousDayInCycle = currentDayInCycle
    }

    if (currentCycle.isNotEmpty()) {
        result.add(currentCycle)
    }

    return result
}

private fun CycleDayData.hasCompleteSubjectiveData(): Boolean {
    return stressScore != null &&
            energyLevel != null &&
            moodScore != null &&
            concentrationScore != null &&
            painLevel != null
}