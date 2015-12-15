package at.yawk.katbot

import org.kitteh.irc.client.library.Client
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import org.kitteh.irc.lib.net.engio.mbassy.listener.Handler
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * @author yawkat
 */
class Uptime @Inject constructor(val client: Client) {
    var startTime: LocalDateTime? = null

    fun start() {
        startTime = LocalDateTime.now()
        client.eventManager.registerEventListener(this)
    }

    @Handler
    fun onPublicMessage(event: ChannelMessageEvent) {
        if (event.message.trimEnd() != "~uptime") return

        val start = startTime
        if (start != null) {
            val times = hashMapOf<String, Long>()

            var now = LocalDateTime.now()
            times["year"] = start.until(now, ChronoUnit.YEARS)
            now = now.minusYears(times["year"]!!)
            times["month"] = start.until(now, ChronoUnit.MONTHS)
            now = now.minusMonths(times["month"]!!)
            times["day"] = start.until(now, ChronoUnit.DAYS)
            now = now.minusDays(times["days"]!!)
            times["hour"] = start.until(now, ChronoUnit.HOURS)
            now = now.minusHours(times["hour"]!!)
            times["minute"] = start.until(now, ChronoUnit.MINUTES)
            now = now.minusMinutes(times["minute"]!!)
            times["second"] = start.until(now, ChronoUnit.SECONDS)

            val timeString = times.entries
                    .filter { it.value > 0 }
                    .joinToString (separator = ", ", transform = { "${it.value} ${it.key}${if (it.value == 1L) "" else "s"}" })

            event.channel.sendMessage("${event.actor.nick}, I've been up for $timeString")
        }
    }
}