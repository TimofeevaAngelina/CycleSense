package com.timofeeva.cyclesense.data.repository

import com.timofeeva.cyclesense.data.local.dao.HeartRateIntervalDao
import com.timofeeva.cyclesense.data.local.entity.HeartRateIntervalEntity
import kotlin.math.roundToInt

class HeartRateIntervalRepository(
    private val dao: HeartRateIntervalDao
) {

    suspend fun insertInterval(interval: HeartRateIntervalEntity): Long {
        return dao.insertInterval(interval)
    }

    suspend fun insertIntervals(intervals: List<HeartRateIntervalEntity>) {
        dao.insertIntervals(intervals)
    }

    suspend fun getIntervalsForProfile(profileId: Long): List<HeartRateIntervalEntity> {
        return dao.getIntervalsForProfile(profileId)
    }

    suspend fun getIntervalsForProfileByDate(
        profileId: Long,
        date: String
    ): List<HeartRateIntervalEntity> {
        return dao.getIntervalsForProfileByDate(profileId, date)
    }

    suspend fun getDaySummary(
        profileId: Long,
        date: String
    ): HeartRateDaySummary? {
        val intervals = dao.getIntervalsForProfileByDate(profileId, date)
        if (intervals.isEmpty()) return null

        val minBpm = intervals.minOf { it.minBpm }
        val maxBpm = intervals.maxOf { it.maxBpm }
        val avgBpm = intervals.map { it.avgBpm }.average().roundToInt()

        return HeartRateDaySummary(
            minBpm = minBpm,
            maxBpm = maxBpm,
            avgBpm = avgBpm
        )
    }

    suspend fun clearAllIntervals() {
        dao.clearAllIntervals()
    }

    suspend fun clearIntervalsForProfile(profileId: Long) {
        dao.clearIntervalsForProfile(profileId)
    }
    suspend fun getDaySummaryMapForProfile(
        profileId: Long
    ): Map<java.time.LocalDate, HeartRateDaySummary> {
        val intervals = dao.getIntervalsForProfile(profileId)
        if (intervals.isEmpty()) return emptyMap()

        return intervals
            .groupBy { java.time.LocalDate.parse(it.date) }
            .mapValues { (_, dayIntervals) ->
                val minBpm = dayIntervals.minOf { it.minBpm }
                val maxBpm = dayIntervals.maxOf { it.maxBpm }
                val avgBpm = dayIntervals.map { it.avgBpm }.average().toInt()

                HeartRateDaySummary(
                    minBpm = minBpm,
                    maxBpm = maxBpm,
                    avgBpm = avgBpm
                )
            }
    }
}