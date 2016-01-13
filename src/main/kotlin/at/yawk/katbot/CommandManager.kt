package at.yawk.katbot

import org.kitteh.irc.client.library.Client
import org.kitteh.irc.client.library.element.Channel
import org.kitteh.irc.client.library.element.MessageReceiver
import org.kitteh.irc.client.library.element.User
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent
import javax.inject.Inject

/**
 * @author yawkat
 */
class CommandManager @Inject constructor(val eventBus: EventBus, val ircProvider: IrcProvider) {
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
        ircProvider.registerEventListener(this)
    }

    @Subscribe
    fun userMessage(event: PrivateMessageEvent) {
        message(event.client, event.actor, null, event.actor, event.message, false)
    }

    @Subscribe
    fun channelMessage(event: ChannelMessageEvent) {
        message(event.client, event.channel, event.channel, event.actor, event.message, true)
    }

    private fun message(
            client: Client,
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

        if (message.startsWith("~")) {
            if (message.startsWith("~~")) {
                // todo: error messages
                val targetStart = findNonWhitespace(message, 2) ?: return
                val targetEnd = findWhitespace(message, targetStart) ?: return

                val targetName = message.substring(targetStart, targetEnd)
                val target = userLocator.getUser(targetName)
                if (target == null) {
                    location.sendMessage("Unknown user.")
                    return
                }

                val commandStart = findNonWhitespace(message, targetEnd) ?: return
                val command = message.substring(commandStart)

                submit(Command(location, userLocator, actor, target, command, public))
                return
            } else {
                val commandStart = findNonWhitespace(message, 1) ?: return
                val command = message.substring(commandStart)

                submit(Command(location, userLocator, actor, null, command, public))
                return
            }
        }

        val ourNick = client.nick
        if (message.startsWith("$ourNick,") || message.startsWith("$ourNick:")) {
            val commandStart = findNonWhitespace(message, ourNick.length + 1) ?: return
            val command = message.substring(commandStart)

            submit(Command(location, userLocator, actor, null, command, public))
            return
        }

        if (!public) {
            submit(Command(location, userLocator, actor, null, message.trimStart(), public))
        }
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
        val target: User?,
        val message: String,
        val public: Boolean
)