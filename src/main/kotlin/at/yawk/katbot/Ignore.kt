package at.yawk.katbot

import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import javax.inject.Inject

/**
 * @author yawkat
 */
class Ignore @Inject constructor(val config: Config, val ircProvider: IrcProvider) {
    fun start() {
        ircProvider.registerEventListener(this)
    }

    @Subscribe(priority = -1000)
    fun onPublicMessage(event: ChannelMessageEvent) {
        if (config.ignore.contains(event.actor.nick)) throw CancelEvent
    }
}