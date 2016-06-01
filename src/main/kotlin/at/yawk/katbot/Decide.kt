/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import javax.inject.Inject

/**
 * @author yawkat
 */
class Decide @Inject constructor(val eventBus: EventBus) {
    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe
    fun command(event: Command) {
        if (!event.line.startsWith("decide")) return
        val possibilities = event.line.parameterRange(1)
        val answer = when (possibilities.size) {
            0, 1 -> randomChoice(listOf("yes", "no"))
            else -> randomChoice(possibilities)
        }

        event.channel.sendMessageSafe("${event.actor.nick}, $answer")
        throw CancelEvent
    }
}