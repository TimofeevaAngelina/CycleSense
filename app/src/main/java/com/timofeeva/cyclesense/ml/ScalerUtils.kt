package com.timofeeva.cyclesense.ml

object ScalerUtils {

    fun scaleValue(
        raw: Float,
        mean: Float,
        scale: Float
    ): Float {
        val safeScale = if (scale == 0f) 1f else scale
        return (raw - mean) / safeScale
    }

    fun inverseScaleValue(
        scaled: Float,
        mean: Float,
        scale: Float
    ): Float {
        return scaled * scale + mean
    }

    fun fillAndScaleFeature(
        raw: Float?,
        scaler: FeatureScalerItem
    ): Float {
        val filled = raw ?: scaler.medianFillValue ?: scaler.mean
        return scaleValue(
            raw = filled,
            mean = scaler.mean,
            scale = scaler.scale
        )
    }

    fun inverseScaleSingleTarget(
        scaled: Float,
        scaler: TargetScalerSpec
    ): Float {
        val mean = scaler.mean ?: 0f
        val scale = scaler.scale ?: 1f
        return inverseScaleValue(scaled, mean, scale)
    }
}