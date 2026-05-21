package com.timofeeva.cyclesense.analytics

import kotlin.compareTo
import kotlin.math.abs
import kotlin.math.sqrt

data class AnalyticsInsight(
    val title: String,
    val description: String
)

class AnalyticsInterpretationService {

    fun buildPsychophysiologyInsights(
        phaseAnalytics: PhaseAnalyticsResult?,
        cycleTrendAnalytics: CycleTrendAnalyticsResult?
    ): List<AnalyticsInsight> {
        if (phaseAnalytics == null && cycleTrendAnalytics == null) {
            return emptyList()
        }



        val hasPsychophysiologyData = cycleTrendAnalytics
            ?.points
            ?.any { it.observationsCount > 0 } == true

        if (!hasPsychophysiologyData) {
            return listOf(
                AnalyticsInsight(
                    title = "Недостаточно данных о самочувствии",
                    description = "Заполните показатели самочувствия хотя бы за несколько дней цикла. После этого приложение сможет построить осмысленную психофизиологическую аналитику."
                )
            )
        }

        val insights = mutableListOf<AnalyticsInsight>()

        phaseAnalytics?.let { analytics ->
            val phases = listOf(
                PhaseSummary("менструальную фазу", "Менструальная", analytics.menstrual),
                PhaseSummary("фолликулярную фазу", "Фолликулярная", analytics.follicular),
                PhaseSummary("овуляторную фазу", "Овуляторная", analytics.ovulatory),
                PhaseSummary("лютеиновую фазу", "Лютеиновая", analytics.luteal)
            )

            addPhaseExtremumInsight(
                insights = insights,
                phases = phases,
                metricTitle = "стресс",
                metricGetter = { it.stress },
                highIsWorse = true,
                threshold = 0.5f,
                title = "Стресс зависит от фазы цикла",
                highText = "выше",
                lowText = "ниже"
            )

            addPhaseExtremumInsight(
                insights = insights,
                phases = phases,
                metricTitle = "боль",
                metricGetter = { it.pain },
                highIsWorse = true,
                threshold = 0.5f,
                title = "Боль выражена неравномерно",
                highText = "выше",
                lowText = "ниже"
            )

            addPhaseExtremumInsight(
                insights = insights,
                phases = phases,
                metricTitle = "энергия",
                metricGetter = { it.energy },
                highIsWorse = false,
                threshold = 0.5f,
                title = "Энергия меняется по фазам",
                highText = "выше",
                lowText = "ниже"
            )

            addPhaseExtremumInsight(
                insights = insights,
                phases = phases,
                metricTitle = "настроение",
                metricGetter = { it.mood },
                highIsWorse = false,
                threshold = 0.5f,
                title = "Настроение имеет фазовые различия",
                highText = "лучше",
                lowText = "ниже"
            )

            addPhaseExtremumInsight(
                insights = insights,
                phases = phases,
                metricTitle = "концентрация",
                metricGetter = { it.concentration },
                highIsWorse = false,
                threshold = 0.5f,
                title = "Концентрация отличается по фазам",
                highText = "выше",
                lowText = "ниже"
            )

            val luteal = phases.firstOrNull { it.shortTitle == "Лютеиновая" }
            val follicular = phases.firstOrNull { it.shortTitle == "Фолликулярная" }

            if (luteal != null && follicular != null) {
                val lutealStressHigher = luteal.metrics.stress - follicular.metrics.stress >= 0.5f
                val lutealEnergyLower = follicular.metrics.energy - luteal.metrics.energy >= 0.5f
                val lutealMoodLower = follicular.metrics.mood - luteal.metrics.mood >= 0.5f

                if (lutealStressHigher && (lutealEnergyLower || lutealMoodLower)) {
                    insights.add(
                        AnalyticsInsight(
                            title = "Лютеиновая фаза выглядит более напряженной",
                            description = "В лютеиновую фазу стресс выше, а энергия или настроение ниже, чем в фолликулярную. Это может быть период, когда стоит заранее планировать более щадящий режим."
                        )
                    )
                }
            }
        }

        cycleTrendAnalytics?.let { trend ->
            val stressPeak = trend.points.maxByOrNull { it.stress }
            val painPeak = trend.points.maxByOrNull { it.pain }
            val energyLow = trend.points.minByOrNull { it.energy }

            if (stressPeak != null && stressPeak.stress >= 6.5f) {
                insights.add(
                    AnalyticsInsight(
                        title = "Есть дни с повышенным стрессом",
                        description = "На графике стресс достигает максимума примерно на ${stressPeak.day}-й день цикла."
                    )
                )
            }

            if (painPeak != null && painPeak.pain >= 6.0f) {
                insights.add(
                    AnalyticsInsight(
                        title = "Есть выраженный пик дискомфорта",
                        description = "Самый высокий уровень дискомфорта приходится примерно на ${painPeak.day}-й день цикла."
                    )
                )
            }

            if (energyLow != null && energyLow.energy <= 5.0f) {
                insights.add(
                    AnalyticsInsight(
                        title = "Есть период снижения энергии",
                        description = "Минимальная энергия наблюдается примерно на ${energyLow.day}-й день."
                    )
                )
            }
        }

        if (insights.isEmpty()) {
            insights.add(
                AnalyticsInsight(
                    title = "Выраженных изменений пока не видно",
                    description = "По текущей истории субъективные показатели между фазами и днями цикла отличаются умеренно. По мере накопления данных выводы станут точнее."
                )
            )
        }

        return prioritizeInsights(insights).take(5)
    }

