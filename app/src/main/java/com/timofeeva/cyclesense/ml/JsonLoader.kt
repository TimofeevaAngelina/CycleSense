package com.timofeeva.cyclesense.ml

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object JsonLoader {

    fun loadInputSpec(context: Context, assetName: String): InputFeatureSpec {
        val json = JSONObject(AssetUtils.readAssetText(context, assetName))

        return InputFeatureSpec(
            modelName = json.optString("model_name", null),
            modelType = json.optString("model_type", null),
            seqCycles = json.optIntOrNull("seq_cycles"),
            nInputFeatures = json.optIntOrNull("n_input_features"),
            nOutputTargets = json.optIntOrNull("n_output_targets"),
            inputFeatures = json.optStringList("input_features"),
            inputTensorShape = json.optNullableIntList("input_tensor_shape"),
            outputTensorShape = json.optNullableIntList("output_tensor_shape"),
            forecastDays = json.optIntOrNull("forecast_days"),
            requiresSelectTfOps = if (json.has("requires_select_tf_ops")) json.optBoolean("requires_select_tf_ops") else null,
            target = json.optString("target", null),
            normCycleLen = json.optIntOrNull("norm_cycle_len"),
            useTargetHistoryAsInput = if (json.has("use_target_history_as_input")) json.optBoolean("use_target_history_as_input") else null,
            supportsCrossCycleForecast = if (json.has("supports_cross_cycle_forecast")) json.optBoolean("supports_cross_cycle_forecast") else null
        )
    }

    fun loadFeatureScalerSpec(context: Context, assetName: String): FeatureScalerSpec {
        val json = JSONObject(AssetUtils.readAssetText(context, assetName))
        val arr = json.optJSONArray("features") ?: JSONArray()

        val items = buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                add(
                    FeatureScalerItem(
                        name = obj.getString("name"),
                        mean = obj.getDouble("mean").toFloat(),
                        scale = obj.getDouble("scale").toFloat(),
                        medianFillValue = if (obj.has("median_fill_value")) {
                            obj.getDouble("median_fill_value").toFloat()
                        } else {
                            null
                        }
                    )
                )
            }
        }

        return FeatureScalerSpec(
            modelName = json.optString("model_name", null),
            target = json.optString("target", null),
            features = items
        )
    }

    fun loadTargetScalerSpec(context: Context, assetName: String): TargetScalerSpec {
        val json = JSONObject(AssetUtils.readAssetText(context, assetName))
        return TargetScalerSpec(
            modelName = json.optString("model_name", null),
            target = json.optString("target", null),
            mean = if (json.has("mean")) json.getDouble("mean").toFloat() else null,
            scale = if (json.has("scale")) json.getDouble("scale").toFloat() else null
        )
    }

    private fun JSONObject.optStringList(key: String): List<String> {
        val arr = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                add(arr.getString(i))
            }
        }
    }

    private fun JSONObject.optNullableIntList(key: String): List<Int?> {
        val arr = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                if (arr.isNull(i)) add(null) else add(arr.getInt(i))
            }
        }
    }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        return if (has(key) && !isNull(key)) getInt(key) else null
    }
}