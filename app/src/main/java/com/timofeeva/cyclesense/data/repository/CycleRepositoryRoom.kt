package com.timofeeva.cyclesense.data.repository

import com.timofeeva.cyclesense.data.local.dao.CycleDao
import com.timofeeva.cyclesense.data.local.dao.CycleDayDao
import com.timofeeva.cyclesense.data.local.entity.CycleDayEntity
import com.timofeeva.cyclesense.data.local.entity.CycleEntity

class CycleRepositoryRoom(
    private val cycleDao: CycleDao,
    private val cycleDayDao: CycleDayDao
) {

    suspend fun insertCycle(cycle: CycleEntity): Long {
        return cycleDao.insertCycle(cycle)
    }

    suspend fun insertDay(day: CycleDayEntity): Long {
        return cycleDayDao.insertDay(day)
    }

    suspend fun insertDays(days: List<CycleDayEntity>) {
        cycleDayDao.insertDays(days)
    }

    suspend fun updateDay(day: CycleDayEntity) {
        cycleDayDao.updateDay(day)
    }

    suspend fun getAllCycles(): List<CycleEntity> {
        return cycleDao.getAllCycles()
    }

    suspend fun getAllDays(): List<CycleDayEntity> {
        return cycleDayDao.getAllDays()
    }

    suspend fun getCyclesForProfile(profileId: Long): List<CycleEntity> {
        return cycleDao.getCyclesForProfile(profileId)
    }

    suspend fun getCompletedCyclesForProfile(profileId: Long): List<CycleEntity> {
        return cycleDao.getCompletedCyclesForProfile(profileId)
    }

    suspend fun getLastCycleForProfile(profileId: Long): CycleEntity? {
        return cycleDao.getLastCycleForProfile(profileId)
    }

    suspend fun getDaysForProfile(profileId: Long): List<CycleDayEntity> {
        return cycleDayDao.getDaysForProfile(profileId)
    }

    suspend fun getDayForProfileByDate(profileId: Long, date: String): CycleDayEntity? {
        return cycleDayDao.getDayForProfileByDate(profileId, date)
    }

    suspend fun clearAllData() {
        cycleDayDao.clearAllDays()
        cycleDao.clearAllCycles()
    }

    suspend fun clearProfileData(profileId: Long) {
        cycleDayDao.clearDaysForProfile(profileId)
        cycleDao.clearCyclesForProfile(profileId)
    }

}