    fun buildPhysiologyInsights(
        activitySleepAnalytics: ActivitySleepAnalyticsResult?,
        heartRateAnalytics: HeartRateAnalyticsResult?
    ): List<AnalyticsInsight> {
        if (activitySleepAnalytics == null && heartRateAnalytics == null) {
            return emptyList()
        }

        val hasActivityOrSleepData = activitySleepAnalytics?.let { analytics ->
            analytics.phasePoints.any { point ->
                point.avgSteps != null ||
                        point.avgCalories != null ||
                        point.avgSleepHours != null
            } || analytics.dayPoints.any { point ->
                point.observationsCount > 0
            }
        } == true

        val hasHeartRateData = heartRateAnalytics?.points?.any { point ->
            point.samplesCount > 0 ||
                    point.minHeartRate != null ||
                    point.maxHeartRate != null ||
                    point.avgHeartRate > 0f
        } == true

        if (!hasActivityOrSleepData && !hasHeartRateData) {
            return listOf(
                AnalyticsInsight(
                    title = "Недостаточно физиологических данных",
                    description = "Пока нет данных сна, активности или пульса для интерпретации. Подключите Health Connect или дождитесь накопления данных за несколько дней."
                )
            )
        }

        val insights = mutableListOf<AnalyticsInsight>()

        activitySleepAnalytics?.let { analytics ->
            val sleepMin = analytics.phasePoints
                .filter { it.avgSleepHours != null }
                .minByOrNull { it.avgSleepHours ?: Float.MAX_VALUE }

            val sleepMax = analytics.phasePoints
                .filter { it.avgSleepHours != null }
                .maxByOrNull { it.avgSleepHours ?: Float.MIN_VALUE }

            if (
                sleepMin?.avgSleepHours != null &&
                sleepMax?.avgSleepHours != null &&
                sleepMax.avgSleepHours - sleepMin.avgSleepHours >= 0.3f
            ) {
                insights.add(
                    AnalyticsInsight(
                        title = "Сон отличается по фазам",
                        description = "Меньше всего сна в среднем приходится на фазу «${sleepMin.phaseTitle}», больше всего — на фазу «${sleepMax.phaseTitle}»."
                    )
                )
            }

            val stepsMin = analytics.phasePoints
                .filter { it.avgSteps != null }
                .minByOrNull { it.avgSteps ?: Float.MAX_VALUE }

            val stepsMax = analytics.phasePoints
                .filter { it.avgSteps != null }
                .maxByOrNull { it.avgSteps ?: Float.MIN_VALUE }

            if (
                stepsMin?.avgSteps != null &&
                stepsMax?.avgSteps != null &&
                stepsMax.avgSteps - stepsMin.avgSteps >= 800f
            ) {
                insights.add(
                    AnalyticsInsight(
                        title = "Активность меняется по фазам",
                        description = "В фазу «${stepsMin.phaseTitle}» шагов меньше, чем в фазу «${stepsMax.phaseTitle}». Это может отражать снижение активности в отдельные периоды цикла."
                    )
                )
            }

            val caloriesMin = analytics.phasePoints
                .filter { it.avgCalories != null }
                .minByOrNull { it.avgCalories ?: Float.MAX_VALUE }

            val caloriesMax = analytics.phasePoints
                .filter { it.avgCalories != null }
                .maxByOrNull { it.avgCalories ?: Float.MIN_VALUE }

            if (
                caloriesMin?.avgCalories != null &&
                caloriesMax?.avgCalories != null &&
                caloriesMax.avgCalories - caloriesMin.avgCalories >= 120f
            ) {
                insights.add(
                    AnalyticsInsight(
                        title = "Расход калорий неравномерен",
                        description = "Средний расход калорий выше в фазу «${caloriesMax.phaseTitle}» и ниже в фазу «${caloriesMin.phaseTitle}»."
                    )
                )
            }

            val sleepLowDays = analytics.dayPoints
                .filter { it.avgSleepHours != null }
                .sortedBy { it.avgSleepHours }
                .take(2)

            if (sleepLowDays.isNotEmpty()) {
                val daysText = sleepLowDays.joinToString { "${it.day}-й" }
                insights.add(
                    AnalyticsInsight(
                        title = "Есть дни с меньшей продолжительностью сна",
                        description = "Наиболее короткий сон заметен примерно в дни: $daysText."
                    )
                )
            }
        }

        heartRateAnalytics?.let { analytics ->
            val avgPeak = analytics.points
                .filter { it.avgHeartRate > 0f }
                .maxByOrNull { it.avgHeartRate }

            val avgLow = analytics.points
                .filter { it.avgHeartRate > 0f }
                .minByOrNull { it.avgHeartRate }

            if (
                avgPeak != null &&
                avgLow != null &&
                avgPeak.avgHeartRate - avgLow.avgHeartRate >= 3f
            ) {
                insights.add(
                    AnalyticsInsight(
                        title = "Пульс меняется в течение цикла",
                        description = "Средний пульс выше примерно на ${avgPeak.dayInCycle}-й день и ниже примерно на ${avgLow.dayInCycle}-й день цикла."
                    )
                )
            }

            val widestRangeDay = analytics.points
                .filter { it.minHeartRate != null && it.maxHeartRate != null }
                .maxByOrNull { (it.maxHeartRate ?: 0f) - (it.minHeartRate ?: 0f) }

            if (
                widestRangeDay?.minHeartRate != null &&
                widestRangeDay.maxHeartRate != null &&
                widestRangeDay.maxHeartRate - widestRangeDay.minHeartRate >= 20f
            ) {
                insights.add(
                    AnalyticsInsight(
                        title = "Есть дни с широким диапазоном пульса",
                        description = "На ${widestRangeDay.dayInCycle}-й день цикла диапазон пульса особенно широкий. Это может отражать более выраженную нагрузку или неоднородность активности в течение дня."
                    )
                )
            }
        }

        if (insights.isEmpty()) {
            insights.add(
                AnalyticsInsight(
                    title = "Выраженных изменений пока не видно",
                    description = "По доступным физиологическим данным сон, активность и пульс меняются умеренно. По мере накопления истории выводы станут точнее."
                )
            )
        }

        return prioritizeInsights(insights).take(5)
    }

