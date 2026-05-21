package com.timofeeva.cyclesense.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.timofeeva.cyclesense.data.local.entity.UserProfileEntity

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profiles ORDER BY isDemo ASC, id ASC")
    suspend fun getAllProfiles(): List<UserProfileEntity>

    @Query("SELECT * FROM user_profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfile(): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE id = :profileId LIMIT 1")
    suspend fun getProfileById(profileId: Long): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE firebaseUid = :firebaseUid AND isDemo = 0 LIMIT 1")
    suspend fun getRealProfileByFirebaseUid(firebaseUid: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<UserProfileEntity>)

    @Query("UPDATE user_profiles SET isActive = 0")
    suspend fun clearActiveProfile()

    @Query("UPDATE user_profiles SET isActive = 1 WHERE id = :profileId")
    suspend fun setActiveProfile(profileId: Long)

    suspend fun activateProfile(profileId: Long) {
        clearActiveProfile()
        setActiveProfile(profileId)
    }

    @Query("SELECT COUNT(*) FROM user_profiles")
    suspend fun getProfilesCount(): Int

    @Update
    suspend fun updateProfile(profile: UserProfileEntity)

    @Query("DELETE FROM user_profiles WHERE id = :profileId")
    suspend fun deleteProfileById(profileId: Long)
}