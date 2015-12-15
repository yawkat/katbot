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
            times["years"] = start.until(now, ChronoUnit.YEARS)
            now = now.plusYears(times["years"]!!)
            times["months"] = start.until(now, ChronoUnit.MONTHS)
            now = now.plusMonths(times["months"]!!)
            times["days"] = start.until(now, ChronoUnit.DAYS)
            now = now.plusDays(times["days"]!!)
            times["hours"] = start.until(now, ChronoUnit.HOURS)
            now = now.plusHours(times["hours"]!!)
            times["minutes"] = start.until(now, ChronoUnit.MINUTES)
            now = now.plusMinutes(times["minutes"]!!)
            times["seconds"] = start.until(now, ChronoUnit.SECONDS)

            val timeString = times.entries
                    .filter { it.value > 0 }
                    .joinToString (separator = ", ", transform = { "${it.value} ${it.key}" })

            event.channel.sendMessage("${event.actor.nick}, I've been up for $timeString")
        }
    }
}