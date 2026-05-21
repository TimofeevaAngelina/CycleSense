package com.timofeeva.cyclesense

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.timofeeva.cyclesense.auth.FirebaseUserInfo
import com.timofeeva.cyclesense.auth.GoogleAuthManager
import com.timofeeva.cyclesense.cloud.CloudBackupRepository
import com.timofeeva.cyclesense.data.local.DatabaseProvider
import com.timofeeva.cyclesense.data.local.entity.CycleDayEntity
import com.timofeeva.cyclesense.data.local.entity.CycleEntity
import com.timofeeva.cyclesense.data.local.entity.UserProfileEntity
import com.timofeeva.cyclesense.data.repository.CycleRepositoryRoom
import com.timofeeva.cyclesense.data.repository.HeartRateIntervalRepository
import com.timofeeva.cyclesense.data.repository.ProfileInitializer
import com.timofeeva.cyclesense.health.HealthConnectManager
import com.timofeeva.cyclesense.premium.BillingManager
import com.timofeeva.cyclesense.premium.PremiumAccessRepository
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.runtime.mutableIntStateOf

private enum class AppScreen {
    LOADING,
    ENTRY,
    REAL_MAIN,
    DEMO_MAIN
}

private enum class OnboardingStartMode {
    CREATE_ACCOUNT,
    SIGN_IN
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val roomDb = remember(context) {
        DatabaseProvider.getDatabase(context)
    }

    val userProfileDao = remember(roomDb) {
        roomDb.userProfileDao()
    }

    val roomRepository = remember(roomDb) {
        CycleRepositoryRoom(
            cycleDao = roomDb.cycleDao(),
            cycleDayDao = roomDb.cycleDayDao()
        )
    }

    val heartRateIntervalRepository = remember(roomDb) {
        HeartRateIntervalRepository(roomDb.heartRateIntervalDao())
    }

    val cloudBackupRepository = remember(roomRepository, heartRateIntervalRepository) {
        CloudBackupRepository(
            roomRepository = roomRepository,
            heartRateIntervalRepository = heartRateIntervalRepository
        )
    }

    val googleAuthManager = remember(context) {
        GoogleAuthManager(context)
    }

    val premiumAccessRepository = remember(context) {
        PremiumAccessRepository(context)
    }

    val healthConnectManager = remember(context) {
        HealthConnectManager(context)
    }

    var healthConnectAvailability by remember {
        mutableStateOf(healthConnectManager.getAvailabilityStatus())
    }

    var isHealthConnectConnected by remember {
        mutableStateOf(false)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, healthConnectManager) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                healthConnectAvailability = healthConnectManager.getAvailabilityStatus()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var isPremiumActive by remember {
        mutableStateOf(premiumAccessRepository.isPremiumActive())
    }

    fun updatePremiumStatus(isActive: Boolean) {
        premiumAccessRepository.setPremiumActive(isActive)
        isPremiumActive = isActive
    }

    var billingStatusText by remember {
        mutableStateOf("Billing ещё не подключён")
    }

    val billingManager = remember(context, premiumAccessRepository) {
        BillingManager(
            context = context,
            premiumAccessRepository = premiumAccessRepository,
            onPremiumChanged = { isActive ->
                updatePremiumStatus(isActive)
            },
            onStatusChanged = { status ->
                billingStatusText = status
            }
        )
    }

    LaunchedEffect(Unit) {
        billingManager.startConnection()
    }

    DisposableEffect(Unit) {
        onDispose {
            billingManager.endConnection()
        }
    }

    val initialFirebaseUser = googleAuthManager.currentUser()

    var currentScreen by remember {
        mutableStateOf(AppScreen.LOADING)
    }

    var authStatusText by remember {
        mutableStateOf("")
    }

    val healthPermissionsLauncher = rememberLauncherForActivityResult(
        contract = healthConnectManager.createPermissionsLauncherContract()
    ) { granted ->
        healthConnectAvailability = healthConnectManager.getAvailabilityStatus()
        isHealthConnectConnected = granted.containsAll(healthConnectManager.permissions)

        authStatusText = if (isHealthConnectConnected) {
            "Разрешения Health Connect выданы"
        } else {
            "Health Connect можно подключить позже в профиле"
        }
    }

