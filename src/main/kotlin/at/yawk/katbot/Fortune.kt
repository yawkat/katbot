package at.yawk.katbot

import com.google.common.io.ByteStreams
import javax.inject.Inject

/**
 * @author yawkat
 */
class Fortune @Inject constructor(val eventBus: EventBus) {
    fun start() {
        eventBus.subscribe(this)
    }

    private fun getFortune(offensive: Boolean = false): String {
        var args = arrayOf("fortune", "-n", "350", "-s")
        if (offensive) args += "-o"
        val process = ProcessBuilder(*args)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
        val bytes = ByteStreams.toByteArray(process.inputStream)
        return String(bytes).replace("\\s+".toRegex(), " ") // platform encoding
    }

    @Subscribe
    fun command(command: Command) {
        if (command.message.startsWith("fortune", ignoreCase = true)) {
            val args = command.message.substring(7).split(" ").filter { it.isNotBlank() }
            command.channel.sendMessage(getFortune(
                    offensive = args.contains("-o")
            ))
        }
    }
}