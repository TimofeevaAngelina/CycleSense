package com.timofeeva.cyclesense

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
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
import com.timofeeva.cyclesense.analytics.ActivitySleepAnalyticsResult
import com.timofeeva.cyclesense.analytics.ActivitySleepAnalyticsService
import com.timofeeva.cyclesense.analytics.CycleTrendAnalyticsResult
import com.timofeeva.cyclesense.analytics.CycleTrendAnalyticsService
import com.timofeeva.cyclesense.analytics.HeartRateAnalyticsResult
import com.timofeeva.cyclesense.analytics.HeartRateAnalyticsService
import com.timofeeva.cyclesense.analytics.PhaseAnalyticsResult
import com.timofeeva.cyclesense.analytics.PhaseAnalyticsService
import com.timofeeva.cyclesense.analytics.RelationshipAnalyticsResult
import com.timofeeva.cyclesense.analytics.RelationshipAnalyticsService
import com.timofeeva.cyclesense.data.local.DatabaseProvider
import com.timofeeva.cyclesense.data.local.entity.CycleDayEntity
import com.timofeeva.cyclesense.data.local.entity.UserProfileEntity
import com.timofeeva.cyclesense.data.repository.CycleRepositoryRoom
import com.timofeeva.cyclesense.data.repository.HeartRateDaySummary
import com.timofeeva.cyclesense.data.repository.HeartRateIntervalRepository
import com.timofeeva.cyclesense.data.repository.ProfileInitializer
import com.timofeeva.cyclesense.data.repository.RoomCycleMapper
import com.timofeeva.cyclesense.health.HealthConnectManager
import com.timofeeva.cyclesense.ml.AllNext3DaysResult
import com.timofeeva.cyclesense.ml.CycleDayData
import com.timofeeva.cyclesense.ml.ForecastDayLabel
import com.timofeeva.cyclesense.ml.PredictedCycleParams
import com.timofeeva.cyclesense.ml.PredictionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import com.timofeeva.cyclesense.analytics.CycleLengthAnalyticsResult
import com.timofeeva.cyclesense.analytics.CycleLengthAnalyticsService


