package com.timofeeva.cyclesense


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import android.widget.NumberPicker
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.text.style.TextAlign
import com.timofeeva.cyclesense.ui.theme.CycleSenseTextSecondary
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak

data class OnboardingResult(
    val name: String,
    val dateOfBirth: String,
    val weightKg: Float,
    val heightCm: Float,
    val lastPeriodStartDate: String,
    val avgCycleLength: Int,
    val avgMensesLength: Int,
    val healthConnectSkipped: Boolean,
    val privacyAcceptedAt: String,


    val termsAccepted: Boolean,
    val privacyAccepted: Boolean,
    val dataProcessingAccepted: Boolean,
    val recommendationsAccepted: Boolean,
    val medicalDisclaimerAccepted: Boolean,
    val securityNoticeAccepted: Boolean,
    val parentConsentAccepted: Boolean
)


enum class OnboardingAuthMode {
    CREATE_ACCOUNT,
    SIGN_IN
}

private val OnboardingGap = 12.dp
private val OnboardingSmallGap = 8.dp


@Composable
fun OnboardingScreen(
    googleUserDisplayName: String?,
    onboardingAuthMode: OnboardingAuthMode,
    initialStep: Int = 0,
    initialDateOfBirth: String? = null,
    accountStatusText: String,
    isGoogleAccountConnected: Boolean,
    onGoogleAuthClick: (String) -> Unit,
    onSwitchToCreateAccount: () -> Unit,
    onSwitchToSignIn: () -> Unit,
    onCancelOnboarding: () -> Unit,
    healthConnectAvailable: Boolean,
    healthConnectConnected: Boolean,
    onRequestHealthConnectPermissions: () -> Unit,
    onOpenHealthConnectStore: () -> Unit,
    onOpenSubscription: () -> Unit,
    onStepChanged: (Int) -> Unit = {},
    onFinish: (OnboardingResult) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by rememberSaveable(initialStep) { mutableIntStateOf(initialStep) }

    val minAllowedStep = if (
        onboardingAuthMode == OnboardingAuthMode.CREATE_ACCOUNT &&
        isGoogleAccountConnected
    ) {
        5
    } else {
        0
    }

    LaunchedEffect(isGoogleAccountConnected, onboardingAuthMode) {
        if (
            onboardingAuthMode == OnboardingAuthMode.CREATE_ACCOUNT &&
            isGoogleAccountConnected &&
            step < 5
        ) {
            step = 5
        }
    }

    LaunchedEffect(step) {
        onStepChanged(step)
    }

    val currentYear = LocalDate.now().year

    val initialBirthDate = remember(initialDateOfBirth) {
        initialDateOfBirth
            ?.takeIf { it.isNotBlank() }
            ?.let { value ->
                runCatching { LocalDate.parse(value) }.getOrNull()
            }
    }

    var name by rememberSaveable {
        mutableStateOf(googleUserDisplayName.orEmpty())
    }


    LaunchedEffect(googleUserDisplayName) {
        if (name.isBlank() && !googleUserDisplayName.isNullOrBlank()) {
            name = googleUserDisplayName
        }
    }


    var selectedBirthYear by rememberSaveable(initialDateOfBirth) {
        mutableIntStateOf(initialBirthDate?.year ?: currentYear - 25)
    }

    var selectedBirthMonth by rememberSaveable(initialDateOfBirth) {
        mutableIntStateOf(initialBirthDate?.monthValue ?: 1)
    }

    var selectedBirthDay by rememberSaveable(initialDateOfBirth) {
        mutableIntStateOf(initialBirthDate?.dayOfMonth ?: 1)
    }


    val birthDate = buildSafeDate(
        year = selectedBirthYear,
        month = selectedBirthMonth,
        day = selectedBirthDay
    )


    val ageYears = calculateAgeYearsFromDate(birthDate)
    val isUnder16 = ageYears < 16


    var weightKgText by rememberSaveable { mutableStateOf("") }
    var heightCmText by rememberSaveable { mutableStateOf("") }


    var avgCycleLength by rememberSaveable { mutableIntStateOf(28) }
    var avgMensesLength by rememberSaveable { mutableIntStateOf(5) }


    var healthConnectSkipped by rememberSaveable { mutableStateOf(false) }


    var termsAccepted by rememberSaveable { mutableStateOf(false) }
    var privacyAccepted by rememberSaveable { mutableStateOf(false) }
    var dataProcessingAccepted by rememberSaveable { mutableStateOf(false) }
    var recommendationsAccepted by rememberSaveable { mutableStateOf(false) }
    var medicalDisclaimerAccepted by rememberSaveable { mutableStateOf(false) }
    var securityNoticeAccepted by rememberSaveable { mutableStateOf(false) }
    var parentConsentAccepted by rememberSaveable { mutableStateOf(false) }


    var lastPeriodCalendarMonthText by rememberSaveable {
        mutableStateOf(YearMonth.now().toString())
    }


    var selectedLastPeriodDateText by rememberSaveable {
        mutableStateOf(LocalDate.now().minusDays(3).toString())
    }


    var visibleLegalDocument by remember {
        mutableStateOf<LegalDocumentType?>(null)
    }


    val lastPeriodCalendarMonth = YearMonth.parse(lastPeriodCalendarMonthText)
    val selectedLastPeriodDate = LocalDate.parse(selectedLastPeriodDateText)


    val totalVisibleSteps = if (isUnder16) {
        12
    } else {
        11
    }


    val visibleStepNumber = if (isUnder16) {
        step + 1
    } else {
        when {
            step <= 1 -> step + 1
            else -> step
        }
    }


    val shouldCenterContent = step in listOf(
        0,  // Добро пожаловать
        1,  // Дата рождения
        2,  // Согласие родителя
        4,  // Google-аккаунт
        5,  // Параметры профиля
        7,  // Длина цикла
        8,  // Длительность менструации
        9,  // Health Connect
        10, // Premium
        11  // Профиль готов
    )

    val disableScrollForPickers = step in listOf(1, 7, 8) // дата рождения, длина цикла, длительность



    AdaptiveScreenContainer(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(
                        state = rememberScrollState(),
                        enabled = !disableScrollForPickers
                    ),
                verticalArrangement = if (shouldCenterContent) {
                    Arrangement.Center
                } else {
                    Arrangement.Top
                }
            ) {


                when (step) {
                    0 -> {
                        StepHeader(
                            title = "Добро пожаловать",
                            stepText = "Шаг $visibleStepNumber из $totalVisibleSteps",
                            text = "CycleSense помогает отслеживать цикл, самочувствие и физиологические показатели."
                        )


                        OnboardingBodyText(
                            text = "Приложение не является медицинским сервисом, не ставит диагнозы и не заменяет консультацию врача.",
                        )


                    }


                    1 -> {
                        StepHeader(
                            title = "Дата рождения",
                            stepText = "Шаг $visibleStepNumber из $totalVisibleSteps",
                            text = "Дата рождения нужна для настройки профиля и проверки возрастных ограничений."
                        )


                        val daysInMonth = YearMonth.of(
                            selectedBirthYear,
                            selectedBirthMonth
                        ).lengthOfMonth()


                        if (selectedBirthDay > daysInMonth) {
                            selectedBirthDay = daysInMonth
                        }


                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = OnboardingGap),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WheelNumberPicker(
                                title = "День",
                                values = (1..daysInMonth).toList(),
                                selectedValue = selectedBirthDay,
                                onValueSelected = { selectedBirthDay = it },
                                modifier = Modifier.weight(1f)
                            )


                            WheelNumberPicker(
                                title = "Месяц",
                                values = (1..12).toList(),
                                selectedValue = selectedBirthMonth,
                                onValueSelected = { selectedBirthMonth = it },
                                modifier = Modifier.weight(1f)
                            )


                            WheelNumberPicker(
                                title = "Год",
                                values = ((currentYear - 100)..(currentYear - 1)).toList(),
                                selectedValue = selectedBirthYear,
                                onValueSelected = { selectedBirthYear = it },
                                modifier = Modifier.weight(1.2f)
                            )
                        }


                    }


                    2 -> {
                        StepHeader(
                            title = "Согласие родителя или опекуна",
                            stepText = "Шаг $visibleStepNumber из $totalVisibleSteps",
                            text = "Если пользователю меньше 16 лет, для использования приложения требуется согласие родителя или законного представителя."
                        )


                        OnboardingBodyText(
                            text = "На следующем шаге нужно будет подтвердить, что такое согласие получено."
                        )


                    }


                    3 -> {
                        StepHeader(
                            title = "Согласия и документы",
                            stepText = "Шаг $visibleStepNumber из $totalVisibleSteps",
                            text = "Перед созданием профиля нужно принять условия использования данных."
                        )

                        OutlinedButton(
                            onClick = {
                                termsAccepted = true
                                privacyAccepted = true
                                dataProcessingAccepted = true
                                recommendationsAccepted = true
                                medicalDisclaimerAccepted = true
                                securityNoticeAccepted = true

                                if (isUnder16) {
                                    parentConsentAccepted = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = OnboardingGap),
                            shape = CycleSenseButtonShape
                        ) {
                            CycleSenseButtonText("Принять все")
                        }

                        AgreementLinkRow(
                            checked = termsAccepted,
                            onCheckedChange = { termsAccepted = it },
                            fullText = "Я принимаю условия использования.",
                            linkText = "условия использования",
                            onLinkClick = {
                                visibleLegalDocument = LegalDocumentType.TERMS_OF_USE
                            }
                        )


                        AgreementLinkRow(
                            checked = privacyAccepted,
                            onCheckedChange = { privacyAccepted = it },
                            fullText = "Я принимаю политику конфиденциальности.",
                            linkText = "политику конфиденциальности",
                            onLinkClick = {
                                visibleLegalDocument = LegalDocumentType.PRIVACY_POLICY
                            }
                        )


                        AgreementRow(
                            checked = dataProcessingAccepted,
                            onCheckedChange = { dataProcessingAccepted = it },
                            text = "Я согласна на обработку данных цикла, самочувствия и физиологических показателей для работы приложения."
                        )


                        AgreementRow(
                            checked = recommendationsAccepted,
                            onCheckedChange = { recommendationsAccepted = it },
                            text = "Я согласна на использование этих данных для аналитики, прогноза и персональных рекомендаций."
                        )


                        AgreementRow(
                            checked = medicalDisclaimerAccepted,
                            onCheckedChange = { medicalDisclaimerAccepted = it },
                            text = "Я понимаю, что CycleSense не является медицинским сервисом и не ставит диагнозы."
                        )


                        AgreementRow(
                            checked = securityNoticeAccepted,
                            onCheckedChange = { securityNoticeAccepted = it },
                            text = "Я понимаю, что данные используются только для работы календаря, аналитики, прогноза и резервного копирования. Доступ к Health Connect можно отозвать в настройках устройства."
                        )


                        if (isUnder16) {
                            AgreementRow(
                                checked = parentConsentAccepted,
                                onCheckedChange = { parentConsentAccepted = it },
                                text = "Я подтверждаю, что родитель или законный представитель дал согласие на использование приложения."
                            )
                        }

                        Spacer(modifier = Modifier.height(OnboardingSmallGap))


                    }


                    4 -> {
                        val isCreateMode = onboardingAuthMode == OnboardingAuthMode.CREATE_ACCOUNT


                        StepHeader(
                            title = if (isCreateMode) {
                                "Создание аккаунта"
                            } else {
                                "Вход в аккаунт"
                            },
                            stepText = "Шаг $visibleStepNumber из $totalVisibleSteps",
                            text = if (isCreateMode) {
                                "Войдите через Google, чтобы создать профиль CycleSense."
                            } else {
                                "Войдите через Google-аккаунт, который уже использовался в CycleSense."
                            }
                        )


                        if (accountStatusText.isNotBlank()) {
                            OnboardingBodyText(
                                text = accountStatusText
                            )
                        }


                        if (isGoogleAccountConnected) {
                            OnboardingBodyText(
                                text = if (!googleUserDisplayName.isNullOrBlank()) {
                                    "Google-аккаунт подключён: $googleUserDisplayName"
                                } else {
                                    "Google-аккаунт подключён."
                                }
                            )
                        }
                    }


                    5 -> {
                        StepHeader(
                            title = "Параметры профиля",
                            stepText = "Шаг $visibleStepNumber из $totalVisibleSteps",
                            text = "Имя нужно для профиля. Вес и рост необязательны, но могут использоваться для персонализации аналитики."
                        )


                        val weightRaw = weightKgText.trim()
                        val heightRaw = heightCmText.trim()


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





                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Имя") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = OnboardingGap),
                            singleLine = true
                        )


                        OutlinedTextField(
                            value = weightKgText,
                            onValueChange = { newValue ->
                                weightKgText = normalizeDecimalInput(
                                    input = newValue,
                                    maxIntegerDigits = 3,
                                    maxDecimalDigits = 1
                                )
                            },
                            label = { Text("Вес, кг (необязательно)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = OnboardingGap),
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
                            value = heightCmText,
                            onValueChange = { newValue ->
                                heightCmText = newValue.filter { it.isDigit() }
                            },
                            label = { Text("Рост, см (необязательно)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = OnboardingGap),
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


                    6 -> {
                        StepHeader(
                            title = "Начало последней менструации",
                            stepText = "Шаг $visibleStepNumber из $totalVisibleSteps",
                            text = "Выберите дату начала последней менструации. На основе этой даты приложение создаст текущий цикл."
                        )





                        SimpleLastPeriodCalendar(
                            month = lastPeriodCalendarMonth,
                            selectedDate = selectedLastPeriodDate,
                            onPrevMonth = {
                                lastPeriodCalendarMonthText = lastPeriodCalendarMonth
                                    .minusMonths(1)
                                    .toString()
                            },
                            onNextMonth = {
                                val nextMonth = lastPeriodCalendarMonth.plusMonths(1)
                                if (!nextMonth.isAfter(YearMonth.now())) {
                                    lastPeriodCalendarMonthText = nextMonth.toString()
                                }
                            },
                            onDateSelected = { date ->
                                if (!date.isAfter(LocalDate.now())) {
                                    selectedLastPeriodDateText = date.toString()
                                }
                            },
                            modifier = Modifier.padding(top = OnboardingSmallGap)
                        )

                        Text(
                            text = "Если не знаете точную дату, выберите примерную. Позже её можно будет скорректировать в календаре.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = OnboardingGap)
                        )
                        Spacer(modifier = Modifier.height(OnboardingGap))


                    }


                    7 -> {
                        StepHeader(
                            title = "Обычная длина цикла",
                            stepText = "Шаг $visibleStepNumber из $totalVisibleSteps",
                            text = "Выберите среднюю длину цикла. Если не знаете точно, укажите примерное значение — его можно будет изменить позже."
                        )


                        WheelNumberPicker(
                            title = "",
                            values = (21..45).toList(),
                            selectedValue = avgCycleLength,
                            onValueSelected = { avgCycleLength = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = OnboardingGap)
                        )


                    }


                    8 -> {
                        StepHeader(
                            title = "Длительность менструации",
                            stepText = "Шаг $visibleStepNumber из $totalVisibleSteps",
                            text = "Выберите обычную длительность менструации."
                        )


                        WheelNumberPicker(
                            title = "",
                            values = (2..10).toList(),
                            selectedValue = avgMensesLength,
                            onValueSelected = { avgMensesLength = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = OnboardingGap)
                        )


                    }


                    9 -> {
                        StepHeader(
                            title = "Health Connect",
                            stepText = "Шаг $visibleStepNumber из $totalVisibleSteps",
                            text = "Вы можете подключить приложение к Health Connect для автоматического импорта шагов, сна и пульса. Это поможет точнее анализировать состояние.",
                            textAlign = TextAlign.Justify
                        )


                        OnboardingBodyText(
                            text = "После подключения нужно отдельно открыть приложение с физиологическими данными, например Mi Fitness, Huawei Health или другое приложение, и включить передачу данных в Health Connect.",
                            textAlign = TextAlign.Justify,
                        )


                        OnboardingBodyText(
                            text = "Health Connect содержит данные, связанные с этим устройством. Если на телефоне используется несколько аккаунтов CycleSense, все они могут видеть одни и те же данные Health Connect. Подключайте Health Connect только к профилю владельца устройства.",
                            textAlign = TextAlign.Justify,
                        )


                        if (healthConnectConnected) {
                            OnboardingBodyText(
                                text = "Health Connect подключён, разрешения для CycleSense выданы."
                            )
                        }
                    }


                    10 -> {
                        StepHeader(
                            title = "Premium",
                            stepText = "Шаг $visibleStepNumber из $totalVisibleSteps",
                            text = "В бесплатной версии доступны календарь, ручной ввод, Health Connect и облачное восстановление."
                        )


                        OnboardingBodyText(
                            text = "Premium открывает доступ к прогнозу самочувствия, расширенной аналитике и анализу взаимосвязей между физиологическими данными и самочувствием.",
                        )


                    }


                    11 -> {
                        StepHeader(
                            title = "Профиль готов",
                            stepText = "Шаг $visibleStepNumber из $totalVisibleSteps",
                            text = "CycleSense создаст текущий цикл на основе указанной даты и откроет календарь."
                        )


                        OnboardingBodyText(
                            text = "Вы сможете изменить параметры профиля, подключить Health Connect и управлять подпиской позже во вкладке «Профиль».",
                        )


                    }
                }
            }

            OnboardingBottomActions(
                step = step,
                minAllowedStep = minAllowedStep,
                isUnder16 = isUnder16,
                isCreateMode = onboardingAuthMode == OnboardingAuthMode.CREATE_ACCOUNT,
                isGoogleAccountConnected = isGoogleAccountConnected,
                canContinueFromAgreements = termsAccepted &&
                        privacyAccepted &&
                        dataProcessingAccepted &&
                        recommendationsAccepted &&
                        medicalDisclaimerAccepted &&
                        securityNoticeAccepted &&
                        (!isUnder16 || parentConsentAccepted),
                canContinueFromProfileParams = name.isNotBlank() &&
                        weightKgText.trim().let { raw ->
                            val weight = raw.toFloatOrNull()
                            raw.isBlank() || (weight != null && weight in 10f..500f)
                        } &&
                        heightCmText.trim().let { raw ->
                            val height = raw.toIntOrNull()
                            raw.isBlank() || (height != null && height in 40..300)
                        },
                healthConnectConnected = healthConnectConnected,
                healthConnectAvailable = healthConnectAvailable,
                onPrimaryClick = {
                    when (step) {
                        0 -> step = 1
                        1 -> step = if (isUnder16) 2 else 3
                        2 -> step = 3
                        3 -> step = 4
                        4 -> {
                            if (isGoogleAccountConnected) {
                                step = 5
                            } else {
                                onGoogleAuthClick(birthDate.toString())
                            }
                        }

                        5 -> step = 6
                        6 -> step = 7
                        7 -> step = 8
                        8 -> step = 9
                        9 -> {
                            if (healthConnectConnected) {
                                healthConnectSkipped = false
                                step = 10
                            } else if (healthConnectAvailable) {
                                healthConnectSkipped = false
                                onRequestHealthConnectPermissions()
                            } else {
                                onOpenHealthConnectStore()
                            }
                        }

                        10 -> onOpenSubscription()
                        11 -> {
                            val result = OnboardingResult(
                                name = name.trim(),
                                dateOfBirth = birthDate.toString(),
                                weightKg = weightKgText.toFloatOrNull() ?: 0f,
                                heightCm = heightCmText.toIntOrNull()?.toFloat() ?: 0f,
                                lastPeriodStartDate = selectedLastPeriodDate.toString(),
                                avgCycleLength = avgCycleLength,
                                avgMensesLength = avgMensesLength,
                                healthConnectSkipped = healthConnectSkipped,
                                privacyAcceptedAt = LocalDate.now().toString(),
                                termsAccepted = termsAccepted,
                                privacyAccepted = privacyAccepted,
                                dataProcessingAccepted = dataProcessingAccepted,
                                recommendationsAccepted = recommendationsAccepted,
                                medicalDisclaimerAccepted = medicalDisclaimerAccepted,
                                securityNoticeAccepted = securityNoticeAccepted,
                                parentConsentAccepted = parentConsentAccepted
                            )

                            onFinish(result)
                        }
                    }
                },
                onSecondaryClick = {
                    when (step) {
                        4 -> {
                            if (onboardingAuthMode == OnboardingAuthMode.CREATE_ACCOUNT) {
                                onSwitchToSignIn()
                                step = 4
                            } else {
                                onSwitchToCreateAccount()
                                step = 4
                            }
                        }

                        9 -> {
                            healthConnectSkipped = true
                            step = 10
                        }

                        10 -> step = 11
                    }
                },
                onBackClick = {
                    when (step) {
                        1 -> step = 0
                        2 -> step = 1
                        3 -> step = if (isUnder16) 2 else 1
                        4 -> {
                            if (onboardingAuthMode == OnboardingAuthMode.SIGN_IN) {
                                onCancelOnboarding()
                            } else {
                                step = 3
                            }
                        }

                        5 -> step = 4.coerceAtLeast(minAllowedStep)
                        6 -> step = 5.coerceAtLeast(minAllowedStep)
                        7 -> step = 6.coerceAtLeast(minAllowedStep)
                        8 -> step = 7.coerceAtLeast(minAllowedStep)
                        9 -> step = 8.coerceAtLeast(minAllowedStep)
                        10 -> step = 9.coerceAtLeast(minAllowedStep)
                        11 -> step = 10.coerceAtLeast(minAllowedStep)
                    }

                },
                modifier = Modifier.fillMaxWidth()

            )
        }


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
private fun OnboardingBottomActions(
    step: Int,
    minAllowedStep: Int,
    isUnder16: Boolean,
    isCreateMode: Boolean,
    isGoogleAccountConnected: Boolean,
    canContinueFromAgreements: Boolean,
    canContinueFromProfileParams: Boolean,
    healthConnectConnected: Boolean,
    healthConnectAvailable: Boolean,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryText = when (step) {
        0 -> "Начать настройку"
        1 -> "Далее"
        2 -> "Понятно"
        3 -> "Продолжить"
        4 -> {
            if (isGoogleAccountConnected) {
                "Продолжить создание профиля"
            } else if (isCreateMode) {
                "Создать аккаунт через Google"
            } else {
                "Войти через Google"
            }
        }

        5 -> "Далее"
        6 -> "Далее"
        7 -> "Далее"
        8 -> "Далее"
        9 -> {
            if (healthConnectConnected) {
                "Продолжить"
            } else if (healthConnectAvailable) {
                "Подключить Health Connect"
            } else {
                "Установить Health Connect"
            }
        }

        10 -> "Посмотреть Premium"
        11 -> "Перейти в приложение"
        else -> "Далее"
    }

    val primaryEnabled = when (step) {
        3 -> canContinueFromAgreements
        5 -> canContinueFromProfileParams
        else -> true
    }

    Column(
        modifier = modifier
            .padding(top = OnboardingGap, bottom = OnboardingSmallGap),
        verticalArrangement = Arrangement.spacedBy(OnboardingSmallGap)
    ) {
        Button(
            onClick = onPrimaryClick,
            enabled = primaryEnabled,
            modifier = Modifier.fillMaxWidth(),
            shape = CycleSenseButtonShape
        ) {
            CycleSenseButtonText(primaryText)
        }

        when (step) {
            4 -> {
                if (!isGoogleAccountConnected) {
                    OutlinedButton(
                        onClick = onSecondaryClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = CycleSenseButtonShape
                    ) {
                        CycleSenseButtonText(
                            if (isCreateMode) {
                                "У меня уже есть аккаунт"
                            } else {
                                "Создать новый аккаунт"
                            }
                        )
                    }
                }
            }

            9 -> {
                if (!healthConnectConnected) {
                    OutlinedButton(
                        onClick = onSecondaryClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = CycleSenseButtonShape
                    ) {
                        CycleSenseButtonText("Пропустить")
                    }
                }
            }

            10 -> {
                OutlinedButton(
                    onClick = onSecondaryClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CycleSenseButtonShape
                ) {
                    CycleSenseButtonText("Продолжить бесплатно")
                }
            }
        }

        if (step > minAllowedStep) {
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth(),
                shape = CycleSenseButtonShape
            ) {
                CycleSenseButtonText("Назад")
            }
        }
    }
}

@Composable
private fun OnboardingBodyText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = textAlign,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = OnboardingGap)
    )
}

