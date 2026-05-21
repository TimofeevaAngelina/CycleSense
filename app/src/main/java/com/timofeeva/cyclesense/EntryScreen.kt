package com.timofeeva.cyclesense

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timofeeva.cyclesense.ui.theme.CycleSenseTextSecondary
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalDensity

private val EntryScreenGap = 12.dp
private val EntryScreenLargeGap = 20.dp

@Composable
fun EntryScreen(
    authStatusText: String,
    onCreateAccount: () -> Unit,
    onSignInAccount: () -> Unit,
    onOpenDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontScale = LocalDensity.current.fontScale

    val logoSize = when {
        fontScale >= 1.5f -> 112.dp
        fontScale >= 1.3f -> 132.dp
        else -> 180.dp
    }

    AdaptiveScreenContainer(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "CycleSense",
                modifier = Modifier
                    .size(logoSize)
                    .padding(bottom = 8.dp)
            )

            Text(
                text = "CycleSense",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Отслеживайте цикл, самочувствие и физиологические показатели. Вы можете создать новый аккаунт или войти в существующий.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = EntryScreenGap)
            )

            Spacer(modifier = Modifier.height(EntryScreenLargeGap))

            Button(
                onClick = onCreateAccount,
                modifier = Modifier.fillMaxWidth(),
                shape = CycleSenseButtonShape
            ) {
                CycleSenseButtonText("Создать аккаунт")
            }

            Button(
                onClick = onSignInAccount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = EntryScreenGap),
                shape = CycleSenseButtonShape
            ) {
                CycleSenseButtonText("Войти в аккаунт")
            }

            Button(
                onClick = onOpenDemo,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = EntryScreenGap),
                shape = CycleSenseButtonShape
            ) {
                CycleSenseButtonText("Открыть demo-режим")
            }

            if (authStatusText.isNotBlank()) {
                Text(
                    text = authStatusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CycleSenseTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = EntryScreenLargeGap)
                )
            }
        }
    }
}