package com.timofeeva.cyclesense.ml

data class PhaseMetricPrediction(
    val stress: Float,
    val energy: Float,
    val mood: Float,
    val concentration: Float,
    val pain: Float
)

data class NextCyclePhasePrediction(
    val menstrual: PhaseMetricPrediction,
    val follicular: PhaseMetricPrediction,
    val ovulatory: PhaseMetricPrediction,
    val luteal: PhaseMetricPrediction
)