package at.yawk.katbot.exposure

import at.yawk.katbot.CancelEvent
import at.yawk.katbot.EventBus
import at.yawk.katbot.Subscribe
import at.yawk.katbot.command.Command
import at.yawk.katbot.sendMessageSafe
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.collect.Lists
import java.lang.StringBuilder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

private val NO_NOISE_TIME = LocalDateTime.of(2020, 6, 23, 8, 0, 0)

data class AnalysisResult(val streaks: List<Streak>)

private val TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private fun accumulate(data: Iterable<AnalysisResult>) =
    data.flatMap { it.streaks }
            .groupBy { it.map { it.startDate }.max()!! }
            .mapValues { it.value.size }

fun buildMessage(data: Map<LocalDateTime, AnalysisResult>): String {
    val accumulated = accumulate(data.values)
    val today = data.keys.max()!!.toLocalDate()
    val accumulatedToday = accumulate(data.filter { it.key.toLocalDate() == today }.values)

    val builder = StringBuilder()
    builder.append("Latest update: ").append(data.keys.max()!!.format(TIME_FORMAT)).append(" | ")
    builder.append("Registered infections by date:")
    for (date in accumulated.keys.sortedDescending()) {
        builder.append(" t-").append(date.until(today, ChronoUnit.DAYS)).append("d: ").append(accumulated[date])
        val keysRegisteredToday = accumulatedToday[date] ?: 0
        if (keysRegisteredToday != 0) {
            builder.append(" (+").append(keysRegisteredToday).append(')')
        }
    }
    return builder.toString()
}

@Singleton
class ExposureFactoid @Inject constructor(val eventBus: EventBus) {
    private val api: ExposureApi = ExposureCache()
    private val analysisCache = CacheBuilder.newBuilder()
            .build(CacheLoader.from<LocalDateTime, AnalysisResult> {
                val finder = StreakFinder(api.loadKeys(it!!).keys)
                AnalysisResult(Lists.newArrayList(finder.findStreaks(requireNoise = it != NO_NOISE_TIME)))
            })

    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe
    fun command(command: Command) {
        if (command.line.messageIs("exposure_notification_summary")) {
            val times = api.listDates().plusElement(LocalDate.now(ZoneId.of("Europe/Berlin")))
                    .distinct()
                    .flatMap { api.listTimes(it) }
            val data = times.associateWith { analysisCache[it]!! }
            command.channel.sendMessageSafe((command.target ?: command.actor).nick + ", " + buildMessage(data))
            throw CancelEvent
        }
    }
}