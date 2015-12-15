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
class Uptime @Inject constructor(val ircProvider: IrcProvider) {
    var startTime: LocalDateTime? = null

    fun start() {
        startTime = LocalDateTime.now()
        ircProvider.registerEventListener(this)
    }

    @Handler
    fun onPublicMessage(event: ChannelMessageEvent) {
        if (event.message.trimEnd() != "~uptime") return

        val start = startTime
        if (start != null) {
            val units = linkedMapOf(
                    Pair("year", ChronoUnit.YEARS),
                    Pair("month", ChronoUnit.MONTHS),
                    Pair("day", ChronoUnit.DAYS),
                    Pair("hour", ChronoUnit.HOURS),
                    Pair("minute", ChronoUnit.MINUTES),
                    Pair("second", ChronoUnit.SECONDS)
            )
            val timeStrings = arrayListOf<String>()
            var time = LocalDateTime.now()
            units.forEach { k, unit ->
                val value = start.until(time, unit)
                time = time.minus(value, unit)
                if (value > 0) {
                    timeStrings += "$value $k${if (value == 1L) "" else "s"}"
                }
            }

            val timeString = if (timeStrings.size == 0) {
                "0 seconds"
            } else if (timeStrings.size == 1) {
                timeStrings[0]
            } else {
                timeStrings.subList(0, timeStrings.size - 1).joinToString(separator = ", ") + " and " + timeStrings.last()
            }

            event.channel.sendMessage("${event.actor.nick}, I've been up for $timeString")
        }
    }
}