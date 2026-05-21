package com.timofeeva.cyclesense.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.timofeeva.cyclesense.ui.theme.CycleSenseTextSecondary
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity

@Composable
fun CycleTrendSection(
    trend: CycleTrendAnalyticsResult,
    phaseAnalytics: PhaseAnalyticsResult? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        MetricTrendChart(
            title = "Напряжение",
            points = trend.points.map { it.stress },
            axisMode = MetricAxisMode.NEGATIVE_UP,
            description = buildMetricDescription(
                metricTitle = "напряжение",
                metricDisplayName = "Напряжение",
                trend = trend,
                valueFromPoint = { it.stress }
            )
        )

        MetricTrendChart(
            title = "Энергия",
            points = trend.points.map { it.energy },
            axisMode = MetricAxisMode.POSITIVE_BIDIRECTIONAL,
            description = buildMetricDescription(
                metricTitle = "энергия",
                metricDisplayName = "Энергия",
                trend = trend,
                valueFromPoint = { it.energy }
            )
        )

        MetricTrendChart(
            title = "Настроение",
            points = trend.points.map { it.mood },
            axisMode = MetricAxisMode.POSITIVE_BIDIRECTIONAL,
            description = buildMetricDescription(
                metricTitle = "настроение",
                metricDisplayName = "Настроение",
                trend = trend,
                valueFromPoint = { it.mood }
            )
        )

        MetricTrendChart(
            title = "Концентрация",
            points = trend.points.map { it.concentration },
            axisMode = MetricAxisMode.POSITIVE_BIDIRECTIONAL,
            description = buildMetricDescription(
                metricTitle = "концентрация",
                metricDisplayName = "Концентрация",
                trend = trend,
                valueFromPoint = { it.concentration }
            )
        )

        MetricTrendChart(
            title = "Дискомфорт",
            points = trend.points.map { it.pain },
            axisMode = MetricAxisMode.NEGATIVE_UP,
            description = buildMetricDescription(
                metricTitle = "дискомфорт",
                metricDisplayName = "Дискомфорт",
                trend = trend,
                valueFromPoint = { it.pain }
            )
        )
    }
}


