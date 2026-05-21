package com.timofeeva.cyclesense

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.timofeeva.cyclesense.data.local.entity.UserProfileEntity
import com.timofeeva.cyclesense.ui.theme.CycleSenseCosmos
import com.timofeeva.cyclesense.ui.theme.CycleSenseError
import com.timofeeva.cyclesense.ui.theme.CycleSenseLavender
import com.timofeeva.cyclesense.ui.theme.CycleSenseSurface
import com.timofeeva.cyclesense.ui.theme.CycleSenseSurfaceSoft
import com.timofeeva.cyclesense.ui.theme.CycleSenseTextSecondary
import java.util.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak

@Composable
fun ProfileTabContent(
    isDemoMode: Boolean,
    googleUserEmail: String?,
    googleUserDisplayName: String?,
    isPremiumActive: Boolean,
    isAccountDeletionRunning: Boolean,
    onOpenSubscription: () -> Unit,
    onConnectHealthConnect: () -> Unit,
    healthConnectStatusText: String,
    isHealthConnectConnected: Boolean,
    activeProfileName: String,
    activeProfileDateOfBirth: String?,
    activeProfileAgeYears: Int?,
    activeProfileWeightKg: Float?,
    activeProfileHeightCm: Float?,
    profiles: List<UserProfileEntity>,
    profileMenuExpanded: Boolean,
    onProfileMenuExpandedChange: (Boolean) -> Unit,
    onProfileSelected: (UserProfileEntity) -> Unit,
    onOpenProfileParams: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccountAndAllData: () -> Unit,
    onBackToEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteAccountDialog by remember {
        mutableStateOf(false)
    }

    var showSignOutDialog by remember {
        mutableStateOf(false)
    }

    var visibleLegalDocument by remember {
        mutableStateOf<LegalDocumentType?>(null)
    }

    val safeActiveProfileName = activeProfileName.ifBlank {
        "Профиль не выбран"
    }

    val filledProfileFields = listOf(
        activeProfileDateOfBirth != null,
        activeProfileWeightKg != null,
        activeProfileHeightCm != null
    ).count { it }

    val profileCompletion = filledProfileFields / 3f
    val showCompletionIndicator = profileCompletion < 1f

    val animatedProfileCompletion by animateFloatAsState(
        targetValue = profileCompletion,
        animationSpec = tween(
            durationMillis = 700,
            easing = FastOutSlowInEasing
        ),
        label = "profileCompletion"
    )

    val progressColor = when {
        animatedProfileCompletion < 0.34f -> CycleSenseError
        animatedProfileCompletion < 0.67f -> Color(0xFFC89B3C)
        else -> CycleSenseCosmos
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (isDemoMode) {
            Button(
                onClick = onBackToEntry,
                modifier = Modifier.fillMaxWidth(),
                shape = CycleSenseButtonShape
            ) {
                CycleSenseButtonText("Назад")
            }
        }

        ProfileSectionCard {
            Text(
                text = "Мой профиль",
                style = MaterialTheme.typography.titleMedium.copy(
                    hyphens = Hyphens.Auto,
                    lineBreak = LineBreak.Paragraph
                ),
                softWrap = true
            )

            val profileInfoText = buildString {
                if (activeProfileAgeYears != null) {
                    append("Возраст: ")
                    append(activeProfileAgeYears)
                }

                if (activeProfileWeightKg != null) {
                    if (isNotEmpty()) append(" • ")
                    append("Вес: ")
                    append(String.format(Locale.US, "%.1f", activeProfileWeightKg))
                    append(" кг")
                }

                if (activeProfileHeightCm != null) {
                    if (isNotEmpty()) append(" • ")
                    append("Рост: ")
                    append(String.format(Locale.US, "%.0f", activeProfileHeightCm))
                    append(" см")
                }
            }

            if (showCompletionIndicator) {
                LinearProgressIndicator(
                    progress = { animatedProfileCompletion },
                    modifier = Modifier.fillMaxWidth(),
                    color = progressColor,
                    trackColor = CycleSenseSurfaceSoft
                )

                Text(
                    text = "Заполненность профиля: ${(animatedProfileCompletion * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = CycleSenseTextSecondary
                )

                ProfileInfoRow(
                    label = "Возраст",
                    value = activeProfileAgeYears?.toString(),
                    isFilled = activeProfileAgeYears != null
                )

                ProfileInfoRow(
                    label = "Вес",
                    value = activeProfileWeightKg?.let {
                        String.format(Locale.US, "%.1f кг", it)
                    },
                    isFilled = activeProfileWeightKg != null
                )

                ProfileInfoRow(
                    label = "Рост",
                    value = activeProfileHeightCm?.let {
                        String.format(Locale.US, "%.0f см", it)
                    },
                    isFilled = activeProfileHeightCm != null
                )
            } else if (profileInfoText.isNotEmpty()) {
                Text(
                    text = profileInfoText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = onOpenProfileParams,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = CycleSenseButtonShape
            ) {
                CycleSenseButtonText("Изменить данные профиля")
            }

            if (!isDemoMode &&
                (activeProfileDateOfBirth == null || activeProfileWeightKg == null || activeProfileHeightCm == null)
            ) {
                Text(
                    text = "Вы можете внести данные о весе и росте для более полной настройки профиля.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CycleSenseTextSecondary
                )
            }
        }

        if (!isDemoMode) {
            ProfileSectionCard {
                Text(
                    text = "Учётная запись",
                    style = MaterialTheme.typography.titleMedium.copy(
                        hyphens = Hyphens.Auto,
                        lineBreak = LineBreak.Paragraph
                    ),
                    softWrap = true
                )

                Text(
                    text = "Имя: $safeActiveProfileName",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Email: ${googleUserEmail ?: "не определён"}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = {
                        showSignOutDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CycleSenseButtonShape
                ) {
                    CycleSenseButtonText("Выйти из учётной записи")
                }
            }
        }

        if (!isDemoMode) {
            ProfileSectionCard {
                Text(
                    text = "Подписка",
                    style = MaterialTheme.typography.titleMedium.copy(
                        hyphens = Hyphens.Auto,
                        lineBreak = LineBreak.Paragraph
                    ),
                    softWrap = true
                )

                Text(
                    text = if (isPremiumActive) {
                        "Premium активен — доступны расширенные функции."
                    } else {
                        "Free — доступны базовые функции приложения."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = onOpenSubscription,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CycleSenseButtonShape
                ) {
                    CycleSenseButtonText(
                        if (isPremiumActive) {
                            "Управление Premium"
                        } else {
                            "Оформить Premium"
                        }
                    )
                }
            }
        }

        if (!isDemoMode) {
            ProfileSectionCard {
                Text(
                    text = "Health Connect",
                    style = MaterialTheme.typography.titleMedium.copy(
                        hyphens = Hyphens.Auto,
                        lineBreak = LineBreak.Paragraph
                    ),
                    softWrap = true
                )

                Text(
                    text = healthConnectStatusText,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Если данные не появляются, проверьте передачу шагов, сна и пульса в приложении-источнике данных.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CycleSenseTextSecondary
                )

                Text(
                    text = "Health Connect хранит данные устройства. Подключайте его только к профилю владельца телефона.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CycleSenseTextSecondary
                )

                Button(
                    onClick = onConnectHealthConnect,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CycleSenseButtonShape
                ) {
                    CycleSenseButtonText(
                        if (isHealthConnectConnected) {
                            "Открыть / проверить Health Connect"
                        } else {
                            "Подключить / настроить Health Connect"
                        }
                    )
                }
            }
        }

        if (isDemoMode) {
            val demoProfiles = profiles.filter { it.isDemo }

            ProfileSectionCard(
                containerColor = CycleSenseLavender
            ) {
                Text(
                    text = "Выбор demo-профиля",
                    style = MaterialTheme.typography.titleMedium.copy(
                        hyphens = Hyphens.Auto,
                        lineBreak = LineBreak.Paragraph
                    ),
                    softWrap = true
                )

                Button(
                    onClick = { onProfileMenuExpandedChange(true) },
                    enabled = demoProfiles.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = CycleSenseButtonShape
                ) {
                    CycleSenseButtonText(
                        if (demoProfiles.isNotEmpty()) {
                            safeActiveProfileName
                        } else {
                            "Demo-профили не найдены"
                        }
                    )
                }

                DropdownMenu(
                    expanded = profileMenuExpanded && demoProfiles.isNotEmpty(),
                    onDismissRequest = { onProfileMenuExpandedChange(false) }
                ) {
                    demoProfiles.forEach { profile ->
                        DropdownMenuItem(
                            text = { Text(profile.name) },
                            onClick = {
                                onProfileMenuExpandedChange(false)
                                onProfileSelected(profile)
                            }
                        )
                    }
                }

                Text(
                    text = "Demo-профили предназначены для тестирования, лучше использовать их на эмуляторе.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CycleSenseTextSecondary
                )
            }
        }

        if (!isDemoMode) {
            ProfileSectionCard {
                Text(
                    text = "Данные и безопасность",
                    style = MaterialTheme.typography.titleMedium.copy(
                        hyphens = Hyphens.Auto,
                        lineBreak = LineBreak.Paragraph
                    ),
                    softWrap = true
                )

                Text(
                    text = "CycleSense использует данные профиля, цикла, самочувствия и Health Connect только для календаря, аналитики, прогноза и резервного копирования.",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedButton(
                    onClick = {
                        visibleLegalDocument = LegalDocumentType.PRIVACY_POLICY
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CycleSenseButtonShape
                ) {
                    CycleSenseButtonText("Политика конфиденциальности")
                }

                OutlinedButton(
                    onClick = {
                        visibleLegalDocument = LegalDocumentType.TERMS_OF_USE
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CycleSenseButtonShape
                ) {
                    CycleSenseButtonText("Условия использования")
                }
            }

            ProfileSectionCard(
                containerColor = CycleSenseSurfaceSoft
            ) {
                Text(
                    text = "Удаление аккаунта",
                    style = MaterialTheme.typography.titleMedium.copy(
                        hyphens = Hyphens.Auto,
                        lineBreak = LineBreak.Paragraph
                    ),
                    softWrap = true
                )

                Text(
                    text = "Удаляет локальные данные, облачную резервную копию и аккаунт CycleSense. Действие нельзя отменить.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CycleSenseTextSecondary
                )

                if (isAccountDeletionRunning) {
                    Text(
                        text = "Удаление аккаунта и данных может занять несколько секунд. Пожалуйста, не закрывайте приложение.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CycleSenseTextSecondary
                    )
                }

                OutlinedButton(
                    onClick = {
                        showDeleteAccountDialog = true
                    },
                    enabled = !isAccountDeletionRunning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = CycleSenseButtonShape
                ) {
                    CycleSenseButtonText(
                        if (isAccountDeletionRunning) {
                            "Удаление аккаунта..."
                        } else {
                            "Удалить аккаунт и все данные"
                        }
                    )
                }
            }
        }
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteAccountDialog = false
            },
            title = {
                Text("Удалить аккаунт?")
            },
            text = {
                Text(
                    "Будут удалены локальные данные на устройстве, облачная резервная копия и аккаунт CycleSense. Это действие нельзя отменить."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                    },
                    shape = CycleSenseButtonShape
                ) {
                    Text(
                        text = "Отмена",
                        textAlign = TextAlign.Center
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isAccountDeletionRunning,
                    onClick = {
                        showDeleteAccountDialog = false
                        onDeleteAccountAndAllData()
                    },
                    shape = CycleSenseButtonShape
                ) {
                    Text(
                        text = if (isAccountDeletionRunning) {
                            "Удаление..."
                        } else {
                            "Удалить"
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = {
                showSignOutDialog = false
            },
            title = {
                Text("Выход из учётной записи")
            },
            text = {
                Text("Выйти из учётной записи?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                    shape = CycleSenseButtonShape
                ) {
                    Text(
                        text = "Да",
                        textAlign = TextAlign.Center
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                    },
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

    visibleLegalDocument?.let { documentType ->
        PrivacyDocumentsDialog(
            documentType = documentType,
            onDismiss = {
                visibleLegalDocument = null
            }
        )
    }
}

@Composable
private fun ProfileSectionCard(
    modifier: Modifier = Modifier,
    containerColor: Color = CycleSenseSurface,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String?,
    isFilled: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(
                    color = if (isFilled) CycleSenseCosmos else CycleSenseError,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isFilled) "✓" else "!",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "$label: ${value ?: "не указано"}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}