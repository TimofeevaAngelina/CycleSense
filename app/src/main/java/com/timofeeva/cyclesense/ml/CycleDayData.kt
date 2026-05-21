package com.timofeeva.cyclesense.ml

data class CycleDayData(
    val dayInCycle: Int,
    val cycleLength: Int,

    val steps: Float? = null,
    val calories: Float? = null,
    val heartRate: Float? = null,
    val rmssd: Float? = null,
    val sleepHours: Float? = null,

    val stressScore: Float? = null,
    val energyLevel: Float? = null,
    val moodScore: Float? = null,
    val concentrationScore: Float? = null,
    val painLevel: Float? = null,

    val phase: String? = null
)