@Composable
fun PsychophysiologyPhaseProfileBlock(
    phaseAnalytics: PhaseAnalyticsResult,
    modifier: Modifier = Modifier
) {
    val fontScale = LocalDensity.current.fontScale
    val shouldStackIndicators = fontScale >= 1.35f

    val phases = listOf(
        PhaseDescriptionSource("Менструальная", phaseAnalytics.menstrual),
        PhaseDescriptionSource("Фолликулярная", phaseAnalytics.follicular),
        PhaseDescriptionSource("Овуляторная", phaseAnalytics.ovulatory),
        PhaseDescriptionSource("Лютеиновая", phaseAnalytics.luteal)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        phases.forEach { phase ->
            val description = buildPhaseDescription(phase, phases)
            val indicators = buildPhaseIndicatorItems(phase, phases)

            Column(
                modifier = Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = phase.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                PhaseIndicatorList(
                    indicators = indicators,
                    stacked = shouldStackIndicators
                )

                Text(
                    text = "Рекомендация: ${description.recommendationText}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CycleSenseTextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun PhaseIndicatorList(
    indicators: List<PhaseIndicatorItem>,
    stacked: Boolean,
    modifier: Modifier = Modifier
) {
    if (stacked) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            indicators.forEach { item ->
                PhaseIndicatorText(
                    item = item,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            indicators.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { item ->
                        PhaseIndicatorText(
                            item = item,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (rowItems.size == 1) {
                        Text(
                            text = "",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhaseIndicatorText(
    item: PhaseIndicatorItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            color = item.color,
            softWrap = true
        )

        Text(
            text = item.arrow,
            style = MaterialTheme.typography.bodyMedium,
            color = item.color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun buildPhaseIndicatorItems(
    phase: PhaseDescriptionSource,
    allPhases: List<PhaseDescriptionSource>
): List<PhaseIndicatorItem> {
    val avgStress = allPhases.map { it.metrics.stress }.average().toFloat()
    val avgEnergy = allPhases.map { it.metrics.energy }.average().toFloat()
    val avgMood = allPhases.map { it.metrics.mood }.average().toFloat()
    val avgConcentration = allPhases.map { it.metrics.concentration }.average().toFloat()
    val avgPain = allPhases.map { it.metrics.pain }.average().toFloat()

    return listOf(
        buildPhaseIndicatorItem(
            title = "Напряжение",
            value = phase.metrics.stress,
            average = avgStress,
            higherIsBetter = false
        ),
        buildPhaseIndicatorItem(
            title = "Энергия",
            value = phase.metrics.energy,
            average = avgEnergy,
            higherIsBetter = true
        ),
        buildPhaseIndicatorItem(
            title = "Настроение",
            value = phase.metrics.mood,
            average = avgMood,
            higherIsBetter = true
        ),
        buildPhaseIndicatorItem(
            title = "Концентрация",
            value = phase.metrics.concentration,
            average = avgConcentration,
            higherIsBetter = true
        ),
        buildPhaseIndicatorItem(
            title = "Дискомфорт",
            value = phase.metrics.pain,
            average = avgPain,
            higherIsBetter = false
        )
    )
}


private fun buildPhaseIndicatorItem(
    title: String,
    value: Float,
    average: Float,
    higherIsBetter: Boolean,
    threshold: Float = 0.4f
): PhaseIndicatorItem {
    val diff = value - average

    val improvementColor = Color(0xFF96DEBD)
    val worseningColor = Color(0xFFDE96B7)
    val neutralColor = Color(0xFFD0C2A4)

    return when {
        diff >= threshold -> {
            PhaseIndicatorItem(
                title = title,
                arrow = "↑",
                color = if (higherIsBetter) improvementColor else worseningColor
            )
        }

        diff <= -threshold -> {
            PhaseIndicatorItem(
                title = title,
                arrow = "↓",
                color = if (higherIsBetter) worseningColor else improvementColor
            )
        }

        else -> {
            PhaseIndicatorItem(
                title = title,
                arrow = "—",
                color = neutralColor
            )
        }
    }
}


private fun buildPhaseDescription(
    phase: PhaseDescriptionSource,
    allPhases: List<PhaseDescriptionSource>
): PhaseTextDescription {
    val avgStress = allPhases.map { it.metrics.stress }.average().toFloat()
    val avgEnergy = allPhases.map { it.metrics.energy }.average().toFloat()
    val avgMood = allPhases.map { it.metrics.mood }.average().toFloat()
    val avgConcentration = allPhases.map { it.metrics.concentration }.average().toFloat()
    val avgPain = allPhases.map { it.metrics.pain }.average().toFloat()

    val recommendation = buildPhaseRecommendation(
        phase = phase,
        avgStress = avgStress,
        avgEnergy = avgEnergy,
        avgMood = avgMood,
        avgConcentration = avgConcentration,
        avgPain = avgPain
    )

    return PhaseTextDescription(
        recommendationText = recommendation
    )
}


private fun buildPhaseRecommendation(
    phase: PhaseDescriptionSource,
    avgStress: Float,
    avgEnergy: Float,
    avgMood: Float,
    avgConcentration: Float,
    avgPain: Float
): String {
    val stressHigher = phase.metrics.stress - avgStress >= 0.4f
    val energyLower = avgEnergy - phase.metrics.energy >= 0.4f
    val moodLower = avgMood - phase.metrics.mood >= 0.4f
    val concentrationLower = avgConcentration - phase.metrics.concentration >= 0.4f
    val painHigher = phase.metrics.pain - avgPain >= 0.4f

    val energyHigher = phase.metrics.energy - avgEnergy >= 0.4f
    val moodHigher = phase.metrics.mood - avgMood >= 0.4f
    val concentrationHigher = phase.metrics.concentration - avgConcentration >= 0.4f
    val stressLower = avgStress - phase.metrics.stress >= 0.4f
    val painLower = avgPain - phase.metrics.pain >= 0.4f

    return when {
        stressHigher && painHigher && energyLower -> {
            "лучше снизить интенсивность нагрузки, оставить больше времени на восстановление и не планировать сложные задачи без необходимости."
        }

        stressHigher && (moodLower || concentrationLower) -> {
            "стоит заранее планировать более спокойный режим, делить сложные задачи на короткие этапы и избегать перегрузки."
        }

        painHigher -> {
            "полезно учитывать возможный физический дискомфорт: выбирать более мягкую активность и оставлять время на отдых."
        }

        energyLower || concentrationLower -> {
            "лучше выбирать умеренную нагрузку, рутинные задачи и больше отдыхать в течение дня."
        }

        energyHigher && concentrationHigher && stressLower -> {
            "этот период выглядит более ресурсным, поэтому подходит для планов, требующих концентрации и повышения активности."
        }

        moodHigher && energyHigher && painLower -> {
            "этот период наиболее благоприятен для активности, социальных дел и задач, требующих сосредоточенности."
        }

        else -> {
            "выраженной необходимости менять нагрузку по текущим данным не видно, но стоит ориентироваться на самочувствие в течение дня."
        }
    }
}


@Composable
private fun MetricTrendChart(
    title: String,
    points: List<Float>,
    axisMode: MetricAxisMode,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )

        description
            .split("\n\n")
            .filter { it.isNotBlank() }
            .forEachIndexed { index, paragraph ->
                Text(
                    text = paragraph,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CycleSenseTextSecondary,
                    modifier = Modifier.padding(top = if (index == 0) 4.dp else 8.dp)
                )
            }



        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .padding(top = 10.dp)
        ) {
            if (points.isEmpty()) return@Canvas

            val axisTextSize = when {
                size.width < 360f -> 23f
                size.width < 520f -> 24f
                else -> 25f
            }

            val textPaint = android.graphics.Paint().apply {
                textSize = axisTextSize
                color = android.graphics.Color.rgb(111, 104, 120)
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
            }

            val yLabelValues = listOf("1", "3", "5.5", "8", "10")
            val maxYLabelWidth = yLabelValues
                .map { label -> textPaint.measureText(label) }
                .maxOrNull()
                ?: 0f

            val leftPadding = (maxYLabelWidth + 22f).coerceAtLeast(48f)
            val rightPadding = 16f
            val topPadding = 20f
            val bottomPadding = 34f

            val chartWidth = size.width - leftPadding - rightPadding
            val chartHeight = size.height - topPadding - bottomPadding
            val baselineY = topPadding + chartHeight
            val centerY = topPadding + chartHeight / 2f

            val lineColor = Color(0xFF6B5B95)
            val fillColor = lineColor.copy(alpha = 0.12f)
            val gridColor = Color.LightGray.copy(alpha = 0.30f)
            val centerGridColor = Color(0xFFD0C2A4).copy(alpha = 0.55f)
            val axisColor = Color.Gray.copy(alpha = 0.55f)

            val worseningColor = Color(0xFFDE96B7)
            val improvementColor = Color(0xFF96DEBD)
            val neutralColor = Color(0xFFD0C2A4)

            drawMetricDirectionGuide(
                mode = axisMode,
                x = 18f,
                topY = topPadding,
                centerY = centerY,
                bottomY = baselineY,
                improvementColor = improvementColor,
                worseningColor = worseningColor,
                neutralColor = neutralColor
            )

            drawLine(
                color = axisColor,
                start = Offset(leftPadding, topPadding),
                end = Offset(leftPadding, baselineY),
                strokeWidth = 2f
            )

            drawLine(
                color = axisColor,
                start = Offset(leftPadding, baselineY),
                end = Offset(leftPadding + chartWidth, baselineY),
                strokeWidth = 2f
            )

            val levelRatios = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)

            levelRatios.forEach { ratio ->
                val y = baselineY - ratio * chartHeight
                val isCenter = ratio == 0.5f

                drawLine(
                    color = if (isCenter) centerGridColor else gridColor,
                    start = Offset(leftPadding, y),
                    end = Offset(leftPadding + chartWidth, y),
                    strokeWidth = if (isCenter) 1.6f else 1f
                )
            }

            val coords = points.mapIndexed { index, value ->
                val x = leftPadding +
                        (index.toFloat() / points.lastIndex.coerceAtLeast(1)) * chartWidth

                val ratio = subjectiveValueToFiveStateRatio(value)
                val y = baselineY - ratio * chartHeight

                Offset(x, y)
            }

            if (coords.size == 1) {
                drawCircle(
                    color = lineColor,
                    radius = 4.5f,
                    center = coords.first()
                )
            } else {
                val linePath = buildDirectionalSmoothChartPath(
                    points = coords,
                    centerY = centerY
                )

                val fillPath = buildDirectionalSmoothChartFillPath(
                    points = coords,
                    baselineY = baselineY,
                    centerY = centerY
                )

                drawPath(
                    path = fillPath,
                    color = fillColor
                )

                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.5f)
                )

                coords.forEach { point ->
                    drawCircle(
                        color = lineColor,
                        radius = 4.5f,
                        center = point
                    )

                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = 2.2f,
                        center = point
                    )
                }
            }



            val xLabelDays = buildList {
                var day = 7
                val lastDay = points.size

                while (day <= lastDay) {
                    add(day)
                    day += 7
                }

                if (lastDay !in this) {
                    add(lastDay)
                }
            }.sorted()

            xLabelDays.forEach { day ->
                val index = day - 1
                val x = leftPadding +
                        (index.toFloat() / points.lastIndex.coerceAtLeast(1)) * chartWidth

                drawLine(
                    color = Color.LightGray.copy(alpha = 0.18f),
                    start = Offset(x, topPadding),
                    end = Offset(x, baselineY),
                    strokeWidth = 1f
                )

                drawContext.canvas.nativeCanvas.drawText(
                    day.toString(),
                    x,
                    baselineY + axisTextSize,
                    textPaint
                )
            }
        }
    }
}


private fun subjectiveValueToFiveStateRatio(value: Float): Float {
    val safeValue = value.coerceIn(1f, 10f)

    val levels = listOf(1f, 3f, 5.5f, 8f, 10f)

    if (safeValue <= levels.first()) return 0f
    if (safeValue >= levels.last()) return 1f

    for (index in 0 until levels.lastIndex) {
        val left = levels[index]
        val right = levels[index + 1]

        if (safeValue in left..right) {
            val localRatio = (safeValue - left) / (right - left)
            val levelPosition = index + localRatio
            return levelPosition / levels.lastIndex.toFloat()
        }
    }

    return 0.5f
}


private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMetricDirectionGuide(
    mode: MetricAxisMode,
    x: Float,
    topY: Float,
    centerY: Float,
    bottomY: Float,
    improvementColor: Color,
    worseningColor: Color,
    neutralColor: Color
) {
    when (mode) {
        MetricAxisMode.NEGATIVE_UP -> {
            drawVerticalArrow(
                x = x,
                startY = bottomY,
                endY = topY,
                color = worseningColor
            )
        }

        MetricAxisMode.POSITIVE_BIDIRECTIONAL -> {
            drawVerticalArrow(
                x = x,
                startY = centerY,
                endY = topY,
                color = improvementColor
            )

            drawVerticalArrow(
                x = x,
                startY = centerY,
                endY = bottomY,
                color = worseningColor
            )

            drawCircle(
                color = neutralColor,
                radius = 3.5f,
                center = Offset(x, centerY)
            )
        }
    }
}


private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVerticalArrow(
    x: Float,
    startY: Float,
    endY: Float,
    color: Color
) {
    val strokeWidth = 4f
    val arrowSize = 9f

    drawLine(
        color = color,
        start = Offset(x, startY),
        end = Offset(x, endY),
        strokeWidth = strokeWidth
    )

    if (endY < startY) {
        drawLine(
            color = color,
            start = Offset(x, endY),
            end = Offset(x - arrowSize, endY + arrowSize),
            strokeWidth = strokeWidth
        )

        drawLine(
            color = color,
            start = Offset(x, endY),
            end = Offset(x + arrowSize, endY + arrowSize),
            strokeWidth = strokeWidth
        )
    } else {
        drawLine(
            color = color,
            start = Offset(x, endY),
            end = Offset(x - arrowSize, endY - arrowSize),
            strokeWidth = strokeWidth
        )

        drawLine(
            color = color,
            start = Offset(x, endY),
            end = Offset(x + arrowSize, endY - arrowSize),
            strokeWidth = strokeWidth
        )
    }
}

private fun buildDirectionalSmoothChartPath(
    points: List<Offset>,
    centerY: Float
): Path {
    return buildMonotoneSmoothChartPath(points)
}


private fun buildDirectionalSmoothChartFillPath(
    points: List<Offset>,
    baselineY: Float,
    centerY: Float
): Path {
    val path = Path()

    if (points.isEmpty()) return path

    path.moveTo(points.first().x, baselineY)
    path.lineTo(points.first().x, points.first().y)

    appendMonotoneSmoothSegments(
        path = path,
        points = points
    )

    path.lineTo(points.last().x, baselineY)
    path.close()

    return path
}


private fun buildMonotoneSmoothChartPath(
    points: List<Offset>
): Path {
    val path = Path()

    if (points.isEmpty()) return path

    path.moveTo(points.first().x, points.first().y)

    appendMonotoneSmoothSegments(
        path = path,
        points = points
    )

    return path
}


private fun appendMonotoneSmoothSegments(
    path: Path,
    points: List<Offset>
) {
    if (points.size < 2) return

    if (points.size == 2) {
        path.lineTo(points.last().x, points.last().y)
        return
    }

    val slopes = buildMonotoneSlopes(points)
    val flatEpsilon = 0.75f

    for (index in 0 until points.lastIndex) {
        val current = points[index]
        val next = points[index + 1]

        val dx = next.x - current.x
        val dy = next.y - current.y

        if (kotlin.math.abs(dy) <= flatEpsilon) {
            path.lineTo(next.x, next.y)
            continue
        }

        val cp1x = current.x + dx / 3f
        val cp1y = current.y + slopes[index] * dx / 3f

        val cp2x = next.x - dx / 3f
        val cp2y = next.y - slopes[index + 1] * dx / 3f

        val minY = minOf(current.y, next.y)
        val maxY = maxOf(current.y, next.y)

        path.cubicTo(
            cp1x,
            cp1y.coerceIn(minY, maxY),
            cp2x,
            cp2y.coerceIn(minY, maxY),
            next.x,
            next.y
        )
    }
}


private fun buildMonotoneSlopes(
    points: List<Offset>
): MutableList<Float> {
    val count = points.size
    val segmentSlopes = MutableList(count - 1) { index ->
        val dx = points[index + 1].x - points[index].x
        val dy = points[index + 1].y - points[index].y

        if (kotlin.math.abs(dx) < 0.001f) {
            0f
        } else {
            dy / dx
        }
    }

    val slopes = MutableList(count) { 0f }

    slopes[0] = segmentSlopes.first()
    slopes[count - 1] = segmentSlopes.last()

    for (index in 1 until count - 1) {
        val previousSlope = segmentSlopes[index - 1]
        val nextSlope = segmentSlopes[index]

        slopes[index] = if (
            previousSlope == 0f ||
            nextSlope == 0f ||
            previousSlope.sign() != nextSlope.sign()
        ) {
            0f
        } else {
            (previousSlope + nextSlope) / 2f
        }
    }

    for (index in segmentSlopes.indices) {
        val segmentSlope = segmentSlopes[index]

        if (segmentSlope == 0f) {
            slopes[index] = 0f
            slopes[index + 1] = 0f
            continue
        }

        val a = slopes[index] / segmentSlope
        val b = slopes[index + 1] / segmentSlope
        val sum = a * a + b * b

        if (sum > 9f) {
            val scale = 3f / kotlin.math.sqrt(sum)
            slopes[index] = scale * a * segmentSlope
            slopes[index + 1] = scale * b * segmentSlope
        }
    }

    return slopes
}


private fun Float.sign(): Int {
    return when {
        this > 0f -> 1
        this < 0f -> -1
        else -> 0
    }
}


private fun buildMetricDescription(
    metricTitle: String,
    metricDisplayName: String,
    trend: CycleTrendAnalyticsResult,
    valueFromPoint: (CycleTrendPoint) -> Float
): String {
    val points = trend.points
    if (points.isEmpty()) {
        return "Недостаточно данных для описания динамики показателя $metricDisplayName."
    }

    val periodSummaries = buildCyclePeriodSummaries(
        points = points,
        totalDays = trend.normalizedLength,
        valueFromPoint = valueFromPoint
    )

    if (periodSummaries.size < 2) {
        return "По дням цикла пока недостаточно данных для выделения устойчивой динамики."
    }

    val maxPeriod = periodSummaries.maxByOrNull { it.averageValue }
    val minPeriod = periodSummaries.minByOrNull { it.averageValue }

    if (maxPeriod == null || minPeriod == null) {
        return "По дням цикла пока недостаточно данных для выделения устойчивой динамики."
    }

    val diff = maxPeriod.averageValue - minPeriod.averageValue

    if (diff < 0.5f) {
        return "Показатель меняется умеренно, без выраженного периода подъёма или снижения. Сейчас нет необходимости отдельно подстраивать режим под этот показатель."
    }

    val baseText = "В среднем показатель выше ${maxPeriod.title}, а ниже — ${minPeriod.title}."

    val interpretation = buildMetricTrendInterpretation(
        metricTitle = metricTitle
    )

    val recommendation = buildMetricTrendRecommendation(
        metricTitle = metricTitle
    )

    return "$baseText $interpretation\n\n$recommendation"
}


private data class CyclePeriodSummary(
    val title: String,
    val averageValue: Float
)


private fun buildCyclePeriodSummaries(
    points: List<CycleTrendPoint>,
    totalDays: Int,
    valueFromPoint: (CycleTrendPoint) -> Float
): List<CyclePeriodSummary> {
    val grouped = points.groupBy { point ->
        describeCyclePeriod(
            day = point.day,
            totalDays = totalDays
        )
    }

    return grouped.mapNotNull { (title, periodPoints) ->
        if (periodPoints.isEmpty()) {
            null
        } else {
            CyclePeriodSummary(
                title = title,
                averageValue = periodPoints
                    .map { valueFromPoint(it) }
                    .average()
                    .toFloat()
            )
        }
    }
}


private fun buildMetricTrendInterpretation(
    metricTitle: String
): String {
    return when (metricTitle) {
        "напряжение" -> {
            "Такой подъём может отражать период большей эмоциональной или физиологической нагрузки."
        }

        "дискомфорт" -> {
            "Рост дискомфорта может быть связан с фазовыми изменениями и может влиять на активность, настроение и концентрацию."
        }

        "энергия" -> {
            "Колебания энергии показывают, в какие периоды цикла организм может быть более или менее ресурсным."
        }

        "настроение" -> {
            "Такая динамика помогает выделить периоды, когда эмоциональное состояние может быть более устойчивым или, наоборот, более изменчивым."
        }

        "концентрация" -> {
            "Изменения концентрации могут показывать, в какие периоды проще или сложнее выполнять задачи, требующие внимания."
        }

        else -> {
            "Такая динамика может быть связана с индивидуальными особенностями цикла."
        }
    }
}


private fun buildMetricTrendRecommendation(
    metricTitle: String
): String {
    return when (metricTitle) {
        "напряжение" -> {
            "В период, когда напряжение выше, лучше снижать нагрузку и заранее планировать отдых."
        }

        "дискомфорт" -> {
            "В период усиления дискомфорта лучше выбирать более мягкую активность и оставлять время на восстановление."
        }

        "энергия" -> {
            "Более сложные и активные дела лучше планировать на период, когда энергия выше, а в период снижения энергии — оставлять больше пространства для отдыха."
        }

        "настроение" -> {
            "В период менее устойчивого настроения стоит острожнее распределять нагрузку и прислушиваться к своим чувствам."
        }

        "концентрация" -> {
            "Задачи, требующие внимания, лучше планировать на период, когда концентрация выше, а при её снижении — выбирать более простые или рутинные дела."
        }

        else -> {
            "Эти периоды можно учитывать при планировании нагрузки и восстановления."
        }
    }
}


private fun describeCyclePeriod(
    day: Int,
    totalDays: Int
): String {
    val safeTotal = totalDays.coerceAtLeast(1)
    val ratio = day.toFloat() / safeTotal.toFloat()

    return when {
        day <= 7 -> "в первую неделю цикла"
        day <= 14 -> "во вторую неделю цикла"
        ratio < 0.65f -> "в середине цикла"
        ratio < 0.85f -> "ближе к концу цикла"
        else -> "в конце цикла"
    }
}


private data class PhaseDescriptionSource(
    val title: String,
    val metrics: PhaseMetricAnalytics
)


private data class PhaseTextDescription(
    val recommendationText: String
)


private data class PhaseIndicatorItem(
    val title: String,
    val arrow: String,
    val color: Color
)


private enum class MetricAxisMode {
    NEGATIVE_UP,
    POSITIVE_BIDIRECTIONAL
}