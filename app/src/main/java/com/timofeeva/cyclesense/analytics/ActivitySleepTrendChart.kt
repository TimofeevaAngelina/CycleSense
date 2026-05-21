package com.timofeeva.cyclesense.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import com.timofeeva.cyclesense.ui.theme.CycleSenseTextSecondary
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity

@Composable
fun ActivitySleepTrendChart(
    analytics: ActivitySleepAnalyticsResult,
    heartRateAnalytics: HeartRateAnalyticsResult? = null,
    modifier: Modifier = Modifier
) {
    val points = analytics.dayPoints.sortedBy { it.day }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        PhysiologyPhaseProfileBlock(
            analytics = analytics,
            heartRateAnalytics = heartRateAnalytics
        )

        SingleLineTrendChart(
            title = "Шаги",
            yAxisTitle = "Шаги",
            points = points.mapNotNull { point ->
                point.avgSteps?.let {
                    SimpleChartPoint(
                        day = point.day,
                        value = it
                    )
                }
            },
            lineColor = Color(0xFF6B5B95),
            minYForced = null,
            yPaddingRatio = 0.55f,
            valueFormatter = { it.roundToInt().toString() },
            description = buildPhysiologyMetricDescription(
                metric = PhysiologyMetricKind.STEPS,
                points = points.mapNotNull { point ->
                    point.avgSteps?.let {
                        SimpleChartPoint(day = point.day, value = it)
                    }
                },
                totalDays = analytics.normalizedLength
            ),
            modifier = Modifier.padding(top = 16.dp)
        )

        SingleLineTrendChart(
            title = "Калории",
            yAxisTitle = "Ккал",
            points = points.mapNotNull { point ->
                point.avgCalories?.let {
                    SimpleChartPoint(
                        day = point.day,
                        value = it
                    )
                }
            },
            lineColor = Color(0xFF6B5B95),
            minYForced = null,
            yPaddingRatio = 0.45f,
            valueFormatter = { it.roundToInt().toString() },
            description = buildPhysiologyMetricDescription(
                metric = PhysiologyMetricKind.CALORIES,
                points = points.mapNotNull { point ->
                    point.avgCalories?.let {
                        SimpleChartPoint(day = point.day, value = it)
                    }
                },
                totalDays = analytics.normalizedLength
            ),
            modifier = Modifier.padding(top = 20.dp)
        )

        SingleLineTrendChart(
            title = "Сон",
            yAxisTitle = "Часы",
            points = points.mapNotNull { point ->
                point.avgSleepHours?.let {
                    SimpleChartPoint(
                        day = point.day,
                        value = it
                    )
                }
            },
            lineColor = Color(0xFF6B5B95),
            minYForced = null,
            yPaddingRatio = 0.65f,
            valueFormatter = { String.format("%.1f", it) },
            description = buildPhysiologyMetricDescription(
                metric = PhysiologyMetricKind.SLEEP,
                points = points.mapNotNull { point ->
                    point.avgSleepHours?.let {
                        SimpleChartPoint(day = point.day, value = it)
                    }
                },
                totalDays = analytics.normalizedLength
            ),
            modifier = Modifier.padding(top = 20.dp)
        )

        heartRateAnalytics?.let { heartRate ->
            val averageHeartRatePoints = heartRate.points
                .filter { it.avgHeartRate > 0f }
                .map { point ->
                    SimpleChartPoint(
                        day = point.dayInCycle,
                        value = point.avgHeartRate
                    )
                }

            SingleLineTrendChart(
                title = "Средний пульс",
                yAxisTitle = "уд/мин",
                points = averageHeartRatePoints,
                lineColor = Color(0xFF6B5B95),
                minYForced = null,
                yPaddingRatio = 0.35f,
                valueFormatter = { it.roundToInt().toString() },
                description = buildPhysiologyMetricDescription(
                    metric = PhysiologyMetricKind.AVERAGE_HEART_RATE,
                    points = averageHeartRatePoints,
                    totalDays = analytics.normalizedLength
                ),
                modifier = Modifier.padding(top = 20.dp)
            )
        }

        heartRateAnalytics?.let { heartRate ->
            HeartRateTrendChart(
                trend = heartRate,
                modifier = Modifier.padding(top = 20.dp)
            )
        }


    }
}

