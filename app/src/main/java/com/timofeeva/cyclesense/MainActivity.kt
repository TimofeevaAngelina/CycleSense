package com.timofeeva.cyclesense

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.timofeeva.cyclesense.analytics.CycleTrendAnalyticsResult
import com.timofeeva.cyclesense.analytics.CycleTrendAnalyticsService
import com.timofeeva.cyclesense.analytics.CycleTrendSection
import com.timofeeva.cyclesense.analytics.PhaseAnalyticsResult
import com.timofeeva.cyclesense.analytics.PhaseAnalyticsService
import com.timofeeva.cyclesense.data.local.DatabaseProvider
import com.timofeeva.cyclesense.data.local.entity.CycleDayEntity
import com.timofeeva.cyclesense.data.local.entity.UserProfileEntity
import com.timofeeva.cyclesense.data.repository.CycleRepositoryRoom
import com.timofeeva.cyclesense.data.repository.ProfileInitializer
import com.timofeeva.cyclesense.data.repository.RoomCycleMapper
import com.timofeeva.cyclesense.health.DailyHeartRateInput
import com.timofeeva.cyclesense.health.DailySleepInput
import com.timofeeva.cyclesense.health.DailyStepsInput
import com.timofeeva.cyclesense.health.HealthConnectManager
import com.timofeeva.cyclesense.ml.AllNext3DaysResult
import com.timofeeva.cyclesense.ml.CycleDayData
import com.timofeeva.cyclesense.ml.CycleParamsPredictor
import com.timofeeva.cyclesense.ml.ForecastDayLabel
import com.timofeeva.cyclesense.ml.ForecastDayLabeler
import com.timofeeva.cyclesense.ml.PredictedCycleParams
import com.timofeeva.cyclesense.ml.PredictionService
import com.timofeeva.cyclesense.synthetic.SyntheticDataSeeder
import com.timofeeva.cyclesense.synthetic.SyntheticProfileType
import com.timofeeva.cyclesense.ui.theme.CycleSenseTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import com.timofeeva.cyclesense.data.repository.HeartRateIntervalRepository
import com.timofeeva.cyclesense.data.repository.HeartRateDaySummary
import com.timofeeva.cyclesense.analytics.HeartRateAnalyticsResult
import com.timofeeva.cyclesense.analytics.HeartRateAnalyticsService
import com.timofeeva.cyclesense.health.HeartRateIntervalInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.timofeeva.cyclesense.health.HealthConnectDaySummary
import com.timofeeva.cyclesense.data.local.entity.HeartRateIntervalEntity
import com.timofeeva.cyclesense.analytics.ActivitySleepAnalyticsResult
import com.timofeeva.cyclesense.analytics.ActivitySleepAnalyticsService
import com.timofeeva.cyclesense.analytics.RelationshipAnalyticsResult
import com.timofeeva.cyclesense.analytics.RelationshipAnalyticsService
import com.timofeeva.cyclesense.analytics.RelationshipMetric





class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CycleSenseTheme {
                AppRoot()
            }
        }
    }
}