@Composable
private fun OnboardingSecondaryText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = CycleSenseTextSecondary,
        textAlign = textAlign,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = OnboardingGap)
    )
}

@Composable
private fun StepHeader(
    title: String,
    stepText: String,
    text: String,
    textAlign: TextAlign = TextAlign.Center
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(OnboardingGap)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                hyphens = Hyphens.Auto,
                lineBreak = LineBreak.Paragraph
            ),
            textAlign = TextAlign.Center,
            softWrap = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stepText,
            style = MaterialTheme.typography.labelLarge,
            color = CycleSenseTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@Composable
private fun AgreementRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = OnboardingGap)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(start = 8.dp, top = 10.dp)
        )
    }
}


@Composable
private fun AgreementLinkRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    fullText: String,
    linkText: String,
    onLinkClick: () -> Unit
) {
    val linkColor = MaterialTheme.colorScheme.primary


    val annotatedText = buildAnnotatedString {
        val startIndex = fullText.indexOf(linkText)
        val endIndex = startIndex + linkText.length


        if (startIndex < 0) {
            append(fullText)
            return@buildAnnotatedString
        }


        append(fullText.substring(0, startIndex))


        pushStringAnnotation(
            tag = "legal_link",
            annotation = linkText
        )
        withStyle(
            style = SpanStyle(
                color = linkColor,
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.SemiBold
            )
        ) {
            append(linkText)
        }
        pop()


        append(fullText.substring(endIndex))
    }


    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = OnboardingGap)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )

        ClickableText(
            text = annotatedText,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.padding(start = 8.dp, top = 10.dp),
            onClick = { offset ->
                annotatedText
                    .getStringAnnotations(
                        tag = "legal_link",
                        start = offset,
                        end = offset
                    )
                    .firstOrNull()
                    ?.let {
                        onLinkClick()
                    }
            }
        )
    }
}