    var googleUserEmail by remember {
        mutableStateOf(initialFirebaseUser?.email)
    }

    var googleUserDisplayName by remember {
        mutableStateOf(initialFirebaseUser?.displayName)
    }

    var showOnboarding by remember {
        mutableStateOf(false)
    }

    var onboardingStartMode by remember {
        mutableStateOf(OnboardingStartMode.CREATE_ACCOUNT)
    }

    var onboardingAccountStatusText by remember {
        mutableStateOf("")
    }

    var showOnboardingSubscription by remember {
        mutableStateOf(false)
    }

    var onboardingProfileId by remember {
        mutableStateOf<Long?>(null)
    }

    var onboardingInitialStep by remember {
        mutableIntStateOf(0)
    }

    var onboardingInitialDateOfBirth by remember {
        mutableStateOf<String?>(null)
    }



    suspend fun findOrCreateRealProfileForGoogleUser(
        user: FirebaseUserInfo
    ): UserProfileEntity {
        ProfileInitializer.ensureDefaultProfiles(userProfileDao)

        val existingProfile = userProfileDao.getRealProfileByFirebaseUid(user.uid)
        if (existingProfile != null) {
            val updatedProfile = existingProfile.copy(
                googleEmail = user.email,
                googleDisplayName = user.displayName
            )
            userProfileDao.updateProfile(updatedProfile)
            return updatedProfile
        }

        val displayName = user.displayName
            ?.takeIf { it.isNotBlank() }
            ?: user.email
                ?.substringBefore("@")
                ?.takeIf { it.isNotBlank() }
            ?: "Google user"

        val newProfile = UserProfileEntity(
            name = "Мой профиль: $displayName",
            profileType = "REAL",
            isDemo = false,
            seed = null,
            isActive = false,
            firebaseUid = user.uid,
            googleEmail = user.email,
            googleDisplayName = user.displayName,
            dateOfBirth = null,
            weightKg = null,
            heightCm = null
        )

        val newId = userProfileDao.insertProfile(newProfile)
        return newProfile.copy(id = newId)
    }

    suspend fun openRealProfile() {
        authStatusText = "Выполняется вход через Google..."

        val currentFirebaseUser = googleAuthManager.currentUser()

        val authResult = if (currentFirebaseUser != null) {
            Result.success(
                FirebaseUserInfo(
                    uid = currentFirebaseUser.uid,
                    email = currentFirebaseUser.email,
                    displayName = currentFirebaseUser.displayName,
                    photoUrl = currentFirebaseUser.photoUrl?.toString()
                )
            )
        } else {
            googleAuthManager.signInWithGoogle()
        }

        authResult
            .onSuccess { user ->
                googleUserEmail = user.email
                googleUserDisplayName = user.displayName

                val realProfile = findOrCreateRealProfileForGoogleUser(user)

                val restoredProfileMeta = cloudBackupRepository.restoreProfileMetaFromCloud(
                    firebaseUid = user.uid,
                    localProfile = realProfile
                )

                userProfileDao.updateProfile(restoredProfileMeta)
                userProfileDao.activateProfile(restoredProfileMeta.id)

                val restoredFromCloud = cloudBackupRepository.restoreProfileFromCloudIfLocalEmpty(
                    firebaseUid = user.uid,
                    profile = restoredProfileMeta
                )

                val freshProfile = userProfileDao.getProfileById(restoredProfileMeta.id)

                if (freshProfile != null && !freshProfile.onboardingCompleted) {
                    onboardingProfileId = freshProfile.id
                    showOnboarding = true
                    authStatusText = "Вход выполнен"
                    currentScreen = AppScreen.REAL_MAIN
                    return@onSuccess
                }

                showOnboarding = false
                onboardingProfileId = null
                currentScreen = AppScreen.REAL_MAIN

                if (restoredFromCloud) {
                    authStatusText = "Данные восстановлены из облака"
                } else {
                    cloudBackupRepository.backupProfileToCloud(
                        firebaseUid = user.uid,
                        profile = restoredProfileMeta
                    )

                    authStatusText = "Вход выполнен"
                }
            }
            .onFailure { error ->
                authStatusText = "Ошибка входа через Google: ${error.message ?: "неизвестная ошибка"}"
            }
    }

