package com.timofeeva.cyclesense.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PhaseRunner(
    context: Context
) {
    private val inputSpec = PhaseJsonLoader.loadInputSpec(context)
    private val targetSpec = PhaseJsonLoader.loadTargetSpec(context)
    private val xScalerSpec = PhaseJsonLoader.loadFeatureScalerSpec(context)
    private val yScalerSpec = PhaseJsonLoader.loadTargetScalerSpec(context)

    private val interpreter: Interpreter by lazy {
        val options = Interpreter.Options().apply {
            setUseXNNPACK(false)
            setNumThreads(1)
        }

        Interpreter(
            AssetUtils.readAssetByteBuffer(
                context,
                "phase_model/phase_model_strict_select_ops.tflite"
            ),
            options
        )
    }

    fun getInputFeatureOrder(): List<String> = inputSpec.inputFeatures

    fun getTargetColumns(): List<String> = targetSpec.targetColumns

    fun getExpectedSeqCycles(): Int = inputSpec.seqCycles ?: 3

    fun getExpectedFeatureCount(): Int = inputSpec.nInputFeatures ?: inputSpec.inputFeatures.size

    fun getExpectedOutputCount(): Int = inputSpec.nOutputTargets ?: targetSpec.targetColumns.size

    fun predictFromInput(inputArray: Array<Array<FloatArray>>): NextCyclePhasePrediction {
        val inputShape = interpreter.getInputTensor(0).shape()
        val outputShape = interpreter.getOutputTensor(0).shape()

        val batch = inputShape[0]
        val seqCycles = inputShape[1]
        val nFeatures = inputShape[2]

        require(batch == 1) {
            "Ожидался batch=1, но модель ожидает batch=$batch"
        }

        require(inputArray.size == 1) {
            "inputArray должен иметь batch=1"
        }

        require(inputArray[0].size == seqCycles) {
            "Ожидалось $seqCycles циклов, но получено ${inputArray[0].size}"
        }

        require(inputArray[0][0].size == nFeatures) {
            "Ожидалось $nFeatures признаков, но получено ${inputArray[0][0].size}"
        }

        val scaledInput = deepCopy3D(inputArray)
        applyFeatureScalingInPlace(scaledInput, xScalerSpec)

        val inputBuffer = float3DArrayToByteBuffer(scaledInput)

        val outputElementCount = outputShape.reduce { acc, value -> acc * value }
        val outputBuffer = ByteBuffer.allocateDirect(outputElementCount * 4)
            .order(ByteOrder.nativeOrder())

        interpreter.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()

        val scaledOutput = FloatArray(outputElementCount)
        for (i in 0 until outputElementCount) {
            scaledOutput[i] = outputBuffer.float
        }

        val restoredOutput = inverseScaleTargets(scaledOutput, yScalerSpec)

        return mapOutputToPhasePrediction(
            targetColumns = targetSpec.targetColumns,
            values = restoredOutput
        )
    }

    private fun applyFeatureScalingInPlace(
        input: Array<Array<FloatArray>>,
        scalerSpec: PhaseFeatureScalerSpec
    ) {
        val features = scalerSpec.features

        for (cycleIdx in input[0].indices) {
            for (featureIdx in features.indices) {
                input[0][cycleIdx][featureIdx] = ScalerUtils.fillAndScaleFeature(
                    raw = input[0][cycleIdx][featureIdx],
                    scaler = FeatureScalerItem(
                        name = features[featureIdx].name,
                        mean = features[featureIdx].mean,
                        scale = features[featureIdx].scale,
                        medianFillValue = features[featureIdx].medianFillValue
                    )
                )
            }
        }
    }

    private fun inverseScaleTargets(
        scaledValues: FloatArray,
        scaler: PhaseTargetScalerSpec
    ): FloatArray {
        if (scaler.targets.isEmpty()) return scaledValues

        val restored = FloatArray(scaledValues.size)
        for (i in scaledValues.indices) {
            val item = scaler.targets[i]
            restored[i] = ScalerUtils.inverseScaleValue(
                scaled = scaledValues[i],
                mean = item.mean,
                scale = item.scale
            ).coerceIn(1f, 10f)
        }
        return restored
    }

    private fun mapOutputToPhasePrediction(
        targetColumns: List<String>,
        values: FloatArray
    ): NextCyclePhasePrediction {
        fun metric(phase: String, metricName: String): Float {
            val column = "${metricName}_${phase}_mean_next"
            val idx = targetColumns.indexOf(column)
            return if (idx in values.indices) values[idx] else 0f
        }

        val menstrual = PhaseMetricPrediction(
            stress = metric("menstrual", "stress_score_cycle"),
            energy = metric("menstrual", "energy_level"),
            mood = metric("menstrual", "mood_score"),
            concentration = metric("menstrual", "concentration_score"),
            pain = metric("menstrual", "pain_level")
        )

        val follicular = PhaseMetricPrediction(
            stress = metric("follicular", "stress_score_cycle"),
            energy = metric("follicular", "energy_level"),
            mood = metric("follicular", "mood_score"),
            concentration = metric("follicular", "concentration_score"),
            pain = metric("follicular", "pain_level")
        )

        val ovulatory = PhaseMetricPrediction(
            stress = metric("ovulatory", "stress_score_cycle"),
            energy = metric("ovulatory", "energy_level"),
            mood = metric("ovulatory", "mood_score"),
            concentration = metric("ovulatory", "concentration_score"),
            pain = metric("ovulatory", "pain_level")
        )

        val luteal = PhaseMetricPrediction(
            stress = metric("luteal", "stress_score_cycle"),
            energy = metric("luteal", "energy_level"),
            mood = metric("luteal", "mood_score"),
            concentration = metric("luteal", "concentration_score"),
            pain = metric("luteal", "pain_level")
        )

        return NextCyclePhasePrediction(
            menstrual = menstrual,
            follicular = follicular,
            ovulatory = ovulatory,
            luteal = luteal
        )
    }

    private fun float3DArrayToByteBuffer(input: Array<Array<FloatArray>>): ByteBuffer {
        val batch = input.size
        val seqCycles = input[0].size
        val nFeatures = input[0][0].size

        val buffer = ByteBuffer
            .allocateDirect(batch * seqCycles * nFeatures * 4)
            .order(ByteOrder.nativeOrder())

        for (b in 0 until batch) {
            for (c in 0 until seqCycles) {
                for (f in 0 until nFeatures) {
                    buffer.putFloat(input[b][c][f])
                }
            }
        }

        buffer.rewind()
        return buffer
    }

    private fun deepCopy3D(input: Array<Array<FloatArray>>): Array<Array<FloatArray>> {
        return Array(input.size) { b ->
            Array(input[b].size) { c ->
                input[b][c].clone()
            }
        }
    }

    fun close() {
        interpreter.close()
    }
}