package at.yawk.katbot

import org.kitteh.irc.client.library.element.MessageReceiver

/**
 * @author yawkat
 */
interface IrcProvider {
    fun findChannels(channelNames: Collection<String>): List<MessageReceiver>
}