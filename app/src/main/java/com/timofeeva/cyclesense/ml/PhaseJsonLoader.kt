package com.timofeeva.cyclesense.ml

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object PhaseJsonLoader {

    fun loadInputSpec(
        context: Context,
        assetName: String = "phase_model/phase_input_spec_strict.json"
    ): PhaseInputSpec {
        val json = JSONObject(AssetUtils.readAssetText(context, assetName))

        return PhaseInputSpec(
            modelName = json.optString("model_name", null),
            modelType = json.optString("model_type", null),
            seqCycles = json.optIntOrNull("seq_cycles"),
            nInputFeatures = json.optIntOrNull("n_input_features"),
            nOutputTargets = json.optIntOrNull("n_output_targets"),
            inputFeatures = json.optStringList("input_features"),
            inputTensorShape = json.optNullableIntList("input_tensor_shape"),
            outputTensorShape = json.optNullableIntList("output_tensor_shape"),
            requiresSelectTfOps = if (json.has("requires_select_tf_ops")) {
                json.optBoolean("requires_select_tf_ops")
            } else {
                null
            }
        )
    }

    fun loadTargetSpec(
        context: Context,
        assetName: String = "phase_model/phase_target_spec_strict.json"
    ): PhaseTargetSpec {
        val json = JSONObject(AssetUtils.readAssetText(context, assetName))

        return PhaseTargetSpec(
            modelName = json.optString("model_name", null),
            targetColumns = json.optStringList("target_columns")
        )
    }

    fun loadFeatureScalerSpec(
        context: Context,
        assetName: String = "phase_model/phase_x_scaler_strict.json"
    ): PhaseFeatureScalerSpec {
        val json = JSONObject(AssetUtils.readAssetText(context, assetName))
        val arr = json.optJSONArray("features") ?: JSONArray()

        val items = buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                add(
                    PhaseFeatureScalerItem(
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

        return PhaseFeatureScalerSpec(
            modelName = json.optString("model_name", null),
            features = items
        )
    }

    fun loadTargetScalerSpec(
        context: Context,
        assetName: String = "phase_model/phase_y_scaler_strict.json"
    ): PhaseTargetScalerSpec {
        val json = JSONObject(AssetUtils.readAssetText(context, assetName))

        val targets = if (json.has("targets")) {
            val arr = json.getJSONArray("targets")
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        PhaseTargetScalerItem(
                            name = obj.getString("name"),
                            mean = obj.getDouble("mean").toFloat(),
                            scale = obj.getDouble("scale").toFloat()
                        )
                    )
                }
            }
        } else {
            emptyList()
        }

        return PhaseTargetScalerSpec(
            modelName = json.optString("model_name", null),
            targets = targets
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