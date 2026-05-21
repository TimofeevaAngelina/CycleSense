package com.timofeeva.cyclesense

import com.timofeeva.cyclesense.health.HealthConnectManager

data class MainHealthConnectStatusResult(
    val isConnected: Boolean,
    val statusText: String
)

suspend fun buildMainHealthConnectStatus(
    healthConnectManager: HealthConnectManager
): MainHealthConnectStatusResult {
    return try {
        val availability = healthConnectManager.getAvailabilityStatus()

        when (availability) {
            HealthConnectManager.HealthConnectAvailability.AVAILABLE -> {
                val hasPermissions = healthConnectManager.hasAllPermissions()

                if (hasPermissions) {
                    MainHealthConnectStatusResult(
                        isConnected = true,
                        statusText = "Health Connect подключён. Проверьте, что Mi Fitness, Huawei Health или другое приложение записывает шаги, сон и пульс в Health Connect."
                    )
                } else {
                    MainHealthConnectStatusResult(
                        isConnected = false,
                        statusText = "Health Connect доступен, но разрешения для CycleSense ещё не выданы."
                    )
                }
            }

            HealthConnectManager.HealthConnectAvailability.NEEDS_INSTALL_OR_UPDATE -> {
                MainHealthConnectStatusResult(
                    isConnected = false,
                    statusText = "Health Connect нужно установить или обновить."
                )
            }

            HealthConnectManager.HealthConnectAvailability.UNAVAILABLE -> {
                MainHealthConnectStatusResult(
                    isConnected = false,
                    statusText = "Health Connect недоступен на этом устройстве."
                )
            }
        }
    } catch (e: Exception) {
        MainHealthConnectStatusResult(
            isConnected = false,
            statusText = "Не удалось проверить Health Connect: ${e.message}"
        )
    }
}

enum class MainHealthConnectConnectAction {
    REQUEST_PERMISSIONS,
    OPEN_SETTINGS,
    OPEN_STORE,
    SHOW_UNAVAILABLE
}

suspend fun resolveMainHealthConnectConnectAction(
    healthConnectManager: HealthConnectManager
): MainHealthConnectConnectAction {
    val availability = healthConnectManager.getAvailabilityStatus()

    return when (availability) {
        HealthConnectManager.HealthConnectAvailability.AVAILABLE -> {
            val hasPermissions = healthConnectManager.hasAllPermissions()

            if (hasPermissions) {
                MainHealthConnectConnectAction.OPEN_SETTINGS
            } else {
                MainHealthConnectConnectAction.REQUEST_PERMISSIONS
            }
        }

        HealthConnectManager.HealthConnectAvailability.NEEDS_INSTALL_OR_UPDATE -> {
            MainHealthConnectConnectAction.OPEN_STORE
        }

        HealthConnectManager.HealthConnectAvailability.UNAVAILABLE -> {
            MainHealthConnectConnectAction.SHOW_UNAVAILABLE
        }
    }
}