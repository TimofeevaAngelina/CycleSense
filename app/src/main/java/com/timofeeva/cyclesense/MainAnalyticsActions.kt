package com.timofeeva.cyclesense

import com.timofeeva.cyclesense.analytics.ActivitySleepAnalyticsResult
import com.timofeeva.cyclesense.analytics.ActivitySleepAnalyticsService
import com.timofeeva.cyclesense.analytics.CycleTrendAnalyticsResult
import com.timofeeva.cyclesense.analytics.CycleTrendAnalyticsService
import com.timofeeva.cyclesense.analytics.HeartRateAnalyticsResult
import com.timofeeva.cyclesense.analytics.HeartRateAnalyticsService
import com.timofeeva.cyclesense.analytics.PhaseAnalyticsResult
import com.timofeeva.cyclesense.analytics.PhaseAnalyticsService
import com.timofeeva.cyclesense.analytics.RelationshipAnalyticsResult
import com.timofeeva.cyclesense.analytics.RelationshipAnalyticsService
import com.timofeeva.cyclesense.analytics.RelationshipMetric
import com.timofeeva.cyclesense.data.repository.CycleRepositoryRoom
import com.timofeeva.cyclesense.data.repository.HeartRateIntervalRepository
import com.timofeeva.cyclesense.ml.CycleDayData

data class PsychophysiologyAnalyticsBuildResult(
    val phaseAnalytics: PhaseAnalyticsResult,
    val cycleTrendAnalytics: CycleTrendAnalyticsResult
)

data class PhysiologyAnalyticsBuildResult(
    val activitySleepAnalytics: ActivitySleepAnalyticsResult,
    val heartRateAnalytics: HeartRateAnalyticsResult
)

suspend fun buildMainPsychophysiologyAnalytics(
    allCycles: List<List<CycleDayData>>,
    phaseAnalyticsService: PhaseAnalyticsService,
    cycleTrendAnalyticsService: CycleTrendAnalyticsService
): PsychophysiologyAnalyticsBuildResult {
    if (allCycles.isEmpty()) {
        error("Нет данных для психофизиологической аналитики.")
    }

    val phase = phaseAnalyticsService.buildPhaseAnalytics(allCycles)
    val trend = cycleTrendAnalyticsService.buildTypicalCycleTrend(allCycles)

    return PsychophysiologyAnalyticsBuildResult(
        phaseAnalytics = phase,
        cycleTrendAnalytics = trend
    )
}

suspend fun buildMainPhysiologyAnalytics(
    profileId: Long?,
    allCycles: List<List<CycleDayData>>,
    roomRepository: CycleRepositoryRoom,
    heartRateIntervalRepository: HeartRateIntervalRepository,
    activitySleepAnalyticsService: ActivitySleepAnalyticsService,
    heartRateAnalyticsService: HeartRateAnalyticsService
): PhysiologyAnalyticsBuildResult {
    if (allCycles.isEmpty()) {
        error("Нет данных для физиологической аналитики.")
    }

    val checkedProfileId = profileId
        ?: error("Нет активного профиля")

    val activitySleep = activitySleepAnalyticsService.buildActivitySleepAnalytics(allCycles)

    val dbCycles = roomRepository.getCyclesForProfile(checkedProfileId)
    val dbDays = roomRepository.getDaysForProfile(checkedProfileId)
    val heartRateSummaryByDate =
        heartRateIntervalRepository.getDaySummaryMapForProfile(checkedProfileId)

    val heartRate = heartRateAnalyticsService.buildTypicalHeartRateTrend(
        cycles = dbCycles,
        days = dbDays,
        heartRateSummaryByDate = heartRateSummaryByDate
    )

    return PhysiologyAnalyticsBuildResult(
        activitySleepAnalytics = activitySleep,
        heartRateAnalytics = heartRate
    )
}

suspend fun buildMainRelationshipAnalytics(
    allCycles: List<List<CycleDayData>>,
    selectedMetrics: List<RelationshipMetric>,
    relationshipAnalyticsService: RelationshipAnalyticsService
): RelationshipAnalyticsResult {
    if (allCycles.isEmpty()) {
        error("Нет данных для анализа взаимосвязей.")
    }

    if (selectedMetrics.size !in 2..3) {
        error("Выберите от 2 до 3 показателей для анализа взаимосвязей.")
    }

    return relationshipAnalyticsService.buildRelationshipAnalytics(
        allCycles = allCycles,
        selectedMetrics = selectedMetrics
    )
}