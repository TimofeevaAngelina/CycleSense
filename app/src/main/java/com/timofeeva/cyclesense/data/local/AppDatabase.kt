package com.timofeeva.cyclesense.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.timofeeva.cyclesense.data.local.dao.CycleDao
import com.timofeeva.cyclesense.data.local.dao.CycleDayDao
import com.timofeeva.cyclesense.data.local.dao.HeartRateIntervalDao
import com.timofeeva.cyclesense.data.local.dao.UserProfileDao
import com.timofeeva.cyclesense.data.local.entity.CycleDayEntity
import com.timofeeva.cyclesense.data.local.entity.CycleEntity
import com.timofeeva.cyclesense.data.local.entity.HeartRateIntervalEntity
import com.timofeeva.cyclesense.data.local.entity.UserProfileEntity

@Database(
    entities = [
        UserProfileEntity::class,
        CycleEntity::class,
        CycleDayEntity::class,
        HeartRateIntervalEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun cycleDao(): CycleDao
    abstract fun cycleDayDao(): CycleDayDao
    abstract fun heartRateIntervalDao(): HeartRateIntervalDao
}