package at.yawk.katbot.action

import at.yawk.katbot.CancelEvent
import at.yawk.katbot.EventBus
import at.yawk.katbot.Subscribe
import at.yawk.katbot.command.Command
import at.yawk.katbot.sendMessageSafe
import java.net.URL
import javax.inject.Inject

/**
 * @author yawkat
 */
internal fun downloadInfo(): Map<Int, CharInfo> = URL("http://www.unicode.org/Public/UNIDATA/UnicodeData.txt")
        .openStream().bufferedReader().lineSequence()
        .map { it.split(';') }
        .associate { Integer.parseInt(it[0], 16) to CharInfo(it[1], it[2]) }

data class CharInfo(
        val name: String,
        val category: String
)

class UnicodeInfo @Inject constructor(
        val eventBus: EventBus
) {
    private val info: Map<Int, CharInfo> by lazy { downloadInfo() }

    fun start() {
        eventBus.subscribe(this)
    }


    @Subscribe
    fun command(command: Command) {
        if (command.line.startsWith("unicode_info")) {
            fun usage(): Nothing {
                command.channel.sendMessageSafe("Usage: ~unicode_info <name/category/xid/did> <character>")
                throw CancelEvent
            }

            if (command.line.parameters.size < 3) usage()

            val op: (Int) -> String = when (command.line.parameters[1]) {
                "name" -> { it -> info[it]?.name ?: "None" }
                "category" -> { it -> info[it]?.category ?: "None" }
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