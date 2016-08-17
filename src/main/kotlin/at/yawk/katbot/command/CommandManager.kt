/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.command

import at.yawk.katbot.CancelEvent
import at.yawk.katbot.EventBus
import at.yawk.katbot.Subscribe
import at.yawk.katbot.security.IrcPermission
import at.yawk.katbot.security.PermissionName
import at.yawk.katbot.security.Security
import at.yawk.katbot.sendMessageSafe
import com.google.inject.ImplementedBy
import org.apache.shiro.mgt.SecurityManager
import org.apache.shiro.subject.Subject
import org.kitteh.irc.client.library.element.Channel
import org.kitteh.irc.client.library.element.MessageReceiver
import org.kitteh.irc.client.library.element.User
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author yawkat
 */
@ImplementedBy(CommandManager::class)
interface CommandBus {
    fun parseAndFire(
            context: CommandContext,
            message: String,
            parseWithoutPrefix: Boolean,
            cause: Cause?
    ): Boolean
}

@Singleton
class CommandManager @Inject constructor(val eventBus: EventBus, val securityManager: SecurityManager) : CommandBus {
    companion object {
        private inline fun indexOf(s: CharSequence, startIndex: Int, predicate: (Char) -> Boolean): Int? {
            for (i in startIndex..s.length - 1) {
                if (predicate(s[i])) {
                    return i
                }
            }
            return null
        }

        private fun findWhitespace(s: CharSequence, startIndex: Int) = indexOf(s, startIndex, { it.isWhitespace() })
        private fun findNonWhitespace(s: CharSequence, startIndex: Int) = indexOf(s, startIndex, { !it.isWhitespace() })
    }

    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe
    fun userMessage(event: PrivateMessageEvent) {
        message(event.actor, null, event.actor, event.message, false)
    }

    @Subscribe
    fun channelMessage(event: ChannelMessageEvent) {
        message(event.channel, event.channel, event.actor, event.message, true)
    }

    private fun message(
            location: MessageReceiver,
            channel: Channel?,
            actor: User,
            message: String,
            public: Boolean
    ) {
        val userLocator = object : UserLocator {
            override fun getUser(nick: String): User? {
                return channel?.getUser(nick)?.orElse(null)
            }
        }

        val session = Security.getSubjectForUser(securityManager, actor)


        if (parseAndFire(
                CommandContext(session, actor, location, userLocator, public),
                message,
                parseWithoutPrefix = !public,
                cause = null)) {
            throw CancelEvent
        }
    }

    override fun parseAndFire(
            context: CommandContext,
            message: String,
            parseWithoutPrefix: Boolean,
            cause: Cause?
    ): Boolean {
        if (message.startsWith("~")) {
            if (message.startsWith("~~")) {
                // todo: error messages
                val targetStart = findNonWhitespace(message, 2) ?: return true
                val targetEnd = findWhitespace(message, targetStart) ?: return true

                val targetName = message.substring(targetStart, targetEnd)
                val target = context.userLocator.getUser(targetName)
                if (target == null) {
                    context.channel.sendMessageSafe("Unknown user.")
                    return true
                }

                val commandStart = findNonWhitespace(message, targetEnd) ?: return true
                val line = CommandLine(message.substring(commandStart))

                submit(Command(context, target, line, cause))
            } else {
                val commandStart = findNonWhitespace(message, 1) ?: return true
                val line = CommandLine(message.substring(commandStart))

                submit(Command(context, null, line, cause))
            }
            return true
        }

        val ourNick = context.channel.client.nick
        if (message.startsWith("$ourNick,") || message.startsWith("$ourNick:")) {
            val commandStart = findNonWhitespace(message, ourNick.length + 1) ?: return true
            val line = CommandLine(message.substring(commandStart))

            submit(Command(context, null, line, cause))
            return true
        }

        if (parseWithoutPrefix) {
            submit(Command(context, null, CommandLine(message), cause))
            return true
        }
        return false
    }

    private fun submit(command: Command) {
        if (!eventBus.post(command)) {
            throw CancelEvent
        }
    }
}

interface UserLocator {
    fun getUser(nick: String): User?
}

data class CommandContext(
        val subject: Subject,
        val actor: User,
        val channel: MessageReceiver,
        val userLocator: UserLocator,
        val public: Boolean
) {
    fun buildScopedPermission(name: PermissionName): IrcPermission =
            Security.createPermissionForChannelAndName(channel as? Channel, name)
}

data class Command(
        val context: CommandContext,
        /** user input - do not trust */
        val target: User?,
        val line: CommandLine,
        val cause: Cause?
) {
    val actor: User
        get() = context.actor
    val channel: MessageReceiver
        get() = context.channel
    val public: Boolean
        get() = context.public

    fun isPermitted(permission: PermissionName) =
            context.subject.isPermitted(context.buildScopedPermission(permission))

    fun checkPermission(permission: PermissionName): Unit =
            context.subject.checkPermission(context.buildScopedPermission(permission))

    fun hasCause(predicate: (Cause) -> Boolean): Boolean {
        if (cause == null) return false
        if (predicate(cause)) return true
        return cause.command.hasCause(predicate)
    }
}

data class Cause(val command: Command, val meta: Any)