private data class SimpleChartPoint(
    val day: Int,
    val value: Float
)

@Composable
private fun SingleLineTrendChart(
    title: String,
    yAxisTitle: String,
    points: List<SimpleChartPoint>,
    lineColor: Color,
    minYForced: Float? = null,
    yPaddingRatio: Float = 0.10f,
    valueFormatter: (Float) -> String,
    description: String,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall
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

        Text(
            text = yAxisTitle,
            style = MaterialTheme.typography.bodySmall,
            color = CycleSenseTextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(top = 8.dp)
        ) {
            val sortedPoints = points.sortedBy { it.day }

            val axisTextSize = when {
                size.width < 360f -> 23f
                size.width < 520f -> 24f
                else -> 25f
            }

            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(111, 104, 120)
                textSize = axisTextSize
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
            }

            val yTextPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(111, 104, 120)
                textSize = axisTextSize
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.LEFT
            }

            val minX = sortedPoints.minOf { it.day }.toFloat()
            val maxX = sortedPoints.maxOf { it.day }.toFloat()
            val xRange = (maxX - minX).takeIf { it > 0f } ?: 1f

            val minYRaw = sortedPoints.minOf { it.value }
            val maxYRaw = sortedPoints.maxOf { it.value }

            val rawRange = (maxYRaw - minYRaw).takeIf { it > 0f } ?: 1f
            val padding = rawRange * yPaddingRatio

            val minY = minYForced ?: (minYRaw - padding)
            val maxY = maxYRaw + padding

            val yLabelValues = listOf(
                maxY,
                (maxY + minY) / 2f,
                minY
            ).distinct()

            val maxYLabelWidth = yLabelValues
                .map { value -> yTextPaint.measureText(valueFormatter(value)) }
                .maxOrNull()
                ?: 0f

            val leftPadding = (maxYLabelWidth + 18f).coerceAtLeast(54f)
            val rightPadding = 18f
            val topPadding = 18f
            val bottomPadding = 36f

            val chartWidth = size.width - leftPadding - rightPadding
            val chartHeight = size.height - topPadding - bottomPadding
            val baselineY = topPadding + chartHeight



            val yRange = (maxY - minY).takeIf { it > 0f } ?: 1f

            fun mapX(day: Float): Float {
                return leftPadding + ((day - minX) / xRange) * chartWidth
            }

            fun mapY(value: Float): Float {
                return topPadding + (1f - ((value - minY) / yRange)) * chartHeight
            }

            val gridColor = Color.LightGray.copy(alpha = 0.30f)
            val axisColor = Color.Gray.copy(alpha = 0.55f)
            val fillColor = lineColor.copy(alpha = 0.12f)

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



            yLabelValues.forEach { value ->
                val y = mapY(value)

                drawLine(
                    color = gridColor,
                    start = Offset(leftPadding, y),
                    end = Offset(leftPadding + chartWidth, y),
                    strokeWidth = 1f
                )

                drawContext.canvas.nativeCanvas.drawText(
                    valueFormatter(value),
                    4f,
                    y + axisTextSize / 3f,
                    yTextPaint
                )
            }

            val xLabelCandidates = buildList {
                var day = 7
                val lastDay = maxX.toInt()

                while (day <= lastDay) {
                    add(day)
                    day += 7
                }

                if (lastDay !in this) {
                    add(lastDay)
                }
            }.sorted()
                .filter { it.toFloat() in minX..maxX }

            xLabelCandidates.forEach { day ->
                val x = mapX(day.toFloat())

                drawLine(
                    color = Color.LightGray.copy(alpha = 0.18f),
                    start = Offset(x, topPadding),
                    end = Offset(x, baselineY),
                    strokeWidth = 1f
                )

                drawContext.canvas.nativeCanvas.drawText(
                    day.toString(),
                    x,
                    baselineY + 24f,
                    textPaint
                )
            }

            val offsets = sortedPoints.map { point ->
                Offset(
                    x = mapX(point.day.toFloat()),
                    y = mapY(point.value)
                )
            }

            if (offsets.size == 1) {
                drawCircle(
                    color = lineColor,
                    radius = 4.5f,
                    center = offsets.first()
                )
            } else {
                val linePath = buildSmoothPhysiologyChartPath(offsets)
                val fillPath = buildSmoothPhysiologyChartFillPath(
                    points = offsets,
                    baselineY = baselineY
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

                offsets.forEach { point ->
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
        }
    }
}

