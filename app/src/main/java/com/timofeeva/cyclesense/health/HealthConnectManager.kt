package com.timofeeva.cyclesense.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.deleteRecords
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class HealthConnectManager(
    private val context: Context
) {
    companion object {
        private const val PROVIDER_PACKAGE_NAME = "com.google.android.apps.healthdata"
    }

    private val zoneId: ZoneId = ZoneId.systemDefault()

    enum class HealthConnectAvailability {
        AVAILABLE,
        NEEDS_INSTALL_OR_UPDATE,
        UNAVAILABLE
    }

    fun getAvailabilityStatus(): HealthConnectAvailability {
        val status = HealthConnectClient.getSdkStatus(
            context,
            PROVIDER_PACKAGE_NAME
        )

        return when (status) {
            HealthConnectClient.SDK_AVAILABLE -> {
                HealthConnectAvailability.AVAILABLE
            }

            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                HealthConnectAvailability.NEEDS_INSTALL_OR_UPDATE
            }

            else -> {
                HealthConnectAvailability.UNAVAILABLE
            }
        }
    }

    fun openHealthConnectInStore() {
        val uriString = "market://details?id=$PROVIDER_PACKAGE_NAME&url=healthconnect%3A%2F%2Fonboarding"

        val marketIntent = Intent(Intent.ACTION_VIEW).apply {
            setPackage("com.android.vending")
            data = Uri.parse(uriString)
            putExtra("overlay", true)
            putExtra("callerId", context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$PROVIDER_PACKAGE_NAME")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(marketIntent)
        } catch (_: Exception) {
            context.startActivity(browserIntent)
        }
    }

    fun openHealthConnectSettings() {
        val action = if (Build.VERSION.SDK_INT >= 34) {
            "android.health.connect.action.HEALTH_HOME_SETTINGS"
        } else {
            "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
        }

        val settingsIntent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(settingsIntent)
        } catch (_: Exception) {
            val launchIntent = context.packageManager
                .getLaunchIntentForPackage(PROVIDER_PACKAGE_NAME)
                ?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

            if (launchIntent != null) {
                context.startActivity(launchIntent)
            } else {
                openHealthConnectInStore()
            }
        }
    }

    fun isAvailable(): Boolean {
        return getAvailabilityStatus() == HealthConnectAvailability.AVAILABLE
    }

    fun getClient(): HealthConnectClient {
        return HealthConnectClient.getOrCreate(
            context,
            PROVIDER_PACKAGE_NAME
        )
    }

    val basePermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),

        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),

        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class)
    )

    val permissions = basePermissions

    suspend fun hasAllPermissions(): Boolean {
        val granted = getClient().permissionController.getGrantedPermissions()
        return granted.containsAll(basePermissions)
    }

    fun createPermissionsLauncherContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    suspend fun readTodaySteps(): Long {
        val startOfDay: Instant = ZonedDateTime.now(zoneId)
            .toLocalDate()
            .atStartOfDay(zoneId)
            .toInstant()

        val now = Instant.now()

        val response = getClient().aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
            )
        )

        return response[StepsRecord.COUNT_TOTAL] ?: 0L
    }

    suspend fun readDayHealthSummary(
        date: LocalDate,
        onlyOwnData: Boolean = false,
        excludeOwnData: Boolean = false
    ): HealthConnectDaySummary {
        val startInstant = date.atStartOfDay(zoneId).toInstant()
        val endInstant = date.plusDays(1).atStartOfDay(zoneId).toInstant()

        val dataOriginFilter =
            if (onlyOwnData) {
                setOf(DataOrigin(context.packageName))
            } else {
                emptySet()
            }

        val steps = readStepsForDate(
            startInstant = startInstant,
            endInstant = endInstant,
            dataOriginFilter = dataOriginFilter,
            excludeOwnData = excludeOwnData
        )

        val heartRate = readHeartRateForDate(
            startInstant = startInstant,
            endInstant = endInstant,
            dataOriginFilter = dataOriginFilter,
            excludeOwnData = excludeOwnData
        )

        val sleep = readSleepForDate(
            date = date,
            startInstant = startInstant,
            endInstant = endInstant,
            dataOriginFilter = dataOriginFilter,
            excludeOwnData = excludeOwnData
        )

        return HealthConnectDaySummary(
            date = date,
            steps = steps,
            heartRateAvgBpm = heartRate.avgBpm,
            heartRateMinBpm = heartRate.minBpm,
            heartRateMaxBpm = heartRate.maxBpm,
            heartRateSamplesCount = heartRate.samplesCount,
            sleepHours = sleep.sleepHours,
            sleepSessionsCount = sleep.sessionsCount
        )
    }

    private suspend fun readStepsForDate(
        startInstant: Instant,
        endInstant: Instant,
        dataOriginFilter: Set<DataOrigin>,
        excludeOwnData: Boolean
    ): Long {
        val response = getClient().readRecords(
            ReadRecordsRequest<StepsRecord>(
                timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant),
                dataOriginFilter = dataOriginFilter
            )
        )

        var total = 0.0

        response.records
            .filterNot { record ->
                excludeOwnData && record.metadata.dataOrigin.packageName == context.packageName
            }
            .forEach { record ->
                val overlapStart = maxOf(record.startTime, startInstant)
                val overlapEnd = minOf(record.endTime, endInstant)

                if (overlapEnd.isAfter(overlapStart)) {
                    val recordDurationMillis =
                        Duration.between(record.startTime, record.endTime).toMillis()

                    val overlapDurationMillis =
                        Duration.between(overlapStart, overlapEnd).toMillis()

                    if (recordDurationMillis > 0L) {
                        val part =
                            record.count.toDouble() * overlapDurationMillis.toDouble() / recordDurationMillis.toDouble()

                        total += part
                    }
                }
            }

        return total.toLong()
    }

    private suspend fun readHeartRateForDate(
        startInstant: Instant,
        endInstant: Instant,
        dataOriginFilter: Set<DataOrigin>,
        excludeOwnData: Boolean
    ): HealthConnectHeartRateSummary {
        val response = getClient().readRecords(
            ReadRecordsRequest<HeartRateRecord>(
                timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant),
                dataOriginFilter = dataOriginFilter
            )
        )

        val bpmValues = response.records
            .filterNot { record ->
                excludeOwnData && record.metadata.dataOrigin.packageName == context.packageName
            }
            .flatMap { record ->
                record.samples
                    .filter { sample ->
                        !sample.time.isBefore(startInstant) &&
                                sample.time.isBefore(endInstant)
                    }
                    .map { sample -> sample.beatsPerMinute }
            }

        if (bpmValues.isEmpty()) {
            return HealthConnectHeartRateSummary(
                avgBpm = null,
                minBpm = null,
                maxBpm = null,
                samplesCount = 0
            )
        }

        return HealthConnectHeartRateSummary(
            avgBpm = bpmValues.average(),
            minBpm = bpmValues.minOrNull(),
            maxBpm = bpmValues.maxOrNull(),
            samplesCount = bpmValues.size
        )
    }

    private suspend fun readSleepForDate(
        date: LocalDate,
        startInstant: Instant,
        endInstant: Instant,
        dataOriginFilter: Set<DataOrigin>,
        excludeOwnData: Boolean
    ): HealthConnectSleepSummary {
        val searchStart = date.minusDays(1).atStartOfDay(zoneId).toInstant()
        val searchEnd = date.plusDays(1).atStartOfDay(zoneId).toInstant()

        val response = getClient().readRecords(
            ReadRecordsRequest<SleepSessionRecord>(
                timeRangeFilter = TimeRangeFilter.between(searchStart, searchEnd),
                dataOriginFilter = dataOriginFilter
            )
        )

        var totalMinutes = 0L
        var overlappingSessionsCount = 0

        response.records
            .filterNot { record ->
                excludeOwnData && record.metadata.dataOrigin.packageName == context.packageName
            }
            .forEach { record ->
                val overlapStart = maxOf(record.startTime, startInstant)
                val overlapEnd = minOf(record.endTime, endInstant)

                if (overlapEnd.isAfter(overlapStart)) {
                    overlappingSessionsCount++
                    totalMinutes += Duration.between(overlapStart, overlapEnd).toMinutes()
                }
            }

        return HealthConnectSleepSummary(
            sleepHours = totalMinutes / 60.0,
            sessionsCount = overlappingSessionsCount
        )
    }

    suspend fun clearOwnDemoDataInRange(
        startDate: LocalDate,
        endDateInclusive: LocalDate
    ) {
        val startInstant = startDate.atStartOfDay(zoneId).toInstant()
        val endInstantExclusive = endDateInclusive
            .plusDays(1)
            .atStartOfDay(zoneId)
            .toInstant()

        deleteOwnSteps(startInstant, endInstantExclusive)
        deleteOwnHeartRate(startInstant, endInstantExclusive)
        deleteOwnSleep(startInstant, endInstantExclusive)
    }

    suspend fun replaceDemoData(
        dailySteps: List<DailyStepsInput>,
        dailyHeartRate: List<DailyHeartRateInput>,
        dailySleep: List<DailySleepInput>,
        heartRateIntervals: List<HeartRateIntervalInput> = emptyList()
    ) {
        val allDates = buildList {
            addAll(dailySteps.map { it.date })
            addAll(dailyHeartRate.map { it.date })
            addAll(dailySleep.map { it.date })
            addAll(heartRateIntervals.map { it.startTime.toLocalDate() })
        }.distinct().sorted()

        if (allDates.isEmpty()) {
            return
        }

        val startDate = allDates.first()
        val endDate = allDates.last()

        clearOwnDemoDataInRange(
            startDate = startDate,
            endDateInclusive = endDate
        )

        insertDemoSteps(dailySteps)

        if (heartRateIntervals.isNotEmpty()) {
            insertDemoHeartRateIntervals(heartRateIntervals)
        } else {
            insertDemoHeartRate(dailyHeartRate)
        }

        insertDemoSleep(dailySleep)
    }

    private suspend fun deleteOwnSteps(
        startInstant: Instant,
        endInstantExclusive: Instant
    ) {
        getClient().deleteRecords<StepsRecord>(
            timeRangeFilter = TimeRangeFilter.between(startInstant, endInstantExclusive)
        )
    }

    private suspend fun deleteOwnHeartRate(
        startInstant: Instant,
        endInstantExclusive: Instant
    ) {
        getClient().deleteRecords<HeartRateRecord>(
            timeRangeFilter = TimeRangeFilter.between(startInstant, endInstantExclusive)
        )
    }

    private suspend fun deleteOwnSleep(
        startInstant: Instant,
        endInstantExclusive: Instant
    ) {
        getClient().deleteRecords<SleepSessionRecord>(
            timeRangeFilter = TimeRangeFilter.between(startInstant, endInstantExclusive)
        )
    }

    suspend fun insertDemoSteps(
        dailySteps: List<DailyStepsInput>
    ) {
        if (dailySteps.isEmpty()) return

        val stepsRecords = dailySteps.map { item: DailyStepsInput ->
            val dayStartInstant = item.date
                .atStartOfDay(zoneId)
                .toInstant()

            val dayEndInstant = item.date
                .plusDays(1)
                .atStartOfDay(zoneId)
                .toInstant()

            StepsRecord(
                count = item.count,
                startTime = dayStartInstant,
                endTime = dayEndInstant,
                startZoneOffset = zoneId.rules.getOffset(dayStartInstant),
                endZoneOffset = zoneId.rules.getOffset(dayEndInstant),
                metadata = Metadata.manualEntry(
                    clientRecordId = "cyclesense_steps_${item.date}",
                    clientRecordVersion = 1L
                )
            )
        }

        if (stepsRecords.isNotEmpty()) {
            getClient().insertRecords(stepsRecords)
        }
    }

    suspend fun insertDemoHeartRate(
        dailyHeartRate: List<DailyHeartRateInput>
    ) {
        if (dailyHeartRate.isEmpty()) return

        val records = dailyHeartRate.map { item ->
            val startInstant = item.date.atStartOfDay(zoneId).toInstant()
            val endInstant = item.date.plusDays(1).atStartOfDay(zoneId).toInstant()

            HeartRateRecord(
                startTime = startInstant,
                endTime = endInstant,
                startZoneOffset = null,
                endZoneOffset = null,
                samples = listOf(
                    HeartRateRecord.Sample(
                        time = startInstant.plusSeconds(12 * 60 * 60),
                        beatsPerMinute = item.bpm
                    )
                ),
                metadata = Metadata.manualEntry(
                    clientRecordId = "cyclesense_hr_${item.date}",
                    clientRecordVersion = 1L
                )
            )
        }

        getClient().insertRecords(records)
    }

    suspend fun insertDemoSleep(
        dailySleep: List<DailySleepInput>
    ) {
        if (dailySleep.isEmpty()) return

        val records = dailySleep.map { item ->
            val totalMinutes = (item.sleepHours * 60f).toInt().coerceAtLeast(60)

            val sleepEnd = item.date.atTime(7, 0).atZone(zoneId).toInstant()
            val sleepStart = sleepEnd.minusSeconds(totalMinutes * 60L)

            val stages = buildDemoSleepStages(
                start = sleepStart,
                end = sleepEnd
            )

            SleepSessionRecord(
                startTime = sleepStart,
                endTime = sleepEnd,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(sleepStart),
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(sleepEnd),
                title = "CycleSense demo sleep",
                stages = stages,
                metadata = Metadata.manualEntry(
                    clientRecordId = "cyclesense_sleep_${item.date}",
                    clientRecordVersion = 1L
                )
            )
        }

        getClient().insertRecords(records)
    }

    private fun buildDemoSleepStages(
        start: Instant,
        end: Instant
    ): List<SleepSessionRecord.Stage> {
        val totalSeconds = (end.epochSecond - start.epochSecond).coerceAtLeast(60L)

        val awake1 = (totalSeconds * 0.05).toLong()
        val light1 = (totalSeconds * 0.30).toLong()
        val deep = (totalSeconds * 0.25).toLong()
        val light2 = (totalSeconds * 0.20).toLong()
        val rem = (totalSeconds * 0.15).toLong()
        val remaining = totalSeconds - awake1 - light1 - deep - light2 - rem

        var cursor = start

        fun nextStage(durationSec: Long, stageType: Int): SleepSessionRecord.Stage {
            val stageStart = cursor
            val stageEnd = cursor.plusSeconds(durationSec.coerceAtLeast(1L))
            cursor = stageEnd
            return SleepSessionRecord.Stage(
                startTime = stageStart,
                endTime = stageEnd,
                stage = stageType
            )
        }

        return listOf(
            nextStage(awake1, SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED),
            nextStage(light1, SleepSessionRecord.STAGE_TYPE_LIGHT),
            nextStage(deep, SleepSessionRecord.STAGE_TYPE_DEEP),
            nextStage(light2, SleepSessionRecord.STAGE_TYPE_LIGHT),
            nextStage(rem, SleepSessionRecord.STAGE_TYPE_REM),
            nextStage(remaining, SleepSessionRecord.STAGE_TYPE_SLEEPING)
        )
    }

    suspend fun insertDemoHeartRateIntervals(
        heartRateIntervals: List<HeartRateIntervalInput>
    ) {
        if (heartRateIntervals.isEmpty()) return

        val records = heartRateIntervals.mapIndexed { index, interval ->
            val startInstant = interval.startTime.atZone(zoneId).toInstant()
            val endInstant = interval.endTime.atZone(zoneId).toInstant()

            val endSampleTime = endInstant.minusSeconds(1)

            HeartRateRecord(
                startTime = startInstant,
                endTime = endInstant,
                startZoneOffset = null,
                endZoneOffset = null,
                samples = listOf(
                    HeartRateRecord.Sample(
                        time = startInstant.plusSeconds(1),
                        beatsPerMinute = interval.minBpm
                    ),
                    HeartRateRecord.Sample(
                        time = endSampleTime,
                        beatsPerMinute = interval.maxBpm
                    )
                ),
                metadata = Metadata.manualEntry(
                    clientRecordId = "cyclesense_hr_interval_${interval.startTime}_${index}",
                    clientRecordVersion = 1L
                )
            )
        }

        getClient().insertRecords(records)
    }
}

data class DailyStepsInput(
    val date: LocalDate,
    val count: Long
)

data class DailyHeartRateInput(
    val date: LocalDate,
    val bpm: Long
)

data class DailySleepInput(
    val date: LocalDate,
    val sleepHours: Float
)

data class HealthConnectDaySummary(
    val date: LocalDate,
    val steps: Long,
    val heartRateAvgBpm: Double?,
    val heartRateMinBpm: Long?,
    val heartRateMaxBpm: Long?,
    val heartRateSamplesCount: Int,
    val sleepHours: Double,
    val sleepSessionsCount: Int
)

private data class HealthConnectHeartRateSummary(
    val avgBpm: Double?,
    val minBpm: Long?,
    val maxBpm: Long?,
    val samplesCount: Int
)

private data class HealthConnectSleepSummary(
    val sleepHours: Double,
    val sessionsCount: Int
)