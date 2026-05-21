package com.timofeeva.cyclesense.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.timofeeva.cyclesense.data.local.entity.HeartRateIntervalEntity

@Dao
interface HeartRateIntervalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterval(interval: HeartRateIntervalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntervals(intervals: List<HeartRateIntervalEntity>)

    @Query("""
        SELECT * FROM heart_rate_intervals
        WHERE userProfileId = :profileId
        ORDER BY date ASC, startTime ASC
    """)
    suspend fun getIntervalsForProfile(profileId: Long): List<HeartRateIntervalEntity>

    @Query("""
        SELECT * FROM heart_rate_intervals
        WHERE userProfileId = :profileId
          AND date = :date
        ORDER BY startTime ASC
    """)
    suspend fun getIntervalsForProfileByDate(
        profileId: Long,
        date: String
    ): List<HeartRateIntervalEntity>

    @Query("DELETE FROM heart_rate_intervals")
    suspend fun clearAllIntervals()

    @Query("DELETE FROM heart_rate_intervals WHERE userProfileId = :profileId")
    suspend fun clearIntervalsForProfile(profileId: Long)
}