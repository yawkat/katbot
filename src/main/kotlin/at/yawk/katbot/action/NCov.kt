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
import java.lang.Exception
import java.lang.StringBuilder
import java.net.URL
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.Inject

private val CharSequence.lengthUtf8: Int
    get() =
        codePoints().map {
            when {
                it < 0x80 -> 1
                it < 0x800 -> 2
                it < 0x10000 -> 3
                else -> 4
            }
        }.sum()

class NCov @Inject constructor(private val eventBus: EventBus) {
    private var cache: DataWithTime? = null

    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe
    fun command(command: Command) {
        if (command.line.startsWith("ncov") && command.line.parameters.size == 3) {
            if (!command.public) {
                command.channel.sendMessageSafe("Not here, sorry.")
                throw CancelEvent
            }

            val wordDead = command.line.parameters[1]
            val wordRecovered = command.line.parameters[2]

            val cached = this.cache
            val now = Instant.now()
            var result: List<Region>
            if (cached == null || cached.time < now.minusSeconds(TimeUnit.MINUTES.toSeconds(15))) {
                result = try {
                    load()
                } catch (e: Exception) {
                    command.channel.sendMessageSafe(e.toString())
                    throw CancelEvent
                }
                this.cache = DataWithTime(now, result)
            } else {
                result = cached.result
            }

            result = result.sortedBy { -it.cases }

            val total = Region(
                    "Total",
                    cases = result.sumBy { it.cases },
                    deaths = result.sumBy { it.deaths },
                    recoveries = result.sumBy { it.recoveries })
            result = listOf(total) + result

            val messageBuilder = StringBuilder((command.target ?: command.actor).nick).append(", nCov cases: ")

            for ((i, region) in result.withIndex()) {
                var text = "${region.name} ${region.cases}"

                val extra = ArrayList<String>()
                if (region.deaths != 0) extra.add("${region.deaths}$wordDead")
                if (region.recoveries != 0) extra.add("${region.recoveries}$wordRecovered")
                if (extra.isNotEmpty()) text += " (" + extra.joinToString(", ") + ")"

                if (region.name == "Germany" || region.name == "Total") text = BOLD + text + RESET
                if (i != 0) text = ", $text"

                if (text.lengthUtf8 + messageBuilder.lengthUtf8 > MAX_MESSAGE_LENGTH - 3) {
                    messageBuilder.append("…")
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
        private val parenthesesNumber = Pattern.compile("\\d+ \\((\\d+) \\+ (\\d+)\\)")

        private fun toInt(s: String) =
                if (s == "" || s == "–") 0
                else s.replace(",", "").trim().toInt()

        fun load(): List<Region> {
            val doc = Jsoup.parse(URL(
                    "https://en.wikipedia.org/wiki/Template:2019-20_Wuhan_coronavirus_data/World"), 1000)
            val regions = ArrayList<Region>()
            for (row in doc.select(".wikitable tbody tr")) {
                if (row.hasClass("sortbottom")) continue
                val tds = row.select("td")
                val ths = row.select("th")
                if (tds.size < 2) continue
                if (ths.size != 1) continue
                val name = ths.single()
                val (cases, deaths) = tds
                val nameString = name.text().trim()
                val recoveries = if (tds.size >= 3) toInt(tds[2].ownText()) else 0
                val casesString = cases.text()
                val casesMatcher = parenthesesNumber.matcher(casesString)
                if (casesMatcher.matches() && nameString.contains("Japan")) {
                    regions.add(Region(
                            name = "Japan",
                            cases = toInt(casesMatcher.group(1)),
                            deaths = toInt(deaths.ownText()),
                            recoveries = recoveries
                    ))
                    regions.add(Region(
                            name = "Diamond Princess",
                            cases = toInt(casesMatcher.group(2)),
                            deaths = 0,
                            recoveries = 0
                    ))
                } else {
                    regions.add(Region(
                            name = nameString,
                            cases = toInt(casesString),
                            deaths = toInt(deaths.ownText()),
                            recoveries = recoveries
                    ))
                }
            }
            return regions
        }
    }

    data class Region(
            val name: String,
            val cases: Int,
            val deaths: Int,
            val recoveries: Int
    )
}
