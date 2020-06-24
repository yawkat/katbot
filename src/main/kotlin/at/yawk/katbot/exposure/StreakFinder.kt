package at.yawk.katbot.exposure

import java.util.TreeMap

private val riskLevels = intArrayOf(5, 6, 8, 8, 8, 5, 3, 1, 1, 1, 1, 1, 1, 1)

typealias Streak = List<TemporaryExposureKey>

class StreakFinder(keys: List<TemporaryExposureKey>) {
    private val byDate = TreeMap(keys
            // filter weird values
            .filter { it.transmission_risk_level in riskLevels }
            .groupBy { it.startDate }
            .mapValues { it.value.toMutableList() })

    private fun findNormalStreaks(): List<Streak> {
        val (reportDate, lastList) = byDate.lastEntry()
        lastList.sortBy { riskLevels.indexOf(it.transmission_risk_level) }
        return lastList.mapNotNull { firstTek ->
            var riskLevelIndex = riskLevels.indexOf(firstTek.transmission_risk_level)
            if (riskLevelIndex == -1) {
                return@mapNotNull null
            }
            var date = reportDate
            val streak = ArrayList<TemporaryExposureKey>()
            while (riskLevelIndex < riskLevels.size) {
                val expectedRiskLevel = riskLevels[riskLevelIndex++]
                val here = (byDate[date] ?: break).find { it.transmission_risk_level == expectedRiskLevel } ?: break
                streak.add(here)
                date = date.minusDays(1)
            }
            streak
        }
    }

    private fun findBuggedStreaks(): List<Streak> {
        val (reportDate, firstList) = byDate.firstEntry()
        firstList.sortBy { riskLevels.indexOf(it.transmission_risk_level) }
        return firstList.mapNotNull { firstTek ->
            var riskLevelIndex = riskLevels.indexOf(firstTek.transmission_risk_level)
            if (riskLevelIndex == -1) {
                return@mapNotNull null
            }
            var date = reportDate
            val streak = ArrayList<TemporaryExposureKey>()
            while (riskLevelIndex < riskLevels.size) {
                val expectedRiskLevel = riskLevels[riskLevelIndex++]
                val here = (byDate[date] ?: break).find { it.transmission_risk_level == expectedRiskLevel } ?: break
                streak.add(here)
                date = date.plusDays(1)
            }
            streak
        }
    }

    private fun removeStreak(streak: Streak) {
        for (v in streak) {
            val removed = byDate[v.startDate]!!.remove(v)
            require(removed)
        }
    }

    private fun mayBeNoise(tek1: TemporaryExposureKey, tek2: TemporaryExposureKey) =
            tek1.rolling_period == tek2.rolling_period &&
                    tek1.transmission_risk_level == tek2.transmission_risk_level &&
                    tek1.rolling_start_interval_number == tek2.rolling_start_interval_number

    private fun hasNoise(streak: Streak, level: Int): Boolean {
        for (v in streak) {
            if (byDate[v.startDate]!!.count { mayBeNoise(it, v) } < level) {
                return false
            }
        }
        return true
    }

    private fun removeNoise(streak: Streak, level: Int): Boolean {
        for (v in streak) {
            val day = byDate[v.startDate]!!
            for (i in 0 until level) {
                day.remove(day.find { mayBeNoise(it, v) }!!)
            }
        }
        return true
    }

    private fun selectBestStreak(): Streak {
        return (findNormalStreaks() + findBuggedStreaks()).maxBy { it.size }!!
    }

    fun findStreaks(requireNoise: Boolean): Iterator<Streak> = iterator {
        while (byDate.isNotEmpty()) {
            val bestStreak = selectBestStreak()
            removeStreak(bestStreak)
            val hasNoise = requireNoise || hasNoise(bestStreak, 9)
            if (hasNoise) {
                removeNoise(bestStreak, 9)
            }
            yield(bestStreak)
            byDate.values.removeIf { it.isEmpty() }
        }
    }
}