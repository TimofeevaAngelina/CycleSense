package com.timofeeva.cyclesense.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val profileType: String,
    val isDemo: Boolean,
    val seed: Int? = null,
    val isActive: Boolean = false,

    // Привязка основного профиля к Google/Firebase
    val firebaseUid: String? = null,
    val googleEmail: String? = null,
    val googleDisplayName: String? = null,

    // Постоянные параметры профиля
    val dateOfBirth: String? = null,   // формат: YYYY-MM-DD
    val weightKg: Float? = null,
    val heightCm: Float? = null,

    val onboardingCompleted: Boolean = false,
    val lastPeriodStartDate: String? = null,
    val avgCycleLength: Int? = null,
    val avgMensesLength: Int? = null,
    val privacyAcceptedAt: String? = null,
    val healthConnectSkippedAt: String? = null
)