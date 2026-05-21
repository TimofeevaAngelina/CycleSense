package com.timofeeva.cyclesense

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Card
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.timofeeva.cyclesense.data.repository.HeartRateDaySummary
import com.timofeeva.cyclesense.ml.AllNext3DaysResult
import com.timofeeva.cyclesense.ml.CycleDayData
import com.timofeeva.cyclesense.ml.ForecastDayLabel
import com.timofeeva.cyclesense.ml.PredictedCycleParams
import java.time.LocalDate
import java.time.YearMonth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import com.timofeeva.cyclesense.ui.theme.CycleSenseTextSecondary
import androidx.compose.ui.text.font.FontWeight
import com.timofeeva.cyclesense.ui.theme.CycleSenseSurface


private val CalendarTabCardShape = 24.dp
private val CalendarTabCardInnerPadding = 16.dp
private val CalendarTabCardElevation = 3.dp
private val CalendarTabCardInnerGap = 10.dp

@Composable
fun CalendarTabContent(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    dayDataByDate: Map<LocalDate, CycleDayData>,
    selectedDateHeartRateSummary: HeartRateDaySummary?,
    selectedDateHasEntry: Boolean,
    statusText: String,
    predictedCycleParams: PredictedCycleParams?,
    result: AllNext3DaysResult?,
    forecastLabels: List<ForecastDayLabel>,
    predictionWarningMessage: String?,
    isPremiumActive: Boolean,
    futureCycleMarks: Map<LocalDate, FutureCycleMarkType>,
    minCalendarMonth: YearMonth,
    maxCalendarMonth: YearMonth,
    cycleTimingHint: CycleTimingHint?,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onBuildPredictionClick: () -> Unit,
    onEditDayClick: () -> Unit,
    isCycleCompletedByDate: Map<LocalDate, Boolean>,
    isMenstruationEditMode: Boolean,
    editingMenstruationDates: Set<LocalDate>,
    onStartMenstruationEdit: () -> Unit,
    onCancelMenstruationEdit: () -> Unit,
    onSaveMenstruationEdit: () -> Unit,
    onToggleMenstruationDate: (LocalDate) -> Unit,
    delayDates: Set<LocalDate>,
    isForecastBlockedByDelay: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CalendarScreen(
            month = currentMonth,
            selectedDate = selectedDate,
            dayDataByDate = dayDataByDate,
            futureCycleMarks = futureCycleMarks,
            minCalendarMonth = minCalendarMonth,
            maxCalendarMonth = maxCalendarMonth,
            cycleTimingHint = cycleTimingHint,
            heartRateSummary = selectedDateHeartRateSummary,
            onPrevMonth = onPrevMonth,
            onNextMonth = onNextMonth,
            onDateSelected = onDateSelected,
            isCycleCompletedByDate = isCycleCompletedByDate,
            isMenstruationEditMode = isMenstruationEditMode,
            editingMenstruationDates = editingMenstruationDates,
            onStartMenstruationEdit = onStartMenstruationEdit,
            onCancelMenstruationEdit = onCancelMenstruationEdit,
            onSaveMenstruationEdit = onSaveMenstruationEdit,
            onToggleMenstruationDate = onToggleMenstruationDate,
            delayDates = delayDates,
            isForecastBlockedByDelay = isForecastBlockedByDelay,
            isPremiumActive = isPremiumActive,
            predictionResult = result,
            forecastLabels = forecastLabels,
            predictionWarningMessage = predictionWarningMessage,
            onBuildPredictionClick = onBuildPredictionClick

        )

        if (statusText.isCalendarErrorMessage()) {
            CalendarTabStatusCard(
                text = statusText
            )
        }



        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isMenstruationEditMode && !selectedDate.isAfter(LocalDate.now())) {
                Button(
                    onClick = onEditDayClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CycleSenseButtonShape
                ) {
                    CycleSenseButtonText(
                        if (selectedDateHasEntry) {
                            "Редактировать данные дня"
                        } else {
                            "Добавить данные дня"
                        }
                    )
                }
            }
        }


    }
}

@Composable
private fun CalendarTabStatusCard(
    text: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CycleSenseSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (
                text.startsWith("Ошибка") ||
                text.contains("Недостаточно", ignoreCase = true) ||
                text.contains("нет данных", ignoreCase = true) ||
                text.contains("нужен", ignoreCase = true)
            ) {
                MaterialTheme.colorScheme.primary
            } else {
                CycleSenseTextSecondary
            }
        )
    }
}

