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

    private fun getFortune(): String {
        val process = ProcessBuilder("fortune", "-n", "350", "-s")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
        val bytes = ByteStreams.toByteArray(process.inputStream)
        return String(bytes) // platform encoding
    }

    @Subscribe
    fun command(command: Command) {
        if (command.message.equals("fortune", ignoreCase = true)) {
            command.channel.sendMessage(getFortune())
        }
    }
}