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
        val parameters = event.line.parameters
        if (parameters.size > 2 || parameters.size < 1) return
        val interactions = config.interactions[parameters[0].toLowerCase()] ?: return

        var target = parameters.getOrNull(1)
        if (target != null && (target.isEmpty() || !target.matches(SUBJECT_PATTERN.toRegex()))) return

        if (target == null || target == event.channel.client.nick) {
            target = event.actor.nick
        }
        Template(randomChoice(interactions))
                .setActorAndTarget(event.actor, target)
                .sendTo(event.channel)
        throw CancelEvent
    }
}