package com.timofeeva.cyclesense.synthetic

import java.time.LocalDate
import kotlin.random.Random

data class SyntheticDayData(
    val date: LocalDate,
    val cycleIndex: Int,
    val dayInCycle: Int,
    val cycleLength: Int,
    val phase: String,

    val steps: Float,
    val calories: Float,
    val heartRate: Float,
    val rmssd: Float,
    val sleepHours: Float,

    val stressScore: Float,
    val energyLevel: Float,
    val moodScore: Float,
    val concentrationScore: Float,
    val painLevel: Float,

    val isMenstruationMarked: Boolean
)

class SyntheticCycleGenerator(
    private val profile: SyntheticProfile
) {
    fun generateHistory(
        startDate: LocalDate,
        cycleCount: Int,
        ageYears: Int = 27,
        weightKg: Float = 60f,
        heightCm: Float = 167f
    ): List<SyntheticDayData> {
        require(cycleCount > 0) { "cycleCount must be > 0" }

        val random = Random(profile.seed)
        val result = mutableListOf<SyntheticDayData>()

        var currentDate = startDate

        var prevStress: Float? = null
        var prevEnergy: Float? = null
        var prevMood: Float? = null
        var prevConcentration: Float? = null
        var prevPain: Float? = null

        val bmrBase = calculateBmr(
            ageYears = ageYears,
            weightKg = weightKg,
            heightCm = heightCm
        )

        repeat(cycleCount) { cycleIndex ->
            val cycleLength = varyCycleLength(random)
            val mensesLength = varyMensesLength(random, cycleLength)
            val ovulationDay = (cycleLength - 14).coerceAtLeast(10)

            val cycleShift = randomSigned(random, profile.cycleNoiseScale)

            var extremeEpisodeDaysLeft = 0
            var extremeEpisodeStrength = 0f

            for (day in 1..cycleLength) {
                val phase = inferPhase(
                    dayInCycle = day,
                    cycleLength = cycleLength,
                    mensesLength = mensesLength,
                    ovulationDay = ovulationDay
                )

                if (profile.profileType == SyntheticProfileType.EXTREME) {
                    val phaseEpisodeChance = when (phase) {
                        "menstrual" -> 0.22
                        "luteal" -> 0.20
                        "ovulatory" -> 0.10
                        else -> 0.08
                    }

                    if (extremeEpisodeDaysLeft == 0 && random.nextDouble() < phaseEpisodeChance) {
                        extremeEpisodeDaysLeft = random.nextInt(2, 5)
                        extremeEpisodeStrength = random.nextDouble(1.4, 2.8).toFloat()
                    }
                }

                val phaseEffects = phaseEffectsFor(profile.profileType, phase)
                val dailyNoise = randomSigned(random, profile.dayNoiseScale)

                val steps = (
                        profile.baseSteps +
                                profile.baseSteps * phaseEffects.stepsFactor +
                                profile.baseSteps * randomSigned(random, 0.08f)
                        ).coerceIn(1500f, 20000f)

                val heartRate = (
                        profile.baseHeartRate +
                                phaseEffects.heartRateShift +
                                cycleShift * 1.2f +
                                randomSigned(random, 2.0f)
                        ).coerceIn(48f, 95f)

                val rmssd = (
                        profile.baseRmssd +
                                phaseEffects.rmssdShift -
                                cycleShift * 2.0f +
                                randomSigned(random, 4.0f)
                        ).coerceIn(12f, 110f)

                val sleepHours = (
                        profile.baseSleepHours +
                                phaseEffects.sleepShift +
                                randomSigned(random, 0.45f)
                        ).coerceIn(4.5f, 10.5f)

                val calories = bmrBase +
                        steps * 0.04f +
                        maxOf(0f, heartRate - 70f) * 2f

                var stress = (
                        profile.baseStress +
                                phaseEffects.stressShift * profile.phaseAmplitude +
                                cycleShift * 0.9f +
                                physiologyStressAdjustment(
                                    sleepHours = sleepHours,
                                    heartRate = heartRate,
                                    rmssd = rmssd
                                ) +
                                dailyNoise
                        )

                var energy = (
                        profile.baseEnergy +
                                phaseEffects.energyShift * profile.phaseAmplitude -
                                cycleShift * 0.6f +
                                physiologyEnergyAdjustment(
                                    sleepHours = sleepHours,
                                    heartRate = heartRate
                                ) +
                                randomSigned(random, profile.dayNoiseScale)
                        )

                var mood = (
                        profile.baseMood +
                                phaseEffects.moodShift * profile.phaseAmplitude +
                                randomSigned(random, profile.dayNoiseScale)
                        )

                var concentration = (
                        profile.baseConcentration +
                                phaseEffects.concentrationShift * profile.phaseAmplitude +
                                randomSigned(random, profile.dayNoiseScale)
                        )

                var pain = (
                        profile.basePain +
                                phaseEffects.painShift * profile.phaseAmplitude +
                                cycleShift * 0.5f +
                                randomSigned(random, profile.dayNoiseScale)
                        )

                if (prevStress != null) {
                    if (profile.profileType == SyntheticProfileType.EXTREME) {
                        stress = 0.90f * stress + 0.10f * prevStress
                        energy = 0.90f * energy + 0.10f * prevEnergy!!
                        mood = 0.90f * mood + 0.10f * prevMood!!
                        concentration = 0.90f * concentration + 0.10f * prevConcentration!!
                        pain = 0.90f * pain + 0.10f * prevPain!!
                    } else {
                        stress = 0.82f * stress + 0.18f * prevStress
                        energy = 0.84f * energy + 0.16f * prevEnergy!!
                        mood = 0.82f * mood + 0.18f * prevMood!!
                        concentration = 0.84f * concentration + 0.16f * prevConcentration!!
                        pain = 0.83f * pain + 0.17f * prevPain!!
                    }
                }

                if (profile.profileType == SyntheticProfileType.EXTREME && extremeEpisodeDaysLeft > 0) {
                    stress += 1.2f * extremeEpisodeStrength
                    pain += 1.3f * extremeEpisodeStrength
                    energy -= 1.0f * extremeEpisodeStrength
                    mood -= 0.8f * extremeEpisodeStrength
                    concentration -= 0.9f * extremeEpisodeStrength

                    if (phase == "menstrual" || phase == "luteal") {
                        stress += 0.4f
                        pain += 0.5f
                    }

                    extremeEpisodeDaysLeft--
                }

                stress = clampScore(stress)
                energy = clampScore(energy)
                mood = clampScore(mood - 0.22f * (stress - 5.5f))
                concentration = clampScore(
                    concentration - 0.20f * (stress - 5.5f) + 0.15f * (energy - 5.5f)
                )
                pain = clampScore(pain + 0.18f * (stress - 5.5f))

                prevStress = stress
                prevEnergy = energy
                prevMood = mood
                prevConcentration = concentration
                prevPain = pain

                result += SyntheticDayData(
                    date = currentDate,
                    cycleIndex = cycleIndex,
                    dayInCycle = day,
                    cycleLength = cycleLength,
                    phase = phase,

                    steps = steps,
                    calories = calories,
                    heartRate = heartRate,
                    rmssd = rmssd,
                    sleepHours = sleepHours,

                    stressScore = stress,
                    energyLevel = energy,
                    moodScore = mood,
                    concentrationScore = concentration,
                    painLevel = pain,

                    isMenstruationMarked = phase == "menstrual"
                )

                currentDate = currentDate.plusDays(1)
            }
        }

        return result
    }

    private fun calculateBmr(
        ageYears: Int,
        weightKg: Float,
        heightCm: Float
    ): Float {
        return 10f * weightKg + 6.25f * heightCm - 5f * ageYears - 161f
    }

    private fun varyCycleLength(random: Random): Int {
        val delta = random.nextInt(-2, 3)
        return (profile.averageCycleLength + delta).coerceIn(24, 35)
    }

    private fun varyMensesLength(
        random: Random,
        cycleLength: Int
    ): Int {
        val delta = random.nextInt(-1, 2)
        return (profile.averageMensesLength + delta).coerceIn(3, minOf(7, cycleLength / 2))
    }

    private fun inferPhase(
        dayInCycle: Int,
        cycleLength: Int,
        mensesLength: Int,
        ovulationDay: Int
    ): String {
        return when {
            dayInCycle <= mensesLength -> "menstrual"
            dayInCycle < ovulationDay - 1 -> "follicular"
            dayInCycle in (ovulationDay - 1)..(ovulationDay + 1) -> "ovulatory"
            dayInCycle <= cycleLength -> "luteal"
            else -> "follicular"
        }
    }

    private fun clampScore(value: Float): Float {
        return value.coerceIn(1f, 10f)
    }

    private fun randomSigned(random: Random, scale: Float): Float {
        return random.nextDouble(-scale.toDouble(), scale.toDouble()).toFloat()
    }

    private fun physiologyStressAdjustment(
        sleepHours: Float,
        heartRate: Float,
        rmssd: Float
    ): Float {
        var adj = 0f

        if (sleepHours < 6.5f) adj += 0.35f
        if (heartRate > 76f) adj += 0.25f
        if (rmssd < 28f) adj += 0.35f

        return adj
    }

    private fun physiologyEnergyAdjustment(
        sleepHours: Float,
        heartRate: Float
    ): Float {
        var adj = 0f

        if (sleepHours > 7.5f) adj += 0.25f
        if (sleepHours < 6.0f) adj -= 0.35f
        if (heartRate > 80f) adj -= 0.20f

        return adj
    }

    private data class PhaseEffects(
        val stepsFactor: Float,
        val heartRateShift: Float,
        val rmssdShift: Float,
        val sleepShift: Float,
        val stressShift: Float,
        val energyShift: Float,
        val moodShift: Float,
        val concentrationShift: Float,
        val painShift: Float
    )

    private fun phaseEffectsFor(
        profileType: SyntheticProfileType,
        phase: String
    ): PhaseEffects {
        return when (profileType) {
            SyntheticProfileType.CLASSIC -> classicEffects(phase)
            SyntheticProfileType.WEAK -> weakEffects(phase)
            SyntheticProfileType.ATYPICAL -> atypicalEffects(phase)
            SyntheticProfileType.FLAT -> flatEffects(phase)
            SyntheticProfileType.EXTREME -> extremeEffects(phase)
        }
    }

    private fun classicEffects(phase: String): PhaseEffects {
        return when (phase) {
            "menstrual" -> PhaseEffects(-0.10f, 1.0f, -1.5f, -0.10f, 0.40f, -0.45f, -0.25f, -0.20f, 1.65f)
            "follicular" -> PhaseEffects(0.06f, -1.4f, 3.0f, 0.15f, -0.35f, 0.45f, 0.25f, 0.30f, -0.55f)
            "ovulatory" -> PhaseEffects(0.10f, 0.7f, 1.2f, 0.05f, -0.10f, 0.35f, 0.30f, 0.22f, -0.20f)
            else -> PhaseEffects(-0.03f, 2.1f, -3.2f, -0.35f, 0.70f, -0.65f, -0.20f, -0.30f, 0.55f)
        }
    }

    private fun weakEffects(phase: String): PhaseEffects {
        return when (phase) {
            "menstrual" -> PhaseEffects(-0.05f, 0.5f, -0.8f, 0.0f, 0.20f, -0.18f, -0.10f, -0.08f, 0.90f)
            "follicular" -> PhaseEffects(0.03f, -0.5f, 1.1f, 0.10f, -0.15f, 0.18f, 0.10f, 0.12f, -0.20f)
            "ovulatory" -> PhaseEffects(0.04f, 0.2f, 0.4f, 0.0f, 0.0f, 0.12f, 0.12f, 0.08f, -0.05f)
            else -> PhaseEffects(-0.02f, 0.8f, -1.1f, -0.12f, 0.28f, -0.22f, -0.08f, -0.10f, 0.30f)
        }
    }

    private fun atypicalEffects(phase: String): PhaseEffects {
        return when (phase) {
            "menstrual" -> PhaseEffects(-0.06f, 0.8f, -1.0f, -0.10f, 0.25f, -0.20f, -0.15f, -0.12f, 1.10f)
            "follicular" -> PhaseEffects(0.03f, 0.0f, 0.8f, 0.05f, 0.0f, 0.10f, 0.08f, 0.10f, 0.0f)
            "ovulatory" -> PhaseEffects(0.07f, 2.2f, -1.8f, -0.08f, 0.45f, -0.10f, 0.02f, -0.05f, 0.45f)
            else -> PhaseEffects(-0.01f, 0.6f, -0.5f, 0.0f, 0.20f, -0.08f, -0.05f, -0.05f, 0.15f)
        }
    }

    private fun flatEffects(phase: String): PhaseEffects {
        return when (phase) {
            "menstrual" -> PhaseEffects(-0.02f, 0.3f, -0.3f, 0.0f, 0.08f, -0.05f, -0.05f, -0.05f, 0.45f)
            "follicular" -> PhaseEffects(0.01f, -0.1f, 0.2f, 0.03f, -0.05f, 0.05f, 0.05f, 0.05f, -0.05f)
            "ovulatory" -> PhaseEffects(0.02f, 0.1f, 0.0f, 0.0f, 0.0f, 0.04f, 0.04f, 0.03f, 0.0f)
            else -> PhaseEffects(0.0f, 0.2f, -0.2f, -0.04f, 0.10f, -0.08f, -0.04f, -0.05f, 0.08f)
        }
    }

    private fun extremeEffects(phase: String): PhaseEffects {
        return when (phase) {
            "menstrual" -> PhaseEffects(-0.18f, 3.0f, -4.5f, -0.45f, 1.00f, -1.00f, -0.65f, -0.70f, 2.40f)
            "follicular" -> PhaseEffects(0.12f, -2.0f, 4.2f, 0.25f, -0.55f, 0.75f, 0.45f, 0.50f, -0.85f)
            "ovulatory" -> PhaseEffects(0.15f, 1.8f, -1.0f, -0.05f, 0.25f, 0.15f, 0.10f, 0.05f, 0.20f)
            else -> PhaseEffects(-0.08f, 4.0f, -5.0f, -0.60f, 1.25f, -1.20f, -0.55f, -0.65f, 1.10f)
        }
    }
}