package com.timofeeva.cyclesense

import com.timofeeva.cyclesense.synthetic.SyntheticProfileType
import java.time.LocalDate

fun mapProfileType(profileType: String): SyntheticProfileType? {
    return when (profileType.uppercase()) {
        "CLASSIC" -> SyntheticProfileType.CLASSIC
        "WEAK" -> SyntheticProfileType.WEAK
        "ATYPICAL" -> SyntheticProfileType.ATYPICAL
        "FLAT" -> SyntheticProfileType.FLAT
        "EXTREME" -> SyntheticProfileType.EXTREME
        else -> null
    }
}

fun calculateAgeYears(dateOfBirth: String?): Int? {
    if (dateOfBirth.isNullOrBlank()) return null

    return try {
        val birthDate = LocalDate.parse(dateOfBirth)
        java.time.Period.between(birthDate, LocalDate.now()).years
    } catch (e: Exception) {
        null
    }
}

fun inferPhaseForManualDay(
    dayInCycle: Int,
    isMenstruationMarked: Boolean
): String {
    if (isMenstruationMarked) return "menstrual"

    return when {
        dayInCycle <= 5 -> "menstrual"
        dayInCycle <= 13 -> "follicular"
        dayInCycle in 14..16 -> "ovulatory"
        else -> "luteal"
    }
}

fun Float.formatOneDecimal(): String {
    return String.format("%.1f", this)
}

fun Float?.formatOptionalOneDecimal(): String {
    return this?.let { String.format("%.1f", it) } ?: "нет данных"
}