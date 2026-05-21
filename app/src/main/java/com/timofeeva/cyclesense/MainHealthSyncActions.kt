package com.timofeeva.cyclesense

import com.timofeeva.cyclesense.data.local.entity.HeartRateIntervalEntity
import com.timofeeva.cyclesense.data.local.entity.UserProfileEntity
import com.timofeeva.cyclesense.data.repository.CycleRepositoryRoom
import com.timofeeva.cyclesense.data.repository.HeartRateIntervalRepository
import com.timofeeva.cyclesense.health.DailyHeartRateInput
import com.timofeeva.cyclesense.health.DailySleepInput
import com.timofeeva.cyclesense.health.DailyStepsInput
import com.timofeeva.cyclesense.health.HealthConnectManager
import com.timofeeva.cyclesense.health.HeartRateIntervalInput
import com.timofeeva.cyclesense.synthetic.SyntheticDataSeeder
import java.time.LocalDate
import java.time.LocalDateTime

class MainHealthSyncActions(
    private val roomRepository: CycleRepositoryRoom,
    private val heartRateIntervalRepository: HeartRateIntervalRepository,
    private val healthConnectManager: HealthConnectManager
) {

    suspend fun ensureDemoProfileSeeded(profile: UserProfileEntity) {
        if (!profile.isDemo) return

        val syntheticType = mapProfileType(profile.profileType) ?: return
        val seed = profile.seed ?: return

        val seeder = SyntheticDataSeeder(
            roomRepository = roomRepository,
            heartRateIntervalRepository = heartRateIntervalRepository
        )

        val existingCycles = roomRepository.getCyclesForProfile(profile.id)
        val existingDays = roomRepository.getDaysForProfile(profile.id)

        if (existingCycles.isEmpty() || existingDays.isEmpty()) {
            seeder.reseedDemoHistory(
                profileId = profile.id,
                seed = seed,
                fullPastCyclesCount = 3,
                endDate = LocalDate.now(),
                profileType = syntheticType,
                profile = profile
            )
            return
        }

        val lastDate = existingDays.maxOfOrNull { LocalDate.parse(it.date) }
        val today = LocalDate.now()

        if (lastDate == null || lastDate.isBefore(today)) {
            seeder.extendCurrentCycleToDate(
                profileId = profile.id,
                targetDate = today,
                seed = seed,
                profileType = syntheticType,
                profile = profile
            )
        }
    }

    suspend fun syncActiveDemoProfileToHealthConnect(profile: UserProfileEntity) {
        if (!profile.isDemo) return

        if (!healthConnectManager.isAvailable()) return

        val hasPermissions = healthConnectManager.hasAllPermissions()
        if (!hasPermissions) return

        val dbDays = roomRepository.getDaysForProfile(profile.id).sortedBy { it.date }
        if (dbDays.isEmpty()) return

        val steps = dbDays.mapNotNull { day ->
            val value = day.steps ?: return@mapNotNull null
            DailyStepsInput(
                date = LocalDate.parse(day.date),
                count = value.toLong()
            )
        }

        val heartRate = dbDays.mapNotNull { day ->
            val value = day.heartRate ?: return@mapNotNull null
            DailyHeartRateInput(
                date = LocalDate.parse(day.date),
                bpm = value.toLong()
            )
        }

        val sleep = dbDays.mapNotNull { day ->
            val value = day.sleepHours ?: return@mapNotNull null
            DailySleepInput(
                date = LocalDate.parse(day.date),
                sleepHours = value
            )
        }

        val intervalEntities = heartRateIntervalRepository.getIntervalsForProfile(profile.id)
        val nowDateTime = LocalDateTime.now()

        val heartRateIntervals = intervalEntities.mapNotNull { interval ->
            try {
                val parsedStart = LocalDateTime.parse(interval.startTime)
                val parsedEnd = LocalDateTime.parse(interval.endTime)

                if (parsedStart.isAfter(nowDateTime)) {
                    return@mapNotNull null
                }

                val safeEnd = if (parsedEnd.isAfter(nowDateTime)) {
                    nowDateTime
                } else {
                    parsedEnd
                }

                if (!safeEnd.isAfter(parsedStart)) {
                    return@mapNotNull null
                }

                HeartRateIntervalInput(
                    startTime = parsedStart,
                    endTime = safeEnd,
                    minBpm = interval.minBpm.toLong(),
                    maxBpm = interval.maxBpm.toLong()
                )
            } catch (_: Exception) {
                null
            }
        }

        healthConnectManager.replaceDemoData(
            dailySteps = steps,
            dailyHeartRate = heartRate,
            dailySleep = sleep,
            heartRateIntervals = heartRateIntervals
        )
    }

    suspend fun syncHealthConnectToRoomForProfile(
        profileId: Long,
        onlyOwnData: Boolean,
        excludeOwnData: Boolean = false,
        ageYears: Int? = null,
        weightKg: Float? = null,
        heightCm: Float? = null
    ): Int {
        if (!healthConnectManager.isAvailable()) {
            return 0
        }

        val hasPermissions = healthConnectManager.hasAllPermissions()
        if (!hasPermissions) {
            return 0
        }

        val today = LocalDate.now()
        val minDateForHealthConnectRead = today.minusDays(29)

        val dbDays = roomRepository.getDaysForProfile(profileId)
            .filter { day ->
                val date = LocalDate.parse(day.date)
                !date.isBefore(minDateForHealthConnectRead) && !date.isAfter(today)
            }
            .sortedBy { it.date }

        if (dbDays.isEmpty()) {
            return 0
        }

        var updatedCount = 0

        dbDays.forEach { day ->
            try {
                val date = LocalDate.parse(day.date)

                val summary = healthConnectManager.readDayHealthSummary(
                    date = date,
                    onlyOwnData = onlyOwnData,
                    excludeOwnData = excludeOwnData
                )

                val newSteps: Float? =
                    if (summary.steps > 0L) {
                        summary.steps.toFloat()
                    } else {
                        day.steps
                    }

                val newHeartRate: Float? =
                    if (summary.heartRateMinBpm != null && summary.heartRateMaxBpm != null) {
                        ((summary.heartRateMinBpm + summary.heartRateMaxBpm) / 2.0).toFloat()
                    } else {
                        day.heartRate
                    }

                val newSleepHours: Float? =
                    if (summary.sleepHours > 0.0) {
                        summary.sleepHours.toFloat()
                    } else {
                        day.sleepHours
                    }

                val newCalories: Float? = calculateDailyCaloriesEstimate(
                    steps = newSteps,
                    heartRate = newHeartRate,
                    ageYears = ageYears,
                    weightKg = weightKg,
                    heightCm = heightCm
                ) ?: day.calories

                val updatedDay = day.copy(
                    steps = newSteps,
                    calories = newCalories,
                    heartRate = newHeartRate,
                    sleepHours = newSleepHours
                )

                val dayChanged =
                    updatedDay.steps != day.steps ||
                            updatedDay.calories != day.calories ||
                            updatedDay.heartRate != day.heartRate ||
                            updatedDay.sleepHours != day.sleepHours

                if (dayChanged) {
                    roomRepository.updateDay(updatedDay)
                    updatedCount++
                }

                if (summary.heartRateMinBpm != null && summary.heartRateMaxBpm != null) {
                    val minBpm = summary.heartRateMinBpm.toInt()
                    val maxBpm = summary.heartRateMaxBpm.toInt()
                    val avgBpm = ((minBpm + maxBpm) / 2.0).toInt()

                    val startTime = date.atStartOfDay()
                    val endTime = date.plusDays(1).atStartOfDay()

                    val existingIntervals =
                        heartRateIntervalRepository.getIntervalsForProfileByDate(
                            profileId = profileId,
                            date = date.toString()
                        )

                    val alreadyHasSameDailyInterval = existingIntervals.any { interval ->
                        interval.startTime == startTime.toString() &&
                                interval.endTime == endTime.toString() &&
                                interval.minBpm == minBpm &&
                                interval.maxBpm == maxBpm &&
                                interval.avgBpm == avgBpm
                    }

                    if (!alreadyHasSameDailyInterval) {
                        heartRateIntervalRepository.insertInterval(
                            HeartRateIntervalEntity(
                                userProfileId = profileId,
                                date = date.toString(),
                                startTime = startTime.toString(),
                                endTime = endTime.toString(),
                                minBpm = minBpm,
                                maxBpm = maxBpm,
                                avgBpm = avgBpm
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // Ошибку по одному дню пропускаем, чтобы синхронизация остальных дней продолжалась.
            }
        }

        return updatedCount
    }

    suspend fun recalculateCaloriesForProfile(
        profileId: Long,
        ageYears: Int?,
        weightKg: Float?,
        heightCm: Float?
    ): Int {
        val days = roomRepository.getDaysForProfile(profileId)

        var updatedCount = 0

        days.forEach { day ->
            val calculatedCalories = calculateDailyCaloriesEstimate(
                steps = day.steps,
                heartRate = day.heartRate,
                ageYears = ageYears,
                weightKg = weightKg,
                heightCm = heightCm
            )

            if (calculatedCalories != null && calculatedCalories != day.calories) {
                roomRepository.updateDay(
                    day.copy(
                        calories = calculatedCalories
                    )
                )
                updatedCount++
            }
        }

        return updatedCount
    }
}