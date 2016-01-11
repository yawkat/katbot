package at.yawk.katbot

import org.kitteh.irc.client.library.element.MessageReceiver

/**
 * @author yawkat
 */
interface IrcProvider {
    fun findChannels(channelNames: List<String>): List<MessageReceiver>

    fun registerEventListener(listener: Any)
}