package com.timofeeva.cyclesense.analytics

data class HeartRateTrendPoint(
    val dayInCycle: Int,
    val avgHeartRate: Float,
    val minHeartRate: Float?,
    val maxHeartRate: Float?,
    val samplesCount: Int
)

data class HeartRatePhasePoint(
    val phaseTitle: String,
    val avgMinHeartRate: Float?,
    val avgMaxHeartRate: Float?,
    val observationsCount: Int
)

data class HeartRateAnalyticsResult(
    val cyclesUsed: Int,
    val points: List<HeartRateTrendPoint>,
    val phasePoints: List<HeartRatePhasePoint> = emptyList()
)