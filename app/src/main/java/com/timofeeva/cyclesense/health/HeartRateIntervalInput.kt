package com.timofeeva.cyclesense.health

import java.time.LocalDateTime

data class HeartRateIntervalInput(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val minBpm: Long,
    val maxBpm: Long
)