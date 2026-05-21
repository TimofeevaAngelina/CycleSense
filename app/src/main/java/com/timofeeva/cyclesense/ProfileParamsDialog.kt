package com.timofeeva.cyclesense

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale
import androidx.compose.ui.text.style.TextAlign

data class ProfileParamsFormData(
    val dateOfBirth: String?,
    val weightKg: Float?,
    val heightCm: Float?
)

@Composable
fun ProfileParamsDialog(
    initialDateOfBirth: String?,
    initialWeightKg: Float?,
    initialHeightCm: Float?,
    onDismiss: () -> Unit,
    onSave: (ProfileParamsFormData) -> Unit
) {
    var weightText by remember(initialWeightKg) {
        mutableStateOf(
            initialWeightKg?.let { formatWeightForInput(it) } ?: ""
        )
    }

    var heightText by remember(initialHeightCm) {
        mutableStateOf(
            initialHeightCm?.toInt()?.toString() ?: ""
        )
    }

    val weightRaw = weightText.trim().replace(',', '.')
    val heightRaw = heightText.trim()

    val weight = weightRaw.toFloatOrNull()
    val height = heightRaw.toIntOrNull()

    val weightError = when {
        weightRaw.isBlank() -> null
        weight == null -> "Введите вес числом, например 62.5"
        weight !in 10f..500f -> "Проверьте значение веса"
        else -> null
    }

    val heightError = when {
        heightRaw.isBlank() -> null
        height == null -> "Введите рост целым числом, например 168"
        height !in 40..300 -> "Проверьте значение роста"
        else -> null
    }

    val canSave = weightError == null && heightError == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Данные профиля")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { newValue ->
                        weightText = normalizeProfileDecimalInput(
                            input = newValue,
                            maxIntegerDigits = 3,
                            maxDecimalDigits = 1
                        )
                    },
                    label = { Text("Вес, кг") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = weightError != null
                )

                if (weightError != null) {
                    Text(
                        text = weightError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                OutlinedTextField(
                    value = heightText,
                    onValueChange = { newValue ->
                        heightText = newValue
                            .filter { it.isDigit() }
                            .take(3)
                    },
                    label = { Text("Рост, см") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    singleLine = true,
                    isError = heightError != null
                )

                if (heightError != null) {
                    Text(
                        text = heightError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }


            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    onSave(
                        ProfileParamsFormData(
                            dateOfBirth = initialDateOfBirth,
                            weightKg = weight,
                            heightCm = height?.toFloat()
                        )
                    )
                },
                shape = CycleSenseButtonShape
            ) {
                Text(
                    text = "Сохранить",
                    textAlign = TextAlign.Center
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = CycleSenseButtonShape
            ) {
                Text(
                    text = "Отмена",
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}

private fun formatWeightForInput(value: Float): String {
    return if (value % 1f == 0f) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}

private fun normalizeProfileDecimalInput(
    input: String,
    maxIntegerDigits: Int,
    maxDecimalDigits: Int
): String {
    val normalized = input.replace(',', '.')
    val filtered = normalized.filter { char ->
        char.isDigit() || char == '.'
    }

    val dotIndex = filtered.indexOf('.')

    return if (dotIndex == -1) {
        filtered.take(maxIntegerDigits)
    } else {
        val integerPart = filtered
            .substring(0, dotIndex)
            .filter { it.isDigit() }
            .take(maxIntegerDigits)

        val decimalPart = filtered
            .substring(dotIndex + 1)
            .filter { it.isDigit() }
            .take(maxDecimalDigits)

        if (maxDecimalDigits == 0) {
            integerPart
        } else if (decimalPart.isEmpty()) {
            "$integerPart."
        } else {
            "$integerPart.$decimalPart"
        }
    }
}