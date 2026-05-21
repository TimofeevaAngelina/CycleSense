package com.timofeeva.cyclesense

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.timofeeva.cyclesense.ml.CycleDayData
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import com.timofeeva.cyclesense.ml.AllNext3DaysResult
import com.timofeeva.cyclesense.ml.ForecastDayLabel
import com.timofeeva.cyclesense.ui.theme.FutureDelay
import com.timofeeva.cyclesense.ui.theme.FutureMenses
import com.timofeeva.cyclesense.ui.theme.FutureOvulation
import com.timofeeva.cyclesense.ui.theme.PhaseFollicular
import com.timofeeva.cyclesense.ui.theme.PhaseLuteal
import com.timofeeva.cyclesense.ui.theme.PhaseMenstrual
import com.timofeeva.cyclesense.ui.theme.PhaseOvulatory
import java.time.temporal.ChronoUnit
import com.timofeeva.cyclesense.ui.theme.PhaseMenstrualBorder
import com.timofeeva.cyclesense.ui.theme.PhaseOvulatoryBorder
import androidx.compose.foundation.layout.ColumnScope
import com.timofeeva.cyclesense.ui.theme.CycleSenseTextSecondary
import com.timofeeva.cyclesense.ui.theme.CycleSenseSurface
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle


private val CalendarSmallGap = 8.dp


private val CalendarCardInnerGap = 8.dp


data class CalendarDayUi(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val cycleDayData: CycleDayData? = null,
    val futureMarkType: FutureCycleMarkType? = null,
    val isDelayDay: Boolean = false
)

@Composable
fun CalendarScreen(
    month: YearMonth,
    selectedDate: LocalDate?,
    dayDataByDate: Map<LocalDate, CycleDayData>,
    futureCycleMarks: Map<LocalDate, FutureCycleMarkType> = emptyMap(),
    minCalendarMonth: YearMonth = YearMonth.of(1970, 1),
    maxCalendarMonth: YearMonth = YearMonth.of(LocalDate.now().year + 1, 12),
    cycleTimingHint: CycleTimingHint? = null,
    isCycleCompletedByDate: Map<LocalDate, Boolean>,
    heartRateSummary: com.timofeeva.cyclesense.data.repository.HeartRateDaySummary? = null,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    isMenstruationEditMode: Boolean = false,
    editingMenstruationDates: Set<LocalDate> = emptySet(),
    onStartMenstruationEdit: () -> Unit = {},
    onCancelMenstruationEdit: () -> Unit = {},
    onSaveMenstruationEdit: () -> Unit = {},
    onToggleMenstruationDate: (LocalDate) -> Unit = {},
    isPremiumActive: Boolean = false,
    predictionResult: AllNext3DaysResult? = null,
    forecastLabels: List<ForecastDayLabel> = emptyList(),
    predictionWarningMessage: String? = null,
    onBuildPredictionClick: () -> Unit,
    delayDates: Set<LocalDate> = emptySet(),
    isForecastBlockedByDelay: Boolean = false,
    modifier: Modifier = Modifier
) {

    val today = LocalDate.now()

    val days = remember(month, dayDataByDate, futureCycleMarks, delayDates) {
        buildCalendarDays(
            month = month,
            dayDataByDate = dayDataByDate,
            futureCycleMarks = futureCycleMarks,
            delayDates = delayDates
        )
    }



    val selectedCycleCompleted = selectedDate?.let { date ->
        isCycleCompletedByDate[date] ?: false
    } ?: false

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CalendarSectionCard {
            CalendarHeader(
                month = month,
                canGoPrev = month > minCalendarMonth,
                canGoNext = month < maxCalendarMonth,
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth
            )

            WeekDaysHeader()

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                days.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        week.forEach { day ->
                            CalendarDayCell(
                                day = if (isMenstruationEditMode) {
                                    day.copy(
                                        futureMarkType = null,
                                        isDelayDay = false
                                    )
                                } else {
                                    day
                                },
                                isSelected = !isMenstruationEditMode && selectedDate == day.date,
                                isToday = today == day.date,
                                isMenstruationEditMode = isMenstruationEditMode,
                                isEditedMenstruationDay = editingMenstruationDates.contains(day.date),
                                onClick = {
                                    if (isMenstruationEditMode) {
                                        if (!day.date.isAfter(today)) {
                                            onToggleMenstruationDate(day.date)
                                        }
                                    } else {
                                        onDateSelected(day.date)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }



        MenstruationEditControls(
            isMenstruationEditMode = isMenstruationEditMode,
            onStartMenstruationEdit = onStartMenstruationEdit,
            onCancelMenstruationEdit = onCancelMenstruationEdit,
            onSaveMenstruationEdit = onSaveMenstruationEdit
        )



        val forecastBaseDate = dayDataByDate.keys
            .filter { date -> !date.isAfter(LocalDate.now()) }
            .maxOrNull()
            ?: LocalDate.now()

        val forecastWindowDates = (1..3)
            .map { step -> forecastBaseDate.plusDays(step.toLong()) }
            .toSet()

        if (!isMenstruationEditMode) {

            DayDetailsCard(
                selectedDate = selectedDate,
                dayData = dayDataByDate[selectedDate],
                isCycleCompleted = selectedCycleCompleted,
                cycleTimingHint = cycleTimingHint,
                isForecastBlockedByDelay = isForecastBlockedByDelay,
                heartRateSummary = heartRateSummary,
                isPremiumActive = isPremiumActive,
                predictionResult = predictionResult,
                forecastLabels = forecastLabels,
                predictionWarningMessage = predictionWarningMessage,
                futureCycleMarks = futureCycleMarks,
                forecastWindowDates = forecastWindowDates,
                onBuildPredictionClick = onBuildPredictionClick
            )
        }


    }
}

@Composable
private fun CalendarArrowButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
        )
    }
}

