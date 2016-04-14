/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import javax.inject.Inject

/**
 * @author yawkat
 */
class Restrict @Inject constructor(val roleManager: RoleManager, val eventBus: EventBus) {
    private var restricted = false

    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe
    fun command(command: Command) {
        val newRestrict = when (command.message) {
            "restrict" -> true
            "unrestrict" -> false
            else -> return
        }

        if (!roleManager.hasRole(command.actor, Role.ADMIN)) {
            command.channel.sendMessage("You aren't allowed to do that.")
            throw CancelEvent
        }
        restricted = newRestrict
        command.channel.sendMessage("Bot ${if (newRestrict) "" else "un"}restricted.")
        throw CancelEvent
    }

    @Subscribe(priority = -999)
    fun onPublicMessage(event: ChannelMessageEvent) {
        if (restricted && !roleManager.hasRole(event.actor, Role.IGNORE_RESTRICT)) throw CancelEvent
    }
}