    suspend fun startCreateAccountOnboarding() {
        googleAuthManager.signOut()

        googleUserEmail = null
        googleUserDisplayName = null
        onboardingProfileId = null
        onboardingInitialStep = 0
        onboardingInitialDateOfBirth = null
        onboardingStartMode = OnboardingStartMode.CREATE_ACCOUNT
        onboardingAccountStatusText = ""
        showOnboarding = true
        authStatusText = ""
        currentScreen = AppScreen.REAL_MAIN
    }



    suspend fun continueWithGoogleFromOnboarding(
        mode: OnboardingStartMode = onboardingStartMode,
        draftDateOfBirth: String? = onboardingInitialDateOfBirth
    ) {
        onboardingStartMode = mode

        onboardingInitialDateOfBirth = draftDateOfBirth

        onboardingAccountStatusText = if (mode == OnboardingStartMode.CREATE_ACCOUNT) {
            "Открывается Google-вход для создания аккаунта..."
        } else {
            "Открывается Google-вход..."
        }

        val authResult = googleAuthManager.signInWithGoogle()

        authResult
            .onSuccess { user ->
                googleUserEmail = user.email
                googleUserDisplayName = user.displayName

                val localExistingProfile = userProfileDao.getRealProfileByFirebaseUid(user.uid)
                val cloudProfileExists = cloudBackupRepository.cloudProfileExists(user.uid)
                val accountExists = localExistingProfile != null || cloudProfileExists

                when (mode) {
                    OnboardingStartMode.CREATE_ACCOUNT -> {
                        if (accountExists) {
                            googleAuthManager.signOut()

                            googleUserEmail = null
                            googleUserDisplayName = null
                            onboardingProfileId = null

                            onboardingAccountStatusText =
                                "Аккаунт CycleSense для выбранного Google-аккаунта уже создан. Вы можете войти в существующий аккаунт или выбрать другой Google-аккаунт."
                            return@onSuccess
                        }

                        val realProfile = findOrCreateRealProfileForGoogleUser(user)

                        val profileWithDraftBirthDate = if (!draftDateOfBirth.isNullOrBlank()) {
                            realProfile.copy(dateOfBirth = draftDateOfBirth)
                        } else {
                            realProfile
                        }

                        userProfileDao.updateProfile(profileWithDraftBirthDate)
                        userProfileDao.activateProfile(profileWithDraftBirthDate.id)

                        onboardingProfileId = profileWithDraftBirthDate.id
                        onboardingInitialStep = 5
                        onboardingInitialDateOfBirth = profileWithDraftBirthDate.dateOfBirth
                        onboardingAccountStatusText =
                            "Google-аккаунт подключён. Теперь можно продолжить создание профиля."

                        authStatusText = "Google-аккаунт подключён"
                    }

                    OnboardingStartMode.SIGN_IN -> {
                        if (!accountExists) {
                            googleAuthManager.signOut()

                            googleUserEmail = null
                            googleUserDisplayName = null
                            onboardingProfileId = null

                            onboardingAccountStatusText =
                                "Для выбранного Google-аккаунта профиль CycleSense не найден. Вы можете создать новый аккаунт."
                            return@onSuccess
                        }

                        val realProfile = localExistingProfile
                            ?: findOrCreateRealProfileForGoogleUser(user)

                        val restoredProfileMeta = cloudBackupRepository.restoreProfileMetaFromCloud(
                            firebaseUid = user.uid,
                            localProfile = realProfile
                        )

                        userProfileDao.updateProfile(restoredProfileMeta)
                        userProfileDao.activateProfile(restoredProfileMeta.id)

                        val restoredFromCloud = cloudBackupRepository.restoreProfileFromCloudIfLocalEmpty(
                            firebaseUid = user.uid,
                            profile = restoredProfileMeta
                        )

                        showOnboarding = false
                        onboardingProfileId = null
                        onboardingAccountStatusText = ""
                        currentScreen = AppScreen.REAL_MAIN

                        authStatusText = if (restoredFromCloud) {
                            "Данные восстановлены из облака"
                        } else {
                            user.email?.let {
                                "Вход выполнен: $it"
                            } ?: "Вход выполнен"
                        }
                    }
                }
            }
            .onFailure { error ->
                onboardingAccountStatusText =
                    "Ошибка Google-входа: ${error.message ?: "неизвестная ошибка"}"
            }
    }

