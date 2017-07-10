package at.yawk.katbot.action

import at.yawk.katbot.CancelEvent
import at.yawk.katbot.EventBus
import at.yawk.katbot.Subscribe
import at.yawk.katbot.command.Command
import at.yawk.katbot.sendMessageSafe
import javax.inject.Inject

/**
 * @author yawkat
 */
class UnicodeInfo @Inject constructor(
        val eventBus: EventBus
) {
    fun start() {
        eventBus.subscribe(this)
    }


    @Subscribe
    fun command(command: Command) {
        if (command.line.startsWith("unicode_info")) {
            fun usage(): Nothing {
                command.channel.sendMessageSafe("Usage: ~unicode_info <name/xid/did> <character>")
                throw CancelEvent
            }

            if (command.line.parameters.size < 3) usage()

            val op: (Int) -> String = when (command.line.parameters[1]) {
                "name" -> Character::getName
                "xid" -> Integer::toHexString
                "did" -> Integer::toString
                else -> usage()
            }

            val char = command.line.tailParameterString(2)
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            val codePoints = (char as java.lang.CharSequence).codePoints().toArray()

            if (codePoints.size != 1) usage()

            command.channel.sendMessageSafe(op(codePoints[0]))

            throw CancelEvent
        }
    }
}