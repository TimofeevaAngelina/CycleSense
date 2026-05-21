package com.timofeeva.cyclesense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.timofeeva.cyclesense.ui.theme.CycleSenseTheme

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CycleSenseTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Политика доступа к данным здоровья",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Приложение CycleSense использует данные шагов и другие данные здоровья только для анализа состояния и построения прогнозов. Данные не используются в медицинских целях.",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}