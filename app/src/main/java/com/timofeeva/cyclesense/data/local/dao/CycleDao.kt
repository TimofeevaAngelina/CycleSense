package com.timofeeva.cyclesense.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.timofeeva.cyclesense.data.local.entity.CycleEntity

@Dao
interface CycleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: CycleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycles(cycles: List<CycleEntity>)

    @Query("SELECT * FROM cycles ORDER BY startDate ASC")
    suspend fun getAllCycles(): List<CycleEntity>

    @Query("""
        SELECT * FROM cycles
        WHERE userProfileId = :profileId
        ORDER BY startDate ASC
    """)
    suspend fun getCyclesForProfile(profileId: Long): List<CycleEntity>

    @Query("""
        SELECT * FROM cycles
        WHERE userProfileId = :profileId AND isCompleted = 1
        ORDER BY startDate ASC
    """)
    suspend fun getCompletedCyclesForProfile(profileId: Long): List<CycleEntity>

    @Query("""
        SELECT * FROM cycles
        WHERE userProfileId = :profileId
        ORDER BY startDate DESC
        LIMIT 1
    """)
    suspend fun getLastCycleForProfile(profileId: Long): CycleEntity?

    @Query("DELETE FROM cycles")
    suspend fun clearAllCycles()

    @Query("DELETE FROM cycles WHERE userProfileId = :profileId")
    suspend fun clearCyclesForProfile(profileId: Long)
}