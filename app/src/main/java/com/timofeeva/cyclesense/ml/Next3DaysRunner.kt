package com.timofeeva.cyclesense.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDate

data class Next3DaysPrediction(
    val targetName: String,
    val values: List<Float>
)

class Next3DaysRunner(
    context: Context,
    private val targetName: String
) {
    private val inputSpec = JsonLoader.loadInputSpec(
        context,
        "${targetName}_daily_gru_any_day_input_spec.json"
    )

    private val xScalerSpec = JsonLoader.loadFeatureScalerSpec(
        context,
        "${targetName}_daily_gru_any_day_x_scaler.json"
    )

    private val yScalerSpec = JsonLoader.loadTargetScalerSpec(
        context,
        "${targetName}_daily_gru_any_day_y_scaler.json"
    )

    private val interpreter: Interpreter by lazy {
        val options = Interpreter.Options().apply {
            setUseXNNPACK(false)
            setNumThreads(1)
        }

        Interpreter(
            AssetUtils.readAssetByteBuffer(
                context,
                "${targetName}_daily_gru_any_day.tflite"
            ),
            options
        )
    }

    fun predictFromSelectedDate(
        selectedDate: LocalDate,
        dayDataByDate: Map<LocalDate, CycleDayData>
    ): Next3DaysPrediction {
        val inputShape = interpreter.getInputTensor(0).shape()
        val outputShape = interpreter.getOutputTensor(0).shape()

        val seqLen = inputShape[1]
        val nFeatures = inputShape[2]
        val forecastDays = outputShape[1]

        val featureOrder = inputSpec.inputFeatures
        val seqCycles = inputSpec.seqCycles ?: 2
        val normCycleLen = inputSpec.normCycleLen ?: (seqLen / seqCycles)

        require(featureOrder.size == nFeatures) {
            "Количество признаков в spec (${featureOrder.size}) не совпадает с моделью ($nFeatures)"
        }

        val neutralRawValues = xScalerSpec.features.associate { feature ->
            feature.name to (feature.medianFillValue ?: 0f)
        }

        val inputArray = NormalizedCycleInputBuilder.buildInputForSelectedDate(
            featureOrder = featureOrder,
            dayDataByDate = dayDataByDate,
            selectedDate = selectedDate,
            seqCycles = seqCycles,
            normCycleLen = normCycleLen,
            neutralRawValues = neutralRawValues
        )

        applyFeatureScalingInPlace(inputArray, xScalerSpec)

        val inputBuffer = float3DArrayToByteBuffer(inputArray)
        val outputBuffer = ByteBuffer.allocateDirect(forecastDays * 4)
            .order(ByteOrder.nativeOrder())

        interpreter.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()

        val scaledOut = FloatArray(forecastDays)
        for (i in 0 until forecastDays) {
            scaledOut[i] = outputBuffer.float
        }

        val restored = FloatArray(forecastDays)
        for (i in scaledOut.indices) {
            restored[i] = ScalerUtils.inverseScaleSingleTarget(
                scaled = scaledOut[i],
                scaler = yScalerSpec
            ).coerceIn(1f, 10f)
        }

        return Next3DaysPrediction(
            targetName = targetName,
            values = restored.toList()
        )
    }

    private fun applyFeatureScalingInPlace(
        input: Array<Array<FloatArray>>,
        scalerSpec: FeatureScalerSpec
    ) {
        val features = scalerSpec.features
        for (t in input[0].indices) {
            for (f in features.indices) {
                input[0][t][f] = ScalerUtils.fillAndScaleFeature(
                    raw = input[0][t][f],
                    scaler = features[f]
                )
            }
        }
    }

    private fun float3DArrayToByteBuffer(input: Array<Array<FloatArray>>): ByteBuffer {
        val batch = input.size
        val seqLen = input[0].size
        val nFeatures = input[0][0].size

        val buffer = ByteBuffer
            .allocateDirect(batch * seqLen * nFeatures * 4)
            .order(ByteOrder.nativeOrder())

        for (b in 0 until batch) {
            for (t in 0 until seqLen) {
                for (f in 0 until nFeatures) {
                    buffer.putFloat(input[b][t][f])
                }
            }
        }

        buffer.rewind()
        return buffer
    }

    fun close() {
        interpreter.close()
    }
}