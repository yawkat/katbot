/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.action

import at.yawk.katbot.CancelEvent
import at.yawk.katbot.EventBus
import at.yawk.katbot.Subscribe
import at.yawk.katbot.command.Command
import at.yawk.katbot.sendMessageSafe
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
        if (command.line.startsWith("fortune")) {
            val args = command.line.parameterRange(1)
            command.channel.sendMessageSafe(getFortune(
                    offensive = args.contains("-o")
            ))
            throw CancelEvent
        }
    }
}