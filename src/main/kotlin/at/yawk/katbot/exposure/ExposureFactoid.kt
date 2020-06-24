package at.yawk.katbot.exposure

import at.yawk.katbot.EventBus
import at.yawk.katbot.Subscribe
import at.yawk.katbot.command.Command
import at.yawk.katbot.sendMessageSafe
import java.lang.StringBuilder
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

internal fun buildMessage(data: Map<LocalDate, TemporaryExposureKeyExport>, today: LocalDate): String {
    val allKeysByDate = data.values
            .flatMap { it.keys }
            .groupBy { it.startDate }

    val builder = StringBuilder()
    builder.append("Latest update: ").append(data.keys.max()).append(" | ")
    builder.append("Keys by date:")
    for (date in allKeysByDate.keys.sortedDescending()) {
        val keysForDate = allKeysByDate.getValue(date)

        builder.append(" t-").append(date.until(today, ChronoUnit.DAYS)).append(": ").append(keysForDate.size)
        val keysRegisteredToday = data[data.keys.max()]!!.keys.filter { it.startDate == date }
        if (keysRegisteredToday.isNotEmpty()) {
            builder.append(" (+").append(keysRegisteredToday.size).append(')')
        }
    }
    return builder.toString()
}

@Singleton
class ExposureFactoid @Inject constructor(val eventBus: EventBus) {
    private val cache = ExposureCache()

    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe
    fun command(command: Command) {
        if (command.line.messageIs("exposure_notification_summary")) {
            val data = cache.listDates().associateWith { cache.loadKeys(it) }
            command.channel.sendMessageSafe((command.target ?: command.actor).nick + ", " + buildMessage(data,
                    LocalDate.now(ZoneOffset.UTC)))
        }
    }
}