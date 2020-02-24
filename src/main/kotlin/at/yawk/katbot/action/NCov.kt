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

class NCov @Inject constructor(private val eventBus: EventBus) {
    private var cache: DataWithTime? = null

    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe
    fun command(command: Command) {
        if (command.line.messageIs("ncov")) {
            command.channel.sendMessageSafe("nCov cases: Total 79545 (2627 dead, 25179 recovered), Mainland China[c] 77150 (2593 dead, 24920 recovered), South Korea 833 (8 dead, 22 recovered), International conveyance[d] 691 (3 dead, 1 recovered), Italy 224 (5 dead, 2 recovered), Japan 154 (1 dead, 23 recovered), Singapore 90 (53 recovered), Hong Kong 79 (2 dead, 19 recovered), Iran 64 (12 dead, 3 recovered), United States 35 (7 recovered), Thailand 35 (21 recovered)")
            throw CancelEvent
        }
    }
}