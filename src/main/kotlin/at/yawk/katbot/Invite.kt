/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import org.kitteh.irc.client.library.element.User
import org.kitteh.irc.client.library.event.channel.ChannelInviteEvent
import javax.inject.Inject

/**
 * @author yawkat
 */
class Invite @Inject constructor(val eventBus: EventBus, val roleManager: RoleManager) {
    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe
    fun onInvite(event: ChannelInviteEvent) {
        val actor = event.actor
        if (event.target == event.client.nick
                && actor is User
                && roleManager.hasRole(actor, Role.INVITE)) {
            event.channel.join()
        }
    }
}