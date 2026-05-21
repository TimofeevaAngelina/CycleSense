package com.timofeeva.cyclesense.ml

object FeatureBuilders {

    fun buildDemoNext3DaysInput(
        featureOrder: List<String>,
        seqCycles: Int = 3,
        maxCycleLen: Int = 51
    ): Array<Array<FloatArray>> {
        val totalSteps = seqCycles * maxCycleLen
        val nFeatures = featureOrder.size

        val result = Array(1) { Array(totalSteps) { FloatArray(nFeatures) } }

        for (cycleIdx in 0 until seqCycles) {
            for (dayIdx in 0 until maxCycleLen) {
                val rowIndex = cycleIdx * maxCycleLen + dayIdx

                val rawMap = mutableMapOf<String, Float?>()

                val dayInCycle = dayIdx + 1
                val normDay = dayInCycle.toFloat() / maxCycleLen.toFloat()

                val phase = when {
                    dayInCycle <= 5 -> "menstrual"
                    dayInCycle <= 13 -> "follicular"
                    dayInCycle <= 16 -> "ovulatory"
                    else -> "luteal"
                }

                rawMap["steps"] = (6500f + cycleIdx * 400f + dayIdx * 35f)
                rawMap["heart_rate"] = (68f + (dayIdx % 7) * 0.8f)
                rawMap["rmssd"] = (42f - (dayIdx % 5) * 0.7f)
                rawMap["sleep_hours"] = (7.2f - (dayIdx % 4) * 0.15f)

                rawMap["stress_score_cycle"] =
                    when (phase) {
                        "menstrual" -> 6.1f
                        "follicular" -> 5.3f
                        "ovulatory" -> 5.4f
                        else -> 6.0f
                    }

                rawMap["energy_level"] =
                    when (phase) {
                        "menstrual" -> 5.8f
                        "follicular" -> 6.2f
                        "ovulatory" -> 6.4f
                        else -> 5.3f
                    }

                rawMap["mood_score"] =
                    when (phase) {
                        "menstrual" -> 6.2f
                        "follicular" -> 6.5f
                        "ovulatory" -> 6.7f
                        else -> 6.0f
                    }

                rawMap["concentration_score"] =
                    when (phase) {
                        "menstrual" -> 6.8f
                        "follicular" -> 7.1f
                        "ovulatory" -> 7.2f
                        else -> 6.5f
                    }

                rawMap["pain_level"] =
                    when (phase) {
                        "menstrual" -> 5.6f
                        "follicular" -> 4.5f
                        "ovulatory" -> 4.6f
                        else -> 5.2f
                    }

                rawMap["day_in_cycle_norm"] = normDay
                rawMap["phase_menstrual"] = if (phase == "menstrual") 1f else 0f
                rawMap["phase_follicular"] = if (phase == "follicular") 1f else 0f
                rawMap["phase_ovulatory"] = if (phase == "ovulatory") 1f else 0f
                rawMap["phase_luteal"] = if (phase == "luteal") 1f else 0f

                for (featureIdx in featureOrder.indices) {
                    result[0][rowIndex][featureIdx] = rawMap[featureOrder[featureIdx]] ?: 0f
                }
            }
        }

        return result
    }

    fun applyFeatureScalingInPlace(
        input: Array<Array<FloatArray>>,
        scalerSpec: FeatureScalerSpec
    ) {
        val features = scalerSpec.features

        for (t in input[0].indices) {
            for (f in features.indices) {
                val raw = input[0][t][f]
                input[0][t][f] = ScalerUtils.fillAndScaleFeature(raw, features[f])
            }
        }
    }
}