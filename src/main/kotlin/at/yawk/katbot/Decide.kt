package at.yawk.katbot

import java.util.concurrent.ThreadLocalRandom
import javax.inject.Inject

/**
 * @author yawkat
 */
class Decide @Inject constructor(val eventBus: EventBus) {
    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe
    fun command(event: Command) {
        if (!event.message.startsWith("decide")) return
        val possibilities = arrayListOf<String>()
        var start = "decide ".length
        while (start < event.message.length) {
            var end = event.message.indexOf(' ', start)
            if (end == -1) end = event.message.length
            var possibility = event.message.substring(start, end)
            if (possibility.length > 0) {
                if (possibility[0] == '"') {
                    end = event.message.indexOf('"', end)
                    possibility = event.message.substring(start + 1, end)
                }
                if (!possibility.trim().isEmpty()) {
                    possibilities += possibility.trim()
                }
            }
            start = end + 1
        }
        val answer = when (possibilities.size) {
            0, 1 -> if (ThreadLocalRandom.current().nextBoolean()) "yes" else "no"
            else -> randomChoice(possibilities)
        }

        event.channel.sendMessage("${event.actor.nick}, $answer")
        throw CancelEvent
    }
}