package com.timofeeva.cyclesense.premium

import android.content.Context

class PremiumAccessRepository(
    context: Context
) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "cyclesense_premium_access",
        Context.MODE_PRIVATE
    )

    fun isPremiumActive(): Boolean {
        return prefs.getBoolean(KEY_IS_PREMIUM_ACTIVE, false)
    }

    fun setPremiumActive(isActive: Boolean) {
        prefs.edit()
            .putBoolean(KEY_IS_PREMIUM_ACTIVE, isActive)
            .apply()
    }

    companion object {
        private const val KEY_IS_PREMIUM_ACTIVE = "is_premium_active"
    }
}