@Composable
private fun CalendarHeader(
    month: YearMonth,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        CalendarArrowButton(
            text = "‹",
            enabled = canGoPrev,
            onClick = onPrevMonth
        )

        Text(
            text = buildMonthTitle(month),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        CalendarArrowButton(
            text = "›",
            enabled = canGoNext,
            onClick = onNextMonth
        )
    }
}

@Composable
private fun WeekDaysHeader() {
    val days = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEach { day ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodySmall,
                    color = CycleSenseTextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDayUi,
    isSelected: Boolean,
    isToday: Boolean,
    isMenstruationEditMode: Boolean,
    isEditedMenstruationDay: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()

    val actualPhase = day.cycleDayData?.phase
    val hasActualCycleDay = day.cycleDayData != null

    // Дополнительная защита:
    // даже если futureMarkType случайно пришёл вместе с фактическим днём,
    // внутри ячейки мы его игнорируем.
    val effectiveFutureMarkType = if (hasActualCycleDay) {
        null
    } else {
        day.futureMarkType
    }

    val effectiveIsDelayDay = day.isDelayDay
    val isFutureInEditMode = isMenstruationEditMode && day.date.isAfter(today)

    val dayBackground = when {
        isSelected ->
            MaterialTheme.colorScheme.tertiary

        isMenstruationEditMode && isEditedMenstruationDay ->
            getPhaseColor("menstrual")

        isMenstruationEditMode && day.date.isAfter(today) ->
            MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)

        isMenstruationEditMode ->
            MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)

// Задержка должна быть видна даже в текущем цикле,
// иначе она сливается с лютеиновой фазой.
        effectiveIsDelayDay ->
            FutureDelay

// Фактическая/текущая фаза важнее обычных предсказаний,
// но не важнее задержки.
        hasActualCycleDay ->
            getPhaseColor(actualPhase)

        // Предсказанная заливка только для дней без фактического cycleDayData.
        effectiveFutureMarkType != null ->
            getFutureMarkColor(effectiveFutureMarkType)

        !day.isCurrentMonth ->
            MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)

        else ->
            MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    }

    val borderColor = when {
        isSelected ->
            Color.Transparent

        isMenstruationEditMode && isEditedMenstruationDay ->
            Color(0xFFE57373)

        effectiveIsDelayDay ->
            Color(0xFFC9B98E)

        isToday ->
            MaterialTheme.colorScheme.primary

        effectiveFutureMarkType != null ->
            getFutureMarkBorderColor(effectiveFutureMarkType)

        !isMenstruationEditMode && actualPhase == "menstrual" ->
            PhaseMenstrualBorder

        !isMenstruationEditMode && actualPhase == "ovulatory" ->
            PhaseOvulatoryBorder

        else ->
            Color.Transparent
    }

    val borderWidth = when {
        isSelected -> 0.dp
        isMenstruationEditMode && isEditedMenstruationDay -> 2.dp
        effectiveIsDelayDay -> 2.dp
        isToday -> 1.5.dp
        effectiveFutureMarkType != null -> 1.dp
        !isMenstruationEditMode && actualPhase == "menstrual" -> 1.dp
        !isMenstruationEditMode && actualPhase == "ovulatory" -> 1.dp
        else -> 0.dp
    }

    val textColor = when {
        isSelected ->
            MaterialTheme.colorScheme.onTertiary

        isFutureInEditMode ->
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)

        day.isCurrentMonth ->
            MaterialTheme.colorScheme.onSurface

        else ->
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(dayBackground)
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
            )
        }

        if (isMenstruationEditMode && isEditedMenstruationDay) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE57373))
            )
        } else if (!isMenstruationEditMode && hasVisibleDayData(day.cycleDayData)) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.onTertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
            )
        }
    }
}

