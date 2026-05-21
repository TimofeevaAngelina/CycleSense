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
import com.timofeeva.cyclesense.ui.theme.CycleSenseTextSecondary

@Composable
fun HeartRateTrendChart(
    trend: HeartRateAnalyticsResult,
    modifier: Modifier = Modifier
) {
    if (trend.points.isEmpty()) return

    val rangeChartPoints = trend.points
        .filter { it.minHeartRate != null && it.maxHeartRate != null }
        .sortedBy { it.dayInCycle }
        .mapNotNull { point ->
            val min = point.minHeartRate
            val max = point.maxHeartRate

            if (min == null || max == null) {
                null
            } else {
                HeartRateRangePoint(
                    day = point.dayInCycle,
                    min = min,
                    max = max
                )
            }
        }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        HeartRateRangeChart(
            title = "Диапазон пульса",
            yAxisTitle = "Min–max, уд/мин",
            points = rangeChartPoints,
            description = buildHeartRateRangeDescription(rangeChartPoints),
            modifier = Modifier.padding(top = 0.dp)
        )
    }
}



private data class HeartRateRangePoint(
    val day: Int,
    val min: Float,
    val max: Float
)

@Composable
private fun HeartRateRangeChart(
    title: String,
    yAxisTitle: String,
    points: List<HeartRateRangePoint>,
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

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = CycleSenseTextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Text(
            text = yAxisTitle,
            style = MaterialTheme.typography.bodySmall,
            color = CycleSenseTextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(top = 8.dp)
        ) {
            val sortedPoints = points.sortedBy { it.day }

            val axisTextSize = when {
                size.width < 360f -> 23f
                size.width < 520f -> 24f
                else -> 25f
            }

            val yTextPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(111, 104, 120)
                textSize = axisTextSize
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.LEFT
            }

            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(111, 104, 120)
                textSize = axisTextSize
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
            }

            val minX = sortedPoints.minOf { it.day }.toFloat()
            val maxX = sortedPoints.maxOf { it.day }.toFloat()
            val xRange = (maxX - minX).takeIf { it > 0f } ?: 1f

            val minYRaw = sortedPoints.minOf { it.min }
            val maxYRaw = sortedPoints.maxOf { it.max }

            val minY = minYRaw - 3f
            val maxY = maxYRaw + 3f
            val yRange = (maxY - minY).takeIf { it > 0f } ?: 1f

            val yLabelValues = listOf(
                maxY,
                (maxY + minY) / 2f,
                minY
            ).distinct()

            val maxYLabelWidth = yLabelValues
                .map { value -> yTextPaint.measureText(value.roundToInt().toString()) }
                .maxOrNull()
                ?: 0f

            val leftPadding = (maxYLabelWidth + 18f).coerceAtLeast(58f)
            val rightPadding = 18f
            val topPadding = 18f
            val bottomPadding = 36f

            val chartWidth = size.width - leftPadding - rightPadding
            val chartHeight = size.height - topPadding - bottomPadding
            val baselineY = topPadding + chartHeight

            fun mapX(day: Float): Float {
                return leftPadding + ((day - minX) / xRange) * chartWidth
            }

            fun mapY(value: Float): Float {
                return topPadding + (1f - ((value - minY) / yRange)) * chartHeight
            }

            val rangeColor = Color(0xFF6B5B95)
            val gridColor = Color.LightGray.copy(alpha = 0.30f)
            val axisColor = Color.Gray.copy(alpha = 0.55f)

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
                    value.roundToInt().toString(),
                    4f,
                    y + axisTextSize / 3f,
                    yTextPaint
                )
            }



            val lastDay = maxX.toInt()

            val xLabelCandidates = buildList {
                var day = 7

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
                    baselineY + axisTextSize,
                    textPaint
                )
            }

            sortedPoints.forEach { point ->
                val x = mapX(point.day.toFloat())
                val yMin = mapY(point.min)
                val yMax = mapY(point.max)

                drawLine(
                    color = rangeColor,
                    start = Offset(x, yMin),
                    end = Offset(x, yMax),
                    strokeWidth = 4.5f
                )

                drawCircle(
                    color = rangeColor,
                    radius = 4.5f,
                    center = Offset(x, yMin)
                )

                drawCircle(
                    color = rangeColor,
                    radius = 4.5f,
                    center = Offset(x, yMax)
                )

                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = 2.1f,
                    center = Offset(x, yMin)
                )

                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = 2.1f,
                    center = Offset(x, yMax)
                )
            }
        }
    }
}






private fun buildHeartRateRangeDescription(
    points: List<HeartRateRangePoint>
): String {
    if (points.isEmpty()) {
        return "Недостаточно данных для описания диапазона пульса."
    }

    val periodSummaries = buildHeartRateRangePeriodSummaries(
        points = points,
        valueProvider = { it.max - it.min }
    )

    val widestPeriod = periodSummaries.maxByOrNull { it.averageValue }
    val narrowestPeriod = periodSummaries.minByOrNull { it.averageValue }

    if (widestPeriod == null || narrowestPeriod == null) {
        return "Данных пока недостаточно для выделения устойчивой динамики диапазона пульса."
    }

    val diff = widestPeriod.averageValue - narrowestPeriod.averageValue

    if (diff < 5f) {
        return "Диапазон пульса выглядит достаточно ровным, без выраженных периодов расширения."
    }

    return "Самый широкий диапазон чаще наблюдается ${widestPeriod.title}. " +
            "Это может отражать более неоднородную нагрузку в течение дня: например, сочетание покоя и активных периодов. " +
            "В такие дни полезно следить за восстановлением и не добавлять лишнюю интенсивную нагрузку без необходимости."
}

private data class HeartRatePeriodSummary(
    val title: String,
    val averageValue: Float
)



private fun buildHeartRateRangePeriodSummaries(
    points: List<HeartRateRangePoint>,
    valueProvider: (HeartRateRangePoint) -> Float
): List<HeartRatePeriodSummary> {
    val totalDays = points.maxOfOrNull { it.day } ?: 28

    return points
        .groupBy { point ->
            describeCyclePeriod(
                day = point.day,
                totalDays = totalDays
            )
        }
        .mapNotNull { (title, periodPoints) ->
            if (periodPoints.isEmpty()) {
                null
            } else {
                HeartRatePeriodSummary(
                    title = title,
                    averageValue = periodPoints
                        .map { valueProvider(it) }
                        .average()
                        .toFloat()
                )
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