    fun buildRelationshipInsights(
        relationshipAnalytics: RelationshipAnalyticsResult?
    ): List<AnalyticsInsight> {
        if (relationshipAnalytics == null || relationshipAnalytics.series.size < 2) {
            return emptyList()
        }

        val insights = mutableListOf<AnalyticsInsight>()
        val series = relationshipAnalytics.series

        for (i in series.indices) {
            for (j in i + 1 until series.size) {
                val first = series[i]
                val second = series[j]

                val correlation = correlationBetween(first, second) ?: continue

                if (abs(correlation) < 0.35f) {
                    continue
                }

                insights.add(
                    buildRelationshipInsight(
                        first = first.metric,
                        second = second.metric,
                        correlation = correlation
                    )
                )
            }
        }

        if (insights.isEmpty()) {
            insights.add(
                AnalyticsInsight(
                    title = "Явной связи пока не видно",
                    description = "Для выбранных показателей выраженная взаимосвязь не обнаружена. Можно попробовать другую комбинацию или дождаться накопления большего числа дней."
                )
            )
        }

        return insights.take(3)
    }

    private fun addPhaseExtremumInsight(
        insights: MutableList<AnalyticsInsight>,
        phases: List<PhaseSummary>,
        metricTitle: String,
        metricGetter: (PhaseMetricAnalytics) -> Float,
        highIsWorse: Boolean,
        threshold: Float,
        title: String,
        highText: String,
        lowText: String
    ) {
        val maxPhase = phases.maxByOrNull { metricGetter(it.metrics) }
        val minPhase = phases.minByOrNull { metricGetter(it.metrics) }

        if (maxPhase == null || minPhase == null) return

        val maxValue = metricGetter(maxPhase.metrics)
        val minValue = metricGetter(minPhase.metrics)
        val diff = maxValue - minValue

        if (diff < threshold) return

        val description = if (highIsWorse) {
            "В ${maxPhase.fullTitle} показатель «$metricTitle» $highText, а в ${minPhase.fullTitle} — $lowText. Это может указывать на период повышенного дискомфорта или нагрузки."
        } else {
            "В ${minPhase.fullTitle} показатель «$metricTitle» $lowText, а в ${maxPhase.fullTitle} — $highText. Это помогает выделить более ресурсные и менее ресурсные периоды цикла."
        }

        insights.add(
            AnalyticsInsight(
                title = title,
                description = description
            )
        )
    }

