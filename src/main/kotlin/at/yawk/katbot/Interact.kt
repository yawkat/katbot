package at.yawk.katbot

import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * @author yawkat
 */
class Interact @Inject constructor(val ircProvider: IrcProvider, val config: Config) {
    companion object {
        private val COMMAND_PATTERN = Pattern.compile("~\\s*(\\w+)\\s*($NAME_PATTERN)\\s*")
    }

    fun start() {
        ircProvider.registerEventListener(this)
    }

    @Subscribe
    fun onPublicMessage(event: ChannelMessageEvent) {
        val matcher = COMMAND_PATTERN.matcher(event.message)
        if (matcher.matches()) {
            val command = matcher.group(1)
            val interactions = config.interactions[command.toLowerCase()]
            val target = matcher.group(2)
            if (interactions != null && target.isNotEmpty()) {
                Template(randomChoice(interactions))
                        .set("bot", event.client.nick)
                        .set("target", target)
                        .sendTo(event.channel)
                throw CancelEvent
            }
        }
    }
}