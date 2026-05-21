package com.timofeeva.cyclesense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import com.timofeeva.cyclesense.ui.theme.CycleSenseTextSecondary

data class EditDayFormData(
    val isMenstruationMarked: Boolean,
    val stressScore: Float?,
    val energyLevel: Float?,
    val moodScore: Float?,
    val concentrationScore: Float?,
    val painLevel: Float?,
    val notes: String?
)

private data class SubjectiveOption(
    val value: Float,
    val label: String
)

@Composable
fun EditDayDataDialog(
    initialIsMenstruationMarked: Boolean,
    initialStressScore: Float?,
    initialEnergyLevel: Float?,
    initialMoodScore: Float?,
    initialConcentrationScore: Float?,
    initialPainLevel: Float?,
    initialNotes: String?,
    onDismiss: () -> Unit,
    onSave: (EditDayFormData) -> Unit
) {
    var stressScore by remember {
        mutableStateOf(initialStressScore?.nearestSubjectiveScaleValue())
    }
    var energyLevel by remember {
        mutableStateOf(initialEnergyLevel?.nearestSubjectiveScaleValue())
    }
    var moodScore by remember {
        mutableStateOf(initialMoodScore?.nearestSubjectiveScaleValue())
    }
    var concentrationScore by remember {
        mutableStateOf(initialConcentrationScore?.nearestSubjectiveScaleValue())
    }
    var painLevel by remember {
        mutableStateOf(initialPainLevel?.nearestSubjectiveScaleValue())
    }



    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        EditDayFormData(
                            isMenstruationMarked = initialIsMenstruationMarked,
                            stressScore = stressScore,
                            energyLevel = energyLevel,
                            moodScore = moodScore,
                            concentrationScore = concentrationScore,
                            painLevel = painLevel,
                            notes = initialNotes
                        )
                    )
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
        title = {
            Text(
                text = "Редактировать самочувствие",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Выберите состояние за день. Для прогноза нужны полностью заполненные дни: напряжение, энергия, настроение, концентрация и дискомфорт.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CycleSenseTextSecondary
                )

                HorizontalDivider()

                SubjectiveChoiceBlock(
                    title = "Напряжение",
                    description = "Оцените уровень напряжения за день",
                    options = stressOptions(),
                    selectedValue = stressScore,
                    onValueSelected = { stressScore = it }
                )

                SubjectiveChoiceBlock(
                    title = "Энергия",
                    description = "Сколько сил и бодрости было в течение дня",
                    options = energyOptions(),
                    selectedValue = energyLevel,
                    onValueSelected = { energyLevel = it }
                )

                SubjectiveChoiceBlock(
                    title = "Настроение",
                    description = "Общее эмоциональное состояние за день",
                    options = moodOptions(),
                    selectedValue = moodScore,
                    onValueSelected = { moodScore = it }
                )

                SubjectiveChoiceBlock(
                    title = "Концентрация",
                    description = "Насколько легко было сосредоточиться",
                    options = concentrationOptions(),
                    selectedValue = concentrationScore,
                    onValueSelected = { concentrationScore = it }
                )

                SubjectiveChoiceBlock(
                    title = "Дискомфорт",
                    description = "Оцените телесный дискомфорт или болевые ощущения за день",
                    options = painOptions(),
                    selectedValue = painLevel,
                    onValueSelected = { painLevel = it }
                )


            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectiveChoiceBlock(
    title: String,
    description: String,
    options: List<SubjectiveOption>,
    selectedValue: Float?,
    onValueSelected: (Float?) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    val selectedLabel = options
        .firstOrNull { it.value == selectedValue }
        ?.label
        ?: "Выберите состояние"

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = CycleSenseTextSecondary
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded
                    )
                }
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {
                DropdownMenuItem(
                    text = {
                        Text("Не заполнено")
                    },
                    onClick = {
                        onValueSelected(null)
                        expanded = false
                    }
                )

                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(option.label)
                        },
                        onClick = {
                            onValueSelected(option.value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

private fun stressOptions(): List<SubjectiveOption> {
    return listOf(
        SubjectiveOption(1f, "Полное спокойствие"),
        SubjectiveOption(3f, "Лёгкое беспокойство"),
        SubjectiveOption(5.5f, "Умеренное напряжение"),
        SubjectiveOption(8f, "Сильный стресс"),
        SubjectiveOption(10f, "Перегрузка")
    )
}

private fun energyOptions(): List<SubjectiveOption> {
    return listOf(
        SubjectiveOption(1f, "Совсем нет сил"),
        SubjectiveOption(3f, "Усталость"),
        SubjectiveOption(5.5f, "Обычное состояние"),
        SubjectiveOption(8f, "Бодрость"),
        SubjectiveOption(10f, "Прилив сил")
    )
}

private fun moodOptions(): List<SubjectiveOption> {
    return listOf(
        SubjectiveOption(1f, "Подавленное"),
        SubjectiveOption(3f, "Грустное"),
        SubjectiveOption(5.5f, "Спокойное"),
        SubjectiveOption(8f, "Приподнятое"),
        SubjectiveOption(10f, "Радостное")
    )
}

private fun concentrationOptions(): List<SubjectiveOption> {
    return listOf(
        SubjectiveOption(1f, "Не могу сосредоточиться"),
        SubjectiveOption(3f, "Рассеянность"),
        SubjectiveOption(5.5f, "Умеренная концентрация"),
        SubjectiveOption(8f, "Собранность"),
        SubjectiveOption(10f, "Полная сосредоточенность")
    )
}

private fun painOptions(): List<SubjectiveOption> {
    return listOf(
        SubjectiveOption(1f, "Ничего не беспокоит"),
        SubjectiveOption(3f, "Лёгкий дискомфорт"),
        SubjectiveOption(5.5f, "Неприятные ощущения"),
        SubjectiveOption(8f, "Выраженный дискомфорт"),
        SubjectiveOption(10f, "Сильная боль")
    )
}

private fun Float.nearestSubjectiveScaleValue(): Float {
    val scaleValues = listOf(1f, 3f, 5.5f, 8f, 10f)

    return scaleValues.minBy { scaleValue ->
        abs(this - scaleValue)
    }
}