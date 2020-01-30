/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.action

import at.yawk.katbot.CancelEvent
import at.yawk.katbot.EventBus
import at.yawk.katbot.MAX_MESSAGE_LENGTH
import at.yawk.katbot.Subscribe
import at.yawk.katbot.command.Command
import at.yawk.katbot.sendMessageSafe
import org.jsoup.Jsoup
import java.lang.StringBuilder
import java.net.URL
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class NCov @Inject constructor(private val eventBus: EventBus) {
    private var cache: DataWithTime? = null

    fun start() {
        eventBus.subscribe(this)
    }

    private fun toString(region: Region): String {
        var s = "${region.name} ${region.cases}"
        if (region.deaths != 0) {
            s += " (${region.deaths} deaths)"
        }
        return s
    }

    @Subscribe
    fun command(command: Command) {
        if (command.line.messageIs("ncov")) {
            if (!command.public) {
                command.channel.sendMessageSafe("Not here, sorry.")
                throw CancelEvent
            }

            val cached = this.cache
            val now = Instant.now()
            var result: List<Region>
            if (cached == null || cached.time < now.minusSeconds(TimeUnit.MINUTES.toSeconds(15))) {
                result = load()
                this.cache = DataWithTime(now, result)
            } else {
                result = cached.result
            }

            val messageBuilder = StringBuilder(command.actor.nick).append(", nCov status: ")

            val germany = result.find { it.name == "Germany" }
            if (germany != null) {
                result = listOf(germany) + result.filter { it != germany }
            }

            for ((i, region) in result.withIndex()) {
                var text = toString(region)
                if (i != 0) text = ", $text"
                if (text.length + messageBuilder.length > MAX_MESSAGE_LENGTH) {
                    break
                }
                messageBuilder.append(text)
            }
            command.channel.sendMessageSafe(messageBuilder.toString())
            throw CancelEvent
        }
    }

    private class DataWithTime(
            val time: Instant,
            val result: List<Region>
    )

    companion object {
        private fun toInt(s: String) = s.replace(",", "").trim().toInt()

        fun load(): List<Region> {
            val doc = Jsoup.parse(URL(
                    "https://en.wikipedia.org/wiki/Template:2019-20_Wuhan_coronavirus_data/World"), 1000)
            val regions = ArrayList<Region>()
            for (row in doc.select(".wikitable tbody tr")) {
                if (row.hasClass("sortbottom")) continue
                val cells = row.select("td")
                if (cells.isEmpty()) continue
                val (name, cases, deaths) = cells
                regions.add(Region(name.text().trim(), toInt(cases.text()), toInt(deaths.text())))
            }
            return regions
        }
    }

    data class Region(
            val name: String,
            val cases: Int,
            val deaths: Int
    )
}