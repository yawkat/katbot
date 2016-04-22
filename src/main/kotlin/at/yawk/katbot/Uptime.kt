/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * @author yawkat
 */
class Uptime @Inject constructor(val eventBus: EventBus) {
    var startTime: LocalDateTime? = null

    fun start() {
        startTime = LocalDateTime.now()
        eventBus.subscribe(this)
    }

    @Subscribe
    fun command(event: Command) {
        if (!event.line.messageIs("uptime")) return

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
            throw CancelEvent
        }
    }
}