    suspend fun signInExistingAccountFromEntry() {
        googleAuthManager.signOut()

        googleUserEmail = null
        googleUserDisplayName = null
        onboardingProfileId = null
        onboardingAccountStatusText = ""
        showOnboarding = false
        authStatusText = "Открывается Google-вход..."

        val authResult = googleAuthManager.signInWithGoogle()

        authResult
            .onSuccess { user ->
                googleUserEmail = user.email
                googleUserDisplayName = user.displayName

                val localExistingProfile = userProfileDao.getRealProfileByFirebaseUid(user.uid)
                val cloudProfileExists = cloudBackupRepository.cloudProfileExists(user.uid)
                val accountExists = localExistingProfile != null || cloudProfileExists

                if (!accountExists) {
                    googleAuthManager.signOut()

                    googleUserEmail = null
                    googleUserDisplayName = null
                    onboardingProfileId = null
                    showOnboarding = false
                    currentScreen = AppScreen.ENTRY

                    authStatusText =
                        "Для выбранного Google-аккаунта профиль CycleSense не найден. Нажмите «Создать аккаунт», чтобы зарегистрироваться."
                    return@onSuccess
                }

                val realProfile = localExistingProfile
                    ?: findOrCreateRealProfileForGoogleUser(user)

                val restoredProfileMeta = cloudBackupRepository.restoreProfileMetaFromCloud(
                    firebaseUid = user.uid,
                    localProfile = realProfile
                )

                userProfileDao.updateProfile(restoredProfileMeta)
                userProfileDao.activateProfile(restoredProfileMeta.id)

                val restoredFromCloud = cloudBackupRepository.restoreProfileFromCloudIfLocalEmpty(
                    firebaseUid = user.uid,
                    profile = restoredProfileMeta
                )

                showOnboarding = false
                onboardingProfileId = null
                onboardingAccountStatusText = ""
                currentScreen = AppScreen.REAL_MAIN

                authStatusText = if (restoredFromCloud) {
                    "Данные восстановлены из облака"
                } else {
                    user.email?.let {
                        "Вход выполнен: $it"
                    } ?: "Вход выполнен"
                }
            }
            .onFailure { error ->
                googleUserEmail = null
                googleUserDisplayName = null
                onboardingProfileId = null
                showOnboarding = false
                currentScreen = AppScreen.ENTRY

                authStatusText =
                    "Ошибка входа через Google: ${error.message ?: "неизвестная ошибка"}"
            }
    }

    suspend fun startSignInOnboarding() {
        googleAuthManager.signOut()

        googleUserEmail = null
        googleUserDisplayName = null
        onboardingProfileId = null
        onboardingInitialStep = 4
        onboardingInitialDateOfBirth = null
        onboardingStartMode = OnboardingStartMode.SIGN_IN
        onboardingAccountStatusText = ""
        showOnboarding = true
        authStatusText = ""
        currentScreen = AppScreen.REAL_MAIN

        continueWithGoogleFromOnboarding(
            mode = OnboardingStartMode.SIGN_IN,
            draftDateOfBirth = null
        )
    }

