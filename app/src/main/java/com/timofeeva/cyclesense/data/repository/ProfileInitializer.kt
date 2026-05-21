package com.timofeeva.cyclesense.data.repository

import com.timofeeva.cyclesense.data.local.dao.UserProfileDao
import com.timofeeva.cyclesense.data.local.entity.UserProfileEntity

object ProfileInitializer {

    suspend fun ensureDefaultProfiles(
        userProfileDao: UserProfileDao
    ) {
        val existingProfiles = userProfileDao.getAllProfiles()
        if (existingProfiles.isNotEmpty()) return

        val profiles = listOf(
            UserProfileEntity(
                name = "Мой профиль",
                profileType = "REAL",
                isDemo = false,
                seed = null,
                isActive = true,
                dateOfBirth = null,
                weightKg = null,
                heightCm = null
            ),
            UserProfileEntity(
                name = "Classic demo",
                profileType = "CLASSIC",
                isDemo = true,
                seed = 101,
                isActive = false,
                dateOfBirth = "1997-06-14",
                weightKg = 60f,
                heightCm = 167f
            ),
            UserProfileEntity(
                name = "Weak demo",
                profileType = "WEAK",
                isDemo = true,
                seed = 102,
                isActive = false,
                dateOfBirth = "1995-09-03",
                weightKg = 58f,
                heightCm = 165f
            ),
            UserProfileEntity(
                name = "Atypical demo",
                profileType = "ATYPICAL",
                isDemo = true,
                seed = 103,
                isActive = false,
                dateOfBirth = "1993-02-21",
                weightKg = 64f,
                heightCm = 170f
            ),
            UserProfileEntity(
                name = "Flat demo",
                profileType = "FLAT",
                isDemo = true,
                seed = 104,
                isActive = false,
                dateOfBirth = "1998-11-08",
                weightKg = 55f,
                heightCm = 163f
            ),
            UserProfileEntity(
                name = "Extreme demo",
                profileType = "EXTREME",
                isDemo = true,
                seed = 105,
                isActive = false,
                dateOfBirth = "1996-04-17",
                weightKg = 62f,
                heightCm = 168f
            )
        )

        userProfileDao.insertProfiles(profiles)
    }
}