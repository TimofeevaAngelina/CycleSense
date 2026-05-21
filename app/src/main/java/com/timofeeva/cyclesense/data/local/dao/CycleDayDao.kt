package com.timofeeva.cyclesense.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.timofeeva.cyclesense.data.local.entity.CycleDayEntity

@Dao
interface CycleDayDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(day: CycleDayEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDays(days: List<CycleDayEntity>)

    @androidx.room.Update
    suspend fun updateDay(day: CycleDayEntity)

    @Query("SELECT * FROM cycle_days ORDER BY date ASC")
    suspend fun getAllDays(): List<CycleDayEntity>

    @Query("""
        SELECT cd.* FROM cycle_days cd
        INNER JOIN cycles c ON cd.cycleId = c.id
        WHERE c.userProfileId = :profileId
        ORDER BY cd.date ASC
    """)
    suspend fun getDaysForProfile(profileId: Long): List<CycleDayEntity>

    @Query("""
        SELECT cd.* FROM cycle_days cd
        INNER JOIN cycles c ON cd.cycleId = c.id
        WHERE c.userProfileId = :profileId
          AND cd.date = :date
        LIMIT 1
    """)
    suspend fun getDayForProfileByDate(
        profileId: Long,
        date: String
    ): CycleDayEntity?

    @Query("DELETE FROM cycle_days")
    suspend fun clearAllDays()

    @Query("""
        DELETE FROM cycle_days
        WHERE cycleId IN (
            SELECT id FROM cycles WHERE userProfileId = :profileId
        )
    """)
    suspend fun clearDaysForProfile(profileId: Long)
}