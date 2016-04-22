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
class Interact @Inject constructor(val eventBus: EventBus, val config: Config) {

    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe
    fun command(event: Command) {
        if (event.line.parameters.size != 2) return
        val interactions = config.interactions[event.line.parameters[0].toLowerCase()] ?: return

        var target = event.line.parameters[1]
        if (!target.matches(SUBJECT_PATTERN.toRegex()) || target.isEmpty()) return

        if (target == event.channel.client.nick) {
            target = event.actor.nick
        }
        Template(randomChoice(interactions))
                .set("target", target)
                .sendAsReply(event)
        throw CancelEvent
    }
}