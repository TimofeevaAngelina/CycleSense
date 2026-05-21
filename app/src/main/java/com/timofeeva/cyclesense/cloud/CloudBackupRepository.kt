package com.timofeeva.cyclesense.cloud

import com.google.firebase.firestore.FirebaseFirestore
import com.timofeeva.cyclesense.data.local.entity.CycleDayEntity
import com.timofeeva.cyclesense.data.local.entity.CycleEntity
import com.timofeeva.cyclesense.data.local.entity.HeartRateIntervalEntity
import com.timofeeva.cyclesense.data.local.entity.UserProfileEntity
import com.timofeeva.cyclesense.data.repository.CycleRepositoryRoom
import com.timofeeva.cyclesense.data.repository.HeartRateIntervalRepository
import kotlinx.coroutines.tasks.await

class CloudBackupRepository(
    private val roomRepository: CycleRepositoryRoom,
    private val heartRateIntervalRepository: HeartRateIntervalRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun cloudProfileExists(
        firebaseUid: String
    ): Boolean {
        val snapshot = firestore
            .collection("users")
            .document(firebaseUid)
            .collection("meta")
            .document("profile")
            .get()
            .await()

        return snapshot.exists()
    }

    suspend fun backupProfileToCloud(
        firebaseUid: String,
        profile: UserProfileEntity
    ) {
        val allCycles = roomRepository.getCyclesForProfile(profile.id)
        val allDays = roomRepository.getDaysForProfile(profile.id)

        val days = allDays.filter { it.hasCloudBackupData() }

        val cycles = allCycles.filter { cycle ->
            days.any { day -> day.cycleId == cycle.id }
        }

        val heartRateIntervals = heartRateIntervalRepository.getIntervalsForProfile(profile.id)

        val userRoot = firestore.collection("users").document(firebaseUid)

        // Профиль просто перезаписываем.
        userRoot.collection("meta")
            .document("profile")
            .set(profile.toCloudMap())
            .await()

        // ВАЖНО:
        // cycles и days делаем зеркалом Room.
        // Если день/цикл был удалён локально, он должен исчезнуть и из Firebase.
        deleteCollectionDocuments(userRoot.collection("cycles").path)
        deleteCollectionDocuments(userRoot.collection("days").path)

        // HeartRateIntervals пока НЕ очищаем:
        // их может быть много, и они не связаны с проблемой пустых календарных дней.
        // Поэтому оставляем прежнюю upsert-логику.

        val cyclesCollection = userRoot.collection("cycles")
        cycles.forEach { cycle ->
            cyclesCollection
                .document(cycle.startDate)
                .set(cycle.toCloudMap())
                .await()
        }

        val daysCollection = userRoot.collection("days")
        days.forEach { day ->
            daysCollection
                .document(day.date)
                .set(day.toCloudMap(cycles = cycles))
                .await()
        }

        val intervalsCollection = userRoot.collection("heartRateIntervals")
        heartRateIntervals.forEach { interval ->
            val documentId = buildHeartRateIntervalDocumentId(interval)
            intervalsCollection
                .document(documentId)
                .set(interval.toCloudMap())
                .await()
        }


    }

    suspend fun restoreProfileFromCloudIfLocalEmpty(
        firebaseUid: String,
        profile: UserProfileEntity
    ): Boolean {
        val localCycles = roomRepository.getCyclesForProfile(profile.id)
        val localDays = roomRepository.getDaysForProfile(profile.id)

        if (localCycles.isNotEmpty() || localDays.isNotEmpty()) {
            return false
        }

        val userRoot = firestore.collection("users").document(firebaseUid)

        val cycleSnapshots = userRoot.collection("cycles")
            .get()
            .await()
            .documents

        if (cycleSnapshots.isEmpty()) {
            return false
        }

        val cloudCycles = cycleSnapshots
            .mapNotNull { it.data?.toCycleEntity(profileId = profile.id) }
            .sortedBy { it.startDate }

        val oldCycleKeyToNewId = mutableMapOf<String, Long>()

        cloudCycles.forEach { cloudCycle ->
            val oldKey = cloudCycle.startDate
            val newId = roomRepository.insertCycle(
                cloudCycle.copy(id = 0L, userProfileId = profile.id)
            )
            oldCycleKeyToNewId[oldKey] = newId
        }

        val daySnapshots = userRoot.collection("days")
            .get()
            .await()
            .documents

        val cloudDays = daySnapshots
            .mapNotNull { document ->
                val data = document.data ?: return@mapNotNull null
                val cycleStartDate = data["cycleStartDate"] as? String ?: return@mapNotNull null
                val newCycleId = oldCycleKeyToNewId[cycleStartDate] ?: return@mapNotNull null
                data.toCycleDayEntity(cycleId = newCycleId)
            }
            .sortedBy { it.date }

        if (cloudDays.isNotEmpty()) {
            roomRepository.insertDays(
                cloudDays.map { it.copy(id = 0L) }
            )
        }

        val intervalSnapshots = userRoot.collection("heartRateIntervals")
            .get()
            .await()
            .documents

        val cloudIntervals = intervalSnapshots
            .mapNotNull { it.data?.toHeartRateIntervalEntity(profileId = profile.id) }
            .sortedWith(compareBy<HeartRateIntervalEntity> { it.date }.thenBy { it.startTime })

        cloudIntervals.forEach { interval ->
            heartRateIntervalRepository.insertInterval(interval.copy(id = 0L))
        }



        return true
    }

    suspend fun restoreProfileMetaFromCloud(
        firebaseUid: String,
        localProfile: UserProfileEntity
    ): UserProfileEntity {
        val userRoot = firestore.collection("users").document(firebaseUid)

        val snapshot = userRoot.collection("meta")
            .document("profile")
            .get()
            .await()

        val data = snapshot.data ?: return localProfile

        val restoredProfile = localProfile.copy(
            name = data["name"] as? String ?: localProfile.name,
            firebaseUid = firebaseUid,
            googleEmail = data["googleEmail"] as? String ?: localProfile.googleEmail,
            googleDisplayName = data["googleDisplayName"] as? String ?: localProfile.googleDisplayName,
            dateOfBirth = data["dateOfBirth"] as? String ?: localProfile.dateOfBirth,
            weightKg = getFloatFromCloud(data["weightKg"]) ?: localProfile.weightKg,
            heightCm = getFloatFromCloud(data["heightCm"]) ?: localProfile.heightCm,
            onboardingCompleted = data["onboardingCompleted"] as? Boolean
                ?: localProfile.onboardingCompleted,
            lastPeriodStartDate = data["lastPeriodStartDate"] as? String
                ?: localProfile.lastPeriodStartDate,
            avgCycleLength = getIntFromCloud(data["avgCycleLength"])
                ?: localProfile.avgCycleLength,
            avgMensesLength = getIntFromCloud(data["avgMensesLength"])
                ?: localProfile.avgMensesLength,
            privacyAcceptedAt = data["privacyAcceptedAt"] as? String
                ?: localProfile.privacyAcceptedAt,
            healthConnectSkippedAt = data["healthConnectSkippedAt"] as? String
                ?: localProfile.healthConnectSkippedAt
        )

        return restoredProfile
    }

    private fun UserProfileEntity.toCloudMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "profileType" to profileType,
            "isDemo" to isDemo,
            "firebaseUid" to firebaseUid,
            "googleEmail" to googleEmail,
            "googleDisplayName" to googleDisplayName,
            "dateOfBirth" to dateOfBirth,
            "weightKg" to weightKg,
            "heightCm" to heightCm,
            "onboardingCompleted" to onboardingCompleted,
            "lastPeriodStartDate" to lastPeriodStartDate,
            "avgCycleLength" to avgCycleLength,
            "avgMensesLength" to avgMensesLength,
            "privacyAcceptedAt" to privacyAcceptedAt,
            "healthConnectSkippedAt" to healthConnectSkippedAt
        )
    }

    private fun CycleEntity.toCloudMap(): Map<String, Any?> {
        return mapOf(
            "startDate" to startDate,
            "endDate" to endDate,
            "cycleLength" to cycleLength,
            "predictedCycleLength" to predictedCycleLength,
            "predictedMensesLength" to predictedMensesLength,
            "predictedOvulationDay" to predictedOvulationDay,
            "isCompleted" to isCompleted
        )
    }

    private fun CycleDayEntity.toCloudMap(
        cycles: List<CycleEntity>
    ): Map<String, Any?> {
        val cycle = cycles.firstOrNull { it.id == cycleId }

        return mapOf(
            "cycleStartDate" to cycle?.startDate,
            "date" to date,
            "dayInCycle" to dayInCycle,
            "phase" to phase.takeIf { it != "unknown" },
            "steps" to steps,
            "calories" to calories,
            "heartRate" to heartRate,
            "rmssd" to rmssd,
            "sleepHours" to sleepHours,
            "stressScore" to stressScore,
            "energyLevel" to energyLevel,
            "moodScore" to moodScore,
            "concentrationScore" to concentrationScore,
            "painLevel" to painLevel,
            "isMenstruationMarked" to isMenstruationMarked,
            "notes" to notes
        )
    }
    private fun CycleDayEntity.hasCloudBackupData(): Boolean {
        val hasKnownPhase = phase != null && phase != "unknown"

        return isMenstruationMarked ||
                hasKnownPhase ||
                steps != null ||
                calories != null ||
                heartRate != null ||
                rmssd != null ||
                sleepHours != null ||
                stressScore != null ||
                energyLevel != null ||
                moodScore != null ||
                concentrationScore != null ||
                painLevel != null ||
                !notes.isNullOrBlank()
    }

    private fun HeartRateIntervalEntity.toCloudMap(): Map<String, Any?> {
        return mapOf(
            "date" to date,
            "startTime" to startTime,
            "endTime" to endTime,
            "minBpm" to minBpm,
            "maxBpm" to maxBpm,
            "avgBpm" to avgBpm
        )
    }

    private fun Map<String, Any>.toCycleEntity(profileId: Long): CycleEntity {
        return CycleEntity(
            id = 0L,
            userProfileId = profileId,
            startDate = getString("startDate").orEmpty(),
            endDate = getString("endDate"),
            cycleLength = getInt("cycleLength"),
            predictedCycleLength = getInt("predictedCycleLength"),
            predictedMensesLength = getInt("predictedMensesLength"),
            predictedOvulationDay = getInt("predictedOvulationDay"),
            isCompleted = getBoolean("isCompleted") ?: false
        )
    }

    private fun Map<String, Any>.toCycleDayEntity(cycleId: Long): CycleDayEntity {
        return CycleDayEntity(
            id = 0L,
            cycleId = cycleId,
            date = getString("date").orEmpty(),
            dayInCycle = getInt("dayInCycle") ?: 1,
            phase = getString("phase"),
            steps = getFloat("steps"),
            calories = getFloat("calories"),
            heartRate = getFloat("heartRate"),
            rmssd = getFloat("rmssd"),
            sleepHours = getFloat("sleepHours"),
            stressScore = getFloat("stressScore"),
            energyLevel = getFloat("energyLevel"),
            moodScore = getFloat("moodScore"),
            concentrationScore = getFloat("concentrationScore"),
            painLevel = getFloat("painLevel"),
            isMenstruationMarked = getBoolean("isMenstruationMarked") ?: false,
            notes = getString("notes")
        )
    }

    private fun Map<String, Any>.toHeartRateIntervalEntity(profileId: Long): HeartRateIntervalEntity {
        return HeartRateIntervalEntity(
            id = 0L,
            userProfileId = profileId,
            date = getString("date").orEmpty(),
            startTime = getString("startTime").orEmpty(),
            endTime = getString("endTime").orEmpty(),
            minBpm = getInt("minBpm") ?: 0,
            maxBpm = getInt("maxBpm") ?: 0,
            avgBpm = getInt("avgBpm") ?: 0
        )
    }

    private fun buildHeartRateIntervalDocumentId(interval: HeartRateIntervalEntity): String {
        return "${interval.date}_${interval.startTime}_${interval.endTime}"
            .replace("/", "_")
            .replace(":", "-")
            .replace(".", "-")
    }

    private fun Map<String, Any>.getString(key: String): String? {
        return this[key] as? String
    }

    private fun Map<String, Any>.getBoolean(key: String): Boolean? {
        return this[key] as? Boolean
    }

    private fun Map<String, Any>.getInt(key: String): Int? {
        return when (val value = this[key]) {
            is Int -> value
            is Long -> value.toInt()
            is Double -> value.toInt()
            is Float -> value.toInt()
            else -> null
        }
    }

    private fun Map<String, Any>.getFloat(key: String): Float? {
        return when (val value = this[key]) {
            is Float -> value
            is Double -> value.toFloat()
            is Long -> value.toFloat()
            is Int -> value.toFloat()
            else -> null
        }
    }

    private fun getFloatFromCloud(value: Any?): Float? {
        return when (value) {
            is Float -> value
            is Double -> value.toFloat()
            is Long -> value.toFloat()
            is Int -> value.toFloat()
            else -> null
        }
    }

    private fun getIntFromCloud(value: Any?): Int? {
        return when (value) {
            is Long -> value.toInt()
            is Int -> value
            is Double -> value.toInt()
            is Float -> value.toInt()
            else -> null
        }
    }

    suspend fun deleteProfileFromCloud(
        firebaseUid: String
    ) {
        val userRoot = firestore.collection("users").document(firebaseUid)

        deleteCollectionDocuments(userRoot.collection("meta").path)
        deleteCollectionDocuments(userRoot.collection("cycles").path)
        deleteCollectionDocuments(userRoot.collection("days").path)
        deleteCollectionDocuments(userRoot.collection("heartRateIntervals").path)

        userRoot.delete().await()


    }



    private suspend fun deleteCollectionDocuments(
        collectionPath: String
    ) {
        val collection = firestore.collection(collectionPath)

        while (true) {
            val snapshot = collection
                .limit(100)
                .get()
                .await()

            if (snapshot.isEmpty) {
                break
            }

            val batch = firestore.batch()

            snapshot.documents.forEach { document ->
                batch.delete(document.reference)
            }

            batch.commit().await()
        }
    }

}