    suspend fun openDemoMode() {
        ProfileInitializer.ensureDefaultProfiles(userProfileDao)

        val profiles = userProfileDao.getAllProfiles()
        val demoProfile = profiles.firstOrNull { it.isDemo && it.profileType == "CLASSIC" }
            ?: profiles.firstOrNull { it.isDemo }

        if (demoProfile != null) {
            userProfileDao.activateProfile(demoProfile.id)
        }

        showOnboarding = false
        onboardingProfileId = null
        currentScreen = AppScreen.DEMO_MAIN
    }

    suspend fun signOutFromGoogle() {
        googleAuthManager.signOut()
        googleUserEmail = null
        googleUserDisplayName = null
        showOnboarding = false
        onboardingProfileId = null
        authStatusText = "Вы вышли из Google-аккаунта"
        currentScreen = AppScreen.ENTRY
    }

    suspend fun deleteAccountAndAllData() {
        val currentFirebaseUser = googleAuthManager.currentUser()
        val activeProfile = userProfileDao.getActiveProfile()

        if (currentFirebaseUser == null || activeProfile == null) {
            authStatusText = "Не удалось удалить аккаунт: пользователь не найден"
            return
        }

        if (activeProfile.isDemo) {
            authStatusText = "Demo-профиль нельзя удалить как аккаунт"
            return
        }

        val uid = currentFirebaseUser.uid

        authStatusText = "Удаление аккаунта и данных..."

        try {
            // 1. Удаляем облачные данные CycleSense
            cloudBackupRepository.deleteProfileFromCloud(uid)

            // 2. Удаляем локальные данные активного профиля
            heartRateIntervalRepository.clearIntervalsForProfile(activeProfile.id)
            roomRepository.clearProfileData(activeProfile.id)
            userProfileDao.deleteProfileById(activeProfile.id)

            // 3. Удаляем Firebase Auth аккаунт
            val deleteResult = googleAuthManager.deleteCurrentUser()

            deleteResult
                .onSuccess {
                    googleUserEmail = null
                    googleUserDisplayName = null
                    showOnboarding = false
                    onboardingProfileId = null
                    authStatusText = "Аккаунт и данные удалены"
                    currentScreen = AppScreen.ENTRY
                }
                .onFailure { error ->
                    googleUserEmail = null
                    googleUserDisplayName = null
                    showOnboarding = false
                    onboardingProfileId = null
                    currentScreen = AppScreen.ENTRY

                    authStatusText = if (error is FirebaseAuthRecentLoginRequiredException) {
                        "Данные CycleSense удалены. Для удаления Google-входа нужно заново войти через Google и повторить удаление аккаунта."
                    } else {
                        "Данные CycleSense удалены, но удалить аккаунт входа не удалось. Попробуйте войти снова и повторить удаление."
                    }
                }
        } catch (_: Exception) {
            authStatusText = "Не удалось удалить аккаунт и данные. Попробуйте позже."
        }
    }

    fun inferInitialPhase(
        dayInCycle: Int,
        avgMensesLength: Int,
        predictedOvulationDay: Int
    ): String {
        return when {
            dayInCycle <= avgMensesLength -> "menstrual"
            dayInCycle < predictedOvulationDay - 1 -> "follicular"
            dayInCycle in (predictedOvulationDay - 1)..(predictedOvulationDay + 1) -> "ovulatory"
            else -> "luteal"
        }
    }

