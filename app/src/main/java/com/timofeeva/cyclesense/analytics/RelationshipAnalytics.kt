package com.timofeeva.cyclesense.analytics

enum class RelationshipMetric(
    val id: String,
    val title: String,
    val unit: String
) {
    STRESS(
        id = "stress",
        title = "Напряжение",
        unit = "баллы"
    ),
    ENERGY(
        id = "energy",
        title = "Энергия",
        unit = "баллы"
    ),
    MOOD(
        id = "mood",
        title = "Настроение",
        unit = "баллы"
    ),
    CONCENTRATION(
        id = "concentration",
        title = "Концентрация",
        unit = "баллы"
    ),
    PAIN(
        id = "pain",
        title = "Дискомфорт",
        unit = "баллы"
    ),
    STEPS(
        id = "steps",
        title = "Шаги",
        unit = "шаги"
    ),
    CALORIES(
        id = "calories",
        title = "Калории",
        unit = "ккал"
    ),
    SLEEP(
        id = "sleep",
        title = "Сон",
        unit = "ч"
    ),
    HEART_RATE(
        id = "heart_rate",
        title = "Пульс",
        unit = "уд/мин"
    )
}

data class RelationshipDayPoint(
    val day: Int,
    val rawValue: Float?,
    val normalizedValue: Float?,
    val observationsCount: Int
)

data class RelationshipMetricSeries(
    val metric: RelationshipMetric,
    val points: List<RelationshipDayPoint>,
    val minRawValue: Float?,
    val maxRawValue: Float?
)

data class RelationshipAnalyticsResult(
    val selectedMetrics: List<RelationshipMetric>,
    val series: List<RelationshipMetricSeries>,
    val sourceCyclesUsed: Int,
    val completedCyclesUsed: Int,
    val normalizedLength: Int
)