    private fun buildRelationshipInsight(
        first: RelationshipMetric,
        second: RelationshipMetric,
        correlation: Float
    ): AnalyticsInsight {
        val isPositive = correlation > 0f

        val title = if (isPositive) {
            "${first.title} и ${second.title} меняются в одном направлении"
        } else {
            "${first.title} и ${second.title} меняются в противоположных направлениях"
        }

        return AnalyticsInsight(
            title = title,
            description = buildNaturalRelationshipText(first, second, isPositive)
        )
    }

    private fun buildNaturalRelationshipText(
        first: RelationshipMetric,
        second: RelationshipMetric,
        isPositive: Boolean
    ): String {
        val pair = setOf(first, second)

        if (pair == setOf(RelationshipMetric.SLEEP, RelationshipMetric.STRESS)) {
            return if (isPositive) {
                "В выбранной истории сон и стресс повышаются одновременно. Это может быть особенностью текущих данных, поэтому связь стоит проверить на более длинной истории."
            } else {
                "В дни, когда сон ниже обычного, стресс чаще оказывается выше."
            }
        }

        if (pair == setOf(RelationshipMetric.SLEEP, RelationshipMetric.ENERGY)) {
            return if (isPositive) {
                "В дни с более длительным сном энергия обычно выше."
            } else {
                "В текущих данных более длительный сон не сопровождается ростом энергии. Возможно, влияют стресс, боль или активность."
            }
        }

        if (pair == setOf(RelationshipMetric.SLEEP, RelationshipMetric.MOOD)) {
            return if (isPositive) {
                "В дни с более длительным сном настроение обычно лучше."
            } else {
                "В выбранной истории связь сна и настроения выглядит нестандартно: при большем сне настроение не повышается."
            }
        }

        if (pair == setOf(RelationshipMetric.SLEEP, RelationshipMetric.CONCENTRATION)) {
            return if (isPositive) {
                "Более длительный сон связан с более высокой концентрацией."
            } else {
                "В выбранных данных концентрация не растёт вместе со сном. Возможно, влияют стресс, боль или активность."
            }
        }

        if (pair == setOf(RelationshipMetric.HEART_RATE, RelationshipMetric.STRESS)) {
            return if (isPositive) {
                "В дни с более высоким пульсом стресс обычно выше."
            } else {
                "В выбранных данных более высокий пульс не сопровождается ростом стресса."
            }
        }

        if (pair == setOf(RelationshipMetric.HEART_RATE, RelationshipMetric.PAIN)) {
            return if (isPositive) {
                "В дни с более высоким пульсом уровень боли обычно выше."
            } else {
                "В выбранных данных пульс и боль меняются в противоположных направлениях."
            }
        }

        if (pair == setOf(RelationshipMetric.STEPS, RelationshipMetric.ENERGY)) {
            return if (isPositive) {
                "В дни с большей активностью энергия обычно выше."
            } else {
                "В выбранных данных больше шагов не связано с более высокой энергией."
            }
        }

        if (pair == setOf(RelationshipMetric.STEPS, RelationshipMetric.MOOD)) {
            return if (isPositive) {
                "В дни с большей активностью настроение обычно лучше."
            } else {
                "В выбранных данных активность и настроение меняются в противоположных направлениях."
            }
        }

        if (pair == setOf(RelationshipMetric.STRESS, RelationshipMetric.MOOD)) {
            return if (isPositive) {
                "Стресс и настроение в выбранных данных растут одновременно. Это нестандартная связь, её стоит проверить на большем числе дней."
            } else {
                "В дни с более высоким стрессом настроение обычно ниже."
            }
        }

        if (pair == setOf(RelationshipMetric.STRESS, RelationshipMetric.ENERGY)) {
            return if (isPositive) {
                "Стресс и энергия в выбранных данных растут одновременно. Возможно, это отражает периоды высокой нагрузки и активности."
            } else {
                "В дни с более высоким стрессом энергия обычно ниже."
            }
        }

        if (pair == setOf(RelationshipMetric.PAIN, RelationshipMetric.MOOD)) {
            return if (isPositive) {
                "Боль и настроение в выбранных данных растут одновременно. Это нестандартная связь, её стоит проверить на большем числе дней."
            } else {
                "В дни с более выраженной болью настроение обычно ниже."
            }
        }

        return if (isPositive) {
            "Когда показатель «${first.title}» выше, показатель «${second.title}» тоже обычно выше."
        } else {
            "Когда показатель «${first.title}» выше, показатель «${second.title}» обычно ниже."
        }
    }