private fun buildSmoothPhysiologyChartPath(
    points: List<Offset>
): Path {
    val path = Path()

    if (points.isEmpty()) return path

    path.moveTo(points.first().x, points.first().y)

    appendSmoothPhysiologySegments(
        path = path,
        points = points
    )

    return path
}


private fun buildSmoothPhysiologyChartFillPath(
    points: List<Offset>,
    baselineY: Float
): Path {
    val path = Path()

    if (points.isEmpty()) return path

    path.moveTo(points.first().x, baselineY)
    path.lineTo(points.first().x, points.first().y)

    appendSmoothPhysiologySegments(
        path = path,
        points = points
    )

    path.lineTo(points.last().x, baselineY)
    path.close()

    return path
}


private fun appendSmoothPhysiologySegments(
    path: Path,
    points: List<Offset>
) {
    if (points.size < 2) return

    if (points.size == 2) {
        path.lineTo(points.last().x, points.last().y)
        return
    }

    val slopes = buildMonotonePhysiologySlopes(points)
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


private fun buildMonotonePhysiologySlopes(
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
            previousSlope.signForPhysiology() != nextSlope.signForPhysiology()
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


private fun Float.signForPhysiology(): Int {
    return when {
        this > 0f -> 1
        this < 0f -> -1
        else -> 0
    }
}

private enum class PhysiologyMetricKind {
    STEPS,
    CALORIES,
    SLEEP,
    AVERAGE_HEART_RATE
}

private fun buildPhysiologyMetricDescription(
    metric: PhysiologyMetricKind,
    points: List<SimpleChartPoint>,
    totalDays: Int
): String {
    if (points.isEmpty()) {
        return "Недостаточно данных для описания динамики."
    }

    val maxPoint = points.maxByOrNull { it.value }
    val minPoint = points.minByOrNull { it.value }

    if (maxPoint == null || minPoint == null) {
        return "Недостаточно данных для выделения устойчивой динамики."
    }

    val diff = maxPoint.value - minPoint.value
    val isMeaningful = when (metric) {
        PhysiologyMetricKind.STEPS -> diff >= 800f
        PhysiologyMetricKind.CALORIES -> diff >= 120f
        PhysiologyMetricKind.SLEEP -> diff >= 0.3f
        PhysiologyMetricKind.AVERAGE_HEART_RATE -> diff >= 3f
    }

    if (!isMeaningful) {
        return when (metric) {
            PhysiologyMetricKind.STEPS -> "Активность меняется умеренно, без выраженных спадов или подъёмов."
            PhysiologyMetricKind.CALORIES -> "Расход калорий выглядит относительно стабильным, без выраженных колебаний."
            PhysiologyMetricKind.SLEEP -> "Продолжительность сна выглядит достаточно стабильной."
            PhysiologyMetricKind.AVERAGE_HEART_RATE -> "Средний пульс выглядит достаточно стабильным, без выраженных периодов повышения или снижения."
        }
    }

    val maxPeriod = describeCyclePeriod(maxPoint.day, totalDays)
    val minPeriod = describeCyclePeriod(minPoint.day, totalDays)

    return when (metric) {
        PhysiologyMetricKind.STEPS -> {
            "Больше всего шагов обычно приходится $maxPeriod, меньше всего — $minPeriod. Если снижение активности совпадает с ухудшением самочувствия, в этот период лучше выбирать мягкую нагрузку и не стремиться к резкому увеличению активности."
        }

        PhysiologyMetricKind.CALORIES -> {
            "Расход калорий выше $maxPeriod и ниже $minPeriod. Этот показатель лучше интерпретировать вместе с шагами и сном: он может отражать как активность, так и общий уровень нагрузки."
        }

        PhysiologyMetricKind.SLEEP -> {
            "Более длительный сон чаще наблюдается $maxPeriod, а более короткий — $minPeriod. В период сокращения сна стоит заранее снижать перегрузку и уделять больше внимания восстановлению."
        }

        PhysiologyMetricKind.AVERAGE_HEART_RATE -> {
            "Средний пульс выше $maxPeriod и ниже $minPeriod. Если повышение пульса совпадает с недосыпом, напряжением или дискомфортом, в этот период лучше внимательнее относиться к восстановлению и интенсивности нагрузки."
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

@Composable
private fun PhysiologyPhaseProfileBlock(
    analytics: ActivitySleepAnalyticsResult,
    heartRateAnalytics: HeartRateAnalyticsResult?,
    modifier: Modifier = Modifier
) {
    val fontScale = LocalDensity.current.fontScale
    val shouldStackIndicators = fontScale >= 1.35f

    val hasAnyActivitySleepData = analytics.phasePoints.any { phase ->
        phase.avgSteps != null ||
                phase.avgCalories != null ||
                phase.avgSleepHours != null
    }

    val hasAnyHeartRateData =
        heartRateAnalytics?.phasePoints?.any { phase ->
            phase.heartRateRangeOrNull() != null ||
                    phase.averageHeartRateOrNull() != null
        } == true ||
                heartRateAnalytics?.points?.any { point ->
                    point.samplesCount > 0
                } == true

    if (!hasAnyActivitySleepData && !hasAnyHeartRateData) {
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {


        analytics.phasePoints.forEach { phase ->
            val heartRatePhase = heartRateAnalytics?.phasePoints
                ?.firstOrNull { it.phaseTitle == phase.phaseTitle }

            val hasPhaseData =
                phase.avgSteps != null ||
                        phase.avgCalories != null ||
                        phase.avgSleepHours != null ||
                        heartRatePhase?.heartRateRangeOrNull() != null ||
                        heartRatePhase?.averageHeartRateOrNull() != null

            if (!hasPhaseData) {
                return@forEach
            }

            val indicators = buildPhysiologyPhaseIndicatorItems(
                phase = phase,
                allPhases = analytics.phasePoints,
                heartRatePhase = heartRatePhase,
                allHeartRatePhases = heartRateAnalytics?.phasePoints.orEmpty()
            )

            Column(
                modifier = Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = phase.phaseTitle,
                    style = MaterialTheme.typography.titleSmall
                )

                PhysiologyIndicatorList(
                    indicators = indicators,
                    stacked = shouldStackIndicators
                )

                Text(
                    text = buildPhysiologyPhaseDescription(
                        phase = phase,
                        allPhases = analytics.phasePoints,
                        heartRatePhase = heartRatePhase,
                        allHeartRatePhases = heartRateAnalytics?.phasePoints.orEmpty()
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun PhysiologyIndicatorList(
    indicators: List<PhysiologyPhaseIndicatorItem>,
    stacked: Boolean,
    modifier: Modifier = Modifier
) {
    if (stacked) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            indicators.forEach { item ->
                PhysiologyIndicatorText(
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
                        PhysiologyIndicatorText(
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
private fun PhysiologyIndicatorText(
    item: PhysiologyPhaseIndicatorItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyMedium,
            color = item.color,
            softWrap = true
        )
    }
}

private fun HeartRatePhasePoint.averageHeartRateOrNull(): Float? {
    val min = avgMinHeartRate
    val max = avgMaxHeartRate

    return when {
        min != null && max != null -> (min + max) / 2f
        min != null -> min
        max != null -> max
        else -> null
    }
}

private fun HeartRatePhasePoint.heartRateRangeOrNull(): Float? {
    val min = avgMinHeartRate
    val max = avgMaxHeartRate

    return if (min != null && max != null) {
        max - min
    } else {
        null
    }
}

private fun buildPhysiologyPhaseIndicatorItems(
    phase: ActivitySleepPhasePoint,
    allPhases: List<ActivitySleepPhasePoint>,
    heartRatePhase: HeartRatePhasePoint?,
    allHeartRatePhases: List<HeartRatePhasePoint>
): List<PhysiologyPhaseIndicatorItem> {
    val avgSteps = allPhases.mapNotNull { it.avgSteps }.averageOrNull()
    val avgCalories = allPhases.mapNotNull { it.avgCalories }.averageOrNull()
    val avgSleep = allPhases.mapNotNull { it.avgSleepHours }.averageOrNull()

    val currentHrRange = heartRatePhase?.heartRateRangeOrNull()
    val avgHrRange = allHeartRatePhases
        .mapNotNull { hr -> hr.heartRateRangeOrNull() }
        .averageOrNull()

    val currentAvgHeartRate = heartRatePhase?.averageHeartRateOrNull()
    val avgHeartRate = allHeartRatePhases
        .mapNotNull { hr -> hr.averageHeartRateOrNull() }
        .averageOrNull()

    return buildList {
        phase.avgSteps?.let { value ->
            add(
                buildPhysiologyPhaseIndicatorItem(
                    title = "Шаги",
                    valueText = "${value.roundToInt()}",
                    value = value,
                    average = avgSteps,
                    threshold = 500f,
                    higherIsBetter = true
                )
            )
        }

        phase.avgCalories?.let { value ->
            add(
                buildPhysiologyPhaseIndicatorItem(
                    title = "Калории",
                    valueText = "${value.roundToInt()}",
                    value = value,
                    average = avgCalories,
                    threshold = 80f,
                    higherIsBetter = true
                )
            )
        }

        phase.avgSleepHours?.let { value ->
            add(
                buildPhysiologyPhaseIndicatorItem(
                    title = "Сон",
                    valueText = "${String.format("%.1f", value)} ч",
                    value = value,
                    average = avgSleep,
                    threshold = 0.25f,
                    higherIsBetter = true
                )
            )
        }

        if (currentHrRange != null) {
            add(
                buildPhysiologyPhaseIndicatorItem(
                    title = "Диапазон пульса",
                    valueText = "${currentHrRange.roundToInt()}",
                    value = currentHrRange,
                    average = avgHrRange,
                    threshold = 5f,
                    higherIsBetter = false
                )
            )
        } else if (currentAvgHeartRate != null) {
            add(
                buildPhysiologyPhaseIndicatorItem(
                    title = "Средний пульс",
                    valueText = "${currentAvgHeartRate.roundToInt()}",
                    value = currentAvgHeartRate,
                    average = avgHeartRate,
                    threshold = 3f,
                    higherIsBetter = false
                )
            )
        }
    }
}


private fun buildPhysiologyPhaseIndicatorItem(
    title: String,
    valueText: String,
    value: Float,
    average: Float?,
    threshold: Float,
    higherIsBetter: Boolean
): PhysiologyPhaseIndicatorItem {
    val improvementColor = Color(0xFF96DEBD)
    val worseningColor = Color(0xFFDE96B7)
    val neutralColor = Color(0xFFD0C2A4)

    if (average == null) {
        return PhysiologyPhaseIndicatorItem(
            text = "$title: $valueText —",
            color = neutralColor
        )
    }

    val diff = value - average

    return when {
        diff >= threshold -> {
            PhysiologyPhaseIndicatorItem(
                text = "$title: $valueText ↑",
                color = if (higherIsBetter) improvementColor else worseningColor
            )
        }

        diff <= -threshold -> {
            PhysiologyPhaseIndicatorItem(
                text = "$title: $valueText ↓",
                color = if (higherIsBetter) worseningColor else improvementColor
            )
        }

        else -> {
            PhysiologyPhaseIndicatorItem(
                text = "$title: $valueText —",
                color = neutralColor
            )
        }
    }
}


private data class PhysiologyPhaseIndicatorItem(
    val text: String,
    val color: Color
)

private fun buildPhysiologyPhaseDescription(
    phase: ActivitySleepPhasePoint,
    allPhases: List<ActivitySleepPhasePoint>,
    heartRatePhase: HeartRatePhasePoint?,
    allHeartRatePhases: List<HeartRatePhasePoint>
): String {
    val avgSteps = allPhases.mapNotNull { it.avgSteps }.averageOrNull()
    val avgCalories = allPhases.mapNotNull { it.avgCalories }.averageOrNull()
    val avgSleep = allPhases.mapNotNull { it.avgSleepHours }.averageOrNull()

    val valuesText = buildString {
        append("В среднем: ")

        append(
            phase.avgSteps?.let { "${it.roundToInt()} шагов" } ?: "шаги — нет данных"
        )

        append(", ")

        append(
            phase.avgCalories?.let { "${it.roundToInt()} ккал" } ?: "калории — нет данных"
        )

        append(", ")

        append(
            phase.avgSleepHours?.let { "сон ${String.format("%.1f", it)} ч" } ?: "сон — нет данных"
        )

        val minHr = heartRatePhase?.avgMinHeartRate
        val maxHr = heartRatePhase?.avgMaxHeartRate
        val avgHr = heartRatePhase?.averageHeartRateOrNull()

        if (minHr != null && maxHr != null) {
            append(", диапазон пульса ${minHr.roundToInt()}–${maxHr.roundToInt()} уд/мин")
        } else if (avgHr != null) {
            append(", средний пульс ${avgHr.roundToInt()} уд/мин")
        }

        append(".")
    }

    val interpretation = buildPhysiologyPhaseInterpretation(
        phase = phase,
        avgSteps = avgSteps,
        avgCalories = avgCalories,
        avgSleep = avgSleep,
        heartRatePhase = heartRatePhase,
        allHeartRatePhases = allHeartRatePhases
    )

    val recommendation = buildPhysiologyPhaseRecommendation(
        phase = phase,
        avgSteps = avgSteps,
        avgCalories = avgCalories,
        avgSleep = avgSleep,
        heartRatePhase = heartRatePhase,
        allHeartRatePhases = allHeartRatePhases
    )

    return "$interpretation $recommendation"
}

private fun buildPhysiologyPhaseInterpretation(
    phase: ActivitySleepPhasePoint,
    avgSteps: Float?,
    avgCalories: Float?,
    avgSleep: Float?,
    heartRatePhase: HeartRatePhasePoint?,
    allHeartRatePhases: List<HeartRatePhasePoint>
): String {
    val sleepLower = phase.avgSleepHours != null && avgSleep != null &&
            avgSleep - phase.avgSleepHours >= 0.25f

    val sleepHigher = phase.avgSleepHours != null && avgSleep != null &&
            phase.avgSleepHours - avgSleep >= 0.25f

    val stepsLower = phase.avgSteps != null && avgSteps != null &&
            avgSteps - phase.avgSteps >= 500f

    val stepsHigher = phase.avgSteps != null && avgSteps != null &&
            phase.avgSteps - avgSteps >= 500f

    val caloriesHigher = phase.avgCalories != null && avgCalories != null &&
            phase.avgCalories - avgCalories >= 80f

    val caloriesLower = phase.avgCalories != null && avgCalories != null &&
            avgCalories - phase.avgCalories >= 80f

    val currentHrRange = heartRatePhase?.heartRateRangeOrNull()
    val avgHrRange = allHeartRatePhases
        .mapNotNull { hr -> hr.heartRateRangeOrNull() }
        .averageOrNull()

    val heartRateRangeWider = currentHrRange != null && avgHrRange != null &&
            currentHrRange - avgHrRange >= 5f

    val heartRateRangeNarrower = currentHrRange != null && avgHrRange != null &&
            avgHrRange - currentHrRange >= 5f

    return when {
        sleepLower && stepsLower && heartRateRangeWider -> {
            "В этот период одновременно короче сон, ниже активность и шире диапазон пульса, что может быть связано с повышенной нагрузкой и недостаточным восстановлением."
        }

        sleepLower && stepsLower -> {
            "В этот период активность ниже, однако сон также короче, поэтому восстановление может быть неполным.."
        }

        sleepLower && caloriesHigher -> {
            "Сон короче, а расход калорий выше, поэтому организму может требоваться больше восстановления."
        }

        sleepHigher && stepsHigher -> {
            "Этот период выглядит более ресурсным: сон дольше, а активность выше."
        }

        stepsHigher && caloriesHigher -> {
            "По активности и расходу калорий этот период выглядит более энергичным."
        }

        stepsLower && caloriesLower -> {
            "Этот период более спокойный по уровню активности и расходу энергии."
        }

        heartRateRangeWider -> {
            "Диапазон пульса в этот период шире обычного, что может отражать более неоднородную нагрузку в течение дня."
        }

        heartRateRangeNarrower -> {
            "Диапазон пульса в этот период уже обычного, показатели выглядят более стабильными."
        }

        sleepLower -> {
            "Главная особенность этого периода — более короткий сон."
        }

        sleepHigher -> {
            "В этот период сон обычно дольше, что может способствовать восстановлению."
        }

        stepsLower -> {
            "Активность в этот период ниже, чем в среднем по циклу."
        }

        stepsHigher -> {
            "Активность в этот период выше, чем в среднем по циклу."
        }

        caloriesHigher -> {
            "Расход калорий этот период выше, что может отражать более активные дни."
        }

        caloriesLower -> {
            "Расход калорий этот период ниже, фаза выглядит более спокойной по нагрузке."
        }

        else -> {
            "Показатели в этот период находятся примерно на индивидуальном уровне без выраженных отклонений."
        }
    }
}

private fun buildPhysiologyPhaseRecommendation(
    phase: ActivitySleepPhasePoint,
    avgSteps: Float?,
    avgCalories: Float?,
    avgSleep: Float?,
    heartRatePhase: HeartRatePhasePoint?,
    allHeartRatePhases: List<HeartRatePhasePoint>
): String {
    val sleepLower = phase.avgSleepHours != null && avgSleep != null &&
            avgSleep - phase.avgSleepHours >= 0.25f

    val sleepHigher = phase.avgSleepHours != null && avgSleep != null &&
            phase.avgSleepHours - avgSleep >= 0.25f

    val stepsLower = phase.avgSteps != null && avgSteps != null &&
            avgSteps - phase.avgSteps >= 500f

    val stepsHigher = phase.avgSteps != null && avgSteps != null &&
            phase.avgSteps - avgSteps >= 500f

    val caloriesHigher = phase.avgCalories != null && avgCalories != null &&
            phase.avgCalories - avgCalories >= 80f

    val caloriesLower = phase.avgCalories != null && avgCalories != null &&
            avgCalories - phase.avgCalories >= 80f

    val currentHrRange = heartRatePhase?.heartRateRangeOrNull()
    val avgHrRange = allHeartRatePhases
        .mapNotNull { hr -> hr.heartRateRangeOrNull() }
        .averageOrNull()

    val heartRateRangeWider = currentHrRange != null && avgHrRange != null &&
            currentHrRange - avgHrRange >= 5f

    return when {
        sleepLower && stepsLower && heartRateRangeWider -> {
            "Лучше не повышать нагрузку резко: подойдут мягкая активность и прогулки в спокойном темпе."
        }

        sleepLower && stepsLower -> {
            "Лучше не повышать нагрузку резко: подойдут мягкая активность, прогулки и отдых."
        }

        sleepLower && caloriesHigher -> {
            "Стоит внимательнее относиться к восстановлению: не перегружать вечер, следить за сном и не добавлять лишнюю интенсивную нагрузку."
        }

        heartRateRangeWider -> {
            "В этот период лучше внимательнее отслеживать восстановление и не добавлять интенсивную нагрузку без необходимости."
        }

        sleepHigher && stepsHigher -> {
            "При хорошем самочувствии этот период можно использовать для более активных дел и задач, требующих энергии."
        }

        stepsHigher && caloriesHigher -> {
            "Если самочувствие хорошее, можно сохранять активный режим, но важно не забывать про восстановление."
        }

        stepsLower && caloriesLower -> {
            "В этот период может быть полезен более спокойный режим активности: регулярное движение без повышенных требований к себе."
        }

        sleepLower -> {
            "Главный акцент лучше сделать на восстановлении: регулярный сон, меньше вечерней перегрузки и больше отдыха."
        }

        stepsLower -> {
            "Снижение активности не обязательно плохо; лучше выбирать посильную активность без перегрузки."
        }

        caloriesHigher -> {
            "При повышенном расходе энергии стоит следить за питанием, восстановлением и качеством сна."
        }

        sleepHigher || stepsHigher -> {
            "Этот период достаточно стабильный; при хорошем самочувствии можно планировать обычную или умеренно повышенную активность."
        }

        else -> {
            "Специально менять режим по этим данным не требуется: можно придерживаться привычной нагрузки и ориентироваться на самочувствие в течение дня."
        }
    }
}

private fun describeRelativeMetricWithValue(
    value: Float,
    average: Float,
    highText: String,
    lowText: String,
    threshold: Float,
    valueText: String
): String? {
    return when {
        value - average >= threshold -> "$highText — $valueText"
        average - value >= threshold -> "$lowText — $valueText"
        else -> null
    }
}

private fun List<Float>.averageOrNull(): Float? {
    if (isEmpty()) return null
    return average().toFloat()
}