/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import at.yawk.katbot.security.Security
import org.apache.shiro.mgt.SecurityManager
import org.kitteh.irc.client.library.element.User
import org.kitteh.irc.client.library.event.channel.ChannelInviteEvent
import javax.inject.Inject

/**
 * @author yawkat
 */
class Invite @Inject constructor(val eventBus: EventBus, val securityManager: SecurityManager) {
    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe
    fun onInvite(event: ChannelInviteEvent) {
        val actor = event.actor
        if (event.target == event.client.nick
                && actor is User
                && Security.getSubjectForUser(securityManager, actor).isPermitted("invite")) {
            event.channel.join()
        }
    }
}