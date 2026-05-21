package com.timofeeva.cyclesense.ml

object DemoDataFactory {

    fun buildThreeDemoCycles(): List<List<CycleDayData>> {
        val cycle1 = buildDemoCycle(cycleLength = 28, cycleOffset = 0)
        val cycle2 = buildDemoCycle(cycleLength = 27, cycleOffset = 1)
        val cycle3 = buildDemoCycle(cycleLength = 29, cycleOffset = 2)

        return listOf(cycle1, cycle2, cycle3)
    }

    private fun buildDemoCycle(
        cycleLength: Int,
        cycleOffset: Int
    ): List<CycleDayData> {
        return (1..cycleLength).map { day ->
            val phase = inferPhase(day, cycleLength)

            CycleDayData(
                dayInCycle = day,
                cycleLength = cycleLength,
                steps = 6500f + cycleOffset * 250f + day * 12f,
                heartRate = 68f + (day % 7) * 0.8f,
                rmssd = 42f - (day % 5) * 0.6f,
                sleepHours = 7.2f - (day % 4) * 0.12f,
                stressScore = when (phase) {
                    "menstrual" -> 6.2f
                    "follicular" -> 5.3f
                    "ovulatory" -> 5.5f
                    else -> 6.0f
                },
                energyLevel = when (phase) {
                    "menstrual" -> 5.7f
                    "follicular" -> 6.3f
                    "ovulatory" -> 6.4f
                    else -> 5.4f
                },
                moodScore = when (phase) {
                    "menstrual" -> 6.1f
                    "follicular" -> 6.5f
                    "ovulatory" -> 6.7f
                    else -> 6.0f
                },
                concentrationScore = when (phase) {
                    "menstrual" -> 6.7f
                    "follicular" -> 7.1f
                    "ovulatory" -> 7.2f
                    else -> 6.5f
                },
                painLevel = when (phase) {
                    "menstrual" -> 6.0f
                    "follicular" -> 4.5f
                    "ovulatory" -> 4.7f
                    else -> 5.2f
                },
                phase = phase
            )
        }
    }

    private fun inferPhase(dayInCycle: Int, cycleLength: Int): String {
        val ovulationDay = (cycleLength - 14).coerceAtLeast(10)

        return when {
            dayInCycle <= 5 -> "menstrual"
            dayInCycle < ovulationDay - 1 -> "follicular"
            dayInCycle in (ovulationDay - 1)..(ovulationDay + 1) -> "ovulatory"
            else -> "luteal"
        }
    }
}