    suspend fun createInitialCycleFromOnboarding(
        profileId: Long,
        lastPeriodStartDate: String,
        avgCycleLength: Int,
        avgMensesLength: Int
    ) {
        val startDate = try {
            LocalDate.parse(lastPeriodStartDate)
        } catch (_: Exception) {
            LocalDate.now()
        }

        val today = LocalDate.now()

        val safeStartDate = if (startDate.isAfter(today)) {
            today
        } else {
            startDate
        }

        val existingCycles = roomRepository.getCyclesForProfile(profileId)
        val existingDays = roomRepository.getDaysForProfile(profileId)

        if (existingCycles.isNotEmpty() || existingDays.isNotEmpty()) {
            return
        }

        val daysFromStart = ChronoUnit.DAYS.between(safeStartDate, today).toInt()
        val predictedOvulationDay = (avgCycleLength - 14).coerceAtLeast(10)

        val cycleId = roomRepository.insertCycle(
            CycleEntity(
                userProfileId = profileId,
                startDate = safeStartDate.toString(),
                endDate = null,
                cycleLength = null,
                predictedCycleLength = avgCycleLength,
                predictedMensesLength = avgMensesLength,
                predictedOvulationDay = predictedOvulationDay,
                isCompleted = false
            )
        )

        for (offset in 0..daysFromStart) {
            val date = safeStartDate.plusDays(offset.toLong())
            val dayInCycle = offset + 1

            val phase = inferInitialPhase(
                dayInCycle = dayInCycle,
                avgMensesLength = avgMensesLength,
                predictedOvulationDay = predictedOvulationDay
            )

            val isMenstruationMarked = dayInCycle <= avgMensesLength

            val day = CycleDayEntity(
                cycleId = cycleId,
                date = date.toString(),
                dayInCycle = dayInCycle,
                phase = phase,

                steps = null,
                calories = null,
                heartRate = null,
                rmssd = null,
                sleepHours = null,

                stressScore = null,
                energyLevel = null,
                moodScore = null,
                concentrationScore = null,
                painLevel = null,

                isMenstruationMarked = isMenstruationMarked,
                notes = null
            )

            roomRepository.insertDay(day)
        }
    }

    suspend fun finishOnboarding(
        profileId: Long,
        result: OnboardingResult
    ) {
        val profile = userProfileDao.getProfileById(profileId) ?: return

        val updatedProfile = profile.copy(
            name = result.name,
            dateOfBirth = result.dateOfBirth,
            weightKg = result.weightKg.takeIf { it > 0f },
            heightCm = result.heightCm.takeIf { it > 0f },
            onboardingCompleted = true,
            lastPeriodStartDate = result.lastPeriodStartDate,
            avgCycleLength = result.avgCycleLength,
            avgMensesLength = result.avgMensesLength,
            privacyAcceptedAt = result.privacyAcceptedAt,
            healthConnectSkippedAt = if (result.healthConnectSkipped) {
                LocalDate.now().toString()
            } else {
                null
            }
        )

        userProfileDao.updateProfile(updatedProfile)
        userProfileDao.activateProfile(profileId)

        createInitialCycleFromOnboarding(
            profileId = profileId,
            lastPeriodStartDate = result.lastPeriodStartDate,
            avgCycleLength = result.avgCycleLength,
            avgMensesLength = result.avgMensesLength
        )

        val currentFirebaseUser = googleAuthManager.currentUser()
        if (currentFirebaseUser != null) {
            cloudBackupRepository.backupProfileToCloud(
                firebaseUid = currentFirebaseUser.uid,
                profile = updatedProfile
            )
        }

        showOnboarding = false
        onboardingProfileId = null
        currentScreen = AppScreen.REAL_MAIN
    }