@Composable
private fun WheelNumberPicker(
    title: String,
    values: List<Int>,
    selectedValue: Int,
    onValueSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeValues = values.ifEmpty { listOf(selectedValue) }
    val safeSelectedValue = selectedValue.coerceIn(
        safeValues.first(),
        safeValues.last()
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(2.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp)
                .clipToBounds(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp),
                    factory = { context ->
                        NumberPicker(context).apply {
                            minValue = safeValues.first()
                            maxValue = safeValues.last()
                            value = safeSelectedValue

                            wrapSelectorWheel = false
                            descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                            overScrollMode = android.view.View.OVER_SCROLL_NEVER

                            clipToPadding = true
                            setPadding(0, 0, 0, 0)

                            setOnValueChangedListener { _, _, newValue ->
                                onValueSelected(newValue)
                            }
                        }
                    },
                    update = { picker ->
                        val min = safeValues.first()
                        val max = safeValues.last()
                        val newValue = safeSelectedValue.coerceIn(min, max)

                        if (picker.minValue != min || picker.maxValue != max) {
                            picker.minValue = min
                            picker.maxValue = max
                        }

                        if (picker.value != newValue) {
                            picker.value = newValue
                        }

                        picker.wrapSelectorWheel = false
                        picker.clipToPadding = true
                        picker.overScrollMode = android.view.View.OVER_SCROLL_NEVER
                        picker.setPadding(0, 0, 0, 0)

                        picker.setOnValueChangedListener { _, _, changedValue ->
                            onValueSelected(changedValue)
                        }
                    }
                )
            }
        }
    }
}


