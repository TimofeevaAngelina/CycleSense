package com.timofeeva.cyclesense

fun calculateBmrForFemale(
    ageYears: Int?,
    weightKg: Float?,
    heightCm: Float?
): Float? {
    val age = ageYears ?: return null
    val weight = weightKg ?: return null
    val height = heightCm ?: return null

    if (age !in 8..100) return null
    if (weight !in 10f..500f) return null
    if (height !in 40f..300f) return null

    return 10f * weight + 6.25f * height - 5f * age - 161f
}

fun calculateDailyCaloriesEstimate(
    steps: Float?,
    heartRate: Float?,
    ageYears: Int?,
    weightKg: Float?,
    heightCm: Float?
): Float? {
    val safeSteps = steps ?: return null
    if (safeSteps <= 0f) return null

    val bmr = calculateBmrForFemale(
        ageYears = ageYears,
        weightKg = weightKg,
        heightCm = heightCm
    ) ?: return null

    val heartRateBonus = heartRate
        ?.let { bpm -> maxOf(0f, bpm - 70f) * 2f }
        ?: 0f

    val calories = bmr +
            safeSteps * 0.04f +
            heartRateBonus

    return calories.coerceIn(900f, 4500f)
}