/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import java.util.*
import java.util.concurrent.ThreadLocalRandom
import javax.inject.Inject

internal fun parseParameters(message: String): ArrayList<String> {
    val possibilities = arrayListOf<String>()
    val possibilityBuilder = StringBuilder()
    var quoted = false
    var escaped = false
    for (c in message) {
        if (!escaped) {
            if (c == '"') {
                quoted = !quoted
                continue
            } else if (c == '\\') {
                escaped = true
                continue
            } else if (c == ' ' && !quoted) {
                if (possibilityBuilder.length > 0) {
                    possibilities.add(possibilityBuilder.toString())
                    possibilityBuilder.setLength(0)
                }
                continue
            }
        }
        possibilityBuilder.append(c)
    }
    if (possibilityBuilder.length > 0) {
        possibilities.add(possibilityBuilder.toString())
    }
    return possibilities
}

/**
 * @author yawkat
 */
class Decide @Inject constructor(val eventBus: EventBus) {
    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe
    fun command(event: Command) {
        val message = event.message
        if (!message.startsWith("decide")) return
        val possibilities = parseParameters(message.substring("decide".length))
        val answer = when (possibilities.size) {
            0, 1 -> if (ThreadLocalRandom.current().nextBoolean()) "yes" else "no"
            else -> randomChoice(possibilities)
        }

        event.channel.sendMessage("${event.actor.nick}, $answer")
        throw CancelEvent
    }
}