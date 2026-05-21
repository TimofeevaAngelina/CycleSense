package com.timofeeva.cyclesense.synthetic

import kotlin.random.Random

enum class SyntheticProfileType {
    CLASSIC,
    WEAK,
    ATYPICAL,
    FLAT,
    EXTREME
}

data class SyntheticProfile(
    val seed: Int,
    val profileType: SyntheticProfileType,

    val averageCycleLength: Int,
    val averageMensesLength: Int,
    val baseSteps: Int,
    val baseHeartRate: Float,
    val baseRmssd: Float,
    val baseSleepHours: Float,

    val baseStress: Float,
    val baseEnergy: Float,
    val baseMood: Float,
    val baseConcentration: Float,
    val basePain: Float,

    val phaseAmplitude: Float,
    val dayNoiseScale: Float,
    val cycleNoiseScale: Float
) {
    companion object {
        fun create(
            seed: Int,
            profileType: SyntheticProfileType? = null
        ): SyntheticProfile {
            val random = Random(seed)

            val resolvedType = profileType ?: pickProfileType(random)

            val cycleLength = when (resolvedType) {
                SyntheticProfileType.CLASSIC -> random.nextInt(27, 31)
                SyntheticProfileType.WEAK -> random.nextInt(26, 31)
                SyntheticProfileType.ATYPICAL -> random.nextInt(25, 33)
                SyntheticProfileType.FLAT -> random.nextInt(27, 30)
                SyntheticProfileType.EXTREME -> random.nextInt(25, 32)
            }

            val mensesLength = when (resolvedType) {
                SyntheticProfileType.CLASSIC -> random.nextInt(4, 6)
                SyntheticProfileType.WEAK -> random.nextInt(4, 6)
                SyntheticProfileType.ATYPICAL -> random.nextInt(3, 7)
                SyntheticProfileType.FLAT -> random.nextInt(4, 6)
                SyntheticProfileType.EXTREME -> random.nextInt(4, 7)
            }

            return SyntheticProfile(
                seed = seed,
                profileType = resolvedType,

                averageCycleLength = cycleLength,
                averageMensesLength = mensesLength,
                baseSteps = random.nextInt(5000, 9000),
                baseHeartRate = random.nextDouble(62.0, 76.0).toFloat(),
                baseRmssd = random.nextDouble(28.0, 55.0).toFloat(),
                baseSleepHours = random.nextDouble(6.4, 8.2).toFloat(),

                baseStress = random.nextDouble(4.6, 6.2).toFloat(),
                baseEnergy = random.nextDouble(5.2, 6.8).toFloat(),
                baseMood = random.nextDouble(5.4, 7.0).toFloat(),
                baseConcentration = random.nextDouble(5.4, 7.2).toFloat(),
                basePain = random.nextDouble(3.2, 5.0).toFloat(),

                phaseAmplitude = when (resolvedType) {
                    SyntheticProfileType.CLASSIC -> 1.0f
                    SyntheticProfileType.WEAK -> 0.55f
                    SyntheticProfileType.ATYPICAL -> 0.85f
                    SyntheticProfileType.FLAT -> 0.20f
                    SyntheticProfileType.EXTREME -> 1.45f
                },
                dayNoiseScale = when (resolvedType) {
                    SyntheticProfileType.CLASSIC -> 0.55f
                    SyntheticProfileType.WEAK -> 0.40f
                    SyntheticProfileType.ATYPICAL -> 0.75f
                    SyntheticProfileType.FLAT -> 0.25f
                    SyntheticProfileType.EXTREME -> 1.05f
                },
                cycleNoiseScale = when (resolvedType) {
                    SyntheticProfileType.CLASSIC -> 0.35f
                    SyntheticProfileType.WEAK -> 0.25f
                    SyntheticProfileType.ATYPICAL -> 0.50f
                    SyntheticProfileType.FLAT -> 0.15f
                    SyntheticProfileType.EXTREME -> 0.80f
                }
            )
        }

        private fun pickProfileType(random: Random): SyntheticProfileType {
            val p = random.nextDouble()
            return when {
                p < 0.40 -> SyntheticProfileType.CLASSIC
                p < 0.63 -> SyntheticProfileType.WEAK
                p < 0.80 -> SyntheticProfileType.ATYPICAL
                p < 0.93 -> SyntheticProfileType.FLAT
                else -> SyntheticProfileType.EXTREME
            }
        }
    }
}