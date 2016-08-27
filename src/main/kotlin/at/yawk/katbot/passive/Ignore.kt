/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.passive

import at.yawk.katbot.CancelEvent
import at.yawk.katbot.Config
import at.yawk.katbot.EventBus
import at.yawk.katbot.Subscribe
import at.yawk.katbot.security.PermissionName
import at.yawk.katbot.security.Security
import org.apache.shiro.util.ThreadContext
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import javax.inject.Inject

/**
 * @author yawkat
 */
class Ignore @Inject constructor(val eventBus: EventBus) {
    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe(priority = -1000)
    fun onPublicMessage(event: ChannelMessageEvent) {
        val permission = Security.createPermissionForChannelAndName(event.channel, PermissionName.TALK)
        if (!ThreadContext.getSubject().isPermitted(permission)) {
            throw CancelEvent
        }
    }
}