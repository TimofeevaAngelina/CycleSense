package com.timofeeva.cyclesense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.timofeeva.cyclesense.analytics.ActivitySleepAnalyticsResult
import com.timofeeva.cyclesense.analytics.ActivitySleepTrendChart
import com.timofeeva.cyclesense.analytics.AnalyticsInsight
import com.timofeeva.cyclesense.analytics.AnalyticsInterpretationService
import com.timofeeva.cyclesense.analytics.CycleLengthAnalyticsResult
import com.timofeeva.cyclesense.analytics.CycleTrendAnalyticsResult
import com.timofeeva.cyclesense.analytics.CycleTrendSection
import com.timofeeva.cyclesense.analytics.HeartRateAnalyticsResult
import com.timofeeva.cyclesense.analytics.PhaseAnalyticsResult
import com.timofeeva.cyclesense.analytics.RelationshipAnalyticsResult
import com.timofeeva.cyclesense.analytics.RelationshipMetric
import com.timofeeva.cyclesense.analytics.RelationshipTrendChart
import java.time.format.DateTimeFormatter
import com.timofeeva.cyclesense.analytics.PsychophysiologyPhaseProfileBlock
import androidx.compose.ui.graphics.Color
import com.timofeeva.cyclesense.ui.theme.CycleSenseSurface
import com.timofeeva.cyclesense.ui.theme.CycleSenseTextSecondary
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalyticsTabContent(
    statusText: String,
    cycleLengthAnalytics: CycleLengthAnalyticsResult?,
    phaseAnalytics: PhaseAnalyticsResult?,
    cycleTrendAnalytics: CycleTrendAnalyticsResult?,
    heartRateAnalytics: HeartRateAnalyticsResult?,
    activitySleepAnalytics: ActivitySleepAnalyticsResult?,
    relationshipAnalytics: RelationshipAnalyticsResult?,
    onBuildPsychophysiologyAnalytics: () -> Unit,
    onBuildPhysiologyAnalytics: () -> Unit,
    onOpenRelationshipAnalytics: () -> Unit,
    onBuildRelationshipAnalytics: (List<RelationshipMetric>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRelationshipMetrics by remember {
        mutableStateOf(
            listOf(
                RelationshipMetric.SLEEP,
                RelationshipMetric.STRESS,
                RelationshipMetric.ENERGY
            )
        )
    }

    var showRelationshipMetricSelector by remember {
        mutableStateOf(false)
    }

    val interpretationService = remember {
        AnalyticsInterpretationService()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CycleLengthAnalyticsBlock(
            analytics = cycleLengthAnalytics
        )

        AnalyticsInfoCard(
            title = "Построить аналитику",
            subtitle = "Выберите тип анализа для текущих данных."
        ) {
            Button(
                onClick = {
                    onBuildPsychophysiologyAnalytics()
                    showRelationshipMetricSelector = false
                },
                modifier = Modifier.fillMaxWidth(),
                shape = CycleSenseButtonShape
            ) {
                CycleSenseButtonText("Аналитика самочувствия")
            }

            Button(
                onClick = {
                    onBuildPhysiologyAnalytics()
                    showRelationshipMetricSelector = false
                },
                modifier = Modifier.fillMaxWidth(),
                shape = CycleSenseButtonShape
            ) {
                CycleSenseButtonText("Физиологическая аналитика")
            }

            Button(
                onClick = {
                    showRelationshipMetricSelector = !showRelationshipMetricSelector
                    onOpenRelationshipAnalytics()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = CycleSenseButtonShape
            ) {
                CycleSenseButtonText("Взаимосвязи показателей")
            }
        }

        if (showRelationshipMetricSelector) {
            AnalyticsInfoCard(
                title = "Взаимосвязи показателей",
                subtitle = "Выберите от 2 до 3 показателей для сравнения на одном графике."
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RelationshipMetric.entries.forEach { metric ->
                        val selected = metric in selectedRelationshipMetrics
                        val canSelectMore = selectedRelationshipMetrics.size < 3

                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedRelationshipMetrics = if (selected) {
                                    selectedRelationshipMetrics - metric
                                } else {
                                    if (canSelectMore) {
                                        selectedRelationshipMetrics + metric
                                    } else {
                                        selectedRelationshipMetrics
                                    }
                                }
                            },
                            enabled = selected || canSelectMore,
                            label = {
                                Text(
                                    text = metric.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }

                Text(
                    text = if (selectedRelationshipMetrics.isEmpty()) {
                        "Выберите 2–3 показателя"
                    } else {
                        "Выбрано: ${selectedRelationshipMetrics.joinToString { it.title }}"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = CycleSenseTextSecondary,
                    textAlign = TextAlign.Start
                )

                Button(
                    onClick = {
                        onBuildRelationshipAnalytics(selectedRelationshipMetrics)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedRelationshipMetrics.size in 2..3,
                    shape = CycleSenseButtonShape
                ) {
                    CycleSenseButtonText("Показать взаимосвязи")
                }
            }
        }

        val shouldShowStatus =
            statusText.contains("ошибка", ignoreCase = true) ||
                    statusText.contains("нет данных", ignoreCase = true) ||
                    statusText.contains("недостаточно", ignoreCase = true) ||
                    statusText.contains("нужен", ignoreCase = true) ||
                    statusText.contains("нужно", ignoreCase = true) ||
                    statusText.contains("заполните", ignoreCase = true) ||
                    statusText.contains("самочувств", ignoreCase = true)

        if (shouldShowStatus) {
            AnalyticsInfoCard(
                title = "Сообщение"
            ) {
                AnalyticsBodyText(
                    text = statusText
                )
            }
        }

        val psychophysiologyObservedDays =
            cycleTrendAnalytics?.points?.count { point ->
                point.observationsCount > 0
            } ?: 0

        val hasPsychophysiologyTrendData =
            psychophysiologyObservedDays >= 3

        if (hasPsychophysiologyTrendData) {
            phaseAnalytics?.let { analytics ->
                AnalyticsInfoCard(
                    title = "Ваше самочувствие в разные фазы цикла"
                ) {
                    PsychophysiologyPhaseProfileBlock(
                        phaseAnalytics = analytics
                    )
                }
            }

            cycleTrendAnalytics?.let { trend ->
                AnalyticsInfoCard(
                    title = "Как меняется самочувствие в течение цикла"
                ) {
                    CycleTrendSection(
                        trend = trend,
                        phaseAnalytics = phaseAnalytics
                    )
                }
            }
        } else if (phaseAnalytics != null || cycleTrendAnalytics != null) {
            AnalyticsInfoCard(
                title = "Самочувствие в течение цикла"
            ) {
                AnalyticsBodyText(
                    text = "Пока недостаточно данных самочувствия для построения аналитики. Заполните несколько дней в календаре: напряжение, энергию, настроение, концентрацию и дискомфорт."
                )
            }
        }



        activitySleepAnalytics?.let { analytics ->
            val hasActivitySleepData = analytics.phasePoints.any { phase ->
                phase.avgSteps != null ||
                        phase.avgCalories != null ||
                        phase.avgSleepHours != null
            }

            val hasHeartRateData =
                heartRateAnalytics?.phasePoints?.any { phase ->
                    phase.avgMinHeartRate != null ||
                            phase.avgMaxHeartRate != null
                } == true ||
                        heartRateAnalytics?.points?.any { point ->
                            point.samplesCount > 0
                        } == true

            val hasAnyPhysiologyData = hasActivitySleepData || hasHeartRateData

            AnalyticsInfoCard(
                title = "Физические показатели в разные фазы цикла"
            ) {
                if (hasAnyPhysiologyData) {
                    ActivitySleepTrendChart(
                        analytics = analytics,
                        heartRateAnalytics = heartRateAnalytics
                    )
                } else {
                    AnalyticsBodyText(
                        text = "Пока недостаточно физиологических данных для построения аналитики. Подключите Health Connect и добавьте данные о шагах, сне или пульсе."
                    )
                }
            }
        }

        relationshipAnalytics?.let { analytics ->
            InsightsBlock(
                title = "Что видно по выбранным показателям",
                insights = interpretationService.buildRelationshipInsights(analytics)
            )

            AnalyticsInfoCard(
                title = "График взаимосвязей",
                subtitle = "Показатели приведены к общей шкале, чтобы можно было сравнить, как они меняются относительно друг друга."
            ) {
                RelationshipTrendChart(
                    analytics = analytics
                )
            }
        }
    }
}

@Composable
private fun InsightsBlock(
    title: String,
    insights: List<AnalyticsInsight>,
    modifier: Modifier = Modifier
) {
    if (insights.isEmpty()) return

    AnalyticsInfoCard(
        title = title,
        modifier = modifier
    ) {
        insights.forEachIndexed { index, insight ->
            if (index > 0) {
                HorizontalDivider()
            }

            Text(
                text = insight.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            AnalyticsBodyText(
                text = insight.description
            )
        }
    }
}

@Composable
private fun CycleLengthAnalyticsBlock(
    analytics: CycleLengthAnalyticsResult?
) {
    AnalyticsInfoCard(
        title = "Аналитика цикла",
        subtitle = "Базовая статистика по предыдущим циклам."
    ) {
        if (analytics == null) {
            AnalyticsBodyText(
                text = "Данные цикла пока не загружены."
            )
            return@AnalyticsInfoCard
        }

        if (analytics.completedCyclesUsed <= 0) {
            AnalyticsBodyText(
                text = "Пока нет завершённых циклов для анализа. После отметки следующих месячных здесь появится статистика по прошлым циклам."
            )
            return@AnalyticsInfoCard
        }

        AnalyticsLine(
            title = "Колебания длины цикла",
            value = buildRangeText(
                min = analytics.minCycleLength,
                max = analytics.maxCycleLength,
                unit = "дн."
            )
        )

        AnalyticsLine(
            title = "Колебания длины менструации",
            value = buildRangeText(
                min = analytics.minMensesLength,
                max = analytics.maxMensesLength,
                unit = "дн."
            )
        )

        AnalyticsLine(
            title = "Длина предыдущего цикла",
            value = analytics.previousCycleLength?.let { "$it дн." } ?: "недостаточно данных"
        )

        AnalyticsLine(
            title = "Длина предыдущих месячных",
            value = analytics.previousMensesLength?.let { "$it дн." } ?: "недостаточно данных"
        )

        Text(
            text = analytics.regularityText,
            style = MaterialTheme.typography.bodySmall,
            color = CycleSenseTextSecondary
        )
    }
}

private fun buildRangeText(
    min: Int?,
    max: Int?,
    unit: String
): String {
    if (min == null || max == null) {
        return "недостаточно данных"
    }

    return if (min == max) {
        "$min $unit"
    } else {
        "$min–$max $unit"
    }
}

@Composable
private fun AnalyticsInfoCard(
    title: String,
    subtitle: String? = null,
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    hyphens = Hyphens.Auto,
                    lineBreak = LineBreak.Paragraph
                ),
                softWrap = true
            )

            if (!subtitle.isNullOrBlank()) {
                AnalyticsBodyText(
                    text = subtitle
                )
            }

            content()
        }
    }
}

@Composable
private fun AnalyticsLine(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = CycleSenseTextSecondary,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun AnalyticsBodyText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = CycleSenseTextSecondary,
        textAlign = TextAlign.Start
    )
}