    private fun correlationBetween(
        first: RelationshipMetricSeries,
        second: RelationshipMetricSeries
    ): Float? {
        val firstByDay = first.points.associateBy { it.day }
        val secondByDay = second.points.associateBy { it.day }

        val paired = firstByDay.keys
            .intersect(secondByDay.keys)
            .mapNotNull { day ->
                val x = firstByDay[day]?.normalizedValue
                val y = secondByDay[day]?.normalizedValue

                if (x == null || y == null) {
                    null
                } else {
                    x to y
                }
            }

        if (paired.size < 4) return null

        val xs = paired.map { it.first }
        val ys = paired.map { it.second }

        val meanX = xs.average().toFloat()
        val meanY = ys.average().toFloat()

        var numerator = 0f
        var sumSqX = 0f
        var sumSqY = 0f

        for (index in paired.indices) {
            val dx = xs[index] - meanX
            val dy = ys[index] - meanY

            numerator += dx * dy
            sumSqX += dx * dx
            sumSqY += dy * dy
        }

        val denominator = sqrt(sumSqX * sumSqY)
        if (denominator == 0f) return null

        return numerator / denominator
    }

    private fun prioritizeInsights(insights: List<AnalyticsInsight>): List<AnalyticsInsight> {
        val priorityWords = listOf(
            "Лютеиновая",
            "Стресс",
            "Сон",
            "Пульс",
            "Боль",
            "Энергия"
        )

        return insights.sortedByDescending { insight ->
            priorityWords.count { word ->
                insight.title.contains(word, ignoreCase = true) ||
                        insight.description.contains(word, ignoreCase = true)
            }
        }
    }

    private data class PhaseSummary(
        val fullTitle: String,
        val shortTitle: String,
        val metrics: PhaseMetricAnalytics
    )
}