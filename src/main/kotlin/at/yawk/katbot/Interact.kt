/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * @author yawkat
 */
class Interact @Inject constructor(val eventBus: EventBus, val config: Config) {
    companion object {
        private val COMMAND_PATTERN = Pattern.compile("(\\w+)\\s*($SUBJECT_PATTERN)\\s*")
    }

    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe
    fun command(event: Command) {
        val matcher = COMMAND_PATTERN.matcher(event.message)
        if (matcher.matches()) {
            val command = matcher.group(1)
            val interactions = config.interactions[command.toLowerCase()]
            var target = matcher.group(2)
            if (target == event.channel.client.nick) {
                target = event.actor.nick
            }
            if (interactions != null && target.isNotEmpty()) {
                Template(randomChoice(interactions))
                        .set("target", target)
                        .sendAsReply(event)
                throw CancelEvent
            }
        }
    }
}