package com.timofeeva.cyclesense.synthetic

import com.timofeeva.cyclesense.data.local.entity.CycleDayEntity
import com.timofeeva.cyclesense.data.local.entity.CycleEntity
import com.timofeeva.cyclesense.data.local.entity.HeartRateIntervalEntity
import com.timofeeva.cyclesense.data.local.entity.UserProfileEntity
import com.timofeeva.cyclesense.data.repository.CycleRepositoryRoom
import com.timofeeva.cyclesense.data.repository.HeartRateIntervalRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import kotlin.random.Random

data class SeedResult(
    val profileId: Long,
    val insertedCycles: Int,
    val insertedDays: Int,
    val profileType: SyntheticProfileType,
    val startDate: LocalDate,
    val endDate: LocalDate
)

class SyntheticDataSeeder(
    private val roomRepository: CycleRepositoryRoom,
    private val heartRateIntervalRepository: HeartRateIntervalRepository
) {

    suspend fun reseedDemoHistory(
        profileId: Long,
        seed: Int,
        fullPastCyclesCount: Int = 3,
        endDate: LocalDate = LocalDate.now(),
        profileType: SyntheticProfileType? = null,
        profile: UserProfileEntity? = null
    ): SeedResult {
        roomRepository.clearProfileData(profileId)
        heartRateIntervalRepository.clearIntervalsForProfile(profileId)

        val syntheticProfile = SyntheticProfile.create(
            seed = seed,
            profileType = profileType
        )

        val generator = SyntheticCycleGenerator(syntheticProfile)
        val random = Random(seed + 10_000)

        val ageYears = calculateAgeYears(profile?.dateOfBirth) ?: 27
        val weightKg = profile?.weightKg ?: 60f
        val heightCm = profile?.heightCm ?: 167f

        val totalCyclesToGenerate = (fullPastCyclesCount + 1).coerceAtLeast(2)
        val estimatedTotalDays = totalCyclesToGenerate * syntheticProfile.averageCycleLength
        val provisionalStartDate = endDate.minusDays((estimatedTotalDays - 1).toLong())

        val rawHistory = generator.generateHistory(
            startDate = provisionalStartDate,
            cycleCount = totalCyclesToGenerate,
            ageYears = ageYears,
            weightKg = weightKg,
            heightCm = heightCm
        )

        if (rawHistory.isEmpty()) {
            return SeedResult(
                profileId = profileId,
                insertedCycles = 0,
                insertedDays = 0,
                profileType = syntheticProfile.profileType,
                startDate = endDate,
                endDate = endDate
            )
        }

        val groupedByCycle = rawHistory
            .groupBy { it.cycleIndex }
            .toSortedMap()

        val cycleEntries = groupedByCycle.entries.toList()
        if (cycleEntries.isEmpty()) {
            return SeedResult(
                profileId = profileId,
                insertedCycles = 0,
                insertedDays = 0,
                profileType = syntheticProfile.profileType,
                startDate = endDate,
                endDate = endDate
            )
        }

        val fullPastCycles = cycleEntries
            .dropLast(1)
            .map { it.value.sortedBy { d -> d.date } }
            .takeLast(fullPastCyclesCount)

        val rawCurrentCycle = cycleEntries
            .last()
            .value
            .sortedBy { it.date }

        if (rawCurrentCycle.isEmpty()) {
            return SeedResult(
                profileId = profileId,
                insertedCycles = 0,
                insertedDays = 0,
                profileType = syntheticProfile.profileType,
                startDate = endDate,
                endDate = endDate
            )
        }

        val currentFullLength = rawCurrentCycle.first().cycleLength

        val minCurrentDay = minOf(7, currentFullLength)
        val maxCurrentDay = maxOf(minCurrentDay, currentFullLength - 3)

        val currentDayInCycle = if (maxCurrentDay > minCurrentDay) {
            random.nextInt(minCurrentDay, maxCurrentDay + 1)
        } else {
            minCurrentDay
        }

        val partialCurrentCycle = rawCurrentCycle
            .take(currentDayInCycle)
            .mapIndexed { index, day ->
                day.copy(dayInCycle = index + 1)
            }

        val finalCycles = fullPastCycles + listOf(partialCurrentCycle)
        val allDaysBeforeShift = finalCycles.flatten().sortedBy { it.date }

        if (allDaysBeforeShift.isEmpty()) {
            return SeedResult(
                profileId = profileId,
                insertedCycles = 0,
                insertedDays = 0,
                profileType = syntheticProfile.profileType,
                startDate = endDate,
                endDate = endDate
            )
        }

        val shiftDays = ChronoUnit.DAYS.between(
            allDaysBeforeShift.last().date,
            endDate
        )

        val shiftedCycles = finalCycles.map { cycle ->
            cycle.map { day ->
                day.copy(date = day.date.plusDays(shiftDays))
            }
        }

        var insertedCycles = 0
        var insertedDays = 0
        val allIntervals = mutableListOf<HeartRateIntervalEntity>()

        shiftedCycles.forEachIndexed { index, cycleDays ->
            if (cycleDays.isEmpty()) return@forEachIndexed

            val sortedDays = cycleDays.sortedBy { it.date }
            val firstDay = sortedDays.first()
            val lastDay = sortedDays.last()

            val isCurrentCycle = index == shiftedCycles.lastIndex

            val cycleId = roomRepository.insertCycle(
                CycleEntity(
                    userProfileId = profileId,
                    startDate = firstDay.date.toString(),
                    endDate = if (isCurrentCycle) null else lastDay.date.toString(),
                    cycleLength = if (isCurrentCycle) null else firstDay.cycleLength,
                    predictedCycleLength = firstDay.cycleLength,
                    predictedMensesLength = sortedDays.count { it.isMenstruationMarked },
                    predictedOvulationDay = estimateOvulationDay(firstDay.cycleLength),
                    isCompleted = !isCurrentCycle
                )
            )

            insertedCycles++

            val dayEntities = sortedDays.map { day ->
                CycleDayEntity(
                    cycleId = cycleId,
                    date = day.date.toString(),
                    dayInCycle = day.dayInCycle,
                    phase = day.phase,

                    steps = day.steps,
                    calories = day.calories,
                    heartRate = day.heartRate,
                    rmssd = day.rmssd,
                    sleepHours = day.sleepHours,

                    stressScore = day.stressScore,
                    energyLevel = day.energyLevel,
                    moodScore = day.moodScore,
                    concentrationScore = day.concentrationScore,
                    painLevel = day.painLevel,

                    isMenstruationMarked = day.isMenstruationMarked,
                    notes = buildString {
                        append("Synthetic profile=")
                        append(syntheticProfile.profileType)
                        append(", seed=")
                        append(seed)
                        append(", profileId=")
                        append(profileId)
                        append(", dateOfBirth=")
                        append(profile?.dateOfBirth ?: "null")
                        append(", weightKg=")
                        append(weightKg)
                        append(", heightCm=")
                        append(heightCm)
                        append(", ageYears=")
                        append(ageYears)
                        if (isCurrentCycle) {
                            append(", currentCycleDay=")
                            append(currentDayInCycle)
                            append("/")
                            append(firstDay.cycleLength)
                        }
                    }
                )
            }

            roomRepository.insertDays(dayEntities)
            insertedDays += dayEntities.size

            sortedDays.forEach { day ->
                allIntervals += generateHeartRateIntervalsForDay(
                    profileId = profileId,
                    date = day.date,
                    baseHeartRate = day.heartRate,
                    random = random
                )
            }
        }

        heartRateIntervalRepository.insertIntervals(allIntervals)

        val insertedAllDays = shiftedCycles.flatten().sortedBy { it.date }

        return SeedResult(
            profileId = profileId,
            insertedCycles = insertedCycles,
            insertedDays = insertedDays,
            profileType = syntheticProfile.profileType,
            startDate = insertedAllDays.first().date,
            endDate = insertedAllDays.last().date
        )
    }

    suspend fun extendCurrentCycleToDate(
        profileId: Long,
        targetDate: LocalDate,
        seed: Int,
        profileType: SyntheticProfileType? = null,
        profile: UserProfileEntity? = null
    ): SeedResult {
        val existingCycles = roomRepository.getCyclesForProfile(profileId)
        val existingDays = roomRepository.getDaysForProfile(profileId)

        if (existingCycles.isEmpty() || existingDays.isEmpty()) {
            return reseedDemoHistory(
                profileId = profileId,
                seed = seed,
                fullPastCyclesCount = 3,
                endDate = targetDate,
                profileType = profileType,
                profile = profile
            )
        }

        return reseedDemoHistory(
            profileId = profileId,
            seed = seed,
            fullPastCyclesCount = 3,
            endDate = targetDate,
            profileType = profileType,
            profile = profile
        )
    }

    fun calculateDaysBetween(
        startDate: LocalDate,
        endDate: LocalDate
    ): Int {
        return ChronoUnit.DAYS.between(startDate, endDate).toInt()
    }

    private fun estimateOvulationDay(cycleLength: Int): Int {
        return (cycleLength - 14).coerceAtLeast(10)
    }

    private fun calculateAgeYears(dateOfBirth: String?): Int? {
        if (dateOfBirth.isNullOrBlank()) return null
        return try {
            val birthDate = LocalDate.parse(dateOfBirth)
            Period.between(birthDate, LocalDate.now()).years
        } catch (e: Exception) {
            null
        }
    }

    private fun generateHeartRateIntervalsForDay(
        profileId: Long,
        date: LocalDate,
        baseHeartRate: Float,
        random: Random
    ): List<HeartRateIntervalEntity> {
        val intervals = mutableListOf<HeartRateIntervalEntity>()

        var currentStart = LocalDateTime.of(date, LocalTime.MIDNIGHT)

        repeat(48) {
            val currentEnd = currentStart.plusMinutes(30)

            val avg = (baseHeartRate + random.nextDouble(-5.0, 5.0).toFloat())
                .roundToInt()
                .coerceIn(45, 110)

            val spread = random.nextInt(4, 11)
            val min = (avg - spread).coerceAtLeast(40)
            val max = (avg + spread).coerceAtMost(140)

            intervals += HeartRateIntervalEntity(
                userProfileId = profileId,
                date = date.toString(),
                startTime = currentStart.toString(),
                endTime = currentEnd.toString(),
                minBpm = min,
                maxBpm = max,
                avgBpm = avg
            )

            currentStart = currentEnd
        }

        return intervals
    }
}