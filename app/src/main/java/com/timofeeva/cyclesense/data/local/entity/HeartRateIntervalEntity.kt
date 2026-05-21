package com.timofeeva.cyclesense.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "heart_rate_intervals",
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["userProfileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userProfileId"]),
        Index(value = ["date"]),
        Index(value = ["userProfileId", "date"])
    ]
)
data class HeartRateIntervalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userProfileId: Long,
    val date: String,          // YYYY-MM-DD
    val startTime: String,     // ISO local datetime
    val endTime: String,       // ISO local datetime
    val minBpm: Int,
    val maxBpm: Int,
    val avgBpm: Int
)