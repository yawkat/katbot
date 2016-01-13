package at.yawk.katbot

import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * @author yawkat
 */
class Interact @Inject constructor(val eventBus: EventBus, val config: Config) {
    companion object {
        private val COMMAND_PATTERN = Pattern.compile("(\\w+)\\s*($SUBJECT_PATTERN)\\s*")
    }

    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe
    fun command(event: Command) {
        val matcher = COMMAND_PATTERN.matcher(event.message)
        if (matcher.matches()) {
            val command = matcher.group(1)
            val interactions = config.interactions[command.toLowerCase()]
            val target = matcher.group(2)
            if (interactions != null && target.isNotEmpty()) {
                Template(randomChoice(interactions))
                        .set("target", target)
                        .sendAsReply(event)
                throw CancelEvent
            }
        }
    }
}