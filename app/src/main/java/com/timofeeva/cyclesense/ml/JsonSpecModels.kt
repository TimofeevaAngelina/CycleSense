package com.timofeeva.cyclesense.ml

data class InputFeatureSpec(
    val modelName: String? = null,
    val modelType: String? = null,
    val seqCycles: Int? = null,
    val nInputFeatures: Int? = null,
    val nOutputTargets: Int? = null,
    val inputFeatures: List<String> = emptyList(),
    val inputTensorShape: List<Int?> = emptyList(),
    val outputTensorShape: List<Int?> = emptyList(),
    val forecastDays: Int? = null,
    val requiresSelectTfOps: Boolean? = null,
    val target: String? = null,
    val normCycleLen: Int? = null,
    val useTargetHistoryAsInput: Boolean? = null,
    val supportsCrossCycleForecast: Boolean? = null
)

data class FeatureScalerItem(
    val name: String,
    val mean: Float,
    val scale: Float,
    val medianFillValue: Float? = null
)

data class FeatureScalerSpec(
    val modelName: String? = null,
    val target: String? = null,
    val features: List<FeatureScalerItem> = emptyList()
)

data class TargetScalerSpec(
    val modelName: String? = null,
    val target: String? = null,
    val mean: Float? = null,
    val scale: Float? = null
)