@Composable
private fun CalendarSectionCard(
    modifier: Modifier = Modifier,
    containerColor: Color = CycleSenseSurface,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}



@Composable
private fun DayDetailsCard(
    selectedDate: LocalDate?,
    dayData: CycleDayData?,
    isCycleCompleted: Boolean,
    cycleTimingHint: CycleTimingHint? = null,
    isForecastBlockedByDelay: Boolean = false,
    heartRateSummary: com.timofeeva.cyclesense.data.repository.HeartRateDaySummary? = null,
    isPremiumActive: Boolean,
    predictionResult: AllNext3DaysResult?,
    forecastLabels: List<ForecastDayLabel>,
    predictionWarningMessage: String?,
    futureCycleMarks: Map<LocalDate, FutureCycleMarkType>,
    forecastWindowDates: Set<LocalDate>,
    onBuildPredictionClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when {
            selectedDate == null -> {
                SimpleInfoCard {
                    Text(
                        text = "Выберите день в календаре",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CycleSenseTextSecondary
                    )
                }
            }

            selectedDate.isAfter(LocalDate.now()) -> {
                FutureCycleCard(
                    selectedDate = selectedDate,
                    futureCycleMarks = futureCycleMarks
                )

                FutureForecastCard(
                    selectedDate = selectedDate,
                    isPremiumActive = isPremiumActive,
                    predictionResult = predictionResult,
                    forecastLabels = forecastLabels,
                    predictionWarningMessage = predictionWarningMessage,
                    isForecastBlockedByDelay = isForecastBlockedByDelay,
                    forecastWindowDates = forecastWindowDates,
                    onBuildPredictionClick = onBuildPredictionClick
                )
            }

            dayData == null -> {
                SimpleInfoCard {
                    Text(
                        text = "За этот день пока нет записанных данных.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CycleSenseTextSecondary
                    )
                }
            }

            else -> {
                ActualCycleCard(
                    dayData = dayData,
                    isCycleCompleted = isCycleCompleted,
                    cycleTimingHint = cycleTimingHint
                )

                val hasFeelingData =
                    dayData.stressScore != null ||
                            dayData.energyLevel != null ||
                            dayData.moodScore != null ||
                            dayData.concentrationScore != null ||
                            dayData.painLevel != null

                if (hasFeelingData) {
                    DayFeelingCard(dayData = dayData)
                }

                val hasBodyData =
                    dayData.steps != null ||
                            dayData.calories != null ||
                            dayData.heartRate != null ||
                            heartRateSummary != null ||
                            dayData.sleepHours != null

                if (hasBodyData) {
                    DayPhysiologyCard(
                        dayData = dayData,
                        heartRateSummary = heartRateSummary
                    )
                }
            }
        }
    }
}

@Composable
private fun SimpleInfoCard(
    content: @Composable ColumnScope.() -> Unit
) {
    CalendarSectionCard(
        content = content
    )
}

