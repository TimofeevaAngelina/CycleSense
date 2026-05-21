package com.timofeeva.cyclesense.ml

data class PhaseInputSpec(
    val modelName: String? = null,
    val modelType: String? = null,
    val seqCycles: Int? = null,
    val nInputFeatures: Int? = null,
    val nOutputTargets: Int? = null,
    val inputFeatures: List<String> = emptyList(),
    val inputTensorShape: List<Int?> = emptyList(),
    val outputTensorShape: List<Int?> = emptyList(),
    val requiresSelectTfOps: Boolean? = null
)

data class PhaseTargetSpec(
    val modelName: String? = null,
    val targetColumns: List<String> = emptyList()
)

data class PhaseFeatureScalerItem(
    val name: String,
    val mean: Float,
    val scale: Float,
    val medianFillValue: Float? = null
)

data class PhaseFeatureScalerSpec(
    val modelName: String? = null,
    val features: List<PhaseFeatureScalerItem> = emptyList()
)

data class PhaseTargetScalerItem(
    val name: String,
    val mean: Float,
    val scale: Float
)

data class PhaseTargetScalerSpec(
    val modelName: String? = null,
    val targets: List<PhaseTargetScalerItem> = emptyList()
)