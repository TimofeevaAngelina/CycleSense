package com.timofeeva.cyclesense.ml

import java.time.LocalDate

class CycleRepository {

    private val cycles = mutableListOf<List<CycleDayData>>()

    init {
        cycles.addAll(DemoDataFactory.buildThreeDemoCycles())
    }

    fun getAllCycles(): List<List<CycleDayData>> {
        return cycles.map { it.toList() }
    }

    fun getLastCycles(count: Int): List<List<CycleDayData>> {
        return cycles.takeLast(count).map { it.toList() }
    }

    fun replaceAllCycles(newCycles: List<List<CycleDayData>>) {
        cycles.clear()
        cycles.addAll(newCycles.map { it.toList() })
    }

    fun updateDay(
        cycleIndex: Int,
        dayIndex: Int,
        newDay: CycleDayData
    ) {
        if (cycleIndex !in cycles.indices) return
        val cycle = cycles[cycleIndex].toMutableList()
        if (dayIndex !in cycle.indices) return

        cycle[dayIndex] = newDay
        cycles[cycleIndex] = cycle.toList()
    }

    fun updateLastCycleDay(
        dayInCycle: Int,
        updater: (CycleDayData) -> CycleDayData
    ) {
        if (cycles.isEmpty()) return

        val lastCycleIndex = cycles.lastIndex
        val lastCycle = cycles[lastCycleIndex].toMutableList()

        val idx = lastCycle.indexOfFirst { it.dayInCycle == dayInCycle }
        if (idx == -1) return

        lastCycle[idx] = updater(lastCycle[idx])
        cycles[lastCycleIndex] = lastCycle.toList()
    }

    fun getHistoryUpToDate(
        selectedDate: LocalDate,
        dayDataByDate: Map<LocalDate, CycleDayData>
    ): List<List<CycleDayData>> {
        val filtered = dayDataByDate
            .filterKeys { !it.isAfter(selectedDate) }
            .toList()
            .sortedBy { it.first }
            .map { it.second }

        if (filtered.isEmpty()) return emptyList()

        val grouped = mutableListOf<MutableList<CycleDayData>>()
        var current = mutableListOf<CycleDayData>()
        var previousDayInCycle = -1

        for (day in filtered) {
            if (current.isNotEmpty() && day.dayInCycle <= previousDayInCycle) {
                grouped.add(current)
                current = mutableListOf()
            }
            current.add(day)
            previousDayInCycle = day.dayInCycle
        }

        if (current.isNotEmpty()) grouped.add(current)

        return grouped.map { it.toList() }
    }
}