@Composable
private fun SimpleLastPeriodCalendar(
    month: YearMonth,
    selectedDate: LocalDate,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val isCurrentMonth = month == YearMonth.now()

    val days = remember(month) {
        buildCalendarMonthDays(month)
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onPrevMonth
            ) {
                Text("←")
            }

            Text(
                text = buildRussianMonthTitle(month),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedButton(
                onClick = onNextMonth,
                enabled = !isCurrentMonth
            ) {
                Text("→")
            }
        }

        Spacer(modifier = Modifier.height(OnboardingSmallGap))

        WeekDaysHeaderSmall()

        Spacer(modifier = Modifier.height(OnboardingSmallGap))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            days.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    week.forEach { day ->
                        LastPeriodCalendarDayCell(
                            date = day.date,
                            isCurrentMonth = day.isCurrentMonth,
                            isSelected = day.date == selectedDate,
                            isToday = day.date == today,
                            isEnabled = day.isCurrentMonth && !day.date.isAfter(today),
                            onClick = {
                                onDateSelected(day.date)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}


private data class SimpleCalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean
)


@Composable
private fun WeekDaysHeaderSmall() {
    val days = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")


    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEach { day ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


@Composable
private fun LastPeriodCalendarDayCell(
    date: LocalDate,
    isCurrentMonth: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.secondaryContainer
        isCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
        !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .then(
                if (isEnabled) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}


private fun buildCalendarMonthDays(
    month: YearMonth
): List<SimpleCalendarDay> {
    val firstDayOfMonth = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()


    val firstDayOffset = when (firstDayOfMonth.dayOfWeek) {
        DayOfWeek.MONDAY -> 0
        DayOfWeek.TUESDAY -> 1
        DayOfWeek.WEDNESDAY -> 2
        DayOfWeek.THURSDAY -> 3
        DayOfWeek.FRIDAY -> 4
        DayOfWeek.SATURDAY -> 5
        DayOfWeek.SUNDAY -> 6
    }


    val result = mutableListOf<SimpleCalendarDay>()


    val prevMonth = month.minusMonths(1)
    val prevMonthLength = prevMonth.lengthOfMonth()


    for (i in firstDayOffset downTo 1) {
        val date = prevMonth.atDay(prevMonthLength - i + 1)
        result.add(
            SimpleCalendarDay(
                date = date,
                isCurrentMonth = false
            )
        )
    }


    for (day in 1..daysInMonth) {
        val date = month.atDay(day)
        result.add(
            SimpleCalendarDay(
                date = date,
                isCurrentMonth = true
            )
        )
    }


    val nextMonth = month.plusMonths(1)
    var nextDay = 1


    while (result.size < 42) {
        val date = nextMonth.atDay(nextDay)
        result.add(
            SimpleCalendarDay(
                date = date,
                isCurrentMonth = false
            )
        )
        nextDay++
    }


    return result
}


private fun buildRussianMonthTitle(month: YearMonth): String {
    val locale = Locale.forLanguageTag("ru")
    val monthName = month.month
        .getDisplayName(JavaTextStyle.FULL_STANDALONE, locale)
        .replaceFirstChar { it.uppercase() }


    return "$monthName ${month.year}"
}


private fun buildSafeDate(
    year: Int,
    month: Int,
    day: Int
): LocalDate {
    val safeMonth = month.coerceIn(1, 12)
    val maxDay = YearMonth.of(year, safeMonth).lengthOfMonth()
    val safeDay = day.coerceIn(1, maxDay)


    return LocalDate.of(year, safeMonth, safeDay)
}


private fun calculateAgeYearsFromDate(date: LocalDate): Int {
    return Period.between(date, LocalDate.now()).years
}


private fun normalizeDecimalInput(
    input: String,
    maxIntegerDigits: Int,
    maxDecimalDigits: Int
): String {
    val normalized = input.replace(",", ".")
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
        } else {
            "$integerPart.$decimalPart"
        }
    }
}