@Composable
private fun ActualCycleCard(
    dayData: CycleDayData,
    isCycleCompleted: Boolean,
    cycleTimingHint: CycleTimingHint?
) {
    SimpleInfoCard {
        val hasKnownPhase = dayData.phase != null && dayData.phase != "unknown"

        Text(
            text = if (isCycleCompleted) {
                "Предыдущий цикл"
            } else {
                "Текущий цикл"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (hasKnownPhase) {
            Text(
                text = "${dayData.dayInCycle} день цикла",
                style = MaterialTheme.typography.bodyMedium,
                color = CycleSenseTextSecondary
            )

            Text(
                text = "${formatPhaseName(dayData.phase).replaceFirstChar { it.uppercase() }} фаза",
                style = MaterialTheme.typography.bodyMedium,
                color = CycleSenseTextSecondary
            )
        } else {
            Text(
                text = "Цикл не определён: отметьте начало менструации",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        cycleTimingHint?.let { hint ->
            Text(
                text = hint.message,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FutureCycleCard(
    selectedDate: LocalDate,
    futureCycleMarks: Map<LocalDate, FutureCycleMarkType>
) {
    val firstFutureMensesDate = futureCycleMarks
        .filter { (date, type) ->
            !date.isBefore(LocalDate.now()) &&
                    type == FutureCycleMarkType.PREDICTED_MENSES
        }
        .keys
        .minOrNull()

    val predictedDayInCycle = calculatePredictedFutureDayInCycle(
        selectedDate = selectedDate,
        futureCycleMarks = futureCycleMarks
    )

    val cycleTitle = if (
        firstFutureMensesDate != null &&
        !selectedDate.isBefore(firstFutureMensesDate)
    ) {
        "Следующий цикл"
    } else {
        "Текущий цикл"
    }

    SimpleInfoCard {
        Text(
            text = cycleTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        predictedDayInCycle?.let { dayInCycle ->
            Text(
                text = "$dayInCycle день цикла",
                style = MaterialTheme.typography.bodyMedium,
                color = CycleSenseTextSecondary
            )
        }

        FutureCycleTimingSummary(
            selectedDate = selectedDate,
            futureCycleMarks = futureCycleMarks
        )
    }
}

@Composable
private fun DayFeelingCard(
    dayData: CycleDayData
) {
    SimpleInfoCard {
        Text(
            text = "Самочувствие",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        DetailLine("Напряжение", dayData.stressScore.toActualTensionText())
        DetailLine("Энергия", dayData.energyLevel.toActualEnergyText())
        DetailLine("Настроение", dayData.moodScore.toActualMoodText())
        DetailLine("Концентрация", dayData.concentrationScore.toActualConcentrationText())
        DetailLine("Дискомфорт", dayData.painLevel.toActualDiscomfortText())
    }
}

@Composable
private fun DayPhysiologyCard(
    dayData: CycleDayData,
    heartRateSummary: com.timofeeva.cyclesense.data.repository.HeartRateDaySummary?
) {
    SimpleInfoCard {
        Text(
            text = "Физиология",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        DetailLine("Шаги", dayData.steps?.formatValue())
        DetailLine("Калории", dayData.calories?.formatValue())

        if (heartRateSummary != null) {
            DetailLine("Средний пульс", "${heartRateSummary.avgBpm} уд/мин")
            DetailLine("Диапазон пульса", "${heartRateSummary.minBpm}–${heartRateSummary.maxBpm} уд/мин")
        } else {
            DetailLine("Пульс", dayData.heartRate?.formatValue(" уд/мин"))
        }

        DetailLine("Сон", dayData.sleepHours?.formatValue(" ч"))
    }
}

@Composable
private fun FutureForecastCard(
    selectedDate: LocalDate,
    isPremiumActive: Boolean,
    predictionResult: AllNext3DaysResult?,
    forecastLabels: List<ForecastDayLabel>,
    predictionWarningMessage: String?,
    isForecastBlockedByDelay: Boolean,
    forecastWindowDates: Set<LocalDate>,
    onBuildPredictionClick: () -> Unit
) {
    val isInsideForecastWindow = forecastWindowDates.contains(selectedDate)

    if (!isInsideForecastWindow) {
        return
    }

    SimpleInfoCard {
        Text(
            text = "Прогноз состояния",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        when {
            !isPremiumActive -> {
                Text(
                    text = "Доступно в Premium.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CycleSenseTextSecondary
                )

                Button(
                    onClick = onBuildPredictionClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CycleSenseButtonShape
                ) {
                    CycleSenseButtonText("Посмотреть прогноз")
                }
            }

            isForecastBlockedByDelay -> {
                Text(
                    text = "Для прогноза отметьте начало последнего цикла.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            else -> {
                val forecastIndex = forecastLabels.indexOfFirst { label ->
                    label.date == selectedDate
                }

                val hasBuiltForecast = predictionResult != null && forecastLabels.isNotEmpty()

                when {
                    !hasBuiltForecast -> {
                        Text(
                            text = "Для ближайших 3 дней можно построить прогноз состояния.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CycleSenseTextSecondary
                        )

                        Button(
                            onClick = onBuildPredictionClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = CycleSenseButtonShape
                        ) {
                            CycleSenseButtonText("Посмотреть прогноз")
                        }
                    }

                    forecastIndex < 0 -> {
                        // Ничего не показываем: выбранная дата не входит в построенный прогноз.
                    }

                    else -> {
                        val label = forecastLabels[forecastIndex]

                        Text(
                            text = "${label.dayInCycle} день цикла",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CycleSenseTextSecondary
                        )

                        Text(
                            text = "${formatPhaseName(label.phase).replaceFirstChar { it.uppercase() }} фаза",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CycleSenseTextSecondary
                        )

                        predictionWarningMessage?.let { warning ->
                            Text(
                                text = warning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        ForecastDetailLine(
                            title = "Напряжение",
                            value = predictionResult.stress.getOrNull(forecastIndex),
                            description = predictionResult.stress
                                .getOrNull(forecastIndex)
                                .toForecastTensionText()
                        )

                        ForecastDetailLine(
                            title = "Энергия",
                            value = predictionResult.energy.getOrNull(forecastIndex),
                            description = predictionResult.energy
                                .getOrNull(forecastIndex)
                                .toForecastEnergyText()
                        )

                        ForecastDetailLine(
                            title = "Настроение",
                            value = predictionResult.mood.getOrNull(forecastIndex),
                            description = predictionResult.mood
                                .getOrNull(forecastIndex)
                                .toForecastMoodText()
                        )

                        ForecastDetailLine(
                            title = "Концентрация",
                            value = predictionResult.concentration.getOrNull(forecastIndex),
                            description = predictionResult.concentration
                                .getOrNull(forecastIndex)
                                .toForecastConcentrationText()
                        )

                        ForecastDetailLine(
                            title = "Дискомфорт",
                            value = predictionResult.pain.getOrNull(forecastIndex),
                            description = predictionResult.pain
                                .getOrNull(forecastIndex)
                                .toForecastDiscomfortText()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FutureCycleTimingSummary(
    selectedDate: LocalDate,
    futureCycleMarks: Map<LocalDate, FutureCycleMarkType>
) {
    val nextMensesDate = futureCycleMarks
        .filter { (date, type) ->
            !date.isBefore(selectedDate) &&
                    type == FutureCycleMarkType.PREDICTED_MENSES
        }
        .keys
        .minOrNull()

    val nextOvulationDate = futureCycleMarks
        .filter { (date, type) ->
            !date.isBefore(selectedDate) &&
                    type == FutureCycleMarkType.PREDICTED_OVULATION
        }
        .keys
        .minOrNull()

    val nearestEvent = listOfNotNull(
        nextMensesDate?.let { date ->
            FutureEvent(
                title = "предполагаемой менструации",
                date = date
            )
        },
        nextOvulationDate?.let { date ->
            FutureEvent(
                title = "предполагаемой овуляции",
                date = date
            )
        }
    ).minByOrNull { event ->
        ChronoUnit.DAYS.between(selectedDate, event.date)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (nearestEvent == null) {
            Text(
                text = "Для этой даты пока нет календарного ориентира.",
                style = MaterialTheme.typography.bodyMedium,
                color = CycleSenseTextSecondary
            )
        } else {
            Text(
                text = buildDaysUntilText(
                    targetName = nearestEvent.title,
                    selectedDate = selectedDate,
                    targetDate = nearestEvent.date
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = CycleSenseTextSecondary
            )
        }
    }
}

private data class FutureEvent(
    val title: String,
    val date: LocalDate
)

private fun buildDaysUntilText(
    targetName: String,
    selectedDate: LocalDate,
    targetDate: LocalDate
): String {
    val days = ChronoUnit.DAYS.between(selectedDate, targetDate).toInt()

    return when {
        days == 0 -> {
            "В этот день возможно начало $targetName"
        }

        days == 1 -> {
            "До $targetName: 1 день"
        }

        days in 2..4 -> {
            "До $targetName: $days дня"
        }

        else -> {
            "До $targetName: $days дней"
        }
    }
}

@Composable
private fun DetailLine(
    title: String,
    value: String?
) {
    if (value.isNullOrBlank()) return

    Text(
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        text = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    color = CycleSenseTextSecondary
                )
            ) {
                append(title)
                append(": ")
            }

            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            ) {
                append(value)
            }
        }
    )
}

@Composable
private fun MenstruationEditControls(
    isMenstruationEditMode: Boolean,
    onStartMenstruationEdit: () -> Unit,
    onCancelMenstruationEdit: () -> Unit,
    onSaveMenstruationEdit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isMenstruationEditMode) {
            Button(
                onClick = onSaveMenstruationEdit,
                modifier = Modifier.fillMaxWidth(),
                shape = CycleSenseButtonShape
            ) {
                CycleSenseButtonText("Сохранить")
            }

            TextButton(
                onClick = onCancelMenstruationEdit,
                modifier = Modifier.fillMaxWidth(),
                shape = CycleSenseButtonShape
            ) {
                CycleSenseButtonText("Отмена")
            }
        } else {
            Button(
                onClick = onStartMenstruationEdit,
                modifier = Modifier.fillMaxWidth(),
                shape = CycleSenseButtonShape
            ) {
                CycleSenseButtonText("Отметить месячные")
            }
        }
    }
}

@Composable
private fun ForecastDetailLine(
    title: String,
    value: Float?,
    description: String
) {
    if (value == null || description.isBlank()) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = CycleSenseTextSecondary
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun buildCalendarDays(
    month: YearMonth,
    dayDataByDate: Map<LocalDate, CycleDayData>,
    futureCycleMarks: Map<LocalDate, FutureCycleMarkType>,
    delayDates: Set<LocalDate>
): List<CalendarDayUi> {
    val firstDayOfMonth = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()

    val firstDayOffset = when (firstDayOfMonth.dayOfWeek) {
        DayOfWeek.MONDAY -> 0
        DayOfWeek.TUESDAY -> 1
        DayOfWeek.WEDNESDAY -> 2
        DayOfWeek.THURSDAY -> 3
        DayOfWeek.FRIDAY -> 4
        DayOfWeek.SATURDAY -> 5
        DayOfWeek.SUNDAY -> 6
    }

    val result = mutableListOf<CalendarDayUi>()

    fun buildDayUi(date: LocalDate, isCurrentMonth: Boolean): CalendarDayUi {
        val dayData = dayDataByDate[date]
        val hasActualCycleDay = dayData != null

        return CalendarDayUi(
            date = date,
            isCurrentMonth = isCurrentMonth,
            cycleDayData = dayData,

            // Предсказанная метка только там, где нет фактической/текущей записи дня.
            futureMarkType = if (hasActualCycleDay) {
                null
            } else {
                futureCycleMarks[date]
            },

            // Задержка тоже только там, где нет фактической/текущей записи дня.
            isDelayDay = delayDates.contains(date)
        )
    }

    val prevMonth = month.minusMonths(1)
    val prevMonthLength = prevMonth.lengthOfMonth()

    for (i in firstDayOffset downTo 1) {
        val date = prevMonth.atDay(prevMonthLength - i + 1)
        result.add(
            buildDayUi(
                date = date,
                isCurrentMonth = false
            )
        )
    }

    for (day in 1..daysInMonth) {
        val date = month.atDay(day)
        result.add(
            buildDayUi(
                date = date,
                isCurrentMonth = true
            )
        )
    }

    val nextMonth = month.plusMonths(1)
    var nextDay = 1

    while (result.size < 42) {
        val date = nextMonth.atDay(nextDay)
        result.add(
            buildDayUi(
                date = date,
                isCurrentMonth = false
            )
        )
        nextDay++
    }

    return result
}

private fun buildMonthTitle(month: YearMonth): String {
    val locale = Locale.forLanguageTag("ru")
    val monthName = month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
        .replaceFirstChar { it.uppercase() }
    return "$monthName ${month.year}"
}

private fun getPhaseColor(phase: String?): Color {
    return when (phase) {
        "menstrual" -> PhaseMenstrual
        "follicular" -> PhaseFollicular
        "ovulatory" -> PhaseOvulatory
        "luteal" -> PhaseLuteal
        else -> Color(0xFFF0EDF5)
    }
}

private fun Float.formatValue(suffix: String = ""): String {
    return if (this % 1f == 0f) {
        "${this.toInt()}$suffix"
    } else {
        "${String.format(Locale.US, "%.1f", this)}$suffix"
    }
}

private fun Float?.toActualTensionText(): String? {
    val value = this ?: return null
    return when (value.nearestSubjectiveScaleValue()) {
        1f -> "Полное спокойствие"
        3f -> "Лёгкое беспокойство"
        5.5f -> "Умеренное напряжение"
        8f -> "Сильный стресс"
        10f -> "Перегрузка"
        else -> null
    }
}

private fun Float?.toActualEnergyText(): String? {
    val value = this ?: return null
    return when (value.nearestSubjectiveScaleValue()) {
        1f -> "Совсем нет сил"
        3f -> "Усталость"
        5.5f -> "Обычное состояние"
        8f -> "Бодрость"
        10f -> "Прилив сил"
        else -> null
    }
}

private fun Float?.toActualMoodText(): String? {
    val value = this ?: return null
    return when (value.nearestSubjectiveScaleValue()) {
        1f -> "Подавленное"
        3f -> "Грустное"
        5.5f -> "Спокойное"
        8f -> "Приподнятое"
        10f -> "Радостное"
        else -> null
    }
}

private fun Float?.toActualConcentrationText(): String? {
    val value = this ?: return null
    return when (value.nearestSubjectiveScaleValue()) {
        1f -> "Не могу сосредоточиться"
        3f -> "Рассеянность"
        5.5f -> "Умеренная концентрация"
        8f -> "Собранность"
        10f -> "Полная сосредоточенность"
        else -> null
    }
}

private fun Float?.toActualDiscomfortText(): String? {
    val value = this ?: return null
    return when (value.nearestSubjectiveScaleValue()) {
        1f -> "Ничего не беспокоит"
        3f -> "Лёгкий дискомфорт"
        5.5f -> "Неприятные ощущения"
        8f -> "Выраженный дискомфорт"
        10f -> "Сильная боль"
        else -> null
    }
}

private fun Float.nearestSubjectiveScaleValue(): Float {
    val scaleValues = listOf(1f, 3f, 5.5f, 8f, 10f)

    return scaleValues.minBy { scaleValue ->
        kotlin.math.abs(this - scaleValue)
    }
}

private fun getFutureMarkColor(type: FutureCycleMarkType?): Color {
    return when (type) {
        FutureCycleMarkType.PREDICTED_MENSES -> FutureMenses.copy(alpha = 0.42f)
        FutureCycleMarkType.PREDICTED_OVULATION -> FutureOvulation.copy(alpha = 0.42f)
        FutureCycleMarkType.DELAY -> FutureDelay
        null -> Color.Transparent
    }
}

private fun getFutureMarkBorderColor(type: FutureCycleMarkType?): Color {
    return when (type) {
        FutureCycleMarkType.PREDICTED_MENSES -> Color(0xFFE57373)
        FutureCycleMarkType.PREDICTED_OVULATION -> Color(0xFF66BB6A)
        FutureCycleMarkType.DELAY -> Color(0xFF7E57C2)
        null -> Color.Transparent
    }
}

private fun formatPhaseName(phase: String?): String {
    return when (phase) {
        "menstrual" -> "менструальная"
        "follicular" -> "фолликулярная"
        "ovulatory" -> "овуляторная"
        "luteal" -> "лютеиновая"
        else -> "-"
    }
}

private fun hasVisibleDayData(day: CycleDayData?): Boolean {
    if (day == null) return false

    return day.steps != null ||
            day.calories != null ||
            day.heartRate != null ||
            day.rmssd != null ||
            day.sleepHours != null ||
            day.stressScore != null ||
            day.energyLevel != null ||
            day.moodScore != null ||
            day.concentrationScore != null ||
            day.painLevel != null
}

private fun Float?.toForecastTensionText(): String {
    val value = this ?: return ""
    return when {
        value <= 5.0f -> "День может пройти без заметного напряжения"
        value <= 6.5f -> "Возможно лёгкое беспокойство"
        value <= 8.0f -> "Может ощущаться заметное напряжение"
        else -> "Есть риск перегрузки, лучше запланировать отдых"
    }
}

private fun Float?.toForecastEnergyText(): String {
    val value = this ?: return ""
    return when {
        value <= 2.0f -> "Вероятен низкий запас сил"
        value <= 4.25f -> "Возможна усталость"
        value <= 6.75f -> "Вероятен привычный уровень энергии"
        value <= 9.0f -> "Состояние может быть достаточно бодрым"
        else -> "Возможен заметный прилив сил"
    }
}

private fun Float?.toForecastMoodText(): String {
    val value = this ?: return ""
    return when {
        value <= 2.0f -> "Стоит бережнее отнестись к себе эмоционально"
        value <= 4.25f -> "Возможна эмоциональная усталость"
        value <= 6.75f -> "Вероятен стабильный эмоциональный фон"
        value <= 9.0f -> "Возможен лёгкий эмоциональный подъём"
        else -> "Вероятен тёплый и радостный настрой"
    }
}

private fun Float?.toForecastConcentrationText(): String {
    val value = this ?: return ""
    return when {
        value <= 2.0f -> "Может быть трудно сосредоточиться"
        value <= 4.25f -> "Возможна рассеянность"
        value <= 6.75f -> "Внимание, скорее всего, будет достаточно устойчивым"
        value <= 9.0f -> "Вероятна хорошая собранность"
        else -> "Возможна высокая продуктивность и собранность"
    }
}

private fun Float?.toForecastDiscomfortText(): String {
    val value = this ?: return ""
    return when {
        value <= 5.0f -> "Скорее всего, ничего не будет беспокоить"
        value <= 6.5f -> "Возможен лёгкий дискомфорт"
        value <= 8.0f -> "Возможны неприятные ощущения"
        else -> "Есть риск выраженного дискомфорта"
    }
}

private fun calculatePredictedFutureDayInCycle(
    selectedDate: LocalDate,
    futureCycleMarks: Map<LocalDate, FutureCycleMarkType>
): Int? {
    val predictedMensesDates = futureCycleMarks
        .filter { (_, type) ->
            type == FutureCycleMarkType.PREDICTED_MENSES
        }
        .keys
        .sorted()

    if (predictedMensesDates.isEmpty()) return null

    val mensesBlocks = mutableListOf<List<LocalDate>>()
    var currentBlock = mutableListOf<LocalDate>()

    predictedMensesDates.forEach { date ->
        if (currentBlock.isEmpty()) {
            currentBlock.add(date)
        } else {
            val previousDate = currentBlock.last()
            if (date == previousDate.plusDays(1)) {
                currentBlock.add(date)
            } else {
                mensesBlocks.add(currentBlock.toList())
                currentBlock = mutableListOf(date)
            }
        }
    }

    if (currentBlock.isNotEmpty()) {
        mensesBlocks.add(currentBlock.toList())
    }

    val cycleStart = mensesBlocks
        .mapNotNull { block -> block.firstOrNull() }
        .filter { startDate -> !startDate.isAfter(selectedDate) }
        .maxOrNull()
        ?: return null

    return ChronoUnit.DAYS.between(cycleStart, selectedDate).toInt() + 1
}