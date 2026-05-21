package com.timofeeva.cyclesense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.timofeeva.cyclesense.data.local.entity.UserProfileEntity

@Composable
fun ProfileSelectionScreen(
    profiles: List<UserProfileEntity>,
    onProfileSelected: (UserProfileEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Выберите профиль",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Локальные профили, сохранённые на устройстве",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )

        profiles.forEach { profile ->
            ProfileCard(
                profile = profile,
                onClick = { onProfileSelected(profile) },
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun ProfileCard(
    profile: UserProfileEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kindText = if (profile.isDemo) "DEMO" else "REAL"

    val description = when (profile.profileType) {
        "REAL" -> "Основной локальный профиль пользователя"
        "CLASSIC" -> "Выраженная типичная цикличность"
        "WEAK" -> "Слабая циклическая зависимость"
        "ATYPICAL" -> "Нестандартный профиль"
        "FLAT" -> "Почти ровная динамика"
        "EXTREME" -> "Резкие перепады и экстремумы"
        else -> "Профиль пользователя"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Тип: ${profile.profileType} • $kindText",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp)
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp)
            )

            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            ) {
                Text("Открыть")
            }
        }
    }
}