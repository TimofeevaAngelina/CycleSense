package com.timofeeva.cyclesense.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.timofeeva.cyclesense.ui.theme.CycleSenseTextSecondary
import androidx.compose.ui.Alignment

@Composable
fun RelationshipTrendChart(
    analytics: RelationshipAnalyticsResult,
    modifier: Modifier = Modifier
) {
    val series = analytics.series
        .filter { item ->
            item.points.any { it.normalizedValue != null }
        }

    if (series.isEmpty()) return

    val colors = listOf(
        Color(0xFF6B5B95),
        Color(0xFFC23D7A),
        Color(0xFF3DC285)
    )

    Column(
        modifier = modifier.fillMaxWidth()
    ) {


        RelationshipLegend(
            series = series,
            colors = colors,
            modifier = Modifier.padding(top = 10.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .padding(top = 12.dp)
        ) {
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

            val yLabels = listOf(100f, 50f, 0f)

            val maxYLabelWidth = yLabels
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

            val minX = 1f
            val maxX = analytics.normalizedLength.toFloat()
            val xRange = (maxX - minX).takeIf { it > 0f } ?: 1f

            fun mapX(day: Float): Float {
                return leftPadding + ((day - minX) / xRange) * chartWidth
            }

            fun mapY(value: Float): Float {
                return topPadding + (1f - (value.coerceIn(0f, 100f) / 100f)) * chartHeight
            }

            val axisColor = Color.Gray.copy(alpha = 0.55f)
            val gridColor = Color.LightGray.copy(alpha = 0.30f)

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



            yLabels.forEach { value ->
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

            val xLabelDays = buildList {
                var day = 7

                while (day <= analytics.normalizedLength) {
                    add(day)
                    day += 7
                }

                if (analytics.normalizedLength !in this) {
                    add(analytics.normalizedLength)
                }
            }.sorted()

            xLabelDays.forEach { day ->
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

            series.forEachIndexed { index, metricSeries ->
                val color = colors[index % colors.size]

                val offsets = metricSeries.points
                    .mapNotNull { point ->
                        val value = point.normalizedValue ?: return@mapNotNull null

                        Offset(
                            x = mapX(point.day.toFloat()),
                            y = mapY(value)
                        )
                    }

                if (offsets.isEmpty()) {
                    return@forEachIndexed
                }

                if (offsets.size == 1) {
                    drawCircle(
                        color = color,
                        radius = 4.5f,
                        center = offsets.first()
                    )

                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = 2.2f,
                        center = offsets.first()
                    )

                    return@forEachIndexed
                }

                val linePath = buildSmoothRelationshipChartPath(offsets)

                val fillPath = buildSmoothRelationshipChartFillPath(
                    points = offsets,
                    baselineY = baselineY
                )

                drawPath(
                    path = fillPath,
                    color = color.copy(alpha = 0.08f)
                )

                drawPath(
                    path = linePath,
                    color = color,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.5f)
                )

                offsets.forEach { point ->
                    drawCircle(
                        color = color,
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


@Composable
private fun RelationshipLegend(
    series: List<RelationshipMetricSeries>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        series.forEachIndexed { index, item ->
            val color = colors[index % colors.size]

            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(
                    modifier = Modifier.size(10.dp)
                ) {
                    drawCircle(
                        color = color,
                        radius = size.minDimension / 2f
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = buildLegendText(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = CycleSenseTextSecondary
                )
            }
        }
    }
}



private fun buildLegendText(series: RelationshipMetricSeries): String {
    val hasData = series.points.any { it.normalizedValue != null }

    return if (hasData) {
        series.metric.title
    } else {
        "${series.metric.title}: нет данных"
    }
}





private fun buildSmoothRelationshipChartPath(
    points: List<Offset>
): Path {
    val path = Path()

    if (points.isEmpty()) return path

    path.moveTo(points.first().x, points.first().y)

    appendSmoothRelationshipSegments(
        path = path,
        points = points
    )

    return path
}


private fun buildSmoothRelationshipChartFillPath(
    points: List<Offset>,
    baselineY: Float
): Path {
    val path = Path()

    if (points.isEmpty()) return path

    path.moveTo(points.first().x, baselineY)
    path.lineTo(points.first().x, points.first().y)

    appendSmoothRelationshipSegments(
        path = path,
        points = points
    )

    path.lineTo(points.last().x, baselineY)
    path.close()

    return path
}


private fun appendSmoothRelationshipSegments(
    path: Path,
    points: List<Offset>
) {
    if (points.size < 2) return

    if (points.size == 2) {
        path.lineTo(points.last().x, points.last().y)
        return
    }

    val slopes = buildMonotoneRelationshipSlopes(points)
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


private fun buildMonotoneRelationshipSlopes(
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
            previousSlope.signForRelationship() != nextSlope.signForRelationship()
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


private fun Float.signForRelationship(): Int {
    return when {
        this > 0f -> 1
        this < 0f -> -1
        else -> 0
    }
}