@Composable
fun CalendarWithPredictionScreen(
    modifier: Modifier = Modifier,
    isDemoMode: Boolean,
    googleUserEmail: String?,
    googleUserDisplayName: String?,
    isPremiumActive: Boolean,
    onPremiumStatusChanged: (Boolean) -> Unit,
    billingStatusText: String,
    onBuyPremium: () -> Unit,
    onRefreshSubscription: () -> Unit,
    onSignOut: () -> Unit,
    onBackupRealProfileToCloud: suspend () -> Unit,
    onDeleteAccountAndAllData: suspend () -> Unit,
    onBackToEntry: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val predictionService = remember(context) {
        PredictionService(context = context)
    }

    val cycleLengthAnalyticsService = remember {
        CycleLengthAnalyticsService()
    }

    val phaseAnalyticsService = remember {
        PhaseAnalyticsService()
    }

    val cycleTrendAnalyticsService = remember {
        CycleTrendAnalyticsService()
    }

    val heartRateAnalyticsService = remember {
        HeartRateAnalyticsService()
    }

    val activitySleepAnalyticsService = remember {
        ActivitySleepAnalyticsService()
    }

    val relationshipAnalyticsService = remember {
        RelationshipAnalyticsService()
    }

    val roomDb = remember(context) {
        DatabaseProvider.getDatabase(context)
    }

    val roomRepository = remember(roomDb) {
        CycleRepositoryRoom(
            cycleDao = roomDb.cycleDao(),
            cycleDayDao = roomDb.cycleDayDao()
        )
    }

    val calendarDataService = remember(roomRepository) {
        CalendarDataService(roomRepository)
    }

    val menstruationEditActions = remember(
        roomRepository,
        calendarDataService
    ) {
        MainMenstruationEditActions(
            roomRepository = roomRepository,
            calendarDataService = calendarDataService
        )
    }

    val heartRateIntervalRepository = remember(roomDb) {
        HeartRateIntervalRepository(roomDb.heartRateIntervalDao())
    }

    val userProfileDao = remember(roomDb) {
        roomDb.userProfileDao()
    }

    val healthConnectManager = remember(context) {
        HealthConnectManager(context)
    }

    val healthSyncActions = remember(
        roomRepository,
        heartRateIntervalRepository,
        healthConnectManager
    ) {
        MainHealthSyncActions(
            roomRepository = roomRepository,
            heartRateIntervalRepository = heartRateIntervalRepository,
            healthConnectManager = healthConnectManager
        )
    }

    var healthPermissionRefreshKey by remember { mutableStateOf(0) }
    var healthPermissionRequestStarted by remember { mutableStateOf(false) }


    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val maxCalendarMonth = YearMonth.of(LocalDate.now().year + 1, 12)
    val maxCalendarDate = maxCalendarMonth.atEndOfMonth()
    val minCalendarMonth = YearMonth.of(1970, 1)

    var statusText by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<AllNext3DaysResult?>(null) }
    var predictedCycleParams by remember { mutableStateOf<PredictedCycleParams?>(null) }
    var forecastLabels by remember { mutableStateOf<List<ForecastDayLabel>>(emptyList()) }
    var predictionWarningMessage by remember { mutableStateOf<String?>(null) }

    var allCycles by remember { mutableStateOf<List<List<CycleDayData>>>(emptyList()) }
    var dayDataByDate by remember { mutableStateOf<Map<LocalDate, CycleDayData>>(emptyMap()) }
    var isCycleCompletedByDate by remember { mutableStateOf<Map<LocalDate, Boolean>>(emptyMap()) }

    var dbCyclesForActiveProfile by remember {
        mutableStateOf<List<com.timofeeva.cyclesense.data.local.entity.CycleEntity>>(emptyList())
    }

    var selectedDateCycleHint by remember {
        mutableStateOf<CycleTimingHint?>(null)
    }

    var profiles by remember { mutableStateOf<List<UserProfileEntity>>(emptyList()) }
    var activeProfileId by remember { mutableStateOf<Long?>(null) }
    var activeProfileName by remember { mutableStateOf("Профиль не выбран") }
    var activeProfileType by remember { mutableStateOf("UNKNOWN") }
    var activeProfileDateOfBirth by remember { mutableStateOf<String?>(null) }
    var activeProfileAgeYears by remember { mutableStateOf<Int?>(null) }
    var activeProfileWeightKg by remember { mutableStateOf<Float?>(null) }
    var activeProfileHeightCm by remember { mutableStateOf<Float?>(null) }
    var activeProfileAvgCycleLength by remember { mutableStateOf<Int?>(null) }
    var activeProfileAvgMensesLength by remember { mutableStateOf<Int?>(null) }
    var profileMenuExpanded by remember { mutableStateOf(false) }

    val delayDates = remember(
        dayDataByDate,
        allCycles,
        dbCyclesForActiveProfile,
        activeProfileAvgCycleLength,
        activeProfileAvgMensesLength
    ) {
        buildDelayDatesForKnownDays(
            dayDataByDate = dayDataByDate,
            allCycles = allCycles,
            dbCycles = dbCyclesForActiveProfile,
            fallbackCycleLength = activeProfileAvgCycleLength ?: 28,
            fallbackMensesLength = activeProfileAvgMensesLength ?: 5
        )
    }

    val isCurrentCycleDelayed = remember(delayDates) {
        delayDates.contains(LocalDate.now())
    }

    val futureCycleMarks = remember(
        dayDataByDate,
        allCycles,
        dbCyclesForActiveProfile,
        maxCalendarDate,
        isCurrentCycleDelayed,
        activeProfileAvgCycleLength,
        activeProfileAvgMensesLength
    ) {
        if (isCurrentCycleDelayed) {
            emptyMap()
        } else {
            buildFutureCycleMarks(
                dayDataByDate = dayDataByDate,
                allCycles = allCycles,
                dbCycles = dbCyclesForActiveProfile,
                maxDate = maxCalendarDate,
                fallbackCycleLength = activeProfileAvgCycleLength ?: 28,
                fallbackMensesLength = activeProfileAvgMensesLength ?: 5
            )
        }
    }


    var cycleLengthAnalytics by remember {
        mutableStateOf<CycleLengthAnalyticsResult?>(null)
    }
    var phaseAnalytics by remember { mutableStateOf<PhaseAnalyticsResult?>(null) }
    var cycleTrendAnalytics by remember { mutableStateOf<CycleTrendAnalyticsResult?>(null) }
    var heartRateAnalytics by remember { mutableStateOf<HeartRateAnalyticsResult?>(null) }

    var activitySleepAnalytics by remember {
        mutableStateOf<ActivitySleepAnalyticsResult?>(null)
    }
    var relationshipAnalytics by remember {
        mutableStateOf<RelationshipAnalyticsResult?>(null)
    }


    var showProfileParamsDialog by remember { mutableStateOf(false) }

    var showEditDayDialog by remember { mutableStateOf(false) }
    var editingDayEntity by remember { mutableStateOf<CycleDayEntity?>(null) }
    var isCreatingNewDay by remember { mutableStateOf(false) }

    var selectedDateHasEntry by remember { mutableStateOf(false) }

    var selectedDateHeartRateSummary by remember { mutableStateOf<HeartRateDaySummary?>(null) }

    var selectedTab by remember { mutableStateOf(MainTab.CALENDAR) }

    var isHealthConnectSyncRunning by remember { mutableStateOf(false) }

    var healthConnectStatusText by remember {
        mutableStateOf("Статус Health Connect не проверен")
    }

    var isHealthConnectConnected by remember {
        mutableStateOf(false)
    }

    var showSubscriptionScreen by remember {
        mutableStateOf(false)
    }

    var isAccountDeletionRunning by remember { mutableStateOf(false) }

    var isMenstruationEditMode by remember { mutableStateOf(false) }

    var editingMenstruationDates by remember {
        mutableStateOf<Set<LocalDate>>(emptySet())
    }

    suspend fun loadProfilesAndActiveProfile() {
        profiles = userProfileDao.getAllProfiles()
        val activeProfile = userProfileDao.getActiveProfile()
        activeProfileId = activeProfile?.id
        activeProfileName = activeProfile?.name ?: "Профиль не выбран"
        activeProfileType = activeProfile?.profileType ?: "UNKNOWN"
        activeProfileDateOfBirth = activeProfile?.dateOfBirth
        activeProfileAgeYears = calculateAgeYears(activeProfile?.dateOfBirth)
        activeProfileWeightKg = activeProfile?.weightKg
        activeProfileHeightCm = activeProfile?.heightCm
        activeProfileAvgCycleLength = activeProfile?.avgCycleLength
        activeProfileAvgMensesLength = activeProfile?.avgMensesLength
    }

    suspend fun refreshHealthConnectStatusForProfile() {
        val status = buildMainHealthConnectStatus(
            healthConnectManager = healthConnectManager
        )

        isHealthConnectConnected = status.isConnected
        healthConnectStatusText = status.statusText
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = healthConnectManager.createPermissionsLauncherContract()
    ) { _: Set<String> ->
        healthPermissionRequestStarted = false
        healthPermissionRefreshKey++

        scope.launch {
            refreshHealthConnectStatusForProfile()
        }
    }

    suspend fun reloadDataFromRoom() {
        loadProfilesAndActiveProfile()

        val profileId = activeProfileId
        if (profileId == null) {
            dbCyclesForActiveProfile = emptyList()
            allCycles = emptyList()
            dayDataByDate = emptyMap()
            isCycleCompletedByDate = emptyMap()
            selectedDateHasEntry = false
            selectedDateHeartRateSummary = null
            selectedDateCycleHint = null
            return
        }

        val dbCycles = roomRepository.getCyclesForProfile(profileId)

        var dbDays = roomRepository.getDaysForProfile(profileId)

        val daysWithRecalculatedCalories = dbDays.map { day ->
            val calculatedCalories = calculateDailyCaloriesEstimate(
                steps = day.steps,
                heartRate = day.heartRate,
                ageYears = activeProfileAgeYears,
                weightKg = activeProfileWeightKg,
                heightCm = activeProfileHeightCm
            )

            if (calculatedCalories != null && calculatedCalories != day.calories) {
                day.copy(calories = calculatedCalories)
            } else {
                day
            }
        }

        val changedCaloriesDays = daysWithRecalculatedCalories.filterIndexed { index, updatedDay ->
            updatedDay.calories != dbDays[index].calories
        }

        changedCaloriesDays.forEach { updatedDay ->
            roomRepository.updateDay(updatedDay)
        }

        dbDays = daysWithRecalculatedCalories

        dbCyclesForActiveProfile = dbCycles
        cycleLengthAnalytics = cycleLengthAnalyticsService.buildCycleLengthAnalytics(
            cycles = dbCycles
        )

        val newAllCycles = RoomCycleMapper.buildCycles(
            cycles = dbCycles,
            days = dbDays
        )

        val newDayDataByDate = RoomCycleMapper.buildDateMap(
            days = dbDays,
            cycles = dbCycles
        )

        allCycles = newAllCycles
        dayDataByDate = newDayDataByDate

        val cycleCompletedMap = dbCycles.associate { cycle ->
            cycle.id to cycle.isCompleted
        }

        isCycleCompletedByDate = dbDays.associate { day ->
            LocalDate.parse(day.date) to (cycleCompletedMap[day.cycleId] ?: false)
        }

        if (newDayDataByDate.isNotEmpty()) {
            val maxDate = newDayDataByDate.keys.maxOrNull()

            if (maxDate != null) {
                selectedDate = if (newDayDataByDate.containsKey(selectedDate)) {
                    selectedDate
                } else {
                    maxDate
                }

                currentMonth = YearMonth.from(selectedDate)
            }
        }

        selectedDateHasEntry = newDayDataByDate.containsKey(selectedDate)

        selectedDateHeartRateSummary = heartRateIntervalRepository.getDaySummary(
            profileId = profileId,
            date = selectedDate.toString()
        )

        val currentDelayDates = buildDelayDatesForKnownDays(
            dayDataByDate = newDayDataByDate,
            allCycles = newAllCycles,
            dbCycles = dbCycles,
            fallbackCycleLength = activeProfileAvgCycleLength ?: 28,
            fallbackMensesLength = activeProfileAvgMensesLength ?: 5
        )

        selectedDateCycleHint = buildDelayHintForDate(
            date = selectedDate,
            dayData = newDayDataByDate[selectedDate],
            delayDates = currentDelayDates,
            allDelayDates = currentDelayDates
        ) ?: if (selectedDate == LocalDate.now()) {
            newDayDataByDate[selectedDate]?.let { selectedDay ->
                val timing = buildCycleTimingPrediction(
                    allCycles = newAllCycles,
                    dbCycles = dbCycles,
                    fallbackCycleLength = activeProfileAvgCycleLength ?: 28,
                    fallbackMensesLength = activeProfileAvgMensesLength ?: 5
                )

                buildCycleTimingHint(
                    currentDayInCycle = selectedDay.dayInCycle,
                    timingPrediction = timing
                )
            }
        } else {
            null
        }
    }

    suspend fun syncRealProfileFromHealthConnectAndReload() {
        if (isDemoMode) return

        val profileId = activeProfileId
        if (profileId == null) {
            statusText = "Нет активного профиля"
            return
        }

        val activeProfile = userProfileDao.getProfileById(profileId)
        if (activeProfile == null) {
            statusText = "Профиль не найден"
            return
        }

        if (!healthConnectManager.isAvailable()) {
            statusText = "Health Connect недоступен на этом устройстве"
            return
        }

        if (!healthConnectManager.hasAllPermissions()) {
            statusText = "Для импорта данных нужно разрешить доступ к Health Connect"
            permissionsLauncher.launch(healthConnectManager.permissions)
            return
        }

        val updatedDays = withContext(Dispatchers.IO) {
            healthSyncActions.syncHealthConnectToRoomForProfile(
                profileId = profileId,
                onlyOwnData = false,
                excludeOwnData = true,
                ageYears = calculateAgeYears(activeProfile.dateOfBirth),
                weightKg = activeProfile.weightKg,
                heightCm = activeProfile.heightCm
            )
        }

        reloadDataFromRoom()

        statusText = if (updatedDays > 0) {
            "Данные из Health Connect обновлены: $updatedDays дней"
        } else {
            "Данные Health Connect не найдены для созданных дней календаря"
        }

        if (updatedDays > 0) {
            onBackupRealProfileToCloud()
        }
    }


    suspend fun activateProfile(profile: UserProfileEntity) {
        userProfileDao.activateProfile(profile.id)

        if (profile.isDemo) {
            if (!isHealthConnectSyncRunning) {
                isHealthConnectSyncRunning = true

                withContext(Dispatchers.IO) {
                    try {
                        healthSyncActions.ensureDemoProfileSeeded(profile)
                        healthSyncActions.syncActiveDemoProfileToHealthConnect(profile)
                        healthSyncActions.syncHealthConnectToRoomForProfile(
                            profileId = profile.id,
                            onlyOwnData = true,
                            excludeOwnData = false,
                            ageYears = calculateAgeYears(profile.dateOfBirth),
                            weightKg = profile.weightKg,
                            heightCm = profile.heightCm
                        )
                    } finally {
                        isHealthConnectSyncRunning = false
                    }
                }
            }
        }

        calendarDataService.ensureCalendarGeneratedUpToToday(profile.id)

        reloadDataFromRoom()

        result = null
        predictedCycleParams = null
        forecastLabels = emptyList()
        predictionWarningMessage = null
        phaseAnalytics = null
        cycleTrendAnalytics = null
        heartRateAnalytics = null
        activitySleepAnalytics = null
        relationshipAnalytics = null

        statusText = if (profile.isDemo) {
            "Активный demo-профиль: ${profile.name}. Данные записаны в Health Connect и перечитаны обратно в приложение"
        } else {
            "Активный профиль: ${profile.name}"
        }
    }

    suspend fun openEditDialogForSelectedDate() {
        val profileId = activeProfileId
        if (profileId == null) {
            statusText = "Нет активного профиля"
            return
        }

        val dayEntity = roomRepository.getDayForProfileByDate(
            profileId = profileId,
            date = selectedDate.toString()
        )

        if (dayEntity != null) {
            editingDayEntity = dayEntity
            isCreatingNewDay = false
            showEditDayDialog = true
            return
        }

        editingDayEntity = null
        isCreatingNewDay = true
        showEditDayDialog = true
    }

    suspend fun createNewDay(formData: EditDayFormData) {
        val profileId = activeProfileId
        if (profileId == null) {
            statusText = "Нет активного профиля"
            return
        }

        val existingDay = roomRepository.getDayForProfileByDate(
            profileId = profileId,
            date = selectedDate.toString()
        )

        if (existingDay != null) {
            statusText = "Запись за эту дату уже существует"
            return
        }

        val cycles = roomRepository.getCyclesForProfile(profileId).sortedBy { it.startDate }
        val lastCycle = cycles.lastOrNull()

        val targetCycleId = when {
            lastCycle == null -> {
                roomRepository.insertCycle(
                    com.timofeeva.cyclesense.data.local.entity.CycleEntity(
                        userProfileId = profileId,
                        startDate = selectedDate.toString(),
                        endDate = null,
                        cycleLength = null,
                        predictedCycleLength = 28,
                        predictedMensesLength = 5,
                        predictedOvulationDay = 14,
                        isCompleted = false
                    )
                )
            }

            lastCycle.isCompleted -> {
                roomRepository.insertCycle(
                    com.timofeeva.cyclesense.data.local.entity.CycleEntity(
                        userProfileId = profileId,
                        startDate = selectedDate.toString(),
                        endDate = null,
                        cycleLength = null,
                        predictedCycleLength = lastCycle.predictedCycleLength
                            ?: lastCycle.cycleLength ?: 28,
                        predictedMensesLength = lastCycle.predictedMensesLength ?: 5,
                        predictedOvulationDay = lastCycle.predictedOvulationDay ?: 14,
                        isCompleted = false
                    )
                )
            }

            else -> {
                lastCycle.id
            }
        }

        val cycleDays = roomRepository.getDaysForProfile(profileId)
            .filter { day -> day.cycleId == targetCycleId }
            .sortedBy { it.date }

        val calculatedDayInCycle = if (cycleDays.isEmpty()) {
            1
        } else {
            val maxExistingDay = cycleDays.maxOfOrNull { it.dayInCycle } ?: 0
            maxExistingDay + 1
        }

        val phase = inferPhaseForManualDay(
            dayInCycle = calculatedDayInCycle,
            isMenstruationMarked = formData.isMenstruationMarked
        )


        val newDay = CycleDayEntity(
            cycleId = targetCycleId,
            date = selectedDate.toString(),
            dayInCycle = calculatedDayInCycle,
            phase = phase,

            steps = null,
            calories = null,
            heartRate = null,
            rmssd = null,
            sleepHours = null,

            stressScore = formData.stressScore,
            energyLevel = formData.energyLevel,
            moodScore = formData.moodScore,
            concentrationScore = formData.concentrationScore,
            painLevel = formData.painLevel,

            isMenstruationMarked = formData.isMenstruationMarked,
            notes = formData.notes
        )

        roomRepository.insertDay(newDay)

        // Сначала закрываем окно, чтобы пользователь сразу вернулся к календарю.
        showEditDayDialog = false
        editingDayEntity = null
        isCreatingNewDay = false

        result = null
        predictedCycleParams = null
        forecastLabels = emptyList()
        predictionWarningMessage = null

        phaseAnalytics = null
        cycleTrendAnalytics = null
        heartRateAnalytics = null
        activitySleepAnalytics = null
        relationshipAnalytics = null

        statusText = "Сохраняем новую запись дня..."

        calendarDataService.rebuildCyclesAfterMenstruationChanges(profileId)
        calendarDataService.ensureCalendarGeneratedUpToToday(profileId)

        reloadDataFromRoom()

        if (!isDemoMode) {
            syncRealProfileFromHealthConnectAndReload()
        } else {
            statusText = "Создана новая запись дня: ${newDay.date}"
        }
    }

    suspend fun saveEditedDay(formData: EditDayFormData) {
        if (isCreatingNewDay) {
            createNewDay(formData)
            return
        }

        val originalDay = editingDayEntity
        if (originalDay == null) {
            statusText = "Не удалось сохранить: день не найден"
            return
        }

        val profileId = activeProfileId
        if (profileId == null) {
            statusText = "Нет активного профиля"
            return
        }

        val updatedDay = originalDay.copy(
            stressScore = formData.stressScore,
            energyLevel = formData.energyLevel,
            moodScore = formData.moodScore,
            concentrationScore = formData.concentrationScore,
            painLevel = formData.painLevel,
            isMenstruationMarked = formData.isMenstruationMarked,
            notes = formData.notes
        )

        // Сначала закрываем окно, чтобы пользователь не ждал тяжёлые операции.
        showEditDayDialog = false
        editingDayEntity = null
        isCreatingNewDay = false

        result = null
        predictedCycleParams = null
        forecastLabels = emptyList()
        predictionWarningMessage = null

        phaseAnalytics = null
        cycleTrendAnalytics = null
        heartRateAnalytics = null
        activitySleepAnalytics = null
        relationshipAnalytics = null

        statusText = "Сохраняем данные дня..."

        roomRepository.updateDay(updatedDay)

        calendarDataService.rebuildCyclesAfterMenstruationChanges(profileId)
        calendarDataService.ensureCalendarGeneratedUpToToday(profileId)

        reloadDataFromRoom()

        if (!isDemoMode) {
            syncRealProfileFromHealthConnectAndReload()
        } else {
            statusText = "Данные дня сохранены: ${updatedDay.date}"
        }
    }

    suspend fun saveProfileParams(formData: ProfileParamsFormData) {
        val activeProfile = userProfileDao.getActiveProfile()
        if (activeProfile == null) {
            statusText = "Нет активного профиля"
            return
        }

        val updatedProfile = activeProfile.copy(
            dateOfBirth = activeProfile.dateOfBirth,
            weightKg = formData.weightKg,
            heightCm = formData.heightCm
        )

        userProfileDao.updateProfile(updatedProfile)

        val recalculatedDays = healthSyncActions.recalculateCaloriesForProfile(
            profileId = activeProfile.id,
            ageYears = calculateAgeYears(updatedProfile.dateOfBirth),
            weightKg = updatedProfile.weightKg,
            heightCm = updatedProfile.heightCm
        )

        loadProfilesAndActiveProfile()
        reloadDataFromRoom()

        if (!isDemoMode) {
            onBackupRealProfileToCloud()
        }

        showProfileParamsDialog = false
        statusText = if (recalculatedDays > 0) {
            "Параметры профиля сохранены. Калории пересчитаны для $recalculatedDays дней"
        } else {
            "Параметры профиля сохранены"
        }
    }

    suspend fun startMenstruationEditMode() {
        val profileId = activeProfileId
        if (profileId == null) {
            statusText = "Нет активного профиля"
            return
        }

        editingMenstruationDates = menstruationEditActions.loadExistingMenstruationDates(
            profileId = profileId
        )

        isMenstruationEditMode = true
        statusText = "Выберите фактические дни менструации до сегодняшнего дня включительно"
    }

    fun cancelMenstruationEditMode() {
        editingMenstruationDates = emptySet()
        isMenstruationEditMode = false
        statusText = ""
    }


    fun getCurrentPredictedMensesLength(): Int {
        val timing = buildCycleTimingPrediction(
            allCycles = allCycles,
            dbCycles = dbCyclesForActiveProfile,
            fallbackCycleLength = activeProfileAvgCycleLength ?: 28,
            fallbackMensesLength = activeProfileAvgMensesLength ?: 5
        )

        return timing.predictedMensesLength.coerceIn(1, 12)
    }


    fun toggleMenstruationDate(date: LocalDate) {
        val predictedMensesLength = getCurrentPredictedMensesLength()

        val toggleResult = menstruationEditActions.toggleMenstruationDate(
            date = date,
            editingMenstruationDates = editingMenstruationDates,
            predictedMensesLength = predictedMensesLength
        )

        editingMenstruationDates = toggleResult.dates

        toggleResult.statusText?.let { message ->
            statusText = message
        }
    }

    suspend fun saveMenstruationDates() {
        val profileId = activeProfileId
        if (profileId == null) {
            statusText = "Нет активного профиля"
            return
        }

        menstruationEditActions.saveMenstruationDates(
            profileId = profileId,
            editingMenstruationDates = editingMenstruationDates
        )

        calendarDataService.rebuildCyclesAfterMenstruationChanges(profileId)
        calendarDataService.ensureCalendarGeneratedUpToToday(profileId)

        reloadDataFromRoom()

        editingMenstruationDates = emptySet()
        isMenstruationEditMode = false

        if (!isDemoMode) {
            syncRealProfileFromHealthConnectAndReload()
        } else {
            statusText = "Даты менструации сохранены"
        }
    }





    DisposableEffect(Unit) {
        onDispose {
            predictionService.close()
        }
    }

    LaunchedEffect(Unit) {
        ProfileInitializer.ensureDefaultProfiles(userProfileDao)

        val activeProfile = userProfileDao.getActiveProfile()
        if (activeProfile != null) {
            calendarDataService.ensureCalendarGeneratedUpToToday(activeProfile.id)
        }

        reloadDataFromRoom()
    }

    LaunchedEffect(Unit, healthPermissionRefreshKey) {
        refreshHealthConnectStatusForProfile()
    }

    LaunchedEffect(activeProfileId, isDemoMode, healthPermissionRefreshKey) {
        if (isDemoMode) return@LaunchedEffect

        val profileId = activeProfileId ?: return@LaunchedEffect
        val activeProfile = userProfileDao.getProfileById(profileId) ?: return@LaunchedEffect

        try {
            if (!healthConnectManager.isAvailable()) {
                return@LaunchedEffect
            }

            if (!healthConnectManager.hasAllPermissions()) {
                return@LaunchedEffect
            }

            val updatedDays = withContext(Dispatchers.IO) {
                healthSyncActions.syncHealthConnectToRoomForProfile(
                    profileId = profileId,
                    onlyOwnData = false,
                    excludeOwnData = true,
                    ageYears = calculateAgeYears(activeProfile.dateOfBirth),
                    weightKg = activeProfile.weightKg,
                    heightCm = activeProfile.heightCm
                )
            }

            if (updatedDays > 0) {
                reloadDataFromRoom()
                onBackupRealProfileToCloud()
                statusText = "Данные из Health Connect обновлены"
            }
        } catch (_: Exception) {
            statusText = "Не удалось обновить данные из Health Connect. Попробуйте позже."
        }
    }

    LaunchedEffect(Unit) {
        try {
            if (!healthConnectManager.isAvailable()) {
                return@LaunchedEffect
            }

            val hasPermissions = healthConnectManager.hasAllPermissions()
            if (!hasPermissions && !healthPermissionRequestStarted) {
                healthPermissionRequestStarted = true
                permissionsLauncher.launch(healthConnectManager.permissions)
            }
        } catch (_: Exception) {
            healthPermissionRequestStarted = false
            statusText = "Не удалось проверить Health Connect. Попробуйте позже."
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            MainBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    showSubscriptionScreen = false
                    if (selectedTab != tab) {
                        statusText = ""
                    }
                    selectedTab = tab
                }
            )
        }
    ) { innerPadding ->
        AdaptiveScreenContainer(
            modifier = Modifier.padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (showSubscriptionScreen) {
                    SubscriptionScreen(
                        isPremiumActive = isPremiumActive,
                        billingStatusText = billingStatusText,
                        onBuyPremium = onBuyPremium,
                        onRefreshSubscription = onRefreshSubscription,
                        onActivateTestPremium = {
                            onPremiumStatusChanged(true)
                            statusText = "Тестовый Premium активирован"
                            showSubscriptionScreen = false
                        },
                        onDisableTestPremium = {
                            onPremiumStatusChanged(false)
                            statusText = "Тестовый Premium отключён"
                            showSubscriptionScreen = false
                        },
                        onBack = {
                            showSubscriptionScreen = false
                        },
                        modifier = Modifier
                    )

                } else {

                    when (selectedTab) {
                        MainTab.CALENDAR -> {
                            CalendarTabContent(
                                currentMonth = currentMonth,
                                selectedDate = selectedDate,
                                dayDataByDate = dayDataByDate,
                                futureCycleMarks = futureCycleMarks,
                                minCalendarMonth = minCalendarMonth,
                                maxCalendarMonth = maxCalendarMonth,
                                cycleTimingHint = selectedDateCycleHint,
                                isCycleCompletedByDate = isCycleCompletedByDate,
                                selectedDateHeartRateSummary = selectedDateHeartRateSummary,
                                selectedDateHasEntry = selectedDateHasEntry,
                                statusText = statusText,
                                predictedCycleParams = predictedCycleParams,
                                result = result,
                                forecastLabels = forecastLabels,
                                predictionWarningMessage = predictionWarningMessage,
                                isPremiumActive = isPremiumActive,
                                delayDates = delayDates,
                                isForecastBlockedByDelay = isCurrentCycleDelayed,
                                onPrevMonth = {
                                    if (currentMonth > minCalendarMonth) {
                                        currentMonth = currentMonth.minusMonths(1)
                                    }
                                },
                                onNextMonth = {
                                    if (currentMonth < maxCalendarMonth) {
                                        currentMonth = currentMonth.plusMonths(1)
                                    }
                                },
                                onDateSelected = { clickedDate ->
                                    statusText = ""

                                    selectedDate = clickedDate
                                    selectedDateHasEntry = dayDataByDate.containsKey(clickedDate)

                                    selectedDateCycleHint = buildDelayHintForDate(
                                        date = clickedDate,
                                        dayData = dayDataByDate[clickedDate],
                                        delayDates = delayDates,
                                        allDelayDates = delayDates
                                    ) ?: if (clickedDate == LocalDate.now()) {
                                        dayDataByDate[clickedDate]?.let { selectedDay ->
                                            val timing = buildCycleTimingPrediction(
                                                allCycles = allCycles,
                                                dbCycles = dbCyclesForActiveProfile,
                                                fallbackCycleLength = activeProfileAvgCycleLength
                                                    ?: 28,
                                                fallbackMensesLength = activeProfileAvgMensesLength
                                                    ?: 5
                                            )

                                            buildCycleTimingHint(
                                                currentDayInCycle = selectedDay.dayInCycle,
                                                timingPrediction = timing
                                            )
                                        }
                                    } else {
                                        null
                                    }

                                    // При выборе даты только меняем выбранный день.
                                    // Прогноз не строим автоматически, чтобы не падать на будущих днях без записи в dayDataByDate.
                                    result = null
                                    predictedCycleParams = null
                                    forecastLabels = emptyList()
                                    predictionWarningMessage = null

                                    scope.launch {
                                        val profileId = activeProfileId
                                        selectedDateHeartRateSummary = if (profileId != null) {
                                            heartRateIntervalRepository.getDaySummary(
                                                profileId = profileId,
                                                date = clickedDate.toString()
                                            )
                                        } else {
                                            null
                                        }
                                    }
                                },
                                onBuildPredictionClick = {
                                    statusText = ""
                                    scope.launch {
                                        try {
                                            if (!isPremiumActive) {
                                                statusText = "Прогноз доступен в Premium"
                                                showSubscriptionScreen = true
                                                return@launch
                                            }

                                            val today = LocalDate.now()

                                            val predictionDate = when {
                                                selectedDate.isAfter(today) -> {
                                                    dayDataByDate.keys
                                                        .filter { knownDate ->
                                                            !knownDate.isAfter(
                                                                today
                                                            )
                                                        }
                                                        .maxOrNull()
                                                        ?: today
                                                }

                                                isDemoMode -> {
                                                    selectedDate
                                                }

                                                else -> {
                                                    today
                                                }
                                            }

                                            val timingPrediction = buildCycleTimingPrediction(
                                                allCycles = allCycles,
                                                dbCycles = dbCyclesForActiveProfile,
                                                fallbackCycleLength = activeProfileAvgCycleLength
                                                    ?: 28,
                                                fallbackMensesLength = activeProfileAvgMensesLength
                                                    ?: 5
                                            )

                                            val predictionBuildResult =
                                                buildMainNext3DaysPrediction(
                                                    predictionService = predictionService,
                                                    selectedDate = predictionDate,
                                                    allCycles = allCycles,
                                                    dayDataByDate = dayDataByDate,
                                                    timingPrediction = timingPrediction
                                                )

                                            predictedCycleParams = predictionBuildResult.cycleParams
                                            result = predictionBuildResult.prediction
                                            forecastLabels = predictionBuildResult.forecastLabels
                                            predictionWarningMessage =
                                                predictionBuildResult.warningMessage

                                            phaseAnalytics = null
                                            cycleTrendAnalytics = null
                                            heartRateAnalytics = null
                                            activitySleepAnalytics = null
                                            relationshipAnalytics = null

                                            statusText = if (selectedDate.isAfter(today)) {
                                                "Прогноз построен для ближайших дней"
                                            } else if (isDemoMode) {
                                                "Прогноз построен от даты $predictionDate"
                                            } else {
                                                "Прогноз состояния построен на ближайшие дни"
                                            }
                                        } catch (e: Exception) {
                                            result = null
                                            predictedCycleParams = null
                                            forecastLabels = emptyList()
                                            predictionWarningMessage = null
                                            statusText = "Ошибка прогноза: ${e.message}"
                                        }
                                    }
                                },

                                isMenstruationEditMode = isMenstruationEditMode,
                                editingMenstruationDates = editingMenstruationDates,
                                onStartMenstruationEdit = {
                                    statusText = ""

                                    scope.launch {
                                        startMenstruationEditMode()
                                    }
                                },
                                onCancelMenstruationEdit = {
                                    statusText = ""
                                    cancelMenstruationEditMode()
                                },
                                onSaveMenstruationEdit = {
                                    statusText = ""
                                    scope.launch {
                                        saveMenstruationDates()
                                    }
                                },
                                onToggleMenstruationDate = { date ->
                                    toggleMenstruationDate(date)
                                },
                                onEditDayClick = {
                                    statusText = ""

                                    scope.launch {
                                        try {
                                            openEditDialogForSelectedDate()
                                        } catch (e: Exception) {
                                            statusText = "Ошибка открытия редактирования: ${e.message}"
                                        }
                                    }
                                }
                            )
                        }

                        MainTab.ANALYTICS -> {
                            AnalyticsTabContent(
                                statusText = statusText,
                                cycleLengthAnalytics = cycleLengthAnalytics,
                                phaseAnalytics = phaseAnalytics,
                                cycleTrendAnalytics = cycleTrendAnalytics,
                                heartRateAnalytics = heartRateAnalytics,
                                activitySleepAnalytics = activitySleepAnalytics,
                                relationshipAnalytics = relationshipAnalytics,

                                onBuildPsychophysiologyAnalytics = {
                                    statusText = ""

                                    // Сразу переключаем экран в режим аналитики самочувствия.
                                    // Поэтому старую физиологическую аналитику и взаимосвязи убираем до построения.
                                    phaseAnalytics = null
                                    cycleTrendAnalytics = null
                                    activitySleepAnalytics = null
                                    heartRateAnalytics = null
                                    relationshipAnalytics = null
                                    result = null

                                    scope.launch {
                                        try {
                                            if (!isPremiumActive) {
                                                statusText = "Расширенная аналитика доступна в Premium"
                                                showSubscriptionScreen = true
                                                return@launch
                                            }

                                            val analytics = buildMainPsychophysiologyAnalytics(
                                                allCycles = allCycles,
                                                phaseAnalyticsService = phaseAnalyticsService,
                                                cycleTrendAnalyticsService = cycleTrendAnalyticsService
                                            )

                                            phaseAnalytics = analytics.phaseAnalytics
                                            cycleTrendAnalytics = analytics.cycleTrendAnalytics

                                            // Успешное системное сообщение не показываем в карточках.
                                            statusText = ""
                                        } catch (e: Exception) {
                                            phaseAnalytics = null
                                            cycleTrendAnalytics = null
                                            activitySleepAnalytics = null
                                            heartRateAnalytics = null
                                            relationshipAnalytics = null
                                            result = null

                                            statusText = when {
                                                e.message?.contains("заверш", ignoreCase = true) == true -> {
                                                    "Для аналитики самочувствия нужен хотя бы один завершённый цикл. Отметьте дни менструации, чтобы приложение могло завершить предыдущий цикл."
                                                }

                                                e.message?.contains("фазовой аналитики", ignoreCase = true) == true -> {
                                                    "Недостаточно данных самочувствия для построения фазовой аналитики. Заполните самочувствие в календаре за несколько дней завершённого цикла."
                                                }

                                                else -> {
                                                    e.message ?: "Недостаточно данных самочувствия для построения аналитики."
                                                }
                                            }
                                        }
                                    }
                                },

                                onBuildPhysiologyAnalytics = {
                                    statusText = ""

                                    // Сразу переключаем экран в режим физиологической аналитики.
                                    // Поэтому старую аналитику самочувствия и взаимосвязи убираем до построения.
                                    phaseAnalytics = null
                                    cycleTrendAnalytics = null
                                    activitySleepAnalytics = null
                                    heartRateAnalytics = null
                                    relationshipAnalytics = null
                                    result = null

                                    scope.launch {
                                        try {
                                            if (!isPremiumActive) {
                                                statusText = "Расширенная аналитика доступна в Premium"
                                                showSubscriptionScreen = true
                                                return@launch
                                            }

                                            val analytics = buildMainPhysiologyAnalytics(
                                                profileId = activeProfileId,
                                                allCycles = allCycles,
                                                roomRepository = roomRepository,
                                                heartRateIntervalRepository = heartRateIntervalRepository,
                                                activitySleepAnalyticsService = activitySleepAnalyticsService,
                                                heartRateAnalyticsService = heartRateAnalyticsService
                                            )

                                            activitySleepAnalytics = analytics.activitySleepAnalytics
                                            heartRateAnalytics = analytics.heartRateAnalytics

                                            // Успешное системное сообщение не показываем в карточках.
                                            statusText = ""
                                        } catch (e: Exception) {
                                            phaseAnalytics = null
                                            cycleTrendAnalytics = null
                                            activitySleepAnalytics = null
                                            heartRateAnalytics = null
                                            relationshipAnalytics = null
                                            result = null

                                            statusText = e.message
                                                ?: "Не удалось построить физиологическую аналитику. Проверьте, есть ли данные о шагах, сне или пульсе."
                                        }
                                    }
                                },

                                onOpenRelationshipAnalytics = {
                                    statusText = ""
                                    phaseAnalytics = null
                                    cycleTrendAnalytics = null
                                    activitySleepAnalytics = null
                                    heartRateAnalytics = null
                                },

                                onBuildRelationshipAnalytics = { selectedMetrics ->
                                    statusText = ""
                                    scope.launch {
                                        try {
                                            if (!isPremiumActive) {
                                                statusText =
                                                    "Расширенная аналитика доступна в Premium"
                                                showSubscriptionScreen = true
                                                return@launch
                                            }

                                            val analytics = buildMainRelationshipAnalytics(
                                                allCycles = allCycles,
                                                selectedMetrics = selectedMetrics,
                                                relationshipAnalyticsService = relationshipAnalyticsService
                                            )

                                            relationshipAnalytics = analytics

                                            phaseAnalytics = null
                                            cycleTrendAnalytics = null
                                            heartRateAnalytics = null
                                            activitySleepAnalytics = null
                                            result = null

                                            statusText = "Анализ взаимосвязей построен"
                                        } catch (e: Exception) {
                                            statusText = "Ошибка анализа взаимосвязей: ${e.message}"
                                        }
                                    }
                                },

                                modifier = Modifier
                            )
                        }

                        MainTab.PROFILE -> {
                            ProfileTabContent(
                                isDemoMode = isDemoMode,
                                googleUserEmail = googleUserEmail,
                                googleUserDisplayName = googleUserDisplayName,
                                isPremiumActive = isPremiumActive,
                                isAccountDeletionRunning = isAccountDeletionRunning,
                                onOpenSubscription = {
                                    statusText = ""
                                    showSubscriptionScreen = true
                                },
                                onConnectHealthConnect = {
                                    statusText = ""
                                    scope.launch {
                                        try {
                                            when (
                                                resolveMainHealthConnectConnectAction(
                                                    healthConnectManager = healthConnectManager
                                                )
                                            ) {
                                                MainHealthConnectConnectAction.OPEN_SETTINGS -> {
                                                    healthConnectManager.openHealthConnectSettings()
                                                }

                                                MainHealthConnectConnectAction.REQUEST_PERMISSIONS -> {
                                                    permissionsLauncher.launch(healthConnectManager.permissions)
                                                }

                                                MainHealthConnectConnectAction.OPEN_STORE -> {
                                                    healthConnectManager.openHealthConnectInStore()
                                                }

                                                MainHealthConnectConnectAction.SHOW_UNAVAILABLE -> {
                                                    statusText =
                                                        "Health Connect недоступен на этом устройстве"
                                                }
                                            }

                                            refreshHealthConnectStatusForProfile()
                                        } catch (e: Exception) {
                                            statusText =
                                                "Ошибка подключения Health Connect: ${e.message}"
                                        }
                                    }
                                },
                                healthConnectStatusText = healthConnectStatusText,
                                isHealthConnectConnected = isHealthConnectConnected,
                                activeProfileName = activeProfileName,
                                activeProfileDateOfBirth = activeProfileDateOfBirth,
                                activeProfileAgeYears = activeProfileAgeYears,
                                activeProfileWeightKg = activeProfileWeightKg,
                                activeProfileHeightCm = activeProfileHeightCm,
                                profiles = profiles,
                                profileMenuExpanded = profileMenuExpanded,
                                onProfileMenuExpandedChange = { profileMenuExpanded = it },
                                onProfileSelected = { profile ->
                                    statusText = ""
                                    scope.launch {
                                        try {
                                            activateProfile(profile)
                                        } catch (e: Exception) {
                                            statusText = "Ошибка выбора профиля: ${e.message}"
                                        }
                                    }
                                },
                                onOpenProfileParams = {
                                    statusText = ""
                                    showProfileParamsDialog = true
                                },
                                onSignOut = {
                                    statusText = ""
                                    onSignOut()
                                },
                                onDeleteAccountAndAllData = {
                                    statusText = ""
                                    scope.launch {
                                        try {
                                            isAccountDeletionRunning = true
                                            statusText = "Удаление аккаунта..."

                                            // Выполнение функции удаления
                                            onDeleteAccountAndAllData()

                                            // Сообщение об успешном удалении
                                            statusText = "Аккаунт и данные удалены"

                                            // Возврат на начальный экран
                                            onBackToEntry()
                                        } catch (e: Exception) {
                                            statusText = "Не удалось удалить аккаунт. Попробуйте позже."
                                        } finally {
                                            isAccountDeletionRunning = false
                                        }
                                    }
                                },
                                onBackToEntry = onBackToEntry,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            )
                        }
                    }
                }

            }
        }
    }

    if (showEditDayDialog) {
        val day = editingDayEntity

        EditDayDataDialog(
            initialIsMenstruationMarked = day?.isMenstruationMarked ?: false,
            initialStressScore = day?.stressScore,
            initialEnergyLevel = day?.energyLevel,
            initialMoodScore = day?.moodScore,
            initialConcentrationScore = day?.concentrationScore,
            initialPainLevel = day?.painLevel,
            initialNotes = day?.notes,
            onDismiss = {
                showEditDayDialog = false
                editingDayEntity = null
                isCreatingNewDay = false
            },
            onSave = { formData ->
                scope.launch {
                    try {
                        saveEditedDay(formData)
                    } catch (e: Exception) {
                        statusText = "Не удалось сохранить данные. Попробуйте позже."
                    }
                }
            }
        )
    }
    if (showProfileParamsDialog) {
        ProfileParamsDialog(
            initialDateOfBirth = activeProfileDateOfBirth,
            initialWeightKg = activeProfileWeightKg,
            initialHeightCm = activeProfileHeightCm,
            onDismiss = {
                showProfileParamsDialog = false
            },
            onSave = { formData ->
                showProfileParamsDialog = false

                scope.launch {
                    try {
                        saveProfileParams(formData)
                    } catch (e: Exception) {
                        statusText = "Ошибка сохранения параметров профиля: ${e.message}"
                    }
                }
            }
        )
    }
}