    fun openHealthConnectInStore() {
        val packageName = "com.google.android.apps.healthdata"

        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(marketIntent)
        } catch (_: Exception) {
            try {
                context.startActivity(browserIntent)
            } catch (_: Exception) {
                authStatusText = "Не удалось открыть страницу Health Connect"
            }
        }
    }

    suspend fun openExistingSessionIfPossible() {
        val currentFirebaseUser = googleAuthManager.currentUser()

        if (currentFirebaseUser == null) {
            currentScreen = AppScreen.ENTRY
            return
        }

        googleUserEmail = currentFirebaseUser.email
        googleUserDisplayName = currentFirebaseUser.displayName

        val user = FirebaseUserInfo(
            uid = currentFirebaseUser.uid,
            email = currentFirebaseUser.email,
            displayName = currentFirebaseUser.displayName,
            photoUrl = currentFirebaseUser.photoUrl?.toString()
        )

        val existingProfile = userProfileDao.getRealProfileByFirebaseUid(user.uid)

        if (existingProfile == null) {
            currentScreen = AppScreen.ENTRY
            authStatusText = "Войдите в аккаунт или создайте профиль"
            return
        }

        val restoredProfileMeta = cloudBackupRepository.restoreProfileMetaFromCloud(
            firebaseUid = user.uid,
            localProfile = existingProfile
        )

        userProfileDao.updateProfile(restoredProfileMeta)
        userProfileDao.activateProfile(restoredProfileMeta.id)

        cloudBackupRepository.restoreProfileFromCloudIfLocalEmpty(
            firebaseUid = user.uid,
            profile = restoredProfileMeta
        )

        val freshProfile = userProfileDao.getProfileById(restoredProfileMeta.id)
            ?: restoredProfileMeta

        if (!freshProfile.onboardingCompleted) {
            onboardingStartMode = OnboardingStartMode.CREATE_ACCOUNT
            onboardingProfileId = freshProfile.id
            onboardingInitialStep = 5
            onboardingInitialDateOfBirth = freshProfile.dateOfBirth
            showOnboarding = true
            onboardingAccountStatusText = "Google-аккаунт подключён. Продолжите создание профиля."
            currentScreen = AppScreen.REAL_MAIN
            authStatusText = "Продолжите настройку профиля"
            return
        }

        showOnboarding = false
        onboardingProfileId = null
        onboardingAccountStatusText = ""
        currentScreen = AppScreen.REAL_MAIN
        authStatusText = currentFirebaseUser.email?.let {
            "Вход выполнен: $it"
        } ?: "Вход выполнен"
    }

    LaunchedEffect(Unit) {
        openExistingSessionIfPossible()
    }

    Scaffold { innerPadding ->
        when (currentScreen) {
            AppScreen.LOADING -> {
                LoadingScreen(
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppScreen.ENTRY -> {
                EntryScreen(
                    authStatusText = authStatusText,
                    onCreateAccount = {
                        scope.launch {
                            startCreateAccountOnboarding()
                        }
                    },
                    onSignInAccount = {
                        scope.launch {
                            signInExistingAccountFromEntry()
                        }
                    },
                    onOpenDemo = {
                        scope.launch {
                            openDemoMode()
                        }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            AppScreen.REAL_MAIN -> {
                if (showOnboarding) {
                    if (showOnboardingSubscription) {
                        SubscriptionScreen(
                            isPremiumActive = isPremiumActive,
                            billingStatusText = billingStatusText,
                            onBuyPremium = {
                                val activity = context as? Activity
                                if (activity != null) {
                                    billingManager.launchPremiumPurchase(activity)
                                }
                            },
                            onRefreshSubscription = {
                                billingManager.queryActiveSubscriptions()
                                billingManager.queryPremiumProduct()
                            },
                            onActivateTestPremium = {
                                updatePremiumStatus(true)
                                showOnboardingSubscription = false
                            },
                            onDisableTestPremium = {
                                updatePremiumStatus(false)
                                showOnboardingSubscription = false
                            },
                            onBack = {
                                showOnboardingSubscription = false
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        OnboardingScreen(
                            googleUserDisplayName = googleUserDisplayName,
                            onboardingAuthMode = if (onboardingStartMode == OnboardingStartMode.CREATE_ACCOUNT) {
                                OnboardingAuthMode.CREATE_ACCOUNT
                            } else {
                                OnboardingAuthMode.SIGN_IN
                            },
                            initialStep = onboardingInitialStep,
                            initialDateOfBirth = onboardingInitialDateOfBirth,
                            accountStatusText = onboardingAccountStatusText,
                            isGoogleAccountConnected = onboardingProfileId != null,
                            onGoogleAuthClick = { draftDateOfBirth ->
                                scope.launch {
                                    continueWithGoogleFromOnboarding(
                                        draftDateOfBirth = draftDateOfBirth
                                    )
                                }
                            },
                            onSwitchToCreateAccount = {
                                onboardingStartMode = OnboardingStartMode.CREATE_ACCOUNT
                                onboardingInitialStep = 4
                                onboardingAccountStatusText = ""
                            },
                            onSwitchToSignIn = {
                                onboardingStartMode = OnboardingStartMode.SIGN_IN
                                onboardingInitialStep = 4
                                onboardingInitialDateOfBirth = null
                                onboardingAccountStatusText = ""
                            },
                            onStepChanged = { newStep ->
                                onboardingInitialStep = newStep
                            },
                            onCancelOnboarding = {
                                showOnboarding = false
                                showOnboardingSubscription = false
                                onboardingProfileId = null
                                onboardingInitialStep = 0
                                onboardingInitialDateOfBirth = null
                                onboardingAccountStatusText = ""
                                currentScreen = AppScreen.ENTRY
                            },
                            healthConnectAvailable = healthConnectAvailability == HealthConnectManager.HealthConnectAvailability.AVAILABLE,
                            healthConnectConnected = isHealthConnectConnected,
                            onRequestHealthConnectPermissions = {
                                healthConnectAvailability = healthConnectManager.getAvailabilityStatus()

                                when (healthConnectAvailability) {
                                    HealthConnectManager.HealthConnectAvailability.AVAILABLE -> {
                                        healthPermissionsLauncher.launch(healthConnectManager.permissions)
                                    }

                                    HealthConnectManager.HealthConnectAvailability.NEEDS_INSTALL_OR_UPDATE -> {
                                        healthConnectManager.openHealthConnectInStore()
                                    }

                                    HealthConnectManager.HealthConnectAvailability.UNAVAILABLE -> {
                                        authStatusText = "Health Connect недоступен на этом устройстве"
                                    }
                                }
                            },
                            onOpenHealthConnectStore = {
                                healthConnectManager.openHealthConnectInStore()
                            },
                            onOpenSubscription = {
                                showOnboardingSubscription = true
                            },
                            onFinish = { result ->
                                scope.launch {
                                    val profileId = onboardingProfileId
                                    if (profileId != null) {
                                        finishOnboarding(
                                            profileId = profileId,
                                            result = result
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                } else {
                    CalendarWithPredictionScreen(
                        modifier = Modifier.padding(innerPadding),
                        isDemoMode = false,
                        googleUserEmail = googleUserEmail,
                        googleUserDisplayName = googleUserDisplayName,
                        isPremiumActive = isPremiumActive,
                        onPremiumStatusChanged = { isActive ->
                            updatePremiumStatus(isActive)
                        },
                        onSignOut = {
                            scope.launch {
                                signOutFromGoogle()
                            }
                        },
                        onBackupRealProfileToCloud = {
                            val currentFirebaseUser = googleAuthManager.currentUser()
                            val activeProfile = userProfileDao.getActiveProfile()

                            if (
                                currentFirebaseUser != null &&
                                activeProfile != null &&
                                !activeProfile.isDemo
                            ) {
                                cloudBackupRepository.backupProfileToCloud(
                                    firebaseUid = currentFirebaseUser.uid,
                                    profile = activeProfile
                                )
                            }
                        },
                        onDeleteAccountAndAllData = {
                            deleteAccountAndAllData()
                        },
                        billingStatusText = billingStatusText,
                        onBuyPremium = {
                            val activity = context as? Activity
                            if (activity != null) {
                                billingManager.launchPremiumPurchase(activity)
                            }
                        },
                        onRefreshSubscription = {
                            billingManager.queryActiveSubscriptions()
                            billingManager.queryPremiumProduct()
                        },
                        onBackToEntry = {
                            currentScreen = AppScreen.ENTRY
                        }
                    )
                }
            }

            AppScreen.DEMO_MAIN -> {
                CalendarWithPredictionScreen(
                    modifier = Modifier.padding(innerPadding),
                    isDemoMode = true,
                    googleUserEmail = null,
                    googleUserDisplayName = null,
                    isPremiumActive = true,
                    onPremiumStatusChanged = {},
                    onSignOut = {},
                    onBackupRealProfileToCloud = {},
                    onDeleteAccountAndAllData = {},
                    billingStatusText = "Demo-режим: Premium включён для тестирования",
                    onBuyPremium = {},
                    onRefreshSubscription = {},
                    onBackToEntry = {
                        currentScreen = AppScreen.ENTRY
                    }
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}