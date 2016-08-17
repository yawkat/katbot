/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.action

import at.yawk.katbot.CancelEvent
import at.yawk.katbot.EventBus
import at.yawk.katbot.Subscribe
import at.yawk.katbot.command.Command
import at.yawk.katbot.security.IrcPermission
import at.yawk.katbot.security.PermissionName
import at.yawk.katbot.security.Security
import at.yawk.katbot.sendMessageSafe
import org.apache.shiro.mgt.SecurityManager
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import javax.inject.Inject

/**
 * @author yawkat
 */
class Restrict @Inject constructor(val eventBus: EventBus, val securityManager: SecurityManager) {
    private var restricted = false

    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe
    fun command(command: Command) {
        val newRestrict = if (command.line.messageIs("restrict")) {
            true
        } else if (command.line.messageIs("unrestrict")) {
            false
        } else return

        command.checkPermission(PermissionName.ADMIN)
        restricted = newRestrict
        command.channel.sendMessageSafe("Bot ${if (newRestrict) "" else "un"}restricted.")
        throw CancelEvent
    }

    @Subscribe(priority = -999)
    fun onPublicMessage(event: ChannelMessageEvent) {
        if (restricted) {
            val subject = Security.getSubjectForUser(securityManager, event.actor)
            val permission = Security.createPermissionForChannelAndName(event.channel, PermissionName.IGNORE_RESTRICT)
            if (!subject.isPermitted(permission)) throw CancelEvent
        }
    }
}