@Composable
private fun ForecastDayCard(
    label: ForecastDayLabel,
    tension: Float?,
    energy: Float?,
    mood: Float?,
    concentration: Float?,
    discomfort: Float?
) {
    val cycleText = when (label.cycleOffset) {
        0 -> "Текущий цикл"
        1 -> "Следующий цикл"
        else -> "Цикл +${label.cycleOffset}"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CalendarTabCardShape),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = CalendarTabCardElevation
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CalendarTabCardInnerPadding),
            verticalArrangement = Arrangement.spacedBy(CalendarTabCardInnerGap)
        ) {
            Text(
                text = label.date.format(forecastDateFormatter()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "$cycleText, день ${label.dayInCycle}",
                style = MaterialTheme.typography.bodyMedium,
                color = CycleSenseTextSecondary
            )

            Text(
                text = "Фаза: ${label.phase.toRussianPhaseName()}",
                style = MaterialTheme.typography.bodyMedium,
                color = CycleSenseTextSecondary
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            ForecastMetricBlock(
                title = "Напряжение",
                valueText = tension.toTensionStateText()
            )

            ForecastMetricBlock(
                title = "Энергия",
                valueText = energy.toEnergyStateText()
            )

            ForecastMetricBlock(
                title = "Настроение",
                valueText = mood.toMoodStateText()
            )

            ForecastMetricBlock(
                title = "Концентрация",
                valueText = concentration.toConcentrationStateText()
            )

            ForecastMetricBlock(
                title = "Дискомфорт",
                valueText = discomfort.toDiscomfortStateText()
            )
        }
    }
}

@Composable
private fun ForecastMetricBlock(
    title: String,
    valueText: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = CycleSenseTextSecondary
        )

        Text(
            text = valueText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun forecastDateFormatter(): DateTimeFormatter {
    return DateTimeFormatter.ofPattern("dd.MM.yyyy")
}

private fun String.toRussianPhaseName(): String {
    return when (this) {
        "menstrual" -> "менструальная"
        "follicular" -> "фолликулярная"
        "ovulatory" -> "овуляторная"
        "luteal" -> "лютеиновая"
        else -> this
    }
}

private fun Float?.toTensionStateText(): String {
    val value = this ?: return "—"

    return when {
        value <= 5.0f -> "День может пройти без заметного напряжения"
        value <= 6.5f -> "Возможно лёгкое беспокойство"
        value <= 8.0f -> "Может ощущаться заметное напряжение"
        else -> "Есть риск перегрузки, лучше запланировать отдых"
    }
}

private fun Float?.toEnergyStateText(): String {
    val value = this ?: return "—"

    return when {
        value <= 2.0f -> "Вероятен низкий запас сил"
        value <= 4.25f -> "Возможна усталость"
        value <= 6.75f -> "Вероятен привычный уровень энергии"
        value <= 9.0f -> "Состояние может быть достаточно бодрым"
        else -> "Возможен заметный прилив сил"
    }
}

private fun Float?.toMoodStateText(): String {
    val value = this ?: return "—"

    return when {
        value <= 2.0f -> "Стоит бережнее отнестись к себе эмоционально"
        value <= 4.25f -> "Возможна эмоциональная усталость"
        value <= 6.75f -> "Вероятен стабильный эмоциональный фон"
        value <= 9.0f -> "Возможен лёгкий эмоциональный подъём"
        else -> "Вероятен тёплый и радостный настрой"
    }
}

private fun Float?.toConcentrationStateText(): String {
    val value = this ?: return "—"

    return when {
        value <= 2.0f -> "Может быть трудно сосредоточиться"
        value <= 4.25f -> "Возможна рассеянность"
        value <= 6.75f -> "Внимание, скорее всего, будет достаточно устойчивым"
        value <= 9.0f -> "Вероятна хорошая собранность"
        else -> "Возможна высокая продуктивность и собранность"
    }
}

private fun Float?.toDiscomfortStateText(): String {
    val value = this ?: return "—"

    return when {
        value <= 5.0f -> "Скорее всего, ничего не будет беспокоить"
        value <= 6.5f -> "Возможен лёгкий дискомфорт"
        value <= 8.0f -> "Возможны неприятные ощущения"
        else -> "Есть риск выраженного дискомфорта"
    }
}

private fun String.isCalendarErrorMessage(): Boolean {
    return startsWith("Ошибка") ||
            contains("Недостаточно", ignoreCase = true) ||
            contains("нет данных", ignoreCase = true) ||
            contains("недостаточно данных", ignoreCase = true) ||
            contains("нужно", ignoreCase = true) ||
            contains("нужен", ignoreCase = true) ||
            contains("не удалось", ignoreCase = true) ||
            contains("сначала", ignoreCase = true)
}



private fun Float?.formatPredictionValue(): String {
    return this?.formatOneDecimal() ?: "—"
}
