package at.yawk.katbot

import org.kitteh.irc.client.library.Client
import org.kitteh.irc.client.library.EventManager
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import org.kitteh.irc.lib.net.engio.mbassy.listener.Handler
import javax.inject.Inject

/**
 * @author yawkat
 */
class Factoid @Inject constructor(val ircProvider: IrcProvider, val config: Config) {
    fun start() {
        ircProvider.registerEventListener(this)
    }

    @Handler
    fun onPublicMessage(event: ChannelMessageEvent) {
        for (factoid in config.factoids.entries) {
            if (event.message.trimEnd().toLowerCase() == "~${factoid.key}") {
                event.channel.sendMessage(
                        Template(factoid.value)
                                .set("sender", event.actor.nick)
                                .finish()
                )
                break
            }
        }
    }
}