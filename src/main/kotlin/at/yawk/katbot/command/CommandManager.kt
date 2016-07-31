/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.command

import at.yawk.katbot.CancelEvent
import at.yawk.katbot.EventBus
import at.yawk.katbot.Subscribe
import at.yawk.katbot.sendMessageSafe
import com.google.inject.ImplementedBy
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
            actor: User,
            location: MessageReceiver,
            message: String,
            public: Boolean,
            parseWithoutPrefix: Boolean,
            userLocator: UserLocator,
            cause: Cause?
    ): Boolean
}

@Singleton
class CommandManager @Inject constructor(val eventBus: EventBus) : CommandBus {
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

        if (parseAndFire(actor, location, message,
                public = public,
                parseWithoutPrefix = !public,
                userLocator = userLocator,
                cause = null)) {

            throw CancelEvent
        }
    }

    override fun parseAndFire(
            actor: User,
            location: MessageReceiver,
            message: String,
            public: Boolean,
            parseWithoutPrefix: Boolean,
            userLocator: UserLocator,
            cause: Cause?
    ): Boolean {
        if (message.startsWith("~")) {
            if (message.startsWith("~~")) {
                // todo: error messages
                val targetStart = findNonWhitespace(message, 2) ?: return true
                val targetEnd = findWhitespace(message, targetStart) ?: return true

                val targetName = message.substring(targetStart, targetEnd)
                val target = userLocator.getUser(targetName)
                if (target == null) {
                    location.sendMessageSafe("Unknown user.")
                    return true
                }

                val commandStart = findNonWhitespace(message, targetEnd) ?: return true
                val line = CommandLine(message.substring(commandStart))

                submit(Command(location, userLocator, actor, target, line, public, cause))
            } else {
                val commandStart = findNonWhitespace(message, 1) ?: return true
                val line = CommandLine(message.substring(commandStart))

                submit(Command(location, userLocator, actor, null, line, public, cause))
            }
            return true
        }

        val ourNick = location.client.nick
        if (message.startsWith("$ourNick,") || message.startsWith("$ourNick:")) {
            val commandStart = findNonWhitespace(message, ourNick.length + 1) ?: return true
            val line = CommandLine(message.substring(commandStart))

            submit(Command(location, userLocator, actor, null, line, public, cause))
            return true
        }

        if (parseWithoutPrefix) {
            submit(Command(location, userLocator, actor, null, CommandLine(message), public, cause))
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

data class Command(
        val channel: MessageReceiver,
        val userLocator: UserLocator,
        val actor: User,
        /** user input - do not trust */
        val target: User?,
        val line: CommandLine,
        val public: Boolean,
        val cause: Cause?
) {
    fun hasCause(predicate: (Cause) -> Boolean): Boolean {
        if (cause == null) return false
        if (predicate(cause)) return true
        return cause.command.hasCause(predicate)
    }
}

data class Cause(val command: Command, val meta: Any)