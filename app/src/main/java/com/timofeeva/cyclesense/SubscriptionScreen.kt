package com.timofeeva.cyclesense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.timofeeva.cyclesense.ui.theme.CycleSenseCosmos
import com.timofeeva.cyclesense.ui.theme.CycleSenseLavender
import com.timofeeva.cyclesense.ui.theme.CycleSenseSurface
import com.timofeeva.cyclesense.ui.theme.CycleSenseSurfaceSoft
import com.timofeeva.cyclesense.ui.theme.CycleSenseTextSecondary
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import com.timofeeva.cyclesense.ui.theme.CycleSenseBackground

@Composable
fun SubscriptionScreen(
    isPremiumActive: Boolean,
    billingStatusText: String,
    onBuyPremium: () -> Unit,
    onRefreshSubscription: () -> Unit,
    onActivateTestPremium: () -> Unit,
    onDisableTestPremium: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CycleSenseBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Premium",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = "Расширенные функции для анализа цикла и самочувствия.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CycleSenseTextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            TextButton(
                onClick = onBack
            ) {
                Text("✕")
            }
        }

        SubscriptionSectionCard(
            containerColor = if (isPremiumActive) {
                CycleSenseLavender
            } else {
                CycleSenseSurface
            }
        ) {
            Text(
                text = "Ваш статус",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = if (isPremiumActive) {
                    "Premium активен — расширенные функции доступны."
                } else {
                    "Вы используете базовую версию."
                },
                style = MaterialTheme.typography.bodyMedium
            )

            val shouldShowBillingStatus =
                billingStatusText.isNotBlank() &&
                        !billingStatusText.contains("подписка пока не найдена", ignoreCase = true)

            if (shouldShowBillingStatus) {
                Text(
                    text = billingStatusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = CycleSenseTextSecondary
                )
            }

            Button(
                onClick = onBuyPremium,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isPremiumActive,
                shape = CycleSenseButtonShape
            ) {
                CycleSenseButtonText(
                    if (isPremiumActive) {
                        "Premium уже активен"
                    } else {
                        "Оформить Premium"
                    }
                )
            }
        }

        SubscriptionSectionCard {
            Text(
                text = "Что входит в Premium",
                style = MaterialTheme.typography.titleMedium
            )

            FeatureLine("Расширенная аналитика по фазам цикла")
            FeatureLine("Динамика самочувствия")
            FeatureLine("Аналитика пульса, сна и активности")
            FeatureLine("Расширенные прогнозы состояния")
        }

        SubscriptionSectionCard {
            Text(
                text = "Базовая версия",
                style = MaterialTheme.typography.titleMedium
            )

            FeatureLine("Календарь цикла")
            FeatureLine("Ручной ввод самочувствия")
            FeatureLine("Health Connect")
            FeatureLine("Google-вход и облачное восстановление")
        }

        SubscriptionSectionCard(
            containerColor = CycleSenseSurfaceSoft
        ) {


            Text(
                text = "CycleSense не является медицинским сервисом и не ставит диагнозы. Прогнозы и аналитика носят информационный характер.",
                style = MaterialTheme.typography.bodySmall,
                color = CycleSenseTextSecondary
            )

            Text(
                text = "Подписка оформляется через Google Play. Управлять подпиской можно в настройках Google Play.",
                style = MaterialTheme.typography.bodySmall,
                color = CycleSenseTextSecondary
            )
        }

        SubscriptionSectionCard(
            containerColor = CycleSenseSurfaceSoft
        ) {
            Text(
                text = "Тестовый доступ",
                style = MaterialTheme.typography.titleMedium
            )

            Button(
                onClick = onActivateTestPremium,
                modifier = Modifier.fillMaxWidth(),
                shape = CycleSenseButtonShape
            ) {
                CycleSenseButtonText("Активировать тестовый Premium")
            }

            OutlinedButton(
                onClick = onDisableTestPremium,
                modifier = Modifier.fillMaxWidth(),
                shape = CycleSenseButtonShape
            ) {
                CycleSenseButtonText("Отключить тестовый Premium")
            }
        }

        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            shape = CycleSenseButtonShape
        ) {
            CycleSenseButtonText("Назад")
        }
    }
}

@Composable
private fun SubscriptionSectionCard(
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
private fun FeatureLine(
    text: String
) {
    Text(
        text = "✓ $text",
        style = MaterialTheme.typography.bodyMedium
    )
}